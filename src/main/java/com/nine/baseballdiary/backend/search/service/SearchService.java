package com.nine.baseballdiary.backend.search.service;

import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameRepository;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.search.dto.*;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.entity.FollowRequestStatus;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.FollowRequestRepository;
import com.nine.baseballdiary.backend.user.repository.UserBlockRepository; // 추가
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final GameRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final UserFollowRepository userFollowRepository;
    private final FollowRequestRepository followRequestRepository;
    private final UserBlockRepository userBlockRepository; // 추가
    private final RedisTemplate<String, String> redisTemplate;

    private static final String RECENT_SEARCH_PREFIX = "search:recent:";
    private static final String POPULAR_SEARCH_KEY = "search:popular:global";
    private static final int MAX_RECENT_SEARCHES = 10;
    private static final int MAX_POPULAR_SEARCHES = 10;

    // 날짜 포맷터
    private static final DateTimeFormatter UPLOAD_FMT =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)요일", Locale.KOREAN);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("H:mm");

    // 통합 검색
    @Transactional
    public SearchResultResponse search(Long userId, String query, int recordPage, int userPage, int recordSize, int userSize) {
        saveSearchQuery(userId, query);
        SearchRecordResponse recordResult = searchRecords(userId, query, recordPage, recordSize);
        SearchUserResponse userResult = searchUsers(userId, query, userPage, userSize);
        return SearchResultResponse.builder()
                .records(recordResult)
                .users(userResult)
                .build();
    }

    // ✅ 차단된 사용자 ID 목록 조회 (새로 추가)
    private Set<Long> getBlockedUserIds(Long currentUserId) {
        // 내가 차단한 사용자들
        Set<Long> myBlockedUsers = userBlockRepository.findBlockedIdsByBlockerId(currentUserId);

        // 나를 차단한 사용자들
        Set<Long> usersWhoBlockedMe = userBlockRepository.findBlockerIdsByBlockedId(currentUserId);

        // 합치기
        Set<Long> allBlockedUsers = new HashSet<>(myBlockedUsers);
        allBlockedUsers.addAll(usersWhoBlockedMe);

        return allBlockedUsers;
    }


    // ✅ 게시글 검색 (차단된 사용자 필터링 추가)
    @Transactional(readOnly = true)
    public SearchRecordResponse searchRecords(Long userId, String query, int page, int size) {
        Set<Long> followingUserIds = userFollowRepository.findByFollower_Id(userId).stream()
                .map(uf -> uf.getFollowee().getId())
                .collect(Collectors.toSet());

        // ✅ 'followingIdsStr' 선언 부분을 추가하여 오류 해결
        String followingIdsStr = followingUserIds.isEmpty() ? "{}" : "{" + followingUserIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "}";

        Pageable pageable = PageRequest.of(page, size);

        Page<GameRecord> recordPage = recordRepository.searchRecordsWithAccessAndBlockFilter(
                query.toLowerCase(), userId, followingIdsStr, pageable);

        // ✅ DTO의 정적 메서드를 사용하여 변환 (코드가 훨씬 깔끔해짐)
        List<SearchRecordDto> records = recordPage.getContent().stream()
                .map(record -> {
                    Game game = gameRepository.findById(record.getGame().getGameId()).orElseThrow();
                    User author = userRepository.findById(record.getUserId()).orElseThrow();
                    return SearchRecordDto.from(record, game, author);
                })
                .collect(Collectors.toList());

        return SearchRecordResponse.builder()
                .records(records)
                .currentPage(recordPage.getNumber())
                .totalPages(recordPage.getTotalPages())
                .totalElements(recordPage.getTotalElements())
                .hasNext(recordPage.hasNext())
                .build();
    }

    // ✅ 사용자 검색 (차단된 사용자 필터링 추가)
    @Transactional(readOnly = true)
    public SearchUserResponse searchUsers(Long currentUserId, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<User> userPage = userRepository.findByNicknameContainingIgnoreCaseAndIdNotExcludingBlocked(
                query, currentUserId, pageable);

        // ✅ DTO의 정적 메서드를 사용하여 변환
        List<SearchUserDto> users = userPage.getContent().stream()
                .map(user -> {
                    FollowStatus followStatus = getFollowStatus(currentUserId, user.getId());
                    return SearchUserDto.of(user, followStatus);
                })
                .collect(Collectors.toList());

        return SearchUserResponse.builder()
                .users(users)
                .currentPage(userPage.getNumber())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .hasNext(userPage.hasNext())
                .build();
    }

    // 최근 검색어 조회
    public List<String> getRecentSearches(Long userId) {
        String key = RECENT_SEARCH_PREFIX + userId;
        Set<String> searches = redisTemplate.opsForZSet().reverseRange(key, 0, MAX_RECENT_SEARCHES - 1);
        return new ArrayList<>(searches != null ? searches : Collections.emptySet());
    }

    // 최근 검색어 개별 삭제
    public void deleteRecentSearch(Long userId, String query) {
        String key = RECENT_SEARCH_PREFIX + userId;
        redisTemplate.opsForZSet().remove(key, query);
    }

    // 최근 검색어 전체 삭제
    public void clearRecentSearches(Long userId) {
        String key = RECENT_SEARCH_PREFIX + userId;
        redisTemplate.delete(key);
    }

    // 인기 검색어 조회
    public List<PopularSearchResponse> getPopularSearches() {
        Set<ZSetOperations.TypedTuple<String>> searches =
                redisTemplate.opsForZSet().reverseRangeWithScores(POPULAR_SEARCH_KEY, 0, MAX_POPULAR_SEARCHES - 1);

        if (searches == null) {
            return Collections.emptyList();
        }

        return searches.stream()
                .map(tuple -> PopularSearchResponse.builder()
                        .query(tuple.getValue())
                        .count(tuple.getScore().longValue())
                        .build())
                .collect(Collectors.toList());
    }

    // ===== Private Helper Methods =====

    // 검색어 저장 (최근 검색어 + 인기 검색어)
    private void saveSearchQuery(Long userId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        String trimmedQuery = query.trim();
        long currentTime = System.currentTimeMillis();

        // 최근 검색어 저장
        String recentKey = RECENT_SEARCH_PREFIX + userId;
        redisTemplate.opsForZSet().add(recentKey, trimmedQuery, currentTime);

        // 최대 개수 초과 시 오래된 것 삭제
        Long count = redisTemplate.opsForZSet().count(recentKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        if (count != null && count > MAX_RECENT_SEARCHES) {
            redisTemplate.opsForZSet().removeRange(recentKey, 0, count - MAX_RECENT_SEARCHES - 1);
        }

        // 인기 검색어 점수 증가
        redisTemplate.opsForZSet().incrementScore(POPULAR_SEARCH_KEY, trimmedQuery, 1.0);
    }


    // 팔로우 상태 확인
    private FollowStatus getFollowStatus(Long currentUserId, Long targetUserId) {
        // 이미 팔로우 중인지 확인
        boolean isFollowing = userFollowRepository.existsByFollower_IdAndFollowee_Id(currentUserId, targetUserId);
        if (isFollowing) {
            return FollowStatus.FOLLOWING;
        }

        // 팔로우 요청 대기 중인지 확인
        boolean isPending = followRequestRepository.existsByRequester_IdAndTarget_IdAndStatus(
                currentUserId, targetUserId, FollowRequestStatus.PENDING);
        if (isPending) {
            return FollowStatus.REQUESTED;
        }

        return FollowStatus.NOT_FOLLOWING;
    }


}
package com.nine.baseballdiary.backend.search.service;

import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameRepository;
import com.nine.baseballdiary.backend.record.Record;
import com.nine.baseballdiary.backend.record.RecordRepository;
import com.nine.baseballdiary.backend.search.dto.*;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.entity.FollowRequest;
import com.nine.baseballdiary.backend.user.entity.FollowRequestStatus;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.FollowRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final UserFollowRepository userFollowRepository;
    private final FollowRequestRepository followRequestRepository;
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
    @Transactional(readOnly = true)
    public SearchResultResponse search(Long userId, String query, int recordPage, int userPage, int recordSize, int userSize) {
        // 검색어 저장 (최근 검색어 & 인기 검색어)
        saveSearchQuery(userId, query);

        // 게시글 검색
        SearchRecordResponse recordResult = searchRecords(userId, query, recordPage, recordSize);

        // 사용자 검색
        SearchUserResponse userResult = searchUsers(userId, query, userPage, userSize);

        return SearchResultResponse.builder()
                .records(recordResult)
                .users(userResult)
                .build();
    }

    // 게시글 검색 (기존 Repository 패턴에 맞춤)
    @Transactional(readOnly = true)
    public SearchRecordResponse searchRecords(Long userId, String query, int page, int size) {
        // 내가 팔로우하는 사용자들의 ID 목록
        Set<Long> followingUserIds = userFollowRepository.findByFollowerId_Id(userId)
                .stream()
                .map(follow -> follow.getFolloweeId().getId())
                .collect(Collectors.toSet());

        // 기존 Repository 패턴에 맞춘 방식 (PostgreSQL bigint[] 사용)
        String followingIdsStr = followingUserIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        Pageable pageable = PageRequest.of(page, size);
        Page<Record> recordPage = recordRepository.searchRecordsWithAccess(
                query.toLowerCase(), userId, followingIdsStr, pageable);

        List<SearchRecordDto> records = recordPage.getContent().stream()
                .map(this::convertToSearchRecordDto)
                .collect(Collectors.toList());

        return SearchRecordResponse.builder()
                .records(records)
                .currentPage(page)
                .totalPages(recordPage.getTotalPages())
                .totalElements(recordPage.getTotalElements())
                .hasNext(recordPage.hasNext())
                .build();
    }

    // 사용자 검색
    @Transactional(readOnly = true)
    public SearchUserResponse searchUsers(Long currentUserId, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findByNicknameContainingIgnoreCaseAndIdNot(
                query, currentUserId, pageable);

        List<SearchUserDto> users = userPage.getContent().stream()
                .map(user -> convertToSearchUserDto(currentUserId, user))
                .collect(Collectors.toList());

        return SearchUserResponse.builder()
                .users(users)
                .currentPage(page)
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

    // Record를 SearchRecordDto로 변환
    private SearchRecordDto convertToSearchRecordDto(Record record) {
        // 게임 정보 조회
        Game game = gameRepository.findById(record.getGameId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임: " + record.getGameId()));

        // 작성자 정보 조회
        User author = userRepository.findById(record.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자: " + record.getUserId()));

        return SearchRecordDto.builder()
                .recordId(record.getRecordId())
                .authorId(author.getId())
                .authorNickname(author.getNickname())
                .authorProfileImage(author.getProfileImageUrl())
                .authorFavTeam(author.getFavTeam())
                .gameDate(game.getDate().format(UPLOAD_FMT))
                .gameTime(game.getTime().format(TIME_FMT))
                .homeTeam(convertTeamName(game.getHomeTeam()))
                .awayTeam(convertTeamName(game.getAwayTeam()))
                .homeScore(game.getHomeScore())
                .awayScore(game.getAwayScore())
                .stadium(convertStadiumName(game.getStadium()))
                .emotionCode(record.getEmotionCode())
                .emotionLabel(convertEmotionLabel(record.getEmotionCode()))
                .comment(record.getComment())
                .longContent(record.getLongContent())
                .result(record.getResult())
                .mediaUrls(record.getMediaUrls())
                .createdAt(record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }

    // User를 SearchUserDto로 변환
    private SearchUserDto convertToSearchUserDto(Long currentUserId, User user) {
        FollowStatus followStatus = getFollowStatus(currentUserId, user.getId());

        return SearchUserDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .favTeam(user.getFavTeam())
                .isPrivate(user.getIsPrivate())
                .followStatus(followStatus)
                .build();
    }

    // 팔로우 상태 확인
    private FollowStatus getFollowStatus(Long currentUserId, Long targetUserId) {
        // 이미 팔로우 중인지 확인
        boolean isFollowing = userFollowRepository.existsByFollowerId_IdAndFolloweeId_Id(currentUserId, targetUserId);
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

    // 팀명 변환
    private String convertTeamName(String teamCode) {
        return switch(teamCode) {
            case "KIA" -> "KIA 타이거즈";
            case "두산" -> "두산 베어스";
            case "롯데" -> "롯데 자이언츠";
            case "삼성" -> "삼성 라이온즈";
            case "키움" -> "키움 히어로즈";
            case "한화" -> "한화 이글스";
            case "KT" -> "KT WIZ";
            case "LG" -> "LG 트윈스";
            case "NC" -> "NC 다이노스";
            case "SSG" -> "SSG 랜더스";
            default -> teamCode;
        };
    }

    // 구장명 변환
    private String convertStadiumName(String stadiumCode) {
        return switch(stadiumCode) {
            case "잠실" -> "잠실야구장";
            case "문학" -> "문학야구장";
            case "고척" -> "고척 SKYDOME";
            case "사직" -> "사직야구장";
            case "수원" -> "KT 위즈 파크";
            case "대전(신)" -> "한화생명 이글스 파크";
            case "대구" -> "대구삼성라이온즈파크";
            case "광주" -> "기아 챔피언스 필드";
            case "창원" -> "NC 파크";
            default -> stadiumCode;
        };
    }

    // 감정 라벨 변환
    private String convertEmotionLabel(int code) {
        return switch(code) {
            case 1 -> "짜릿해요";
            case 2 -> "만족해요";
            case 3 -> "감동이에요";
            case 4 -> "놀랐어요";
            case 5 -> "행복해요";
            case 6 -> "답답해요";
            case 7 -> "아쉬워요";
            case 8 -> "화났어요";
            case 9 -> "지쳤어요";
            default -> "알 수 없음";
        };
    }
}
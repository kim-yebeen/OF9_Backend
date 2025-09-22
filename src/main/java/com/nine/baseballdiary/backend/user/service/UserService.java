package com.nine.baseballdiary.backend.user.service;

import com.nine.baseballdiary.backend.Notifiation.NotificationService;
import com.nine.baseballdiary.backend.S3.S3Service;
import com.nine.baseballdiary.backend.reaction.RecordReactionRepository;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.search.dto.FollowStatus;
import com.nine.baseballdiary.backend.user.dto.*;
import com.nine.baseballdiary.backend.user.entity.*;
import com.nine.baseballdiary.backend.user.repository.FollowRequestRepository;
import com.nine.baseballdiary.backend.user.repository.UserBlockRepository;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final UserFollowRepository followRepo;
    private final GameRecordRepository recordRepo;
    private final FollowRequestRepository reqRepo;
    private final NotificationService notificationService;
    private final UserBlockRepository userBlockRepo;
    private final RecordReactionRepository recordReactionRepo;
    private final S3Service s3Service;

    // ✅ 기존의 간단한 사용자 검색 메서드 (수정 완료)
    // 이 메서드는 UserDto를 반환하며, 페이징이 필요 없는 간단한 검색에 사용됩니다.
    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(Long currentUserId, String q) {
        // DB에서 닉네임으로 사용자 검색
        List<User> users = userRepo.findByNicknameContainingIgnoreCaseAndIdNotExcludingBlocked(
                q, currentUserId, org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        // DTO로 변환하여 반환
        return users.stream()
                .map(UserDto::from) // new UserDto(...) 대신 정적 팩토리 메서드 사용
                .collect(Collectors.toList());
    }



    // ✅ 팔로잉 목록 (수정 완료)
    @Transactional(readOnly = true)
    public List<UserDto> getFollowing(Long profileUserId, Long currentUserId) {
        // 1. 팔로잉하는 사람들의 ID 목록을 가져옵니다.
        List<Long> followingIds = followRepo.findByFollowerId_Id(profileUserId).stream()
                .map(follow -> follow.getFolloweeId().getId())
                .collect(Collectors.toList());

        if (followingIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. ID 목록을 사용해 User 엔티티를 조회하면서,
        //    현재 로그인한 사용자와 차단 관계인 사용자를 DB에서 미리 제외합니다.
        List<User> filteredUserList = userRepo.findByIdInExcludingBlocked(followingIds, currentUserId);

        // 3. 필터링된 사용자 목록을 DTO로 변환합니다.
        return convertToUserDtoWithFollowStatus(filteredUserList, currentUserId);
    }


    // ✅ 팔로워 목록 (수정 완료)
    @Transactional(readOnly = true)
    public List<UserDto> getFollowers(Long profileUserId, Long currentUserId) {
        // 1. 팔로워들의 ID 목록을 가져옵니다.
        List<Long> followerIds = followRepo.findByFolloweeId_Id(profileUserId).stream()
                .map(follow -> follow.getFollowerId().getId())
                .collect(Collectors.toList());

        if (followerIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. ID 목록을 사용해 User 엔티티를 조회하면서,
        //    현재 로그인한 사용자와 차단 관계인 사용자를 DB에서 미리 제외합니다.
        List<User> filteredUserList = userRepo.findByIdInExcludingBlocked(followerIds, currentUserId);

        // 3. 필터링된 사용자 목록을 DTO로 변환합니다.
        return convertToUserDtoWithFollowStatus(filteredUserList, currentUserId);
    }

    //유저 목록을 팔로우 상태가 포함된 userdto목록으로 변환하는 헬퍼 메서드
    private List<UserDto> convertToUserDtoWithFollowStatus(List<User> userList, Long currentUserId) {
        if (userList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> targetUserIds = userList.stream().map(User::getId).collect(Collectors.toList());
        Set<Long> followingIdSet = followRepo.findFolloweeIdsByFollowerIdAndInTargetUserIds(currentUserId, targetUserIds);
        Set<Long> requestedIdSet = reqRepo.findPendingRequestTargetIdsByRequesterIdAndInTargetUserIds(currentUserId, targetUserIds);

        return userList.stream().map(user -> {
            FollowStatus status;
            if (user.getId().equals(currentUserId)) {
                status = FollowStatus.ME;
            } else if (followingIdSet.contains(user.getId())) {
                status = FollowStatus.FOLLOWING;
            } else if (requestedIdSet.contains(user.getId())) {
                status = FollowStatus.REQUESTED;
            } else {
                status = FollowStatus.NOT_FOLLOWING;
            }
            return UserDto.from(user, status);
        }).collect(Collectors.toList());
    }

    /**
     * 1) 팔로우 요청 (공개면 즉시, 비공개면 PENDING 생성)
     */
    // ✅ 팔로우 요청 (차단된 사용자는 팔로우 불가)
    @Transactional
    public FollowResponse requestFollow(Long meId, Long targetId) {
        // 차단 여부 확인
        if (isBlockedEachOther(meId, targetId)) {
            throw new IllegalArgumentException("차단된 사용자와는 팔로우할 수 없습니다");
        }

        User me = userRepo.findById(meId).orElseThrow();
        User target = userRepo.findById(targetId).orElseThrow();

        // 이미 팔로우 중이면 아무 동작 없이 false 반환
        if (followRepo.existsByFollowerId_IdAndFolloweeId_Id(meId, targetId)) {
            return new FollowResponse(false, false, null);
        }

        if (Boolean.TRUE.equals(target.getIsPrivate())) {
            // 비공개 계정: PENDING 요청 생성
            FollowRequest req = FollowRequest.builder()
                    .requester(me)
                    .target(target)
                    .status(FollowRequestStatus.PENDING)
                    .build();
            req = reqRepo.save(req);
            notificationService.createFollowRequestNotification(targetId, meId);
            return new FollowResponse(true, true, req.getId());
        } else {
            // 공개 계정: 즉시 팔로우
            followRepo.save(new UserFollow(null, me, target));
            notificationService.createFollowNotification(targetId, meId);
            return new FollowResponse(true, false, null);
        }
    }

    // 2) 내 계정으로 온 PENDING 요청 리스트 조회
    //    (import org.springframework.transaction.annotation.Transactional;)
    @Transactional(readOnly = true)
    public List<FollowRequestDto> listIncomingRequests(Long me) {
        return reqRepo.findByTarget_IdAndStatus(me, FollowRequestStatus.PENDING)
                .stream()
                .map(req -> new FollowRequestDto(
                        req.getId(),
                        req.getRequester().getId(),
                        req.getRequester().getNickname(),
                        req.getCreatedAt()
                ))
                .toList();
    }

    /**
     * 3) 비공개 계정 주인이 수락
     */
    @Transactional
    public void acceptFollowRequest(Long me, Long requestId) {
        FollowRequest req = reqRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 요청입니다."));

        // 본인이 받은 요청이 아니면 403
        if (!req.getTarget().getId().equals(me)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "내 계정으로 들어온 요청만 승인할 수 있습니다."
            );
        }

        // 이미 처리된 요청이면 400
        if (req.getStatus() != FollowRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미 처리된 요청입니다."
            );
        }

        followRepo.save(new UserFollow(null, req.getRequester(), req.getTarget()));
        req.setStatus(FollowRequestStatus.ACCEPTED);
    }

    /**
     * 4) 비공개 계정 주인이 거절
     */
    @Transactional
    public void rejectFollowRequest(Long me, Long requestId) {
        FollowRequest req = reqRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 요청입니다."));

        if (!req.getTarget().getId().equals(me)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "내 계정으로 들어온 요청만 거절할 수 있습니다."
            );
        }

        if (req.getStatus() != FollowRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미 처리된 요청입니다."
            );
        }

        req.setStatus(FollowRequestStatus.REJECTED);
    }

    @Transactional
    public void unfollow(Long meId, Long targetId) {
        // 1. 실제 팔로우 관계가 있다면 삭제를 시도합니다.
        followRepo.deleteByFollowerId_IdAndFolloweeId_Id(meId, targetId);

        // 2. PENDING 상태의 팔로우 요청이 있다면 함께 삭제(취소)합니다.
        reqRepo.deleteByRequester_IdAndTarget_IdAndStatus(meId, targetId, FollowRequestStatus.PENDING);
    }
    // 내 프로필 조회
    public UserProfileDto getMyProfile(Long userId) {
        User u = userRepo.findById(userId).orElseThrow();
        long followerCnt = followRepo.countByFolloweeId_Id(userId);
        long followingCnt = followRepo.countByFollowerId_Id(userId);
        long recordCnt = recordRepo.countByUserId(userId);
        return new UserProfileDto(
                u.getId(), u.getNickname(), u.getProfileImageUrl(),
                u.getFavTeam(), u.getIsPrivate(),
                followerCnt, followingCnt, recordCnt
        );
    }

    // 내 정보 수정
    @Transactional
    public void updateUser(Long userId, UpdateUserRequest req) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 닉네임 중복 확인 (닉네임이 변경된 경우만)
        if (!u.getNickname().equals(req.getNickname())
                && userRepo.existsByNickname(req.getNickname())) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
        }

        u.setNickname(req.getNickname());
        u.setFavTeam(req.getFavTeam());
        u.setProfileImageUrl(req.getProfileImageUrl() != null && !req.getProfileImageUrl().trim().isEmpty()
                ? req.getProfileImageUrl().trim() : null);

        if (req.getIsPrivate() != null) {
            u.setIsPrivate(req.getIsPrivate());
        }
    }

    // 로그아웃
    public void logout(Long userId) { /* JWT 토큰 무효화 로직 (필요시) */ }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // ✅ List<Record> -> List<GameRecord>로 타입 변경
        List<GameRecord> userRecords = recordRepo.findByUserId(userId);

        userRecords.forEach(record -> {
            if (record.getMediaUrls() != null) {
                record.getMediaUrls().forEach(s3Service::deleteFile);
            }
        });

        s3Service.deleteFile(user.getProfileImageUrl());

        recordReactionRepo.deleteAllByUserId(userId);
        recordRepo.deleteAll(userRecords); // ✅ deleteAll(List<GameRecord>) 호출
        reqRepo.deleteAllByRequesterIdOrTargetId(userId);
        userBlockRepo.deleteAllByBlockerIdOrBlockedId(userId);
        followRepo.deleteAllByFollowerIdOrFolloweeId(userId);

        userRepo.delete(user);
    }

    // ✅ [신규] 누락되었던 isNicknameAvailable 메서드를 다시 추가합니다.
    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.trim().isEmpty() || nickname.length() > 15) {
            return false;
        }
        return !userRepo.existsByNickname(nickname);
    }

    @Transactional
    public void blockUser(Long blockerId, Long targetId) {
        if (blockerId.equals(targetId)) throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다");
        User target = userRepo.findById(targetId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
        User blocker = userRepo.findById(blockerId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
        if (userBlockRepo.existsByBlocker_IdAndBlocked_Id(blockerId, targetId)) throw new IllegalArgumentException("이미 차단된 사용자입니다");

        userBlockRepo.save(UserBlock.builder().blocker(blocker).blocked(target).build());

        // 차단 시 관련 데이터 정리
        followRepo.deleteByFollowerId_IdAndFolloweeId_Id(blockerId, targetId);
        followRepo.deleteByFollowerId_IdAndFolloweeId_Id(targetId, blockerId);
        reqRepo.deleteByBothUsers(blockerId, targetId);
        recordReactionRepo.deleteByBothUsers(blockerId, targetId);
    }

    @Transactional
    public void unblockUser(Long blockerId, Long targetId) {
        if (!userBlockRepo.existsByBlocker_IdAndBlocked_Id(blockerId, targetId)) {
            throw new IllegalArgumentException("차단되지 않은 사용자입니다");
        }
        userBlockRepo.deleteByBlocker_IdAndBlocked_Id(blockerId, targetId);
    }

    @Transactional(readOnly = true)
    public List<BlockedUserDto> getBlockedUsers(Long blockerId) {
        return userBlockRepo.findByBlocker_IdOrderByCreatedAtDesc(blockerId)
                .stream()
                .map(userBlock -> new BlockedUserDto(
                        userBlock.getBlocked().getId(),
                        userBlock.getBlocked().getNickname(),
                        userBlock.getBlocked().getProfileImageUrl(),
                        userBlock.getBlocked().getFavTeam(),
                        userBlock.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public boolean isBlockedEachOther(Long userId1, Long userId2) {
        return userBlockRepo.existsByBlocker_IdAndBlocked_Id(userId1, userId2) ||
                userBlockRepo.existsByBlocker_IdAndBlocked_Id(userId2, userId1);
    }
}
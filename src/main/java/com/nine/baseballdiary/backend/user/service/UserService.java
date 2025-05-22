package com.nine.baseballdiary.backend.user.service;

import com.nine.baseballdiary.backend.record.RecordRepository;
import com.nine.baseballdiary.backend.user.dto.*;
import com.nine.baseballdiary.backend.user.entity.FollowRequest;
import com.nine.baseballdiary.backend.user.entity.FollowRequestStatus;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.entity.UserFollow;
import com.nine.baseballdiary.backend.user.repository.FollowRequestRepository;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final UserFollowRepository followRepo;
    private final RecordRepository      recordRepo;
    private final FollowRequestRepository reqRepo;

    // 친구 검색
    public List<UserDto> searchUsers(String q) {
        return userRepo.findByNicknameContainingIgnoreCase(q).stream()
                .map(u -> new UserDto(u.getId(), u.getNickname(), u.getProfileImageUrl()))
                .toList();
    }

    /** 1) 팔로우 요청 (공개면 즉시, 비공개면 PENDING 생성) */
    @Transactional
    public FollowResponse requestFollow(Long meId, Long targetId) {
        User me     = userRepo.findById(meId).orElseThrow();
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
            return new FollowResponse(true, true, req.getId());
        } else {
            // 공개 계정: 즉시 팔로우
            followRepo.save(new UserFollow(null, me, target));
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

    /** 3) 비공개 계정 주인이 수락 */
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

    /** 4) 비공개 계정 주인이 거절 */
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

    /** 언팔로우 */
    @Transactional
    public void unfollow(Long meId, Long targetId) {
        followRepo.deleteByFollowerId_IdAndFolloweeId_Id(meId, targetId);
    }

    /** 내가 팔로잉하는 사람 목록 */
    @Transactional(readOnly = true)
    public List<UserDto> getFollowing(Long userId) {
        return followRepo.findByFollowerId_Id(userId).stream()
                .map(uf -> uf.getFolloweeId())                      // 엔티티 꺼내고
                .map(u  -> new UserDto(u.getId(), u.getNickname(), u.getProfileImageUrl()))
                .collect(Collectors.toList());
    }

    /** 나를 팔로잉하는 사람 목록 */
    @Transactional(readOnly = true)
    public List<UserDto> getFollowers(Long userId) {
        return followRepo.findByFolloweeId_Id(userId).stream()
                .map(uf -> uf.getFollowerId())
                .map(u  -> new UserDto(u.getId(), u.getNickname(), u.getProfileImageUrl()))
                .collect(Collectors.toList());
    }

    // 내 프로필 조회
    public UserProfileDto getMyProfile(Long userId) {
        User u = userRepo.findById(userId).orElseThrow();
        long followerCnt  = followRepo.findByFolloweeId_Id(userId).size();
        long followingCnt = followRepo.findByFollowerId_Id(userId).size();
        long recordCnt    = recordRepo.countByUserId(userId);
        return new UserProfileDto(
                u.getId(), u.getNickname(), u.getProfileImageUrl(),
                u.getFavTeam(), u.getIsPrivate(),
                followerCnt, followingCnt, recordCnt
        );
    }

    // 내 정보 수정
    @Transactional
    public void updateUser(Long userId, UpdateUserRequest req) {
        User u = userRepo.findById(userId).orElseThrow();
        if (!u.getNickname().equals(req.getNickname())
                && userRepo.existsByNickname(req.getNickname())) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
        }
        u.setNickname(req.getNickname());
        u.setProfileImageUrl(req.getProfileImageUrl());
        u.setFavTeam(req.getFavTeam());
        u.setIsPrivate(req.getIsPrivate());
        // 변경감지로 자동 업데이트
    }

    // 로그아웃: 경우에 따라 토큰 무효화 로직 추가
    public void logout(Long userId) { /* no-op or invalidate JWT */ }

    // 회원탈퇴
    @Transactional
    public void deleteUser(Long userId) {
        userRepo.deleteById(userId);
    }

}


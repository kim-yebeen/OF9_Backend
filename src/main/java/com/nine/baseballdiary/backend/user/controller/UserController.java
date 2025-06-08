package com.nine.baseballdiary.backend.user.controller;

import com.nine.baseballdiary.backend.user.dto.*;
import com.nine.baseballdiary.backend.user.entity.FollowRequest;
import com.nine.baseballdiary.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController @RequestMapping("/users") @RequiredArgsConstructor
public class UserController {
    private final UserService svc;

    //jwt 토큰에서 현재 사용자 id를 가져오는 헬퍼 메서드
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자가 아닙니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }

    //내 정보 조회
    @GetMapping("/me")
    public UserProfileDto me() {
        Long userId = getCurrentUserId();
        return svc.getMyProfile(userId);
    }

    //내 정보 수정
    @PatchMapping("/me")
    public ResponseEntity<UserProfileDto> updateMe(
            @RequestBody @Valid UpdateUserRequest req) {
        Long userId = getCurrentUserId();
        svc.updateUser(userId, req);
        UserProfileDto updated = svc.getMyProfile(userId);
        return ResponseEntity.ok(updated);
    }

    //친구 검색
    @GetMapping("/search")
    public List<UserDto> search(
            @RequestParam("nickname") String nickname
    ) {
        return svc.searchUsers(nickname);
    }

    //follow request
    @PostMapping("/{targetId}/follow")
    public ResponseEntity<FollowResponse> follow(
            @PathVariable Long targetId
    ) {
        Long me = getCurrentUserId();
        FollowResponse res = svc.requestFollow(me, targetId);
        HttpStatus status = res.isPending() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(res);
    }

    /** 2) 비공개 계정 주인의 PENDING 요청 조회 */
    @GetMapping("/me/requests")
    public List<FollowRequestDto> incomingRequests() {
        Long me = getCurrentUserId();
        return svc.listIncomingRequests(me);
    }

    /** 3) 비공개 계정 주인의 수락 */
    @PatchMapping("/me/requests/{reqId}/accept")
    public ResponseEntity<Void> accept(
            @PathVariable Long reqId
    ) {
        Long me = getCurrentUserId();
        svc.acceptFollowRequest(me, reqId);
        return ResponseEntity.ok().build();
    }

    /** 4) 비공개 계정 주인의 거절 */
    @PatchMapping("/me/requests/{reqId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long reqId
    ) {
        Long me = getCurrentUserId();
        svc.rejectFollowRequest(me, reqId);
        return ResponseEntity.noContent().build();
    }

    // 언팔로우
    @DeleteMapping("/{targetId}/follow")
    public ResponseEntity<Void> unfollow(
            @PathVariable Long targetId
    ) {
        Long me = getCurrentUserId();
        svc.unfollow(me, targetId);
        return ResponseEntity.noContent().build();
    }

    //특정 사용자의 팔로잉 목록
    @GetMapping("/{userId}/following")
    public List<UserDto> following(@PathVariable Long userId) {
        return svc.getFollowing(userId);
    }

    //특정사용자의 팔로워 목록
    @GetMapping("/{userId}/followers")
    public List<UserDto> followers(@PathVariable Long userId) {
        return svc.getFollowers(userId);
    }

    //logout
    @PostMapping("/me/logout")
    public ResponseEntity<Void> logout() {
        Long userId = getCurrentUserId();
        svc.logout(userId);
        return ResponseEntity.noContent().build();
    }

    //withdraw
    @DeleteMapping("/me")
    public ResponseEntity<Void> delete() {
        Long userId = getCurrentUserId();
        svc.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }


}

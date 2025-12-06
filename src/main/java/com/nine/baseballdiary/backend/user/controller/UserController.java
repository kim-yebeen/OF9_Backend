package com.nine.baseballdiary.backend.user.controller;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.user.dto.*;
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
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // JWT 토큰에서 현재 사용자 ID 추출
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자가 아닙니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }

    // ✅ 1. 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getMyProfile() {
        Long userId = getCurrentUserId();
        UserProfileDto profile = userService.getMyProfile(userId);

        return ResponseEntity.ok(ApiResponse.success("프로필을 조회했습니다", profile));
    }

    // ✅ 2. 내 정보 수정
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateMyProfile(@Valid @RequestBody UpdateUserRequest request) {
        Long userId = getCurrentUserId();
        userService.updateUser(userId, request);

        UserProfileDto updatedProfile = userService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다", updatedProfile));
    }

    // ✅ 3. 닉네임 중복 확인
    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkNickname(@RequestParam String nickname) {
        boolean isAvailable = userService.isNicknameAvailable(nickname);
        String message = isAvailable ? "사용 가능한 닉네임입니다" : "이미 사용중인 닉네임입니다";

        Map<String, Object> result = Map.of(
                "available", isAvailable,
                "message", message
        );

        return ResponseEntity.ok(ApiResponse.success("닉네임 중복 확인 완료", result));
    }

    // ✅ 4. 사용자 검색 (수정)
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserDto>>> searchUsers(@RequestParam String nickname) {
        Long currentUserId = getCurrentUserId();
        List<UserDto> users = userService.searchUsers(currentUserId, nickname);

        return ResponseEntity.ok(ApiResponse.success("사용자 검색이 완료되었습니다", users));
    }

    // ✅ 5. 팔로우 요청/즉시 팔로우
    @PostMapping("/{targetId}/follow")
    public ResponseEntity<ApiResponse<FollowResponse>> followUser(@PathVariable Long targetId) {
        Long currentUserId = getCurrentUserId();
        FollowResponse response = userService.requestFollow(currentUserId, targetId);

        String message = response.isPending() ?
                "팔로우 요청을 보냈습니다" :
                "팔로우했습니다";

        HttpStatus status = response.isPending() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.success(message, response));
    }

    // ✅ 6. 언팔로우
    @DeleteMapping("/{targetId}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(@PathVariable Long targetId) {
        Long currentUserId = getCurrentUserId();
        userService.unfollow(currentUserId, targetId);

        return ResponseEntity.ok(ApiResponse.success("언팔로우했습니다"));
    }

    // ✅ 7. 받은 팔로우 요청 목록 조회
    @GetMapping("/me/follow-requests")
    public ResponseEntity<ApiResponse<List<FollowRequestDto>>> getFollowRequests() {
        Long currentUserId = getCurrentUserId();
        List<FollowRequestDto> requests = userService.listIncomingRequests(currentUserId);

        return ResponseEntity.ok(ApiResponse.success("팔로우 요청 목록을 조회했습니다", requests));
    }

    // ✅ 8. 팔로우 요청 수락
    @PostMapping("/me/follow-requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptFollowRequest(@PathVariable Long requestId) {
        Long currentUserId = getCurrentUserId();
        userService.acceptFollowRequest(currentUserId, requestId);

        return ResponseEntity.ok(ApiResponse.success("팔로우 요청을 수락했습니다"));
    }

    // ✅ 9. 팔로우 요청 거절
    @PostMapping("/me/follow-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectFollowRequest(@PathVariable Long requestId) {
        Long currentUserId = getCurrentUserId();
        userService.rejectFollowRequest(currentUserId, requestId);

        return ResponseEntity.ok(ApiResponse.success("팔로우 요청을 거절했습니다"));
    }

    // ✅ 10. 특정 사용자의 팔로워 목록
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<List<UserDto>>> getFollowers(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        List<UserDto> followers = userService.getFollowers(userId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("팔로워 목록을 조회했습니다", followers));
    }

    // ✅ 11. 특정 사용자의 팔로잉 목록
    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<List<UserDto>>> getFollowing(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        List<UserDto> following = userService.getFollowing(userId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("팔로잉 목록을 조회했습니다", following));
    }


    

    // ✅ 13. 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        Long userId = getCurrentUserId();
        userService.deleteUser(userId);

        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다"));
    }
    // ✅ 14. 사용자 차단
    @PostMapping("/{targetId}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long targetId) {
        Long currentUserId = getCurrentUserId();
        userService.blockUser(currentUserId, targetId);

        return ResponseEntity.ok(ApiResponse.success("사용자를 차단했습니다"));
    }

    // ✅ 15. 차단 해제
    @DeleteMapping("/{targetId}/block")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long targetId) {
        Long currentUserId = getCurrentUserId();
        userService.unblockUser(currentUserId, targetId);

        return ResponseEntity.ok(ApiResponse.success("차단을 해제했습니다"));
    }

    // ✅ 16. 차단된 사용자 목록 조회
    @GetMapping("/me/blocked")
    public ResponseEntity<ApiResponse<List<BlockedUserDto>>> getBlockedUsers() {
        Long currentUserId = getCurrentUserId();
        List<BlockedUserDto> blockedUsers = userService.getBlockedUsers(currentUserId);

        return ResponseEntity.ok(ApiResponse.success("차단된 사용자 목록을 조회했습니다", blockedUsers));
    }
}
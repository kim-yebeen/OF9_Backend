package com.nine.baseballdiary.backend.user.controller;

import com.nine.baseballdiary.backend.user.dto.*;
import com.nine.baseballdiary.backend.user.entity.FollowRequest;
import com.nine.baseballdiary.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/users") @RequiredArgsConstructor
public class UserController {
    private final UserService svc;

    //내 정보 조회 - 헤더에 유저아이디값담아서
    @GetMapping("/me")
    public UserProfileDto me(@RequestHeader("userId") Long userId) {
        return svc.getMyProfile(userId);
    }

    //내 정보 수정 - 헤더에 유저아이디값담아서
    @PatchMapping("/me")
    public ResponseEntity<UserProfileDto> updateMe(
            @RequestHeader("userId") Long userId,
            @RequestBody @Valid UpdateUserRequest req) {
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

    /**
     * 1) 팔로우 요청
     *    공개 계정 → 200 OK
     *    비공개 계정 → 202 Accepted (PENDING)
     */
    @PostMapping("/{targetId}/follow")
    public ResponseEntity<FollowResponse> follow(
            @RequestHeader("userId") Long me,
            @PathVariable Long targetId
    ) {
        FollowResponse res = svc.requestFollow(me, targetId);
        HttpStatus status = res.isPending() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(res);
    }

    /** 2) 비공개 계정 주인의 PENDING 요청 조회 */
    @GetMapping("/me/requests")
    public List<FollowRequestDto> incomingRequests(
            @RequestHeader("userId") Long me
    ) {
        return svc.listIncomingRequests(me);
    }

    /** 3) 비공개 계정 주인의 수락 */
    @PatchMapping("/me/requests/{reqId}/accept")
    public ResponseEntity<Void> accept(
            @RequestHeader("userId") Long me,
            @PathVariable Long reqId
    ) {
        svc.acceptFollowRequest(me, reqId);
        return ResponseEntity.ok().build();
    }

    /** 4) 비공개 계정 주인의 거절 */
    @PatchMapping("/me/requests/{reqId}/reject")
    public ResponseEntity<Void> reject(
            @RequestHeader("userId") Long me,
            @PathVariable Long reqId
    ) {
        svc.rejectFollowRequest(me, reqId);
        return ResponseEntity.noContent().build();
    }

    // 언팔로우
    @DeleteMapping("/{targetId}/follow")
    public ResponseEntity<Void> unfollow(
            @RequestHeader("userId") Long me,
            @PathVariable Long targetId
    ) {
        svc.unfollow(me, targetId);
        return ResponseEntity.noContent().build();
    }

    //내 팔로잉 목록
    @GetMapping("/{userId}/following")
    public List<UserDto> following(@PathVariable Long userId) {
        return svc.getFollowing(userId);
    }

    //내 팔로워 목록
    @GetMapping("/{userId}/followers")
    public List<UserDto> followers(@PathVariable Long userId) {
        return svc.getFollowers(userId);
    }

    //logout
    @PostMapping("/me/logout")
    public ResponseEntity<Void> logout(@RequestHeader("userId") Long userId) {
        svc.logout(userId);
        return ResponseEntity.noContent().build();
    }

    //withdraw
    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(@RequestHeader("userId") Long userId) {
        svc.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }


}

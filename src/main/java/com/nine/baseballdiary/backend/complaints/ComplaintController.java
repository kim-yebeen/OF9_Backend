package com.nine.baseballdiary.backend.complaints;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.complaints.CreateComplaintRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자가 아닙니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }

    /**
     * 신고하기
     * - 사용자 또는 게시글을 신고할 수 있음
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createComplaint(@RequestBody CreateComplaintRequest request) {
        Long reporterId = getCurrentUserId();
        complaintService.createComplaint(reporterId, request);

        return ResponseEntity.ok(ApiResponse.success("신고가 접수되었습니다"));
    }
}
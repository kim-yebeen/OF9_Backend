package com.nine.baseballdiary.backend.S3;

import com.nine.baseballdiary.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final S3Service s3Service;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong((String) authentication.getPrincipal());
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(@RequestBody PresignedUrlRequest request) {
        Long userId = getCurrentUserId();
        PresignedUrlResponse response = s3Service.generatePresignedUrl(userId, request.getDomain(), request.getFileName());
        return ResponseEntity.ok(ApiResponse.success("업로드 URL이 발급되었습니다", response));
    }
}
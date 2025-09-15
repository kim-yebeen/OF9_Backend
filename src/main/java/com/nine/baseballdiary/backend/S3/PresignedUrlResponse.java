package com.nine.baseballdiary.backend.S3;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

@RequiredArgsConstructor
@Getter
class PresignedUrlResponse {
    private final String presignedUrl; // 파일 업로드용 임시 URL
    private final String finalUrl;     // DB 저장용 최종 URL
}
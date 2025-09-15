package com.nine.baseballdiary.backend.S3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
class PresignedUrlRequest {
    private final String domain; // "profiles" 또는 "records"
    private final String fileName;
}
package com.nine.baseballdiary.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationInMs;

    // 키(Key) 생성 규칙: "RT:userId" 형태
    private String getKey(String userId) {
        return "RT:" + userId;
    }

    /**
     * Redis에 Refresh Token 저장
     * key: RT:{userId}
     * value: {refreshToken}
     * ttl: 리프레시 토큰의 만료 시간과 동일하게 설정 (자동 삭제됨)
     */
    public void saveRefreshToken(String userId, String refreshToken) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        values.set(getKey(userId), refreshToken, Duration.ofMillis(refreshExpirationInMs));
    }

    /**
     * Redis에서 Refresh Token 조회
     */
    public String getRefreshToken(String userId) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        return values.get(getKey(userId));
    }

    /**
     * 로그아웃/재발급 시 기존 Token 삭제
     */
    public void deleteRefreshToken(String userId) {
        redisTemplate.delete(getKey(userId));
    }
}
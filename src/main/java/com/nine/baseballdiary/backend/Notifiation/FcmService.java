package com.nine.baseballdiary.backend.Notifiation;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(String token, String title, String body, String type, String targetId) {
        // 👇 [수정] 토큰이 없을 때 로그를 찍도록 변경
        if (token == null || token.isEmpty()) {
            log.warn("🚫 [FCM 전송 실패] 사용자에게 FCM 토큰이 없습니다. (Target ID: {})", targetId);
            return;
        }

        log.info("🚀 [FCM 전송 시도] 제목: {}, 토큰(일부): {}...", title, token.substring(0, Math.min(token.length(), 10)));

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putData("type", type)
                .putData("targetId", targetId != null ? targetId : "")
                .build();

        try {
            String response = firebaseMessaging.send(message);
            log.info("✅ [FCM 전송 성공] Response: {}", response);
        } catch (Exception e) {
            log.error("❌ [FCM 전송 에러] ", e);
        }
    }
}
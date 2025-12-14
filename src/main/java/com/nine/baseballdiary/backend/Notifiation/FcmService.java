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
        if (token == null || token.isEmpty()) {
            log.info("FCM Token is empty, skip sending push.");
            return;
        }

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        // 프론트엔드 라우팅을 위한 데이터
        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putData("type", type) // 알림 타입 (LIKE, COMMENT 등)
                .putData("targetId", targetId != null ? targetId : "") // 이동할 ID (게시글 ID 등)
                .build();

        try {
            String response = firebaseMessaging.send(message);
            log.info("FCM Push Sent: " + response);
        } catch (Exception e) {
            log.error("FCM Push Failed: ", e);
        }
    }
}
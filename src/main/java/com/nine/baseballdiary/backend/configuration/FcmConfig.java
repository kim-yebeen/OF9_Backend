package com.nine.baseballdiary.backend.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FcmConfig {
    @Bean
    FirebaseMessaging firebaseMessaging() throws IOException {
        ClassPathResource resource = new ClassPathResource("firebase-service-account.json");

        // 파일이 없을 경우 예외 처리나 로그를 추가하는 것이 좋습니다.
        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        // 중복 초기화 방지
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }

        return FirebaseMessaging.getInstance();
    }
}
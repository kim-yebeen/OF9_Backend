package com.nine.baseballdiary.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // 모든 요청 허용 (개발 단계용)
                )
                .csrf(csrf -> csrf.disable())  // CSRF 비활성화
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.deny())); // 최신 방식

        return http.build();
    }
}
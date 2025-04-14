package com.nine.baseballdiary.backend.user;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // TODO: 마이페이지 조회 및 수정 로직 구현 예정
}

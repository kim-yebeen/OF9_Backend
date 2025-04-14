package com.nine.baseballdiary.backend.user;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 📝 닉네임/소개/응원팀 수정 예정
    @PutMapping("/me")
    public String updateMyProfile(@RequestBody Object request) {
        // TODO: 프로필 수정 로직 작성 예정
        return "사용자 프로필 수정은 아직 구현되지 않았습니다.";
    }

    // 📝 마이페이지 조회 예정
    @GetMapping("/me")
    public String getMyProfile() {
        // TODO: 마이페이지 조회 로직 작성 예정
        return "마이페이지 조회는 아직 구현되지 않았습니다.";
    }
}

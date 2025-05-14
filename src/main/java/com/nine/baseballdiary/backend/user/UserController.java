package com.nine.baseballdiary.backend.user;

import com.nine.baseballdiary.backend.record.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RecordService recordService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUserInfo(@RequestParam Long userId) {
        UserResponse userInfo = userService.getUserInfo(userId);
        return ResponseEntity.ok(userInfo);
    }

    // 내 직관 기록 조회
    @GetMapping("/me/records")
    public ResponseEntity<?> getUserRecords(@RequestParam Long userId, @RequestParam String format) {
        switch (format.toLowerCase()) {
            case "feed":
                return ResponseEntity.ok(recordService.getUserRecordsFeed(userId));
            case "list":
                return ResponseEntity.ok(recordService.getUserRecordsList(userId));
            case "calendar":
                return ResponseEntity.ok(recordService.getUserRecordsCalendar(userId));
            default:
                return ResponseEntity.badRequest().body("Invalid format");
        }
    }
}
package com.nine.baseballdiary.backend.stadium;

import com.nine.baseballdiary.backend.stadium.StadiumSeatResponse;
import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.stadium.StadiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/common/stadium")
@RequiredArgsConstructor
public class StadiumController {

    private final StadiumService stadiumService;

    // 구장 이름을 받아서 좌석 구역 목록을 반환
    @GetMapping("/seats")
    public ResponseEntity<ApiResponse<StadiumSeatResponse>> getStadiumSeats(@RequestParam String name) {
        StadiumSeatResponse response = stadiumService.getStadiumSeats(name);
        return ResponseEntity.ok(ApiResponse.success("좌석 구역 정보를 조회했습니다.", response));
    }
}
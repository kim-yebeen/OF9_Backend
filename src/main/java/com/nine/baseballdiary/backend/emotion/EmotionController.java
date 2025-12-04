package com.nine.baseballdiary.backend.emotion;

import com.nine.baseballdiary.backend.emotion.EmotionResponse;
import com.nine.baseballdiary.backend.emotion.EmotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emotions")
@RequiredArgsConstructor
public class EmotionController {

    private final EmotionService emotionService;

    /**
     * 카테고리별 감정 목록 조회
     *
     * GET /api/emotions?category=전체
     * GET /api/emotions?category=승리
     * GET /api/emotions?category=무승부
     * GET /api/emotions?category=패배
     *
     * @param category 카테고리 ("전체", "승리", "무승부", "패배")
     * @return 해당 카테고리의 감정 목록
     */
    @GetMapping
    public ResponseEntity<List<EmotionResponse>> getEmotions(
            @RequestParam(name = "category", defaultValue = "전체") String category) {

        List<EmotionResponse> emotions = emotionService.getEmotionsByCategory(category);
        return ResponseEntity.ok(emotions);
    }
}
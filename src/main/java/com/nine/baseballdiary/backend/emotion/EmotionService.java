package com.nine.baseballdiary.backend.emotion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionService {

    private final EmotionCategoryRepository emotionCategoryRepository;

    /**
     * 카테고리별 감정 목록 조회
     * @param category "전체", "승리", "무승부", "패배"
     */
    public List<EmotionResponse> getEmotionsByCategory(String category) {
        // 카테고리 검증
        if (!isValidCategory(category)) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + category);
        }

        List<EmotionCategory> emotions = emotionCategoryRepository
                .findByCategoryOrderByDisplayOrder(category);

        return emotions.stream()
                .map(ec -> EmotionResponse.builder()
                        .code(ec.getEmotion().getCode())
                        .label(ec.getEmotion().getLabel())
                        .displayOrder(ec.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 감정 코드로 레이블 조회
     */
    public String getEmotionLabel(Integer emotionCode) {
        if (emotionCode == null) {
            return "알 수 없음";
        }

        switch (emotionCode) {
            case 1: return "행복해요";
            case 2: return "놀랐어요";
            case 3: return "짜릿해요";
            case 4: return "벅차요";
            case 5: return "통쾌해요";
            case 6: return "만족해요";
            case 7: return "지루해요";
            case 8: return "무난해요";
            case 9: return "긴장돼요";
            case 10: return "질투나요";
            case 11: return "답답해요";
            case 12: return "아쉬워요";
            case 13: return "지쳤어요";
            case 14: return "허탈해요";
            case 15: return "짜증나요";
            case 16: return "화나요";
            default: return "알 수 없음";
        }
    }

    private boolean isValidCategory(String category) {
        return category != null &&
                (category.equals("전체") ||
                        category.equals("승리") ||
                        category.equals("무승부") ||
                        category.equals("패배"));
    }
}

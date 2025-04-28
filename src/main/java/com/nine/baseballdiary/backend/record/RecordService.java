package com.nine.baseballdiary.backend.record;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository repo;

    /**
     * 1) Draft 생성 (OCR 결과를 클라이언트가 보내온 그대로 저장)
     */
    @Transactional
    public RecordResponse createDraft(Integer userId, CreateDraftRequest req) {
        Record r = Record.builder()
                .userId(userId)
                .gameId(req.getGameId())
                .gameDate(LocalDate.parse(req.getGameDate()))
                .homeTeam(req.getHomeTeam())
                .awayTeam(req.getAwayTeam())
                .startTime(LocalTime.parse(req.getStartTime()))
                .seatInfo(req.getSeatInfo())
                .status("DRAFT")
                .build();

        r = repo.save(r);
        return toResponse(r);
    }

    /**
     * 2) Draft 조회
     */
    public RecordResponse getRecord(Integer id) {
        return repo.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 recordId: " + id));
    }

    /**
     * 3) Draft 보정 (optional 필드만 업데이트)
     */
    @Transactional
    public RecordResponse updateDraft(Integer id, UpdateDraftRequest req) {
        Record r = repo.findById(id).orElseThrow();
        if (req.getGameDate()   != null) r.setGameDate(LocalDate.parse(req.getGameDate()));
        if (req.getHomeTeam()   != null) r.setHomeTeam(req.getHomeTeam());
        if (req.getAwayTeam()   != null) r.setAwayTeam(req.getAwayTeam());
        if (req.getStartTime()  != null) r.setStartTime(LocalTime.parse(req.getStartTime()));
        if (req.getSeatInfo()   != null) r.setSeatInfo(req.getSeatInfo());

        r = repo.save(r);
        return toResponse(r);
    }

    /**
     * 4) 세부 입력 저장 → emotionEmoji 없으면 예외, status=COMPLETED
     */
    @Transactional
    public RecordResponse completeDetail(Integer id, CreateDetailRequest req) {
        if (req.getEmotionEmoji() == null) {
            throw new IllegalArgumentException("감정 이모지는 반드시 선택해야 합니다.");
        }
        Record r = repo.findById(id).orElseThrow();
        r.setEmotionEmoji(req.getEmotionEmoji());
        r.setComment(req.getComment());
        r.setBestPlayer(req.getBestPlayer());
        r.setFoodTags(req.getFoodTags());
        r.setMediaUrls(req.getMediaUrls());
        r.setResult(req.getResult());
        r.setStatus("COMPLETED");

        r = repo.save(r);
        return toResponse(r);
    }

    private RecordResponse toResponse(Record r) {
        return new RecordResponse(
                r.getRecordId(),
                r.getGameId(),
                r.getGameDate().toString(),
                r.getHomeTeam(),
                r.getAwayTeam(),
                r.getStartTime().toString(),
                r.getSeatInfo() == null ? "" : r.getSeatInfo(),
                r.getEmotionEmoji(),
                r.getComment()    == null ? "" : r.getComment(),
                r.getBestPlayer() == null ? "" : r.getBestPlayer(),
                r.getFoodTags(),
                r.getMediaUrls(),
                r.getResult()     == null ? "" : r.getResult(),
                r.getStatus(),
                r.getCreatedAt().toString(),      // String
                r.getUpdatedAt().toString()       // String
        );
    }
}

package com.nine.baseballdiary.backend.record;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class RecordService {

    private final RecordRepository repo;

    @Value("${app.upload.dir}") // application.properties에 설정 필요
    private String uploadDir;

    public RecordService(RecordRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public UploadRecordResponse uploadTicket(Integer userId, MultipartFile file) throws IOException {
        // 1) 파일 저장
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path target = Paths.get(uploadDir).resolve(filename);
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());
        String imageUrl = "/uploads/" + filename; // 실제 URL 매핑 필요

        // 2) OCR 처리

        // 3) DB 저장 OCR 추출값이 준비되면 값을 set,없으면 null
        Record r = Record.builder()
                .userId(userId)
                .gameId(null)              // 게임아이디도 OCR 후 생성 or UpdateDraft 에서 생성
                .ticketImageUrl(imageUrl)
                .gameDate(null)
                .homeTeam(null)
                .awayTeam(null)
                .startTime(null)
                .seatInfo(null)
                .build();

        r = repo.save(r);

        return new UploadRecordResponse(
                r.getRecordId(),
                r.getTicketImageUrl(),
                r.getGameDate(),
                r.getHomeTeam(),
                r.getAwayTeam(),
                r.getStartTime(),
                r.getSeatInfo()
        );
    }

    public RecordResponse getRecord(Integer id) {
        Record r = repo.findById(id).orElseThrow();
        return new RecordResponse(
                r.getRecordId(),
                r.getTicketImageUrl(),
                r.getGameDate(), r.getHomeTeam(), r.getAwayTeam(),
                r.getStartTime(), r.getSeatInfo(),
                r.getEmotionEmoji(), r.getComment(), r.getBestPlayer(),
                r.getFoodTags(), r.getMediaUrls(),
                r.getResult(), r.getStatus(),
                r.getCreatedAt(), r.getUpdatedAt()
        );
    }

    @Transactional
    public RecordResponse updateDraft(Integer id, UpdateDraftRequest req) {
        Record r = repo.findById(id).orElseThrow();
        Optional.ofNullable(req.getGameDate()).ifPresent(r::setGameDate);
        Optional.ofNullable(req.getStartTime()).ifPresent(r::setStartTime);
        Optional.ofNullable(req.getSeatInfo()).ifPresent(r::setSeatInfo);
        // status stays DRAFT
        r = repo.save(r);
        return getRecord(id);
    }

    @Transactional
    public RecordResponse completeDetail(Integer id, CreateDetailRequest req) {
        if (req.getEmotionEmoji() == null) {
            throw new IllegalArgumentException("감정 이모지는 반드시 선택해야 합니다.");
        }
        Record r = repo.findById(id).orElseThrow();

        r.setEmotionEmoji(req.getEmotionEmoji());
        Optional.ofNullable(req.getComment()).ifPresent(r::setComment);
        Optional.ofNullable(req.getBestPlayer()).ifPresent(r::setBestPlayer);
        Optional.ofNullable(req.getFoodTags()).ifPresent(r::setFoodTags);
        Optional.ofNullable(req.getMediaUrls()).ifPresent(r::setMediaUrls);
        Optional.ofNullable(req.getResult()).ifPresent(r::setResult);

        r.setStatus("COMPLETED");
        r = repo.save(r);
        return new RecordResponse(r.getRecordId(), null, null, null, null, null, null,
                r.getEmotionEmoji(), r.getComment(), r.getBestPlayer(),
                r.getFoodTags(), r.getMediaUrls(), r.getResult(),
                r.getStatus(), r.getCreatedAt(), r.getUpdatedAt());
    }
}

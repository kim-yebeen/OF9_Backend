package com.nine.baseballdiary.backend.complaints;

import com.nine.baseballdiary.backend.complaints.CreateComplaintRequest;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepo;
    private final UserRepository userRepo;
    private final GameRecordRepository recordRepo;

    @Transactional
    public void createComplaint(Long reporterId, CreateComplaintRequest request) {
        // 1. 최소 하나는 신고 대상이어야 함
        if (request.getReportedUserId() == null && request.getReportedRecordId() == null) {
            throw new IllegalArgumentException("신고 대상을 선택해주세요");
        }

        // 2. 중복 신고 확인 (같은 날 같은 대상 신고 방지)
        if (complaintRepo.existsTodayComplaint(
                reporterId,
                request.getReportedUserId(),
                request.getReportedRecordId())) {
            throw new IllegalArgumentException("이미 신고한 내용입니다");
        }

        // 3. 자기 자신 신고 방지
        if (request.getReportedUserId() != null
                && request.getReportedUserId().equals(reporterId)) {
            throw new IllegalArgumentException("자기 자신을 신고할 수 없습니다");
        }

        // 4. 신고자 조회
        User reporter = userRepo.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 5. 신고 대상 조회
        User reportedUser = null;
        GameRecord reportedRecord = null;

        if (request.getReportedUserId() != null) {
            reportedUser = userRepo.findById(request.getReportedUserId())
                    .orElseThrow(() -> new IllegalArgumentException("신고 대상 사용자를 찾을 수 없습니다"));
        }

        if (request.getReportedRecordId() != null) {
            reportedRecord = recordRepo.findById(request.getReportedRecordId())
                    .orElseThrow(() -> new IllegalArgumentException("신고 대상 게시글을 찾을 수 없습니다"));
        }

        // 6. 신고 생성
        Complaint complaint = Complaint.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportedRecord(reportedRecord)
                .build();

        complaintRepo.save(complaint);

        log.info("신고 접수 - 신고자: {}, 대상 사용자: {}, 게시글: {}",
                reporterId,
                request.getReportedUserId(),
                request.getReportedRecordId());
    }

    // 관리자용: 신고 통계 조회
    @Transactional(readOnly = true)
    public long getComplaintCountByUserId(Long userId) {
        return complaintRepo.countByReportedUserId(userId);
    }

    @Transactional(readOnly = true)
    public long getComplaintCountByRecordId(Long recordId) {
        return complaintRepo.countByReportedRecordId(recordId);
    }
}
package com.nine.baseballdiary.backend.complaints;

import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.record.GameRecord;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_record_id")
    private GameRecord reportedRecord;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "report_date", nullable = false)  // ✅ 추가
    private LocalDate reportDate;  // ✅ 추가

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.reportDate = LocalDate.now();  // ✅ 추가
    }
}
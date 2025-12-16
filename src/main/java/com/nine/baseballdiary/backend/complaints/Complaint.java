package com.nine.baseballdiary.backend.complaints;

import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.record.GameRecord;
import jakarta.persistence.*;
import lombok.*;

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
    private User reporter;  // 신고한 사람

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;  // 신고된 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_record_id")
    private GameRecord reportedRecord;  // 신고된 게시글

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
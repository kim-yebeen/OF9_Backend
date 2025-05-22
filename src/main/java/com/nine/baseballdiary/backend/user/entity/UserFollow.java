package com.nine.baseballdiary.backend.user.entity;

import com.nine.baseballdiary.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import static jakarta.persistence.GenerationType.IDENTITY;


@Getter
@Setter
@Entity
@Table(name="user_follow", uniqueConstraints=@UniqueConstraint(columnNames={"follower_id","followee_id"}))
@NoArgsConstructor @AllArgsConstructor
public class UserFollow {
    @Id @GeneratedValue(strategy=IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User followerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followeeId;
}
package com.nine.baseballdiary.backend.user.repository;

import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    /** 이미 팔로우 중인지 체크 */
    boolean existsByFollowerId_IdAndFolloweeId_Id(Long followerId, Long followeeId);

    /** 언팔로우 */
    void deleteByFollowerId_IdAndFolloweeId_Id(Long followerId, Long followeeId);

    /** 내가 팔로잉한 사람들 */
    List<UserFollow> findByFollowerId_Id(Long followerId);

    /** 나를 팔로잉한 사람들 */
    List<UserFollow> findByFolloweeId_Id(Long followeeId);
}
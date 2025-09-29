package com.nine.baseballdiary.backend.user.repository;

import com.nine.baseballdiary.backend.user.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollow.UserFollowId> {
    // 제네릭 타입: Long → UserFollow.UserFollowId

    /** 이미 팔로우 중인지 체크 */
    boolean existsByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);
    // followerId_IdAndFolloweeId_Id → follower_IdAndFollowee_Id

    /** 언팔로우 */
    void deleteByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);
    // followerId_IdAndFolloweeId_Id → follower_IdAndFollowee_Id

    /** 내가 팔로잉한 사람들 */
    List<UserFollow> findByFollower_Id(Long followerId);
    // followerId_Id → follower_Id

    /** 나를 팔로잉한 사람들 */
    List<UserFollow> findByFollowee_Id(Long followeeId);
    // followeeId_Id → followee_Id

    /** 팔로잉 숫자 카운트 */
    long countByFollower_Id(Long followerId);
    // followerId_Id → follower_Id

    /** 팔로워 숫자 카운트 */
    long countByFollowee_Id(Long followeeId);
    // followeeId_Id → followee_Id

    /** 내가 팔로우하는 사람들의 ID 리스트 */
    @Query("SELECT uf.followee.id FROM UserFollow uf WHERE uf.follower.id = :userId")
    List<Long> findFollowingIds(@Param("userId") Long userId);
    // uf.followeeId.id → uf.followee.id
    // uf.followerId.id → uf.follower.id

    /** 나를 팔로우하는 사람들의 ID 리스트 */
    @Query("SELECT uf.follower.id FROM UserFollow uf WHERE uf.followee.id = :userId")
    List<Long> findFollowerIds(@Param("userId") Long userId);
    // uf.followerId.id → uf.follower.id
    // uf.followeeId.id → uf.followee.id

    @Query("SELECT uf.followee.id FROM UserFollow uf WHERE uf.follower.id = :currentUserId AND uf.followee.id IN :targetUserIds")
    Set<Long> findFolloweeIdsByFollowerIdAndInTargetUserIds(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserIds") List<Long> targetUserIds
    );
    // uf.followeeId.id → uf.followee.id
    // uf.followerId.id → uf.follower.id

    @Modifying
    @Query("DELETE FROM UserFollow uf WHERE uf.follower.id = :userId OR uf.followee.id = :userId")
    void deleteAllByFollowerIdOrFolloweeId(@Param("userId") Long userId);
    // uf.followerId.id → uf.follower.id
    // uf.followeeId.id → uf.followee.id
}
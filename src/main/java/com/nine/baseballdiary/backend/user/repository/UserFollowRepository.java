package com.nine.baseballdiary.backend.user.repository;

import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    //내가 팔로우하는사람들의 ID 리스트
    @Query("select uf.followeeId.id from UserFollow uf where uf.followerId.id = :userId")
    List<Long> findFollowingIds(Long userId);

    //나를 팔로우하는 사람들의 ID 리스트
    @Query("select uf.followerId.id from UserFollow uf where uf.followeeId.id = :userId")
    List<Long> findFollowerIds(Long userId);

}
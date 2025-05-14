package com.nine.baseballdiary.backend.user;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFollowService {

    private final UserFollowRepository userFollowRepository;

    // 팔로우 요청
    public void follow(Long followerId, Long followeeId) {
        // 이미 팔로우 관계가 존재하는지 체크
        if (!userFollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            UserFollow userFollow = new UserFollow();
            userFollow.setFollower(new User(followerId));
            userFollow.setFollowee(new User(followeeId));
            userFollowRepository.save(userFollow);
        } else {
            throw new IllegalArgumentException("이미 팔로우 중입니다.");
        }
    }

    // 언팔로우 요청
    public void unfollow(Long followerId, Long followeeId) {
        userFollowRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
    }
}

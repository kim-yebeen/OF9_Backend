package com.nine.baseballdiary.backend.user;

import com.nine.baseballdiary.backend.record.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RecordRepository recordRepository;
    private final UserFollowRepository userFollowRepository;

    // 유저의 정보 조회
    public UserResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        // 게시글 수, 팔로워 수, 팔로잉 수 계산
        long postCount = recordRepository.countByUserId(userId);
        long followingCount = userFollowRepository.countByFollowerId(userId); // 팔로잉 수
        long followerCount = userFollowRepository.countByFolloweeId(userId); // 팔로워 수

        return new UserResponse(
                user.getNickname(),
                user.getFavTeam(),
                user.getProfileImageUrl(),
                postCount,
                followingCount,
                followerCount
        );
    }
}


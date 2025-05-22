package com.nine.baseballdiary.backend.auth;

import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class KakaoService {
    private final KakaoClient kakaoClient;
    private final UserRepository userRepository;

    public KakaoService(KakaoClient kakaoClient, UserRepository userRepository) {
        this.kakaoClient = kakaoClient;
        this.userRepository = userRepository;
    }

    public User processLogin(String accessToken, String favTeam) {
        Long kakaoId = kakaoClient.getKakaoId(accessToken);
        Optional<User> existing = userRepository.findByKakaoId(kakaoId);
        if (existing.isPresent()) return existing.get();

        User newUser = new User();
        newUser.setKakaoId(kakaoId);
        newUser.setNickname(generateRandomNickname());
        newUser.setFavTeam(favTeam); // ✅ 등록 시 구단명 저장
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(newUser);
    }

    private String generateRandomNickname() {
        List<String> list = List.of(
                "부끄러운 프로직관러", "귀여운 승리요정", "조용한 홈런탐지기",
                "소심한 타석해설가", "수줍은 외야방랑자", "느긋한 투수관찰자",
                "심취한 중계해설러", "덤덤한 벤치지킴이", "과몰입한 응원단장",
                "무해한 스탯계산러", "설레는 굿즈수집가", "침착한 응원지기",
                "반짝이는 응원봉러버", "열정적인 응원마스터", "활기찬 지정석매니아",
                "멍때리는 테이블석러", "흐뭇한 승리예감러", "활기찬 직관요정",
                "기특한 럭키요정", "뿌듯한 구단살이"
        );
        Random r = new Random();
        return list.get(r.nextInt(list.size())) + " " + (1000 + r.nextInt(9000));
    }
}

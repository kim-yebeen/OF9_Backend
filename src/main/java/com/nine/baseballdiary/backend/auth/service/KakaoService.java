package com.nine.baseballdiary.backend.auth.service;

import com.nine.baseballdiary.backend.S3.S3Service;
import com.nine.baseballdiary.backend.auth.client.KakaoClient;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class KakaoService {
    private final KakaoClient kakaoClient;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoAppRedirectUri;

    @Value("${kakao.web.redirect-uri}")
    private String kakaoWebRedirectUri;


    @Transactional
    public User processKakaoLogin(String authCode, String favTeam, String platform) {
        // 1. 플랫폼(app/web)에 맞는 redirect-uri 선택
        String redirectUri = "web".equalsIgnoreCase(platform) ? kakaoWebRedirectUri : kakaoAppRedirectUri;

        // 2. Authorization Code로 카카오 Access Token 획득
        String kakaoAccessToken = getKakaoAccessToken(authCode, redirectUri);

        // 3. 카카오 Access Token으로 사용자 정보 조회
        Map<String, Object> kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);

        // 4. 사용자 정보 기반으로 우리 서비스의 유저 조회 또는 생성
        return getOrCreateUser(kakaoUserInfo, favTeam);
    }

    //카카오 유저 정보 바탕으로 유저 조회 및 생성
    private User getOrCreateUser(Map<String, Object> kakaoUserInfo, String favTeam) {
        Long kakaoId = Long.valueOf(kakaoUserInfo.get("id").toString());
        Optional<User> existingUser = userRepository.findByKakaoId(kakaoId);

        // 이미 가입된 유저이면 바로 반환
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // --- 신규 유저 생성 ---
        Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoUserInfo.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String kakaoNickname = (String) profile.get("nickname");
        String kakaoProfileUrl = (String) profile.get("profile_image_url");

        // 닉네임 중복 처리
        String finalNickname = kakaoNickname;
        if (finalNickname == null || finalNickname.isBlank() || userRepository.existsByNickname(finalNickname)) {
            finalNickname = generateRandomNickname();
        }

        // User 엔티티 생성
        User newUser = User.builder()
                .kakaoId(kakaoId)
                .nickname(finalNickname)
                .favTeam(favTeam)
                .isPrivate(false)
                .build();

        // User를 먼저 한 번 저장하여 ID를 부여받습니다. (S3 경로에 userId를 사용하기 위함)
        User savedUser = userRepository.save(newUser);

        // ✅ 카카오 프로필 이미지를 우리 S3로 복사
        if (kakaoProfileUrl != null) {
            String ourS3Url = s3Service.uploadImageFromUrl(kakaoProfileUrl, savedUser.getId(), "profiles");
            savedUser.setProfileImageUrl(ourS3Url);
            return userRepository.save(savedUser); // 이미지 URL 업데이트 후 다시 저장
        }

        return savedUser;
    }

    //카카오 액세스 토큰으로 카카오 ID만 조회
    public Long getKakaoIdFromToken(String accessToken) {
        Map<String, Object> kakaoUserInfo = getKakaoUserInfo(accessToken);
        return Long.valueOf(kakaoUserInfo.get("id").toString());
    }


    // Authorization Code로 액세스 토큰 획득 (redirect-uri를 파라미터로 받음)
    private String getKakaoAccessToken(String authCode, String redirectUri) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", redirectUri); // 파라미터로 받은 redirect-uri 사용
        params.add("code", authCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    // 액세스 토큰으로 사용자 정보 조회
    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUrl, HttpMethod.GET, request, Map.class);

        return response.getBody();
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
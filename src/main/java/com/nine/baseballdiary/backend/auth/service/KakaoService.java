package com.nine.baseballdiary.backend.auth.service;

import com.nine.baseballdiary.backend.auth.client.KakaoClient;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
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
public class KakaoService {
    private final KakaoClient kakaoClient;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.web.redirect-uri}")
    private String kakaoWebRedirectUri;

    public KakaoService(KakaoClient kakaoClient, UserRepository userRepository) {
        this.kakaoClient = kakaoClient;
        this.userRepository = userRepository;
    }

    // === 앱용 메서드들 (기존 유지) ===

    // 기존 앱용 토큰 방식 (호환성 유지)
    @Transactional
    public User processLogin(String accessToken, String favTeam) {
        Long kakaoId = kakaoClient.getKakaoId(accessToken);
        Optional<User> existing = userRepository.findByKakaoId(kakaoId);
        if (existing.isPresent()) return existing.get();

        User newUser = new User();
        newUser.setKakaoId(kakaoId);
        newUser.setNickname(generateRandomNickname());
        newUser.setFavTeam(favTeam);
        newUser.setIsPrivate(false);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(newUser);
    }

    // === 웹용 메서드들 (새로 추가) ===

    // 웹용 Authorization Code 방식
    @Transactional
    public User processKakaoWebLogin(String authCode, String favTeam) {
        // 1. Authorization Code로 액세스 토큰 획득 (웹용 redirect-uri 사용)
        String accessToken = getKakaoAccessToken(authCode, kakaoWebRedirectUri);

        // 2. 액세스 토큰으로 사용자 정보 조회
        Map<String, Object> kakaoUserInfo = getKakaoUserInfo(accessToken);

        // 3. 사용자 생성/조회
        Long kakaoId = Long.valueOf(kakaoUserInfo.get("id").toString());
        Optional<User> existing = userRepository.findByKakaoId(kakaoId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // 4. 새 사용자 생성
        Map<String, Object> properties = (Map<String, Object>) kakaoUserInfo.get("properties");
        String kakaoNickname = properties != null ? (String) properties.get("nickname") : null;

        String nickname = (kakaoNickname != null && !kakaoNickname.isEmpty()) ?
                kakaoNickname : generateRandomNickname();

        // 닉네임 중복 체크
        String finalNickname = nickname;
        int counter = 1;
        while (userRepository.existsByNickname(finalNickname)) {
            finalNickname = nickname + counter;
            counter++;
        }

        User newUser = new User();
        newUser.setKakaoId(kakaoId);
        newUser.setNickname(finalNickname);
        newUser.setFavTeam(favTeam);
        newUser.setIsPrivate(false);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        if (properties != null && properties.get("profile_image") != null) {
            newUser.setProfileImageUrl((String) properties.get("profile_image"));
        }

        return userRepository.save(newUser);
    }

    // === 기존 통합 메서드 (앱용 redirect-uri 사용) ===

    // 통일된 Authorization Code 방식 (앱용 - 호환성을 위해 유지)
    @Transactional
    public User processKakaoLogin(String authCode, String favTeam) {
        // 앱용 redirect-uri 사용
        String accessToken = getKakaoAccessToken(authCode, kakaoRedirectUri);

        Map<String, Object> kakaoUserInfo = getKakaoUserInfo(accessToken);
        Long kakaoId = Long.valueOf(kakaoUserInfo.get("id").toString());
        Optional<User> existing = userRepository.findByKakaoId(kakaoId);

        if (existing.isPresent()) {
            return existing.get();
        }

        Map<String, Object> properties = (Map<String, Object>) kakaoUserInfo.get("properties");
        String kakaoNickname = properties != null ? (String) properties.get("nickname") : null;

        String nickname = (kakaoNickname != null && !kakaoNickname.isEmpty()) ?
                kakaoNickname : generateRandomNickname();

        String finalNickname = nickname;
        int counter = 1;
        while (userRepository.existsByNickname(finalNickname)) {
            finalNickname = nickname + counter;
            counter++;
        }

        User newUser = new User();
        newUser.setKakaoId(kakaoId);
        newUser.setNickname(finalNickname);
        newUser.setFavTeam(favTeam);
        newUser.setIsPrivate(false);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        if (properties != null && properties.get("profile_image") != null) {
            newUser.setProfileImageUrl((String) properties.get("profile_image"));
        }

        return userRepository.save(newUser);
    }

    // === 공통 private 메서드들 ===

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

    // 테스트용 사용자 생성
    @Transactional
    public User createTestUser(String favTeam) {
        Long testKakaoId = System.currentTimeMillis();

        Optional<User> existing = userRepository.findByKakaoId(testKakaoId);
        if (existing.isPresent()) {
            return existing.get();
        }

        User testUser = new User();
        testUser.setKakaoId(testKakaoId);
        testUser.setNickname(generateRandomNickname());
        testUser.setFavTeam(favTeam);
        testUser.setIsPrivate(false);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(testUser);
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

    public Long getKakaoIdFromToken(String accessToken) {
        return kakaoClient.getKakaoId(accessToken);
    }
}
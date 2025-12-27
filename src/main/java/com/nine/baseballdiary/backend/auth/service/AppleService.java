package com.nine.baseballdiary.backend.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nine.baseballdiary.backend.user.entity.SocialType;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public User processAppleLogin(String identityToken, String userJson, String favTeam) {
        // 1. 애플 ID 토큰 검증 및 사용자 ID(sub) 추출
        Map<String, String> appleUserInfo = getAppleUserInfo(identityToken);
        String socialId = appleUserInfo.get("sub"); // 유저 고유 ID
        String email = appleUserInfo.get("email");  // 유저 이메일

        // 2. 유저 조회 또는 생성
        return getOrCreateUser(socialId, email, favTeam);
    }

    private User getOrCreateUser(String socialId, String email, String favTeam) {
        // DB에서 socialId와 socialType으로 조회
        Optional<User> existingUser = userRepository.findBySocialIdAndSocialType(socialId, SocialType.APPLE);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // 없으면 회원가입
        String nickname = generateUniqueRandomNickname();

        User newUser = User.builder()
                .socialId(socialId)
                .socialType(SocialType.APPLE)
                .nickname(nickname)
                .favTeam(favTeam)
                .isPrivate(false)
                .build();

        return userRepository.save(newUser);
    }

    // 🍎 핵심: identityToken 검증 및 정보 추출
    private Map<String, String> getAppleUserInfo(String identityToken) {
        try {
            // 1. 애플 공개키 목록 가져오기
            String applePublicKeysUrl = "https://appleid.apple.com/auth/keys";
            String response = restTemplate.getForObject(applePublicKeysUrl, String.class);

            // 2. identityToken 헤더에서 kid(Key ID) 추출
            String headerOfIdentityToken = identityToken.substring(0, identityToken.indexOf("."));
            Map<String, String> header = objectMapper.readValue(new String(Base64.getUrlDecoder().decode(headerOfIdentityToken)), Map.class);
            String kid = header.get("kid");

            // 3. 공개키 목록에서 내 토큰과 맞는 키 찾기
            JsonNode keys = objectMapper.readTree(response).get("keys");
            JsonNode correctKey = null;
            for (JsonNode key : keys) {
                if (key.get("kid").asText().equals(kid)) {
                    correctKey = key;
                    break;
                }
            }

            if (correctKey == null) {
                throw new RuntimeException("일치하는 애플 공개키를 찾을 수 없습니다.");
            }

            // 4. RSA 공개키 생성
            BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(correctKey.get("n").asText()));
            BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(correctKey.get("e").asText()));
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
            KeyFactory keyFactory = KeyFactory.getInstance(correctKey.get("kty").asText());
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            // 5. 토큰 검증 및 파싱
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(identityToken)
                    .getBody();

            return Map.of(
                    "sub", claims.getSubject(),
                    "email", claims.get("email", String.class) != null ? claims.get("email", String.class) : ""
            );

        } catch (Exception e) {
            log.error("애플 토큰 검증 실패", e);
            throw new RuntimeException("애플 로그인 실패: 토큰 검증 오류");
        }
    }

    // 닉네임 생성 로직은 KakaoService와 중복되므로, 별도 유틸 클래스로 빼는 게 좋지만 일단 복사
    private String generateUniqueRandomNickname() {
        // ... (KakaoService에 있는 로직과 동일하게 구현) ...
        String nickname;
        do {
            nickname = generateRandomNickname();
        } while (userRepository.existsByNickname(nickname));
        return nickname;
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
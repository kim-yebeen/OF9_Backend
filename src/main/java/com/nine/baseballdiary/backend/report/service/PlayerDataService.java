package com.nine.baseballdiary.backend.report.service;

import com.nine.baseballdiary.backend.report.dto.PlayerInfoDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PlayerDataService {

    private static final Map<String, List<PlayerInfoDto>> TEAM_PLAYERS = Map.of(
            "KIA", List.of(
                    PlayerInfoDto.builder().name("김도영").team("KIA").position("내야수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("소크라테스").team("KIA").position("투수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("윤영철").team("KIA").position("외야수").imageUrl(null).build()
            ),
            "NC", List.of(
                    PlayerInfoDto.builder().name("손아섭").team("NC").position("내야수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("카일 하트").team("NC").position("투수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("박민우").team("NC").position("내야수").imageUrl(null).build()
            ),
            "삼성", List.of(
                    PlayerInfoDto.builder().name("구자욱").team("삼성").position("외야수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("원태인").team("삼성").position("투수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("김헌곤").team("삼성").position("내야수").imageUrl(null).build()
            ),
            "LG", List.of(
                    PlayerInfoDto.builder().name("오스틴").team("LG").position("내야수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("임찬규").team("LG").position("투수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("박동원").team("LG").position("포수").imageUrl(null).build()
            ),
            "두산", List.of(
                    PlayerInfoDto.builder().name("양의지").team("두산").position("포수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("알칸타라").team("두산").position("투수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("김재환").team("두산").position("내야수").imageUrl(null).build()
            ),
            "KT", List.of(
                    PlayerInfoDto.builder().name("로하스 주니어").team("KT").position("내야수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("엄상백").team("KT").position("투수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("장성우").team("KT").position("외야수").imageUrl(null).build()
            ),
            "SSG", List.of(
                    PlayerInfoDto.builder().name("최정").team("SSG").position("내야수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("김광현").team("SSG").position("투수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("박성한").team("SSG").position("내야수").imageUrl(null).build()
            ),
            "롯데", List.of(
                    PlayerInfoDto.builder().name("나균안").team("롯데").position("외야수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("박세웅").team("롯데").position("투수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("손호영").team("롯데").position("외야수").imageUrl(null).build()
            ),
            "한화", List.of(
                    PlayerInfoDto.builder().name("페라자").team("한화").position("내야수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("문동주").team("한화").position("투수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("노시환").team("한화").position("내야수").imageUrl(null).build()
            ),
            "키움", List.of(
                    PlayerInfoDto.builder().name("송성문").team("키움").position("외야수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("후라도").team("키움").position("투수").imageUrl(null).build(),
                    PlayerInfoDto.builder().name("이정후").team("키움").position("외야수").imageUrl(null).build()
            )
    );

    public List<PlayerInfoDto> getPlayersByTeam(String teamCode) {
        return TEAM_PLAYERS.getOrDefault(teamCode, List.of());
    }

    public List<PlayerInfoDto> getAllPlayers() {
        return TEAM_PLAYERS.values().stream()
                .flatMap(List::stream)
                .toList();
    }
}
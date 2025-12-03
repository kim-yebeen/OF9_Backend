package com.nine.baseballdiary.backend.stadium;

import com.nine.baseballdiary.backend.stadium.StadiumSeatResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StadiumService {

    private final Map<String, List<String>> stadiumZoneMap = new HashMap<>();

    public StadiumSeatResponse getStadiumSeats(String stadiumName) {
        List<String> zones = stadiumZoneMap.get(stadiumName);

        if (zones == null) {
            // 정확한 이름이 없을 경우 공백 제거 후 검색 시도 (유연성 확보)
            zones = stadiumZoneMap.entrySet().stream()
                    .filter(entry -> entry.getKey().replace(" ", "").equals(stadiumName.replace(" ", "")))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 구장입니다: " + stadiumName));
        }

        return StadiumSeatResponse.builder()
                .stadiumName(stadiumName)
                .zones(zones)
                .build();
    }

    @PostConstruct
    public void initData() {
        // 1. 잠실 야구장
        stadiumZoneMap.put("잠실 야구장", List.of(
                "1루 테이블석", "1루 블루석", "1루 오렌지석", "1루 레드석", "1루 네이비석", "1루 외야석",
                "중앙 네이비석", "3루 테이블석", "3루 블루석", "3루 오렌지석", "3루 레드석", "3루 네이비석",
                "3루 외야석", "익사이팅존"
        ));

        // 2. 사직 야구장
        stadiumZoneMap.put("사직 야구장", List.of(
                "SKY BOX", "에비뉴엘석", "중앙탁자석", "응원탁자석", "와이드탁자석", "3루 단체석",
                "1루 내야상단석", "1루 내야필드석", "중앙 상단석", "3루 내야상단석", "3루 내야필드석",
                "1루 외야석", "3루 외야석", "1루 외야 탁자석", "3루 외야 탁자석", "휠체어석"
        ));

        // 3. 고척 SKYDOME
        stadiumZoneMap.put("고척 SKYDOME", List.of(
                "R.d_club석", "1루 테이블석", "중앙 테이블석", "3루 테이블석",
                "1루 다크버건디석", "3루 다크버건디석", "1루 버건디석", "3루 버건디석",
                "1루 3층 지정석", "3루 3층 지정석", "1루 4층 지정석", "중앙 4층 지정석", "3루 4층 지정석",
                "1루 1~2층 외야 일반석", "1루 3~4층 외야 일반석", "3루 1~2층 외야 일반석", "3루 3~4층 외야 일반석",
                "커플석", "패밀리석", "유아동반석", "휠체어석"
        ));

        // 4. 대구삼성라이온즈파크
        stadiumZoneMap.put("대구삼성라이온즈파크", List.of(
                "VIP석", "1루 테이블석", "중앙 테이블석", "3루 테이블석",
                "1루 익사이팅석", "3루 익사이팅석", "원정응원석", "블루존",
                "1루 내야지정석", "3루 내야지정석", "내야 패밀리석", "SKY 하단지정석",
                "1루 SKY 상단지정석", "중앙 SKY 상단지정석", "3루 SKY 상단지정석",
                "외야지정석", "외야패밀리석", "외야테이블석", "외야커플테이블석",
                "루프탑 테이블석", "파티플로어 라이브석", "캠핑존", "잔디그린존", "휠체어 장애인석"
        ));

        // 5. 대전 한화생명볼파크
        stadiumZoneMap.put("대전 한화생명볼파크", List.of(
                "1루 내야지정석A", "3루 내야지정석A", "1루 내야지정석B", "3루 내야지정석B",
                "응원단석", "포수후면석", "중앙지정석", "중앙탁자석",
                "1루 내야커플석", "3루 내야박스석", "1루 내야탁자석",
                "외야지정석", "잔디석", "외야탁자석", "이닝스 VIP 바&룸", "스카이박스",
                "중앙 휠체어석", "내야 휠체어석", "외야 휠체어석"
        ));

        // 6. 기아 챔피언스 필드
        stadiumZoneMap.put("기아 챔피언스 필드", List.of(
                "챔피언석", "중앙테이블석", "서프라이즈석", "타이거즈 가족석", "파티석", "스카이피크닉석",
                "외야가족석", "테이블석", "응원특별석",
                "1루 내야석A(K9)", "1루 내야석B(K8)", "1루 내야석C(K5)",
                "3루 내야석A(K9)", "3루 내야석B(K8)", "3루 내야석C(K5)",
                "1루 내야 상단석(EV석)", "3루 내야 상단석(EV석)",
                "외야석", "1루 휠체어 장애인석", "3루 휠체어 장애인석", "스카이박스석"
        ));

        // 7. 수원 KT WIZ 파크
        stadiumZoneMap.put("수원 KT WIZ 파크", List.of(
                "중앙 내야석", "1루 테이블석", "3루 테이블석", "중앙 지정석",
                "1루 응원 지정석", "3루 응원 지정석", "1루 스카이존", "3루 스카이존",
                "익사이팅", "외야잔디/자유석", "외야 테이블석", "위즈 캠핑존",
                "1루 휠체어석", "3루 휠체어석"
        ));

        // 8. 창원 NC파크
        stadiumZoneMap.put("창원 NC파크", List.of(
                "프리미엄석", "1루 내야석", "2루 내야석", "3루 내야석",
                "미니테이블석", "테이블석", "피크닉테이블석", "라운드테이블석",
                "외야잔디석(5인)", "외야석", "바베큐석", "가족석(2인)",
                "불펜석", "불펜가족석", "휠체어석", "스카이박스",
                "노스피크캠프닉석(4인)", "노스피크캠프닉석(8인)", "카운터석"
        ));

        // 9. 인천 SSG 랜더스필드
        stadiumZoneMap.put("인천 SSG 랜더스필드", List.of(
                "랜더스 라이브존", "프렌들리존", "1층 테이블석", "2층 테이블석",
                "1루 덕아웃 상단석", "1루 으쓱이존", "1루 내야패밀리존", "1루 내야 필드석", "1루 외야 필드석",
                "3루 덕아웃 상단석", "3루 원정응원석", "3루 내야패밀리존", "3루 내야 필드석", "3루 외야 필드석",
                "4층 SKY뷰석", "SKY탁자석", "홈런커플존", "휠체어 장애인석",
                "그린존", "바비큐존", "외야파티덱", "외야패밀리존", "초가정자", "미니스카이박스", "스카이박스"
        ));
    }
}
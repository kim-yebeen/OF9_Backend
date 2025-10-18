package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.user.dto.UserDto;
import lombok.Getter;
import lombok.Builder;
import lombok.Setter;

import java.util.List;

// 클릭 시 상세 보기 용
@Getter @Setter
@Builder
public class RecordDetailResponse {

    private Long userId;           // 작성자 ID
    private String nickname;       // 작성자 닉네임
    private String profileImageUrl; // 작성자 프로필 이미지
    private String favTeam;        // 작성자 응원팀

    private Long recordId;
    private String gameDate;
    private String gameTime;
    private Integer emotionCode;
    private String emotionLabel;
    private String ticketImageUrl;
    private String homeTeam;
    private String awayTeam;
    private String stadium;
    private String seatInfo;
    private Integer homeScore;
    private Integer awayScore;
    private String result;
    private String comment;
    private String longContent;
    private String bestPlayer;
    private List<UserDto> companions;
    private List<String> foodTags;
    private List<String> mediaUrls;
    private String createdAt;

    //좋아요 및 댓글 정보로 변경
    private Long likeCount;        // 좋아요 개수
    private Boolean isLiked;       // 현재 사용자가 좋아요 했는지
    private Long commentCount;     // 댓글 개수
}
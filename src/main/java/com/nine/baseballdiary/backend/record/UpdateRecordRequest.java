// 1-3) 업데이트 요청: 선택입력값만
package com.nine.baseballdiary.backend.record;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class UpdateRecordRequest {
    private String               comment;
    private String               longContent;
    private String               bestPlayer;
    private List<String>         companions;
    private List<String>         foodTags;
    private List<String>         mediaUrls;
}

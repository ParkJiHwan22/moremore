package com.ssafy.memberservice.member.controller.dto.response;

import lombok.Data;

@Data
public class LoginJwtResponse {

    private String accessToken;
    private String refreshToken;
    private Long memberId;
    private String type = "Bearer";

    public LoginJwtResponse(String accessToken, String refreshToken, Long memberId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.memberId = memberId;

    }
}

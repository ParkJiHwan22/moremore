package com.ssafy.clubservice.club.controller.dto.response;


import com.ssafy.clubservice.club.enumeration.AcceptanceStatus;
import com.ssafy.clubservice.club.enumeration.ClubRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParticipantUpdateResponse {
    private Long participantId;
    private String clubCode;
    private Long userId;
    private ClubRole clubRole;
    private AcceptanceStatus acceptanceStatus;
    private LocalDateTime createdDate;
}

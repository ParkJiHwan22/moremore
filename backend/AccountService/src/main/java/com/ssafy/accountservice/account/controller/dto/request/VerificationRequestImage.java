package com.ssafy.accountservice.account.controller.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VerificationRequestImage {
    private MultipartFile accountHistoryImage;  // 계좌내역증빙 이미지
}

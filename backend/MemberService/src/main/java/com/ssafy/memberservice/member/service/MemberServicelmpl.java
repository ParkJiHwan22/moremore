package com.ssafy.memberservice.member.service;


import com.ssafy.memberservice.member.controller.dto.request.MemberCreateRequest;
import com.ssafy.memberservice.member.controller.dto.response.MemberAllGetResponse;
import com.ssafy.memberservice.member.controller.dto.response.MemberGetResponse;
import com.ssafy.memberservice.member.infrastructure.repository.MemberRepository;
import com.ssafy.memberservice.member.infrastructure.repository.entity.MemberEntity;
import com.ssafy.memberservice.member.infrastructure.s3.S3Connector;
import com.ssafy.memberservice.member.mapper.MemberObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberServicelmpl implements MemberService{
    private final S3Connector s3Connector;
    private final PasswordEncoder passwordEncoder;
    private final MemberObjectMapper memberObjectMapper;
    private final MemberRepository memberRepository;
    private final SsafyApiService ssafyApiService;

    @Override
    public void registerMember(MemberCreateRequest memberRequest) {
        MemberEntity memberEntity= memberObjectMapper.fromMemberCreateRequestToEntity(memberRequest);

        // 1. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(memberRequest.getPassword());

        MultipartFile profileImage = memberRequest.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()) {
            // 고유한 파일 이름 생성 (예: email + UUID)
            String randomString = UUID.randomUUID().toString();
            String fileName = memberRequest.getEmail() + "_" + randomString;

            // S3에 파일 업로드
            s3Connector.upload(fileName, profileImage);
            // 업로드된 파일의 URL 가져오기
            String imageUrl = s3Connector.getImageURL(fileName);
            // 이미지 URL을 MemberEntity에 설정
            memberEntity.setProfileImageUrl(imageUrl);
        }

        // 외부 API에서 userKey 가져오기 (SsafyApiService에서 처리)
        String userKey = ssafyApiService.fetchUserKeyFromApi(memberRequest.getEmail());
        memberEntity.setUserKey(userKey);


        memberEntity.setPassword(encodedPassword);
        memberRepository.saveMember(memberEntity);

    }

    @Override
    public MemberGetResponse findByMemberId(Long memberId) {
        return memberObjectMapper.fromEntityToMemberGetResponse(memberRepository.findByMemberId(memberId)) ;
    }

    @Override
    public List<MemberAllGetResponse> findAllMembers() {
        return memberObjectMapper.fromEntitysToMemberAllGetResponses(memberRepository.findAllMembers());
    }

    @Override
    public String findName(String accountNumber) {
        return memberRepository.findByAccountNumber(accountNumber);
    }

    @Override
    public Boolean checkByAccountNumber(String accountNumber) { return memberRepository.checkAccount(accountNumber);
    }

    @Override
    public void updateFcmToken(Long memberId, String fcmToken) {
        memberRepository.updateFcmToken(memberId, fcmToken);
    }

    @Override
    public String getFcmTokenByMemberId(Long memberId) {
        return memberRepository.getFcmTokenByMemberId(memberId);
    }
}

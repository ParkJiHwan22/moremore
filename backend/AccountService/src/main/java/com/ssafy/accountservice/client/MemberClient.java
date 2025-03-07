package com.ssafy.accountservice.client;

import com.ssafy.accountservice.account.controller.dto.response.MemberGetResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service", url = "https://j11a605.p.ssafy.io/api/member")
public interface MemberClient {

    @GetMapping("/{memberId}")
    MemberGetResponse getMember(@PathVariable("memberId") Long memberId);
}
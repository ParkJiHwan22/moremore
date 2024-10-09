package com.ssafy.accountservice.account.service;

import com.ssafy.accountservice.account.controller.dto.request.*;
import com.ssafy.accountservice.account.controller.dto.response.*;
import com.ssafy.accountservice.account.infrastructure.repository.AccountRepository;
import com.ssafy.accountservice.account.infrastructure.repository.entity.AccountHistoryEntity;
import com.ssafy.accountservice.account.infrastructure.repository.entity.VerifyEntity;
import com.ssafy.accountservice.account.service.domain.*;
import com.ssafy.accountservice.client.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountFeignClient accountFeignClient;
    private final SelectAccountNumFeignClient selectAccountNumFeignClient;
    private final AccountTransferFeignClient accountTransferFeignClient;
    private final UseCardFeignClient useCardFeignClient;
    private final MemberClient memberClient;

    @Override
    public void accountCreate(Account account) {
        String ssafyUserKey = account.getSsafyUserKey();
        String apiKey = AccountUtils.getApiKey();

        // Header 생성
        AccountCreateApiRequest.Header header = new AccountCreateApiRequest.Header();
        header.setApiKey(apiKey);
        header.setUserKey(ssafyUserKey);

        // 전체 API 요청 객체 생성
        AccountCreateApiRequest accountCreateApiRequest = new AccountCreateApiRequest();
        accountCreateApiRequest.setHeader(header);
        accountCreateApiRequest.setAccountTypeUniqueNo("001-1-7c1e096a0f2d40");

        // Feign Client를 사용해 POST 요청 전송
        AccountCreateApiResponse accountCreateApiResponse = accountFeignClient.createAccount(accountCreateApiRequest);
        ArrayList<String> arr = new ArrayList<>();

        arr.add(accountCreateApiResponse.getRec().getAccountNo());
        arr.add(account.getClubCode());
        arr.add(account.getPwd());
        arr.add(ssafyUserKey);

        accountRepository.saveAccount(arr);
    }


    @Override
    public Map<String, String> accountSelectNumberAndBalance(String clubCode) {
        String apiKey = AccountUtils.getApiKey();

        // 모임코드 들고 왔을 때, 해당 모임의 총무 api key를 넣어서 조회
        Map<String, String> map = accountRepository.selectAccountNumberAndUserKey(clubCode);

        String managerKey = map.get("ssafy_user_key");
        String accountNum = map.get("ssafy_account_number");

        AccountSelectApiRequest accountSelectApiRequest = new AccountSelectApiRequest();
        accountSelectApiRequest.getHeader().setApiKey(apiKey);
        accountSelectApiRequest.getHeader().setUserKey(managerKey);
        accountSelectApiRequest.setAccountNo(accountNum);

        // Feign Client
        AccountSelectBalanceApiResponse response = selectAccountNumFeignClient.selectAccountBalance(accountSelectApiRequest);

        // account number, account balance만 담아서 return
        Map<String, String> numAndBalance = new HashMap<>();
        numAndBalance.put("account_num", accountNum);
        numAndBalance.put("account_balance", response.getRec().getAccountBalance());

        return numAndBalance;
    }


    @Override
    public ArrayList<String> accountTransfer(AccountTransfer accountTransfer) {
        String apiKey = AccountUtils.getApiKey();

        MemberGetResponse memberGetResponse = memberClient.getMember(Long.valueOf(accountTransfer.getMemberId()));
        String userKey = accountRepository.selectUserKey(accountTransfer.getClubCode());
        String postAccountNum = memberGetResponse.getAccountNumber();
        String name = memberGetResponse.getName();

        // 모임코드 들고 왔을 때, 해당 모임의 총무 api key를 넣어서 조회
        String withdrawalAccountNo = accountRepository.selectAccountNumber(accountTransfer.getClubCode());

        AccountTransferApiRequest accountTransferApiRequest = new AccountTransferApiRequest();
        accountTransferApiRequest.getHeader().setApiKey(apiKey);
        accountTransferApiRequest.getHeader().setUserKey(userKey);
        accountTransferApiRequest.setDepositAccountNo(postAccountNum);
        accountTransferApiRequest.setTransactionBalance(accountTransfer.getTransactionBalance());
        accountTransferApiRequest.setWithdrawalAccountNo(withdrawalAccountNo);
        
        // Feign Client
        AccountTransferApiResponse accountTransferApiResponse = accountTransferFeignClient.transferAccountBalance(accountTransferApiRequest);
        AccountTransferApiResponse.REC firstRec = accountTransferApiResponse.getRec().get(0);

        // 계좌 잔고 들고 오는 로직
        Map<String, String> map = accountRepository.selectAccountNumberAndUserKey(accountTransfer.getClubCode());

        String managerKey = map.get("ssafy_user_key");
        String accountNum = map.get("ssafy_account_number");

        AccountSelectApiRequest accountSelectApiRequest = new AccountSelectApiRequest();
        accountSelectApiRequest.getHeader().setApiKey(apiKey);
        accountSelectApiRequest.getHeader().setUserKey(managerKey);
        accountSelectApiRequest.setAccountNo(accountNum);

        // Feign Client
        AccountSelectBalanceApiResponse balanceResponse = selectAccountNumFeignClient.selectAccountBalance(accountSelectApiRequest);

        // 계좌 잔고
        String accountBalance = balanceResponse.getRec().getAccountBalance();

        // 받는 계좌
        String depositAccountNo = firstRec.getTransactionAccountNo();

        // 송금 금액
        String transactionBalance = accountTransfer.getTransactionBalance();

        // 이체 정보를 기록할 ArrayList
        ArrayList<String> arr = new ArrayList<>();
        arr.add(depositAccountNo);
        arr.add(transactionBalance);
        arr.add(accountBalance);

        // account_history 테이블에 저장할 AccountHistoryAll 객체 생성 및 데이터 설정
        AccountHistoryAll accountHistoryAll = new AccountHistoryAll();
        accountHistoryAll.setAccountId(balanceResponse.getRec().getAccountNo());   // 계좌 ID
        accountHistoryAll.setTagName(LocalDate.now() + "." + firstRec.getTransactionType());  // 결제 태그
        accountHistoryAll.setSsafyTransactionNumber(balanceResponse.getHeader().getInstitutionTransactionUniqueNo());  // SSAFY 거래번호
        accountHistoryAll.setPaymentType(firstRec.getTransactionTypeName());       // 결제 타입
        accountHistoryAll.setPaymentAmount(transactionBalance);   // 송금 금액
        accountHistoryAll.setAccountBalance(accountBalance);      // 계좌 잔액
        accountHistoryAll.setPaymentData(name);                 // 계좌 내용
//        accountHistoryAll.setAccountHistoryVerificationContent();  // 증빙 내용

        // accountRepository를 사용해 account_history 테이블에 내역 저장
        accountRepository.insertAccountHistory(accountHistoryAll);

        return arr;
    }


    @Override
    public ArrayList<String> accountFill(AccountTransferFillRequest accountTransferFillRequest) {
        String apiKey = AccountUtils.getApiKey();

        // 클럽 코드로 계좌 번호 조회 (입금 계좌)
        String depositAccountNo = accountRepository.selectAccountNumber(accountTransferFillRequest.getClubCode());

        MemberGetResponse memberGetResponse = memberClient.getMember(Long.valueOf(accountTransferFillRequest.getMemberId()));
        String userKey = memberGetResponse.getUserKey();
        String withdrawAccountNum = memberGetResponse.getAccountNumber();
        String name = memberGetResponse.getName();

        // 이체 API 요청 준비
        AccountTransferApiRequest accountTransferApiRequest = new AccountTransferApiRequest();
        accountTransferApiRequest.getHeader().setApiKey(apiKey);
        accountTransferApiRequest.getHeader().setUserKey(userKey);
        accountTransferApiRequest.setDepositAccountNo(depositAccountNo);  // 입금 계좌
        accountTransferApiRequest.setTransactionBalance(accountTransferFillRequest.getTransactionBalance());
        accountTransferApiRequest.setWithdrawalAccountNo(withdrawAccountNum); // 출금 계좌

        // Feign Client 사용하여 이체 API 호출
        AccountTransferApiResponse accountTransferApiResponse = accountTransferFeignClient.transferAccountBalance(accountTransferApiRequest);
        AccountTransferApiResponse.REC secondRec = accountTransferApiResponse.getRec().get(1);

        // 입금 계좌 정보 및 송금 내역 저장
        ArrayList<String> arr = new ArrayList<>();
        arr.add(secondRec.getTransactionAccountNo());  // 입금 계좌
        arr.add(accountTransferFillRequest.getTransactionBalance());    // 이체 금액

        // 계좌 잔고 조회 로직
        Map<String, String> map = accountRepository.selectAccountNumberAndUserKey(accountTransferFillRequest.getClubCode());
        String managerKey = map.get("ssafy_user_key");
        String accountNum = map.get("ssafy_account_number");

        AccountSelectApiRequest accountSelectApiRequest = new AccountSelectApiRequest();
        accountSelectApiRequest.getHeader().setApiKey(apiKey);
        accountSelectApiRequest.getHeader().setUserKey(managerKey);
        accountSelectApiRequest.setAccountNo(accountNum);

        // Feign Client로 잔고 조회
        AccountSelectBalanceApiResponse balanceResponse = selectAccountNumFeignClient.selectAccountBalance(accountSelectApiRequest);

        // 잔고 추가
        arr.add(balanceResponse.getRec().getAccountBalance());

        // 이체 내역을 기록할 AccountHistoryAll 객체 생성
        AccountHistoryAll accountHistoryAll = new AccountHistoryAll();
        accountHistoryAll.setAccountId(balanceResponse.getRec().getAccountNo());  // 계좌 ID
        accountHistoryAll.setTagName(LocalDate.now() + "." + secondRec.getTransactionType());  // 결제 태그
        accountHistoryAll.setSsafyTransactionNumber(balanceResponse.getHeader().getInstitutionTransactionUniqueNo());  // SSAFY 거래번호
        accountHistoryAll.setPaymentType(secondRec.getTransactionTypeName()); // 결제 타입
        accountHistoryAll.setPaymentAmount(accountTransferFillRequest.getTransactionBalance()); // 이체 금액
        accountHistoryAll.setAccountBalance(balanceResponse.getRec().getAccountBalance());  // 계좌 잔고
        accountHistoryAll.setPaymentData(name);                 // 계좌 내용

        // accountRepository를 통해 내역 저장
        accountRepository.insertAccountHistory(accountHistoryAll);

        return arr;
    }


    @Override
    public List<AccountHistoryEntity> accountHistory(String clubCode) {
        String accountNum = accountRepository.selectAccountNum(clubCode);
        return accountRepository.selectAccountHistory(accountNum);
    }

    @Override
    public String cardUse(CardRequest cardRequest) {
        // pg DB이용하여 클럽코드 가져옴

        String apiKey = AccountUtils.getApiKey();
        String pgUserKey = AccountUtils.getUserKey();
        String accountNo = AccountUtils.getAccountNo();
        String clubCode = accountRepository.useAccountPg(cardRequest.getCardNo());

        // 모임코드 들고 왔을 때, 해당 모임의 총무 api key를 넣어서 조회
        Map<String, String> map = accountRepository.selectAccountNumberAndUserKey(clubCode);

        String managerKey = map.get("ssafy_user_key");
        String accountNum = map.get("ssafy_account_number");

        AccountSelectApiRequest accountSelectApiRequest = new AccountSelectApiRequest();
        accountSelectApiRequest.getHeader().setApiKey(apiKey);
        accountSelectApiRequest.getHeader().setUserKey(managerKey);
        accountSelectApiRequest.setAccountNo(accountNum);
        
        // Feign Client를 통해 계좌 잔액 조회
        AccountSelectBalanceApiResponse response = selectAccountNumFeignClient.selectAccountBalance(accountSelectApiRequest);

        // 결제 금액보다 계좌 잔고가 클 경우
        Long accountBalance = Long.valueOf(response.getRec().getAccountBalance());
        Long cardPayment = Long.valueOf(cardRequest.getPaymentBalance());

        if (accountBalance >= cardPayment) {
            // PG사로 송금
            AccountTransferApiRequest accountTransferApiRequest = new AccountTransferApiRequest();
            accountTransferApiRequest.getHeader().setApiKey(apiKey);
            accountTransferApiRequest.getHeader().setUserKey(managerKey);
            accountTransferApiRequest.setDepositAccountNo(accountNo);
            accountTransferApiRequest.setTransactionBalance(cardRequest.getPaymentBalance());
            accountTransferApiRequest.setWithdrawalAccountNo(accountNum);

            // Feign Client - 모임 계좌에서 PG로 돈 이동
            accountTransferFeignClient.transferAccountBalance(accountTransferApiRequest);

            // 카드 결제 진행
            UseCardApiRequest useCardApiRequest = new UseCardApiRequest();
            useCardApiRequest.getHeader().setApiKey(apiKey);
            useCardApiRequest.getHeader().setUserKey(pgUserKey);
            useCardApiRequest.setCardNo(cardRequest.getCardNo());
            useCardApiRequest.setCvc(cardRequest.getCvc());
            useCardApiRequest.setMerchantId(cardRequest.getMerchantId());
            useCardApiRequest.setPaymentBalance(cardRequest.getPaymentBalance());

            System.out.println("useCardApiRequest = " + useCardApiRequest);
            
            UseCardApiResponse useCardApiResponse = useCardFeignClient.useCardByPg(useCardApiRequest);


            // Feign Client를 통해 계좌 잔액 다시 조회
            AccountSelectApiRequest accountSelectReApiRequest = new AccountSelectApiRequest();
            accountSelectReApiRequest.getHeader().setApiKey(apiKey);
            accountSelectReApiRequest.getHeader().setUserKey(managerKey);
            accountSelectReApiRequest.setAccountNo(accountNum);

            AccountSelectBalanceApiResponse apiResponse = selectAccountNumFeignClient.selectAccountBalance(accountSelectReApiRequest);

            // 카드 결제 내역을 DB에 저장
            AccountHistoryAll accountHistoryAll = new AccountHistoryAll();
            accountHistoryAll.setAccountId(apiResponse.getRec().getAccountNo());  // 계좌 ID
            accountHistoryAll.setTagName(LocalDate.now() + "." + useCardApiResponse.getRec().getMerchantName() + "." + useCardApiResponse.getRec().getTransactionTime());  // 결제 태그
            accountHistoryAll.setSsafyTransactionNumber(useCardApiResponse.getHeader().getInstitutionTransactionUniqueNo());  // SSAFY 거래번호
            accountHistoryAll.setPaymentType("출금(이체)");  // 결제 타입
            accountHistoryAll.setPaymentAmount(useCardApiResponse.getRec().getPaymentBalance());  // 결제 금액
            accountHistoryAll.setAccountBalance(apiResponse.getRec().getAccountBalance());  // 계좌 잔고
            accountHistoryAll.setPaymentData(useCardApiResponse.getRec().getMerchantName());                 // 계좌 내용

            // accountRepository를 통해 내역 저장
            accountRepository.insertAccountHistory(accountHistoryAll);

            // 결제 성공 메시지 반환
            return "결제가 성공적으로 완료되었습니다.";
        } else {
            // 결제 실패 (잔액 부족)
            return "결제 실패: 잔액이 부족합니다.";
        }
    }


    @Override
    public AccountHistoryEntity historyGetOnly(String ssafyTransactionNumber) {
        return accountRepository.selectHistoryOnly(ssafyTransactionNumber);
    }


    @Override
    public void verifySave(VerificationSaveRequest verificationSaveRequest) {
        accountRepository.insertVerify(verificationSaveRequest);
    }


    @Override
    public VerifyEntity verifySelect(String ssafyTransactionNumber) {
        return accountRepository.selectVerify(ssafyTransactionNumber);
    }

    @Override
    public void verifyUpdate(String ssafyTransactionNumber, VerificationSaveRequest verificationSaveRequest) {
        accountRepository.updateVerify(ssafyTransactionNumber, verificationSaveRequest);
    }


    @Override
    public void verifyDelete(String ssafyTransactionNumber) {
        accountRepository.deletetVerify(ssafyTransactionNumber);
    }


    @Override
    public Map<String, String> accountBalanceMemberId(Long memberId) {
        String apiKey = AccountUtils.getApiKey();

        MemberGetResponse memberGetResponse = memberClient.getMember(memberId);
        String userKey = memberGetResponse.getUserKey();
        String accountNum = memberGetResponse.getAccountNumber();

        AccountSelectApiRequest accountSelectApiRequest = new AccountSelectApiRequest();
        accountSelectApiRequest.getHeader().setApiKey(apiKey);
        accountSelectApiRequest.getHeader().setUserKey(userKey);
        accountSelectApiRequest.setAccountNo(accountNum);

        // Feign Client로 잔고 조회
        AccountSelectBalanceApiResponse balanceResponse = selectAccountNumFeignClient.selectAccountBalance(accountSelectApiRequest);
        Map<String, String> map = new HashMap<>();
        map.put("accountNo", accountNum);
        map.put("balance", balanceResponse.getRec().getAccountBalance());

        return map;
    }

    @Override
    public List<AccountHistoryEntity> accountHistoryByDate(String clubCode, String date) {
        String accountNum = accountRepository.selectAccountNum(clubCode);
        return accountRepository.selectAccountNumByDate(accountNum, date);
    }
}
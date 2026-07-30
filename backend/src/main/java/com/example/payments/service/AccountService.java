package com.example.payments.service;

import java.util.List;

import com.example.payments.dto.response.AccountBalanceResponse;

/**
 * 账户查询服务接口。
 */
public interface AccountService {

  AccountBalanceResponse getAccountBalance(String accountNo);

  List<AccountBalanceResponse> listAccounts(String keyword);
}
package com.example.payments.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.payments.dto.response.AccountBalanceResponse;
import com.example.payments.dto.response.ApiResponse;
import com.example.payments.service.AccountService;

/**
 * 账户查询控制器。
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  public ApiResponse<List<AccountBalanceResponse>> listAccounts(
      @RequestParam(required = false) String keyword) {
    List<AccountBalanceResponse> response = accountService.listAccounts(keyword);
    return ApiResponse.ok(response);
  }

  @GetMapping("/{accountNo}/balance")
  public ApiResponse<AccountBalanceResponse> getAccountBalance(@PathVariable String accountNo) {
    AccountBalanceResponse response = accountService.getAccountBalance(accountNo);
    return ApiResponse.ok(response);
  }
}
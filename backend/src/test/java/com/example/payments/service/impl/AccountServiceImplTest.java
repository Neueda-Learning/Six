package com.example.payments.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.payments.dto.response.AccountBalanceResponse;
import com.example.payments.entity.Account;
import com.example.payments.enums.ErrorCode;
import com.example.payments.exception.PaymentException;
import com.example.payments.mapper.AccountMapper;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

  @Mock
  private AccountMapper accountMapper;

  private AccountServiceImpl accountService;

  @BeforeEach
  void setUp() {
    accountService = new AccountServiceImpl(accountMapper);
  }

  @Test
  void getAccountBalance_existingAccount_returnsBalanceSnapshot() {
    Account account = new Account();
    account.setAccountNo("ACC10001");
    account.setOwnerName("Alice Zhang");
    account.setCurrency("USD");
    account.setStatus("ACTIVE");
    account.setBalance(new BigDecimal("99988.89"));
    when(accountMapper.selectById("ACC10001")).thenReturn(account);

    AccountBalanceResponse response = accountService.getAccountBalance("ACC10001");

    assertEquals("ACC10001", response.getAccountNo());
    assertEquals("Alice Zhang", response.getOwnerName());
    assertEquals("USD", response.getCurrency());
    assertEquals("ACTIVE", response.getStatus());
    assertEquals(new BigDecimal("99988.89"), response.getBalance());
  }

  @Test
  void getAccountBalance_missingAccount_throwsNotFound() {
    when(accountMapper.selectById("ACC99999")).thenReturn(null);

    PaymentException ex = assertThrows(PaymentException.class,
        () -> accountService.getAccountBalance("ACC99999"));

    assertEquals(ErrorCode.INVALID_ACCOUNT.name(), ex.getErrorCode());
    assertEquals(404, ex.getHttpStatus().value());
  }

  @Test
  void listAccounts_returnsMappedAccounts() {
    Account first = new Account();
    first.setAccountNo("ACC10001");
    first.setOwnerName("Alice Zhang");
    first.setCurrency("USD");
    first.setStatus("ACTIVE");
    first.setBalance(new BigDecimal("99988.89"));

    Account second = new Account();
    second.setAccountNo("ACC20001");
    second.setOwnerName("David Li");
    second.setCurrency("USD");
    second.setStatus("ACTIVE");
    second.setBalance(new BigDecimal("100011.11"));

    when(accountMapper.selectList(any())).thenReturn(List.of(first, second));

    List<AccountBalanceResponse> response = accountService.listAccounts("ACC");

    assertEquals(2, response.size());
    assertEquals("ACC10001", response.get(0).getAccountNo());
    assertEquals(new BigDecimal("99988.89"), response.get(0).getBalance());
    assertEquals("ACC20001", response.get(1).getAccountNo());
  }

  // 不传关键字（对应前端账户余额页初次打开、尚未输入搜索词的场景），应返回 Mapper 提供的全部账户，顺序保持不变
  @Test
  void listAccounts_blankKeyword_returnsAllAccountsInMapperOrder() {
    Account first = new Account();
    first.setAccountNo("ACC10001");
    first.setOwnerName("Alice Zhang");
    first.setCurrency("USD");
    first.setStatus("ACTIVE");
    first.setBalance(new BigDecimal("1500.00"));

    Account second = new Account();
    second.setAccountNo("ACC10002");
    second.setOwnerName("Bob Chen");
    second.setCurrency("EUR");
    second.setStatus("ACTIVE");
    second.setBalance(new BigDecimal("800.00"));

    when(accountMapper.selectList(any())).thenReturn(List.of(first, second));

    List<AccountBalanceResponse> response = accountService.listAccounts(null);

    assertEquals(2, response.size());
    assertEquals("ACC10001", response.get(0).getAccountNo());
    assertEquals("ACC10002", response.get(1).getAccountNo());
  }

  // 关键字为户主名（而非账户号），服务层应正确转发查询并映射结果——覆盖“按关键字可查到对应账户”里
  // “关键字”不仅限于账户号这一种输入形式的场景
  @Test
  void listAccounts_keywordMatchesOwnerName_returnsMatchedAccount() {
    Account matched = new Account();
    matched.setAccountNo("ACC10003");
    matched.setOwnerName("Cindy Wang");
    matched.setCurrency("GBP");
    matched.setStatus("ACTIVE");
    matched.setBalance(new BigDecimal("300.50"));

    when(accountMapper.selectList(any())).thenReturn(List.of(matched));

    List<AccountBalanceResponse> response = accountService.listAccounts("Cindy");

    assertEquals(1, response.size());
    assertEquals("ACC10003", response.get(0).getAccountNo());
    assertEquals("Cindy Wang", response.get(0).getOwnerName());
    assertEquals(new BigDecimal("300.50"), response.get(0).getBalance());
  }

  // 关键字未命中任何账户，应返回空列表而不是抛异常，前端页面据此展示“暂无数据”的空状态
  @Test
  void listAccounts_keywordNoMatch_returnsEmptyList() {
    when(accountMapper.selectList(any())).thenReturn(List.of());

    List<AccountBalanceResponse> response = accountService.listAccounts("does-not-exist");

    assertEquals(0, response.size());
  }
}
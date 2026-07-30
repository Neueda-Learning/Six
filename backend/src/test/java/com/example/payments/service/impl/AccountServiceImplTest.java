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
}
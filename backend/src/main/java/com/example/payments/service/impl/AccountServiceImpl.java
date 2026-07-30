package com.example.payments.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payments.dto.response.AccountBalanceResponse;
import com.example.payments.entity.Account;
import com.example.payments.enums.ErrorCode;
import com.example.payments.exception.PaymentException;
import com.example.payments.mapper.AccountMapper;
import com.example.payments.service.AccountService;

/**
 * 账户查询服务默认实现。
 */
@Service
public class AccountServiceImpl implements AccountService {

  private final AccountMapper accountMapper;

  public AccountServiceImpl(AccountMapper accountMapper) {
    this.accountMapper = accountMapper;
  }

  @Override
  public AccountBalanceResponse getAccountBalance(String accountNo) {
    Account account = accountMapper.selectById(accountNo);
    if (account == null) {
      throw new PaymentException(ErrorCode.INVALID_ACCOUNT.name(), "账户不存在: " + accountNo,
          HttpStatus.NOT_FOUND);
    }

    return toResponse(account);
  }

  @Override
  public List<AccountBalanceResponse> listAccounts(String keyword) {
    LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.hasText(keyword)) {
      String trimmedKeyword = keyword.trim();
      wrapper.and(qw -> qw.like(Account::getAccountNo, trimmedKeyword)
          .or()
          .like(Account::getOwnerName, trimmedKeyword));
    }
    wrapper.orderByAsc(Account::getAccountNo);

    return accountMapper.selectList(wrapper).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  private AccountBalanceResponse toResponse(Account account) {
    AccountBalanceResponse response = new AccountBalanceResponse();
    response.setAccountNo(account.getAccountNo());
    response.setOwnerName(account.getOwnerName());
    response.setCurrency(account.getCurrency());
    response.setStatus(account.getStatus());
    response.setBalance(account.getBalance());
    return response;
  }
}
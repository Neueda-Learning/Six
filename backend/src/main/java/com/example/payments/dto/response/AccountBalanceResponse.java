package com.example.payments.dto.response;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 账户余额查询响应 DTO。
 * 该对象用于向前端返回指定账户当前的余额快照及基础账户信息。
 */
@Data
public class AccountBalanceResponse {

  private String accountNo;
  private String ownerName;
  private String currency;
  private String status;
  private BigDecimal balance;
}
// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 账户表对应的实体对象。
 * 该实体用于表示本地模拟账户信息，支持支付发起前的账户存在性、币种和状态校验，
 * 同时为前端或调试场景提供基础账户展示数据。
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("accounts")
public class Account {

  @TableId
  private String accountNo;
  private String ownerName;
  private String currency;
  private String status;
  private LocalDateTime createdAt;
}
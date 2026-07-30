// 该文件用于定义账户模拟数据的数据访问接口，供支付创建时的账户存在性校验查询使用。
package com.example.payments.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.payments.entity.Account;

/**
 * 账户模拟数据表的数据访问接口。
 * 基于 MyBatis-Plus 的 BaseMapper，主要用于按账户号码（account_no）查询账户是否存在，
 * 暂不需要额外的自定义查询方法。
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

  @Update("""
      UPDATE accounts
      SET balance = balance - #{amount}
      WHERE account_no = #{accountNo}
        AND balance >= #{amount}
      """)
  int debitBalance(@Param("accountNo") String accountNo, @Param("amount") BigDecimal amount);

  @Update("""
      UPDATE accounts
      SET balance = balance + #{amount}
      WHERE account_no = #{accountNo}
      """)
  int creditBalance(@Param("accountNo") String accountNo, @Param("amount") BigDecimal amount);
}

package com.example.payments.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.payments.config.PaymentProperties;
import com.example.payments.entity.Account;
import com.example.payments.enums.ErrorCode;
import com.example.payments.exception.PaymentException;
import com.example.payments.mapper.AccountMapper;

/**
 * 覆盖 test-cases.md 第二章（金额校验 TC-05~TC-11）与第四章（账户校验 TC-15~TC-18）。
 * PaymentValidator 不依赖 Spring 容器，直接 new 出来配合 Mockito mock AccountMapper 即可测试。
 */
@ExtendWith(MockitoExtension.class)
class PaymentValidatorTest {

    @Mock
    private AccountMapper accountMapper;

    private PaymentValidator paymentValidator;

    @BeforeEach
    void setUp() {
        PaymentProperties paymentProperties = new PaymentProperties();
        paymentProperties.setSupportedCurrencies(List.of("USD", "EUR", "GBP"));
        paymentProperties.setNetworkMaxRetry(3);
        paymentValidator = new PaymentValidator(accountMapper, paymentProperties);
    }

    // ---------- 第二章：金额校验 ----------

    // TC-05/06/07/09：金额为 0、负数、达到上限临界值、小数位超过两位，均应抛出 INVALID_AMOUNT
    @ParameterizedTest(name = "amount={0} 应抛出 INVALID_AMOUNT")
    @ValueSource(strings = {"0", "-10", "1000000", "10.123"})
    void validateAmount_invalidValues_throwsInvalidAmount(String amountStr) {
        BigDecimal amount = new BigDecimal(amountStr);

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentValidator.validate(amount, "USD", "ACC10001", "ACC20001"));

        assertEquals(ErrorCode.INVALID_AMOUNT.name(), ex.getErrorCode());
    }

    // TC-11：amount 为 null，应抛出 INVALID_AMOUNT
    @Test
    void validateAmount_null_throwsInvalidAmount() {
        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentValidator.validate(null, "USD", "ACC10001", "ACC20001"));

        assertEquals(ErrorCode.INVALID_AMOUNT.name(), ex.getErrorCode());
    }

    // TC-08/10：金额刚好小于上限、金额为极小正数，均应通过校验（不抛异常）
    @ParameterizedTest(name = "amount={0} 应通过校验")
    @ValueSource(strings = {"999999.99", "0.01"})
    void validateAmount_validBoundaryValues_passes(String amountStr) {
        BigDecimal amount = new BigDecimal(amountStr);
        when(accountMapper.selectById("ACC10001")).thenReturn(new Account());
        when(accountMapper.selectById("ACC20001")).thenReturn(new Account());

        assertDoesNotThrow(() -> paymentValidator.validate(amount, "USD", "ACC10001", "ACC20001"));
    }

    // ---------- 第四章：账户校验 ----------

    // TC-15：源账户与目标账户相同，应抛出 INVALID_ACCOUNT（在到达账户存在性校验之前就被拦截）
    @Test
    void validateAccounts_sameAccount_throwsInvalidAccount() {
        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentValidator.validate(new BigDecimal("100"), "USD", "ACC10001", "ACC10001"));

        assertEquals(ErrorCode.INVALID_ACCOUNT.name(), ex.getErrorCode());
    }

    // TC-16：源账户不存在，应抛出 INVALID_ACCOUNT
    @Test
    void validateAccounts_fromAccountNotExist_throwsInvalidAccount() {
        when(accountMapper.selectById("ACC99999")).thenReturn(null);

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentValidator.validate(new BigDecimal("100"), "USD", "ACC99999", "ACC20001"));

        assertEquals(ErrorCode.INVALID_ACCOUNT.name(), ex.getErrorCode());
    }

    // TC-17：目标账户不存在，应抛出 INVALID_ACCOUNT
    @Test
    void validateAccounts_toAccountNotExist_throwsInvalidAccount() {
        when(accountMapper.selectById("ACC10001")).thenReturn(new Account());
        when(accountMapper.selectById("ACC99999")).thenReturn(null);

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentValidator.validate(new BigDecimal("100"), "USD", "ACC10001", "ACC99999"));

        assertEquals(ErrorCode.INVALID_ACCOUNT.name(), ex.getErrorCode());
    }

    // TC-18：金额与账户同时非法时，校验顺序为“金额 -> 币种 -> 账户”，应优先报 INVALID_AMOUNT
    @Test
    void validate_amountAndAccountBothInvalid_amountErrorTakesPriority() {
        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentValidator.validate(BigDecimal.ZERO, "USD", "ACC10001", "ACC10001"));

        assertEquals(ErrorCode.INVALID_AMOUNT.name(), ex.getErrorCode());
    }

    // ---------- 余额充足性校验（转账新功能） ----------

    // 账户存在且余额 >= 支付金额，应返回 true
    @Test
    void hasSufficientBalance_balanceGreaterOrEqualToAmount_returnsTrue() {
        Account account = new Account();
        account.setBalance(new BigDecimal("500.00"));
        when(accountMapper.selectById("ACC10001")).thenReturn(account);

        assertEquals(true, paymentValidator.hasSufficientBalance("ACC10001", new BigDecimal("500.00")));
    }

    // 余额小于支付金额，应返回 false
    @Test
    void hasSufficientBalance_balanceLessThanAmount_returnsFalse() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.00"));
        when(accountMapper.selectById("ACC10002")).thenReturn(account);

        assertEquals(false, paymentValidator.hasSufficientBalance("ACC10002", new BigDecimal("8000.00")));
    }

    // 账户不存在，应返回 false（而不是抛异常）
    @Test
    void hasSufficientBalance_accountNotExist_returnsFalse() {
        when(accountMapper.selectById("ACC99999")).thenReturn(null);

        assertEquals(false, paymentValidator.hasSufficientBalance("ACC99999", new BigDecimal("100.00")));
    }

    // 账户存在但 balance 字段为 null（如未执行余额初始化脚本），应返回 false 而不是抛 NPE
    @Test
    void hasSufficientBalance_balanceFieldNull_returnsFalse() {
        Account account = new Account();
        account.setBalance(null);
        when(accountMapper.selectById("ACC10003")).thenReturn(account);

        assertEquals(false, paymentValidator.hasSufficientBalance("ACC10003", new BigDecimal("1.00")));
    }
}

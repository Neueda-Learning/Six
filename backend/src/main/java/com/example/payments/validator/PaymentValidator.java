// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.validator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.example.payments.config.PaymentProperties;
import com.example.payments.dto.request.CreatePaymentRequest;
import com.example.payments.entity.Account;
import com.example.payments.enums.ErrorCode;
import com.example.payments.exception.PaymentException;
import com.example.payments.mapper.AccountMapper;

/**
 * 支付请求领域校验组件。
 * 该类用于补充注解校验之外的业务规则校验，例如金额范围、账户合法性、币种支持范围、
 * 源账户与目标账户是否冲突等。
 * 这些校验通常发生在服务层正式处理支付之前，用于尽早拦截无效请求。
 */
@Component
public class PaymentValidator {

    // 金额下限：必须大于 0（DTO 上的 @DecimalMin("0.01") 已做基础拦截，这里再显式复核业务口径）
    private static final BigDecimal MIN_AMOUNT = BigDecimal.ZERO;

    // 金额上限：单笔交易金额不得达到或超过 1,000,000（严格小于该值）
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    // 金额允许的最大小数位数
    private static final int MAX_AMOUNT_SCALE = 2;

    private final AccountMapper accountMapper;
    private final PaymentProperties paymentProperties;

    public PaymentValidator(AccountMapper accountMapper, PaymentProperties paymentProperties) {
        this.accountMapper = accountMapper;
        this.paymentProperties = paymentProperties;
    }

    /**
     * 校验创建支付请求是否满足业务规则。
     * 按“金额 -&gt; 币种 -&gt; 账户”的顺序依次校验，任意一项不满足立即抛出对应错误码的异常，
     * 由全局异常处理器统一转换为标准错误响应。
     *
     * @param request 创建支付请求参数
     */
    public void validateCreateRequest(CreatePaymentRequest request) {
        validate(request.getAmount(), request.getCurrency(), request.getFromAccount(), request.getToAccount());
    }

    /**
     * 支付三层业务规则校验的核心方法：金额校验、币种校验、账户校验。
     * 之所以抽出该方法，是为了让“创建支付”等不同入口都能复用同一套校验逻辑，避免规则重复实现。
     *
     * @param amount      支付金额
     * @param currency    货币代码
     * @param fromAccount 源账户（付款账户）
     * @param toAccount   目标账户（收款账户）
     */
    public void validate(BigDecimal amount, String currency, String fromAccount, String toAccount) {
        validateAmount(amount);
        validateCurrency(currency);
        validateAccounts(fromAccount, toAccount);
    }

    /**
     * 金额校验：必须大于 0，不得达到或超过单笔限额，且小数位数不得超过两位。
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_AMOUNT) <= 0) {
            throw new PaymentException(ErrorCode.INVALID_AMOUNT.name(), "支付金额必须大于 0", HttpStatus.BAD_REQUEST);
        }
        if (amount.compareTo(MAX_AMOUNT) >= 0) {
            throw new PaymentException(ErrorCode.INVALID_AMOUNT.name(), "支付金额不得超过单笔限额 1,000,000",
                    HttpStatus.BAD_REQUEST);
        }
        if (amount.stripTrailingZeros().scale() > MAX_AMOUNT_SCALE) {
            throw new PaymentException(ErrorCode.INVALID_AMOUNT.name(), "支付金额最多保留两位小数",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 币种校验：必须在系统配置的受支持货币白名单内（application.yml 中的
     * payments.supported-currencies）。
     */
    private void validateCurrency(String currency) {
        List<String> supportedCurrencies = paymentProperties.getSupportedCurrencies();
        String normalized = currency == null ? null : currency.toUpperCase(Locale.ROOT);
        if (normalized == null || supportedCurrencies == null || !supportedCurrencies.contains(normalized)) {
            throw new PaymentException(ErrorCode.INVALID_CURRENCY.name(), "不支持的货币类型: " + currency,
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 账户校验：源账户与目标账户不能相同，且两者都必须在账户模拟数据（accounts 表）中真实存在。
     */
    private void validateAccounts(String fromAccount, String toAccount) {
        if (fromAccount != null && fromAccount.equals(toAccount)) {
            throw new PaymentException(ErrorCode.INVALID_ACCOUNT.name(), "源账户与目标账户不能相同",
                    HttpStatus.BAD_REQUEST);
        }
        if (accountMapper.selectById(fromAccount) == null) {
            throw new PaymentException(ErrorCode.INVALID_ACCOUNT.name(), "源账户不存在: " + fromAccount,
                    HttpStatus.BAD_REQUEST);
        }
        if (accountMapper.selectById(toAccount) == null) {
            throw new PaymentException(ErrorCode.INVALID_ACCOUNT.name(), "目标账户不存在: " + toAccount,
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 余额充足性只读校验：仅在支付状态由 CREATED 流转到 VALIDATED 时调用，
     * 判断源账户当前余额是否足以支付本次金额。
     * 该方法只读取余额用于判断，不做任何扣款/冻结等资金变动，账户余额自始至终保持不变。
     *
     * @param fromAccount 源账户号
     * @param amount      本次支付金额
     * @return true 表示余额充足（大于等于支付金额），false 表示余额不足或账户/余额数据缺失
     */
    public boolean hasSufficientBalance(String fromAccount, BigDecimal amount) {
        Account account = accountMapper.selectById(fromAccount);
        if (account == null || account.getBalance() == null || amount == null) {
            return false;
        }
        return account.getBalance().compareTo(amount) >= 0;
    }
}


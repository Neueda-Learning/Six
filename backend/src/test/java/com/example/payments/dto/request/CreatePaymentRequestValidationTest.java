package com.example.payments.dto.request;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * 覆盖 test-cases.md 第三章（币种格式校验 TC-13/TC-14）与第五章（幂等键格式校验 TC-22/TC-23）。
 *
 * 这几个用例对应的 400 VALIDATION_FAILED 并不是来自 PaymentValidator 的业务规则判断（那部分见
 * PaymentValidatorTest），而是 Jakarta Bean Validation 在 Controller 入参上触发的注解校验
 * （@Size/@NotBlank），由 Spring 的 MethodArgumentNotValidException -> GlobalExceptionHandler
 * 转换成 VALIDATION_FAILED。这里直接用 jakarta.validation.Validator 对 DTO 做校验，
 * 不依赖 Spring 容器或真实 HTTP 请求，属于纯单元测试。
 */
class CreatePaymentRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    /** 构造一个各字段均合法的请求，作为逐项修改单个字段进行反证的基准 */
    private CreatePaymentRequest validRequest() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setIdempotencyKey("valid-idempotency-key-001");
        request.setFromAccount("ACC10001");
        request.setToAccount("ACC20001");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        return request;
    }

    // ---------- 第三章：币种格式校验 ----------

    // TC-13：currency 非 3 位字母（如 "US"、"USDX"），@Size(min=3,max=3) 应拦截
    @Test
    void currency_wrongLength_violatesSizeConstraint() {
        CreatePaymentRequest tooShort = validRequest();
        tooShort.setCurrency("US");
        assertTrue(hasViolationOn(tooShort, "currency"));

        CreatePaymentRequest tooLong = validRequest();
        tooLong.setCurrency("USDX");
        assertTrue(hasViolationOn(tooLong, "currency"));
    }

    // TC-14：currency 为空字符串，@NotBlank（叠加 @Size）应拦截
    @Test
    void currency_blank_violatesNotBlankConstraint() {
        CreatePaymentRequest request = validRequest();
        request.setCurrency("");

        assertTrue(hasViolationOn(request, "currency"));
    }

    // ---------- 第五章：幂等键格式校验 ----------

    // TC-22：idempotencyKey 超过 64 字符（65 位），@Size(max=64) 应拦截
    @Test
    void idempotencyKey_tooLong_violatesSizeConstraint() {
        CreatePaymentRequest request = validRequest();
        request.setIdempotencyKey("k".repeat(65));

        assertTrue(hasViolationOn(request, "idempotencyKey"));
    }

    // TC-23：idempotencyKey 为空字符串，@NotBlank 应拦截
    @Test
    void idempotencyKey_blank_violatesNotBlankConstraint() {
        CreatePaymentRequest request = validRequest();
        request.setIdempotencyKey("");

        assertTrue(hasViolationOn(request, "idempotencyKey"));
    }

    // 对照组：字段均合法的请求不应产生任何校验错误
    @Test
    void validRequest_hasNoViolations() {
        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(validRequest());

        assertTrue(violations.isEmpty());
    }

    /** 校验给定请求，判断指定字段路径上是否存在约束违反 */
    private boolean hasViolationOn(CreatePaymentRequest request, String propertyName) {
        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(propertyName));
    }
}

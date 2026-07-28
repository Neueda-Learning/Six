package com.example.payments.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.payments.dto.request.UpdatePaymentStatusRequest;
import com.example.payments.dto.response.PaymentResponse;
import com.example.payments.entity.Payment;
import com.example.payments.entity.PaymentStatusHistory;
import com.example.payments.enums.ErrorCode;
import com.example.payments.enums.PaymentStatus;
import com.example.payments.exception.PaymentException;
import com.example.payments.mapper.PaymentMapper;
import com.example.payments.mapper.PaymentStatusHistoryMapper;
import com.example.payments.statemachine.PaymentStateMachine;
import com.example.payments.validator.PaymentValidator;

/**
 * 覆盖 test-cases.md 第六章的 TC-33/TC-34（updatePaymentStatus 的目标状态解析）
 * 与第八章的 TC-45/TC-46（并发与乐观锁）。
 * 全部使用 Mockito mock 数据访问层与状态机，不连接真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentStatusHistoryMapper paymentStatusHistoryMapper;

    @Mock
    private PaymentValidator paymentValidator;

    @Mock
    private PaymentStateMachine paymentStateMachine;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentMapper, paymentStatusHistoryMapper, paymentValidator,
                paymentStateMachine);
    }

    private Payment existingPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(status.name());
        payment.setFromAccount("ACC10001");
        payment.setToAccount("ACC20001");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setVersion(0);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }

    // ---------- 第六章：状态解析（TC-33/34） ----------

    // TC-33：targetStatus 传入非法字符串，应抛出 VALIDATION_FAILED，且不应到达状态机判断
    @Test
    void updatePaymentStatus_invalidTargetStatusString_throwsValidationFailed() {
        when(paymentMapper.selectById(1L)).thenReturn(existingPayment(PaymentStatus.CREATED));

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("XXXX");

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.updatePaymentStatus(1L, request));

        assertEquals(ErrorCode.VALIDATION_FAILED.name(), ex.getErrorCode());
        verifyNoInteractions(paymentStateMachine);
    }

    // TC-34：targetStatus 大小写不敏感，"validated" 应正确解析为 VALIDATED 并成功流转
    @Test
    void updatePaymentStatus_lowerCaseTargetStatus_parsesAndTransitionsSuccessfully() {
        when(paymentMapper.selectById(1L)).thenReturn(existingPayment(PaymentStatus.CREATED));
        when(paymentStateMachine.canTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED)).thenReturn(true);
        // CREATED -> VALIDATED 会触发余额校验，本用例只关心状态解析逻辑，因此固定桩为余额充足
        when(paymentValidator.hasSufficientBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(true);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("validated");

        PaymentResponse response = paymentService.updatePaymentStatus(1L, request);

        assertEquals("VALIDATED", response.getStatus());
        verify(paymentMapper).updateById(any(Payment.class));
        verify(paymentStatusHistoryMapper).insert(any(PaymentStatusHistory.class));
    }

    // ---------- 第八章：并发与乐观锁（TC-45/46） ----------

    /**
     * TC-45：模拟乐观锁版本冲突场景 —— MyBatis-Plus 乐观锁插件在 UPDATE 语句的 WHERE version=? 未命中时，
     * updateById 会返回受影响行数 0，但不会抛出异常。
     *
     * ⚠️ 已知缺口（见 test-cases.md 待确认事项）：当前 applyStatusTransition 完全没有检查 updateById 的返回值，
     * 因此即使发生版本冲突，服务层也会“当作成功”继续写历史并返回更新后的响应。
     * 本测试如实记录当前的真实行为，而不是臆造一个“应该抛异常”的期望；
     * 后续若修复该缺口（例如根据返回值抛出 PROCESSING_ERROR 或专门的版本冲突错误码），需要同步更新本测试。
     */
    @Test
    void updatePaymentStatus_optimisticLockConflict_currentlyNotDetected() {
        when(paymentMapper.selectById(1L)).thenReturn(existingPayment(PaymentStatus.CREATED));
        when(paymentStateMachine.canTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED)).thenReturn(true);
        when(paymentValidator.hasSufficientBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(true);
        // 模拟版本冲突：updateById 返回受影响行数 0（现有实现完全忽略这个返回值）
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(0);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("VALIDATED");

        PaymentResponse response = assertDoesNotThrow(() -> paymentService.updatePaymentStatus(1L, request));

        assertEquals("VALIDATED", response.getStatus());
        verify(paymentStatusHistoryMapper).insert(any(PaymentStatusHistory.class));
    }

    /**
     * TC-46：版本冲突后，客户端重新查询最新状态再重试，应能成功流转。
     * 由于当前服务层没有自动重试机制，这里模拟的是“调用方基于旧快照发起请求失败后，
     * 重新查询拿到最新快照再调用同一接口”的手动重试场景。
     */
    @Test
    void updatePaymentStatus_retryAfterRefetchingLatestVersion_succeeds() {
        Payment staleSnapshot = existingPayment(PaymentStatus.CREATED);
        Payment latestSnapshot = existingPayment(PaymentStatus.VALIDATED);
        latestSnapshot.setVersion(1);

        when(paymentMapper.selectById(1L))
                .thenReturn(staleSnapshot)
                .thenReturn(latestSnapshot);
        when(paymentStateMachine.canTransition(PaymentStatus.CREATED, PaymentStatus.SENT)).thenReturn(false);
        when(paymentStateMachine.canTransition(PaymentStatus.VALIDATED, PaymentStatus.SENT)).thenReturn(true);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("SENT");

        // 第一次调用：基于旧快照（CREATED）请求流转到 SENT，属于非法跳级，应失败
        assertThrows(PaymentException.class, () -> paymentService.updatePaymentStatus(1L, request));

        // 重新查询后拿到最新快照（VALIDATED），再次调用同一接口，应成功流转到 SENT
        PaymentResponse response = paymentService.updatePaymentStatus(1L, request);
        assertEquals("SENT", response.getStatus());
    }

    // ---------- 余额充足性校验（转账新功能）----------

    // CREATED -> VALIDATED 手动流转时，若源账户余额真实不足，应强制转为 FAILED/INSUFFICIENT_FUNDS，
    // 而不是按调用方请求的 VALIDATED 继续
    @Test
    void updatePaymentStatus_createdToValidated_insufficientBalance_overridesToFailedInsufficientFunds() {
        when(paymentMapper.selectById(1L)).thenReturn(existingPayment(PaymentStatus.CREATED));
        when(paymentStateMachine.canTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED)).thenReturn(true);
        // applyStatusTransition 内部会对最终实际执行的目标状态（FAILED）再次校验一次状态机合法性
        when(paymentStateMachine.canTransition(PaymentStatus.CREATED, PaymentStatus.FAILED)).thenReturn(true);
        when(paymentValidator.hasSufficientBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(false);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("VALIDATED");

        PaymentResponse response = paymentService.updatePaymentStatus(1L, request);

        assertEquals("FAILED", response.getStatus());
        assertEquals(ErrorCode.INSUFFICIENT_FUNDS.name(), response.getErrorCode());
    }

    // 自动推进调度：CREATED -> VALIDATED 时若余额不足，无论随机失败概率是多少，都必然转为 FAILED/INSUFFICIENT_FUNDS
    @Test
    void autoAdvancePendingPayments_insufficientBalance_forcesFailedRegardlessOfRandomProbability() {
        Payment payment = existingPayment(PaymentStatus.CREATED);
        when(paymentMapper.selectList(any())).thenReturn(List.of(payment));
        when(paymentValidator.hasSufficientBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(false);
        // applyStatusTransition 内部会对最终实际执行的目标状态（FAILED）再次校验一次状态机合法性
        when(paymentStateMachine.canTransition(PaymentStatus.CREATED, PaymentStatus.FAILED)).thenReturn(true);
        // 随机失败概率设为 0（本应“必然不触发随机失败”），验证余额不足这条真实业务规则不受随机概率影响
        ReflectionTestUtils.setField(paymentService, "autoFailureProbability", 0.0);

        int advancedCount = paymentService.autoAdvancePendingPayments();

        assertEquals(1, advancedCount);
        assertEquals("FAILED", payment.getStatus());
        assertEquals(ErrorCode.INSUFFICIENT_FUNDS.name(), payment.getErrorCode());
    }

    // 自动推进调度：余额充足时，CREATED -> VALIDATED 正常推进，不受余额校验影响
    @Test
    void autoAdvancePendingPayments_sufficientBalance_advancesNormally() {
        Payment payment = existingPayment(PaymentStatus.CREATED);
        when(paymentMapper.selectList(any())).thenReturn(List.of(payment));
        when(paymentValidator.hasSufficientBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(true);
        when(paymentStateMachine.canTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED)).thenReturn(true);
        ReflectionTestUtils.setField(paymentService, "autoFailureProbability", 0.0);

        int advancedCount = paymentService.autoAdvancePendingPayments();

        assertEquals(1, advancedCount);
        assertEquals("VALIDATED", payment.getStatus());
    }
}

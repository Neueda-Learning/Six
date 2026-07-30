package com.example.payments.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.dao.DuplicateKeyException;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.payments.dto.request.CreatePaymentRequest;
import com.example.payments.dto.request.UpdatePaymentStatusRequest;
import com.example.payments.dto.response.PageResponse;
import com.example.payments.dto.response.PaymentHistoryItemResponse;
import com.example.payments.dto.response.PaymentResponse;
import com.example.payments.entity.Payment;
import com.example.payments.entity.PaymentStatusHistory;
import com.example.payments.enums.ErrorCode;
import com.example.payments.enums.PaymentStatus;
import com.example.payments.exception.PaymentException;
import com.example.payments.mapper.AccountMapper;
import com.example.payments.mapper.PaymentMapper;
import com.example.payments.mapper.PaymentStatusHistoryMapper;
import com.example.payments.statemachine.PaymentStateMachine;
import com.example.payments.validator.PaymentValidator;

/**
 * 覆盖 test-cases.md 第一章（创建支付 Happy Path TC-01~TC-04）、第五章（幂等性 TC-19~TC-21，
 * TC-22/23 的 DTO 格式校验见 CreatePaymentRequestValidationTest）、第六章（TC-33/TC-34）、
 * 第七章（查询类接口 TC-35~TC-44）、第八章（并发与乐观锁 TC-45/46）与第九章（TC-47，
 * TC-48 见 PaymentPropertiesTest）。
 * 全部使用 Mockito mock 数据访问层与状态机，不连接真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentStatusHistoryMapper paymentStatusHistoryMapper;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private PaymentValidator paymentValidator;

    @Mock
    private PaymentStateMachine paymentStateMachine;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentMapper, paymentStatusHistoryMapper, accountMapper,
                paymentValidator,
                paymentStateMachine);
        lenient().when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);
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

    private CreatePaymentRequest createPaymentRequest(String idempotencyKey, String currency) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setIdempotencyKey(idempotencyKey);
        request.setFromAccount("ACC10001");
        request.setToAccount("ACC20002");
        request.setAmount(new BigDecimal("100.50"));
        request.setCurrency(currency);
        request.setRemark("test remark");
        return request;
    }

    // ---------- 第一章：创建支付 Happy Path（TC-01~TC-04） ----------

    // TC-01：正常创建支付，应返回 CREATED 状态，并写入一条 fromStatus=null -> toStatus=CREATED 的初始历史记录
    @Test
    void createPayment_newRequest_createsWithCreatedStatusAndInitialHistory() {
        when(paymentMapper.selectOne(any())).thenReturn(null);

        PaymentResponse response = paymentService.createPayment(createPaymentRequest("idem-tc01", "USD"));

        assertEquals("CREATED", response.getStatus());
        assertEquals("USD", response.getCurrency());
        assertNull(response.getErrorCode());
        verify(paymentMapper).insert(any(Payment.class));

        ArgumentCaptor<PaymentStatusHistory> historyCaptor = ArgumentCaptor.forClass(PaymentStatusHistory.class);
        verify(paymentStatusHistoryMapper).insert(historyCaptor.capture());
        PaymentStatusHistory history = historyCaptor.getValue();
        assertNull(history.getFromStatus());
        assertEquals("CREATED", history.getToStatus());
        assertEquals("SYSTEM", history.getOperator());
    }

    /**
     * TC-02：创建后依次调用状态流转，完整走完 CREATED -> VALIDATED -> SENT -> COMPLETED。
     * 由于是纯单元测试（不启动后台自动推进调度器），这里手动串联 3 次 updatePaymentStatus 调用，
     * 并让 selectById 始终返回同一个 Payment 对象引用（会被前一步 mutate），
     * 模拟数据库里状态被逐步更新、后续查询总能读到最新状态的效果。
     */
    @Test
    void createPayment_thenManualTransitions_reachesCompletedWithFullHistory() {
        when(paymentMapper.selectOne(any())).thenReturn(null);
        PaymentResponse created = paymentService.createPayment(createPaymentRequest("idem-tc02", "USD"));
        assertEquals("CREATED", created.getStatus());

        Payment mutablePayment = existingPayment(PaymentStatus.CREATED);
        when(paymentMapper.selectById(1L)).thenReturn(mutablePayment);
        when(paymentStateMachine.canTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED)).thenReturn(true);
        when(paymentStateMachine.canTransition(PaymentStatus.VALIDATED, PaymentStatus.SENT)).thenReturn(true);
        when(paymentStateMachine.canTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED)).thenReturn(true);
        when(paymentValidator.hasSufficientBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(true);
        when(accountMapper.debitBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(1);
        when(accountMapper.creditBalance("ACC20001", new BigDecimal("100.00"))).thenReturn(1);

        UpdatePaymentStatusRequest toValidated = new UpdatePaymentStatusRequest();
        toValidated.setTargetStatus("VALIDATED");
        assertEquals("VALIDATED", paymentService.updatePaymentStatus(1L, toValidated).getStatus());

        UpdatePaymentStatusRequest toSent = new UpdatePaymentStatusRequest();
        toSent.setTargetStatus("SENT");
        assertEquals("SENT", paymentService.updatePaymentStatus(1L, toSent).getStatus());

        UpdatePaymentStatusRequest toCompleted = new UpdatePaymentStatusRequest();
        toCompleted.setTargetStatus("COMPLETED");
        PaymentResponse finalResponse = paymentService.updatePaymentStatus(1L, toCompleted);

        assertEquals("COMPLETED", finalResponse.getStatus());
        // 1 次创建历史 + 3 次流转历史 = 4 条完整时间线
        verify(paymentStatusHistoryMapper, times(4)).insert(any(PaymentStatusHistory.class));
    }

    // TC-03：三种受支持币种（USD/EUR/GBP）均应创建成功
    @ParameterizedTest(name = "currency={0} 应创建成功")
    @ValueSource(strings = { "USD", "EUR", "GBP" })
    void createPayment_supportedCurrencies_allSucceed(String currency) {
        when(paymentMapper.selectOne(any())).thenReturn(null);

        PaymentResponse response = paymentService
                .createPayment(createPaymentRequest("idem-tc03-" + currency, currency));

        assertEquals("CREATED", response.getStatus());
        assertEquals(currency, response.getCurrency());
    }

    // TC-04：currency 小写输入应自动归一化为大写存储。paymentValidator 在本测试类中是 mock，
    // 不会真的拦截任何币种，因此这里专注测试 createPayment 自身的大小写归一化逻辑，
    // 与 PaymentValidator 的币种白名单校验（见 PaymentValidatorTest）相互独立。
    @Test
    void createPayment_lowerCaseCurrency_normalizesToUpperCase() {
        when(paymentMapper.selectOne(any())).thenReturn(null);

        PaymentResponse response = paymentService.createPayment(createPaymentRequest("idem-tc04", "usd"));

        assertEquals("USD", response.getCurrency());
    }

    // ---------- 第五章：幂等性（TC-19~TC-21，TC-22/23 见 CreatePaymentRequestValidationTest）
    // ----------

    // TC-19：相同 idempotencyKey 重复提交，应直接返回已存在的记录，不再执行 insert
    @Test
    void createPayment_duplicateIdempotencyKey_returnsExistingRecordWithoutInsert() {
        Payment existing = existingPayment(PaymentStatus.CREATED);
        existing.setIdempotencyKey("idem-tc19");
        when(paymentMapper.selectOne(any())).thenReturn(existing);

        PaymentResponse response = paymentService.createPayment(createPaymentRequest("idem-tc19", "USD"));

        assertEquals(existing.getId(), response.getId());
        verify(paymentMapper, never()).insert(any(Payment.class));
        verify(paymentStatusHistoryMapper, never()).insert(any(PaymentStatusHistory.class));
    }

    /**
     * TC-20：相同 idempotencyKey 但请求体参数不同（金额/币种/账户都变了）。
     * 当前实现按设计只按 key 查重，完全不比对请求体其余字段，因此仍然返回第一次创建时的记录，
     * 而不是报错或按新参数重新创建。本测试如实记录这个已确认的设计行为（详见 test-cases.md TC-20）。
     */
    @Test
    void createPayment_sameKeyDifferentPayload_stillReturnsFirstRecord() {
        Payment existing = existingPayment(PaymentStatus.CREATED);
        existing.setIdempotencyKey("idem-tc20");
        when(paymentMapper.selectOne(any())).thenReturn(existing);

        CreatePaymentRequest differentPayload = createPaymentRequest("idem-tc20", "EUR");
        differentPayload.setAmount(new BigDecimal("999.99"));
        differentPayload.setFromAccount("ACC10003");

        PaymentResponse response = paymentService.createPayment(differentPayload);

        // 返回的是第一次创建时的快照（USD/100.00），而不是本次请求体里的 EUR/999.99
        assertEquals(existing.getAmount(), response.getAmount());
        assertEquals(existing.getCurrency(), response.getCurrency());
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    /**
     * TC-21：并发重复提交场景 —— 第一次查重时记录还不存在（selectOne 返回 null），
     * 但 insert 时触发唯一索引冲突（模拟另一个并发请求抢先插入成功），
     * 服务层应捕获 DuplicateKeyException 并重新查询返回并发写入的那条记录，
     * 而不是让异常直接抛给调用方，也不应该重复写入历史记录。
     */
    @Test
    void createPayment_concurrentDuplicateInsert_recoversByRequeryingExistingRecord() {
        Payment concurrentlyCreated = existingPayment(PaymentStatus.CREATED);
        concurrentlyCreated.setIdempotencyKey("idem-tc21");

        when(paymentMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(concurrentlyCreated);
        when(paymentMapper.insert(any(Payment.class)))
                .thenThrow(new DuplicateKeyException("duplicate idempotency_key"));

        PaymentResponse response = paymentService.createPayment(createPaymentRequest("idem-tc21", "USD"));

        assertEquals(concurrentlyCreated.getId(), response.getId());
        verify(paymentStatusHistoryMapper, never()).insert(any(PaymentStatusHistory.class));
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
     * ⚠️ 已知缺口（见 test-cases.md 待确认事项）：当前 applyStatusTransition 完全没有检查 updateById
     * 的返回值，
     * 因此即使发生版本冲突，服务层也会“当作成功”继续写历史并返回更新后的响应。
     * 本测试如实记录当前的真实行为，而不是臆造一个“应该抛异常”的期望；
     * 后续若修复该缺口（例如根据返回值抛出 PROCESSING_ERROR 或专门的版本冲突错误码），需要同步更新本测试。
     */
    @Test
    void updatePaymentStatus_optimisticLockConflict_currentlyNotDetected() {
        when(paymentMapper.selectById(1L)).thenReturn(existingPayment(PaymentStatus.CREATED));
        when(paymentStateMachine.canTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED)).thenReturn(true);
        when(paymentValidator.hasSufficientBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(true);
        // 模拟版本冲突：updateById 返回受影响行数 0，应转换为并发冲突错误而不是继续写历史
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(0);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("VALIDATED");

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.updatePaymentStatus(1L, request));

        assertEquals(ErrorCode.PROCESSING_ERROR.name(), ex.getErrorCode());
        verify(paymentStatusHistoryMapper, never()).insert(any(PaymentStatusHistory.class));
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

    // 自动推进调度：CREATED -> VALIDATED 时若余额不足，无论随机失败概率是多少，都必然转为
    // FAILED/INSUFFICIENT_FUNDS
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

    @Test
    void updatePaymentStatus_sentToCompleted_transfersBalancesAndCompletes() {
        Payment payment = existingPayment(PaymentStatus.SENT);
        when(paymentMapper.selectById(1L)).thenReturn(payment);
        when(paymentStateMachine.canTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED)).thenReturn(true);
        when(accountMapper.debitBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(1);
        when(accountMapper.creditBalance("ACC20001", new BigDecimal("100.00"))).thenReturn(1);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("COMPLETED");

        PaymentResponse response = paymentService.updatePaymentStatus(1L, request);

        assertEquals("COMPLETED", response.getStatus());
        verify(accountMapper).debitBalance("ACC10001", new BigDecimal("100.00"));
        verify(accountMapper).creditBalance("ACC20001", new BigDecimal("100.00"));
    }

    @Test
    void updatePaymentStatus_sentToCompleted_whenDebitFails_throwsInsufficientFunds() {
        Payment payment = existingPayment(PaymentStatus.SENT);
        when(paymentMapper.selectById(1L)).thenReturn(payment);
        when(paymentStateMachine.canTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED)).thenReturn(true);
        when(accountMapper.debitBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(0);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("COMPLETED");

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.updatePaymentStatus(1L, request));

        assertEquals(ErrorCode.INSUFFICIENT_FUNDS.name(), ex.getErrorCode());
        verify(accountMapper, never()).creditBalance(any(), any());
        verify(paymentStatusHistoryMapper, never()).insert(any(PaymentStatusHistory.class));
    }

    // 扣款成功但入账失败（理论上不该发生，属于极端兼底场景），应抛出 PROCESSING_ERROR，
    // 且不应该写入状态历史（本次流转视为未完成）
    @Test
    void updatePaymentStatus_sentToCompleted_whenCreditFails_throwsProcessingError() {
        Payment payment = existingPayment(PaymentStatus.SENT);
        when(paymentMapper.selectById(1L)).thenReturn(payment);
        when(paymentStateMachine.canTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED)).thenReturn(true);
        when(accountMapper.debitBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(1);
        when(accountMapper.creditBalance("ACC20001", new BigDecimal("100.00"))).thenReturn(0);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("COMPLETED");

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.updatePaymentStatus(1L, request));

        assertEquals(ErrorCode.PROCESSING_ERROR.name(), ex.getErrorCode());
        verify(paymentStatusHistoryMapper, never()).insert(any(PaymentStatusHistory.class));
    }

    /**
     * 转账执行前会先调用 PaymentValidator.validateTransferCurrency 做一次同币种复核；
     * 若该复核判定币种不一致（如账户数据被篡改导致与支付币种不符），应直接抛出异常，
     * 且不应该执行任何扣款/入账操作。
     */
    @Test
    void updatePaymentStatus_sentToCompleted_currencyMismatchDetectedBeforeTransfer_skipsDebitAndCredit() {
        Payment payment = existingPayment(PaymentStatus.SENT);
        when(paymentMapper.selectById(1L)).thenReturn(payment);
        when(paymentStateMachine.canTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED)).thenReturn(true);
        doThrow(new PaymentException(ErrorCode.INVALID_CURRENCY.name(), "源账户、目标账户与支付币种必须一致",
                org.springframework.http.HttpStatus.BAD_REQUEST))
                .when(paymentValidator).validateTransferCurrency("USD", "ACC10001", "ACC20001");

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("COMPLETED");

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.updatePaymentStatus(1L, request));

        assertEquals(ErrorCode.INVALID_CURRENCY.name(), ex.getErrorCode());
        verify(accountMapper, never()).debitBalance(any(), any());
        verify(accountMapper, never()).creditBalance(any(), any());
        verify(paymentStatusHistoryMapper, never()).insert(any(PaymentStatusHistory.class));
    }

    // 正常转账时应确实调用了同币种复核（验证 applyBalanceTransfer 真的执行了这一步，而不是被跳过）
    @Test
    void updatePaymentStatus_sentToCompleted_verifiesTransferCurrencyValidationInvoked() {
        Payment payment = existingPayment(PaymentStatus.SENT);
        when(paymentMapper.selectById(1L)).thenReturn(payment);
        when(paymentStateMachine.canTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED)).thenReturn(true);
        when(accountMapper.debitBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(1);
        when(accountMapper.creditBalance("ACC20001", new BigDecimal("100.00"))).thenReturn(1);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("COMPLETED");

        paymentService.updatePaymentStatus(1L, request);

        verify(paymentValidator).validateTransferCurrency("USD", "ACC10001", "ACC20001");
    }

    @Test
    void autoAdvancePendingPayments_sentToCompleted_transfersBalances() {
        Payment payment = existingPayment(PaymentStatus.SENT);
        when(paymentMapper.selectList(any())).thenReturn(List.of(payment));
        when(paymentStateMachine.canTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED)).thenReturn(true);
        when(accountMapper.debitBalance("ACC10001", new BigDecimal("100.00"))).thenReturn(1);
        when(accountMapper.creditBalance("ACC20001", new BigDecimal("100.00"))).thenReturn(1);
        ReflectionTestUtils.setField(paymentService, "autoFailureProbability", 0.0);

        int advancedCount = paymentService.autoAdvancePendingPayments();

        assertEquals(1, advancedCount);
        assertEquals("COMPLETED", payment.getStatus());
        verify(accountMapper).debitBalance("ACC10001", new BigDecimal("100.00"));
        verify(accountMapper).creditBalance("ACC20001", new BigDecimal("100.00"));
    }

    // ---------- 第七章：查询类接口（TC-35~TC-44） ----------

    // TC-35：查询存在且非 FAILED 的支付详情，errorCode/errorMessage 应为 null，其余字段应完整透传
    @Test
    void getPaymentById_existingNonFailedPayment_returnsFullFieldsWithNullError() {
        Payment payment = existingPayment(PaymentStatus.COMPLETED);
        when(paymentMapper.selectById(1L)).thenReturn(payment);

        PaymentResponse response = paymentService.getPaymentById(1L);

        assertEquals("COMPLETED", response.getStatus());
        assertNull(response.getErrorCode());
        assertNull(response.getErrorMessage());
        assertEquals(payment.getFromAccount(), response.getFromAccount());
        assertEquals(payment.getToAccount(), response.getToAccount());
        assertEquals(payment.getAmount(), response.getAmount());
    }

    // TC-36：查询不存在的支付 ID，应抛出 PAYMENT_NOT_FOUND
    @Test
    void getPaymentById_nonExisting_throwsPaymentNotFound() {
        when(paymentMapper.selectById(999L)).thenReturn(null);

        PaymentException ex = assertThrows(PaymentException.class, () -> paymentService.getPaymentById(999L));

        assertEquals(ErrorCode.PAYMENT_NOT_FOUND.name(), ex.getErrorCode());
    }

    // TC-37：查询不存在支付的历史，应抛出 PAYMENT_NOT_FOUND，且不应该再去查询历史表
    @Test
    void getPaymentHistory_nonExistingPayment_throwsPaymentNotFoundWithoutQueryingHistory() {
        when(paymentMapper.selectById(999L)).thenReturn(null);

        PaymentException ex = assertThrows(PaymentException.class, () -> paymentService.getPaymentHistory(999L));

        assertEquals(ErrorCode.PAYMENT_NOT_FOUND.name(), ex.getErrorCode());
        verify(paymentStatusHistoryMapper, never()).selectList(any());
    }

    // TC-38：查询存在支付的历史，应完整保留 Mapper 返回的顺序映射到响应列表。
    // 真实的“按 createdAt 正序排列”由 SQL ORDER BY 保证，需要真实数据库才能验证；
    // 这里只验证服务层不会打乱/丢失 Mapper 已经排好序返回的数据。
    @Test
    void getPaymentHistory_existingPayment_preservesMapperOrder() {
        when(paymentMapper.selectById(1L)).thenReturn(existingPayment(PaymentStatus.COMPLETED));

        PaymentStatusHistory h1 = new PaymentStatusHistory();
        h1.setId(1L);
        h1.setToStatus("CREATED");
        PaymentStatusHistory h2 = new PaymentStatusHistory();
        h2.setId(2L);
        h2.setFromStatus("CREATED");
        h2.setToStatus("VALIDATED");
        when(paymentStatusHistoryMapper.selectList(any())).thenReturn(List.of(h1, h2));

        List<PaymentHistoryItemResponse> history = paymentService.getPaymentHistory(1L);

        assertEquals(2, history.size());
        assertEquals("CREATED", history.get(0).getToStatus());
        assertEquals("VALIDATED", history.get(1).getToStatus());
    }

    // TC-39：按状态筛选（如 failed，小写输入），验证服务层把 status 归一化为大写后传给了查询条件
    @SuppressWarnings("unchecked")
    @Test
    void listPayments_filterByStatus_passesUppercasedStatusToQueryWrapper() {
        Page<Payment> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        ArgumentCaptor<Wrapper<Payment>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        when(paymentMapper.selectPage(any(), wrapperCaptor.capture())).thenReturn(emptyPage);

        paymentService.listPayments("failed", null, null, null);

        assertParamValuesContain(wrapperCaptor.getValue(), "FAILED");
    }

    // TC-40：按数字关键字筛选，应同时匹配 ID（精确等值）与备注（模糊匹配），即关键字会被解析成 Long 传给 eq 条件
    @SuppressWarnings("unchecked")
    @Test
    void listPayments_numericKeyword_matchesBothIdAndRemark() {
        Page<Payment> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        ArgumentCaptor<Wrapper<Payment>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        when(paymentMapper.selectPage(any(), wrapperCaptor.capture())).thenReturn(emptyPage);

        paymentService.listPayments(null, "1", null, null);

        assertParamValuesContain(wrapperCaptor.getValue(), 1L);
        assertParamValuesContainSubstring(wrapperCaptor.getValue(), "1");
    }

    // TC-41：按非数字关键字筛选，应仅按备注做模糊匹配（不会尝试把关键字解析成 ID）
    @SuppressWarnings("unchecked")
    @Test
    void listPayments_nonNumericKeyword_matchesRemarkOnly() {
        Page<Payment> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        ArgumentCaptor<Wrapper<Payment>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        when(paymentMapper.selectPage(any(), wrapperCaptor.capture())).thenReturn(emptyPage);

        paymentService.listPayments(null, "invoice-2026-07-seed-05", null, null);

        assertParamValuesContainSubstring(wrapperCaptor.getValue(), "invoice-2026-07-seed-05");
    }

    // TC-42：不传 page/size，应回退到默认值 page=1,size=10
    @SuppressWarnings("unchecked")
    @Test
    void listPayments_missingPageAndSize_defaultsToPage1Size10() {
        Page<Payment> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        ArgumentCaptor<Page<Payment>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        when(paymentMapper.selectPage(pageCaptor.capture(), any())).thenReturn(emptyPage);

        PageResponse<PaymentResponse> response = paymentService.listPayments(null, null, null, null);

        assertEquals(1, pageCaptor.getValue().getCurrent());
        assertEquals(10, pageCaptor.getValue().getSize());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getSize());
    }

    // TC-43：page=0、size=负数等非法分页参数，均应回退到默认值（page=1，size=10）
    @SuppressWarnings("unchecked")
    @Test
    void listPayments_invalidPageAndSize_fallsBackToDefaults() {
        Page<Payment> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        ArgumentCaptor<Page<Payment>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        when(paymentMapper.selectPage(pageCaptor.capture(), any())).thenReturn(emptyPage);

        paymentService.listPayments(null, null, 0, -5);

        assertEquals(1, pageCaptor.getValue().getCurrent());
        assertEquals(10, pageCaptor.getValue().getSize());
    }

    /**
     * TC-44：status 传入一个不属于 PaymentStatus 枚举的非法值（如 "UNKNOWN"）。
     * 现有代码里 listPayments 完全没有校验 status 是否为合法枚举值——它只是把字符串大写后
     * 直接塞进查询条件，真实数据库里不会有任何记录匹配该值，因此预期效果是返回空列表而不是报错。
     * 本测试如实验证"不会抛异常"这一当前行为，同时这也是 test-cases.md 里标注的待确认设计点。
     */
    @Test
    void listPayments_unrecognizedStatusValue_doesNotThrowAndYieldsEmptyResult() {
        Page<Payment> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        when(paymentMapper.selectPage(any(), any())).thenReturn(emptyPage);

        PageResponse<PaymentResponse> response = assertDoesNotThrow(
                () -> paymentService.listPayments("UNKNOWN", null, null, null));

        assertTrue(response.getList().isEmpty());
    }

    /** 断言查询条件的绑定参数值中，存在与期望值完全相等的一项（用于 eq 精确匹配的条件） */
    private void assertParamValuesContain(Wrapper<Payment> wrapper, Object expectedValue) {
        AbstractWrapper<Payment, ?, ?> abstractWrapper = (AbstractWrapper<Payment, ?, ?>) wrapper;
        // MyBatis-Plus 的参数值是在构建 SQL 片段时才登记进 paramNameValuePairs 的，
        // 因此必须先触发一次 getSqlSegment()，否则这里读到的参数表始终是空的
        abstractWrapper.getSqlSegment();
        assertTrue(abstractWrapper.getParamNameValuePairs().containsValue(expectedValue),
                () -> "期望的参数值 " + expectedValue + " 未出现在查询条件中，实际值为: "
                        + abstractWrapper.getParamNameValuePairs().values());
    }

    /**
     * 断言查询条件的绑定参数值中，存在包含期望子串的一项（用于 like 模糊匹配的条件——
     * MyBatis-Plus 会把值包装成 "%xxx%" 再存入参数表，所以这里按“包含”而不是“完全相等”判断）
     */
    private void assertParamValuesContainSubstring(Wrapper<Payment> wrapper, String expectedSubstring) {
        AbstractWrapper<Payment, ?, ?> abstractWrapper = (AbstractWrapper<Payment, ?, ?>) wrapper;
        // 同上，必须先触发 getSqlSegment() 才会填充参数表
        abstractWrapper.getSqlSegment();
        boolean found = abstractWrapper.getParamNameValuePairs().values().stream()
                .anyMatch(v -> String.valueOf(v).contains(expectedSubstring));
        assertTrue(found, () -> "期望包含子串 \"" + expectedSubstring + "\" 的参数值未找到，实际值为: "
                + abstractWrapper.getParamNameValuePairs().values());
    }

    // ---------- 第九章：网络失败模拟（TC-47，TC-48 见 PaymentPropertiesTest） ----------

    /**
     * TC-47：手动流转到 FAILED 并携带 NETWORK_ERROR 错误码与描述，
     * 支付快照与历史记录都应正确保存 errorCode/errorMessage。
     */
    @Test
    void updatePaymentStatus_manualTransitionToFailedWithNetworkError_persistsErrorDetails() {
        when(paymentMapper.selectById(1L)).thenReturn(existingPayment(PaymentStatus.SENT));
        when(paymentStateMachine.canTransition(PaymentStatus.SENT, PaymentStatus.FAILED)).thenReturn(true);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setTargetStatus("FAILED");
        request.setErrorCode(ErrorCode.NETWORK_ERROR.name());
        request.setErrorMessage("mock network timeout");

        PaymentResponse response = paymentService.updatePaymentStatus(1L, request);

        assertEquals("FAILED", response.getStatus());
        assertEquals(ErrorCode.NETWORK_ERROR.name(), response.getErrorCode());
        assertEquals("mock network timeout", response.getErrorMessage());

        ArgumentCaptor<PaymentStatusHistory> historyCaptor = ArgumentCaptor.forClass(PaymentStatusHistory.class);
        verify(paymentStatusHistoryMapper).insert(historyCaptor.capture());
        assertEquals(ErrorCode.NETWORK_ERROR.name(), historyCaptor.getValue().getErrorCode());
        assertEquals("mock network timeout", historyCaptor.getValue().getErrorMessage());
    }
}

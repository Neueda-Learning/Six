// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.example.payments.mapper.PaymentMapper;
import com.example.payments.mapper.PaymentStatusHistoryMapper;
import com.example.payments.service.PaymentService;
import com.example.payments.statemachine.PaymentStateMachine;
import com.example.payments.validator.PaymentValidator;

/**
 * 支付业务服务接口的默认实现。
 * 该类承载支付全过程的关键业务逻辑：幂等键检查、参数校验、状态机控制、
 * 支付主表与历史表写入、分页查询以及手动状态流转。
 * 在分层设计上，它是连接控制器、校验器、状态机和数据访问层的核心协调者。
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final int RECYCLE_BIN_RETENTION_DAYS = 30;
    private static final String OPERATOR_SYSTEM_AUTO = "SYSTEM_AUTO";

    /** 审计历史记录中，系统自动触发（创建支付时的初始状态）的操作来源标识 */
    private static final String OPERATOR_SYSTEM = "SYSTEM";

    /** 审计历史记录中，人工/测试接口手动触发状态流转的操作来源标识 */
    private static final String OPERATOR_MANUAL = "MANUAL";

    /** 默认页码 */
    private static final int DEFAULT_PAGE = 1;

    /** 默认每页条数 */
    private static final int DEFAULT_SIZE = 10;

    private final PaymentMapper paymentMapper;
    private final PaymentStatusHistoryMapper paymentStatusHistoryMapper;
    private final PaymentValidator paymentValidator;
    private final PaymentStateMachine paymentStateMachine;

    public PaymentServiceImpl(PaymentMapper paymentMapper,
            PaymentStatusHistoryMapper paymentStatusHistoryMapper,
            PaymentValidator paymentValidator,
            PaymentStateMachine paymentStateMachine) {
        this.paymentMapper = paymentMapper;
        this.paymentStatusHistoryMapper = paymentStatusHistoryMapper;
        this.paymentValidator = paymentValidator;
        this.paymentStateMachine = paymentStateMachine;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        // 1. 幂等性检查：优先按 idempotencyKey 查询是否已存在同一笔支付，命中则直接返回，不重复创建
        Payment existing = findByIdempotencyKey(request.getIdempotencyKey());
        if (existing != null) {
            return toResponse(existing);
        }

        // 2. 业务规则校验：金额范围与精度、币种白名单、账户存在性与源目标账户不能相同
        paymentValidator.validateCreateRequest(request);

        // 3. 构造支付主记录，创建阶段的初始状态固定为 CREATED（状态推进交由手动状态流转接口完成）
        LocalDateTime now = LocalDateTime.now();
        Payment payment = new Payment();
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setFromAccount(request.getFromAccount());
        payment.setToAccount(request.getToAccount());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency().toUpperCase(Locale.ROOT));
        payment.setStatus(PaymentStatus.CREATED.name());
        payment.setRemark(request.getRemark());
        payment.setVersion(0);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        try {
            paymentMapper.insert(payment);
        } catch (DuplicateKeyException ex) {
            // 4. 并发兜底：唯一索引冲突说明同一幂等键的另一并发请求已抢先创建成功，
            // 此时重新查询并返回该记录，而不是让客户端收到失败响应
            Payment concurrentlyCreated = findByIdempotencyKey(request.getIdempotencyKey());
            if (concurrentlyCreated != null) {
                return toResponse(concurrentlyCreated);
            }
            throw new PaymentException(ErrorCode.DUPLICATE_PAYMENT.name(), "幂等键冲突，请勿重复提交",
                    HttpStatus.CONFLICT);
        }

        // 5. 写入初始状态历史，作为支付审计时间线的第一条记录（变更前状态为空）
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPaymentId(payment.getId());
        history.setFromStatus(null);
        history.setToStatus(PaymentStatus.CREATED.name());
        history.setOperator(OPERATOR_SYSTEM);
        history.setCreatedAt(now);
        paymentStatusHistoryMapper.insert(history);

        return toResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentById(Long id) {
        return toResponse(ensureActivePaymentExists(id));
    }

    @Override
    public List<PaymentHistoryItemResponse> getPaymentHistory(Long id) {
        // 先确认支付是否存在，不存在则统一返回 PAYMENT_NOT_FOUND，与详情接口保持一致的错误语义
        ensureActivePaymentExists(id);

        List<PaymentStatusHistory> historyList = paymentStatusHistoryMapper.selectList(
                new LambdaQueryWrapper<PaymentStatusHistory>()
                        .eq(PaymentStatusHistory::getPaymentId, id)
                        .orderByAsc(PaymentStatusHistory::getCreatedAt));

        return historyList.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<PaymentResponse> listDeletedPayments(String keyword, Integer page, Integer size) {
        int pageNum = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int pageSize = (size == null || size < 1) ? DEFAULT_SIZE : size;

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Payment::getDeletedAt)
                .isNull(Payment::getPermanentlyDeletedAt)
                .ge(Payment::getDeletedAt, recycleBinCutoff())
                .orderByDesc(Payment::getDeletedAt);

        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            Long keywordAsId = parseAsId(trimmedKeyword);
            wrapper.and(qw -> {
                if (keywordAsId != null) {
                    qw.eq(Payment::getId, keywordAsId).or().like(Payment::getRemark, trimmedKeyword);
                } else {
                    qw.like(Payment::getRemark, trimmedKeyword);
                }
            });
        }

        Page<Payment> pageResult = paymentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        PageResponse<PaymentResponse> response = new PageResponse<>();
        response.setList(pageResult.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        response.setTotal(pageResult.getTotal());
        response.setPage(pageNum);
        response.setSize(pageSize);
        return response;
    }

    @Override
    public PageResponse<PaymentResponse> listPayments(String status, String keyword, Integer page, Integer size) {
        int pageNum = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int pageSize = (size == null || size < 1) ? DEFAULT_SIZE : size;

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Payment::getDeletedAt)
                .isNull(Payment::getPermanentlyDeletedAt);
        // 按状态精确筛选（可选）
        if (StringUtils.hasText(status)) {
            wrapper.eq(Payment::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        // 按关键字筛选（可选）：数字关键字按支付 ID 精确匹配，同时也允许匹配备注；非数字关键字仅匹配备注
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            Long keywordAsId = parseAsId(trimmedKeyword);
            wrapper.and(qw -> {
                if (keywordAsId != null) {
                    qw.eq(Payment::getId, keywordAsId).or().like(Payment::getRemark, trimmedKeyword);
                } else {
                    qw.like(Payment::getRemark, trimmedKeyword);
                }
            });
        }
        // 默认按创建时间倒序，最新创建的支付排在前面，便于列表页浏览
        wrapper.orderByDesc(Payment::getCreatedAt);

        Page<Payment> pageResult = paymentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        PageResponse<PaymentResponse> response = new PageResponse<>();
        response.setList(pageResult.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        response.setTotal(pageResult.getTotal());
        response.setPage(pageNum);
        response.setSize(pageSize);
        return response;
    }

    @Override
    @Transactional
    public int autoAdvancePendingPayments() {
        List<Payment> pendingPayments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .isNull(Payment::getDeletedAt)
                .isNull(Payment::getPermanentlyDeletedAt)
                .in(Payment::getStatus,
                        List.of(PaymentStatus.CREATED.name(), PaymentStatus.VALIDATED.name(),
                                PaymentStatus.SENT.name()))
                .orderByAsc(Payment::getUpdatedAt, Payment::getId));

        int advancedCount = 0;
        for (Payment payment : pendingPayments) {
            PaymentStatus currentStatus = PaymentStatus.valueOf(payment.getStatus());
            PaymentStatus nextStatus = nextAutoTransitionStatus(currentStatus);
            if (nextStatus == null) {
                continue;
            }
            applyStatusTransition(payment, currentStatus, nextStatus, null, null, payment.getRemark(),
                    OPERATOR_SYSTEM_AUTO);
            advancedCount++;
        }

        return advancedCount;
    }

    @Override
    @Transactional
    public PaymentResponse softDeletePayment(Long id) {
        Payment payment = ensureActivePaymentExists(id);
        LocalDateTime now = LocalDateTime.now();
        payment.setDeletedAt(now);
        payment.setUpdatedAt(now);
        paymentMapper.updateById(payment);
        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse restorePayment(Long id) {
        Payment payment = ensureRecoverableDeletedPaymentExists(id);
        LocalDateTime now = LocalDateTime.now();
        paymentMapper.update(null, new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getId, payment.getId())
                .set(Payment::getDeletedAt, null)
                .set(Payment::getUpdatedAt, now));
        payment.setDeletedAt(null);
        payment.setUpdatedAt(now);
        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse permanentlyDeletePayment(Long id) {
        Payment payment = ensureRecoverableDeletedPaymentExists(id);
        LocalDateTime now = LocalDateTime.now();
        paymentMapper.update(null, new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getId, payment.getId())
                .set(Payment::getPermanentlyDeletedAt, now)
                .set(Payment::getUpdatedAt, now));
        payment.setPermanentlyDeletedAt(now);
        payment.setUpdatedAt(now);
        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(Long id, UpdatePaymentStatusRequest request) {
        Payment payment = ensureActivePaymentExists(id);

        // 1. 解析目标状态：传入非法的状态字符串视为参数校验失败，而不是状态流转非法
        PaymentStatus targetStatus = parseStatus(request.getTargetStatus());
        PaymentStatus currentStatus = PaymentStatus.valueOf(payment.getStatus());

        // 2. 状态机合法性校验：严禁越级或逆向流转，非法流转统一抛出 INVALID_STATUS_TRANSITION
        if (!paymentStateMachine.canTransition(currentStatus, targetStatus)) {
            throw new PaymentException(ErrorCode.INVALID_STATUS_TRANSITION.name(),
                    String.format("不允许从 %s 流转到 %s", currentStatus, targetStatus), HttpStatus.BAD_REQUEST);
        }

        applyStatusTransition(payment, currentStatus, targetStatus,
                request.getErrorCode(), request.getErrorMessage(), request.getRemark(), OPERATOR_MANUAL);

        return toResponse(payment);
    }

    private void applyStatusTransition(Payment payment,
            PaymentStatus currentStatus,
            PaymentStatus targetStatus,
            String errorCode,
            String errorMessage,
            String remark,
            String operator) {
        if (!paymentStateMachine.canTransition(currentStatus, targetStatus)) {
            throw new PaymentException(ErrorCode.INVALID_STATUS_TRANSITION.name(),
                    String.format("不允许从 %s 流转到 %s", currentStatus, targetStatus), HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(targetStatus.name());
        payment.setErrorCode(errorCode);
        payment.setErrorMessage(errorMessage);
        if (StringUtils.hasText(remark)) {
            payment.setRemark(remark);
        }
        payment.setUpdatedAt(now);
        paymentMapper.updateById(payment);

        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPaymentId(payment.getId());
        history.setFromStatus(currentStatus.name());
        history.setToStatus(targetStatus.name());
        history.setErrorCode(errorCode);
        history.setErrorMessage(errorMessage);
        history.setRemark(remark);
        history.setOperator(operator);
        history.setCreatedAt(now);
        paymentStatusHistoryMapper.insert(history);
    }

    private PaymentStatus nextAutoTransitionStatus(PaymentStatus currentStatus) {
        return switch (currentStatus) {
            case CREATED -> PaymentStatus.VALIDATED;
            case VALIDATED -> PaymentStatus.SENT;
            case SENT -> PaymentStatus.COMPLETED;
            default -> null;
        };
    }

    /**
     * 按幂等键查询已存在的支付记录。
     *
     * @param idempotencyKey 客户端提供的幂等键
     * @return 已存在的支付实体，不存在则返回 null
     */
    private Payment findByIdempotencyKey(String idempotencyKey) {
        return paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getIdempotencyKey, idempotencyKey));
    }

    /**
     * 按主键查询支付，若不存在则抛出 PAYMENT_NOT_FOUND（HTTP 404）。
     *
     * @param id 支付主键 ID
     * @return 已存在的支付实体
     */
    private Payment ensurePaymentExists(Long id) {
        Payment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw new PaymentException(ErrorCode.PAYMENT_NOT_FOUND.name(), "支付记录不存在: " + id,
                    HttpStatus.NOT_FOUND);
        }
        return payment;
    }

    /**
     * 按主键查询可在正常前端列表中展示的支付。
     */
    private Payment ensureActivePaymentExists(Long id) {
        Payment payment = ensurePaymentExists(id);
        if (payment.getDeletedAt() != null || payment.getPermanentlyDeletedAt() != null) {
            throw new PaymentException(ErrorCode.PAYMENT_NOT_FOUND.name(), "支付记录不存在: " + id,
                    HttpStatus.NOT_FOUND);
        }
        return payment;
    }

    /**
     * 按主键查询仍处于回收站有效期内的记录。
     */
    private Payment ensureRecoverableDeletedPaymentExists(Long id) {
        Payment payment = ensurePaymentExists(id);
        if (payment.getDeletedAt() == null
                || payment.getPermanentlyDeletedAt() != null
                || payment.getDeletedAt().isBefore(recycleBinCutoff())) {
            throw new PaymentException(ErrorCode.RECYCLE_BIN_RECORD_NOT_FOUND.name(), "回收站记录不存在或已超过恢复期限: " + id,
                    HttpStatus.NOT_FOUND);
        }
        return payment;
    }

    private LocalDateTime recycleBinCutoff() {
        return LocalDateTime.now().minusDays(RECYCLE_BIN_RETENTION_DAYS);
    }

    /**
     * 将目标状态字符串解析为 PaymentStatus 枚举，非法值统一转换为 VALIDATION_FAILED 异常。
     *
     * @param targetStatus 请求中提交的目标状态字符串
     * @return 解析后的支付状态枚举
     */
    private PaymentStatus parseStatus(String targetStatus) {
        try {
            return PaymentStatus.valueOf(targetStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new PaymentException(ErrorCode.VALIDATION_FAILED.name(),
                    "targetStatus 不是合法的状态值: " + targetStatus, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 尝试将关键字解析为支付主键 ID，解析失败则返回 null，表示按普通文本关键字处理。
     *
     * @param keyword 关键字
     * @return 解析成功的 ID，或 null
     */
    private Long parseAsId(String keyword) {
        try {
            return Long.parseLong(keyword);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 将支付实体转换为对外的支付详情响应 DTO。
     *
     * @param payment 支付实体
     * @return 支付详情响应对象
     */
    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setIdempotencyKey(payment.getIdempotencyKey());
        response.setFromAccount(payment.getFromAccount());
        response.setToAccount(payment.getToAccount());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setErrorCode(payment.getErrorCode());
        response.setErrorMessage(payment.getErrorMessage());
        response.setRemark(payment.getRemark());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        response.setDeletedAt(payment.getDeletedAt());
        if (payment.getDeletedAt() != null) {
            response.setRecoverableUntil(payment.getDeletedAt().plusDays(RECYCLE_BIN_RETENTION_DAYS));
        }
        return response;
    }

    /**
     * 将支付状态历史实体转换为对外的历史记录响应 DTO。
     *
     * @param history 支付状态历史实体
     * @return 历史记录响应对象
     */
    private PaymentHistoryItemResponse toHistoryResponse(PaymentStatusHistory history) {
        PaymentHistoryItemResponse response = new PaymentHistoryItemResponse();
        response.setId(history.getId());
        response.setPaymentId(history.getPaymentId());
        response.setFromStatus(history.getFromStatus());
        response.setToStatus(history.getToStatus());
        response.setErrorCode(history.getErrorCode());
        response.setErrorMessage(history.getErrorMessage());
        response.setRemark(history.getRemark());
        response.setOperator(history.getOperator());
        response.setCreatedAt(history.getCreatedAt());
        return response;
    }
}

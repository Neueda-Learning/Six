// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.service;

import java.util.List;

import com.example.payments.dto.request.CreatePaymentRequest;
import com.example.payments.dto.request.UpdatePaymentStatusRequest;
import com.example.payments.dto.response.PageResponse;
import com.example.payments.dto.response.PaymentHistoryItemResponse;
import com.example.payments.dto.response.PaymentResponse;

/**
 * 支付业务服务接口。
 * 该接口定义控制器可调用的核心用例，包括支付创建、详情查询、历史查询、分页筛选以及状态更新。
 * 通过先声明接口再提供实现，可以让控制器依赖抽象，便于后续替换实现、补充测试或扩展业务规则。
 */
public interface PaymentService {

    /**
     * 创建支付并返回创建结果。
     * 若 idempotencyKey 命中已存在的支付，则直接返回该支付，不重复创建，也不重复写入历史。
     *
     * @param request 创建支付请求参数
     * @return 创建（或幂等命中）后的支付详情
     */
    PaymentResponse createPayment(CreatePaymentRequest request);

    /**
     * 根据支付 ID 查询支付详情。
     *
     * @param id 支付主键 ID
     * @return 支付详情对象
     */
    PaymentResponse getPaymentById(Long id);

    /**
     * 查询指定支付的状态历史，按发生时间升序排列。
     *
     * @param id 支付主键 ID
     * @return 历史记录列表
     */
    List<PaymentHistoryItemResponse> getPaymentHistory(Long id);

    /**
     * 查询最近删除的支付列表，仅返回仍在 30 天回收窗口内的记录。
     *
     * @param keyword 关键字筛选条件（可选，匹配支付 ID 或备注）
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @return 分页结果对象
     */
    PageResponse<PaymentResponse> listDeletedPayments(String keyword, Integer page, Integer size);

    /**
     * 按条件分页查询支付列表。
     *
     * @param status  支付状态筛选条件（可选）
     * @param keyword 关键字筛选条件（可选，匹配支付 ID 或备注）
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @return 分页结果对象
     */
    PageResponse<PaymentResponse> listPayments(String status, String keyword, Integer page, Integer size);

    /**
     * 将支付记录移入回收站。
     *
     * @param id 支付主键 ID
     * @return 更新后的支付详情
     */
    PaymentResponse softDeletePayment(Long id);

    /**
     * 将支付记录从回收站恢复。
     *
     * @param id 支付主键 ID
     * @return 恢复后的支付详情
     */
    PaymentResponse restorePayment(Long id);

    /**
     * 手动更新支付状态，主要用于课程演示、模拟失败与非法流转校验场景。
     *
     * @param id      支付主键 ID
     * @param request 状态更新请求参数
     * @return 更新后的支付详情
     */
    PaymentResponse updatePaymentStatus(Long id, UpdatePaymentStatusRequest request);
}

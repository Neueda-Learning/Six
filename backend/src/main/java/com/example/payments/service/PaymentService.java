// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.service;

import com.example.payments.dto.request.CreatePaymentRequest;
import com.example.payments.dto.request.UpdatePaymentStatusRequest;

/**
 * 支付业务服务接口。
 * 该接口定义控制器可调用的核心用例，包括支付创建、详情查询、历史查询、分页筛选以及状态更新。
 * 通过先声明接口再提供实现，可以让控制器依赖抽象，便于后续替换实现、补充测试或扩展业务规则。
 */
public interface PaymentService {

    /**
     * 创建支付并返回创建结果。
     *
     * @param request 创建支付请求参数
     * @return 创建后的业务结果对象
     */
    Object createPayment(CreatePaymentRequest request);

    /**
     * 根据支付 ID 查询支付详情。
     *
     * @param id 支付主键 ID
     * @return 支付详情对象
     */
    Object getPaymentById(Long id);

    /**
     * 查询指定支付的状态历史。
     *
     * @param id 支付主键 ID
     * @return 历史记录结果对象
     */
    Object getPaymentHistory(Long id);

    /**
     * 按条件分页查询支付列表。
     *
     * @param status  支付状态筛选条件
     * @param keyword 关键字筛选条件
     * @param page    页码
     * @param size    每页条数
     * @return 分页结果对象
     */
    Object listPayments(String status, String keyword, Integer page, Integer size);

    /**
     * 手动更新支付状态。
     *
     * @param id      支付主键 ID
     * @param request 状态更新请求参数
     * @return 更新后的业务结果对象
     */
    Object updatePaymentStatus(Long id, UpdatePaymentStatusRequest request);
}

//该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.payments.dto.request.CreatePaymentRequest;
import com.example.payments.dto.request.UpdatePaymentStatusRequest;
import com.example.payments.dto.response.ApiResponse;
import com.example.payments.dto.response.PageResponse;
import com.example.payments.dto.response.PaymentHistoryItemResponse;
import com.example.payments.dto.response.PaymentResponse;
import com.example.payments.service.PaymentService;

import jakarta.validation.Valid;

/**
 * 支付模块的 REST 控制器。
 * 该类位于接口层，负责接收前端或外部客户端发起的 HTTP 请求，完成参数绑定、基础校验触发，
 * 然后将请求转交给服务层处理，并把结果包装成统一的 API 响应结构返回给调用方。
 * 当前控制器覆盖创建支付、查询详情、查询历史、分页筛选和手动状态流转等课程要求的核心入口。
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // 统一使用构造器注入（此前同时存在字段 @Autowired 与构造器注入的重复写法，这里只保留构造器注入）
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 创建一笔新的支付请求。
     * 该接口会先按 idempotencyKey 查重，再执行金额/币种/账户等业务规则校验，
     * 校验通过后写入支付主记录（初始状态 CREATED）及审计历史首条记录。
     * 若 idempotencyKey 命中已存在的支付，则直接返回该支付，不会重复创建。
     *
     * @param request 创建支付时提交的业务参数
     * @return 统一包装后的创建结果
     */
    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ApiResponse.ok(response);
    }

    /**
     * 按支付 ID 查询支付详情。
     * 主要用于详情页展示当前支付的基础信息、状态和错误信息。
     *
     * @param id 支付主键 ID
     * @return 统一包装后的支付详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getPaymentById(@PathVariable Long id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ApiResponse.ok(response);
    }

    /**
     * 查询指定支付的状态变更历史。
     * 返回结果通常用于展示审计时间线，帮助调用方了解支付从创建到完成或失败的全过程。
     *
     * @param id 支付主键 ID
     * @return 统一包装后的历史记录列表
     */
    @GetMapping("/{id}/history")
    public ApiResponse<List<PaymentHistoryItemResponse>> getPaymentHistory(@PathVariable Long id) {
        List<PaymentHistoryItemResponse> response = paymentService.getPaymentHistory(id);
        return ApiResponse.ok(response);
    }

    /**
     * 分页查询支付列表，并按状态或关键字进行筛选。
     * 该接口通常服务于列表页或后台管理页，用于快速定位特定支付记录。
     *
     * @param status  可选的支付状态过滤条件
     * @param keyword 可选的关键字过滤条件
     * @param page    当前页码
     * @param size    每页条数
     * @return 统一包装后的分页结果
     */
    @GetMapping
    public ApiResponse<PageResponse<PaymentResponse>> listPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResponse<PaymentResponse> response = paymentService.listPayments(status, keyword, page, size);
        return ApiResponse.ok(response);
    }

    /**
     * 手动推进或修改支付状态。
     * 该接口主要用于课程演示、测试异常流转和模拟失败场景，并不一定适用于真实生产流程。
     *
     * @param id      支付主键 ID
     * @param request 目标状态及可选错误信息
     * @return 统一包装后的状态更新结果
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<PaymentResponse> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        PaymentResponse response = paymentService.updatePaymentStatus(id, request);
        return ApiResponse.ok(response);
    }
}


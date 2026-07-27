// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.response;

/**
 * 统一 API 响应包装对象。
 * 该类用于约定所有接口返回的外层结构，通过 success、data、errorCode 和 message 字段，
 * 让前端能够以一致方式处理成功结果与错误结果。
 * 它是控制器对外输出的标准响应模型之一。
 */
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String errorCode;
    private String message;

    /**
     * 构造一个成功响应。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = "OK";
        return response;
    }

    /**
     * 构造一个失败响应。
     *
     * @param errorCode 业务错误码
     * @param message   错误描述信息
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> fail(String errorCode, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.errorCode = errorCode;
        response.message = message;
        return response;
    }

    /**
     * 构造一个临时占位响应。
     * 当前方法仅用于项目骨架阶段，后续应由真实业务返回替代。
     *
     * @param <T> 数据类型
     * @return 占位成功响应
     */
    public static <T> ApiResponse<T> todo() {
        // todo replace with real response in controller/service
        return ok(null);
    }

    // todo generate getters/setters
}
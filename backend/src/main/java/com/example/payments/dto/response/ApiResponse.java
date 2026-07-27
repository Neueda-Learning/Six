// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.response;

public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String errorCode;
    private String message;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = "OK";
        return response;
    }

    public static <T> ApiResponse<T> fail(String errorCode, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.errorCode = errorCode;
        response.message = message;
        return response;
    }

    public static <T> ApiResponse<T> todo() {
        //todo replace with real response in controller/service
        return ok(null);
    }

    //todo generate getters/setters
}
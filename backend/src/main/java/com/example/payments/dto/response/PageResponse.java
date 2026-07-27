// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.response;

import java.util.List;

public class PageResponse<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;

    //todo generate getters/setters
}
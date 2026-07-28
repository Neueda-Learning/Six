// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.response;

import java.util.List;

import lombok.Data;

/**
 * 分页响应 DTO。
 * 该对象用于承载列表接口返回的分页数据，包含当前页数据列表、总记录数、当前页码和每页大小。
 * 它通常会作为统一响应对象中的 data 字段内容返回给前端。
 */
@Data
public class PageResponse<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;

    // todo generate getters/setters
}
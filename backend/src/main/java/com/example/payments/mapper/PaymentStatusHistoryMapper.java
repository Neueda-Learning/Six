// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.payments.entity.PaymentStatusHistory;

@Mapper
public interface PaymentStatusHistoryMapper extends BaseMapper<PaymentStatusHistory> {
    //todo add query for timeline ordering when needed
}
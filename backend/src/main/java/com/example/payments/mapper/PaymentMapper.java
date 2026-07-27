// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.payments.entity.Payment;

/**
 * 支付主表的数据访问接口。
 * 该接口基于 MyBatis-Plus 的 BaseMapper，提供对支付主表的基础增删改查能力。
 * 当通用 CRUD 无法满足需求时，也可以在这里继续扩展自定义查询方法。
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
    // todo add custom query methods if BaseMapper is not enough
}
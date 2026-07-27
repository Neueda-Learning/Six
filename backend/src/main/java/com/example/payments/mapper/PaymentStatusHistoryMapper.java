// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.payments.entity.PaymentStatusHistory;

/**
 * 支付状态历史表的数据访问接口。
 * 该接口用于读写支付状态历史记录，通常服务于审计记录保存、时间线查询以及故障排查场景。
 * 当前继承 BaseMapper 提供通用能力，后续可按需要补充排序或聚合查询。
 */
@Mapper
public interface PaymentStatusHistoryMapper extends BaseMapper<PaymentStatusHistory> {
    // todo add query for timeline ordering when needed
}
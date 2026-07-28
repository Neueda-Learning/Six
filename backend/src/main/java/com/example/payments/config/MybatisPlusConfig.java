// 该文件用于配置 MyBatis-Plus 全局插件，包含分页插件与乐观锁插件。
package com.example.payments.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
/**
 * MyBatis-Plus 全局插件配置类。
 * 注册分页插件（PaginationInnerInterceptor）和乐观锁插件（OptimisticLockerInnerInterceptor）：
 * - 分页插件让 Page&lt;Payment&gt; 分页查询在数据库层面真正生效（否则 selectPage 不会加 LIMIT/OFFSET，
 * total 也不准确），支付列表筛选接口依赖该插件。
 * - 乐观锁插件配合 Payment 实体上的 @Version 字段，在执行 updateById 时自动附加版本号条件并递增，
 * 防止手工状态流转接口在并发场景下发生更新覆盖。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器链。
     *
     * @return 包含分页与乐观锁插件的拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件：指定数据库类型为 MySQL，供支付列表分页查询使用
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁插件：配合 Payment.version 字段，更新时自动做版本号校验与递增
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}

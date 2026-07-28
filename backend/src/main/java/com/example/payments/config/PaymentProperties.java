// 该文件用于绑定 application.yml 中 payments.* 配置项，供校验组件读取受支持币种等业务参数。
package com.example.payments.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 支付业务相关的可配置参数。
 * 对应 application.yml 中的 payments 节点，目前包含受支持币种白名单与
 * 模拟网络发送的最大重试次数，供校验器与后续业务逻辑复用，避免在代码中硬编码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "payments")
public class PaymentProperties {

    /** 受支持的货币白名单（ISO 4217 三位代码），对应 payments.supported-currencies */
    private List<String> supportedCurrencies;

    /** 模拟网络发送的最大重试次数，对应 payments.network-max-retry */
    private int networkMaxRetry;
}

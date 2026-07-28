package com.example.payments.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 覆盖 test-cases.md 第九章 TC-48：确认 payments.network-max-retry 配置项当前的真实使用情况。
 *
 * 经代码检索（PaymentServiceImpl、PaymentValidator 等全部业务代码）确认：
 * networkMaxRetry 目前只是被 @ConfigurationProperties 绑定读取到这个 POJO 上，
 * 全项目没有任何地方调用 getNetworkMaxRetry()，也没有实现任何"发送失败后重试 N 次"的循环逻辑
 * ——application.yml 里的 payments.network-max-retry: 3 目前是一项未被消费的死配置。
 *
 * 因此这里只能验证"配置值可以正确绑定并通过 getter 读取"，不能也不应该伪造一个
 * "重试 3 次后才失败"之类看起来测了、实际没有对应代码支撑的断言。
 * 若后续要真正实现网络重试，需要在 PaymentServiceImpl 中补充读取该配置驱动的重试循环，
 * 并在本类补充相应的行为测试。
 */
class PaymentPropertiesTest {

    // TC-48：PaymentProperties 能正确持有 network-max-retry 配置的值（当前仅此而已，无重试逻辑可测）
    @Test
    void networkMaxRetry_setAndGet_returnsBoundValue() {
        PaymentProperties properties = new PaymentProperties();
        properties.setNetworkMaxRetry(3);

        assertEquals(3, properties.getNetworkMaxRetry());
    }
}

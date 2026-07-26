# Copilot 全局记忆配置（课程项目）

## 项目定位
- 这是课程实践项目，目标是实现支付生命周期管理，不做大型企业级扩展。
- 技术栈固定：Spring Boot 3.x + JDK 17 + Maven，前端 Vue 3 + Element Plus，数据库 MySQL，持久层 MyBatis-Plus。

## 功能范围边界
- 仅实现以下 5 项功能：
1. 创建支付。
2. 查看支付状态与详情。
3. 查看支付状态历史（audit trail）。
4. 按状态筛选与检索支付。
5. 查看失败支付错误详情。
- 不实现登录认证、权限、账户归属、多租户、真实支付网关对接。

## 支付状态规则
- 状态集合：CREATED, VALIDATED, SENT, COMPLETED, FAILED。
- 合法流转：
1. CREATED -> VALIDATED
2. CREATED -> FAILED
3. VALIDATED -> SENT
4. VALIDATED -> FAILED
5. SENT -> COMPLETED
6. SENT -> FAILED
- 非法流转必须返回 INVALID_STATUS_TRANSITION（HTTP 400）。

## 幂等与错误处理约定
- 创建支付必须支持 idempotencyKey。
- 重复请求优先返回已存在支付（HTTP 200，响应中标识幂等命中）。
- 错误响应统一结构：success, data, errorCode, message。
- 优先使用以下错误码：
1. VALIDATION_FAILED
2. INSUFFICIENT_FUNDS
3. INVALID_ACCOUNT
4. INVALID_CURRENCY
5. INVALID_AMOUNT
6. DUPLICATE_PAYMENT
7. INVALID_STATUS_TRANSITION
8. PAYMENT_NOT_FOUND
9. PROCESSING_ERROR
10. NETWORK_ERROR

## 数据与事务约定
- 核心表：payments, payment_status_history。
- 所有状态变化必须写入 payment_status_history。
- 创建支付与状态推进建议使用单事务，保证主表与历史表一致性。
- 并发更新使用乐观锁 version 字段。

## 代码生成与修改偏好
- 优先小步改动，保持目录清晰：controller/service/mapper/entity/dto/enums/exception。
- 先保证可运行 MVP，再逐步补充校验、错误码与历史时间线。
- 提供 REST 接口时必须同步给出请求示例与响应示例。
- 涉及接口变更时，同步更新 OpenAPI 注解与文档。
# Copilot 全局记忆配置（课程项目）

## 1. 项目定位
- 这是课程实践项目，目标是实现支付生命周期管理，不做大型企业级扩展。
- 技术栈固定：Spring Boot 3.x + JDK 17 + Maven，前端 Vue 3 + Element Plus，数据库 MySQL，持久层 MyBatis-Plus。

## 2. 功能范围边界

- 不实现登录认证、权限、账户归属、多租户、真实支付网关对接。


## 3. 支付状态规则
- 状态集合：CREATED, VALIDATED, SENT, COMPLETED, FAILED。
- 合法流转：
1. CREATED -> VALIDATED
2. CREATED -> FAILED
3. VALIDATED -> SENT
4. VALIDATED -> FAILED
5. SENT -> COMPLETED
6. SENT -> FAILED

- 非法流转必须返回 INVALID_STATUS_TRANSITION（HTTP 400）。
**注意**: `FAILED`（失败）属于终态，任何状态节点在发生不可逆业务阻碍或网络重试耗尽时，均可迁移至 `FAILED` 状态。

* **状态修改守则**: 严禁直接使用属性 Setter 越级或逆向修改交易状态。

## 4. 幂等与错误处理约定
在由 `CREATED` 转换至 `VALIDATED` 时，必须严格执行三层校验：
1. **金额校验**: 交易金额必须大于零 (Amount > 0)，最高单笔交易限额不得超过 Amount < 1000000 元，且小数位数不得超过两位。
2. **账户校验**: 源账户与目的账户不能为同一个，且都必须在系统初始化数据中真实存在。
3. **币种校验**: 货币必须符合 ISO 4217 代码（如 USD, EUR, GBP 等）并在支持的白名单内。
- 创建支付必须支持 idempotencyKey。
- 重复请求优先返回已存在支付（HTTP 200，响应中标识幂等命中）。
- 错误响应统一结构：success, data, errorCode, message。
- 优先使用以下错误码：
| 错误码 | HTTP状态码 | 业务场景 |
| :--- | :--- | :--- |
| `VALIDATION_FAILED` | 400 Bad Request | 基础表单输入字段格式校验失败 |
| `INSUFFICIENT_FUNDS` | 400 Bad Request | 付款账户可用余额不足 |
| `INVALID_ACCOUNT` | 400 Bad Request | 账户格式非法或账户在系统数据中不存在 |
| `INVALID_CURRENCY` | 400 Bad Request | 不支持的或不合规的货币符号 |
| `INVALID_AMOUNT` | 400 Bad Request | 金额为负、零或超过单笔百万限制 |
| `DUPLICATE_PAYMENT` | 409 Conflict | 幂等密钥冲突，交易处理中且禁止重试 |
| `INVALID_STATUS_TRANSITION` | 400 Bad Request | 企图越级或逆向流转状态机状态 |
| `PAYMENT_NOT_FOUND` | 404 Not Found | 检索的支付记录主键 ID 不存在 |
| `PROCESSING_ERROR` | 500 Internal Error | 后端运行时非预期异常 |
| `NETWORK_ERROR` | 503 Service Unavailable | 模拟通道通信超时且重试次数耗尽 |


## 5. 代码编写规范

### 后端 Spring Boot 编码守则
* **分层边界**:
  * `Controller`: 负责解析 HTTP 请求。
  * `Service`: 核心业务逻辑处理，事务边界在此定义。
  * `Mapper (MyBatis-Plus)`: 数据实体的高性能 CRUD 操作。
* **异常处理**: 使用 `@RestControllerAdvice` 结合全局自定义异常类，拦截特定异常并统一返回包含 `errorCode`、`message`、`timestamp` 的标准 JSON 数据体。
* **命名**: 使用标准的 JavaBean、MyBatis-Plus 命名规范。

## 6. 代码生成与修改偏好
- 优先小步改动，保持目录清晰：controller/service/mapper/entity/dto/enums/exception。
- 先保证可运行 MVP，再逐步补充校验、错误码与历史时间线。
- 提供 REST 接口时必须同步给出请求示例与响应示例。
- 涉及接口变更时，同步更新 OpenAPI 注解与文档。

## 7. 沟通方式
- 默认中文回复；代码、命令、变量名、文件路径保持英文
- 结论先行，简洁直接，不先铺垫背景
- 不谄媚，不夸"这是个很好的问题"，不以"当然可以"开头
- 给真实判断——方案有问题直接指出，发现更好做法主动说明

## 8. Git 操作规则
- 不自动 `git commit` 或 `git push`，除非我明确要求
- 提交前先展示将要提交的变更摘要
- commit message 使用简洁英文

## 9. 红线操作
以下操作即使在 auto-accept 模式下也必须先询问：
- 删除文件、目录或 git 历史
- 修改 `.env`、密钥、token、证书、CI/CD 配置
- `git push`、`git rebase`、`git reset --hard`、强制推送
- 公开发布（`npm publish`、生产部署等）
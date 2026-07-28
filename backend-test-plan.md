# 支付后端接口测试方案

> 本文档面向 `backend`（Spring Boot + MyBatis-Plus + MySQL）已实现的 5 个支付接口，给出每个功能的测试用例、测试方式（curl 示例）和测试成功时的响应结构。测试基于 `backend/src/main/resources/db/data.sql` 中的种子数据。

## 1. 测试环境准备

1. **JDK 22**：本机 Maven 默认 `JAVA_HOME` 可能是 JDK 17，需要临时切到 JDK 22 再编译/运行：
   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\OpenJDK\jdk-22.0.2'
   $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
   ```
2. **初始化数据库**：Spring Boot 不会对外部 MySQL 自动执行 `schema.sql`/`data.sql`，需要手动执行一次：
   ```powershell
   mysql -u root -p < backend\src\main\resources\db\schema.sql
   mysql -u root -p payments_db < backend\src\main\resources\db\data.sql
   ```
3. **启动应用**（使用默认 `application.yml`，不要额外指定 `dev` profile，否则会连到未初始化的 `payments_db_dev`）：
   ```powershell
   cd backend
   mvn spring-boot:run
   ```
4. 应用启动后：
   - 接口基础地址：`http://localhost:8080/api/payments`
   - Swagger UI（可视化调试）：`http://localhost:8080/swagger-ui.html`

## 2. 通用响应结构

所有接口统一返回如下信封结构，测试时先看 `success` 判断成功/失败，再看 `data` 或 `errorCode`：

```json
{
  "success": true,
  "data": {},
  "errorCode": null,
  "message": "OK"
}
```

失败时 `success=false`，`data` 通常为 `null`，`errorCode` 为 `ErrorCode` 枚举值之一，`message` 为具体错误描述。

## 3. 种子数据参考（data.sql）

| 支付 ID | idempotencyKey | 状态 | 说明 |
|---|---|---|---|
| 1 | seed-happy-completed-001 | COMPLETED | 已完成，终态，用于测试非法逆向流转 |
| 2 | seed-in-flight-sent-002 | SENT | 可流转到 COMPLETED 或 FAILED |
| 3 | seed-in-flight-validated-003 | VALIDATED | 可流转到 SENT 或 FAILED |
| 4 | seed-in-flight-created-004 | CREATED | 可流转到 VALIDATED 或 FAILED |
| 5 | seed-failed-validation-005 | FAILED | 终态，errorCode=INSUFFICIENT_FUNDS |
| 6 | seed-failed-network-006 | FAILED | 终态，errorCode=NETWORK_ERROR |

账户（accounts 表）：`ACC10001`(USD)、`ACC10002`(EUR)、`ACC10003`(GBP)、`ACC20001`(USD)、`ACC20002`(EUR)、`ACC20003`(GBP)，均为真实存在的账户，可用于账户校验测试；不存在的账户可用 `ACC99999` 之类的编号测试。

---

## 4. 功能一：创建支付 — `POST /api/payments`

### TC-1 正常创建（Happy Path）

**测试方法**：
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "test-create-001",
    "fromAccount": "ACC10001",
    "toAccount": "ACC20002",
    "amount": 100.50,
    "currency": "USD",
    "remark": "test happy path"
  }'
```

**预期响应**（HTTP 200，新支付被创建，状态固定为 `CREATED`；`id` 为数据库自增值，实际返回以真实结果为准）：
```json
{
  "success": true,
  "data": {
    "id": 7,
    "idempotencyKey": "test-create-001",
    "fromAccount": "ACC10001",
    "toAccount": "ACC20002",
    "amount": 100.50,
    "currency": "USD",
    "status": "CREATED",
    "errorCode": null,
    "errorMessage": null,
    "remark": "test happy path",
    "createdAt": "2026-07-28T10:00:00",
    "updatedAt": "2026-07-28T10:00:00"
  },
  "errorCode": null,
  "message": "OK"
}
```
**验证点**：额外调用 `GET /api/payments/{id}/history`，应能看到一条 `fromStatus=null, toStatus=CREATED, operator=SYSTEM` 的历史记录。

### TC-2 幂等命中（重复提交）

**测试方法**：使用与 TC-1 完全相同的 `idempotencyKey` 再次提交一次（其他字段随意）。

**预期响应**：HTTP 200，`success=true`，返回的是 TC-1 中已创建的那笔支付（`id` 与 TC-1 相同，不会新建记录）。

### TC-3 金额非法

**测试方法**（分别测试三种非法金额）：
```bash
# 负数/零
curl -X POST http://localhost:8080/api/payments -H "Content-Type: application/json" \
  -d '{"idempotencyKey":"test-amt-1","fromAccount":"ACC10001","toAccount":"ACC20002","amount":-10,"currency":"USD"}'

# 超过 1,000,000
curl -X POST http://localhost:8080/api/payments -H "Content-Type: application/json" \
  -d '{"idempotencyKey":"test-amt-2","fromAccount":"ACC10001","toAccount":"ACC20002","amount":1000000,"currency":"USD"}'

# 超过两位小数
curl -X POST http://localhost:8080/api/payments -H "Content-Type: application/json" \
  -d '{"idempotencyKey":"test-amt-3","fromAccount":"ACC10001","toAccount":"ACC20002","amount":100.123,"currency":"USD"}'
```
> 注：`amount=-10` 和 `amount=0` 会先被 DTO 上的 `@DecimalMin("0.01")` 拦截（走 TC-6 的 VALIDATION_FAILED 分支）；`amount=1000000` 与三位小数会穿过注解校验，走到 `PaymentValidator`，返回 `INVALID_AMOUNT`。

**预期响应**（超限/超精度场景，HTTP 400）：
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_AMOUNT",
  "message": "支付金额不得超过单笔限额 1,000,000"
}
```

### TC-4 币种不支持

**测试方法**：
```bash
curl -X POST http://localhost:8080/api/payments -H "Content-Type: application/json" \
  -d '{"idempotencyKey":"test-cur-1","fromAccount":"ACC10001","toAccount":"ACC20002","amount":100,"currency":"JPY"}'
```

**预期响应**（HTTP 400）：
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_CURRENCY",
  "message": "不支持的货币类型: JPY"
}
```

### TC-5 账户非法（相同账户 / 账户不存在）

**测试方法**：
```bash
# 源账户与目标账户相同
curl -X POST http://localhost:8080/api/payments -H "Content-Type: application/json" \
  -d '{"idempotencyKey":"test-acc-1","fromAccount":"ACC10001","toAccount":"ACC10001","amount":100,"currency":"USD"}'

# 账户不存在
curl -X POST http://localhost:8080/api/payments -H "Content-Type: application/json" \
  -d '{"idempotencyKey":"test-acc-2","fromAccount":"ACC99999","toAccount":"ACC20002","amount":100,"currency":"USD"}'
```

**预期响应**（HTTP 400，以“账户不存在”为例）：
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_ACCOUNT",
  "message": "源账户不存在: ACC99999"
}
```

### TC-6 必填字段缺失（基础参数校验）

**测试方法**：
```bash
curl -X POST http://localhost:8080/api/payments -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC10001","toAccount":"ACC20002","amount":100,"currency":"USD"}'
```
（缺少必填的 `idempotencyKey`）

**预期响应**（HTTP 400；`message` 的具体文案取决于运行环境默认语言，可能为中文或英文，格式为 `字段名: 错误描述`）：
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_FAILED",
  "message": "idempotencyKey: 不能为空"
}
```

---

## 5. 功能二：查询支付详情 — `GET /api/payments/{id}`

### TC-1 存在的支付

**测试方法**：
```bash
curl http://localhost:8080/api/payments/5
```

**预期响应**（HTTP 200，对应 data.sql 中 id=5 的失败支付，包含错误详情）：
```json
{
  "success": true,
  "data": {
    "id": 5,
    "idempotencyKey": "seed-failed-validation-005",
    "fromAccount": "ACC10002",
    "toAccount": "ACC20002",
    "amount": 8000.00,
    "currency": "USD",
    "status": "FAILED",
    "errorCode": "INSUFFICIENT_FUNDS",
    "errorMessage": "mock insufficient balance in from_account",
    "remark": "invoice-2026-07-seed-05",
    "createdAt": "2026-07-24T13:00:00",
    "updatedAt": "2026-07-24T13:01:00"
  },
  "errorCode": null,
  "message": "OK"
}
```

### TC-2 不存在的支付

**测试方法**：
```bash
curl http://localhost:8080/api/payments/999999
```

**预期响应**（HTTP 404）：
```json
{
  "success": false,
  "data": null,
  "errorCode": "PAYMENT_NOT_FOUND",
  "message": "支付记录不存在: 999999"
}
```

---

## 6. 功能三：查询支付状态历史 — `GET /api/payments/{id}/history`

### TC-1 有多条历史记录的支付

**测试方法**：
```bash
curl http://localhost:8080/api/payments/1/history
```

**预期响应**（HTTP 200，按 `createdAt` 升序返回 data.sql 中 id=1 的完整状态流转时间线）：
```json
{
  "success": true,
  "data": [
    { "id": 1, "paymentId": 1, "fromStatus": null, "toStatus": "CREATED", "errorCode": null, "errorMessage": null, "remark": null, "operator": "SYSTEM", "createdAt": "2026-07-20T09:00:00" },
    { "id": 2, "paymentId": 1, "fromStatus": "CREATED", "toStatus": "VALIDATED", "errorCode": null, "errorMessage": null, "remark": null, "operator": "SYSTEM", "createdAt": "2026-07-20T09:01:00" },
    { "id": 3, "paymentId": 1, "fromStatus": "VALIDATED", "toStatus": "SENT", "errorCode": null, "errorMessage": null, "remark": null, "operator": "SYSTEM", "createdAt": "2026-07-20T09:02:00" },
    { "id": 4, "paymentId": 1, "fromStatus": "SENT", "toStatus": "COMPLETED", "errorCode": null, "errorMessage": null, "remark": null, "operator": "SYSTEM", "createdAt": "2026-07-20T09:03:00" }
  ],
  "errorCode": null,
  "message": "OK"
}
```

### TC-2 不存在的支付

**测试方法**：
```bash
curl http://localhost:8080/api/payments/999999/history
```

**预期响应**（HTTP 404，同功能二 TC-2）：
```json
{
  "success": false,
  "data": null,
  "errorCode": "PAYMENT_NOT_FOUND",
  "message": "支付记录不存在: 999999"
}
```

---

## 7. 功能四：支付列表分页与筛选 — `GET /api/payments`

### TC-1 无筛选条件（默认分页）

**测试方法**：
```bash
curl "http://localhost:8080/api/payments"
```

**预期响应**（HTTP 200，默认 `page=1&size=10`，`total` 应为当前库中全部支付条数，seed 数据下为 6+已创建的测试数据）：
```json
{
  "success": true,
  "data": {
    "list": [ { "id": 7, "status": "CREATED", "...": "..." }, "...": "共 total 条中最多 size 条" ],
    "total": 6,
    "page": 1,
    "size": 10
  },
  "errorCode": null,
  "message": "OK"
}
```

### TC-2 按状态筛选

**测试方法**：
```bash
curl "http://localhost:8080/api/payments?status=FAILED"
```

**预期响应**（HTTP 200，`list` 中只包含 `status=FAILED` 的支付，对应 data.sql 中 id=5 和 id=6，`total=2`）。

### TC-3 按关键字筛选（匹配备注或支付 ID）

**测试方法**：
```bash
# 关键字为数字 5，按支付 ID 精确匹配（或匹配备注中含 "5" 的记录）
curl "http://localhost:8080/api/payments?keyword=5"

# 关键字为文本，按备注模糊匹配
curl "http://localhost:8080/api/payments?keyword=seed-05"
```

**预期响应**：HTTP 200，`list` 中应至少包含 `id=5` 的支付（其 remark 为 `invoice-2026-07-seed-05`）。

### TC-4 分页参数验证

**测试方法**：
```bash
curl "http://localhost:8080/api/payments?page=1&size=2"
```

**预期响应**：HTTP 200，`data.list` 长度为 2，`data.page=1`，`data.size=2`，`data.total` 为全库总数（不受 size 影响）。

---

## 8. 功能五：手动状态流转 — `PATCH /api/payments/{id}/status`

### TC-1 合法流转（CREATED → VALIDATED）

**测试方法**（对 data.sql 中状态为 `CREATED` 的 id=4）：
```bash
curl -X PATCH http://localhost:8080/api/payments/4/status \
  -H "Content-Type: application/json" \
  -d '{"targetStatus":"VALIDATED"}'
```

**预期响应**（HTTP 200，`status` 变为 `VALIDATED`）：
```json
{
  "success": true,
  "data": {
    "id": 4,
    "status": "VALIDATED",
    "errorCode": null,
    "errorMessage": null,
    "...": "其余字段同详情接口"
  },
  "errorCode": null,
  "message": "OK"
}
```
**验证点**：再调用 `GET /api/payments/4/history`，应新增一条 `fromStatus=CREATED, toStatus=VALIDATED, operator=MANUAL` 的记录。

### TC-2 非法流转（终态逆向流转）

**测试方法**（对 data.sql 中状态为 `COMPLETED` 的 id=1）：
```bash
curl -X PATCH http://localhost:8080/api/payments/1/status \
  -H "Content-Type: application/json" \
  -d '{"targetStatus":"CREATED"}'
```

**预期响应**（HTTP 400）：
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_STATUS_TRANSITION",
  "message": "不允许从 COMPLETED 流转到 CREATED"
}
```

### TC-3 目标状态值非法（拼写错误/不存在的状态）

**测试方法**：
```bash
curl -X PATCH http://localhost:8080/api/payments/4/status \
  -H "Content-Type: application/json" \
  -d '{"targetStatus":"UNKNOWN_STATUS"}'
```

**预期响应**（HTTP 400）：
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_FAILED",
  "message": "targetStatus 不是合法的状态值: UNKNOWN_STATUS"
}
```

### TC-4 模拟失败（流转到 FAILED 并携带错误码）

**测试方法**（对 data.sql 中状态为 `SENT` 的 id=2）：
```bash
curl -X PATCH http://localhost:8080/api/payments/2/status \
  -H "Content-Type: application/json" \
  -d '{"targetStatus":"FAILED","errorCode":"NETWORK_ERROR","errorMessage":"mock network timeout"}'
```

**预期响应**（HTTP 200，支付进入终态 `FAILED` 并带上错误详情）：
```json
{
  "success": true,
  "data": {
    "id": 2,
    "status": "FAILED",
    "errorCode": "NETWORK_ERROR",
    "errorMessage": "mock network timeout",
    "...": "其余字段同详情接口"
  },
  "errorCode": null,
  "message": "OK"
}
```

### TC-5 支付不存在

**测试方法**：
```bash
curl -X PATCH http://localhost:8080/api/payments/999999/status \
  -H "Content-Type: application/json" \
  -d '{"targetStatus":"VALIDATED"}'
```

**预期响应**（HTTP 404，同功能二 TC-2）：
```json
{
  "success": false,
  "data": null,
  "errorCode": "PAYMENT_NOT_FOUND",
  "message": "支付记录不存在: 999999"
}
```

---

## 9. 回归建议

按顺序执行以下场景可覆盖完整生命周期（Happy Path）：

1. `POST /api/payments` 创建一笔新支付（状态 CREATED）
2. `PATCH .../status` 推进到 `VALIDATED`
3. `PATCH .../status` 推进到 `SENT`
4. `PATCH .../status` 推进到 `COMPLETED`
5. 每一步之后调用 `GET .../history`，确认审计时间线按顺序完整记录
6. 最后尝试 `PATCH .../status` 把该支付从 `COMPLETED` 改回任意其他状态，确认被 `INVALID_STATUS_TRANSITION` 拦截

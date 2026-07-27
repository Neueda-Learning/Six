# 用户故事（User Stories）
# Payment Processing System

## 故事地图总览

| 编号 | Epic | 用户故事标题 |
| :--- | :--- | :--- |
| US-001 | 支付创建 | 创建新支付 |
| US-002 | 支付创建 | 防止重复支付 |
| US-003 | 查看详情 | 查看单笔支付详情 |
| US-004 | 查看详情 | 查看失败支付的错误详情 |
| US-005 | 审计历史 | 查看支付状态变更历史 |
| US-006 | 检索筛选 | 按状态筛选支付列表 |
| US-007 | 状态流转 | 推进支付状态 |
| US-008 | 状态流转 | 验证支付规则 |

---

## Epic 1：支付创建

### US-001：创建新支付

> 作为一名**财务操作员**，我希望能够提交一笔新支付（包含金额、货币、来源账户和目标账户），以便启动支付流程。

**验收条件：**
- [ ] 提交有效的支付信息后，系统返回唯一支付 ID 和初始状态 `CREATED`
- [ ] 支持 `idempotencyKey` 字段，每笔支付创建时可携带
- [ ] 金额必须 > 0 且 < 1,000,000，最多两位小数；否则返回 `INVALID_AMOUNT`（HTTP 400）
- [ ] 来源账户与目标账户不能相同，且必须在系统中存在；否则返回 `INVALID_ACCOUNT`（HTTP 400）
- [ ] 货币代码须符合 ISO 4217 白名单（如 USD、EUR、CNY）；否则返回 `INVALID_CURRENCY`（HTTP 400）
- [ ] 必填字段缺失或格式错误时返回 `VALIDATION_FAILED`（HTTP 400）

**请求示例：**
```json
POST /api/payments
{
  "idempotencyKey": "order-20240101-001",
  "amount": 199.99,
  "currency": "USD",
  "sourceAccount": "ACC-001",
  "targetAccount": "ACC-002"
}
```

**响应示例（成功）：**
```json
{
  "success": true,
  "data": {
    "id": "PAY-123",
    "status": "CREATED",
    "amount": 199.99,
    "currency": "USD"
  },
  "errorCode": null,
  "message": null
}
```

---

### US-002：防止重复支付

> 作为一名**财务操作员**，我希望系统能识别重复提交的支付请求，以避免因网络重试导致的重复扣款。

**验收条件：**
- [ ] 使用相同 `idempotencyKey` 的第二次请求，系统返回 HTTP 200 及原始支付数据
- [ ] 响应中包含幂等命中标识 `"idempotent": true`，而非创建新记录
- [ ] 不会产生新的支付记录或状态历史条目

**响应示例（幂等命中）：**
```json
{
  "success": true,
  "data": {
    "id": "PAY-123",
    "status": "CREATED",
    "idempotent": true
  },
  "errorCode": null,
  "message": null
}
```

---

## Epic 2：查看支付状态与详情

### US-003：查看单笔支付详情

> 作为一名**财务操作员**，我希望通过支付 ID 查看支付的完整信息（金额、货币、状态、时间等），以便了解支付的当前情况。

**验收条件：**
- [ ] 输入有效 ID 返回支付完整详情（id、amount、currency、status、createdAt、updatedAt）
- [ ] 输入不存在的 ID 返回 `PAYMENT_NOT_FOUND`（HTTP 404）

**请求示例：**
```
GET /api/payments/PAY-123
```

**响应示例：**
```json
{
  "success": true,
  "data": {
    "id": "PAY-123",
    "amount": 199.99,
    "currency": "USD",
    "status": "VALIDATED",
    "sourceAccount": "ACC-001",
    "targetAccount": "ACC-002",
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-01T10:01:00Z"
  },
  "errorCode": null,
  "message": null
}
```

---

### US-004：查看失败支付的错误详情

> 作为一名**财务操作员**，我希望在支付失败时能看到具体的错误原因和错误码，以便排查和处理问题。

**验收条件：**
- [ ] 状态为 `FAILED` 的支付，详情中包含 `errorCode` 和 `message`
- [ ] 错误码来自系统规范的标准列表

**响应示例（失败支付）：**
```json
{
  "success": true,
  "data": {
    "id": "PAY-456",
    "status": "FAILED",
    "errorCode": "INSUFFICIENT_FUNDS",
    "message": "付款账户可用余额不足",
    "updatedAt": "2024-01-01T10:05:00Z"
  },
  "errorCode": null,
  "message": null
}
```

---

## Epic 3：支付状态历史（Audit Trail）

### US-005：查看支付状态变更历史

> 作为一名**审计员**，我希望查看某笔支付的完整状态流转历史（每次从哪个状态变更到哪个状态、发生时间），以满足合规审计需求。

**验收条件：**
- [ ] 返回该支付所有历史状态条目
- [ ] 每条记录包含：`fromStatus`、`toStatus`、`changedAt`
- [ ] 按时间升序排列
- [ ] 支付 ID 不存在时返回 `PAYMENT_NOT_FOUND`（HTTP 404）

**请求示例：**
```
GET /api/payments/PAY-123/history
```

**响应示例：**
```json
{
  "success": true,
  "data": [
    { "fromStatus": null,       "toStatus": "CREATED",   "changedAt": "2024-01-01T10:00:00Z" },
    { "fromStatus": "CREATED",  "toStatus": "VALIDATED", "changedAt": "2024-01-01T10:01:00Z" },
    { "fromStatus": "VALIDATED","toStatus": "SENT",      "changedAt": "2024-01-01T10:02:00Z" },
    { "fromStatus": "SENT",     "toStatus": "COMPLETED", "changedAt": "2024-01-01T10:03:00Z" }
  ],
  "errorCode": null,
  "message": null
}
```

---

## Epic 4：支付检索与筛选

### US-006：按状态筛选支付列表

> 作为一名**财务操作员**，我希望能按支付状态筛选支付列表（如只查看 `FAILED` 的支付），以便快速定位需要处理的记录。

**验收条件：**
- [ ] 支持按 `status` 参数过滤：`CREATED`、`VALIDATED`、`SENT`、`COMPLETED`、`FAILED`
- [ ] 支持分页返回（`page`、`size`），响应包含 `total`、`pages`、`current` 和数据列表
- [ ] 不传 `status` 时返回所有支付
- [ ] 传入非法 `status` 值时返回 `VALIDATION_FAILED`（HTTP 400）

**请求示例：**
```
GET /api/payments?status=FAILED&page=1&size=10
```

**响应示例：**
```json
{
  "success": true,
  "data": {
    "total": 3,
    "pages": 1,
    "current": 1,
    "records": [
      { "id": "PAY-456", "status": "FAILED", "amount": 500.00, "currency": "USD" }
    ]
  },
  "errorCode": null,
  "message": null
}
```

---

## Epic 5：支付状态流转

### US-007：推进支付状态

> 作为一名**系统/后台服务**，我希望能将支付状态按合法路径推进（如 `CREATED` → `VALIDATED`），以驱动支付生命周期向前运行。

**验收条件：**
- [ ] 合法流转路径：`CREATED→VALIDATED`、`VALIDATED→SENT`、`SENT→COMPLETED`
- [ ] 任意阶段均可流转至 `FAILED`
- [ ] 非法流转（如 `COMPLETED→CREATED`）返回 `INVALID_STATUS_TRANSITION`（HTTP 400）
- [ ] `FAILED` 为终态，不可再发生任何流转
- [ ] 状态变更同时写入状态历史表

**状态流转图：**
```
CREATED → VALIDATED → SENT → COMPLETED
   ↓           ↓        ↓
 FAILED      FAILED   FAILED
```

**请求示例：**
```json
PATCH /api/payments/PAY-123/status
{
  "status": "VALIDATED"
}
```

---

### US-008：验证支付规则

> 作为一名**系统**，当支付从 `CREATED` 转为 `VALIDATED` 时，我希望自动执行三层业务规则校验，以确保只有合法支付才能进入后续流程。

**验收条件：**

**层级 1 — 金额校验：**
- [ ] 金额必须 > 0；否则返回 `INVALID_AMOUNT`
- [ ] 金额必须 < 1,000,000；否则返回 `INVALID_AMOUNT`
- [ ] 小数位数不得超过两位；否则返回 `INVALID_AMOUNT`

**层级 2 — 账户校验：**
- [ ] 来源账户与目标账户不能为同一账户；否则返回 `INVALID_ACCOUNT`
- [ ] 两个账户必须在系统初始化数据中真实存在；否则返回 `INVALID_ACCOUNT`

**层级 3 — 币种校验：**
- [ ] 货币代码必须符合 ISO 4217 且在支持白名单内；否则返回 `INVALID_CURRENCY`

**通过后：**
- [ ] 全部三层校验通过，状态更新为 `VALIDATED`
- [ ] 任一校验失败，状态更新为 `FAILED` 并记录对应 `errorCode`

---

## 错误码参考

| 错误码 | HTTP 状态码 | 业务场景 |
| :--- | :--- | :--- |
| `VALIDATION_FAILED` | 400 | 基础表单字段格式校验失败 |
| `INSUFFICIENT_FUNDS` | 400 | 付款账户可用余额不足 |
| `INVALID_ACCOUNT` | 400 | 账户不存在或来源与目标账户相同 |
| `INVALID_CURRENCY` | 400 | 不支持的货币代码 |
| `INVALID_AMOUNT` | 400 | 金额为零、负数或超过单笔限额 |
| `DUPLICATE_PAYMENT` | 409 | 幂等密钥冲突 |
| `INVALID_STATUS_TRANSITION` | 400 | 非法状态流转 |
| `PAYMENT_NOT_FOUND` | 404 | 支付记录不存在 |
| `PROCESSING_ERROR` | 500 | 后端非预期异常 |
| `NETWORK_ERROR` | 503 | 模拟通道超时且重试耗尽 |

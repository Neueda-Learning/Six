# 支付系统接口 Postman 测试文档

本文档面向在 Postman 中手动测试后端接口，覆盖全部 9 个接口的请求方法、URL、请求体、成功响应和典型失败响应。

## 0. 测试前准备

### 0.1 基础信息
- Base URL：`http://localhost:8080`
- 所有请求（除 GET）Header 需要设置：`Content-Type: application/json`
- 统一响应结构：
```json
{
  "success": true,
  "data": {},
  "errorCode": null,
  "message": "OK"
}
```
失败时 `success` 为 `false`，`data` 为 `null`，`errorCode`/`message` 携带具体错误信息。

### 0.2 重要提示：ID 是字符串
支付主键 `id` 由雪花算法生成（19 位数字），后端已将其序列化为**字符串**（如 `"2081937140881129474"`），Postman 里请求路径参数和响应比对时都以字符串处理，不要用数字类型转换，否则会精度丢失导致查询不到记录。

### 0.3 重要提示：自动状态推进定时任务
项目里有一个定时任务（[PaymentAutoTransitionScheduler](backend/src/main/java/com/example/payments/scheduler/PaymentAutoTransitionScheduler.java)），默认每 5 秒扫描一次处于 `CREATED`/`VALIDATED`/`SENT` 的支付并自动推进一步（最终会到 `COMPLETED`，不会自动到 `FAILED`）。

**这会影响你的测试**：
- 种子数据里 `id=2`（SENT）、`id=3`（VALIDATED）、`id=4`（CREATED）会在应用启动后几秒到几十秒内被自动推进为 `COMPLETED`。如果你想测试"查询中间状态"，请尽快在应用刚启动时查询，或临时把 [application.yml](backend/src/main/resources/application.yml) 里的 `payments.auto-transition.fixed-delay-ms` 改大（比如 `600000`）后重启再测。
- 如果你想手动测试 `PATCH /{id}/status` 把某条记录流转到某个状态，建议使用你自己新建的支付，并尽快在定时任务下一次 tick 之前完成操作。

### 0.4 种子数据（可直接用于查询类接口测试）

| id | idempotencyKey | fromAccount | toAccount | amount | currency | status | errorCode |
|---|---|---|---|---|---|---|---|
| 1 | seed-happy-completed-001 | ACC10001 | ACC20002 | 1200.50 | USD | COMPLETED | - |
| 2 | seed-in-flight-sent-002 | ACC10002 | ACC20003 | 500.00 | EUR | SENT（会被自动推进） | - |
| 3 | seed-in-flight-validated-003 | ACC10003 | ACC20001 | 99.99 | GBP | VALIDATED（会被自动推进） | - |
| 4 | seed-in-flight-created-004 | ACC10001 | ACC20003 | 250.00 | USD | CREATED（会被自动推进） | - |
| 5 | seed-failed-validation-005 | ACC10002 | ACC20002 | 8000.00 | USD | FAILED | INSUFFICIENT_FUNDS |
| 6 | seed-failed-network-006 | ACC10003 | ACC20001 | 750.00 | EUR | FAILED | NETWORK_ERROR |

可用账户（账户校验白名单）：`ACC10001` `ACC10002` `ACC10003` `ACC20001` `ACC20002` `ACC20003`

支持币种白名单：`USD` `EUR` `GBP`

---

## 1. 创建支付 POST /api/payments

### 请求
- Method：`POST`
- URL：`http://localhost:8080/api/payments`
- Body（raw / JSON）：
```json
{
  "idempotencyKey": "postman-test-001",
  "fromAccount": "ACC10001",
  "toAccount": "ACC20001",
  "amount": 100.50,
  "currency": "USD",
  "remark": "postman测试创建"
}
```

### 成功响应（200）
新建成功，状态固定为 `CREATED`：
```json
{
  "success": true,
  "data": {
    "id": "2081937140881129474",
    "idempotencyKey": "postman-test-001",
    "fromAccount": "ACC10001",
    "toAccount": "ACC20001",
    "amount": 100.50,
    "currency": "USD",
    "status": "CREATED",
    "errorCode": null,
    "errorMessage": null,
    "remark": "postman测试创建",
    "createdAt": "2026-07-28T10:00:00",
    "updatedAt": "2026-07-28T10:00:00"
  },
  "errorCode": null,
  "message": "OK"
}
```

### 幂等命中（200）
用**同一个** `idempotencyKey` 再提交一次（哪怕其他字段不同），直接返回已存在的记录，不会新建：
- 响应结构与上面一致，`data` 是第一次创建时的记录（不是本次提交的新数据）。

### 失败响应示例

**金额非法（负数/零/超限/超两位小数）→ `INVALID_AMOUNT`，400**
```json
{ "amount": -10 }
```
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_AMOUNT",
  "message": "支付金额必须大于 0"
}
```

**货币不在白名单 → `INVALID_CURRENCY`，400**（如 `"currency": "CNY"`）
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_CURRENCY",
  "message": "不支持的货币类型: CNY"
}
```

**源账户与目标账户相同 / 账户不存在 → `INVALID_ACCOUNT`，400**（如 `fromAccount` 和 `toAccount` 都是 `ACC10001`，或用了不存在的 `ACC99999`）
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_ACCOUNT",
  "message": "源账户与目标账户不能相同"
}
```

**必填字段缺失（如缺 `idempotencyKey`）→ `VALIDATION_FAILED`，400**
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_FAILED",
  "message": "idempotencyKey: 不能为空"
}
```

---

## 2. 查询支付详情 GET /api/payments/{id}

### 请求
- Method：`GET`
- URL：`http://localhost:8080/api/payments/1`（用种子数据 id=1 举例）

### 成功响应（200）
```json
{
  "success": true,
  "data": {
    "id": "1",
    "idempotencyKey": "seed-happy-completed-001",
    "fromAccount": "ACC10001",
    "toAccount": "ACC20002",
    "amount": 1200.50,
    "currency": "USD",
    "status": "COMPLETED",
    "errorCode": null,
    "errorMessage": null,
    "remark": "invoice-2026-07-seed-01",
    "createdAt": "2026-07-20T09:00:00",
    "updatedAt": "2026-07-20T09:03:00"
  },
  "errorCode": null,
  "message": "OK"
}
```

### 失败响应：id 不存在 → `PAYMENT_NOT_FOUND`，404
URL：`http://localhost:8080/api/payments/999999`
```json
{
  "success": false,
  "data": null,
  "errorCode": "PAYMENT_NOT_FOUND",
  "message": "支付记录不存在: 999999"
}
```

---

## 3. 查询支付状态历史 GET /api/payments/{id}/history

### 请求
- Method：`GET`
- URL：`http://localhost:8080/api/payments/1/history`

### 成功响应（200）
```json
{
  "success": true,
  "data": [
    { "id": "1", "paymentId": "1", "fromStatus": null, "toStatus": "CREATED", "errorCode": null, "errorMessage": null, "remark": null, "operator": "SYSTEM", "createdAt": "2026-07-20T09:00:00" },
    { "id": "2", "paymentId": "1", "fromStatus": "CREATED", "toStatus": "VALIDATED", "errorCode": null, "errorMessage": null, "remark": null, "operator": "SYSTEM", "createdAt": "2026-07-20T09:01:00" },
    { "id": "3", "paymentId": "1", "fromStatus": "VALIDATED", "toStatus": "SENT", "errorCode": null, "errorMessage": null, "remark": null, "operator": "SYSTEM", "createdAt": "2026-07-20T09:02:00" },
    { "id": "4", "paymentId": "1", "fromStatus": "SENT", "toStatus": "COMPLETED", "errorCode": null, "errorMessage": null, "remark": null, "operator": "SYSTEM", "createdAt": "2026-07-20T09:03:00" }
  ],
  "errorCode": null,
  "message": "OK"
}
```

### 失败响应：id 不存在 → `PAYMENT_NOT_FOUND`，404
与第 2 节一致，`message` 为 `"支付记录不存在: {id}"`。

---

## 4. 分页查询支付列表 GET /api/payments

### 请求
- Method：`GET`
- URL 示例（无筛选）：`http://localhost:8080/api/payments?page=1&size=10`
- URL 示例（按状态筛选）：`http://localhost:8080/api/payments?status=FAILED&page=1&size=10`
- URL 示例（按关键字筛选，数字关键字会精确匹配 id，同时匹配 remark）：`http://localhost:8080/api/payments?keyword=seed-failed`

Query 参数说明：

| 参数 | 是否必填 | 默认值 | 说明 |
|---|---|---|---|
| status | 否 | 无 | 精确匹配 `CREATED`/`VALIDATED`/`SENT`/`COMPLETED`/`FAILED` |
| keyword | 否 | 无 | 数字视为支付 id 精确匹配，同时模糊匹配 remark；非数字仅模糊匹配 remark |
| page | 否 | 1 | 页码，从 1 开始 |
| size | 否 | 10 | 每页条数 |

### 成功响应（200）
```json
{
  "success": true,
  "data": {
    "list": [
      { "id": "6", "idempotencyKey": "seed-failed-network-006", "fromAccount": "ACC10003", "toAccount": "ACC20001", "amount": 750.00, "currency": "EUR", "status": "FAILED", "errorCode": "NETWORK_ERROR", "errorMessage": "mock network timeout after max retries", "remark": "invoice-2026-07-seed-06", "createdAt": "2026-07-25T14:00:00", "updatedAt": "2026-07-25T14:03:00" },
      { "id": "5", "idempotencyKey": "seed-failed-validation-005", "fromAccount": "ACC10002", "toAccount": "ACC20002", "amount": 8000.00, "currency": "USD", "status": "FAILED", "errorCode": "INSUFFICIENT_FUNDS", "errorMessage": "mock insufficient balance in from_account", "remark": "invoice-2026-07-seed-05", "createdAt": "2026-07-24T13:00:00", "updatedAt": "2026-07-24T13:01:00" }
    ],
    "total": 2,
    "page": 1,
    "size": 10
  },
  "errorCode": null,
  "message": "OK"
}
```

### 失败响应
该接口所有参数都是可选的，没有强制校验规则，正常情况下不会返回业务失败；如果 `status` 传入非枚举值（如 `status=XXX`），不会报错，只是查询结果为空列表（`total: 0`）。

---

## 5. 手动更新支付状态 PATCH /api/payments/{id}/status

### 请求（合法流转示例：CREATED → VALIDATED）
- Method：`PATCH`
- URL：`http://localhost:8080/api/payments/{刚创建的id}/status`
- Body：
```json
{
  "targetStatus": "VALIDATED"
}
```

### 成功响应（200）
```json
{
  "success": true,
  "data": {
    "id": "2081937140881129474",
    "status": "VALIDATED",
    "errorCode": null,
    "errorMessage": null,
    "updatedAt": "2026-07-28T10:05:00"
  },
  "errorCode": null,
  "message": "OK"
}
```
（省略字段与创建接口返回结构一致）

### 请求（模拟失败示例：任意非终态 → FAILED）
```json
{
  "targetStatus": "FAILED",
  "errorCode": "NETWORK_ERROR",
  "errorMessage": "mock network timeout after max retries"
}
```
成功响应中 `status` 为 `FAILED`，`errorCode`/`errorMessage` 回显你传入的值。

### 失败响应示例

**非法状态流转（越级/逆向/终态再流转）→ `INVALID_STATUS_TRANSITION`，400**  
比如对种子数据 `id=1`（已是 `COMPLETED`）执行：
```json
{ "targetStatus": "CREATED" }
```
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_STATUS_TRANSITION",
  "message": "不允许从 COMPLETED 流转到 CREATED"
}
```

**targetStatus 不是合法枚举值 → `VALIDATION_FAILED`，400**
```json
{ "targetStatus": "UNKNOWN" }
```
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_FAILED",
  "message": "targetStatus 不是合法的状态值: UNKNOWN"
}
```

**id 不存在 → `PAYMENT_NOT_FOUND`，404**（同第 2 节）

---

## 6. 软删除（移入回收站） PATCH /api/payments/{id}/delete

### 请求
- Method：`PATCH`
- URL：`http://localhost:8080/api/payments/{id}/delete`
- Body：无需请求体

### 成功响应（200）
```json
{
  "success": true,
  "data": {
    "id": "1",
    "status": "COMPLETED"
  },
  "errorCode": null,
  "message": "OK"
}
```
删除后：
- 该记录会从 `GET /api/payments`、`GET /api/payments/{id}` 等常规接口的可见范围消失（视为不存在）。
- 可以在 `GET /api/payments/recycle-bin` 中查到。

### 失败响应：id 不存在或已被删除 → `PAYMENT_NOT_FOUND`，404
（同第 2 节；对已经软删除过的记录再次调用本接口，会因为查询时已过滤掉已删除记录而返回 404）

---

## 7. 查询回收站列表 GET /api/payments/recycle-bin

### 请求
- Method：`GET`
- URL：`http://localhost:8080/api/payments/recycle-bin?page=1&size=10`
- 可选参数：`keyword`（同列表接口的关键字匹配规则）

### 成功响应（200）
```json
{
  "success": true,
  "data": {
    "list": [
      { "id": "1", "status": "COMPLETED", "idempotencyKey": "seed-happy-completed-001" }
    ],
    "total": 1,
    "page": 1,
    "size": 10
  },
  "errorCode": null,
  "message": "OK"
}
```
说明：仅返回 30 天回收窗口内、尚未被永久删除的记录。

### 失败响应
无强制校验规则，参数缺失或非法关键字不会报错，只会返回空列表。

---

## 8. 从回收站恢复 PATCH /api/payments/{id}/restore

### 请求
- Method：`PATCH`
- URL：`http://localhost:8080/api/payments/{id}/restore`
- Body：无需请求体

### 成功响应（200）
```json
{
  "success": true,
  "data": {
    "id": "1",
    "status": "COMPLETED"
  },
  "errorCode": null,
  "message": "OK"
}
```
恢复后该记录会重新出现在常规列表/详情接口中。

### 失败响应：记录不在回收站/已超 30 天/已被永久删除 → `RECYCLE_BIN_RECORD_NOT_FOUND`，404
```json
{
  "success": false,
  "data": null,
  "errorCode": "RECYCLE_BIN_RECORD_NOT_FOUND",
  "message": "回收站记录不存在或已超过恢复期限: 1"
}
```
（对一条从未被删除过的记录调用本接口，也会触发该错误）

---

## 9. 永久删除 PATCH /api/payments/{id}/permanent-delete

### 请求
- Method：`PATCH`
- URL：`http://localhost:8080/api/payments/{id}/permanent-delete`
- Body：无需请求体
- 前提：该记录必须先经过第 6 节的软删除，处于回收站中

### 成功响应（200）
```json
{
  "success": true,
  "data": {
    "id": "1",
    "status": "COMPLETED"
  },
  "errorCode": null,
  "message": "OK"
}
```
说明：该操作只是标记 `permanentlyDeletedAt`，不会物理删除数据库行，但之后该记录不会再出现在任何列表/回收站/恢复接口中。

### 失败响应：记录不在回收站/已超期/已被永久删除过 → `RECYCLE_BIN_RECORD_NOT_FOUND`，404
（同第 8 节）

---

## 10. 建议的 Postman 测试顺序（一条完整链路）

1. `POST /api/payments` 创建一条新支付，记录返回的字符串 `id`。
2. `GET /api/payments/{id}` 确认详情正确。
3. `PATCH /api/payments/{id}/status` 目标 `VALIDATED`，确认成功。
4. `GET /api/payments/{id}/history` 确认多出一条 `CREATED→VALIDATED` 记录。
5. `PATCH /api/payments/{id}/status` 目标 `FAILED`（带 `errorCode`/`errorMessage`），确认成功并终止流转。
6. `PATCH /api/payments/{id}/status` 再次尝试改成 `SENT`，验证返回 `INVALID_STATUS_TRANSITION`（终态不可再流转）。
7. `PATCH /api/payments/{id}/delete` 软删除，`GET /api/payments/{id}` 验证变为 404。
8. `GET /api/payments/recycle-bin` 确认能查到该记录。
9. `PATCH /api/payments/{id}/restore` 恢复，`GET /api/payments/{id}` 验证恢复正常。
10. 再次 `PATCH /api/payments/{id}/delete` → `PATCH /api/payments/{id}/permanent-delete`，`PATCH /api/payments/{id}/restore` 验证返回 `RECYCLE_BIN_RECORD_NOT_FOUND`。

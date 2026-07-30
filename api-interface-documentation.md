# 支付处理系统前后端接口文档

> 本文档描述前端（Vue3 + Element Plus）与后端（Spring Boot）之间的 REST API 契约，供前后端联调与后续维护参考。页面文案使用英文，本文档使用中文。

---

## 1. 基础约定

### 1.1 服务地址

| 环境 | 前端访问地址 | 后端地址 | 说明 |
|---|---|---|---|
| 开发环境 | `http://localhost:5173` | `http://localhost:8080` | 前端通过 Vite 代理将 `/api/**` 转发到后端（见 `vite.config.js`） |
| 生产环境 | 由部署方配置 | 由部署方配置 | 需在 `.env.production` 中配置 `VITE_API_BASE_URL` |

前端所有请求统一走 `fronted/api/http.js` 中创建的 Axios 实例，`baseURL` 默认值为 `/api`。

### 1.2 统一响应结构

所有接口无论成功或失败，均返回如下信封结构：

```json
{
  "success": true,
  "data": {},
  "errorCode": null,
  "message": "OK"
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| success | boolean | 请求是否业务成功 |
| data | object / array / null | 业务数据，失败时通常为 `null` |
| errorCode | string / null | 业务错误码，成功时为 `null`，详见第 4 节 |
| message | string | 提示信息，成功默认 `"OK"`，失败时为具体错误描述 |

前端拦截器（`fronted/api/http.js`）行为：
- 当 `success === true` 时，返回信封整体（即 `{ success, data, errorCode, message }`），页面通过 `res.data` 取出业务数据。
- 当 `success === false` 时，统一通过 `ElMessage.error` 弹出 `message`，并 `reject` 给调用方，页面无需重复弹提示。
- 当 HTTP 状态码非 2xx（网络错误、5xx 等）时，同样统一弹出错误提示。

### 1.3 分页数据结构

列表类接口的 `data` 字段为如下分页结构：

```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "size": 10
}
```

---

## 2. 支付状态机

### 2.1 状态定义

| 状态 | 说明 |
|---|---|
| CREATED | 支付已提交，尚未通过验证 |
| VALIDATED | 支付已通过全部验证规则，可发送 |
| SENT | 支付已发送到目标系统（内部模拟） |
| COMPLETED | 支付已成功处理并确认（终态） |
| FAILED | 支付在流程中某节点失败，携带错误码（终态） |

### 2.2 合法状态转换

| 当前状态 | 允许的目标状态 |
|---|---|
| CREATED | VALIDATED, FAILED |
| VALIDATED | SENT, FAILED |
| SENT | COMPLETED, FAILED |
| COMPLETED | 无（终态） |
| FAILED | 无（终态） |

非法流转（如 `COMPLETED → CREATED`）将返回 `INVALID_STATUS_TRANSITION`（HTTP 400）。

---

## 3. 接口清单

| Method | Path | 说明 | 前端封装函数（`fronted/api/payment.js`） |
|---|---|---|---|
| POST | `/api/payments` | 创建支付（幂等） | `createPayment(payload)` |
| GET | `/api/payments/{id}` | 查询支付详情 | `getPaymentById(id)` |
| GET | `/api/payments/{id}/history` | 查询支付状态历史 | `getPaymentHistory(id)` |
| GET | `/api/payments` | 支付列表分页 + 筛选 | `listPayments(params)` |
| PATCH | `/api/payments/{id}/status` | 手工状态流转（测试/演示用） | `updatePaymentStatus(id, payload)` |

---

## 4. 错误码

| 错误码 | HTTP 状态码 | 业务场景 |
|---|---|---|
| VALIDATION_FAILED | 400 | 基础表单输入字段格式校验失败 |
| INSUFFICIENT_FUNDS | 400 | 付款账户可用余额不足 |
| INVALID_ACCOUNT | 400 | 账户格式非法或账户在系统数据中不存在 |
| INVALID_CURRENCY | 400 | 不支持的或不合规的货币符号 |
| INVALID_AMOUNT | 400 | 金额为负、零或超过单笔百万限制 |
| DUPLICATE_PAYMENT | 409 | 幂等密钥冲突，交易处理中且禁止重试 |
| INVALID_STATUS_TRANSITION | 400 | 企图越级或逆向流转状态机状态 |
| PAYMENT_NOT_FOUND | 404 | 检索的支付记录主键 ID 不存在 |
| PROCESSING_ERROR | 500 | 后端运行时非预期异常 |
| NETWORK_ERROR | 503 | 模拟通道通信超时且重试次数耗尽 |

前端页面通过 `payment.status === 'FAILED'` 判断展示 `errorCode` + `errorMessage`（详情页），其余错误码统一由 `http.js` 拦截器弹窗提示。

---

## 5. 接口详情

### 5.1 创建支付

- **Method / Path**：`POST /api/payments`
- **使用页面**：`PaymentCreate.vue`
- **说明**：创建一笔新支付；若 `idempotencyKey` 与已存在支付相同，直接返回已存在的支付（HTTP 200，`success: true`），不会重复创建。

**请求体**

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| idempotencyKey | string | 是 | 长度 ≤ 64，前端自动生成 UUID，可手动修改 |
| fromAccount | string | 是 | 需在系统账户数据中存在，且与 toAccount 不同 |
| toAccount | string | 是 | 需在系统账户数据中存在，且与 fromAccount 不同 |
| amount | number | 是 | > 0 且 ≤ 1,000,000，最多两位小数 |
| currency | string | 是 | ISO 4217 代码，当前白名单：USD / EUR / GBP |
| remark | string | 否 | 备注信息 |

```json
{
  "idempotencyKey": "b4b7c4c0-ef3f-4a2f-8da3-57fa7a111111",
  "fromAccount": "ACC10001",
  "toAccount": "ACC20002",
  "amount": 1200.50,
  "currency": "USD",
  "remark": "invoice-2026-07"
}
```

**成功响应**

```json
{
  "success": true,
  "data": {
    "id": 7,
    "idempotencyKey": "b4b7c4c0-ef3f-4a2f-8da3-57fa7a111111",
    "fromAccount": "ACC10001",
    "toAccount": "ACC20002",
    "amount": 1200.50,
    "currency": "USD",
    "status": "CREATED",
    "errorCode": null,
    "errorMessage": null,
    "remark": "invoice-2026-07",
    "createdAt": "2026-07-27T10:00:00",
    "updatedAt": "2026-07-27T10:00:00"
  },
  "errorCode": null,
  "message": "OK"
}
```

**失败场景**：`INVALID_AMOUNT`、`INVALID_ACCOUNT`、`INVALID_CURRENCY`、`DUPLICATE_PAYMENT`、`VALIDATION_FAILED`。

---

### 5.2 查询账户列表

- **Method / Path**：`GET /api/accounts`
- **使用页面**：账户余额独立页面

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 否 | 按账户号或户主名称模糊搜索 |

**成功响应**

```json
{
  "success": true,
  "data": [
    {
      "accountNo": "ACC10001",
      "ownerName": "Alice Zhang",
      "currency": "USD",
      "status": "ACTIVE",
      "balance": 99988.89
    },
    {
      "accountNo": "ACC20001",
      "ownerName": "David Li",
      "currency": "USD",
      "status": "ACTIVE",
      "balance": 100011.11
    }
  ],
  "errorCode": null,
  "message": "OK"
}
```

---

### 5.3 查询账户余额

- **Method / Path**：`GET /api/accounts/{accountNo}/balance`
- **使用场景**：余额查询、转账结果核对、支付完成后的账户对账

**路径参数**

| 参数 | 类型 | 说明 |
|---|---|---|
| accountNo | string | 账户号，如 `ACC10001` |

**成功响应**

```json
{
  "success": true,
  "data": {
    "accountNo": "ACC10001",
    "ownerName": "Alice Zhang",
    "currency": "USD",
    "status": "ACTIVE",
    "balance": 99988.89
  },
  "errorCode": null,
  "message": "OK"
}
```

**失败场景**：`INVALID_ACCOUNT`（HTTP 404）。

---

### 5.4 查询支付详情

- **Method / Path**：`GET /api/payments/{id}`
- **使用页面**：`PaymentDetail.vue`

**路径参数**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | number | 支付主键 ID |

**成功响应**

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

**失败场景**：`PAYMENT_NOT_FOUND`（HTTP 404）。

---

### 5.5 查询支付状态历史

- **Method / Path**：`GET /api/payments/{id}/history`
- **使用页面**：`PaymentDetail.vue`（时间线展示）

**成功响应**（`data` 为数组）

```json
{
  "success": true,
  "data": [
    {
      "id": 11,
      "paymentId": 5,
      "fromStatus": null,
      "toStatus": "CREATED",
      "errorCode": null,
      "errorMessage": null,
      "remark": null,
      "operator": "SYSTEM",
      "createdAt": "2026-07-24T13:00:00"
    },
    {
      "id": 12,
      "paymentId": 5,
      "fromStatus": "CREATED",
      "toStatus": "FAILED",
      "errorCode": "INSUFFICIENT_FUNDS",
      "errorMessage": "mock insufficient balance in from_account",
      "remark": null,
      "operator": "SYSTEM",
      "createdAt": "2026-07-24T13:01:00"
    }
  ],
  "errorCode": null,
  "message": "OK"
}
```

**失败场景**：`PAYMENT_NOT_FOUND`（HTTP 404）。

---

### 5.6 支付列表分页与筛选

- **Method / Path**：`GET /api/payments`
- **使用页面**：`PaymentList.vue`

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| status | string | 否 | 无（不筛选） | 取值：CREATED / VALIDATED / SENT / COMPLETED / FAILED |
| keyword | string | 否 | 无 | 匹配支付 ID 或备注 |
| page | number | 否 | 1 | 页码，从 1 开始 |
| size | number | 否 | 10 | 每页条数 |

**成功响应**

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "fromAccount": "ACC10001",
        "toAccount": "ACC20002",
        "amount": 1200.50,
        "currency": "USD",
        "status": "COMPLETED",
        "remark": "invoice-2026-07-seed-01",
        "createdAt": "2026-07-20T09:00:00"
      }
    ],
    "total": 6,
    "page": 1,
    "size": 10
  },
  "errorCode": null,
  "message": "OK"
}
```

---

### 5.5 手工状态流转（测试/演示用）

- **Method / Path**：`PATCH /api/payments/{id}/status`
- **使用场景**：课程演示、模拟失败、验证非法流转拦截；不在标准页面流程中暴露入口，可通过 Swagger UI 调试。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| targetStatus | string | 是 | 目标状态，需符合状态机合法转换规则 |
| errorCode | string | 否 | 目标状态为 FAILED 时建议填写 |
| errorMessage | string | 否 | 目标状态为 FAILED 时建议填写 |
| remark | string | 否 | 补充备注 |

```json
{
  "targetStatus": "FAILED",
  "errorCode": "NETWORK_ERROR",
  "errorMessage": "mock network timeout"
}
```

**失败场景**：`INVALID_STATUS_TRANSITION`（HTTP 400）、`PAYMENT_NOT_FOUND`（HTTP 404）。

---

## 6. 前端页面与接口对应关系

| 页面 | 路由 | 调用接口 |
|---|---|---|
| `PaymentList.vue` | `/` | `listPayments(params)` |
| `PaymentCreate.vue` | `/payments/create` | `createPayment(payload)` |
| `PaymentDetail.vue` | `/payments/:id` | `getPaymentById(id)`、`getPaymentHistory(id)` |

---

## 7. 前端字段与后端 DTO 对照

### 7.1 CreatePaymentRequest（创建支付请求）

| 前端表单字段 | 后端字段 | 类型 |
|---|---|---|
| idempotencyKey | idempotencyKey | String |
| fromAccount | fromAccount | String |
| toAccount | toAccount | String |
| amount | amount | BigDecimal |
| currency | currency | String |
| remark | remark | String |

### 7.2 PaymentResponse（支付详情响应）

| 后端字段 | 类型 | 前端展示位置 |
|---|---|---|
| id | Long | 详情页 / 列表页 |
| idempotencyKey | String | 详情页 |
| fromAccount / toAccount | String | 详情页 / 列表页 |
| amount / currency | BigDecimal / String | 详情页 / 列表页 |
| status | String | 详情页 / 列表页（状态标签） |
| errorCode / errorMessage | String | 详情页失败提示（仅 FAILED 时展示） |
| remark | String | 详情页 / 列表页 |
| createdAt / updatedAt | LocalDateTime | 详情页 |

### 7.3 PaymentHistoryItemResponse（状态历史响应）

| 后端字段 | 类型 | 前端展示位置 |
|---|---|---|
| fromStatus / toStatus | String | 时间线节点标题 |
| operator | String | 时间线节点副标题 |
| errorCode / errorMessage | String | 时间线节点错误提示 |
| remark | String | 时间线节点备注 |
| createdAt | LocalDateTime | 时间线节点时间戳 |

---

## 8. 待后端补齐事项

当前后端 Controller/Service 仍为骨架（`// todo` 占位），本文档中的响应示例为**契约约定**而非当前真实返回值。后端实现时需保证：

1. 响应字段命名与本文档第 7 节一致（驼峰命名）。
2. `errorCode` 取值必须来自 `ErrorCode` 枚举，与第 4 节表格一致。
3. 分页 `data` 结构必须为 `{ list, total, page, size }`。
4. 创建支付幂等命中时仍返回 `success: true` + 已存在支付数据，不返回 `DUPLICATE_PAYMENT`（该错误码仅用于极端并发兜底场景，见设计文档 6.5 节）。

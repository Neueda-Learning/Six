# Payments Processing System 测试用例

依据 [payment_processing.md](../Downloads/payment_processing.md)（尤其 Appendix F 测试建议）与当前项目实际实现（[PaymentValidator.java](backend/src/main/java/com/example/payments/validator/PaymentValidator.java)、[PaymentStateMachine.java](backend/src/main/java/com/example/payments/statemachine/PaymentStateMachine.java)、[PaymentServiceImpl.java](backend/src/main/java/com/example/payments/service/impl/PaymentServiceImpl.java)）整理。仅列用例，不含测试代码。

[TOC]

## 测试环境与基础数据

- 后端：`http://localhost:8080/api/payments`
- 种子账户（[data.sql](backend/src/main/resources/db/data.sql)）：`ACC10001/ACC10002/ACC10003`（源账户示例）、`ACC20001/ACC20002/ACC20003`（目标账户示例）
- 支持币种白名单：`USD`、`EUR`、`GBP`
- 单笔金额上限：`< 1,000,000`，最多两位小数
- 种子支付数据：id=1（COMPLETED）、id=2（SENT）、id=3（VALIDATED）、id=4（CREATED）、id=5（FAILED / INSUFFICIENT_FUNDS）、id=6（FAILED / NETWORK_ERROR）

---

## 一、创建支付 —— Happy Path

| 用例 ID | 描述 | 输入 | 预期结果 |
|---|---|---|---|
| TC-01 | 正常创建支付 | fromAccount=`ACC10001`, toAccount=`ACC20002`, amount=`100.50`, currency=`USD`, 唯一 idempotencyKey | HTTP 200/201，返回支付对象，`status=CREATED`；历史表新增一条 `null → CREATED`（operator=SYSTEM） |
| TC-02 | 创建后完整走完生命周期 | 依次调用状态流转接口：`CREATED→VALIDATED→SENT→COMPLETED` | 每步都成功，最终状态 `COMPLETED`，历史记录含 4 条完整时间线 |
| TC-03 | 三种受支持币种均可创建成功 | currency 分别为 `USD`/`EUR`/`GBP` | 均创建成功 |
| TC-04 | currency 小写输入自动归一化 | currency=`usd` | 创建成功且存储为大写 `USD` |

## 二、金额校验（`INVALID_AMOUNT`，400）

| 用例 ID | 描述 | 输入 | 预期结果 |
|---|---|---|---|
| TC-05 | 金额为 0 | amount=`0` | 400，`errorCode=INVALID_AMOUNT` |
| TC-06 | 金额为负数 | amount=`-10` | 400，`errorCode=INVALID_AMOUNT` |
| TC-07 | 金额等于上限临界值 | amount=`1000000` | 400，`errorCode=INVALID_AMOUNT`（代码用 `>=` 拒绝） |
| TC-08 | 金额刚好小于上限 | amount=`999999.99` | 创建成功 |
| TC-09 | 小数位超过两位 | amount=`10.123` | 400，`errorCode=INVALID_AMOUNT` |
| TC-10 | 金额为极小正数 | amount=`0.01` | 创建成功 |
| TC-11 | amount 字段缺失/为 null | 请求体不含 amount | 400，`errorCode=VALIDATION_FAILED` |

## 三、币种校验（`INVALID_CURRENCY`，400）

| 用例 ID | 描述 | 输入 | 预期结果 |
|---|---|---|---|
| TC-12 | 不在白名单内的合法 ISO 代码 | currency=`JPY` | 400，`errorCode=INVALID_CURRENCY` |
| TC-13 | 非法格式（非 3 位字母） | currency=`US` 或 `USDX` | 400，`errorCode=VALIDATION_FAILED`（DTO `@Size(min=3,max=3)` 先拦截） |
| TC-14 | currency 为空字符串/缺失 | currency=`""` | 400，`errorCode=VALIDATION_FAILED` |

## 四、账户校验（`INVALID_ACCOUNT`，400）

| 用例 ID | 描述 | 输入 | 预期结果 |
|---|---|---|---|
| TC-15 | 源账户与目标账户相同 | fromAccount=toAccount=`ACC10001` | 400，`errorCode=INVALID_ACCOUNT` |
| TC-16 | 源账户不存在 | fromAccount=`ACC99999` | 400，`errorCode=INVALID_ACCOUNT` |
| TC-17 | 目标账户不存在 | toAccount=`ACC99999` | 400，`errorCode=INVALID_ACCOUNT` |
| TC-18 | 校验优先级验证 | amount 非法 **且** 账户非法同时出现 | 应先命中 `INVALID_AMOUNT`（校验顺序：金额→币种→账户），需确认实际顺序符合预期 |

## 五、幂等性（`DUPLICATE_PAYMENT` / 幂等命中）

| 用例 ID | 描述 | 步骤 | 预期结果 |
|---|---|---|---|
| TC-19 | 相同 idempotencyKey 重复提交 | 用同一个 key 连续调用两次创建接口（参数完全一致） | 第二次返回 HTTP 200，返回第一次创建的同一条记录（同 `id`），未新建记录 |
| TC-20 | 相同 key 但请求体参数不同 | 第二次请求换了 amount/账户，但 idempotencyKey 相同 | 仍返回第一次创建的记录（当前实现只按 key 查重）——需确认是否符合预期设计 |
| TC-21 | 并发重复提交（模拟竞态） | 两个线程/请求同时用同一个新 idempotencyKey 创建 | 只应成功插入一条记录，另一个通过异常兜底逻辑查回同一条记录 |
| TC-22 | idempotencyKey 超长 | 65 个字符（超过 `@Size(max=64)`） | 400，`errorCode=VALIDATION_FAILED` |
| TC-23 | idempotencyKey 为空 | 空字符串 | 400，`errorCode=VALIDATION_FAILED` |

## 六、状态流转（`INVALID_STATUS_TRANSITION`，400）

| 用例 ID | 描述 | 输入 | 预期结果 |
|---|---|---|---|
| TC-24 | 合法流转：CREATED→VALIDATED | — | 200，状态更新，历史新增一条 |
| TC-25 | 合法流转：VALIDATED→SENT | — | 200 |
| TC-26 | 合法流转：SENT→COMPLETED | — | 200 |
| TC-27 | 任意阶段→FAILED（CREATED/VALIDATED/SENT→FAILED） | 分别测试三种 | 均 200，且携带 errorCode/errorMessage 一并写入 |
| TC-28 | 非法跳级：CREATED→SENT | — | 400，`INVALID_STATUS_TRANSITION` |
| TC-29 | 非法跳级：CREATED→COMPLETED | — | 400 |
| TC-30 | 非法逆向：COMPLETED→CREATED | 基于种子数据 id=1 | 400 |
| TC-31 | 终态不可再流转：FAILED→任意状态 | 基于种子数据 id=5/6 | 400，验证 FAILED 是真正的终态 |
| TC-32 | 终态不可再流转：COMPLETED→任意状态 | — | 400 |
| TC-33 | targetStatus 传入非法字符串 | targetStatus=`"XXXX"` | 400，`errorCode=VALIDATION_FAILED` |
| TC-34 | targetStatus 大小写不敏感 | targetStatus=`"validated"` | 应能正确解析为 `VALIDATED` |

## 七、查询类接口

| 用例 ID | 描述 | 预期结果 |
|---|---|---|
| TC-35 | 查询存在的支付详情 | 200，返回完整字段（非 FAILED 时 errorCode/errorMessage 应为 null） |
| TC-36 | 查询不存在的支付 ID | 404，`errorCode=PAYMENT_NOT_FOUND` |
| TC-37 | 查询不存在支付的历史 | 404，`errorCode=PAYMENT_NOT_FOUND` |
| TC-38 | 查询存在支付的历史 | 200，按时间正序排列，条数与状态流转次数一致 |
| TC-39 | 按状态筛选列表（如 `FAILED`） | 200，仅返回该状态记录 |
| TC-40 | 按关键字筛选（数字关键字） | keyword=`1`，同时匹配 ID 与备注 |
| TC-41 | 按关键字筛选（非数字关键字） | keyword=`invoice-2026-07-seed-05`，仅匹配备注 |
| TC-42 | 分页参数默认值 | 不传 page/size，默认 page=1, size=10 |
| TC-43 | 分页参数非法（如 page=0 或负数） | 应回退到默认值 1 |
| TC-44 | status 参数非法枚举值 | status=`"UNKNOWN"`，需确认返回空列表还是报错 |

## 八、并发与乐观锁

| 用例 ID | 描述 | 预期结果 |
|---|---|---|
| TC-45 | 两个请求同时对同一支付执行不同的状态流转 | 由 `@Version` 乐观锁保证只有一个成功写入，另一个应因版本冲突失败 |
| TC-46 | 版本冲突后重试 | 重新查询最新版本后重试流转，应能成功 |

## 九、网络失败模拟（`NETWORK_ERROR`，503）

| 用例 ID | 描述 | 预期结果 |
|---|---|---|
| TC-47 | 手动流转到 FAILED 并携带 `NETWORK_ERROR` | 200，历史记录与详情正确保存 errorCode/errorMessage |
| TC-48 | `network-max-retry: 3` 配置项 | 确认代码中是否真正使用该配置做重试逻辑 |

## 十、前端 UI 测试

| 用例 ID | 描述 | 预期结果 |
|---|---|---|
| TC-49 | 创建支付表单必填校验 | 留空必填字段提交，前端表单校验拦截，不发请求 |
| TC-50 | 前端"收款账户不能等于付款账户"校验 | 两账户填相同值，blur 时前端提示错误 |
| TC-51 | 幂等键自动生成与"重新生成"按钮 | 点击 Regenerate，idempotencyKey 变为新值 |
| TC-52 | 列表页状态筛选 | 选择 `FAILED`，表格只展示失败支付 |
| TC-53 | 列表页关键字搜索 + 分页联动 | 搜索后修改每页条数，应重置回第一页 |
| TC-54 | 详情页失败支付展示错误详情 | 打开种子数据 id=5，显示 `INSUFFICIENT_FUNDS` 错误码与描述 |
| TC-55 | 详情页状态历史时间线顺序与颜色 | 打开 id=1（COMPLETED），时间线按时间正序，COMPLETED 节点绿色 |
| TC-56 | 网络异常时的全局错误提示 | 后端关闭情况下操作前端，弹出统一错误提示，不白屏 |

---

## 待确认事项（代码审查中发现的模糊点）

- **TC-18**：金额与账户同时非法时，实际返回的 errorCode 是否符合"金额优先"的校验顺序预期
- **TC-20**：相同 idempotencyKey 但请求参数不同时，是否应该报错而不是静默返回旧记录
- **TC-44**：`status` 传入非枚举值时的行为未明确定义
- **TC-45**：乐观锁版本冲突时，服务层是否显式捕获异常并转换为友好的错误码（当前代码未见相关处理）
- **TC-48**：`network-max-retry` 配置是否已在业务逻辑中真正使用，还是预留 TODO

# Payments Processing System 测试用例

依据 [payment_processing.md](../Downloads/payment_processing.md)（尤其 Appendix F 测试建议）与当前项目实际实现（[PaymentValidator.java](backend/src/main/java/com/example/payments/validator/PaymentValidator.java)、[PaymentStateMachine.java](backend/src/main/java/com/example/payments/statemachine/PaymentStateMachine.java)、[PaymentServiceImpl.java](backend/src/main/java/com/example/payments/service/impl/PaymentServiceImpl.java)、[PaymentAutoTransitionScheduler.java](backend/src/main/java/com/example/payments/scheduler/PaymentAutoTransitionScheduler.java)、[PaymentController.java](backend/src/main/java/com/example/payments/controller/PaymentController.java)）整理。仅列用例，不含测试代码。

[TOC]

## 测试环境与基础数据

- 后端：`http://localhost:8080/api/payments`
- 种子账户（[data.sql](backend/src/main/resources/db/data.sql)）：`ACC10001/ACC10002/ACC10003`（源账户示例）、`ACC20001/ACC20002/ACC20003`（目标账户示例）
- 支持币种白名单：`USD`、`EUR`、`GBP`
- 单笔金额上限：`< 1,000,000`，最多两位小数
- 种子支付数据：id=1（COMPLETED）、id=2（SENT）、id=3（VALIDATED）、id=4（CREATED）、id=5（FAILED / INSUFFICIENT_FUNDS）、id=6（FAILED / NETWORK_ERROR）
- 回收站保留期：`RECYCLE_BIN_RETENTION_DAYS = 30` 天（[PaymentServiceImpl.java](backend/src/main/java/com/example/payments/service/impl/PaymentServiceImpl.java)）
- 自动状态推进调度：[PaymentAutoTransitionScheduler.java](backend/src/main/java/com/example/payments/scheduler/PaymentAutoTransitionScheduler.java) 每 `fixed-delay-ms`（默认 5000ms）推进一次，每次每笔待处理支付只推进一步；同时按 `payments.auto-transition.failure-probability`（默认 0.2）随机判定为 `FAILED`

### ⚠️ 编写自动化测试前必须处理：后台调度任务会干扰确定性断言

`PaymentAutoTransitionScheduler` 是一个全局 `@Component`，只要 Spring 容器启动就会在后台持续运行，每 5 秒对所有 `CREATED`/`VALIDATED`/`SENT` 的支付（含测试中新建的）做一次"要么推进、要么随机失败"的操作。如果直接用 `@SpringBootTest` 跑集成测试，会出现：
- 测试期望某支付停留在 `CREATED` 以便手动调用状态流转接口，结果调度任务先一步把它推进/弄失败，导致断言随机失败（flaky test）
- `TC-02`（完整生命周期）这类用例如果不加控制，可能被调度任务提前介入，得到非预期的中间状态或提前进入 `FAILED`

**建议在测试环境禁用或钝化该调度器**，例如：
1. 在 `application-test.yml`（新建）中把 `payments.auto-transition.initial-delay-ms` 设置成一个测试运行时间内不可能触发的极大值（如 `3600000`），且 `failure-probability: 0`；或
2. 用 `@MockBean` 替换 `PaymentAutoTransitionScheduler`，使其 `autoAdvancePendingPayments()` 变为空实现；或
3. 在测试类上加 `@TestPropertySource` 覆盖上述两个配置项

第十二章的用例专门针对该调度器本身的行为，**需要单独在允许其运行的测试环境下验证**，与其余章节（要求调度器被禁用）分开执行。

---

## 一、创建支付 —— Happy Path

| 用例 ID | 描述 | 输入 | 预期结果 |
|---|---|---|---|
| TC-01 | 正常创建支付 | fromAccount=`ACC10001`, toAccount=`ACC20002`, amount=`100.50`, currency=`USD`, 唯一 idempotencyKey | HTTP 200/201，返回支付对象，`status=CREATED`；历史表新增一条 `null → CREATED`（operator=SYSTEM） |
| TC-02 | 创建后完整走完生命周期 | 依次调用状态流转接口：`CREATED→VALIDATED→SENT→COMPLETED` | 每步都成功，最终状态 `COMPLETED`，历史记录含 4 条完整时间线（需先禁用自动推进调度器，否则可能被后台任务抢先推进或随机转为 FAILED，见上方环境准备说明） |
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

## 十一、回收站（软删除 / 恢复 / 永久删除）

涉及接口：`PATCH /{id}/delete`、`PATCH /{id}/restore`、`PATCH /{id}/permanent-delete`、`GET /recycle-bin`（[PaymentController.java](backend/src/main/java/com/example/payments/controller/PaymentController.java)）。核心规则：软删除后的记录在正常列表/详情/历史/状态流转接口中一律视为不存在（统一返回 `PAYMENT_NOT_FOUND`），只能通过回收站相关接口访问；超过 30 天保留期或已永久删除的记录，恢复/永久删除操作返回 `RECYCLE_BIN_RECORD_NOT_FOUND`。

| 用例 ID | 描述 | 输入/前置条件 | 预期结果 |
|---|---|---|---|
| TC-57 | 正常软删除一笔活跃支付 | 对种子数据 id=4（CREATED，未删除） 调用 `PATCH /4/delete` | 200，返回支付详情，`deletedAt` 被设置为当前时间 |
| TC-58 | 软删除后原有查询接口应视为不存在 | 紧接 TC-57，调用 `GET /4`、`GET /4/history`、`PATCH /4/status` | 均返回 404，`errorCode=PAYMENT_NOT_FOUND` |
| TC-59 | 软删除后不再出现在普通列表 | 紧接 TC-57，调用 `GET /api/payments`（不筛选状态） | 返回列表中不包含 id=4 |
| TC-60 | 软删除后出现在回收站列表 | 紧接 TC-57，调用 `GET /recycle-bin` | 返回列表中包含 id=4，且带有 `deletedAt`/`recoverableUntil` 字段 |
| TC-61 | 重复软删除同一条记录 | 对已软删除的记录再次调用 `PATCH /{id}/delete` | 404，`errorCode=PAYMENT_NOT_FOUND`（因为 `ensureActivePaymentExists` 已判定其不存在） |
| TC-62 | 软删除不存在的支付 ID | `PATCH /99999/delete` | 404，`errorCode=PAYMENT_NOT_FOUND` |
| TC-63 | 正常恢复回收站内的记录 | 对 TC-57 的 id=4 调用 `PATCH /4/restore` | 200，`deletedAt` 清空，恢复后重新出现在普通列表，可正常 `GET /4` |
| TC-64 | 恢复一个从未被删除的记录 | 对种子数据 id=1（正常状态，未删除）调用 `PATCH /1/restore` | 404，`errorCode=RECYCLE_BIN_RECORD_NOT_FOUND` |
| TC-65 | 恢复一个已永久删除的记录 | 先永久删除某条记录，再对其调用 `/restore` | 404，`errorCode=RECYCLE_BIN_RECORD_NOT_FOUND` |
| TC-66 | 恢复一个超过 30 天保留期的记录 | 构造 `deletedAt` 早于 `now - 30天` 的测试数据后调用 `/restore` | 404，`errorCode=RECYCLE_BIN_RECORD_NOT_FOUND`（需要直接操纵数据库时间字段才能构造，无法单纯通过 API 触发） |
| TC-67 | 保留期边界值：恰好 30 天 | `deletedAt = now - 30天`（`isBefore` 判断的临界点） | 需要明确"30 天整"算不算过期（代码用 `isBefore`，等于 30 天不算过期），建议专门断言这个边界 |
| TC-68 | 正常永久删除回收站内的记录 | 对已软删除且未过期的记录调用 `PATCH /{id}/permanent-delete` | 200，`permanentlyDeletedAt` 被设置 |
| TC-69 | 重复永久删除同一条记录 | 对已永久删除的记录再次调用 `/permanent-delete` | 404，`errorCode=RECYCLE_BIN_RECORD_NOT_FOUND` |
| TC-70 | 永久删除一个从未软删除的记录 | 对正常状态的记录直接调用 `/permanent-delete` | 404，`errorCode=RECYCLE_BIN_RECORD_NOT_FOUND` |
| TC-71 | 回收站列表关键字筛选 | `GET /recycle-bin?keyword=<数字ID或备注>` | 与 `listPayments` 的关键字匹配规则一致：数字关键字同时匹配 ID 与备注，非数字仅匹配备注 |
| TC-72 | 回收站列表分页默认值与非法值 | 不传 page/size；page=0 | 默认 page=1,size=10；非法 page 回退到 1（与 `listPayments` 保持一致） |

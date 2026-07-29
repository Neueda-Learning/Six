# 我的项目实现说明（Implementation Notes）

本文档汇总我在 Payments Processing System 课程项目中亲自设计并实现的功能模块，逐行说明关键代码的作用，供答辩前自查与提交材料使用。

[TOC]

---

## 一、多语言国际化（i18n）功能

### 1.1 需求背景

项目原本只支持中文界面。为提升前端工程完整性，我引入 `vue-i18n`，实现中文 / English / Deutsch 三语言切换，且**只改前端代码，不涉及后端改动**。

### 1.2 涉及文件

| 文件 | 作用 |
|---|---|
| [fronted/i18n/index.js](fronted/i18n/index.js) | 创建全局 i18n 实例，管理语言持久化、浏览器语言探测、数字/日期格式化配置 |
| [fronted/i18n/locales/zh.js](fronted/i18n/locales/zh.js) / `en.js` / `de.js` | 三份语言字典，含界面文案 + 错误码本地化描述 |
| [fronted/App.vue](fronted/App.vue) | 顶部语言切换下拉菜单 + `el-config-provider` 联动 Element Plus 内置组件语言 |
| [fronted/main.js](fronted/main.js) | 注册 i18n 到 Vue 应用 |
| [fronted/api/http.js](fronted/api/http.js) | 网络异常等前端兜底提示文案本地化 |
| `fronted/views/*.vue` | 各页面文案改用 `t()` 调用 |

### 1.3 核心代码逐行解析

**(1) 语言初始化优先级 —— `resolveInitialLocale()`**

```js
function resolveInitialLocale() {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved && SUPPORTED_LOCALES.includes(saved)) {
    return saved;
  }
  const browserLang = (navigator.language || '').slice(0, 2).toLowerCase();
  if (SUPPORTED_LOCALES.includes(browserLang)) {
    return browserLang;
  }
  return 'zh';
}
```
- 第 2 行：读取 `localStorage` 中上次保存的语言选择。
- 第 3-5 行：若存在且受支持，直接使用（尊重用户上次主动选择）。
- 第 7 行：`navigator.language` 返回如 `'de-DE'`，`slice(0, 2)` 截取语言主码 `'de'`。
- 第 8-10 行：匹配到受支持语言则使用，让首次访问的用户直接看到系统语言对应界面。
- 第 12 行：都不匹配则回退中文，保证不出现空白语言。

**(2) 数字/日期本地化格式化配置**

```js
const numberFormats = {
  zh: { currency: { style: 'currency', currency: 'CNY', currencyDisplay: 'symbol' } },
  en: { currency: { style: 'currency', currency: 'USD', currencyDisplay: 'symbol' } },
  de: { currency: { style: 'currency', currency: 'EUR', currencyDisplay: 'symbol' } }
};
```
- 每个语言配置里的 `currency` 字段只是必填占位符，真正展示时通过调用处的 `n(amount, { key: 'currency', currency: row.currency })` 动态覆盖为该笔支付**自己的币种**。
- 随界面语言变化的是千分位分隔符、小数点符号（英文 `1,234.56` vs 德文 `1.234,56`），而不是货币符号本身。

**(3) 语言切换核心函数 —— `setLocale()`**

```js
export function setLocale(locale) {
  if (!SUPPORTED_LOCALES.includes(locale)) return;
  i18n.global.locale.value = locale;       // 触发所有 t()/locale 依赖组件重新渲染
  localStorage.setItem(STORAGE_KEY, locale); // 持久化，刷新页面后仍生效
  document.documentElement.lang = locale;    // 同步 <html lang>，服务无障碍/翻译工具
}
```
`i18n.global.locale` 是 Vue 的响应式 `ref`，修改 `.value` 会自动触发所有用到 `t()` 的组件重新渲染，这是切换语言"无需刷新页面"的核心原理。

**(4) 错误码本地化字典（PaymentDetail.vue）**

```js
function errorDescription(code, fallbackMessage) {
  const key = `errors.${code}`;
  if (code && te(key)) {
    return t(key);
  }
  return fallbackMessage || t('detail.noErrorMessage');
}
```
- `te(key)` 是 vue-i18n 提供的"翻译键是否存在"检测函数。
- 若错误码（如 `INSUFFICIENT_FUNDS`）在前端字典里有对应翻译，优先展示前端本地化文案；
- 否则回退展示后端返回的 `errorMessage`（后端固定语言），保证未来新增错误码时不会出现空白。

**(5) Element Plus 内置组件联动语言 —— `el-config-provider`**

```html
<el-config-provider :locale="elLocale">
  <el-container>...</el-container>
</el-config-provider>
```
```js
const elLocaleMap = { zh: zhCn, en, de };
const elLocale = computed(() => elLocaleMap[locale.value]);
```
`el-config-provider` 利用 Vue 的 `provide/inject` 机制，把语言配置一次性注入所有子孙组件，让分页器等组件库自带文案（如"共 x 条"）也跟随切换，避免出现"自己写的字是中文，组件自带的字是英文"的割裂问题。

---

## 二、支付列表页 / 详情页自动轮询

### 2.1 需求背景

后端 `PaymentAutoTransitionScheduler` 每 5 秒自动推进一次非终态支付状态。若前端页面不主动刷新，用户会误以为"状态半天不变"。

### 2.2 详情页轮询（[PaymentDetail.vue](fronted/views/PaymentDetail.vue)）

```js
const POLLING_INTERVAL_MS = 5000;
let pollingTimer = null;

function shouldPoll(status) {
  return status === 'CREATED' || status === 'VALIDATED' || status === 'SENT';
}

function syncPolling() {
  if (!shouldPoll(payment.value && payment.value.status)) {
    stopPolling();
    return;
  }
  if (pollingTimer !== null) {
    isPolling.value = true;
    return;
  }
  pollingTimer = setInterval(() => {
    fetchDetail({ silent: true });
  }, POLLING_INTERVAL_MS);
  isPolling.value = true;
}
```
- 轮询间隔 `5000ms` 与后端调度间隔严格对齐。
- `shouldPoll()` 只对三个非终态状态返回 `true`，一旦状态到达 `COMPLETED`/`FAILED`，`syncPolling()` 会调用 `stopPolling()` 自动停止，不产生无意义的后台请求。
- `fetchDetail({ silent: true })` 静默刷新，不触发整页 `loading` 遮罩，避免闪烁。
- `onUnmounted(stopPolling)` 保证离开页面后清理定时器。

### 2.3 列表页轮询（我新增的部分，[PaymentList.vue](fronted/views/PaymentList.vue)）

```js
const PENDING_STATUSES = ['CREATED', 'VALIDATED', 'SENT'];

function shouldPoll(list) {
  return list.some((item) => PENDING_STATUSES.includes(item.status));
}
```
与详情页的区别：详情页只判断**单笔支付**的状态，列表页需要判断**当前页所有行**中是否还存在非终态记录，任意一行未到终态就继续轮询，全部到终态才停止。这是我把详情页已有模式复用到列表页时做的关键适配。

---

## 三、支付自动状态推进 + 随机失败模拟

### 3.1 需求背景

课程要求演示 `FAILED` 分支，但自动推进原逻辑（`nextAutoTransitionStatus`）每次都固定走向下一个正常状态，导致所有支付最终必然 `COMPLETED`。我在此基础上加入**可配置的随机失败概率**。

### 3.2 配置项（[application.yml](backend/src/main/resources/application.yml)）

```yaml
payments:
  auto-transition:
    initial-delay-ms: 5000
    fixed-delay-ms: 5000
    failure-probability: 0.2   # 每次推进时判定失败的概率
```

### 3.3 核心代码（[PaymentServiceImpl.java](backend/src/main/java/com/example/payments/service/impl/PaymentServiceImpl.java)）

```java
@Value("${payments.auto-transition.failure-probability:0.2}")
private double autoFailureProbability;

private static final Map<PaymentStatus, List<String>> AUTO_FAILURE_CANDIDATE_ERROR_CODES = Map.of(
        PaymentStatus.CREATED, List.of(ErrorCode.PROCESSING_ERROR.name()),
        PaymentStatus.VALIDATED, List.of(ErrorCode.PROCESSING_ERROR.name()),
        PaymentStatus.SENT, List.of(ErrorCode.NETWORK_ERROR.name()));
```
- `@Value` 注入配置项，默认 0.2（20%）。
- `AUTO_FAILURE_CANDIDATE_ERROR_CODES` 按当前所处阶段选用贴近真实场景的错误码：`SENT` 阶段失败更可能是网络问题（`NETWORK_ERROR`），其余阶段用 `PROCESSING_ERROR`。
- 注：`CREATED` 阶段的 `INSUFFICIENT_FUNDS` 已改由第四节的**真实余额校验**产生，不再纳入随机模拟范围，避免同一个错误码"有时是真的余额不足、有时是随机凑数"造成语义混乱——这是我在实现余额校验后回头做的重构。

```java
if (ThreadLocalRandom.current().nextDouble() < autoFailureProbability) {
    String errorCode = pickAutoFailureErrorCode(currentStatus);
    applyStatusTransition(payment, currentStatus, PaymentStatus.FAILED,
            errorCode, AUTO_FAILURE_ERROR_MESSAGES.get(errorCode), payment.getRemark(),
            OPERATOR_SYSTEM_AUTO);
    advancedCount++;
    continue;
}
```
每次推进前先掷一次骰子（`ThreadLocalRandom.current().nextDouble()` 生成 `[0,1)` 随机数），小于配置概率则直接转为 `FAILED` 并跳过正常推进逻辑（`continue`）。由于一笔支付要经过最多 3 次检查才能到终态，默认 20% 单次概率下，整体失败率约为 $1-(1-0.2)^3 \approx 49\%$，接近一半支付会演示失败分支。

---

## 四、账户余额充足性校验（本人独立设计实现）

### 4.1 需求背景

原 `accounts` 表没有余额字段，转账时不校验余额是否充足。需求明确：**只做余额充足性只读判断，不做真实扣款，不涉及并发扣减，账户余额自始至终不变**。

### 4.2 数据库迁移（不修改 schema.sql / data.sql，单独提供迁移脚本）

[account-balance-migration.sql](backend/src/main/resources/db/account-balance-migration.sql)：
```sql
ALTER TABLE accounts
    ADD COLUMN balance DECIMAL(18,2) NOT NULL DEFAULT 0
        COMMENT '账户可用余额，仅用于转账时的余额充足性只读校验，不参与真实扣款';

UPDATE accounts SET balance = 300.00 WHERE account_no = 'ACC10002';
-- ...其余账户余额初始化
```
`ACC10002` 特意设置为较低余额（300.00），配合种子数据里那笔 8000.00 USD 的失败案例，让"余额不足"场景开箱即可复现。

### 4.3 实体层新增字段（[Account.java](backend/src/main/java/com/example/payments/entity/Account.java)）

```java
// 账户可用余额：仅用于转账时的余额充足性只读校验（判断 fromAccount 余额是否 >= 支付金额），
// 不涉及扣款、不涉及并发扣减，余额自始至终不会被修改。
private BigDecimal balance;
```

### 4.4 校验逻辑（[PaymentValidator.java](backend/src/main/java/com/example/payments/validator/PaymentValidator.java)）

```java
public boolean hasSufficientBalance(String fromAccount, BigDecimal amount) {
    Account account = accountMapper.selectById(fromAccount);
    if (account == null || account.getBalance() == null || amount == null) {
        return false;
    }
    return account.getBalance().compareTo(amount) >= 0;
}
```
- 只读查询账户（`selectById`），**没有任何 `update`/`insert` 语句**，严格满足"余额自始至终不变"的约束。
- 账户不存在、余额字段为空、金额为空这三种异常情况统一按"余额不足"处理（返回 `false`），避免空指针异常。
- `compareTo(amount) >= 0` 即"余额 ≥ 支付金额"才算充足。

### 4.5 与状态机的集成点（[PaymentServiceImpl.java](backend/src/main/java/com/example/payments/service/impl/PaymentServiceImpl.java)）

这是本功能设计的关键：**余额校验只在 `CREATED -> VALIDATED` 这一步触发**，因为这是"验证支付是否合法"的语义节点，与课程状态机设计一致。

```java
private boolean isInsufficientForValidation(Payment payment, PaymentStatus currentStatus,
        PaymentStatus targetStatus) {
    return currentStatus == PaymentStatus.CREATED && targetStatus == PaymentStatus.VALIDATED
            && !paymentValidator.hasSufficientBalance(payment.getFromAccount(), payment.getAmount());
}
```
三个条件同时满足才拦截：① 当前状态是 `CREATED`；② 目标状态是 `VALIDATED`；③ 余额不足。这样即使调用方尝试对已经过了 `CREATED` 阶段的支付做别的流转，也不会被这段逻辑误伤。

**手动状态流转接口** `updatePaymentStatus`：
```java
if (isInsufficientForValidation(payment, currentStatus, targetStatus)) {
    applyStatusTransition(payment, currentStatus, PaymentStatus.FAILED,
            ErrorCode.INSUFFICIENT_FUNDS.name(), INSUFFICIENT_BALANCE_MESSAGE, request.getRemark(),
            OPERATOR_MANUAL);
    return toResponse(payment);
}
applyStatusTransition(payment, currentStatus, targetStatus, ...);
```
关键设计：**即使调用方请求的目标状态是 `VALIDATED`，只要余额不足，实际执行的目标状态会被强制改写为 `FAILED`**，返回 `INSUFFICIENT_FUNDS` 错误码——真实业务规则优先于调用方传入的参数。

**自动推进调度** `autoAdvancePendingPayments`：
```java
if (isInsufficientForValidation(payment, currentStatus, nextStatus)) {
    applyStatusTransition(payment, currentStatus, PaymentStatus.FAILED,
            ErrorCode.INSUFFICIENT_FUNDS.name(), INSUFFICIENT_BALANCE_MESSAGE, payment.getRemark(),
            OPERATOR_SYSTEM_AUTO);
    advancedCount++;
    continue;
}
// 之后才轮到随机失败模拟判断
if (ThreadLocalRandom.current().nextDouble() < autoFailureProbability) { ... }
```
同一段 `isInsufficientForValidation` 判断复用在手动接口和自动调度两处入口，**余额校验的优先级高于随机失败模拟**——真实业务规则不应该被"演示用的随机数"覆盖或掩盖。

### 4.6 单元测试（[PaymentValidatorTest.java](backend/src/test/java/com/example/payments/validator/PaymentValidatorTest.java)）

```java
@Test
void hasSufficientBalance_balanceGreaterOrEqualToAmount_returnsTrue() { ... }

@Test
void hasSufficientBalance_balanceLessThanAmount_returnsFalse() { ... }

@Test
void hasSufficientBalance_accountNotExist_returnsFalse() { ... }

@Test
void hasSufficientBalance_balanceFieldNull_returnsFalse() { ... }
```
覆盖了：余额充足、余额不足、账户不存在、余额字段为 `null` 四种分支，对应 `hasSufficientBalance` 方法里的全部判断路径。

---

## 五、测试用例文档与 JUnit 5 单元测试

### 5.1 test-cases.md

编写了覆盖十四个章节、共 96 个测试用例的 [test-cases.md](test-cases.md)，涵盖：创建支付 Happy Path、金额/币种/账户校验、幂等性、状态流转、查询接口、并发乐观锁、网络失败模拟、回收站、自动推进随机失败、前端多语言、前端 UI 交互。文档中还主动标注了 5 处代码审查中发现的"待确认事项"（如乐观锁冲突未被服务层检测、状态校验优先级等），体现了不臆造预期、如实记录代码现状的测试态度。

### 5.2 JUnit 5 单元测试

| 测试类 | 覆盖内容 | 用例数 |
|---|---|---|
| [PaymentValidatorTest.java](backend/src/test/java/com/example/payments/validator/PaymentValidatorTest.java) | 金额校验、账户校验、余额充足性校验 | 15 |
| [PaymentStateMachineTest.java](backend/src/test/java/com/example/payments/statemachine/PaymentStateMachineTest.java) | 全部 5×5=25 种状态流转组合 | 25 |
| [PaymentServiceImplTest.java](backend/src/test/java/com/example/payments/service/impl/PaymentServiceImplTest.java) | 状态解析、乐观锁冲突场景 | 4 |

技术要点：
- 全部使用 **Mockito** mock 数据访问层（`PaymentMapper`/`AccountMapper` 等），不连接真实 MySQL，也不受后台调度任务干扰。
- `PaymentStateMachineTest` 用一个 `@ParameterizedTest` + `@MethodSource` 遍历全部 25 种组合，而不是手写 25 个方法。
- 乐观锁测试（TC-45）**如实记录代码现状**：证明当前 `updateById` 返回值（版本冲突时为 0）完全未被服务层检查，而不是臆造一个"应该抛异常"的假设。

---

## 六、与需求的对照自查

| 需求点 | 落实情况 |
|---|---|
| 只做余额充足性校验，不扣款 | `hasSufficientBalance` 全程只读，无任何 UPDATE 语句 |
| 不涉及并发扣减 | 未引入任何锁机制，因为余额从不被修改，天然无并发扣减问题 |
| 账户余额自始至终不变 | 唯一修改余额的地方是迁移脚本的初始化 UPDATE，业务代码路径中无写操作 |
| 不改动 data.sql / schema.sql | 余额字段与初始数据独立放在 `account-balance-migration.sql`，需手动执行 |
| 余额不足在状态转换时转为 FAILED | 手动流转与自动调度两处入口均已接入 `isInsufficientForValidation` |
| 返回 INSUFFICIENT_FUNDS 错误码 | `ErrorCode.INSUFFICIENT_FUNDS` 已复用既有枚举值，未新增错误码 |

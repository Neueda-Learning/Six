-- 该文件用于给 accounts 表补充“账户余额”字段及余额初始化数据，供转账时的余额充足性校验使用。
-- 按需求不修改 schema.sql / data.sql，需在已执行过 schema.sql + data.sql 的数据库上单独手动执行本脚本：
--   mysql -u root -p payments_db < backend/src/main/resources/db/account-balance-migration.sql
--
-- 说明：
-- 1. balance 仅用于“余额是否充足”的只读判断，不涉及扣款/回滚等资金变动逻辑，账户余额在整个支付生命周期内保持不变。
-- 2. 若字段已存在（重复执行本脚本），ALTER TABLE 会报错 "Duplicate column name"，属预期行为，避免重复执行。

USE payments_db;

ALTER TABLE accounts
    ADD COLUMN balance DECIMAL(18,2) NOT NULL DEFAULT 0
        COMMENT '账户可用余额，仅用于转账时的余额充足性只读校验，不参与真实扣款';

-- 初始化各账户余额：
-- ACC10002 特意设置为较低余额（300.00），用于配合 data.sql 中 id=5 那笔 8000.00 USD 的
-- INSUFFICIENT_FUNDS 失败种子数据的业务背景；其余账户余额相对充裕，便于日常测试正常通过余额校验。
UPDATE accounts SET balance = 5000.00  WHERE account_no = 'ACC10001';
UPDATE accounts SET balance = 300.00   WHERE account_no = 'ACC10002';
UPDATE accounts SET balance = 10000.00 WHERE account_no = 'ACC10003';
UPDATE accounts SET balance = 2000.00  WHERE account_no = 'ACC20001';
UPDATE accounts SET balance = 800.00   WHERE account_no = 'ACC20002';
UPDATE accounts SET balance = 15000.00 WHERE account_no = 'ACC20003';

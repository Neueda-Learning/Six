-- 该文件用于给已初始化的旧版 accounts 表补充“账户余额”字段，供转账时的余额充足性校验使用。
-- 新环境直接执行 schema.sql + data.sql 即可；仅旧库需要单独执行本脚本：
--   mysql -u root -p payments_db < backend/src/main/resources/db/account-balance-migration.sql
--
-- 说明：
-- 1. balance 仅用于“余额是否充足”的只读判断，不涉及扣款/回滚等资金变动逻辑，账户余额在整个支付生命周期内保持不变。
-- 2. 本脚本应支持重复执行，不覆盖已有非零余额。

USE payments_db;

SET @add_balance_column = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'payments_db'
              AND TABLE_NAME = 'accounts'
              AND COLUMN_NAME = 'balance'
        ),
        'SELECT 1',
        "ALTER TABLE accounts ADD COLUMN balance DECIMAL(18,2) NOT NULL DEFAULT 100000.00 COMMENT '账户可用余额，仅用于转账时的余额充足性只读校验，不参与真实扣款'"
    )
);

PREPARE add_balance_column_stmt FROM @add_balance_column;
EXECUTE add_balance_column_stmt;
DEALLOCATE PREPARE add_balance_column_stmt;

-- 仅为旧数据回填默认余额；若库中已经配置过真实测试值，则保持不变。
UPDATE accounts SET balance = 100000.00 WHERE balance = 0;

-- 该文件用于定义支付课程项目的数据库与表结构（建库建表脚本），供本地/开发环境初始化数据库时执行。

-- 创建数据库（与 application.yml 中的 payments_db 保持一致），使用 utf8mb4 支持中文备注与表情符号
CREATE DATABASE IF NOT EXISTS payments_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE payments_db;

-- 账户表：用于模拟账户是否真实存在的校验数据源（系统初始化数据），非真实支付网关账户体系
CREATE TABLE IF NOT EXISTS accounts (
    account_no VARCHAR(50) PRIMARY KEY,                        -- 账户号码，主键，供支付的源账户/目标账户做存在性校验
    owner_name VARCHAR(100) NOT NULL,                          -- 账户持有人姓名，仅用于模拟数据展示
    currency VARCHAR(3) NOT NULL,                              -- 账户默认币种（ISO 4217三位代码，如USD/EUR/GBP）
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',              -- 账户状态，如ACTIVE表示可用，仅用于模拟校验，不参与真实风控
    balance DECIMAL(18,2) NOT NULL DEFAULT 100000.00,          -- 账户可用余额：校验阶段用于判断，支付 COMPLETED 时会真实扣减/入账
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP     -- 账户记录创建时间
);

-- 支付主表：记录支付从创建到完成/失败的当前状态与核心字段
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,                      -- 支付主键ID，自增
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,               -- 幂等键，客户端生成的唯一标识，用于防止重复提交同一笔支付
    from_account VARCHAR(50) NOT NULL,                         -- 源账户（付款账户），必须在accounts表中真实存在，且不能与to_account相同
    to_account VARCHAR(50) NOT NULL,                           -- 目标账户（收款账户），必须在accounts表中真实存在，且不能与from_account相同
    amount DECIMAL(18,2) NOT NULL,                             -- 支付金额，须大于0且不超过1,000,000，最多两位小数
    currency VARCHAR(3) NOT NULL,                              -- 币种（ISO 4217三位代码），须在受支持白名单内（如USD/EUR/GBP）
    status VARCHAR(20) NOT NULL,                               -- 支付当前状态：CREATED/VALIDATED/SENT/COMPLETED/FAILED
    error_code VARCHAR(50) NULL,                               -- 失败错误码（仅status=FAILED时有值），如INSUFFICIENT_FUNDS、NETWORK_ERROR等
    error_message VARCHAR(255) NULL,                           -- 失败详细描述信息（仅status=FAILED时有值），便于前端展示与排查
    remark VARCHAR(255) NULL,                                  -- 备注/引用信息，如发票号或用户填写的说明，可为空
    version INT NOT NULL DEFAULT 0,                            -- 乐观锁版本号，每次状态更新自增，防止并发更新覆盖
    created_at DATETIME NOT NULL,                              -- 支付创建时间
    updated_at DATETIME NOT NULL,                              -- 支付最近一次更新时间
    deleted_at DATETIME NULL,                                  -- 软删除时间：非空表示该记录已进入回收站，前端默认列表不展示
    permanently_deleted_at DATETIME NULL,                      -- 永久隐藏时间：非空表示记录已从用户界面彻底隐藏，不可恢复，但数据库仍保留
    INDEX idx_payments_status (status),                        -- 按状态筛选支付列表时使用的索引
    INDEX idx_payments_deleted_at (deleted_at),                -- 回收站最近删除列表查询时使用的索引
    INDEX idx_payments_permanently_deleted_at (permanently_deleted_at) -- 永久隐藏过滤时使用的索引
);

-- 支付状态历史表（audit trail）：记录每一次状态流转，作为支付时间线与失败原因的唯一数据来源
CREATE TABLE IF NOT EXISTS payment_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,                      -- 状态历史记录主键ID，自增
    payment_id BIGINT NOT NULL,                                -- 关联的支付ID，对应payments表主键
    from_status VARCHAR(20) NULL,                              -- 变更前状态，支付首次创建时该字段为空
    to_status VARCHAR(20) NOT NULL,                            -- 变更后状态：CREATED/VALIDATED/SENT/COMPLETED/FAILED
    error_code VARCHAR(50) NULL,                               -- 本次变更若为失败流转，记录对应错误码
    error_message VARCHAR(255) NULL,                           -- 本次变更若为失败流转，记录详细错误描述
    remark VARCHAR(255) NULL,                                  -- 本次状态变更的补充备注，可为空
    operator VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',            -- 触发本次状态变更的操作来源，如SYSTEM（系统自动流转）或MANUAL（手工接口触发）
    created_at DATETIME NOT NULL,                              -- 本次状态变更发生的时间，用于组成支付时间线
    INDEX idx_history_payment_id (payment_id),                 -- 按支付ID查询历史时间线时使用的索引
    CONSTRAINT fk_history_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id)  -- 外键约束，确保历史记录必须归属于一笔已存在的支付
);

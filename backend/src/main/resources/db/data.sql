-- 该文件用于插入支付课程项目的模拟初始化数据（种子数据），依赖 schema.sql 已创建的表结构。

USE payments_db;

-- 账户模拟数据：供账户存在性校验使用（来源账户/目标账户必须在此列表中）
INSERT INTO accounts (account_no, owner_name, currency, status, created_at) VALUES
('ACC10001', 'Alice Zhang', 'USD', 'ACTIVE', '2026-01-01 09:00:00'),
('ACC10002', 'Bob Chen', 'EUR', 'ACTIVE', '2026-01-01 09:00:00'),
('ACC10003', 'Cindy Wang', 'GBP', 'ACTIVE', '2026-01-01 09:00:00'),
('ACC20001', 'David Li', 'USD', 'ACTIVE', '2026-01-01 09:00:00'),
('ACC20002', 'Eva Liu', 'EUR', 'ACTIVE', '2026-01-01 09:00:00'),
('ACC20003', 'Frank Zhou', 'GBP', 'ACTIVE', '2026-01-01 09:00:00');

-- 支付模拟数据：覆盖 COMPLETED / SENT / VALIDATED / CREATED / FAILED（校验失败与网络失败）五种典型状态
INSERT INTO payments
(id, idempotency_key, from_account, to_account, amount, currency, status, error_code, error_message, remark, version, created_at, updated_at)
VALUES
(1, 'seed-happy-completed-001', 'ACC10001', 'ACC20002', 1200.50, 'USD', 'COMPLETED', NULL, NULL, 'invoice-2026-07-seed-01', 3, '2026-07-20 09:00:00', '2026-07-20 09:03:00'),
(2, 'seed-in-flight-sent-002', 'ACC10002', 'ACC20003', 500.00, 'EUR', 'SENT', NULL, NULL, 'invoice-2026-07-seed-02', 2, '2026-07-21 10:00:00', '2026-07-21 10:02:00'),
(3, 'seed-in-flight-validated-003', 'ACC10003', 'ACC20001', 99.99, 'GBP', 'VALIDATED', NULL, NULL, 'invoice-2026-07-seed-03', 1, '2026-07-22 11:00:00', '2026-07-22 11:01:00'),
(4, 'seed-in-flight-created-004', 'ACC10001', 'ACC20003', 250.00, 'USD', 'CREATED', NULL, NULL, 'invoice-2026-07-seed-04', 0, '2026-07-23 12:00:00', '2026-07-23 12:00:00'),
(5, 'seed-failed-validation-005', 'ACC10002', 'ACC20002', 8000.00, 'USD', 'FAILED', 'INSUFFICIENT_FUNDS', 'mock insufficient balance in from_account', 'invoice-2026-07-seed-05', 1, '2026-07-24 13:00:00', '2026-07-24 13:01:00'),
(6, 'seed-failed-network-006', 'ACC10003', 'ACC20001', 750.00, 'EUR', 'FAILED', 'NETWORK_ERROR', 'mock network timeout after max retries', 'invoice-2026-07-seed-06', 3, '2026-07-25 14:00:00', '2026-07-25 14:03:00');

-- 支付状态历史模拟数据（audit trail）：与上方 payments 的每条记录状态流转一一对应
INSERT INTO payment_status_history
(id, payment_id, from_status, to_status, error_code, error_message, remark, operator, created_at)
VALUES
-- payment 1：CREATED -> VALIDATED -> SENT -> COMPLETED
(1, 1, NULL, 'CREATED', NULL, NULL, NULL, 'SYSTEM', '2026-07-20 09:00:00'),
(2, 1, 'CREATED', 'VALIDATED', NULL, NULL, NULL, 'SYSTEM', '2026-07-20 09:01:00'),
(3, 1, 'VALIDATED', 'SENT', NULL, NULL, NULL, 'SYSTEM', '2026-07-20 09:02:00'),
(4, 1, 'SENT', 'COMPLETED', NULL, NULL, NULL, 'SYSTEM', '2026-07-20 09:03:00'),

-- payment 2：CREATED -> VALIDATED -> SENT
(5, 2, NULL, 'CREATED', NULL, NULL, NULL, 'SYSTEM', '2026-07-21 10:00:00'),
(6, 2, 'CREATED', 'VALIDATED', NULL, NULL, NULL, 'SYSTEM', '2026-07-21 10:01:00'),
(7, 2, 'VALIDATED', 'SENT', NULL, NULL, NULL, 'SYSTEM', '2026-07-21 10:02:00'),

-- payment 3：CREATED -> VALIDATED
(8, 3, NULL, 'CREATED', NULL, NULL, NULL, 'SYSTEM', '2026-07-22 11:00:00'),
(9, 3, 'CREATED', 'VALIDATED', NULL, NULL, NULL, 'SYSTEM', '2026-07-22 11:01:00'),

-- payment 4：仅 CREATED
(10, 4, NULL, 'CREATED', NULL, NULL, NULL, 'SYSTEM', '2026-07-23 12:00:00'),

-- payment 5：CREATED -> FAILED（校验阶段余额不足）
(11, 5, NULL, 'CREATED', NULL, NULL, NULL, 'SYSTEM', '2026-07-24 13:00:00'),
(12, 5, 'CREATED', 'FAILED', 'INSUFFICIENT_FUNDS', 'mock insufficient balance in from_account', NULL, 'SYSTEM', '2026-07-24 13:01:00'),

-- payment 6：CREATED -> VALIDATED -> SENT -> FAILED（发送后网络重试耗尽）
(13, 6, NULL, 'CREATED', NULL, NULL, NULL, 'SYSTEM', '2026-07-25 14:00:00'),
(14, 6, 'CREATED', 'VALIDATED', NULL, NULL, NULL, 'SYSTEM', '2026-07-25 14:01:00'),
(15, 6, 'VALIDATED', 'SENT', NULL, NULL, NULL, 'SYSTEM', '2026-07-25 14:02:00'),
(16, 6, 'SENT', 'FAILED', 'NETWORK_ERROR', 'mock network timeout after max retries', NULL, 'SYSTEM', '2026-07-25 14:03:00');

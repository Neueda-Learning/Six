-- 该文件用于定义数据库结构或初始化数据，后续需完成建表细节与种子数据。
-- //todo initialize payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    from_account VARCHAR(50) NOT NULL,
    to_account VARCHAR(50) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50) NULL,
    error_message VARCHAR(255) NULL,
    remark VARCHAR(255) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_payments_status (status)
);

-- //todo initialize payment_status_history table
CREATE TABLE IF NOT EXISTS payment_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50) NULL,
    error_message VARCHAR(255) NULL,
    remark VARCHAR(255) NULL,
    operator VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    created_at DATETIME NOT NULL,
    INDEX idx_history_payment_id (payment_id),
    CONSTRAINT fk_history_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id)
);

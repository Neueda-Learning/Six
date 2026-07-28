package com.example.payments.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 在应用启动时补齐当前版本所需的最小数据库结构。
 * 当前项目未接入 Flyway/Liquibase，因此对本地已有数据库采用轻量级自修复，
 * 以保证新增字段上线后无需人工手动执行 ALTER TABLE。
 */
@Component
public class PaymentSchemaMigrationRunner implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  public PaymentSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    ensureDeletedAtColumn();
    ensureDeletedAtIndex();
  }

  private void ensureDeletedAtColumn() {
    Integer count = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'payments'
              AND COLUMN_NAME = 'deleted_at'
            """,
        Integer.class);

    if (count != null && count == 0) {
      jdbcTemplate.execute("ALTER TABLE payments ADD COLUMN deleted_at DATETIME NULL");
    }
  }

  private void ensureDeletedAtIndex() {
    Integer count = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*)
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'payments'
              AND INDEX_NAME = 'idx_payments_deleted_at'
            """,
        Integer.class);

    if (count != null && count == 0) {
      jdbcTemplate.execute("CREATE INDEX idx_payments_deleted_at ON payments(deleted_at)");
    }
  }
}
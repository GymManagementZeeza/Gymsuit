package com.zeezaglobal.gymmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-time column fixes that {@code ddl-auto=update} can't make: it adds columns but never changes an
 * existing column's type or nullability. Each fix checks the live schema first, so it runs once and is
 * a no-op afterwards.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaFixups implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // users.role was created as MySQL ENUM(...); saving a role added later (NUTRITIONIST) failed with
        // "Data truncated". VARCHAR stores any current or future Role value.
        fix("users", "role",
                (type, nullable) -> "enum".equalsIgnoreCase(type),
                "ALTER TABLE users MODIFY COLUMN role VARCHAR(32) NOT NULL",
                "Converted users.role from ENUM to VARCHAR(32) so new roles can be stored");

        // challenges.created_by was created NOT NULL, but deleting your own account keeps the challenges you
        // created for the other participants and clears the creator (see Challenge.createdBy). Without this,
        // anyone who ever created a challenge could not delete their account.
        fix("challenges", "created_by",
                (type, nullable) -> !nullable,
                "ALTER TABLE challenges MODIFY COLUMN created_by BIGINT NULL",
                "Made challenges.created_by nullable so account deletion can keep a user's challenges");
    }

    private interface Needs {
        boolean test(String dataType, boolean nullable);
    }

    private void fix(String table, String column, Needs needs, String sql, String done) {
        try {
            List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT DATA_TYPE, IS_NULLABLE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    table, column);
            if (rows.isEmpty()) {
                return;
            }
            String type = String.valueOf(rows.get(0).get("DATA_TYPE"));
            boolean nullable = "YES".equalsIgnoreCase(String.valueOf(rows.get(0).get("IS_NULLABLE")));
            if (needs.test(type, nullable)) {
                jdbcTemplate.execute(sql);
                log.info(done);
            }
        } catch (Exception e) {
            log.warn("Could not check/fix {}.{}: {}", table, column, e.getMessage());
        }
    }
}

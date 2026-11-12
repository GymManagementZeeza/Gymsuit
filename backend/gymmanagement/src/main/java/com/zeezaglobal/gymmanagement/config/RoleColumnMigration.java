package com.zeezaglobal.gymmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hibernate creates {@code users.role} as a MySQL {@code ENUM(...)} column, and
 * {@code ddl-auto=update} never widens an existing column. Without this, saving a
 * user with a role added later (e.g. NUTRITIONIST) fails with "Data truncated".
 * Converting the column to VARCHAR once makes any future Role value storable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleColumnMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> types = jdbcTemplate.queryForList(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'role'",
                    String.class);
            if (!types.isEmpty() && "enum".equalsIgnoreCase(types.get(0))) {
                jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(32) NOT NULL");
                log.info("Converted users.role from ENUM to VARCHAR(32) so new roles can be stored");
            }
        } catch (Exception e) {
            log.warn("Could not check/convert users.role column type: {}", e.getMessage());
        }
    }
}

package com.zeezaglobal.gymmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports the ENTIRE application database as CSV files — one CSV per table —
 * bundled into a single ZIP archive. Built for manual off-site backups: call
 * {@code GET /api/admin/backup/export} (see BackupController), save the ZIP,
 * and store it wherever you like (Google Drive, external disk, ...).
 *
 * <p>The export is fully generic JDBC: the table list is read from the live
 * database metadata at export time, so tables added later are included
 * automatically with no code changes. Only tables in the application's own
 * database (the JDBC connection's catalog) are exported.
 *
 * <p>Value formatting inside each CSV:
 * <ul>
 *   <li>NULL is written as an empty field.</li>
 *   <li>Timestamps/dates are written in ISO-8601 form (e.g. 2026-10-22T14:03:11).</li>
 *   <li>Binary columns (BLOB/BINARY/VARBINARY) are Base64-encoded so no data
 *       is silently dropped.</li>
 *   <li>Fields containing commas, quotes or newlines are quoted per RFC 4180
 *       (embedded quotes are doubled).</li>
 * </ul>
 *
 * <p>Uses only the JDK + the existing Spring stack — no new dependencies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseExportService {

    private final DataSource dataSource;

    /**
     * Builds the ZIP archive (one {@code <table>.csv} per table, plus a small
     * {@code _export-info.txt} manifest) and returns it as bytes.
     *
     * @throws IllegalStateException if the export fails (surfaced as HTTP 500
     *                               by the global exception handler)
     */
    public byte[] exportDatabaseZip() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Connection connection = dataSource.getConnection();
             ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {

            List<String> tables = listTables(connection);
            log.info("Database export started: {} tables in catalog '{}'",
                    tables.size(), connection.getCatalog());

            for (String table : tables) {
                int rows = writeTableCsv(connection, table, zip);
                log.info("Database export: table '{}' -> {} rows", table, rows);
            }

            zip.putNextEntry(new ZipEntry("_export-info.txt"));
            String info = "GymSuit database export\r\n"
                    + "Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\r\n"
                    + "Database: " + connection.getCatalog() + "\r\n"
                    + "Tables: " + tables.size() + " (" + String.join(", ", tables) + ")\r\n"
                    + "Format: one CSV per table; header row + all rows; NULL = empty field; "
                    + "timestamps ISO-8601; binary columns Base64-encoded.\r\n";
            zip.write(info.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.finish();
            log.info("Database export complete: {} tables, {} bytes", tables.size(), bytes.size());
            return bytes.toByteArray();
        } catch (Exception e) {
            log.error("Database export failed", e);
            throw new IllegalStateException("Database export failed: " + e.getMessage(), e);
        }
    }

    /** All base tables in the application's own database, sorted by name. */
    private List<String> listTables(Connection connection) throws Exception {
        DatabaseMetaData meta = connection.getMetaData();
        List<String> tables = new ArrayList<>();
        // Restrict to the connection's catalog so we only ever export the app
        // schema, never system schemas (information_schema, mysql, ...).
        try (ResultSet rs = meta.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        Collections.sort(tables);
        return tables;
    }

    /** Writes one table's CSV into the ZIP and returns its data-row count. */
    private int writeTableCsv(Connection connection, String table, ZipOutputStream zip) throws Exception {
        zip.putNextEntry(new ZipEntry(table + ".csv"));
        Writer writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
        int rows = 0;
        // Table names come from the database's own metadata (never user input);
        // backtick-quote them for MySQL and double any embedded backtick.
        String sql = "SELECT * FROM `" + table.replace("`", "``") + "`";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();

            List<String> header = new ArrayList<>(columns);
            for (int i = 1; i <= columns; i++) {
                header.add(meta.getColumnLabel(i));
            }
            writeRow(writer, header);

            while (rs.next()) {
                List<String> row = new ArrayList<>(columns);
                for (int i = 1; i <= columns; i++) {
                    row.add(formatValue(rs.getObject(i)));
                }
                writeRow(writer, row);
                rows++;
            }
        }
        writer.flush();
        zip.closeEntry();
        return rows;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof byte[] binary) {
            return Base64.getEncoder().encodeToString(binary);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (value instanceof java.sql.Time time) {
            return time.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
        }
        return String.valueOf(value);
    }

    private void writeRow(Writer writer, List<String> fields) throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escapeCsv(fields.get(i)));
        }
        writer.write("\r\n");
    }

    /** RFC 4180 quoting: quote when the field contains a comma, quote or newline. */
    private String escapeCsv(String field) {
        if (field == null || field.isEmpty()) {
            return "";
        }
        boolean needsQuoting = field.indexOf(',') >= 0 || field.indexOf('"') >= 0
                || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0;
        if (!needsQuoting) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }
}

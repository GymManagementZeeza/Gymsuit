package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.service.DatabaseExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manual database backup download.
 *
 * <p>{@code GET /api/admin/backup/export} returns a ZIP containing one CSV
 * per database table (plus a small manifest). It is a manual, on-demand
 * export: call it with an ADMIN login token, save the file, and store it
 * wherever you keep backups (e.g. upload it to Google Drive yourself).
 *
 * <pre>
 * curl -H "Authorization: Bearer &lt;ADMIN_TOKEN&gt;" \
 *      https://api.gymsuit.app/api/admin/backup/export \
 *      -o gymsuit-db-export.zip
 * </pre>
 *
 * <p>Security: the path is NOT in SecurityConfig's permitAll list, so a valid
 * JWT is required, and {@code @PreAuthorize} further restricts it to the
 * ADMIN role (authorities are {@code ROLE_<name>} — see UserPrincipal).
 */
@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
public class BackupController {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    private final DatabaseExportService databaseExportService;

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportDatabase() {
        byte[] zip = databaseExportService.exportDatabaseZip();
        String filename = "gymsuit-db-export-"
                + LocalDateTime.now().format(FILE_TIMESTAMP) + ".zip";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(zip.length);
        return ResponseEntity.ok().headers(headers).body(zip);
    }
}

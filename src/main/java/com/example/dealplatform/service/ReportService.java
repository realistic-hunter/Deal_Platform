package com.example.dealplatform.service;

import com.example.dealplatform.model.Report;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ReportService {
    private final JdbcTemplate jdbcTemplate;

    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(Long reporterId, String targetType, Long targetId, String reason, String description) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Report reason is required");
        }
        jdbcTemplate.update("""
                INSERT INTO report(reporter_id, target_type, target_id, reason, description, status)
                VALUES(?, ?, ?, ?, ?, 0)
                """, reporterId, targetType, targetId, reason.trim(), description);
    }

    public void handle(Long id, Long handlerId, Integer status, String result) {
        jdbcTemplate.update("""
                UPDATE report
                SET status = ?, handle_result = ?, handler_id = ?, handle_time = NOW()
                WHERE id = ?
                """, status == null ? 1 : status, result, handlerId, id);
    }

    public List<Report> list() {
        return jdbcTemplate.query("""
                SELECT r.*, u.username AS reporter_name
                FROM report r
                JOIN user u ON r.reporter_id = u.id
                ORDER BY r.create_time DESC
                """, (rs, rowNum) -> {
            Report report = new Report();
            report.setId(rs.getLong("id"));
            report.setReporterName(rs.getString("reporter_name"));
            report.setTargetType(rs.getString("target_type"));
            report.setTargetId(rs.getLong("target_id"));
            report.setReason(rs.getString("reason"));
            report.setDescription(rs.getString("description"));
            report.setStatus(rs.getInt("status"));
            report.setHandleResult(rs.getString("handle_result"));
            report.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            return report;
        });
    }
}

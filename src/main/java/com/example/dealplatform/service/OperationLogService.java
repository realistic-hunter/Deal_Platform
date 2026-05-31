package com.example.dealplatform.service;

import com.example.dealplatform.model.OperationLog;
import com.example.dealplatform.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogService {
    private final JdbcTemplate jdbcTemplate;

    public OperationLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(User user, String type, String content, HttpServletRequest request) {
        jdbcTemplate.update("""
                INSERT INTO operation_log(user_id, username, operation_type, operation_content, ip_address)
                VALUES(?, ?, ?, ?, ?)
                """, user == null ? null : user.getId(), user == null ? null : user.getUsername(),
                type, content, request == null ? null : request.getRemoteAddr());
    }

    public List<OperationLog> list() {
        return jdbcTemplate.query("""
                SELECT id, username, operation_type, operation_content, ip_address, create_time
                FROM operation_log
                ORDER BY create_time DESC
                LIMIT 200
                """, (rs, rowNum) -> {
            OperationLog log = new OperationLog();
            log.setId(rs.getLong("id"));
            log.setUsername(rs.getString("username"));
            log.setOperationType(rs.getString("operation_type"));
            log.setOperationContent(rs.getString("operation_content"));
            log.setIpAddress(rs.getString("ip_address"));
            log.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            return log;
        });
    }
}

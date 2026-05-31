package com.example.dealplatform.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExportService {
    private final JdbcTemplate jdbcTemplate;

    public ExportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String goodsCsv() {
        StringBuilder csv = new StringBuilder("\uFEFFID,Title,Category,Seller,Price,Status,PublishTime\n");
        jdbcTemplate.query("""
                SELECT g.id, g.title, c.category_name, u.username, g.price, g.status, g.publish_time
                FROM goods g
                JOIN category c ON g.category_id = c.id
                JOIN user u ON g.seller_id = u.id
                WHERE g.is_deleted = 0
                ORDER BY g.publish_time DESC
                """, rs -> csv.append(rs.getLong("id")).append(',')
                .append(escape(rs.getString("title"))).append(',')
                .append(escape(rs.getString("category_name"))).append(',')
                .append(escape(rs.getString("username"))).append(',')
                .append(rs.getBigDecimal("price")).append(',')
                .append(rs.getInt("status")).append(',')
                .append(rs.getTimestamp("publish_time")).append('\n'));
        return csv.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

package com.example.dealplatform.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StatisticsService {
    private final JdbcTemplate jdbcTemplate;

    public StatisticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Integer> cards() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("用户总数", count("SELECT COUNT(*) FROM user WHERE is_deleted = 0"));
        data.put("商品总数", count("SELECT COUNT(*) FROM goods WHERE is_deleted = 0"));
        data.put("订单总数", count("SELECT COUNT(*) FROM orders"));
        data.put("待审核商品", count("SELECT COUNT(*) FROM goods WHERE status = 0 AND is_deleted = 0"));
        data.put("今日新增商品", count("SELECT COUNT(*) FROM goods WHERE DATE(create_time) = CURDATE()"));
        data.put("今日新增订单", count("SELECT COUNT(*) FROM orders WHERE DATE(create_time) = CURDATE()"));
        return data;
    }

    public Map<String, Integer> goodsByCategory() {
        Map<String, Integer> data = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT c.category_name, COUNT(g.id) AS total
                FROM category c
                LEFT JOIN goods g ON c.id = g.category_id AND g.is_deleted = 0
                GROUP BY c.id, c.category_name
                ORDER BY total DESC
                """, rs -> data.put(rs.getString("category_name"), rs.getInt("total")));
        return data;
    }

    public Map<String, Integer> ordersByStatus() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("待确认", count("SELECT COUNT(*) FROM orders WHERE status = 0"));
        data.put("交易中", count("SELECT COUNT(*) FROM orders WHERE status = 1"));
        data.put("已完成", count("SELECT COUNT(*) FROM orders WHERE status = 2"));
        data.put("已取消", count("SELECT COUNT(*) FROM orders WHERE status = 3"));
        return data;
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}

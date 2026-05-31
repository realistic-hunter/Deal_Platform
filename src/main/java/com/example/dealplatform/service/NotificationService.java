package com.example.dealplatform.service;

import com.example.dealplatform.model.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    private final JdbcTemplate jdbcTemplate;

    public NotificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void send(Long receiverId, String title, String content, String type, Long relatedId) {
        jdbcTemplate.update("""
                INSERT INTO notification(receiver_id, title, content, type, related_id)
                VALUES(?, ?, ?, ?, ?)
                """, receiverId, title, content, type, relatedId);
    }

    public List<Notification> list(Long receiverId) {
        return jdbcTemplate.query("""
                SELECT id, title, content, type, is_read, create_time
                FROM notification
                WHERE receiver_id = ?
                ORDER BY create_time DESC
                """, (rs, rowNum) -> {
            Notification notification = new Notification();
            notification.setId(rs.getLong("id"));
            notification.setTitle(rs.getString("title"));
            notification.setContent(rs.getString("content"));
            notification.setType(rs.getString("type"));
            notification.setIsRead(rs.getInt("is_read"));
            notification.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            return notification;
        }, receiverId);
    }

    public int unreadCount(Long receiverId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification WHERE receiver_id = ? AND is_read = 0",
                Integer.class, receiverId);
        return count == null ? 0 : count;
    }

    public void markRead(Long id, Long receiverId) {
        jdbcTemplate.update("UPDATE notification SET is_read = 1 WHERE id = ? AND receiver_id = ?", id, receiverId);
    }
}

package com.example.dealplatform.service;

import com.example.dealplatform.model.Notice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class NoticeService {
    private final JdbcTemplate jdbcTemplate;

    public NoticeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Notice> publicNotices() {
        return list("WHERE status = 1");
    }

    public List<Notice> all() {
        return list("");
    }

    public void save(Long publisherId, String title, String content, Integer status) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Title and content are required");
        }
        jdbcTemplate.update("INSERT INTO notice(title, content, publisher_id, status) VALUES(?, ?, ?, ?)",
                title.trim(), content.trim(), publisherId, status == null ? 1 : status);
    }

    public void update(Long id, String title, String content, Integer status) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Title and content are required");
        }
        jdbcTemplate.update("UPDATE notice SET title = ?, content = ?, status = ? WHERE id = ?",
                title.trim(), content.trim(), status == null ? 1 : status, id);
    }

    private List<Notice> list(String where) {
        return jdbcTemplate.query("SELECT id, title, content, status, create_time FROM notice " + where + " ORDER BY create_time DESC",
                (rs, rowNum) -> {
                    Notice notice = new Notice();
                    notice.setId(rs.getLong("id"));
                    notice.setTitle(rs.getString("title"));
                    notice.setContent(rs.getString("content"));
                    notice.setStatus(rs.getInt("status"));
                    notice.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    return notice;
                });
    }
}

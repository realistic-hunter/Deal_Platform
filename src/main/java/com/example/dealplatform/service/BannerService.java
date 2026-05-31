package com.example.dealplatform.service;

import com.example.dealplatform.model.Banner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class BannerService {
    private final JdbcTemplate jdbcTemplate;

    public BannerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Banner> publicBanners() {
        return list("WHERE status = 1");
    }

    public List<Banner> all() {
        return list("");
    }

    public void save(String title, String imageUrl, String linkUrl, Integer sortOrder, Integer status) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(imageUrl)) {
            throw new IllegalArgumentException("Title and image are required");
        }
        jdbcTemplate.update("""
                INSERT INTO banner(title, image_url, link_url, sort_order, status)
                VALUES(?, ?, ?, ?, ?)
                """, title.trim(), imageUrl.trim(), linkUrl, sortOrder == null ? 0 : sortOrder, status == null ? 1 : status);
    }

    public void update(Long id, String title, String imageUrl, String linkUrl, Integer sortOrder, Integer status) {
        jdbcTemplate.update("""
                UPDATE banner SET title = ?, image_url = ?, link_url = ?, sort_order = ?, status = ?
                WHERE id = ?
                """, title, imageUrl, linkUrl, sortOrder == null ? 0 : sortOrder, status == null ? 1 : status, id);
    }

    private List<Banner> list(String where) {
        return jdbcTemplate.query("SELECT id, title, image_url, link_url, sort_order, status FROM banner " + where + " ORDER BY sort_order, id",
                (rs, rowNum) -> {
                    Banner banner = new Banner();
                    banner.setId(rs.getLong("id"));
                    banner.setTitle(rs.getString("title"));
                    banner.setImageUrl(rs.getString("image_url"));
                    banner.setLinkUrl(rs.getString("link_url"));
                    banner.setSortOrder(rs.getInt("sort_order"));
                    banner.setStatus(rs.getInt("status"));
                    return banner;
                });
    }
}

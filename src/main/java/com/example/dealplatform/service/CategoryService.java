package com.example.dealplatform.service;

import com.example.dealplatform.model.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CategoryService {
    private final JdbcTemplate jdbcTemplate;

    public CategoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Category> findAllEnabled() {
        return jdbcTemplate.query("SELECT id, category_name, sort_order, status FROM category WHERE status = 1 ORDER BY sort_order, id",
                (rs, rowNum) -> mapCategory(rs));
    }

    public List<Category> findAll() {
        return jdbcTemplate.query("SELECT id, category_name, sort_order, status FROM category ORDER BY sort_order, id",
                (rs, rowNum) -> mapCategory(rs));
    }

    public void save(String categoryName, Integer sortOrder) {
        if (!StringUtils.hasText(categoryName)) {
            throw new IllegalArgumentException("Category name is required");
        }
        jdbcTemplate.update("INSERT INTO category(category_name, sort_order, status) VALUES(?, ?, 1)",
                categoryName.trim(), sortOrder == null ? 0 : sortOrder);
    }

    public void update(Long id, String categoryName, Integer sortOrder, Integer status) {
        if (!StringUtils.hasText(categoryName)) {
            throw new IllegalArgumentException("Category name is required");
        }
        jdbcTemplate.update("UPDATE category SET category_name = ?, sort_order = ?, status = ? WHERE id = ?",
                categoryName.trim(), sortOrder == null ? 0 : sortOrder, status == null ? 1 : status, id);
    }

    private Category mapCategory(java.sql.ResultSet rs) throws java.sql.SQLException {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setCategoryName(rs.getString("category_name"));
        category.setSortOrder(rs.getInt("sort_order"));
        category.setStatus(rs.getInt("status"));
        return category;
    }
}

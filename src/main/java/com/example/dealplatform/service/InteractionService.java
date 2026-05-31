package com.example.dealplatform.service;

import com.example.dealplatform.model.Goods;
import com.example.dealplatform.model.GoodsMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class InteractionService {
    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public InteractionService(JdbcTemplate jdbcTemplate, NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    @Transactional
    public void favorite(Long userId, Long goodsId) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM favorite WHERE user_id = ? AND goods_id = ?",
                Integer.class, userId, goodsId);
        if (exists != null && exists > 0) {
            jdbcTemplate.update("DELETE FROM favorite WHERE user_id = ? AND goods_id = ?", userId, goodsId);
            jdbcTemplate.update("UPDATE goods SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE id = ?", goodsId);
        } else {
            jdbcTemplate.update("INSERT INTO favorite(user_id, goods_id) VALUES(?, ?)", userId, goodsId);
            jdbcTemplate.update("UPDATE goods SET favorite_count = favorite_count + 1 WHERE id = ?", goodsId);
        }
    }

    public boolean isFavorite(Long userId, Long goodsId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM favorite WHERE user_id = ? AND goods_id = ?",
                Integer.class, userId, goodsId);
        return count != null && count > 0;
    }

    public List<Goods> favorites(Long userId) {
        return goodsList("""
                SELECT g.*, c.category_name, u.username AS seller_name
                FROM favorite f
                JOIN goods g ON f.goods_id = g.id
                JOIN category c ON g.category_id = c.id
                JOIN user u ON g.seller_id = u.id
                WHERE f.user_id = ? AND g.is_deleted = 0
                ORDER BY f.create_time DESC
                """, userId);
    }

    public void recordBrowse(Long userId, Long goodsId) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM browse_history WHERE user_id = ? AND goods_id = ?",
                Integer.class, userId, goodsId);
        if (exists != null && exists > 0) {
            jdbcTemplate.update("UPDATE browse_history SET browse_time = NOW() WHERE user_id = ? AND goods_id = ?", userId, goodsId);
        } else {
            jdbcTemplate.update("INSERT INTO browse_history(user_id, goods_id) VALUES(?, ?)", userId, goodsId);
        }
    }

    public List<Goods> history(Long userId) {
        return goodsList("""
                SELECT g.*, c.category_name, u.username AS seller_name
                FROM browse_history h
                JOIN goods g ON h.goods_id = g.id
                JOIN category c ON g.category_id = c.id
                JOIN user u ON g.seller_id = u.id
                WHERE h.user_id = ? AND g.is_deleted = 0
                ORDER BY h.browse_time DESC
                """, userId);
    }

    public List<GoodsMessage> messages(Long goodsId) {
        return jdbcTemplate.query("""
                SELECT m.*, u.username
                FROM message m
                JOIN user u ON m.user_id = u.id
                WHERE m.goods_id = ? AND m.is_deleted = 0 AND m.status = 1
                ORDER BY COALESCE(m.parent_id, m.id), m.create_time
                """, (rs, rowNum) -> {
            GoodsMessage message = new GoodsMessage();
            message.setId(rs.getLong("id"));
            message.setGoodsId(rs.getLong("goods_id"));
            message.setUserId(rs.getLong("user_id"));
            message.setUsername(rs.getString("username"));
            long parentId = rs.getLong("parent_id");
            message.setParentId(rs.wasNull() ? null : parentId);
            message.setContent(rs.getString("content"));
            message.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            return message;
        }, goodsId);
    }

    public void addMessage(Long userId, Long goodsId, Long parentId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Message content is required");
        }
        jdbcTemplate.update("INSERT INTO message(goods_id, user_id, parent_id, content) VALUES(?, ?, ?, ?)",
                goodsId, userId, parentId, content.trim());
        Long sellerId = jdbcTemplate.queryForObject("SELECT seller_id FROM goods WHERE id = ?", Long.class, goodsId);
        if (sellerId != null && !sellerId.equals(userId)) {
            notificationService.send(sellerId, "New message", "Your goods received a new message.", "message", goodsId);
        }
    }

    private List<Goods> goodsList(String sql, Long userId) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Goods goods = new Goods();
            goods.setId(rs.getLong("id"));
            goods.setSellerId(rs.getLong("seller_id"));
            goods.setSellerName(rs.getString("seller_name"));
            goods.setCategoryId(rs.getLong("category_id"));
            goods.setCategoryName(rs.getString("category_name"));
            goods.setTitle(rs.getString("title"));
            goods.setDescription(rs.getString("description"));
            goods.setPrice(rs.getBigDecimal("price"));
            goods.setConditionLevel(rs.getInt("condition_level"));
            goods.setTradePlace(rs.getString("trade_place"));
            goods.setCoverImageUrl(rs.getString("cover_image_url"));
            goods.setStatus(rs.getInt("status"));
            goods.setViewCount(rs.getInt("view_count"));
            goods.setPublishTime(rs.getTimestamp("publish_time").toLocalDateTime());
            return goods;
        }, userId);
    }
}

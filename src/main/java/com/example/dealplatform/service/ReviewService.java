package com.example.dealplatform.service;

import com.example.dealplatform.model.Review;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ReviewService {
    private final JdbcTemplate jdbcTemplate;

    public ReviewService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(Long buyerId, Long orderId, Integer score, String content) {
        if (score == null || score < 1 || score > 5) {
            throw new IllegalArgumentException("Score must be between 1 and 5");
        }
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM review WHERE order_id = ?", Integer.class, orderId);
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("Order already reviewed");
        }
        jdbcTemplate.update("""
                INSERT INTO review(order_id, goods_id, buyer_id, seller_id, score, content)
                SELECT id, goods_id, buyer_id, seller_id, ?, ?
                FROM orders
                WHERE id = ? AND buyer_id = ? AND status = 2
                """, score, StringUtils.hasText(content) ? content.trim() : null, orderId, buyerId);
    }

    public List<Review> byGoods(Long goodsId) {
        return list("WHERE r.goods_id = ?", goodsId);
    }

    public List<Review> all() {
        return list("");
    }

    private List<Review> list(String where, Object... args) {
        return jdbcTemplate.query("""
                SELECT r.*, g.title AS goods_title, buyer.username AS buyer_name, seller.username AS seller_name
                FROM review r
                JOIN goods g ON r.goods_id = g.id
                JOIN user buyer ON r.buyer_id = buyer.id
                JOIN user seller ON r.seller_id = seller.id
                """ + where + " ORDER BY r.create_time DESC", (rs, rowNum) -> {
            Review review = new Review();
            review.setId(rs.getLong("id"));
            review.setOrderId(rs.getLong("order_id"));
            review.setGoodsId(rs.getLong("goods_id"));
            review.setGoodsTitle(rs.getString("goods_title"));
            review.setBuyerName(rs.getString("buyer_name"));
            review.setSellerName(rs.getString("seller_name"));
            review.setScore(rs.getInt("score"));
            review.setContent(rs.getString("content"));
            review.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            return review;
        }, args);
    }
}

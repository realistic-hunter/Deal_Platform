package com.example.dealplatform.service;

import com.example.dealplatform.model.Goods;
import com.example.dealplatform.model.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoodsService {
    private final JdbcTemplate jdbcTemplate;

    public GoodsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Goods> rowMapper = (rs, rowNum) -> {
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
    };

    public PageResult<Goods> list(String keyword, Long categoryId, Integer page, Integer size) {
        return list(keyword, categoryId, null, false, page, size);
    }

    public PageResult<Goods> list(String keyword, Long categoryId, String sort, boolean onlyOnSale, Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 6 : size;
        StringBuilder where = new StringBuilder(" WHERE g.is_deleted = 0");
        List<Object> args = new ArrayList<>();
        if (onlyOnSale) {
            where.append(" AND g.status = 1");
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (g.title LIKE ? OR g.description LIKE ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (categoryId != null) {
            where.append(" AND g.category_id = ?");
            args.add(categoryId);
        }
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM goods g" + where, Integer.class, args.toArray());
        String orderBy = switch (sort == null ? "" : sort) {
            case "price_asc" -> " ORDER BY g.price ASC";
            case "price_desc" -> " ORDER BY g.price DESC";
            case "views" -> " ORDER BY g.view_count DESC";
            default -> " ORDER BY g.publish_time DESC";
        };
        args.add((pageNo - 1) * pageSize);
        args.add(pageSize);
        List<Goods> records = jdbcTemplate.query(baseSql() + where + orderBy + " LIMIT ?, ?",
                rowMapper, args.toArray());
        return new PageResult<>(records, pageNo, pageSize, total == null ? 0 : total);
    }

    public PageResult<Goods> listBySeller(Long sellerId, Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 8 : size;
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM goods WHERE seller_id = ? AND is_deleted = 0",
                Integer.class, sellerId);
        List<Goods> records = jdbcTemplate.query(baseSql() + """
                WHERE g.seller_id = ? AND g.is_deleted = 0
                ORDER BY g.publish_time DESC LIMIT ?, ?
                """, rowMapper, sellerId, (pageNo - 1) * pageSize, pageSize);
        return new PageResult<>(records, pageNo, pageSize, total == null ? 0 : total);
    }

    public Goods findById(Long id) {
        return jdbcTemplate.queryForObject(baseSql() + " WHERE g.id = ? AND g.is_deleted = 0", rowMapper, id);
    }

    public void create(Goods goods) {
        validate(goods);
        jdbcTemplate.update("""
                INSERT INTO goods(seller_id, category_id, title, description, price, condition_level, trade_place, cover_image_url, status)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, 0)
                """, goods.getSellerId(), goods.getCategoryId(), goods.getTitle().trim(), goods.getDescription().trim(),
                goods.getPrice(), goods.getConditionLevel(), goods.getTradePlace(), goods.getCoverImageUrl());
    }

    public void update(Goods goods, Long operatorId, boolean admin) {
        validate(goods);
        if (!admin) {
            assertOwner(goods.getId(), operatorId);
        }
        jdbcTemplate.update("""
                UPDATE goods
                SET category_id = ?, title = ?, description = ?, price = ?, condition_level = ?, trade_place = ?, cover_image_url = ?
                WHERE id = ? AND is_deleted = 0
                """, goods.getCategoryId(), goods.getTitle().trim(), goods.getDescription().trim(),
                goods.getPrice(), goods.getConditionLevel(), goods.getTradePlace(), goods.getCoverImageUrl(), goods.getId());
    }

    public void delete(Long id, Long operatorId, boolean admin) {
        if (!admin) {
            assertOwner(id, operatorId);
        }
        jdbcTemplate.update("UPDATE goods SET is_deleted = 1 WHERE id = ?", id);
    }

    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        jdbcTemplate.update("UPDATE goods SET is_deleted = 1 WHERE id IN (" + placeholders + ")", ids.toArray());
    }

    public void changeStatus(Long id, int status) {
        jdbcTemplate.update("UPDATE goods SET status = ? WHERE id = ? AND is_deleted = 0", status, id);
    }

    public void increaseViewCount(Long id) {
        jdbcTemplate.update("UPDATE goods SET view_count = view_count + 1 WHERE id = ?", id);
    }

    private void validate(Goods goods) {
        if (!StringUtils.hasText(goods.getTitle())) {
            throw new IllegalArgumentException("Goods title is required");
        }
        if (goods.getCategoryId() == null) {
            throw new IllegalArgumentException("Category is required");
        }
        if (goods.getPrice() == null || goods.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be greater than or equal to 0");
        }
        if (!StringUtils.hasText(goods.getDescription())) {
            throw new IllegalArgumentException("Goods description is required");
        }
    }

    private void assertOwner(Long goodsId, Long sellerId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM goods WHERE id = ? AND seller_id = ? AND is_deleted = 0",
                Integer.class, goodsId, sellerId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Only the seller can operate this goods");
        }
    }

    private String baseSql() {
        return """
                SELECT g.*, c.category_name, u.username AS seller_name
                FROM goods g
                JOIN category c ON g.category_id = c.id
                JOIN user u ON g.seller_id = u.id
                """;
    }
}

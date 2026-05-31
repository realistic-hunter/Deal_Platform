package com.example.dealplatform.service;

import com.example.dealplatform.model.Goods;
import com.example.dealplatform.model.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrderService {
    private final JdbcTemplate jdbcTemplate;
    private final GoodsService goodsService;
    private final NotificationService notificationService;

    public OrderService(JdbcTemplate jdbcTemplate, GoodsService goodsService, NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.goodsService = goodsService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void create(Long buyerId, Long goodsId, String tradePlace, String buyerRemark) {
        Goods goods = goodsService.findById(goodsId);
        if (goods.getSellerId().equals(buyerId)) {
            throw new IllegalArgumentException("Cannot buy your own goods");
        }
        if (goods.getStatus() != 1) {
            throw new IllegalArgumentException("Goods is not on sale");
        }
        if (!StringUtils.hasText(tradePlace)) {
            throw new IllegalArgumentException("Trade place is required");
        }
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE goods_id = ? AND status IN (0, 1, 2)",
                Integer.class, goodsId);
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("Goods already has an active order");
        }
        String orderNo = "OD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        jdbcTemplate.update("""
                INSERT INTO orders(order_no, goods_id, buyer_id, seller_id, total_amount, trade_place, buyer_remark, status)
                VALUES(?, ?, ?, ?, ?, ?, ?, 1)
                """, orderNo, goodsId, buyerId, goods.getSellerId(), goods.getPrice(), tradePlace.trim(), buyerRemark);
        Long orderId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("UPDATE goods SET status = 3 WHERE id = ?", goodsId);
        log(orderId, null, 1, buyerId, "Create order");
        notificationService.send(goods.getSellerId(), "New order", "Your goods has a new order.", "order", orderId);
    }

    @Transactional
    public void cancel(Long orderId, Long buyerId) {
        Order order = findById(orderId);
        if (!order.getBuyerId().equals(buyerId) || order.getStatus() > 1) {
            throw new IllegalArgumentException("Order cannot be cancelled");
        }
        jdbcTemplate.update("UPDATE orders SET status = 3, cancel_time = NOW() WHERE id = ?", orderId);
        jdbcTemplate.update("UPDATE goods SET status = 1 WHERE id = ?", order.getGoodsId());
        log(orderId, order.getStatus(), 3, buyerId, "Cancel order");
        notificationService.send(order.getSellerId(), "Order cancelled", "Buyer cancelled an order.", "order", orderId);
    }

    @Transactional
    public void complete(Long orderId, Long sellerId) {
        Order order = findById(orderId);
        if (!order.getSellerId().equals(sellerId) || order.getStatus() != 1) {
            throw new IllegalArgumentException("Order cannot be completed");
        }
        jdbcTemplate.update("UPDATE orders SET status = 2, complete_time = NOW() WHERE id = ?", orderId);
        jdbcTemplate.update("UPDATE goods SET status = 3 WHERE id = ?", order.getGoodsId());
        log(orderId, order.getStatus(), 2, sellerId, "Complete order");
        notificationService.send(order.getBuyerId(), "Order completed", "Seller confirmed the transaction completed.", "order", orderId);
    }

    public List<Order> buyerOrders(Long buyerId) {
        return list("WHERE o.buyer_id = ?", buyerId);
    }

    public List<Order> sellerOrders(Long sellerId) {
        return list("WHERE o.seller_id = ?", sellerId);
    }

    public List<Order> allOrders() {
        return list("");
    }

    private Order findById(Long id) {
        return jdbcTemplate.queryForObject(baseSql() + " WHERE o.id = ?", (rs, rowNum) -> mapOrder(rs), id);
    }

    private List<Order> list(String where, Object... args) {
        return jdbcTemplate.query(baseSql() + " " + where + " ORDER BY o.create_time DESC", (rs, rowNum) -> mapOrder(rs), args);
    }

    private void log(Long orderId, Integer oldStatus, Integer newStatus, Long operatorId, String remark) {
        jdbcTemplate.update("""
                INSERT INTO order_status_log(order_id, old_status, new_status, operator_id, remark)
                VALUES(?, ?, ?, ?, ?)
                """, orderId, oldStatus, newStatus, operatorId, remark);
    }

    private String baseSql() {
        return """
                SELECT o.*, g.title AS goods_title, buyer.username AS buyer_name, seller.username AS seller_name
                FROM orders o
                JOIN goods g ON o.goods_id = g.id
                JOIN user buyer ON o.buyer_id = buyer.id
                JOIN user seller ON o.seller_id = seller.id
                """;
    }

    private Order mapOrder(java.sql.ResultSet rs) throws java.sql.SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setOrderNo(rs.getString("order_no"));
        order.setGoodsId(rs.getLong("goods_id"));
        order.setGoodsTitle(rs.getString("goods_title"));
        order.setBuyerId(rs.getLong("buyer_id"));
        order.setBuyerName(rs.getString("buyer_name"));
        order.setSellerId(rs.getLong("seller_id"));
        order.setSellerName(rs.getString("seller_name"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setTradePlace(rs.getString("trade_place"));
        order.setBuyerRemark(rs.getString("buyer_remark"));
        order.setStatus(rs.getInt("status"));
        order.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return order;
    }
}

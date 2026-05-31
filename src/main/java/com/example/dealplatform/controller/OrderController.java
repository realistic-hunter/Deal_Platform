package com.example.dealplatform.controller;

import com.example.dealplatform.model.User;
import com.example.dealplatform.service.OrderService;
import com.example.dealplatform.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class OrderController {
    private final OrderService orderService;
    private final ReviewService reviewService;

    public OrderController(OrderService orderService, ReviewService reviewService) {
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    @PostMapping("/orders/create/{goodsId}")
    public String create(@PathVariable Long goodsId, String tradePlace, String buyerRemark, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        orderService.create(user.getId(), goodsId, tradePlace, buyerRemark);
        return "redirect:/orders/buy";
    }

    @GetMapping("/orders/buy")
    public String buy(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("orders", orderService.buyerOrders(user.getId()));
        model.addAttribute("title", "我的购买订单");
        return "orders/list";
    }

    @GetMapping("/orders/sell")
    public String sell(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("orders", orderService.sellerOrders(user.getId()));
        model.addAttribute("title", "我的出售订单");
        return "orders/list";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        orderService.cancel(id, user.getId());
        return "redirect:/orders/buy";
    }

    @PostMapping("/orders/{id}/complete")
    public String complete(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        orderService.complete(id, user.getId());
        return "redirect:/orders/sell";
    }

    @PostMapping("/orders/{id}/review")
    public String review(@PathVariable Long id, Integer score, String content, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        reviewService.create(user.getId(), id, score, content);
        return "redirect:/orders/buy";
    }
}

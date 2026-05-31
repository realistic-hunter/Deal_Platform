package com.example.dealplatform.controller;

import com.example.dealplatform.model.User;
import com.example.dealplatform.service.CategoryService;
import com.example.dealplatform.service.BannerService;
import com.example.dealplatform.service.ExportService;
import com.example.dealplatform.service.GoodsService;
import com.example.dealplatform.service.NoticeService;
import com.example.dealplatform.service.NotificationService;
import com.example.dealplatform.service.OperationLogService;
import com.example.dealplatform.service.OrderService;
import com.example.dealplatform.service.ReportService;
import com.example.dealplatform.service.ReviewService;
import com.example.dealplatform.service.StatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class AdminController {
    private final GoodsService goodsService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final NoticeService noticeService;
    private final NotificationService notificationService;
    private final StatisticsService statisticsService;
    private final ReportService reportService;
    private final ReviewService reviewService;
    private final OperationLogService operationLogService;
    private final BannerService bannerService;
    private final ExportService exportService;

    public AdminController(GoodsService goodsService, CategoryService categoryService,
                           OrderService orderService, NoticeService noticeService,
                           NotificationService notificationService, StatisticsService statisticsService,
                           ReportService reportService, ReviewService reviewService,
                           OperationLogService operationLogService, BannerService bannerService,
                           ExportService exportService) {
        this.goodsService = goodsService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.noticeService = noticeService;
        this.notificationService = notificationService;
        this.statisticsService = statisticsService;
        this.reportService = reportService;
        this.reviewService = reviewService;
        this.operationLogService = operationLogService;
        this.bannerService = bannerService;
        this.exportService = exportService;
    }

    @GetMapping("/admin")
    public String index(Model model) {
        model.addAttribute("pageData", goodsService.list(null, null, 1, 5));
        model.addAttribute("cards", statisticsService.cards());
        model.addAttribute("goodsByCategory", statisticsService.goodsByCategory());
        model.addAttribute("ordersByStatus", statisticsService.ordersByStatus());
        return "admin/index";
    }

    @GetMapping("/admin/goods")
    public String goods(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(defaultValue = "1") Integer page,
                        Model model) {
        model.addAttribute("pageData", goodsService.list(keyword, categoryId, page, 8));
        model.addAttribute("categories", categoryService.findAllEnabled());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        return "admin/goods";
    }

    @PostMapping("/admin/goods/{id}/status")
    public String status(@PathVariable Long id, int status, HttpSession session, HttpServletRequest request) {
        var goods = goodsService.findById(id);
        goodsService.changeStatus(id, status);
        User user = (User) session.getAttribute("loginUser");
        operationLogService.record(user, "GOODS_AUDIT", "Change goods " + id + " status to " + status, request);
        if (status == 1 || status == 4) {
            String title = status == 1 ? "Goods approved" : "Goods rejected";
            String content = status == 1 ? "Your goods has been approved and listed." : "Your goods was rejected by admin.";
            notificationService.send(goods.getSellerId(), title, content, "audit", id);
        }
        return "redirect:/admin/goods";
    }

    @PostMapping("/admin/goods/batch-delete")
    public String batchDelete(@RequestParam(required = false) List<Long> ids, HttpSession session, HttpServletRequest request) {
        goodsService.batchDelete(ids);
        operationLogService.record((User) session.getAttribute("loginUser"), "BATCH_DELETE", "Batch delete goods: " + ids, request);
        return "redirect:/admin/goods";
    }

    @GetMapping("/admin/goods/export")
    public void exportGoods(HttpServletResponse response) throws IOException {
        String filename = URLEncoder.encode("goods-export.csv", StandardCharsets.UTF_8);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.getWriter().write(exportService.goodsCsv());
    }

    @GetMapping("/admin/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories";
    }

    @PostMapping("/admin/categories")
    public String saveCategory(String categoryName, Integer sortOrder) {
        categoryService.save(categoryName, sortOrder);
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}")
    public String updateCategory(@PathVariable Long id, String categoryName, Integer sortOrder, Integer status) {
        categoryService.update(id, categoryName, sortOrder, status);
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/orders")
    public String orders(Model model) {
        model.addAttribute("orders", orderService.allOrders());
        model.addAttribute("title", "订单管理");
        return "orders/list";
    }

    @GetMapping("/admin/notices")
    public String notices(Model model) {
        model.addAttribute("notices", noticeService.all());
        return "admin/notices";
    }

    @PostMapping("/admin/notices")
    public String saveNotice(String title, String content, Integer status, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        noticeService.save(user.getId(), title, content, status);
        return "redirect:/admin/notices";
    }

    @PostMapping("/admin/notices/{id}")
    public String updateNotice(@PathVariable Long id, String title, String content, Integer status) {
        noticeService.update(id, title, content, status);
        return "redirect:/admin/notices";
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("reports", reportService.list());
        return "admin/reports";
    }

    @PostMapping("/admin/reports/{id}")
    public String handleReport(@PathVariable Long id, Integer status, String handleResult, HttpSession session, HttpServletRequest request) {
        User user = (User) session.getAttribute("loginUser");
        reportService.handle(id, user.getId(), status, handleResult);
        operationLogService.record(user, "REPORT_HANDLE", "Handle report " + id, request);
        return "redirect:/admin/reports";
    }

    @GetMapping("/admin/reviews")
    public String reviews(Model model) {
        model.addAttribute("reviews", reviewService.all());
        return "admin/reviews";
    }

    @GetMapping("/admin/logs")
    public String logs(Model model) {
        model.addAttribute("logs", operationLogService.list());
        return "admin/logs";
    }

    @GetMapping("/admin/banners")
    public String banners(Model model) {
        model.addAttribute("banners", bannerService.all());
        return "admin/banners";
    }

    @PostMapping("/admin/banners")
    public String saveBanner(String title, String imageUrl, String linkUrl, Integer sortOrder, Integer status) {
        bannerService.save(title, imageUrl, linkUrl, sortOrder, status);
        return "redirect:/admin/banners";
    }

    @PostMapping("/admin/banners/{id}")
    public String updateBanner(@PathVariable Long id, String title, String imageUrl, String linkUrl, Integer sortOrder, Integer status) {
        bannerService.update(id, title, imageUrl, linkUrl, sortOrder, status);
        return "redirect:/admin/banners";
    }
}

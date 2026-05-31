package com.example.dealplatform.controller;

import com.example.dealplatform.model.Goods;
import com.example.dealplatform.model.User;
import com.example.dealplatform.service.CategoryService;
import com.example.dealplatform.service.BannerService;
import com.example.dealplatform.service.GoodsService;
import com.example.dealplatform.service.InteractionService;
import com.example.dealplatform.service.NoticeService;
import com.example.dealplatform.service.ReportService;
import com.example.dealplatform.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Controller
public class GoodsController {
    private final GoodsService goodsService;
    private final CategoryService categoryService;
    private final InteractionService interactionService;
    private final NoticeService noticeService;
    private final ReviewService reviewService;
    private final ReportService reportService;
    private final BannerService bannerService;

    public GoodsController(GoodsService goodsService, CategoryService categoryService,
                           InteractionService interactionService, NoticeService noticeService,
                           ReviewService reviewService, ReportService reportService, BannerService bannerService) {
        this.goodsService = goodsService;
        this.categoryService = categoryService;
        this.interactionService = interactionService;
        this.noticeService = noticeService;
        this.reviewService = reviewService;
        this.reportService = reportService;
        this.bannerService = bannerService;
    }

    @GetMapping("/goods")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String sort,
                       @RequestParam(defaultValue = "1") Integer page,
                       Model model) {
        model.addAttribute("pageData", goodsService.list(keyword, categoryId, sort, true, page, 6));
        model.addAttribute("categories", categoryService.findAllEnabled());
        model.addAttribute("notices", noticeService.publicNotices());
        model.addAttribute("banners", bannerService.publicBanners());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        return "goods/list";
    }

    @GetMapping("/goods/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        goodsService.increaseViewCount(id);
        Goods goods = goodsService.findById(id);
        User user = (User) session.getAttribute("loginUser");
        if (user != null) {
            interactionService.recordBrowse(user.getId(), id);
            model.addAttribute("favorite", interactionService.isFavorite(user.getId(), id));
        }
        model.addAttribute("goods", goods);
        model.addAttribute("messages", interactionService.messages(id));
        model.addAttribute("reviews", reviewService.byGoods(id));
        return "goods/detail";
    }

    @GetMapping("/goods/create")
    public String createPage(Model model) {
        model.addAttribute("goods", new Goods());
        model.addAttribute("categories", categoryService.findAllEnabled());
        return "goods/form";
    }

    @PostMapping("/goods/save")
    public String save(Goods goods, @RequestParam(required = false) MultipartFile imageFile,
                       HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        goods.setSellerId(user.getId());
        try {
            goods.setCoverImageUrl(saveImage(imageFile, goods.getCoverImageUrl()));
            goodsService.create(goods);
            return "redirect:/goods/my";
        } catch (IllegalArgumentException | IOException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("goods", goods);
            model.addAttribute("categories", categoryService.findAllEnabled());
            return "goods/form";
        }
    }

    @GetMapping("/goods/my")
    public String myGoods(@RequestParam(defaultValue = "1") Integer page, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("pageData", goodsService.listBySeller(user.getId(), page, 8));
        return "goods/my";
    }

    @GetMapping("/goods/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("goods", goodsService.findById(id));
        model.addAttribute("categories", categoryService.findAllEnabled());
        return "goods/form";
    }

    @PostMapping("/goods/update/{id}")
    public String update(@PathVariable Long id, Goods goods, @RequestParam(required = false) MultipartFile imageFile,
                         HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        goods.setId(id);
        boolean admin = "admin".equals(user.getRoleCode());
        try {
            goods.setCoverImageUrl(saveImage(imageFile, goods.getCoverImageUrl()));
            goodsService.update(goods, user.getId(), admin);
            return admin ? "redirect:/admin/goods" : "redirect:/goods/my";
        } catch (IllegalArgumentException | IOException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("goods", goods);
            model.addAttribute("categories", categoryService.findAllEnabled());
            return "goods/form";
        }
    }

    @PostMapping("/goods/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        goodsService.delete(id, user.getId(), "admin".equals(user.getRoleCode()));
        return "admin".equals(user.getRoleCode()) ? "redirect:/admin/goods" : "redirect:/goods/my";
    }

    @PostMapping("/goods/{id}/favorite")
    public String favorite(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        interactionService.favorite(user.getId(), id);
        return "redirect:/goods/" + id;
    }

    @PostMapping("/goods/{id}/message")
    public String message(@PathVariable Long id, @RequestParam(required = false) Long parentId,
                          String content, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        interactionService.addMessage(user.getId(), id, parentId, content);
        return "redirect:/goods/" + id;
    }

    @PostMapping("/goods/{id}/report")
    public String report(@PathVariable Long id, String reason, String description, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        reportService.create(user.getId(), "goods", id, reason, description);
        return "redirect:/goods/" + id;
    }

    @GetMapping("/favorites")
    public String favorites(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("goodsList", interactionService.favorites(user.getId()));
        model.addAttribute("title", "我的收藏");
        return "goods/simple-list";
    }

    @GetMapping("/history")
    public String history(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("goodsList", interactionService.history(user.getId()));
        model.addAttribute("title", "浏览记录");
        return "goods/simple-list";
    }

    private String saveImage(MultipartFile file, String oldUrl) throws IOException {
        if (file == null || file.isEmpty()) {
            return oldUrl;
        }
        String original = file.getOriginalFilename() == null ? "goods.jpg" : file.getOriginalFilename();
        String suffix = original.contains(".") ? original.substring(original.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID() + suffix;
        Path dir = Path.of("src", "main", "resources", "static", "uploads", "goods");
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(filename));
        return "/uploads/goods/" + filename;
    }
}

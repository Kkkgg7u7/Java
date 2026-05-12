package com.account.controller;

import com.account.entity.Category;
import com.account.service.CategoryService;
import com.account.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Integer type, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        List<Category> categories = type != null 
            ? categoryService.getByUserIdAndType(userId, type) 
            : categoryService.getByUserId(userId);
        return Result.success(categories);
    }

    @PostMapping("/add")
    public Result add(@RequestParam String categoryName,
                     @RequestParam Integer type,
                     HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        Category category = new Category();
        category.setUserId(userId);
        category.setCategoryName(categoryName);
        category.setType(type);
        boolean success = categoryService.add(category);
        return success ? Result.success("添加成功") : Result.error("添加失败");
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        Category category = categoryService.getById(id);
        if (category == null || !category.getUserId().equals(userId)) {
            return Result.error("分类不存在或无权操作");
        }
        boolean success = categoryService.delete(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}

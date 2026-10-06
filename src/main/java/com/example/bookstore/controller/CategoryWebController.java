package com.example.bookstore.controller;

import com.example.bookstore.entity.Category;
import com.example.bookstore.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/categories")
public class CategoryWebController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public String viewCategoriesPage(Model model) {
        model.addAttribute("listCategories", categoryService.getAllCategories());
        model.addAttribute("category", new Category());
        return "admin/categories";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("category") Category category) {
        categoryService.saveCategory(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/edit/{id}")
    public String showEditCategoryForm(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("listCategories", categoryService.getAllCategories());
        model.addAttribute("category", categoryService.getCategoryById(id));
        return "admin/categories";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id) {
        categoryService.deleteCategory(id);
        return "redirect:/admin/categories";
    }
}

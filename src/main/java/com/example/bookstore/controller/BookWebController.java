package com.example.bookstore.controller;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Category;
import com.example.bookstore.service.BookService;
import com.example.bookstore.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/books")
public class BookWebController {

    @Autowired
    private BookService bookService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public String viewBooksPage(Model model) {
        model.addAttribute("listBooks", bookService.getAllBooks());
        return "admin/books";
    }

    @GetMapping("/new")
    public String showNewBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("listCategories", categoryService.getAllCategories());
        model.addAttribute("bookImages", java.util.List.of());
        return "admin/book-form";
    }

    @PostMapping("/save")
    public String saveBook(@ModelAttribute("book") Book formBook,
                           @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
                           RedirectAttributes redirectAttributes) {
        try {
            Book book;
            if (formBook.getId() != null) {
                book = bookService.getBookById(formBook.getId());
                if (book == null) {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy sách.");
                    return "redirect:/admin/books";
                }
                book.setTitle(formBook.getTitle());
                book.setAuthor(formBook.getAuthor());
                book.setDescription(formBook.getDescription());
                book.setPrice(formBook.getPrice());
                book.setStockQuantity(formBook.getStockQuantity());
            } else {
                book = formBook;
                if (book.getImages() == null) {
                    book.setImages(new java.util.ArrayList<>());
                }
            }

            if (formBook.getCategory() != null && formBook.getCategory().getId() != null) {
                Category cat = categoryService.getCategoryById(formBook.getCategory().getId());
                book.setCategory(cat);
            } else {
                book.setCategory(null);
            }

            bookService.saveBook(book);
            bookService.addImages(book, imageFiles);
            redirectAttributes.addFlashAttribute("success", "Đã lưu sách.");
            return "redirect:/admin/books/edit/" + book.getId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Lỗi lưu sách: " + ex.getMessage());
            if (formBook.getId() != null) {
                return "redirect:/admin/books/edit/" + formBook.getId();
            }
            return "redirect:/admin/books/new";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditBookForm(@PathVariable("id") Integer id, Model model) {
        Book book = bookService.getBookById(id);
        if (book == null) {
            return "redirect:/admin/books";
        }
        bookService.ensureCoverSyncedToGallery(book);
        model.addAttribute("book", book);
        model.addAttribute("listCategories", categoryService.getAllCategories());
        model.addAttribute("bookImages", bookService.getBookImages(id));
        return "admin/book-form";
    }

    @PostMapping("/{bookId}/images/{imageId}/delete")
    public String deleteImage(@PathVariable("bookId") Integer bookId,
                              @PathVariable("imageId") Integer imageId,
                              RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBookImage(bookId, imageId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa ảnh.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không xóa được ảnh: " + ex.getMessage());
        }
        return "redirect:/admin/books/edit/" + bookId;
    }

    @PostMapping("/{bookId}/images/{imageId}/cover")
    public String setCover(@PathVariable("bookId") Integer bookId,
                           @PathVariable("imageId") Integer imageId,
                           RedirectAttributes redirectAttributes) {
        try {
            bookService.setCoverImage(bookId, imageId);
            redirectAttributes.addFlashAttribute("success", "Đã đặt ảnh bìa.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không đặt được ảnh bìa: " + ex.getMessage());
        }
        return "redirect:/admin/books/edit/" + bookId;
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable("id") Integer id) {
        bookService.deleteBook(id);
        return "redirect:/admin/books";
    }
}

package com.example.bookstore.model;

import com.example.bookstore.entity.Book;

public class CartItem {
    private Integer bookId;
    private String title;
    private String author;
    private Double price;
    private Integer quantity;
    private Integer stockQuantity;

    public CartItem() {
    }

    public CartItem(Book book, Integer quantity) {
        this.bookId = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.price = book.getPrice();
        this.stockQuantity = book.getStockQuantity();
        this.quantity = quantity;
    }

    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public double getLineTotal() {
        if (price == null || quantity == null) {
            return 0;
        }
        return price * quantity;
    }
}

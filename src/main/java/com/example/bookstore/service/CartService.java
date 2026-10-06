package com.example.bookstore.service;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.DiscountCode;
import com.example.bookstore.model.CartItem;
import com.example.bookstore.repository.DiscountCodeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    public static final String CART_SESSION_KEY = "cart";
    public static final String COUPON_SESSION_KEY = "couponCode";

    @Autowired
    private BookService bookService;

    @Autowired
    private DiscountCodeRepository discountCodeRepository;

    @SuppressWarnings("unchecked")
    public Map<Integer, CartItem> getCartMap(HttpSession session) {
        Object raw = session.getAttribute(CART_SESSION_KEY);
        if (raw instanceof Map<?, ?> map) {
            return (Map<Integer, CartItem>) map;
        }
        Map<Integer, CartItem> cart = new LinkedHashMap<>();
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    public List<CartItem> getItems(HttpSession session) {
        return new ArrayList<>(getCartMap(session).values());
    }

    public int getItemCount(HttpSession session) {
        return getCartMap(session).values().stream()
                .mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0)
                .sum();
    }

    public double getSubtotal(HttpSession session) {
        return getCartMap(session).values().stream()
                .mapToDouble(CartItem::getLineTotal)
                .sum();
    }

    public double getTotal(HttpSession session) {
        return Math.max(0, getSubtotal(session) - getDiscountAmount(session));
    }

    public DiscountCode getAppliedDiscount(HttpSession session) {
        Object raw = session.getAttribute(COUPON_SESSION_KEY);
        if (!(raw instanceof String code) || code.isBlank()) {
            return null;
        }
        DiscountCode found = discountCodeRepository.findByCodeIgnoreCase(code.trim());
        if (found == null || !Boolean.TRUE.equals(found.getActive()) || !found.hasRemainingUses()) {
            session.removeAttribute(COUPON_SESSION_KEY);
            return null;
        }
        return found;
    }

    public double getDiscountAmount(HttpSession session) {
        DiscountCode discount = getAppliedDiscount(session);
        if (discount == null || discount.getPercentOff() == null) {
            return 0;
        }
        return getSubtotal(session) * discount.getPercentOff() / 100.0;
    }

    public String applyCoupon(HttpSession session, String code) {
        if (code == null || code.isBlank()) {
            return "Vui lòng nhập mã giảm giá.";
        }
        DiscountCode found = discountCodeRepository.findByCodeIgnoreCase(code.trim());
        if (found == null) {
            return "Mã giảm giá không tồn tại.";
        }
        if (!Boolean.TRUE.equals(found.getActive())) {
            return "Mã giảm giá đã bị tắt.";
        }
        if (!found.hasRemainingUses()) {
            return "Mã giảm giá đã hết lượt sử dụng.";
        }
        session.setAttribute(COUPON_SESSION_KEY, found.getCode());
        return null;
    }

    /** Trừ 1 lượt dùng sau khi đặt hàng thành công. */
    public void consumeCoupon(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        DiscountCode found = discountCodeRepository.findByCodeIgnoreCase(code.trim());
        if (found == null) {
            return;
        }
        int used = found.getUsedCount() != null ? found.getUsedCount() : 0;
        found.setUsedCount(used + 1);
        if (found.getMaxUses() != null && found.getUsedCount() >= found.getMaxUses()) {
            found.setActive(false);
        }
        discountCodeRepository.save(found);
    }

    public void removeCoupon(HttpSession session) {
        session.removeAttribute(COUPON_SESSION_KEY);
    }

    public String addToCart(HttpSession session, Integer bookId, Integer quantity) {
        if (bookId == null) {
            return "Thiếu mã sách.";
        }
        int qty = quantity != null && quantity > 0 ? quantity : 1;
        Book book = bookService.getBookById(bookId);
        if (book == null) {
            return "Không tìm thấy sách.";
        }
        int stock = book.getStockQuantity() != null ? book.getStockQuantity() : 0;
        if (stock <= 0) {
            return "Sách đã hết hàng.";
        }

        Map<Integer, CartItem> cart = getCartMap(session);
        CartItem existing = cart.get(bookId);
        int newQty = (existing != null ? existing.getQuantity() : 0) + qty;
        if (newQty > stock) {
            return "Không đủ tồn kho (còn " + stock + " cuốn).";
        }

        if (existing == null) {
            cart.put(bookId, new CartItem(book, newQty));
        } else {
            existing.setQuantity(newQty);
            existing.setPrice(book.getPrice());
            existing.setStockQuantity(stock);
            existing.setTitle(book.getTitle());
        }
        return null;
    }

    public String updateQuantity(HttpSession session, Integer bookId, Integer quantity) {
        Map<Integer, CartItem> cart = getCartMap(session);
        CartItem item = cart.get(bookId);
        if (item == null) {
            return "Sản phẩm không có trong giỏ.";
        }
        if (quantity == null || quantity <= 0) {
            cart.remove(bookId);
            return null;
        }
        Book book = bookService.getBookById(bookId);
        int stock = book != null && book.getStockQuantity() != null ? book.getStockQuantity() : 0;
        if (quantity > stock) {
            return "Không đủ tồn kho (còn " + stock + " cuốn).";
        }
        item.setQuantity(quantity);
        if (book != null) {
            item.setPrice(book.getPrice());
            item.setStockQuantity(stock);
        }
        return null;
    }

    public void removeItem(HttpSession session, Integer bookId) {
        getCartMap(session).remove(bookId);
    }

    public void clear(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
        session.removeAttribute(COUPON_SESSION_KEY);
    }
}

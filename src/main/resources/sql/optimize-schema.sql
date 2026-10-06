-- ============================================================
-- Tối ưu database book_store (chạy 1 lần trong phpMyAdmin)
-- Hibernate ddl-auto=update KHÔNG xóa cột thừa → cần script này.
-- Nếu báo "Duplicate key name" khi tạo index → bỏ qua (đã có sẵn).
-- ============================================================

USE book_store;

-- ----- 1. Đồng bộ unit_price trước khi xóa cột price -----
UPDATE order_details
SET unit_price = price
WHERE price IS NOT NULL
  AND (unit_price IS NULL OR unit_price = 0);

-- ----- 2. Xóa cột thừa (chạy từng câu; bỏ qua nếu "Unknown column") -----
ALTER TABLE orders DROP COLUMN shipping_address;
ALTER TABLE return_requests DROP COLUMN request_date;
ALTER TABLE return_requests DROP COLUMN admin_comment;
ALTER TABLE order_details DROP COLUMN price;

-- ----- 3. Index tra cứu (bỏ qua nếu đã tồn tại) -----
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_books_category ON books (category_id);
CREATE INDEX idx_books_title ON books (title);
CREATE INDEX idx_book_images_book ON book_images (book_id);
CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_date ON orders (order_date);
CREATE INDEX idx_order_details_order ON order_details (order_id);
CREATE INDEX idx_order_details_book ON order_details (book_id);
CREATE INDEX idx_returns_order ON return_requests (order_id);
CREATE INDEX idx_returns_user ON return_requests (user_id);
CREATE INDEX idx_returns_status ON return_requests (status);
CREATE INDEX idx_shifts_user ON work_shifts (user_id);
CREATE INDEX idx_shifts_date ON work_shifts (work_date);
CREATE INDEX idx_discount_active ON discount_codes (active);

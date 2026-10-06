Trí Tuệ Bookstore - Hệ thống Quản lý Nhà sách Trực tuyến

**Trí Tuệ Bookstore** là một ứng dụng web thương mại điện tử chuyên cung cấp các đầu sách tuyển chọn. Điểm nhấn kỹ thuật của dự án là việc xử lý luồng nghiệp vụ mua bán khép kín: từ lúc khách hàng duyệt sách, đặt hàng, cho đến tính năng nâng cao là **quản lý hoàn trả đơn hàng (Order Return Management)** với khả năng tự động đồng bộ hóa hàng tồn kho.

Dự án được phát triển dưới dạng bài tập lớn môn **Công nghệ Java**, áp dụng triệt để kiến trúc MVC và các công nghệ hiện đại của Spring Framework.

Ngăn xếp Công nghệ (Tech Stack)

*   **Ngôn ngữ lập trình:** Java 17.
*   **Backend Framework:** Spring Boot 3.x, Spring Web MVC.
*   **Truy cập dữ liệu (ORM):** Spring Data JPA, Hibernate.
*   **Cơ sở dữ liệu:** MySQL (RDBMS).
*   **Template Engine (Frontend):** Thymeleaf.
*   **Giao diện & UI/UX:** HTML5, CSS3 nguyên bản (Thiết kế theo phong cách Modern, Minimalist E-commerce).
*   **Môi trường phát triển:** Apache NetBeans / IntelliJ IDEA, Maven.
*   **Quản lý phiên bản:** Git, GitHub.
---

Tính năng Nổi bật & Luồng Nghiệp vụ (Business Flow)
1. Phân hệ Khách hàng (Customer Site)
*   **Duyệt Sản phẩm:** Hiển thị danh sách sách theo dạng lưới hiện đại. Phân trang và lọc theo danh mục.
*   **Quản lý Giỏ hàng (Shopping Cart):** Sử dụng cơ chế Session/Cookie để lưu trữ sản phẩm tạm thời. Người dùng có thể tăng/giảm số lượng hoặc xóa sản phẩm khỏi giỏ.
*   **Quy trình Thanh toán (Checkout):** Xác nhận thông tin giao hàng, tính tổng tiền (Total Amount) và tạo `Order` mới với trạng thái ban đầu là `PENDING`.
*   **Yêu cầu Hoàn trả (Return Request):** 
    * Chỉ các đơn hàng đã giao (`DELIVERED`) mới có quyền yêu cầu hoàn trả.
    * Khách hàng điền lý do, hệ thống tạo bản ghi `ReturnRequest` và chuyển trạng thái `Order` sang `RETURN_REQUESTED`.
2. Phân hệ Quản trị (Admin Dashboard)
*   **Quản lý Danh mục & Sản phẩm:** Thêm, Sửa, Xóa thông tin sách, cập nhật ảnh bìa và giá bán.
*   **Xử lý Đơn hàng:** Theo dõi và cập nhật trạng thái đơn hàng (từ `PENDING` -> `SHIPPED` -> `DELIVERED`).
*   **Xử lý Nghiệp vụ Hoàn trả (Core Feature):**
    * Xem danh sách các yêu cầu hoàn trả đang chờ (`PENDING`).
    * **Approve (Chấp nhận):** Nếu Admin duyệt, hệ thống sẽ thực hiện một Transaction quan trọng: Cập nhật `ReturnRequest` thành `APPROVED`, đổi trạng thái `Order` thành `RETURNED`, và **quét qua toàn bộ `OrderDetail` của đơn đó để cộng ngược lại trường `quantity` vào `stock_quantity` của bảng `Book`**.
    * **Reject (Từ chối):** Hủy yêu cầu hoàn trả, trạng thái đơn hàng giữ nguyên `DELIVERED`.

Mô hình Cấu trúc Cơ sở dữ liệu (Database Schema)
Hệ thống xoay quanh 5 thực thể chính được liên kết chặt chẽ qua JPA:
1.  `User`: Lưu thông tin đăng nhập và phân quyền (ROLE_USER, ROLE_ADMIN).
2.  **`Category`: Phân loại sách. (Quan hệ 1-N với Book).
3.  `Book`: Quản lý thông tin sách, đặc biệt là trường tồn kho (`stock_quantity`).
4.  `Order` & `OrderDetail`: Lưu trữ hóa đơn tổng và chi tiết từng mặt hàng đã mua. (Quan hệ 1-N).
5.  `ReturnRequest`: Bảng nghiệp vụ chuyên biệt quản lý lịch sử và lý do hoàn trả của khách hàng. (Quan hệ 1-1 hoặc N-1 với Order).

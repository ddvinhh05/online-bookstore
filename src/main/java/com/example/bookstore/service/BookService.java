package com.example.bookstore.service;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.BookImage;
import com.example.bookstore.repository.BookImageRepository;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.util.ImageStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class BookService {

    public static final Path UPLOAD_DIR = Paths.get("uploads", "books").toAbsolutePath().normalize();
    public static final int MAX_IMAGES_PER_BOOK = 8;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookImageRepository bookImageRepository;

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    /** Sách nổi bật trang chủ: ưu tiên còn hàng, có ảnh, mới hơn. */
    public List<Book> getFeaturedBooks(int limit) {
        int size = Math.max(1, limit);
        return bookRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((Book b) -> b.getStockQuantity() == null || b.getStockQuantity() <= 0)
                        .thenComparing(b -> b.getImagePath() == null || b.getImagePath().isBlank())
                        .thenComparing(Book::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(size)
                .toList();
    }

    /** Sách mới: theo id giảm dần. */
    public List<Book> getNewestBooks(int limit) {
        int size = Math.max(1, limit);
        return bookRepository.findAll().stream()
                .sorted(Comparator.comparing(Book::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(size)
                .toList();
    }

    public List<Book> searchBooks(String keyword, Integer categoryId) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCategory = categoryId != null;

        if (!hasKeyword && !hasCategory) {
            return getAllBooks();
        }
        if (hasCategory && !hasKeyword) {
            return bookRepository.findByCategory_Id(categoryId);
        }
        if (!hasCategory) {
            return bookRepository.search(keyword.trim());
        }
        return bookRepository.searchInCategory(keyword.trim(), categoryId);
    }

    public void saveBook(Book book) {
        bookRepository.save(book);
    }

    public String saveBookImage(MultipartFile file) throws IOException {
        return ImageStorage.save(file, UPLOAD_DIR, "/uploads/books/");
    }

    public Book getBookById(Integer id) {
        return bookRepository.findById(id).orElse(null);
    }

    public List<BookImage> getBookImages(Integer bookId) {
        if (bookId == null) {
            return List.of();
        }
        return bookImageRepository.findByBook_IdOrderBySortOrderAscIdAsc(bookId);
    }

    /** Tất cả URL ảnh để hiển thị gallery (bìa + phụ). */
    public List<String> getDisplayImagePaths(Book book) {
        List<String> paths = new ArrayList<>();
        if (book == null) {
            return paths;
        }
        if (book.getId() != null) {
            for (BookImage img : getBookImages(book.getId())) {
                if (img.getImagePath() != null && !img.getImagePath().isBlank()) {
                    paths.add(img.getImagePath());
                }
            }
        }
        if (paths.isEmpty() && book.getImagePath() != null && !book.getImagePath().isBlank()) {
            paths.add(book.getImagePath());
        }
        return paths;
    }

    @Transactional
    public void addImages(Book book, MultipartFile[] files) throws IOException {
        if (book == null || book.getId() == null || files == null || files.length == 0) {
            return;
        }

        List<BookImage> existing = getBookImages(book.getId());
        int nextOrder = existing.stream()
                .map(BookImage::getSortOrder)
                .filter(o -> o != null)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
        int remaining = MAX_IMAGES_PER_BOOK - existing.size();
        if (remaining <= 0) {
            throw new IOException("Mỗi sách tối đa " + MAX_IMAGES_PER_BOOK + " ảnh.");
        }

        boolean hasCover = book.getImagePath() != null && !book.getImagePath().isBlank();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (remaining <= 0) {
                break;
            }
            String path = saveBookImage(file);
            BookImage image = new BookImage();
            image.setBook(book);
            image.setImagePath(path);
            image.setSortOrder(nextOrder++);
            bookImageRepository.save(image);
            remaining--;

            if (!hasCover) {
                book.setImagePath(path);
                bookRepository.save(book);
                hasCover = true;
            }
        }
    }

    /** Nếu sách chỉ có imagePath cũ, đồng bộ vào bảng gallery để có nút xóa/đặt bìa. */
    @Transactional
    public void ensureCoverSyncedToGallery(Book book) {
        if (book == null || book.getId() == null) {
            return;
        }
        if (book.getImagePath() == null || book.getImagePath().isBlank()) {
            return;
        }
        List<BookImage> existing = getBookImages(book.getId());
        boolean already = existing.stream()
                .anyMatch(img -> book.getImagePath().equals(img.getImagePath()));
        if (already) {
            return;
        }
        BookImage image = new BookImage();
        image.setBook(book);
        image.setImagePath(book.getImagePath());
        image.setSortOrder(0);
        bookImageRepository.save(image);
    }

    @Transactional
    public void deleteBookImage(Integer bookId, Integer imageId) {
        if (bookId == null || imageId == null) {
            return;
        }
        BookImage image = bookImageRepository.findByIdAndBook_Id(imageId, bookId).orElse(null);
        if (image == null) {
            return;
        }
        String removedPath = image.getImagePath();
        bookImageRepository.delete(image);
        bookImageRepository.flush();

        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return;
        }

        List<BookImage> remaining = getBookImages(bookId);
        if (book.getImagePath() != null && book.getImagePath().equals(removedPath)) {
            book.setImagePath(remaining.isEmpty() ? null : remaining.get(0).getImagePath());
            bookRepository.save(book);
        } else if ((book.getImagePath() == null || book.getImagePath().isBlank()) && !remaining.isEmpty()) {
            book.setImagePath(remaining.get(0).getImagePath());
            bookRepository.save(book);
        }
    }

    @Transactional
    public void setCoverImage(Integer bookId, Integer imageId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        BookImage image = bookImageRepository.findByIdAndBook_Id(imageId, bookId).orElse(null);
        if (book == null || image == null) {
            return;
        }
        book.setImagePath(image.getImagePath());
        bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Integer id) {
        bookImageRepository.deleteByBook_Id(id);
        bookRepository.deleteById(id);
    }
}

package com.example.bookstore.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class ImageStorage {

    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private ImageStorage() {
    }

    /**
     * Lưu ảnh vào thư mục và trả về URL công khai, ví dụ {@code /uploads/books/xxx.jpg}.
     */
    public static String save(MultipartFile file, Path uploadDir, String urlPrefix) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String lower = original.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        String ext = dot >= 0 ? lower.substring(dot) : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IOException("Chỉ chấp nhận ảnh JPG, PNG, GIF, WEBP.");
        }
        Files.createDirectories(uploadDir);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Files.copy(file.getInputStream(), uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        String prefix = urlPrefix.endsWith("/") ? urlPrefix : urlPrefix + "/";
        return prefix + filename;
    }
}

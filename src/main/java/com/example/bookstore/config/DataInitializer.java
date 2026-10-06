package com.example.bookstore.config;

import com.example.bookstore.entity.DiscountCode;
import com.example.bookstore.entity.User;
import com.example.bookstore.model.StaffPermissions;
import com.example.bookstore.model.UserRoles;
import com.example.bookstore.repository.DiscountCodeRepository;
import com.example.bookstore.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDefaultAccounts(UserRepository userRepository,
                                          DiscountCodeRepository discountCodeRepository) {
        return args -> {
            if (userRepository.findByUsername("admin") == null) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword("admin123");
                admin.setFullName("Quản trị viên");
                admin.setEmail("admin@bookstore.local");
                admin.setRole(UserRoles.ADMIN);
                userRepository.save(admin);
            }
            if (userRepository.findByUsername("staff") == null) {
                User staff = new User();
                staff.setUsername("staff");
                staff.setPassword("staff123");
                staff.setFullName("Nhân viên bán hàng");
                staff.setEmail("staff@bookstore.local");
                staff.setRole(UserRoles.STAFF);
                staff.setPermissions(StaffPermissions.join(StaffPermissions.defaultAll()));
                userRepository.save(staff);
            } else {
                User staff = userRepository.findByUsername("staff");
                if (staff.getPermissions() == null || staff.getPermissions().isBlank()) {
                    staff.setPermissions(StaffPermissions.join(StaffPermissions.defaultAll()));
                    userRepository.save(staff);
                }
            }

            if (discountCodeRepository.findByCodeIgnoreCase("SALE10") == null) {
                DiscountCode sale = new DiscountCode();
                sale.setCode("SALE10");
                sale.setPercentOff(10);
                sale.setDescription("Giảm 10% toàn đơn");
                sale.setMaxUses(100);
                sale.setUsedCount(0);
                sale.setActive(true);
                discountCodeRepository.save(sale);
            }
        };
    }
}

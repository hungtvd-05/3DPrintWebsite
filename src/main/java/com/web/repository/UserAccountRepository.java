package com.web.repository;

import com.web.model.User;
import com.web.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    
    UserAccount findByEmail(String email);

    UserAccount findFirstByRole(String role);
    
    // Nếu cần tìm User thông qua UserAccount, phải dùng join query
    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    User findUserByUserId(@Param("userId") Long userId);
    
    // Hoặc tạo query method riêng nếu cần
    @Query("SELECT u FROM User u JOIN u.userAccount ua WHERE ua.email = :email")
    User findUserByUserAccountEmail(@Param("email") String email);

    UserAccount findByUserId(Long id);
}
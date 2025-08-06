package com.web.repository;

import com.web.model.User;
import com.web.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    
    UserAccount findByEmail(String email);

    UserAccount findFirstByRole(String role);

    UserAccount findByUserId(Long id);

    List<UserAccount> findAllByRole(String role);

    @Query("SELECT ua FROM UserAccount ua WHERE ua.role = 'ROLE_USER' AND (ua.email LIKE %:search% OR ua.fullName LIKE %:search% OR ua.phoneNumber LIKE %:search%)")
    Page<UserAccount> searchUserAccount(Pageable pageable, @Param("search") String search);


    Page<UserAccount> findAllByRole(String role, Pageable pageable);

    @Query("SELECT ua FROM UserAccount ua WHERE ua.role = 'ROLE_ADMIN' AND (ua.email LIKE %:search% OR ua.fullName LIKE %:search% OR ua.phoneNumber LIKE %:search%)")
    List<UserAccount> searchAllAdminAccount(@Param("search") String search);
}
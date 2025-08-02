package com.web.repository;

import com.web.model.User;
import com.web.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmailAndUserAccount_Confirmed(String email, Boolean userAccountConfirmed);
    User findByConfirmToken(String confirmToken);
    User findByEmail(String email);
    User findByResetToken(String token);
}

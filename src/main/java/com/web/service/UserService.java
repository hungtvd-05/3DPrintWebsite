package com.web.service;

import com.web.model.Product;
import com.web.model.User;
import com.web.model.UserAccount;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

public interface UserService {
    void increaseFailedAttempt(User user);
    void userAccountLock(User user);
    boolean unlockAccountTimeExpired(User user);
    Boolean existsEmail(String email);
    User addUser(User user, UserAccount userAccount);
    User addAdmin(User user, UserAccount userAccount);
    User confirmEmail(String confirmToken);
    void updateUserResetToken(User user, String resetToken);
    User findByEmail(String email);
    User getUserByToken(String token);
    User updateUser(User user);
    User getUserById(Long id);
    UserAccount updateUserAccount(UserAccount userAccount);
    UserAccount getAdminAccount();
    UserAccount getCurrentUserAccount();
    UserAccount getUserAccountById(Long id);
    UserAccount getUserAccountByEmail(String email);
    List<UserAccount> getAllAdminAccount();
    List<UserAccount> searchAllAdminAccount(String search);
    Page<UserAccount> getAllUsersPage(Integer pageNumber, Integer pageSize, String search);
    Set<String> searchAllUsers();
    Set<String> searchAllAdmin(UserAccount currentAdmin);
    Boolean deleteAdmin(User admin);
}

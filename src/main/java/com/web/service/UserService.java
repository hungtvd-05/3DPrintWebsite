package com.web.service;

import com.web.model.Product;
import com.web.model.User;
import com.web.model.UserAccount;

public interface UserService {
    void increaseFailedAttempt(User user);
    void userAccountLock(User user);
    boolean unlockAccountTimeExpired(User user);
//    UserDTO getUserDTOByEmail(String email);
    Boolean existsEmail(String email);
    User addUser(User user, UserAccount userAccount);
    void updateConfirmEmailToken(String email, String confirmToken);
    User confirmEmail(String confirmToken);
    void updateUserResetToken(User user, String resetToken);
    User findByEmail(String email);
    User getUserByToken(String token);
    User updateUser(User user);
    UserAccount updateUserAccount(UserAccount userAccount);
    UserAccount getAdminAccount();
    UserAccount getCurrentUserAccount();
    UserAccount getUserAccountById(Long id);
    UserAccount getUserAccountByEmail(String email);
}

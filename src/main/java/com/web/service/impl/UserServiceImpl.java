package com.web.service.impl;

import com.web.model.Product;
import com.web.model.User;
import com.web.model.UserAccount;
import com.web.repository.UserAccountRepository;
import com.web.repository.UserRepository;
import com.web.service.UserService;
import com.web.util.AppConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Override
    @Transactional
    public void increaseFailedAttempt(User user) {
        int attempt = user.getFailedAttempt() + 1;
        user.setFailedAttempt(attempt);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void userAccountLock(User user) {
        user.setAccountNonLocked(false);
        user.setLockTime(new Date());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public boolean unlockAccountTimeExpired(User user) {
        long lockTime = user.getLockTime().getTime();
        long unlockTime = lockTime + AppConstant.UNLOCK_DURATION_TIME;

        long currentTime = System.currentTimeMillis();

        if (currentTime > unlockTime) {
            user.setAccountNonLocked(true);
            user.setFailedAttempt(0);
            user.setLockTime(null);
            userRepository.save(user);
            return true;
        }

        return false;
    }

//    @Override
//    public UserDTO getUserDTOByEmail(String email) {
//        return userRepository.getUserDTOByEmail(email);
//    }

    @Override
    public Boolean existsEmail(String email) {
        User user = userRepository.findByEmail(email);

        if (ObjectUtils.isEmpty(user)) {
            return false;
        } else if (!user.getUserAccount().getConfirmed()) {
            userRepository.delete(user);
            return false;
        }
        return true;
    }

    @Override
    @Transactional
    public User addUser(User user, UserAccount userAccount) {

        userAccount.setProfileImage("default.png");
        userAccount.setRole("ROLE_USER");
        userAccount.setIsEnable(true);
        userAccount.setConfirmed(false);
        UserAccount savedUserAccount = userAccountRepository.save(userAccount);

        user.setUserId(savedUserAccount.getUserId());
        user.setAccountNonLocked(true);
        user.setFailedAttempt(0);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateConfirmEmailToken(String email, String confirmToken) {
        User user = userRepository.findByEmailAndUserAccount_Confirmed(email, false);
        if (!ObjectUtils.isEmpty(user)) {
            user.setConfirmToken(confirmToken);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public User confirmEmail(String confirmToken) {
        User user = userRepository.findByConfirmToken(confirmToken);
        if (ObjectUtils.isEmpty(user)) {
            return null;
        }
        user.setConfirmToken(null);
        user.getUserAccount().setConfirmed(true);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserResetToken(User user, String resetToken) {
        user.setResetToken(resetToken);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByToken(String token) {
        return userRepository.findByResetToken(token);
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public UserAccount updateUserAccount(UserAccount userAccount) {
        return userAccountRepository.save(userAccount);
    }

    @Override
    public UserAccount getAdminAccount() {
        return userAccountRepository.findFirstByRole("ROLE_ADMIN");
    }

    @Override
    public UserAccount getCurrentUserAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        return userAccountRepository.findByEmail(email);
    }

    @Override
    public UserAccount getUserAccountById(Long id) {
        return userAccountRepository.findByUserId(id);
    }

    @Override
    public UserAccount getUserAccountByEmail(String email) {
        return userAccountRepository.findByEmail(email);
    }

}

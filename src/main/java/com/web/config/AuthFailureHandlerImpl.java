package com.web.config;

import com.web.model.User;
import com.web.repository.UserRepository;
import com.web.service.UserService;
import com.web.util.AppConstant;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class AuthFailureHandlerImpl extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        String  email = request.getParameter("username");

        User user = userRepository.findByEmailAndUserAccount_Confirmed(email, true);

        if (user != null) {
            if (user.getUserAccount().getIsEnable()) {

                if (user.getAccountNonLocked()) {

                    if (user.getFailedAttempt() < AppConstant.ATTEMPT_TIME) {
                        exception = new LockedException("Tài khoản hoặc mật khẩu không đúng!");
                        userService.increaseFailedAttempt(user);
                    } else {
                        userService.userAccountLock(user);
                        exception = new LockedException("Tài khoản của bạn đã bị khóa! || Đăng nhập thất bại!");
                    }

                } else {

                    if (userService.unlockAccountTimeExpired(user)) {
                        exception = new LockedException("Tài khoản của bạn đã mở khóa! || Hãy thử đăng nhập lại!");
                    } else {
                        exception = new LockedException("Tài khoản của bạn đã bị khóa! || Hãy đăng nhập lại sau!");
                    }
                }

            } else {
                exception = new LockedException("Tài khoản của bạn không hoạt động!");
            }
        } else {
            exception = new LockedException("Tài khoản không tồn tại!");
        }

        super.setDefaultFailureUrl("/signin?error");
        super.onAuthenticationFailure(request, response, exception);
    }
}

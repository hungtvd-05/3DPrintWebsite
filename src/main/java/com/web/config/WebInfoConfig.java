package com.web.config;

import com.web.model.UserAccount;
import com.web.model.WebInfo;
import com.web.service.UserService;
import com.web.service.WebInfoService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@Getter
public class WebInfoConfig {

    @Autowired
    private WebInfoService webInfoService;

    @Autowired
    private UserService userService;

    private WebInfo cachedWebInfo;

    private List<UserAccount> cachedAdminAccounts;

    @PostConstruct
    public void initWebInfo() {
        refreshWebInfo();
    }

    // Refresh cache mỗi 30 phút
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void refreshWebInfo() {
        try {
            WebInfo webInfo = webInfoService.getWebInfo();
            this.cachedWebInfo = (webInfo != null) ? webInfo : new WebInfo();
            System.out.println(webInfo.getEmail());
            System.out.println("✅ WebInfo cache refreshed");
            this.cachedAdminAccounts = userService.getAllAdminAccount();
            System.out.println("✅ User accounts cache refreshed");
        } catch (Exception e) {
            System.err.println("❌ Error refreshing WebInfo cache: " + e.getMessage());
            if (this.cachedWebInfo == null) {
                this.cachedWebInfo = new WebInfo();
            }
            if (this.cachedAdminAccounts == null) {
                this.cachedAdminAccounts = null;
            }
        }
    }

    public WebInfo getWebInfo() {
        return this.cachedWebInfo != null ? this.cachedWebInfo : new WebInfo();
    }

    public List<UserAccount> getAdminAccounts() {
        return this.cachedAdminAccounts != null ? this.cachedAdminAccounts : List.of();
    }

    // Method để force refresh cache khi cần
    public void forceRefresh() {
        refreshWebInfo();
    }
}
package com.yunrui.edu.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserAccessGuard {
    public void require(long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !Long.toString(userId).equals(authentication.getName())) {
            throw new IllegalStateException("不能访问其他用户的数据");
        }
    }
}

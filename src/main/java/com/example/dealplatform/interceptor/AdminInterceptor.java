package com.example.dealplatform.interceptor;

import com.example.dealplatform.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        User user = (User) request.getSession().getAttribute("loginUser");
        if (user == null || !"admin".equals(user.getRoleCode())) {
            response.sendRedirect("/forbidden");
            return false;
        }
        return true;
    }
}

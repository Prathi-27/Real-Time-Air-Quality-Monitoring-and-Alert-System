package com.airquality.servlet;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * AuthenticationFilter intercepts requests to protected resources (air.html, history.html).
 * If the user does not possess a valid HttpSession with "userId", redirects to login.html.
 */
public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization if needed
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Check if an existing session exists
        HttpSession session = request.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("userId") != null);

        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();

        // Prevent browser caching on protected pages so the back button doesn't reveal data after logout
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (isLoggedIn) {
            // User is authenticated, proceed normally
            chain.doFilter(req, res);
        } else {
            // User is not logged in, redirect to login page
            response.sendRedirect(contextPath + "/login.html?session_expired=true");
        }
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}

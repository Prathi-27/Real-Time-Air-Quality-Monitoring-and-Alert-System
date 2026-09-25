package com.airquality.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * LogoutServlet terminates the current user session and redirects to login.html.
 */
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    private void processLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Fetch current session without creating a new one
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.removeAttribute("userId");
            session.removeAttribute("username");
            session.removeAttribute("fullName");
            session.invalidate(); // Destroy session
        }

        // Prevent browser caching of protected pages after logout
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setDateHeader("Expires", 0); // Proxies

        // Redirect user to login page
        response.sendRedirect(request.getContextPath() + "/login.html?logged_out=true");
    }
}

package com.airquality.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * CheckSessionServlet returns session authentication status and user profile
 * info as JSON. Used by air.html and history.html on initial page load.
 */
public class CheckSessionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("userId") != null) {
            int userId = (Integer) session.getAttribute("userId");
            String username = (String) session.getAttribute("username");
            String fullName = (String) session.getAttribute("fullName");

            out.write(String.format(
                "{\"authenticated\":true,\"userId\":%d,\"username\":\"%s\",\"fullName\":\"%s\"}",
                userId,
                escapeJson(username != null ? username : ""),
                escapeJson(fullName != null ? fullName : "")
            ));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"authenticated\":false,\"message\":\"No active session\"}");
        }
        out.flush();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

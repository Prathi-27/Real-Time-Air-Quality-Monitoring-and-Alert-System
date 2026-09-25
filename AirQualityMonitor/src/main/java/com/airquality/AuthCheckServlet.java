package com.airquality;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * AuthCheckServlet provides a server-side session check endpoint.
 * Called immediately upon loading air.html or history.html.
 * If the user is unauthenticated, returns HTTP 401 or redirects to login.html.
 */
@WebServlet("/AuthCheckServlet")
public class AuthCheckServlet extends HttpServlet {

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

            response.setStatus(HttpServletResponse.SC_OK);
            out.write(String.format(
                "{\"authenticated\":true,\"userId\":%d,\"username\":\"%s\",\"fullName\":\"%s\"}",
                userId,
                escapeJson(username != null ? username : ""),
                escapeJson(fullName != null ? fullName : "")
            ));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"authenticated\":false,\"redirect\":\"login.html?session_expired=true\"}");
        }
        out.flush();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

package com.airquality.servlet;

import com.airquality.util.DBConnection;
import com.airquality.util.PasswordUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * LoginServlet handles user authentication.
 * Validates credentials via JDBC, initializes HttpSession, and redirects to air.html.
 */
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        // Can accept username or email in the login field
        String loginIdentifier = request.getParameter("username");
        String password = request.getParameter("password");

        // Server-side validation
        if (loginIdentifier == null || loginIdentifier.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            handleFailure(request, response, "Username/Email and Password are required.");
            return;
        }

        loginIdentifier = loginIdentifier.trim();
        password = password.trim();

        // Query user by username OR email using PreparedStatement (SQL Injection prevention)
        String sql = "SELECT id, full_name, username, email, password FROM users WHERE username = ? OR email = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, loginIdentifier);
            ps.setString(2, loginIdentifier);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String fullName = rs.getString("full_name");
                    String username = rs.getString("username");
                    String storedPasswordHash = rs.getString("password");

                    // Verify hashed password
                    if (PasswordUtil.verifyPassword(password, storedPasswordHash)) {
                        // Invalidate existing session if present to prevent session fixation attacks
                        HttpSession oldSession = request.getSession(false);
                        if (oldSession != null) {
                            oldSession.invalidate();
                        }

                        // Create new HTTP Session
                        HttpSession session = request.getSession(true);
                        session.setAttribute("userId", userId);
                        session.setAttribute("username", username);
                        session.setAttribute("fullName", fullName);
                        // Optional: session timeout of 30 minutes (1800 seconds)
                        session.setMaxInactiveInterval(1800);

                        // Check if client expects JSON or standard form redirection
                        String acceptHeader = request.getHeader("Accept");
                        String requestedWith = request.getHeader("X-Requested-With");
                        if ((acceptHeader != null && acceptHeader.contains("application/json")) ||
                            "XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                            response.setContentType("application/json;charset=UTF-8");
                            PrintWriter out = response.getWriter();
                            out.write("{\"status\":\"success\",\"message\":\"Login successful\",\"redirect\":\"air.html\"}");
                            out.flush();
                        } else {
                            response.sendRedirect(request.getContextPath() + "/air.html");
                        }
                        return;
                    }
                }
            }

            // If user not found or password incorrect
            handleFailure(request, response, "Invalid username/email or password.");

        } catch (SQLException e) {
            e.printStackTrace();
            handleFailure(request, response, "Database error occurred during login. Please try again.");
        }
    }

    private void handleFailure(HttpServletRequest request, HttpServletResponse response, String errorMessage)
            throws IOException {
        String acceptHeader = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        if ((acceptHeader != null && acceptHeader.contains("application/json")) ||
            "XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write("{\"status\":\"error\",\"message\":\"" + escapeJson(errorMessage) + "\"}");
            out.flush();
        } else {
            response.sendRedirect(request.getContextPath() + "/login.html?error=" +
                    java.net.URLEncoder.encode(errorMessage, "UTF-8"));
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/login.html");
    }
}

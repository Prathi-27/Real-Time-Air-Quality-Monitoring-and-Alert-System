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

/**
 * RegisterServlet handles new user account creation.
 * Checks for empty fields, password matching, duplicate username, and duplicate email.
 * Stores salted SHA-256 hashed password in MySQL.
 */
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String fullName = request.getParameter("full_name");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirm_password");

        // 1. Check for empty fields
        if (fullName == null || fullName.trim().isEmpty() ||
            username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            confirmPassword == null || confirmPassword.trim().isEmpty()) {

            handleFailure(request, response, "All fields are required.");
            return;
        }

        fullName = fullName.trim();
        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();

        // 2. Validate password match
        if (!password.equals(confirmPassword)) {
            handleFailure(request, response, "Passwords do not match.");
            return;
        }

        // Basic password length policy
        if (password.length() < 6) {
            handleFailure(request, response, "Password must be at least 6 characters long.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            // 3. Check for duplicate username
            String checkUserSql = "SELECT id FROM users WHERE username = ? LIMIT 1";
            try (PreparedStatement psUser = conn.prepareStatement(checkUserSql)) {
                psUser.setString(1, username);
                try (ResultSet rs = psUser.executeQuery()) {
                    if (rs.next()) {
                        handleFailure(request, response, "Username is already taken. Please choose another.");
                        return;
                    }
                }
            }

            // 4. Check for duplicate email
            String checkEmailSql = "SELECT id FROM users WHERE email = ? LIMIT 1";
            try (PreparedStatement psEmail = conn.prepareStatement(checkEmailSql)) {
                psEmail.setString(1, email);
                try (ResultSet rs = psEmail.executeQuery()) {
                    if (rs.next()) {
                        handleFailure(request, response, "Email is already registered. Please sign in or use another.");
                        return;
                    }
                }
            }

            // 5. Hash password (Salting + Hashing)
            String hashedPassword = PasswordUtil.hashPassword(password);

            // 6. Insert into MySQL users table
            String insertSql = "INSERT INTO users (full_name, username, email, password) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                insertPs.setString(1, fullName);
                insertPs.setString(2, username);
                insertPs.setString(3, email);
                insertPs.setString(4, hashedPassword);

                int rowsAffected = insertPs.executeUpdate();
                if (rowsAffected > 0) {
                    // Success response (AJAX or Redirect)
                    String acceptHeader = request.getHeader("Accept");
                    String requestedWith = request.getHeader("X-Requested-With");
                    if ((acceptHeader != null && acceptHeader.contains("application/json")) ||
                        "XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                        response.setContentType("application/json;charset=UTF-8");
                        PrintWriter out = response.getWriter();
                        out.write("{\"status\":\"success\",\"message\":\"Registration successful! Please log in.\",\"redirect\":\"login.html?registered=true\"}");
                        out.flush();
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login.html?registered=true");
                    }
                    return;
                } else {
                    handleFailure(request, response, "Registration failed. Please try again.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            handleFailure(request, response, "Database error during registration: " + e.getMessage());
        }
    }

    private void handleFailure(HttpServletRequest request, HttpServletResponse response, String errorMessage)
            throws IOException {
        String acceptHeader = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        if ((acceptHeader != null && acceptHeader.contains("application/json")) ||
            "XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write("{\"status\":\"error\",\"message\":\"" + escapeJson(errorMessage) + "\"}");
            out.flush();
        } else {
            response.sendRedirect(request.getContextPath() + "/register.html?error=" +
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
        response.sendRedirect(request.getContextPath() + "/register.html");
    }
}

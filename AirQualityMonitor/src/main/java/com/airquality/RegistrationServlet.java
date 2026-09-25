package com.airquality;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * RegistrationServlet handles new user registration.
 * Validates full name, email format, password match and length,
 * checks for duplicate email, hashes the password with BCrypt,
 * and inserts the record into MySQL air_quality_db.users table.
 */
@WebServlet({"/RegistrationServlet", "/RegisterServlet"})
public class RegistrationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        Map<String, String> params = extractParameters(request);
        String fullName = params.get("full_name");
        String email = params.get("email");
        String password = params.get("password");
        String confirmPassword = params.get("confirm_password");

        // 1. Validate non-empty fields
        if (fullName == null || fullName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            confirmPassword == null || confirmPassword.trim().isEmpty()) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"All fields are required.\"}");
            out.flush();
            return;
        }

        fullName = fullName.trim();
        email = email.trim().toLowerCase();

        // 2. Validate email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Please enter a valid email address.\"}");
            out.flush();
            return;
        }

        // 3. Confirm password match
        if (!password.equals(confirmPassword)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Passwords do not match. Please verify.\"}");
            out.flush();
            return;
        }

        // 4. Password minimum length
        if (password.length() < 6) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Password must be at least 6 characters long.\"}");
            out.flush();
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            // 5. Check duplicate email in MySQL users table
            String checkEmailSql = "SELECT id FROM users WHERE email = ? LIMIT 1";
            try (PreparedStatement psEmail = conn.prepareStatement(checkEmailSql)) {
                psEmail.setString(1, email);
                try (ResultSet rs = psEmail.executeQuery()) {
                    if (rs.next()) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        out.write("{\"status\":\"error\",\"message\":\"This email is already registered. Please sign in.\"}");
                        out.flush();
                        return;
                    }
                }
            }

            // 6. Securely hash password with BCrypt (12 rounds)
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

            // 7. Insert into MySQL users table
            String insertSql = "INSERT INTO users (full_name, email, password) VALUES (?, ?, ?)";
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psInsert.setString(1, fullName);
                psInsert.setString(2, email);
                psInsert.setString(3, hashedPassword);

                int rowsAffected = psInsert.executeUpdate();
                if (rowsAffected > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.write("{\"status\":\"success\",\"message\":\"Account created successfully! Redirecting to sign in...\",\"redirect\":\"login.html?registered=true\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.write("{\"status\":\"error\",\"message\":\"Failed to create account. Please try again.\"}");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            out.flush();
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private Map<String, String> extractParameters(HttpServletRequest request) throws IOException {
        Map<String, String> map = new HashMap<>();
        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            String json = sb.toString().trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                String content = json.substring(1, json.length() - 1);
                String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        map.put(kv[0].trim().replace("\"", ""), kv[1].trim().replace("\"", ""));
                    }
                }
            }
        } else {
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                if (entry.getValue() != null && entry.getValue().length > 0) {
                    map.put(entry.getKey(), entry.getValue()[0]);
                }
            }
        }
        return map;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("register.html");
    }
}

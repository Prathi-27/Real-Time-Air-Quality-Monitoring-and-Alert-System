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
 * RegisterServlet handles user account creation.
 * Checks empty fields, email format, password confirmation, duplicate username & email.
 * Applies BCrypt password hashing before inserting into MySQL.
 */
@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        Map<String, String> params = extractParameters(request);
        String fullName = params.get("full_name");
        String username = params.get("username");
        String email = params.get("email");
        String password = params.get("password");
        String confirmPassword = params.get("confirm_password");

        // 1. Check for empty fields
        if (fullName == null || fullName.trim().isEmpty() ||
            username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            confirmPassword == null || confirmPassword.trim().isEmpty()) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"All fields are required.\"}");
            out.flush();
            return;
        }

        fullName = fullName.trim();
        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();

        // 2. Validate email format
        if (!EMAIL_REGEX.matcher(email).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Please enter a valid email address.\"}");
            out.flush();
            return;
        }

        // 3. Check password confirmation
        if (!password.equals(confirmPassword)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Passwords do not match.\"}");
            out.flush();
            return;
        }

        if (password.length() < 6) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Password must be at least 6 characters long.\"}");
            out.flush();
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            // 4. Check for duplicate username
            String checkUserSql = "SELECT id FROM users WHERE username = ? LIMIT 1";
            try (PreparedStatement psUser = conn.prepareStatement(checkUserSql)) {
                psUser.setString(1, username);
                try (ResultSet rs = psUser.executeQuery()) {
                    if (rs.next()) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        out.write("{\"status\":\"error\",\"message\":\"Username is already taken. Please choose another.\"}");
                        out.flush();
                        return;
                    }
                }
            }

            // 5. Check for duplicate email
            String checkEmailSql = "SELECT id FROM users WHERE email = ? LIMIT 1";
            try (PreparedStatement psEmail = conn.prepareStatement(checkEmailSql)) {
                psEmail.setString(1, email);
                try (ResultSet rs = psEmail.executeQuery()) {
                    if (rs.next()) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        out.write("{\"status\":\"error\",\"message\":\"Email is already registered. Please sign in or use another.\"}");
                        out.flush();
                        return;
                    }
                }
            }

            // 6. BCrypt Password Hashing (Cost factor 12)
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

            // 7. Insert new user into MySQL
            String insertSql = "INSERT INTO users (full_name, username, email, password) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psInsert.setString(1, fullName);
                psInsert.setString(2, username);
                psInsert.setString(3, email);
                psInsert.setString(4, hashedPassword);

                int rowsAffected = psInsert.executeUpdate();
                if (rowsAffected > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.write("{\"status\":\"success\",\"message\":\"Registration successful! Please sign in.\",\"redirect\":\"login.html?registered=true\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.write("{\"status\":\"error\",\"message\":\"Registration failed. Please try again.\"}");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error during registration: " + escapeJson(e.getMessage()) + "\"}");
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
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String json = sb.toString().trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                String content = json.substring(1, json.length() - 1);
                String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String k = kv[0].trim().replace("\"", "");
                        String v = kv[1].trim().replace("\"", "");
                        map.put(k, v);
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

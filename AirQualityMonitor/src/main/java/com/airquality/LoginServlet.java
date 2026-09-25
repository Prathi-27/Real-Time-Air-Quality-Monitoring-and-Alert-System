package com.airquality;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

/**
 * LoginServlet handles user authentication using Email Address & Password.
 * PreparedStatement prevents SQL injection.
 * BCrypt securely checks password against stored hash.
 * Creates an active HttpSession on success with userId and fullName.
 */
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        Map<String, String> params = extractParameters(request);
        String email = params.get("email");
        if (email == null || email.trim().isEmpty()) {
            email = params.get("username"); // fallback support if submitted as username field
        }
        String password = params.get("password");

        // 1. Validation check
        if (email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Please enter both email and password.\"}");
            out.flush();
            return;
        }

        email = email.trim().toLowerCase();
        password = password.trim();

        // 2. Query user by email using PreparedStatement
        String sql = "SELECT id, full_name, email, password FROM users WHERE email = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String fullName = rs.getString("full_name");
                    String storedHash = rs.getString("password");

                    // 3. Verify BCrypt password
                    if (BCrypt.checkpw(password, storedHash)) {
                        // Invalidate old session to prevent fixation
                        HttpSession oldSession = request.getSession(false);
                        if (oldSession != null) {
                            oldSession.invalidate();
                        }

                        // Create fresh HttpSession
                        HttpSession session = request.getSession(true);
                        session.setAttribute("userId", userId);
                        session.setAttribute("email", email);
                        session.setAttribute("fullName", fullName);
                        session.setMaxInactiveInterval(1800); // 30-minute session timeout

                        response.setStatus(HttpServletResponse.SC_OK);
                        out.write("{\"status\":\"success\",\"message\":\"Login successful!\",\"redirect\":\"air.html\"}");
                        out.flush();
                        return;
                    }
                }
            }

            // Authentication failure - Exact required message
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"status\":\"error\",\"message\":\"Invalid email or password.\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error occurred. Please verify MySQL is running.\"}");
        } finally {
            out.flush();
        }
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
        response.sendRedirect("login.html");
    }
}

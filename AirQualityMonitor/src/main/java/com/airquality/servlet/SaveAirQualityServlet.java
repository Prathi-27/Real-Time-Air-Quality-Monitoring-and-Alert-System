package com.airquality.servlet;

import com.airquality.util.DBConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * SaveAirQualityServlet records air quality metrics into MySQL.
 * Strictly derives the user_id from the authenticated HttpSession.
 * Supports both JSON payloads and Form-URL-Encoded requests.
 */
public class SaveAirQualityServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 1. Enforce active user session - NEVER trust user ID from client
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"status\":\"error\",\"message\":\"Unauthorized: Please log in first.\"}");
            out.flush();
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        // 2. Read parameters (handles both standard POST params and raw JSON payload)
        Map<String, String> data = extractParameters(request);

        String city = data.get("city");
        String latStr = data.get("latitude");
        String lonStr = data.get("longitude");
        String aqiStr = data.get("aqi");
        String coStr = data.get("co");
        String noStr = data.get("no");
        String no2Str = data.get("no2");
        String o3Str = data.get("o3");
        String so2Str = data.get("so2");
        String pm25Str = data.get("pm2_5");
        if (pm25Str == null) pm25Str = data.get("pm25"); // alternate key fallback
        String pm10Str = data.get("pm10");
        String nh3Str = data.get("nh3");

        if (city == null || city.trim().isEmpty() || aqiStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Missing required air quality data fields.\"}");
            out.flush();
            return;
        }

        try {
            double latitude = parseDoubleSafe(latStr);
            double longitude = parseDoubleSafe(lonStr);
            int aqi = Integer.parseInt(aqiStr.trim());
            double co = parseDoubleSafe(coStr);
            double no = parseDoubleSafe(noStr);
            double no2 = parseDoubleSafe(no2Str);
            double o3 = parseDoubleSafe(o3Str);
            double so2 = parseDoubleSafe(so2Str);
            double pm25 = parseDoubleSafe(pm25Str);
            double pm10 = parseDoubleSafe(pm10Str);
            double nh3 = parseDoubleSafe(nh3Str);

            // 3. Insert into MySQL air_quality_history table
            String sql = "INSERT INTO air_quality_history " +
                         "(user_id, city, latitude, longitude, aqi, co, no, no2, o3, so2, pm2_5, pm10, nh3) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, userId);
                ps.setString(2, city.trim());
                ps.setDouble(3, latitude);
                ps.setDouble(4, longitude);
                ps.setInt(5, aqi);
                ps.setDouble(6, co);
                ps.setDouble(7, no);
                ps.setDouble(8, no2);
                ps.setDouble(9, o3);
                ps.setDouble(10, so2);
                ps.setDouble(11, pm25);
                ps.setDouble(12, pm10);
                ps.setDouble(13, nh3);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.write("{\"status\":\"success\",\"message\":\"Air quality data saved to history.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.write("{\"status\":\"error\",\"message\":\"Failed to save air quality data.\"}");
                }
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Invalid numeric values provided in payload.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            out.flush();
        }
    }

    private double parseDoubleSafe(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    /**
     * Extracts parameters from either application/x-www-form-urlencoded
     * or application/json request body without external libraries.
     */
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
            // Lightweight JSON key-value extraction for Core Java
            if (json.startsWith("{") && json.endsWith("}")) {
                String trimmed = json.substring(1, json.length() - 1);
                String[] pairs = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        map.put(key, value);
                    }
                }
            }
        } else {
            // Standard form parameter retrieval
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                if (entry.getValue() != null && entry.getValue().length > 0) {
                    map.put(entry.getKey(), entry.getValue()[0]);
                }
            }
        }
        return map;
    }
}

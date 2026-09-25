package com.airquality;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * SaveAirQualityServlet saves air pollution metrics into MySQL.
 * Strictly derives the user_id from the authenticated HttpSession.
 * Frontend is never allowed to specify userId.
 */
@WebServlet("/SaveAirQualityServlet")
public class SaveAirQualityServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 1. Get existing session & check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"status\":\"error\",\"message\":\"Unauthorized: Please sign in first.\"}");
            out.flush();
            return;
        }

        // 2. Obtain userId strictly from session
        int userId = (Integer) session.getAttribute("userId");

        // 3. Read air-quality data from request payload
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
        if (pm25Str == null) pm25Str = data.get("pm25");
        String pm10Str = data.get("pm10");
        String nh3Str = data.get("nh3");

        // 4. Validate data
        if (city == null || city.trim().isEmpty() || aqiStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Missing required city or AQI information.\"}");
            out.flush();
            return;
        }

        try {
            double latitude = parseDouble(latStr);
            double longitude = parseDouble(lonStr);
            int aqi = Integer.parseInt(aqiStr.trim());
            double co = parseDouble(coStr);
            double no = parseDouble(noStr);
            double no2 = parseDouble(no2Str);
            double o3 = parseDouble(o3Str);
            double so2 = parseDouble(so2Str);
            double pm25 = parseDouble(pm25Str);
            double pm10 = parseDouble(pm10Str);
            double nh3 = parseDouble(nh3Str);

            // 5. Insert into air_quality_history using JDBC PreparedStatement
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
                    out.write("{\"status\":\"success\",\"message\":\"Air quality data saved to MySQL history.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.write("{\"status\":\"error\",\"message\":\"Failed to save history.\"}");
                }
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"status\":\"error\",\"message\":\"Invalid numerical pollutant figures.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            out.flush();
        }
    }

    private double parseDouble(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return 0.0;
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
}

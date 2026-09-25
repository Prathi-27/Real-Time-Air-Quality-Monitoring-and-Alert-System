package com.airquality;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * HistoryServlet retrieves only the logged-in user's previous air quality searches.
 * Enforces strict user isolation via HttpSession.
 */
@WebServlet("/HistoryServlet")
public class HistoryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        PrintWriter out = response.getWriter();

        // 1. Get userId from HttpSession
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"status\":\"error\",\"message\":\"Unauthorized: Please sign in.\",\"data\":[]}");
            out.flush();
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        // 2. Query only that user's history
        String sql = "SELECT id, city, latitude, longitude, aqi, co, no, no2, o3, so2, pm2_5, pm10, nh3, searched_at " +
                     "FROM air_quality_history " +
                     "WHERE user_id = ? " +
                     "ORDER BY searched_at DESC";

        StringBuilder json = new StringBuilder("{\"status\":\"success\",\"data\":[");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) {
                        json.append(",");
                    }
                    first = false;

                    Timestamp searchedAt = rs.getTimestamp("searched_at");
                    String dateFormatted = (searchedAt != null) ? dateFormat.format(searchedAt) : "";

                    json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"city\":\"").append(escapeJson(rs.getString("city"))).append("\",")
                        .append("\"latitude\":").append(rs.getDouble("latitude")).append(",")
                        .append("\"longitude\":").append(rs.getDouble("longitude")).append(",")
                        .append("\"aqi\":").append(rs.getInt("aqi")).append(",")
                        .append("\"co\":").append(rs.getDouble("co")).append(",")
                        .append("\"no\":").append(rs.getDouble("no")).append(",")
                        .append("\"no2\":").append(rs.getDouble("no2")).append(",")
                        .append("\"o3\":").append(rs.getDouble("o3")).append(",")
                        .append("\"so2\":").append(rs.getDouble("so2")).append(",")
                        .append("\"pm2_5\":").append(rs.getDouble("pm2_5")).append(",")
                        .append("\"pm10\":").append(rs.getDouble("pm10")).append(",")
                        .append("\"nh3\":").append(rs.getDouble("nh3")).append(",")
                        .append("\"searched_at\":\"").append(dateFormatted).append("\"")
                        .append("}");
                }
            }

            json.append("]}");
            out.write(json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\",\"data\":[]}");
        } finally {
            out.flush();
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }
}

package com.airquality.servlet;

import com.airquality.util.DBConnection;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * HistoryServlet retrieves the authenticated user's air quality search history from MySQL.
 * Strictly queries only the records belonging to the session's userId.
 * Returns results formatted as JSON.
 */
public class HistoryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        // Disable browser caching for dynamic history results
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        PrintWriter out = response.getWriter();

        // 1. Get userId from HttpSession
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"status\":\"error\",\"message\":\"Unauthorized: Please log in.\",\"data\":[]}");
            out.flush();
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        // 2. Query only that user's records
        String sql = "SELECT id, city, latitude, longitude, aqi, co, no, no2, o3, so2, pm2_5, pm10, nh3, searched_at " +
                     "FROM air_quality_history " +
                     "WHERE user_id = ? " +
                     "ORDER BY searched_at DESC";

        StringBuilder json = new StringBuilder();
        json.append("{\"status\":\"success\",\"data\":[");

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

                    int id = rs.getInt("id");
                    String city = rs.getString("city");
                    double lat = rs.getDouble("latitude");
                    double lon = rs.getDouble("longitude");
                    int aqi = rs.getInt("aqi");
                    double co = rs.getDouble("co");
                    double no = rs.getDouble("no");
                    double no2 = rs.getDouble("no2");
                    double o3 = rs.getDouble("o3");
                    double so2 = rs.getDouble("so2");
                    double pm25 = rs.getDouble("pm2_5");
                    double pm10 = rs.getDouble("pm10");
                    double nh3 = rs.getDouble("nh3");
                    Timestamp searchedAt = rs.getTimestamp("searched_at");
                    String dateFormatted = (searchedAt != null) ? dateFormat.format(searchedAt) : "";

                    json.append("{")
                        .append("\"id\":").append(id).append(",")
                        .append("\"city\":\"").append(escapeJson(city)).append("\",")
                        .append("\"latitude\":").append(lat).append(",")
                        .append("\"longitude\":").append(lon).append(",")
                        .append("\"aqi\":").append(aqi).append(",")
                        .append("\"co\":").append(co).append(",")
                        .append("\"no\":").append(no).append(",")
                        .append("\"no2\":").append(no2).append(",")
                        .append("\"o3\":").append(o3).append(",")
                        .append("\"so2\":").append(so2).append(",")
                        .append("\"pm2_5\":").append(pm25).append(",")
                        .append("\"pm10\":").append(pm10).append(",")
                        .append("\"nh3\":").append(nh3).append(",")
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

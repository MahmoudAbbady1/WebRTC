package com.webrtc.backend.servlet;

import com.webrtc.backend.util.DatabaseUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/changeStatus")
public class ChangeStatusServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Set users offline if last_update is older than 2 minutes
            String sql = "UPDATE users SET online = FALSE WHERE online = TRUE AND last_update < (NOW() - INTERVAL 2 MINUTE)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int affected = stmt.executeUpdate();
                out.print("{\"success\":true,\"affected\":" + affected + "}");
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Database error\"}");
        }
    }
} 
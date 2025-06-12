package com.webrtc.backend.servlet;

import com.webrtc.backend.util.DatabaseUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String sessionId = session.getId();

        // Check sessionId in the database
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT session_id FROM users WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String dbSessionId = rs.getString("session_id");
                    if (dbSessionId == null || !dbSessionId.equals(sessionId)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        out.print("{\"error\":\"Session expired or logged in elsewhere\"}");
                        return;
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.print("{\"error\":\"User not found\"}");
                    return;
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Database error\"}");
            return;
        }

        // If reached here, the session is valid
        out.print("{\"success\":true}");
    }
} 
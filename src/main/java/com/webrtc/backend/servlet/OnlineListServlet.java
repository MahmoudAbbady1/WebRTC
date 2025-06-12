package com.webrtc.backend.servlet;

import com.webrtc.backend.model.User;
import com.webrtc.backend.util.DatabaseUtil;
import com.google.gson.Gson;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/onlineList")
public class OnlineListServlet extends HttpServlet {
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
        List<User> onlineUsers = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT id, username, msisdn, online, last_update, created_at FROM users WHERE online = TRUE AND last_update > (NOW() - INTERVAL 1 MINUTE)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setMsisdn(rs.getString("msisdn"));
                    user.setOnline(rs.getBoolean("online"));
                    user.setLastUpdate(rs.getTimestamp("last_update"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    onlineUsers.add(user);
                }
            }
            Gson gson = new Gson();
            out.print(gson.toJson(onlineUsers));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Database error\"}");
        }
    }
} 
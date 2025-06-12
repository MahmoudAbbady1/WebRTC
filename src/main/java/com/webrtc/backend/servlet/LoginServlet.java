package com.webrtc.backend.servlet;

import com.webrtc.backend.util.DatabaseUtil;
import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;
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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        if (username == null || password == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Missing fields\"}");
            return;
        }
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT id, password_hash FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    int userId = rs.getInt("id");
                    if (BCrypt.checkpw(password, hash)) {
                        // Update user status to online, update last_update and session_id
                        String updateSql = "UPDATE users SET online = TRUE, last_update = CURRENT_TIMESTAMP, session_id = ? WHERE id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, request.getSession().getId());
                            updateStmt.setInt(2, userId);
                            updateStmt.executeUpdate();
                        }
                        // Create session
                        HttpSession session = request.getSession(true);
                        session.setAttribute("userId", userId);
                        session.setAttribute("username", username);
                        out.print("{\"success\":true,\"userId\":" + userId + "}");
                        return;
                    }
                }
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Invalid username or password\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Database error\"}");
        }
    }
} 
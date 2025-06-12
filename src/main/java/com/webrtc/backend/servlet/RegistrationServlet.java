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
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/register")
public class RegistrationServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        String username = request.getParameter("username");
        String msisdn = request.getParameter("msisdn");
        String password = request.getParameter("password");
        if (username == null || msisdn == null || password == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Missing fields\"}");
            return;
        }
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Check if username or MSISDN already exists
            String checkSql = "SELECT id FROM users WHERE username = ? OR msisdn = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                checkStmt.setString(2, msisdn);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print("{\"error\":\"Username or MSISDN already exists\"}");
                    return;
                }
            }
            // Hash the password
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            // Insert the new user
            String insertSql = "INSERT INTO users (username, msisdn, password_hash, online) VALUES (?, ?, ?, TRUE)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, msisdn);
                insertStmt.setString(3, hashed);
                int affected = insertStmt.executeUpdate();
                if (affected == 0) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"error\":\"Registration failed\"}");
                    return;
                }
                ResultSet keys = insertStmt.getGeneratedKeys();
                if (keys.next()) {
                    int userId = keys.getInt(1);
                    // Create a session for the user
                    HttpSession session = request.getSession(true);
                    session.setAttribute("userId", userId);
                    session.setAttribute("username", username);
                    out.print("{\"success\":true,\"userId\":" + userId + "}");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Database error\"}");
        }
    }
} 
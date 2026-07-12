package servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import utils.PasswordUtil;

@WebServlet("/registerServlet")
public class registerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String empName = request.getParameter("username");
        String empNo = request.getParameter("empNo");
        String password = request.getParameter("password");

        if (empName == null || empNo == null || password == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incomplete registration data.");
            return;
        }

        try {
            // Hash the password before storing it in the database 
            String hashedPassword = PasswordUtil.hashPassword(password);

            if (registerUser(empName,empNo, hashedPassword)) {
                response.sendRedirect("login.jsp");
            } else {
                response.sendRedirect("register.jsp?error=registration_failed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown error during registration: " + e.getMessage());
        }
    }

    private boolean registerUser(String empName, String empNo, String password) throws SQLException {
        String insertSQL = "INSERT INTO `pump_testing_station_log_data` (emp_name , emp_no , password) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection()) {
            
            // Ensure the pump_testing_station_log_data table exists
            try (java.sql.Statement createStmt = conn.createStatement()) {
                createStmt.execute(
                    "CREATE TABLE IF NOT EXISTS `pump_testing_station_log_data` (" +
                    "  emp_name VARCHAR(255), " +
                    "  emp_no VARCHAR(255), " +
                    "  password VARCHAR(255)" +
                    ")"
                );
            }

            try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                stmt.setString(1, empName);
                stmt.setString(2, empNo); 
                stmt.setString(3, password);
                int rowsInserted = stmt.executeUpdate(); 
                return rowsInserted > 0;
            }
        }
    }
}

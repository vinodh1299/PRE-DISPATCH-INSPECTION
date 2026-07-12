package servlets;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import utils.PasswordUtil;

@WebServlet("/loginServlet")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        LOGGER.info("Login attempt with username: " + username);

        if (authenticateWithUsernameAndPassword(username, password, session)) {
            LOGGER.info("Login successful for username: " + username);
            session.setAttribute("isLoggedIn", true); // Set isLoggedIn attribute upon successful login
            
            // Get emp_name, emp_no, and loginTime from session
            String empName = (String) session.getAttribute("emp_name");
            String empNo = (String) session.getAttribute("emp_no");
            Date loginTime = (Date) session.getAttribute("loginTime");
            
            // Handle potential null values
            if (empName == null || empNo == null) {
                LOGGER.warning("emp_name or emp_no is null. Redirecting to an error page.");
                response.sendRedirect("errorPage.jsp");
                return;
            }
            
            if (loginTime == null) {
                LOGGER.warning("loginTime is null. Setting loginTime to current time.");
                loginTime = new Date();
                session.setAttribute("loginTime", loginTime); // Optionally set it back to session
            }

            // Format the loginTime to "dd/MM/yyyy HH:mm:ss"
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String loginTimeStr = sdf.format(loginTime);

            // Redirect to Home.html with query parameters
            response.sendRedirect("index.html?emp_name=" + URLEncoder.encode(empName, "UTF-8") + 
                                  "&emp_no=" + URLEncoder.encode(empNo, "UTF-8") + 
                                  "&login_time=" + URLEncoder.encode(loginTimeStr, "UTF-8"));
        } else {
            LOGGER.warning("Login failed for username: " + username);
            request.setAttribute("errorMessage", "Invalid username or password.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    private boolean authenticateWithUsernameAndPassword(String username, String password, HttpSession session) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT emp_name, emp_no, password FROM `pump_testing_station_log_data` WHERE emp_name = ? OR emp_no = ?")) {
            
            // Set both parameters to handle emp_name or emp_no login
            stmt.setString(1, username);
            stmt.setString(2, username); // Bind the username to emp_no as well
            
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                if (PasswordUtil.verifyPassword(password, storedPassword)) {
                    String empName = rs.getString("emp_name");
                    String empNo = rs.getString("emp_no");
                    
                    LOGGER.info("Storing emp_name: " + empName + " and emp_no: " + empNo + " in session.");
                    session.setAttribute("emp_name", empName);
                    session.setAttribute("emp_no", empNo);
                    
                    // Set login time to the current time
                    session.setAttribute("loginTime", new Date());
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while authenticating with username and password.", e);
        }
        return false;
    }
}

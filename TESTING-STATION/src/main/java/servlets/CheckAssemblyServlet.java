package servlets;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/check-assembly")
public class CheckAssemblyServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String scannedValue = request.getParameter("value");
        boolean exists = false;
        String createdAt = null;

        try (Connection conn = DatabaseUtil.getConnection()) {

            PreparedStatement ps = conn.prepareStatement(
                "SELECT created_at FROM pump_assembly_data WHERE data = ? LIMIT 1"
            );
            ps.setString(1, scannedValue);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                exists = true;
                createdAt = rs.getString("created_at");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Build JSON safely
        String json = "{"
                + "\"exists\":" + exists + ","
                + "\"created_at\":\"" + (createdAt != null ? createdAt : "") + "\""
                + "}";

        response.getWriter().write(json);
    }
}

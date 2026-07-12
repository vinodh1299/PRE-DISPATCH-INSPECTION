package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ChecklistUploadServlet")
public class ChecklistUploadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String monthParam = request.getParameter("month");
        String yearParam  = request.getParameter("year");

        response.setContentType("text/html; charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {

            // --- Check if form params are missing ---
            if (monthParam == null || yearParam == null) {
                out.println("<p>Please go back to the <a href='reportForm.html'>report form</a> and select month/year.</p>");
                return;
            }

            int month = Integer.parseInt(monthParam);
            int year  = Integer.parseInt(yearParam);

            // --- Query: get ONLY one latest record per entry_no ---
            // Using id as tiebreaker to avoid duplicates when created_at is the same
            String sql =
                "SELECT a.entry_no, a.emp_name, a.emp_no, a.workstation, a.shift, a.created_at " +
                "FROM assembly_data a " +
                "JOIN ( " +
                "   SELECT entry_no, MAX(id) AS max_id " +
                "   FROM assembly_data " +
                "   WHERE MONTH(created_at) = ? AND YEAR(created_at) = ? " +
                "   GROUP BY entry_no " +
                ") b ON a.id = b.max_id " +
                "ORDER BY a.entry_no";

            List<Map<String, Object>> rows = new ArrayList<>();

            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, month);
                ps.setInt(2, year);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("entry_no", rs.getString("entry_no"));
                        row.put("emp_name", rs.getString("emp_name"));
                        row.put("emp_no", rs.getString("emp_no"));
                        row.put("workstation", rs.getString("workstation"));
                        row.put("shift", rs.getString("shift"));
                        row.put("created_at", rs.getTimestamp("created_at"));
                        rows.add(row);
                    }
                }

            } catch (SQLException e) {
                throw new ServletException("DB Error: " + e.getMessage(), e);
            }

            // --- Display results ---
            out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
            out.println("<style>");
            out.println("body{font-family:Arial,Helvetica,sans-serif;font-size:14px;color:#111}");
            out.println("table{border-collapse:collapse;width:100%;max-width:1000px;margin-top:20px}");
            out.println("th,td{border:1px solid #ddd;padding:8px;text-align:left;white-space:nowrap}");
            out.println("th{background:#303d99;color:#fff}");
            out.println("tr:nth-child(even){background:#fafafa}");
            out.println("a { text-decoration: none;}");
            out.println("</style></head><body>");

            out.println("<h2>Results for " + Month.of(month) + " " + year + "</h2>");

            if (rows.isEmpty()) {
                out.println("<p><em>No records found for this period.</em></p>");
            } else {
                out.println("<table>");
                out.println("<tr><th>Entry No</th><th>Emp Name</th><th>Emp No</th><th>Workstation</th><th>Shift</th><th>Created At</th></tr>");

                DateTimeFormatter tsFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (Map<String, Object> r : rows) {
                    Timestamp ts = (Timestamp) r.get("created_at");
                    String tsStr = ts.toLocalDateTime().format(tsFmt);
                    String entryNo = escape(r.get("entry_no"));

                    out.println("<tr>");
                    out.println("<td>" + entryNo + "</td>");
                    out.println("<td><a href='details.html?entry_no=" + entryNo + "'>"
                                + escape(r.get("emp_name")) + "</a></td>");
                    out.println("<td>" + escape(r.get("emp_no")) + "</td>");
                    out.println("<td>" + escape(r.get("workstation")) + "</td>");
                    out.println("<td>" + escape(r.get("shift")) + "</td>");
                    out.println("<td>" + tsStr + "</td>");
                    out.println("</tr>");
                }

                out.println("</table>");
            }

            out.println("<p><a href='reportForm.html'>Back to Form</a></p>");
            out.println("</body></html>");
        }
    }

    // Minimal HTML escaping
    private static String escape(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

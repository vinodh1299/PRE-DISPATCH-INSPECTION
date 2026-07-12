package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/testServlet")
public class testServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ---- Compute "yesterday" label ----
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String yLabel = yesterday.format(DateTimeFormatter.ISO_DATE); // yyyy-MM-dd

        // ---- Query: get exactly ONE row per entry_no (latest by id) ----
        String sql =
            "SELECT t.entry_no, t.emp_name, t.emp_no, t.workstation, t.shift, t.created_at " +
            "FROM test_ok_data t " +
            "INNER JOIN ( " +
            "    SELECT entry_no, MAX(id) AS max_id " +
            "    FROM test_ok_data " +
            "    WHERE DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY) " +
            "    GROUP BY entry_no " +
            ") x ON t.entry_no = x.entry_no AND t.id = x.max_id " +
            "ORDER BY t.entry_no";

        List<Map<String, Object>> rows = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

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

        } catch (SQLException e) {
            throw new ServletException("DB Error: " + e.getMessage(), e);
        }

        // ---- Build HTML body ----
     // ---- Build HTML body ----
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            .append("<style>")
            .append("body{font-family:Arial,Helvetica,sans-serif;font-size:14px;color:#111}")
            .append("h2{margin:0 0 10px 0}")
            .append("p{margin:4px 0 12px 0}")
            .append("table{border-collapse:collapse;width:100%;max-width:1000px}")
            .append("th,td{border:1px solid #ddd;padding:8px;text-align:left;white-space:nowrap}")
            .append("th{background:#303d99;color:#fff}")   // ✅ Changed background & text color
            .append("tr:nth-child(even){background:#fafafa}")
            .append(".muted{color:#666}")
            .append("a{color:#303d99;text-decoration:none;font-weight:bold}") // ✅ Styling links
            .append("</style></head><body>");

        html.append("<h2>YIL Assembly Tracking System - Report Summary</h2>");
        html.append("<p class='muted'>Date: ").append(yLabel)
            .append(" &nbsp;|&nbsp; Unique entries: ").append(rows.size()).append("</p>");

        if (rows.isEmpty()) {
            html.append("<p><em>No records found for yesterday.</em></p>");
        } else {
            html.append("<table>")
                .append("<tr>")
                .append("<th>Entry No</th>")
                .append("<th>Emp Name</th>")
                .append("<th>Emp No</th>")
                .append("<th>Workstation</th>")
                .append("<th>Shift</th>")
                .append("<th>Created At</th>")
                .append("</tr>");

            DateTimeFormatter tsFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Map<String, Object> r : rows) {
                Timestamp ts = (Timestamp) r.get("created_at");
                String tsStr = ts.toLocalDateTime().format(tsFmt);

                String entryNo = escape(r.get("entry_no"));
                String empName = escape(r.get("emp_name"));
                String shift   = escape(r.get("shift"));

                // ✅ New format: shift-emp_name as clickable link
                String empDisplay = shift + "-" + empName;
                String empLink = "<a href='http://localhost:8007/REPORT/test-station.html?entry_no=" + entryNo + "'>" 
                                 + empDisplay + "</a>";

                html.append("<tr>")
                    .append("<td>").append(entryNo).append("</td>")
                    .append("<td>").append(empLink).append("</td>")
                    .append("<td>").append(escape(r.get("emp_no"))).append("</td>")
                    .append("<td>").append(escape(r.get("workstation"))).append("</td>")
                    .append("<td>").append(shift).append("</td>")
                    .append("<td>").append(tsStr).append("</td>")
                    .append("</tr>");
            }
            html.append("</table>");
        }

        html.append("</body></html>");


        // ---- Send Email ----
        final String subject = "YIL Assembly Tracking System - Report Summary";

        String smtpHost = "smtp.gmail.com";
        String smtpPort = "587";
        String mailFrom = "YMCNCTPM@gmail.com";
        String mailTo   = "vinodhanu007@gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");                // ✅ required
        props.put("mail.smtp.starttls.enable", "true");     // ✅ required

        Session mailSession = Session.getInstance(props , new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("YMCNCTPM@gmail.com", "hkth xhto eppn eyvp");
            }
        });

        // enable debug for troubleshooting (optional)
        // mailSession.setDebug(true);

        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(mailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailTo));
            message.setSubject(subject, "UTF-8");
            message.setContent(html.toString(), "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (MessagingException e) {
            throw new ServletException("Email Error: " + e.getMessage(), e);
        }

        // ---- Simple HTTP response ----
        response.setContentType("text/html; charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<p>Report emailed successfully with subject: <strong>" + subject + "</strong></p>");
            out.println("<p>Date: " + yLabel + " | Unique entries: " + rows.size() + "</p>");
        }
    }

    // Minimal HTML escaping
    private static String escape(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

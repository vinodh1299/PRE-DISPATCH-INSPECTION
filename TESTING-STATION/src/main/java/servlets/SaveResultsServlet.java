package servlets;

import com.google.gson.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.time.Year;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

@WebServlet("/save-results")
public class SaveResultsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        String body = request.getReader().lines().reduce("", (acc, line) -> acc + line);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();

            String empName = (json.has("emp_name") && !json.get("emp_name").isJsonNull()) ? json.get("emp_name").getAsString() : "";
            String empNo = (json.has("emp_no") && !json.get("emp_no").isJsonNull()) ? json.get("emp_no").getAsString() : "";
            String shift = (json.has("shift") && !json.get("shift").isJsonNull()) ? json.get("shift").getAsString() : "";

            // Ensure the pdi_inspection table exists
            try (Statement createStmt = conn.createStatement()) {
                createStmt.execute(
                    "CREATE TABLE IF NOT EXISTS pdi_inspection (" +
                    "  Serial_key VARCHAR(255) PRIMARY KEY, " +
                    "  Serial_No VARCHAR(255), " +
                    "  Label VARCHAR(255), " +
                    "  emp_name VARCHAR(255), " +
                    "  emp_no VARCHAR(255), " +
                    "  shift VARCHAR(255)" +
                    ")"
                );
            }

            conn.setAutoCommit(false);

            // --- Insert scanned_items ---
            JsonArray scannedItems = json.getAsJsonArray("scanned_items");
            if (scannedItems != null) {
                try (PreparedStatement psScan = conn.prepareStatement(
                    "INSERT INTO pdi_inspection (Serial_key, Serial_No, Label, emp_name, emp_no, shift) VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (JsonElement el : scannedItems) {
                        JsonObject obj = el.getAsJsonObject();
                        String serialNo = obj.get("scanned_data").getAsString();
                        String label = obj.get("label_status").getAsString();
                        String uniqueKey = UUID.randomUUID().toString();

                        psScan.setString(1, uniqueKey);
                        psScan.setString(2, serialNo);
                        psScan.setString(3, label);
                        psScan.setString(4, empName);
                        psScan.setString(5, empNo);
                        psScan.setString(6, shift);
                        psScan.addBatch();
                    }
                    psScan.executeBatch();
                }
            }

            /*
            PreparedStatement psGreen = conn.prepareStatement(
                "INSERT INTO pump_test_ok_data (entry_no, emp_name, emp_no, workstation, scanned_data, status, shift) VALUES (?, ?, ?, ?, ?, ?, ?)");
            PreparedStatement psRed = conn.prepareStatement(
                "INSERT INTO pump_test_not_ok_data (entry_no, emp_name, emp_no, workstation, scanned_data, status, reason, shift) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

            String year = String.valueOf(Year.now().getValue()).substring(2);
            String nextEntryNo = getNextEntryNo(conn, "pump_test_ok_data", year);

            // --- Insert OK entries ---
            JsonArray okArr = json.getAsJsonArray("ok");
            if (okArr != null) {
                for (JsonElement el : okArr) {
                    JsonObject obj = el.getAsJsonObject();

                    psGreen.setString(1, nextEntryNo);
                    psGreen.setString(2, obj.get("emp_name").getAsString());
                    psGreen.setString(3, obj.get("emp_no").getAsString());
                    psGreen.setString(4, obj.get("workstation").getAsString());
                    psGreen.setString(5, obj.get("scanned_data").getAsString());
                    psGreen.setString(6, obj.get("status").getAsString());
                    psGreen.setString(7, obj.get("shift").getAsString());
                    psGreen.addBatch();
                }
                psGreen.executeBatch();
            }

            // --- Insert NOT OK entries ---
            JsonArray notOkArr = json.getAsJsonArray("not_ok");
            if (notOkArr != null) {
                for (JsonElement el : notOkArr) {
                    JsonObject obj = el.getAsJsonObject();

                    psRed.setString(1, nextEntryNo);
                    psRed.setString(2, obj.get("emp_name").getAsString());
                    psRed.setString(3, obj.get("emp_no").getAsString());
                    psRed.setString(4, obj.get("workstation").getAsString());
                    psRed.setString(5, obj.get("scanned_data").getAsString());
                    psRed.setString(6, obj.get("status").getAsString());
                    psRed.setString(7, obj.get("reason").getAsString());
                    psRed.setString(8, obj.get("shift").getAsString());
                    psRed.addBatch();
                }
                psRed.executeBatch();
            }
            */

            conn.commit();

            /*
            // --- ⚡ Handle Not Scanned List (No DB insert) ---
            JsonArray notScannedArr = json.getAsJsonArray("not_scanned");
            if (notScannedArr != null && notScannedArr.size() > 0) {
                List<Map<String, String>> notScannedData = new ArrayList<>();

                for (JsonElement el : notScannedArr) {
                    JsonObject obj = el.getAsJsonObject();
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("entry_no", nextEntryNo);
                    row.put("emp_name", obj.get("emp_name").getAsString());
                    row.put("emp_no", obj.get("emp_no").getAsString());
                    row.put("workstation", obj.get("workstation").getAsString());
                    row.put("scanned_data", obj.get("scanned_data").getAsString());
                    row.put("shift", obj.get("shift").getAsString());
                    notScannedData.add(row);
                }

                // ⚡ Send Email only — do not store in DB
                sendNotScannedMail(notScannedData);
            }
            */

            response.setContentType("application/json");
            response.getWriter().write("{\"success\":true}");

        } catch (Exception e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
            response.sendError(500, e.getMessage());
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
    }

    private String getNextEntryNo(Connection conn, String tableName, String year) throws SQLException {
        String prefix = "YT" + year + "/";
        String lastEntry = null;

        String sql = "SELECT entry_no FROM " + tableName + " WHERE entry_no LIKE ? ORDER BY entry_no DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) lastEntry = rs.getString("entry_no");
            }
        }

        int nextNumber = 1;
        if (lastEntry != null) {
            String[] parts = lastEntry.split("/");
            if (parts.length == 2) nextNumber = Integer.parseInt(parts[1]) + 1;
        }

        return prefix + String.format("%06d", nextNumber);
    }

 // ⚡ Email sender for Not Scanned List
    private void sendNotScannedMail(List<Map<String, String>> data) {
        final String from = "yil.erp@yukenindia.com";
        final String password = "yil16@123"; // or app password
        final String to = "raghavendra.v@yukenindia.com,ashoka.v@yukenindia.com,rajkumar.r@yukenindia.com,manjunatha.n@yukenindia.com";
        final String cc = "nandakumar_b@yukenindia.com"; // Add your CC recipient(s)
        final String bcc = "mahesha_s@yukenindia.com,sheela.kn@yukenindia.com";     // Add your BCC recipient(s)

        System.out.println("[MAIL-LOG] Preparing to send Not Scanned mail...");

        Properties props = new Properties();
        props.put("mail.smtp.host", "pod51022.outlook.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        try {
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(from, password);
                }
            });

            System.out.println("[MAIL-LOG] Mail session initialized successfully.");

            // Build HTML mail content
            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h3 style='color: red;'>⚠️ NOT SCANNED LIST ALERT</h3>");
            html.append("<table style='width:100%; border-collapse: collapse; background-color: #0b0b4b; color: white;'>");
            html.append("<tr>")
                .append("<th style='padding: 10px; border: 3px solid #bbb9b9;'>Entry No</th>")
                .append("<th style='padding: 10px; border: 3px solid #bbb9b9;'>Emp Name</th>")
                .append("<th style='padding: 10px; border: 3px solid #bbb9b9;'>Emp No</th>")
                .append("<th style='padding: 10px; border: 3px solid #bbb9b9;'>Workstation</th>")
                .append("<th style='padding: 10px; border: 3px solid #bbb9b9;'>Scanned Data</th>")
                .append("<th style='padding: 10px; border: 3px solid #bbb9b9;'>Shift</th>")
                .append("</tr>");

            for (Map<String, String> row : data) {
                html.append("<tr>")
                    .append("<td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>")
                    .append(row.get("entry_no")).append("</td>")
                    .append("<td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>")
                    .append(row.get("emp_name")).append("</td>")
                    .append("<td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>")
                    .append(row.get("emp_no")).append("</td>")
                    .append("<td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>")
                    .append(row.get("workstation")).append("</td>")
                    .append("<td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>")
                    .append(row.get("scanned_data")).append("</td>")
                    .append("<td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>")
                    .append(row.get("shift")).append("</td>")
                    .append("</tr>");
            }

            html.append("</table>");
            html.append("<p style='font-size:12px;color:gray;'>This is an automated alert. Please verify the missing scans.</p>");
            html.append("</body></html>");

            System.out.println("[MAIL-LOG] HTML content built. Preparing message object...");

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
            message.setSubject("ASSEMBLED PUMP NOT CAPTURED IN ASSEMBLY WORKSTATION - " + new java.util.Date());
            message.setContent(html.toString(), "text/html; charset=utf-8");

            System.out.println("[MAIL-LOG] Message composed successfully. Attempting to send...");

            Transport.send(message);

            System.out.println("[MAIL-LOG] ✅ Mail sent successfully to " + to + 
                               " (Cc: " + cc + ", Bcc: " + bcc + ")");

        } catch (AuthenticationFailedException e) {
            System.err.println("[MAIL-LOG] ❌ Authentication failed! Check app password or sender email.");
            e.printStackTrace();
        } catch (SendFailedException e) {
            System.err.println("[MAIL-LOG] ❌ Send failed! Invalid recipient or network issue.");
            e.printStackTrace();
        } catch (MessagingException e) {
            System.err.println("[MAIL-LOG] ❌ MessagingException occurred during mail send.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[MAIL-LOG] ❌ Unexpected error while sending mail.");
            e.printStackTrace();
        }
    }


}

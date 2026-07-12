package servlets;

import com.google.gson.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.Properties;

@WebServlet("/send-reason")
public class SendReasonServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            // Read and parse JSON body
            String body = request.getReader().lines().reduce("", (acc, line) -> acc + line);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            // Extract required fields
            String empName = json.get("emp_name").getAsString();
            String empNo = json.get("emp_no").getAsString();
            String workstation = json.get("workstation").getAsString();
            String scannedData = json.get("scanned_data").getAsString();
            String shift = json.get("shift").getAsString();
            String reason = json.get("reason").getAsString();

            // Fixed recipients
            String to = "raghavendra.v@yukenindia.com,ashoka.v@yukenindia.com,rajkumar.r@yukenindia.com,manjunatha.n@yukenindia.com"; // Primary recipient
            String cc = "nandakumar_b@yukenindia.com"; // CC recipient
            String bcc = "mahesha_s@yukenindia.com,sheela.kn@yukenindia.com"; // BCC recipients

            // Send the email
            sendEmail(to, cc, bcc, "TESTED PUMP NOT OK ALERT", scannedData, reason, empName, empNo, shift, workstation);

            // Success response
            out.write("{\"success\": true}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\": false, \"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        } finally {
            out.flush();
        }
    }

    private void sendEmail(String to, String cc, String bcc, String subject, String scannedData, String reason,
                           String empName, String empNo, String shift, String workstation) throws MessagingException {

        // SMTP Configuration
        Properties props = new Properties();
        props.put("mail.smtp.host", "pod51022.outlook.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // Gmail App Password authentication
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("yil.erp@yukenindia.com", "yil16@123");
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress("yil.erp@yukenindia.com"));
        message.setSubject(subject);

        // Add recipients
        addRecipients(message, Message.RecipientType.TO, to);
        addRecipients(message, Message.RecipientType.CC, cc);
        addRecipients(message, Message.RecipientType.BCC, bcc);

        // Build HTML email content
        String htmlContent = "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<h2 style='color: navy;'>TESTED PUMP NOT OK ALERT</h2>" +
                "<table style='width:100%; border-collapse: collapse; background-color: #0b0b4b; color: white;'>" +
                "  <tr>" +
                "    <th style='padding: 10px; border: 3px solid #bbb9b9;'>Employee Name</th>" +
                "    <th style='padding: 10px; border: 3px solid #bbb9b9;'>Employee No</th>" +
                "    <th style='padding: 10px; border: 3px solid #bbb9b9;'>Shift</th>" +
                "    <th style='padding: 10px; border: 3px solid #bbb9b9;'>Scanned Data</th>" +
                "    <th style='padding: 10px; border: 3px solid #bbb9b9;'>Workstation</th>" +
                "    <th style='padding: 10px; border: 3px solid #bbb9b9;'>Reason</th>" +
                "  </tr>" +
                "  <tr>" +
                "    <td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>" + empName + "</td>" +
                "    <td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>" + empNo + "</td>" +
                "    <td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>" + shift + "</td>" +
                "    <td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>" + scannedData + "</td>" +
                "    <td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>" + workstation + "</td>" +
                "    <td style='padding: 10px; border: 3px solid #bbb9b9; text-align:center;'>" + reason + "</td>" +
                "  </tr>" +
                "</table>" +
                "</body></html>";

        message.setContent(htmlContent, "text/html; charset=UTF-8");

        Transport.send(message);
    }

    private void addRecipients(Message message, Message.RecipientType type, String recipients) throws MessagingException {
        if (recipients != null && !recipients.trim().isEmpty()) {
            InternetAddress[] addresses = InternetAddress.parse(recipients);
            message.addRecipients(type, addresses);
        }
    }
}

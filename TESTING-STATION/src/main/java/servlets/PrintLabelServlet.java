package servlets;

import java.io.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.print.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.json.JSONObject;

@WebServlet("/print-label")
public class PrintLabelServlet extends HttpServlet {

    private static final ConcurrentLinkedQueue<String> printQueue = new ConcurrentLinkedQueue<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        
        String job = printQueue.poll();
        if (job != null) {
            response.setContentType("text/plain");
            response.getWriter().write(job);
        } else {
            response.setContentType("text/plain");
            response.getWriter().write("NO_JOBS");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        try {
            String body = request.getReader().lines().collect(Collectors.joining());
            JSONObject json = new JSONObject(body);

            String qrData = json.getString("qrData");
            String text = json.getString("text");
            String createdAt = json.optString("created_at", "");

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String dateTimeStr = sdf.format(new java.util.Date());

            String logoPath = getServletContext().getRealPath("/Assets/YUKEN LOGO.png");
            byte[] logoBytes = getLogoBitmapBytes(logoPath, 40, 30);

            String prefix =
                    "SIZE 81.1 mm,39.4 mm\n" +
                    "GAP 3 mm,0\n" +
                    "DIRECTION 1\n" +
                    "CLS\n";

            String suffix =
                    "TEXT 450,45,\"3\",0,1,1,\"" + qrData + "\"\n" +
                    "TEXT 370,80,\"2\",0,1,1,\"" + dateTimeStr + "\"\n" +
                    "TEXT 40,130,\"2\",0,2,2,\"" + text + "\"\n" +
                    "PUTPCX 60,160,\"check.pcx\"\n" +
                    "QRCODE 450,130,H,6,A,0,\"" + qrData + "\"\n" +
                    "PRINT 1,1\n";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(prefix.getBytes("UTF-8"));
            if (logoBytes != null) {
                baos.write(logoBytes);
            }
            baos.write(suffix.getBytes("UTF-8"));
            byte[] printPayload = baos.toByteArray();

            String base64Payload = java.util.Base64.getEncoder().encodeToString(printPayload);

            // Add the print payload to the queue
            printQueue.add(base64Payload);

            // Respond success JSON to the browser
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"queued\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("ERROR: " + e.getMessage());
        }
    }

    private byte[] getLogoBitmapBytes(String imagePath, int targetX, int targetY) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                System.err.println("[PRINT-LOG] Logo file not found at: " + imagePath);
                return null;
            }
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(file);
            if (img == null) {
                return null;
            }

            // Target size for the logo in dots: 160 width, 40 height
            int width = 160;
            int height = 40;

            // Resize image
            java.awt.Image tmp = img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
            java.awt.image.BufferedImage dimg = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_BYTE_BINARY);
            java.awt.Graphics2D g2d = dimg.createGraphics();
            g2d.drawImage(tmp, 0, 0, null);
            g2d.dispose();

            int widthBytes = (width + 7) / 8;
            byte[] bitmapData = new byte[widthBytes * height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = dimg.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int gray = (r + g + b) / 3;
                    if (gray < 200) { 
                        int byteIdx = y * widthBytes + (x / 8);
                        int bitIdx = 7 - (x % 8);
                        bitmapData[byteIdx] |= (1 << bitIdx);
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String header = "BITMAP " + targetX + "," + targetY + "," + widthBytes + "," + height + ",0,";
            baos.write(header.getBytes("US-ASCII"));
            baos.write(bitmapData);
            baos.write("\r\n".getBytes("US-ASCII"));
            return baos.toByteArray();

        } catch (Exception e) {
            System.err.println("[PRINT-LOG] Error converting logo to bitmap: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class OCRSpaceAPI {
    public static void main(String[] args) throws IOException {
        String apiKey = "";

        File imageFile = new File("C:\\Users\\HP\\OneDrive\\Desktop\\3.png");
        if (!imageFile.exists()) {
            System.err.println("File not found!");
            return;
        }

        byte[] imageData = Files.readAllBytes(imageFile.toPath());

        URL url = new URL("https://api.ocr.space/parse/image");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("apikey", apiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=*****");
        conn.setDoOutput(true);

        DataOutputStream request = new DataOutputStream(conn.getOutputStream());

        // Send image part
        request.writeBytes("--*****\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + imageFile.getName() + "\"\r\n");
        request.writeBytes("Content-Type: image/jpeg\r\n\r\n");
        request.write(imageData);
        request.writeBytes("\r\n");

        // Send language and other params
        request.writeBytes("--*****\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"language\"\r\n\r\n");
        request.writeBytes("eng\r\n");

        request.writeBytes("--*****--\r\n");
        request.flush();
        request.close();

        // Get response
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Print response
        // System.out.println("OCR Result:");
        // System.out.println(response.toString());

        // Extract ParsedText manually
        String json = response.toString();
        String key = "\"ParsedText\":\"";
        int startIndex = json.indexOf(key);
        if (startIndex != -1) {
            startIndex += key.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex != -1) {
                String parsedText = json.substring(startIndex, endIndex);
                // Replace escaped newlines or other characters if needed
                parsedText = parsedText.replace("\\r", "").replace("\\n", "\n");
                System.out.println("Extracted Text:");
                System.out.println(parsedText);
            } else {
                System.out.println("Could not find end of ParsedText.");
            }
        } else {
            System.out.println("ParsedText not found in response.");
        }
    }
}

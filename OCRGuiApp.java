import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class OCRGuiApp extends JFrame {
    private JTextField filePathField;
    private JTextArea resultArea;
    private File selectedFile;
    private final String apiKey = "K84263010888957";

    public OCRGuiApp() {
        setTitle("OCR Image Text Extractor");
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);

        // Top Panel
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(new Color(250, 250, 250));
        topPanel.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(200, 200, 200)),
            new EmptyBorder(15, 20, 15, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;

        JButton browseButton = new JButton("Browse Image");
        browseButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        browseButton.setBackground(new Color(70, 130, 180));
        browseButton.setForeground(Color.WHITE);
        browseButton.setFocusPainted(false);
        browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        browseButton.setPreferredSize(new Dimension(150, 35));

        gbc.anchor = GridBagConstraints.WEST;
        topPanel.add(browseButton, gbc);

        filePathField = new JTextField(40);
        filePathField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        filePathField.setEditable(false);
        filePathField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180)),
            new EmptyBorder(5, 10, 5, 10)
        ));
        gbc.gridx++;
        topPanel.add(filePathField, gbc);

        // Text Area
        resultArea = new JTextArea();
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setBackground(Color.WHITE);
        resultArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                "Extracted Text", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 16), new Color(60, 60, 60)),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(new EmptyBorder(10, 20, 10, 20));
        scrollPane.setPreferredSize(new Dimension(750, 300));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(new Color(250, 250, 250));
        bottomPanel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
            new EmptyBorder(15, 10, 15, 10)
        ));

        JButton extractButton = new JButton("Extract Text");
        extractButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        extractButton.setBackground(new Color(60, 179, 113));
        extractButton.setForeground(Color.WHITE);
        extractButton.setFocusPainted(false);
        extractButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        extractButton.setPreferredSize(new Dimension(200, 45));

        bottomPanel.add(extractButton);

        // Add panels
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Browse Action
        browseButton.addActionListener((ActionEvent e) -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        // Extract Action
        extractButton.addActionListener((ActionEvent e) -> {
            if (selectedFile != null) {
                try {
                    String extractedText = sendImageToOCRSpace(selectedFile);
                    resultArea.setText(extractedText);
                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "⚠ Please select an image first.");
            }
        });
    }

    private String sendImageToOCRSpace(File imageFile) throws IOException {
        byte[] imageData = Files.readAllBytes(imageFile.toPath());

        URL url = new URL("https://api.ocr.space/parse/image");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("apikey", apiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=*****");
        conn.setDoOutput(true);

        DataOutputStream request = new DataOutputStream(conn.getOutputStream());

        request.writeBytes("--*****\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + imageFile.getName() + "\"\r\n");
        request.writeBytes("Content-Type: image/jpeg\r\n\r\n");
        request.write(imageData);
        request.writeBytes("\r\n");

        request.writeBytes("--*****\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"language\"\r\n\r\n");
        request.writeBytes("eng\r\n");

        request.writeBytes("--*****--\r\n");
        request.flush();
        request.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String json = response.toString();
        String key = "\"ParsedText\":\"";
        int startIndex = json.indexOf(key);
        if (startIndex != -1) {
            startIndex += key.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex != -1) {
                String parsedText = json.substring(startIndex, endIndex);
                parsedText = parsedText.replace("\\r", "").replace("\\n", "\n");
                return parsedText;
            }
        }

        return "Text not found in response.";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OCRGuiApp app = new OCRGuiApp();
            app.setVisible(true);
        });
    }
}

package com.yourcompany.mp3joiner.ui;

import com.yourcompany.mp3joiner.db.DatabaseManager;
import com.yourcompany.mp3joiner.service.AIService;
import com.yourcompany.mp3joiner.service.AudioProcessor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

public class SettingsDialog extends JDialog {
    private JTextField ffmpegPathField;
    private JTextField voskModelPathField;
    private JTextField dbServerField, dbNameField, dbUserField;
    private JPasswordField dbPasswordField;

    private AudioProcessor audioProcessor;
    private AIService aiService;
    private DatabaseManager dbManager;

    public SettingsDialog(Frame owner) {
        super(owner, "Cài đặt", true);
        this.audioProcessor = new AudioProcessor(); // Khởi tạo để lấy/lưu path
        this.aiService = new AIService();
        this.dbManager = DatabaseManager.getInstance();

        setLayout(new BorderLayout());
        setSize(550, 350);
        setLocationRelativeTo(owner);
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        // FFMPEG Path
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Đường dẫn FFMPEG (ffmpeg.exe):"), gbc);
        ffmpegPathField = new JTextField(30);
        ffmpegPathField.setText(audioProcessor.getFFmpegPathFromConfig());
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(ffmpegPathField, gbc);
        JButton browseFFmpegButton = new JButton("...");
        browseFFmpegButton.addActionListener(e -> browseForFile(ffmpegPathField, false));
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(browseFFmpegButton, gbc);

        // Vosk Model Path
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Đường dẫn Vosk Model (thư mục):"), gbc);
        voskModelPathField = new JTextField(30);
        voskModelPathField.setText(aiService.getModelPathFromConfig());
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(voskModelPathField, gbc);
        JButton browseVoskButton = new JButton("...");
        browseVoskButton.addActionListener(e -> browseForDirectory(voskModelPathField));
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(browseVoskButton, gbc);

        // DB Settings
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("DB Server (VD: localhost\\SQLEXPRESS):"), gbc);
        dbServerField = new JTextField(30);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(dbServerField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("DB Name (VD: MP3ManagerDB):"), gbc);
        dbNameField = new JTextField(30);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(dbNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("DB User:"), gbc);
        dbUserField = new JTextField(30);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(dbUserField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("DB Password:"), gbc);
        dbPasswordField = new JPasswordField(30);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(dbPasswordField, gbc);

        // Load current DB config
        String[] currentDbConfig = dbManager.getCurrentConfig();
        dbServerField.setText(currentDbConfig[0]);
        dbNameField.setText(currentDbConfig[1]);
        dbUserField.setText(currentDbConfig[2]);
        dbPasswordField.setText(currentDbConfig[3]);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Lưu");
        saveButton.addActionListener(e -> saveSettings());
        JButton cancelButton = new JButton("Hủy");
        cancelButton.addActionListener(e -> setVisible(false));
        JButton testDbButton = new JButton("Kiểm tra CSDL");
        testDbButton.addActionListener(e -> testDbConnection());

        buttonPanel.add(testDbButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // private void browseForFile(JTextField targetField, boolean directoryOnly) {
    // JFileChooser chooser = new JFileChooser();
    // if (directoryOnly) {
    // chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    // } else {
    // chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    // }

    // File currentFile = new File(targetField.getText());
    // if (currentFile.exists()) {
    // chooser.setCurrentDirectory(directoryOnly ? currentFile :
    // currentFile.getParentFile());
    // }

    // if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
    // targetField.setText(chooser.getSelectedFile().getAbsolutePath());
    // }
    // }
    // Trong SettingsDialog.java -> browseForFile
    private void browseForFile(JTextField targetField, boolean directoryOnly) {
        JFileChooser chooser = new JFileChooser();
        if (directoryOnly) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        }

        // THỬ ĐẶT THƯ MỤC HIỆN TẠI LÀ MỘT THƯ MỤC CỤ THỂ, "SẠCH"
        File safeDir = new File("D:\\TempTestFolder"); // Nhớ tạo thư mục này trước
        if (safeDir.exists() && safeDir.isDirectory()) {
            chooser.setCurrentDirectory(safeDir);
            System.out.println("JFileChooser current directory set to: " + safeDir.getAbsolutePath());
        } else {
            // Fallback về thư mục người dùng nếu thư mục test không tồn tại
            File userHomeDir = new File(System.getProperty("user.home"));
            if (userHomeDir.exists() && userHomeDir.isDirectory()) {
                chooser.setCurrentDirectory(userHomeDir);
                System.out.println("JFileChooser current directory set to user.home: " + userHomeDir.getAbsolutePath());
            }
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseForDirectory(JTextField targetField) {
        browseForFile(targetField, true);
    }

    private void saveSettings() {
        audioProcessor.saveFFmpegPath(ffmpegPathField.getText());
        aiService.saveModelPath(voskModelPathField.getText());
        dbManager.saveConfig(
                dbServerField.getText(),
                dbNameField.getText(),
                dbUserField.getText(),
                new String(dbPasswordField.getPassword()));
        JOptionPane.showMessageDialog(this, "Cài đặt đã được lưu. Một số thay đổi cần khởi động lại ứng dụng.",
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        setVisible(false);
    }

    private void testDbConnection() {
        // Lưu tạm thời để test, không commit vào preferences
        String server = dbServerField.getText();
        String name = dbNameField.getText();
        String user = dbUserField.getText();
        String pass = new String(dbPasswordField.getPassword());

        // Tạo một instance tạm thời hoặc có một hàm test trong dbManager
        DatabaseManager tempDbManager = DatabaseManager.getInstance(); // Lấy instance hiện tại
        String[] originalConfig = tempDbManager.getCurrentConfig(); // Lưu config gốc

        tempDbManager.saveConfig(server, name, user, pass); // Cập nhật config cho instance hiện tại để test

        if (tempDbManager.testConnection()) {
            JOptionPane.showMessageDialog(this, "Kết nối CSDL thành công!", "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Kết nối CSDL thất bại. Vui lòng kiểm tra lại thông tin.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
        // Khôi phục config gốc
        tempDbManager.saveConfig(originalConfig[0], originalConfig[1], originalConfig[2], originalConfig[3]);
    }
}

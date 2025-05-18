package com.yourcompany.mp3joiner.db;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class DatabaseManager {
    // Nên để người dùng cấu hình thông tin này, hoặc lưu trong file properties
    // Ví dụ sử dụng Preferences API để lưu/tải cấu hình
    private static final String PREF_DB_SERVER = "db_server";
    private static final String PREF_DB_NAME = "db_name";
    private static final String PREF_DB_USER = "db_user";
    private static final String PREF_DB_PASSWORD = "db_password"; // Cẩn thận khi lưu password

    private String dbServer; // = "localhost\\SQLEXPRESS"; // Thay thế bằng server của bạn
    private String dbName; // = "MP3ManagerDB";
    private String dbUser; // = "your_user"; //
    private String dbPassword; // = "your_password";

    private static DatabaseManager instance;

    private DatabaseManager() {
        loadConfig();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void loadConfig() {
        Preferences prefs = Preferences.userNodeForPackage(DatabaseManager.class);
        this.dbServer = prefs.get(PREF_DB_SERVER, "localhost\\SQLEXPRESS"); // Giá trị mặc định
        this.dbName = prefs.get(PREF_DB_NAME, "MP3ManagerDB");
        this.dbUser = prefs.get(PREF_DB_USER, "sa"); // Cẩn thận user mặc định
        this.dbPassword = prefs.get(PREF_DB_PASSWORD, "yourStrong(!)Password"); // Cẩn thận pass mặc định
    }

    public void saveConfig(String server, String dbName, String user, String password) {
        Preferences prefs = Preferences.userNodeForPackage(DatabaseManager.class);
        prefs.put(PREF_DB_SERVER, server);
        prefs.put(PREF_DB_NAME, dbName);
        prefs.put(PREF_DB_USER, user);
        prefs.put(PREF_DB_PASSWORD, password); // Cần mã hóa nếu lưu trữ thực tế

        // Cập nhật lại biến instance
        this.dbServer = server;
        this.dbName = dbName;
        this.dbUser = user;
        this.dbPassword = password;
    }

    public String[] getCurrentConfig() {
        return new String[]{dbServer, dbName, dbUser, dbPassword};
    }


    // private Connection connect() throws SQLException {
    //      String connectionUrl = String.format(
    //         "jdbc:sqlserver://%s;databaseName=%s;user=%s;password=%s;encrypt=false;trustServerCertificate=true;loginTimeout=10;",
    //         dbServer, dbName, dbUser, dbPassword
    //     );
    //     // For Windows Authentication (nếu SQL Server và app chạy cùng user và đã cấu hình)
    //     // String connectionUrl = String.format(
    //     //    "jdbc:sqlserver://%s;databaseName=%s;integratedSecurity=true;encrypt=true;trustServerCertificate=true;",
    //     //    dbServer, dbName
    //     // );
    //     return DriverManager.getConnection(connectionUrl);
    // }

    private Connection connect() throws SQLException {
    String serverForJdbc = dbServer; // dbServer là giá trị bạn nhập từ dialog (ví dụ: "localhost,1433")

    if (dbServer != null && dbServer.contains(",")) {
        // Nếu dbServer chứa dấu phẩy (vd: "localhost,1433"),
        // thay thế nó bằng dấu hai chấm cho đúng cú pháp JDBC (vd: "localhost:1433")
        serverForJdbc = dbServer.replace(',', ':');
    }

    // Bạn có thể vẫn đang thử với encrypt=false từ gợi ý trước, hãy giữ nó nếu cần
    String connectionUrl = String.format(
        "jdbc:sqlserver://%s;databaseName=%s;user=%s;password=%s;encrypt=false;loginTimeout=10;",
        serverForJdbc, // Sử dụng serverForJdbc đã được điều chỉnh
        dbName,
        dbUser,
        dbPassword
    );

    System.out.println("Attempting to connect to: " + connectionUrl); // Thêm dòng này để debug chuỗi kết nối

    return DriverManager.getConnection(connectionUrl);
}

    // 
    public boolean testConnection() {
        try (Connection conn = connect()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Test connection failed: " + e.getMessage());
            return false;
        }
    }


    // CRUD cho Files
    public int addFile(FileInfo file) {
        String sql = "INSERT INTO tbl_files(file_name, file_path, duration_seconds) VALUES(?, ?, ?)";
        int generatedId = -1;
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, file.getFileName());
            pstmt.setString(2, file.getFilePath());
            pstmt.setDouble(3, file.getDurationSeconds());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding file: " + e.getMessage());
        }
        return generatedId;
    }

    public List<FileInfo> getAllFiles() {
        List<FileInfo> files = new ArrayList<>();
        String sql = "SELECT id, file_name, file_path, duration_seconds, created_at FROM tbl_files ORDER BY file_name";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                files.add(new FileInfo(
                        rs.getInt("id"),
                        rs.getString("file_name"),
                        rs.getString("file_path"),
                        rs.getDouble("duration_seconds"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting files: " + e.getMessage());
        }
        return files;
    }

    public boolean deleteFile(int fileId) {
        String sql = "DELETE FROM tbl_files WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting file: " + e.getMessage());
            return false;
        }
    }

    public double getFileDuration(int fileId) {
        String sql = "SELECT duration_seconds FROM tbl_files WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("duration_seconds");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting file duration: " + e.getMessage());
        }
        return 0; // Or throw exception
    }


    // CRUD cho Sentences
    public int addSentence(SentenceInfo sentence) {
        String sql = "INSERT INTO tbl_sentences(file_id, start_time_seconds, end_time_seconds, sentence_text) VALUES(?, ?, ?, ?)";
         int generatedId = -1;
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sentence.getFileId());
            pstmt.setDouble(2, sentence.getStartTimeSeconds());
            pstmt.setDouble(3, sentence.getEndTimeSeconds());
            pstmt.setString(4, sentence.getSentenceText());
            int affectedRows = pstmt.executeUpdate();

             if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1);
                        sentence.setId(generatedId); // Cập nhật ID cho đối tượng
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding sentence: " + e.getMessage());
        }
        return generatedId;
    }

    public List<SentenceInfo> getSentencesByFileId(int fileId) {
        List<SentenceInfo> sentences = new ArrayList<>();
        String sql = "SELECT id, file_id, start_time_seconds, end_time_seconds, sentence_text, last_modified_at " +
                     "FROM tbl_sentences WHERE file_id = ? ORDER BY start_time_seconds";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sentences.add(new SentenceInfo(
                            rs.getInt("id"),
                            rs.getInt("file_id"),
                            rs.getDouble("start_time_seconds"),
                            rs.getDouble("end_time_seconds"),
                            rs.getString("sentence_text"),
                            rs.getTimestamp("last_modified_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting sentences: " + e.getMessage());
        }
        return sentences;
    }

    public boolean updateSentence(SentenceInfo sentence) {
        String sql = "UPDATE tbl_sentences SET start_time_seconds = ?, end_time_seconds = ?, sentence_text = ?, last_modified_at = GETDATE() " +
                     "WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, sentence.getStartTimeSeconds());
            pstmt.setDouble(2, sentence.getEndTimeSeconds());
            pstmt.setString(3, sentence.getSentenceText());
            pstmt.setInt(4, sentence.getId());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating sentence: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteSentencesByFileId(int fileId) {
         String sql = "DELETE FROM tbl_sentences WHERE file_id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            pstmt.executeUpdate(); // Không cần kiểm tra số dòng bị ảnh hưởng nếu mục đích là xóa hết
            return true;
        } catch (SQLException e) {
            System.err.println("Error deleting sentences for fileId " + fileId + ": " + e.getMessage());
            return false;
        }
    }

    // CRUD cho Compositions
    public int addComposition(CompositionInfo composition) {
        String sql = "INSERT INTO tbl_compositions(composition_name, composition_path) VALUES(?, ?)";
        int generatedId = -1;
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, composition.getCompositionName());
            pstmt.setString(2, composition.getCompositionPath());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding composition: " + e.getMessage());
        }
        return generatedId;
    }

    public List<CompositionInfo> getAllCompositions() {
        List<CompositionInfo> compositions = new ArrayList<>();
        String sql = "SELECT id, composition_name, composition_path, created_at FROM tbl_compositions ORDER BY created_at DESC";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                compositions.add(new CompositionInfo(
                        rs.getInt("id"),
                        rs.getString("composition_name"),
                        rs.getString("composition_path"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting compositions: " + e.getMessage());
        }
        return compositions;
    }
}
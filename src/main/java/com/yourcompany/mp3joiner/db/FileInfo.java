package com.yourcompany.mp3joiner.db;

import java.time.LocalDateTime;

public class FileInfo {
    private int id;
    private String fileName;
    private String filePath;
    private double durationSeconds;
    private LocalDateTime createdAt;

    public FileInfo(int id, String fileName, String filePath, double durationSeconds, LocalDateTime createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.durationSeconds = durationSeconds;
        this.createdAt = createdAt;
    }
    public FileInfo(String fileName, String filePath, double durationSeconds) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.durationSeconds = durationSeconds;
    }


    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(double durationSeconds) { this.durationSeconds = durationSeconds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { // Dùng cho hiển thị trong JList chẳng hạn
        return fileName;
    }
}
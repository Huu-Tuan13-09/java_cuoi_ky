package com.yourcompany.mp3joiner.db;

import java.time.LocalDateTime;

public class CompositionInfo {
    private int id;
    private String compositionName;
    private String compositionPath;
    private LocalDateTime createdAt;

    public CompositionInfo(int id, String compositionName, String compositionPath, LocalDateTime createdAt) {
        this.id = id;
        this.compositionName = compositionName;
        this.compositionPath = compositionPath;
        this.createdAt = createdAt;
    }

    public CompositionInfo(String compositionName, String compositionPath) {
         this.compositionName = compositionName;
        this.compositionPath = compositionPath;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCompositionName() { return compositionName; }
    public void setCompositionName(String compositionName) { this.compositionName = compositionName; }
    public String getCompositionPath() { return compositionPath; }
    public void setCompositionPath(String compositionPath) { this.compositionPath = compositionPath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
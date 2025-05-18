package com.yourcompany.mp3joiner.db;

import java.time.LocalDateTime;
import com.yourcompany.mp3joiner.util.TimeConverter;


public class SentenceInfo {
    private int id;
    private int fileId;
    private double startTimeSeconds;
    private double endTimeSeconds;
    private String sentenceText;
    private LocalDateTime lastModifiedAt;

    public SentenceInfo(int id, int fileId, double startTimeSeconds, double endTimeSeconds, String sentenceText, LocalDateTime lastModifiedAt) {
        this.id = id;
        this.fileId = fileId;
        this.startTimeSeconds = startTimeSeconds;
        this.endTimeSeconds = endTimeSeconds;
        this.sentenceText = sentenceText;
        this.lastModifiedAt = lastModifiedAt;
    }

     public SentenceInfo(int fileId, double startTimeSeconds, double endTimeSeconds, String sentenceText) {
        this.fileId = fileId;
        this.startTimeSeconds = startTimeSeconds;
        this.endTimeSeconds = endTimeSeconds;
        this.sentenceText = sentenceText;
    }


    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getFileId() { return fileId; }
    public void setFileId(int fileId) { this.fileId = fileId; }
    public double getStartTimeSeconds() { return startTimeSeconds; }
    public void setStartTimeSeconds(double startTimeSeconds) { this.startTimeSeconds = startTimeSeconds; }
    public double getEndTimeSeconds() { return endTimeSeconds; }
    public void setEndTimeSeconds(double endTimeSeconds) { this.endTimeSeconds = endTimeSeconds; }
    public String getSentenceText() { return sentenceText; }
    public void setSentenceText(String sentenceText) { this.sentenceText = sentenceText; }
    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(LocalDateTime lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }

    public String getFormattedTimestamp() {
        return TimeConverter.secondsToHmmss(startTimeSeconds) + "-" + TimeConverter.secondsToHmmss(endTimeSeconds);
    }

    @Override
    public String toString() {
        return getFormattedTimestamp() + " " + sentenceText;
    }
}
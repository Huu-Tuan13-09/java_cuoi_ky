package com.yourcompany.mp3joiner.util;

import java.util.concurrent.TimeUnit;

public class TimeConverter {

    // Chuyển từ tổng số giây sang định dạng H:MM:SS
    public static String secondsToHmmss(double totalSeconds) {
        long hours = TimeUnit.SECONDS.toHours((long) totalSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes((long) totalSeconds) % 60;
        long seconds = TimeUnit.SECONDS.toSeconds((long) totalSeconds) % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    // Chuyển từ tổng số giây sang định dạng HH:MM:SS.mmm (cho FFMPEG)
    public static String secondsToFFmpegTime(double totalSeconds) {
        long hours = TimeUnit.SECONDS.toHours((long) totalSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes((long) totalSeconds) % 60;
        // Giữ lại phần thập phân cho giây
        double secondsWithMillis = totalSeconds - (hours * 3600) - (minutes * 60);
        return String.format("%02d:%02d:%06.3f", hours, minutes, secondsWithMillis);
    }


    // Chuyển từ chuỗi H:MM:SS hoặc MM:SS sang tổng số giây
    public static double hmmssToSeconds(String timeString) {
        String[] parts = timeString.split(":");
        double totalSeconds = 0;
        try {
            if (parts.length == 3) { // H:MM:SS
                totalSeconds = Integer.parseInt(parts[0]) * 3600 +
                               Integer.parseInt(parts[1]) * 60 +
                               Double.parseDouble(parts[2]);
            } else if (parts.length == 2) { // MM:SS
                totalSeconds = Integer.parseInt(parts[0]) * 60 +
                               Double.parseDouble(parts[1]);
            } else {
                throw new IllegalArgumentException("Invalid time format. Use H:MM:SS or MM:SS");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in time format: " + e.getMessage());
        }
        return totalSeconds;
    }
}
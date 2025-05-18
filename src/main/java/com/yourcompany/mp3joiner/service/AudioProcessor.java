package com.yourcompany.mp3joiner.service;

import com.yourcompany.mp3joiner.util.TimeConverter;
import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.prefs.Preferences;


public class AudioProcessor {
    private String ffmpegPath = "ffmpeg"; // Mặc định
    private String ffprobePath = "ffprobe"; // Mặc định
    private static final String PREF_FFMPEG_PATH = "ffmpeg_path";


    public AudioProcessor() {
        loadConfig();
    }

    private void loadConfig() {
        Preferences prefs = Preferences.userNodeForPackage(AudioProcessor.class);
        String savedPath = prefs.get(PREF_FFMPEG_PATH, "");
        if (savedPath != null && !savedPath.isEmpty()) {
            File ffmpegFile = new File(savedPath);
            if (ffmpegFile.exists() && ffmpegFile.isFile()) {
                 this.ffmpegPath = ffmpegFile.getAbsolutePath();
                 // Giả định ffprobe nằm cùng thư mục với ffmpeg
                 this.ffprobePath = Paths.get(ffmpegFile.getParent(), "ffprobe").toString();
            } else {
                // Thử tìm trong thư mục làm việc của ứng dụng nếu người dùng copy vào đó
                File localFFmpeg = new File("ffmpeg/bin/ffmpeg.exe"); // Hoặc ffmpeg không có .exe
                 if(localFFmpeg.exists()){
                     this.ffmpegPath = localFFmpeg.getAbsolutePath();
                     this.ffprobePath = Paths.get(localFFmpeg.getParent(), "ffprobe.exe").toString();
                 }
            }
        }
    }

    public void saveFFmpegPath(String path) {
        Preferences prefs = Preferences.userNodeForPackage(AudioProcessor.class);
        prefs.put(PREF_FFMPEG_PATH, path);
        loadConfig(); // Tải lại để cập nhật đường dẫn hiện tại
    }

    public String getFFmpegPathFromConfig() {
         Preferences prefs = Preferences.userNodeForPackage(AudioProcessor.class);
         return prefs.get(PREF_FFMPEG_PATH, "ffmpeg"); // Trả về ffmpeg nếu chưa set
    }

    public double getAudioDuration(String inputFile) {
        ProcessBuilder pb = new ProcessBuilder(
            ffprobePath,
            "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            inputFile
        );
        System.out.println("Executing FFPROBE: " + String.join(" ", pb.command()));
        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return Double.parseDouble(output.toString().trim());
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    System.err.println("FFPROBE Error Output:");
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                }
                System.err.println("FFPROBE Lấy thời lượng thất bại, exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException | NumberFormatException e) {
            System.err.println("Lỗi khi chạy FFPROBE: " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }


    public String cutAudio(String inputFile, double startTimeSeconds, double endTimeSeconds, String outputDir, Consumer<String> logger) {
        File outDirFile = new File(outputDir);
        if (!outDirFile.exists()) {
            outDirFile.mkdirs();
        }

        String outputFileName = "segment_" + UUID.randomUUID().toString() + ".mp3";
        String outputFilePath = new File(outputDir, outputFileName).getAbsolutePath();

        String startTimeFormatted = TimeConverter.secondsToFFmpegTime(startTimeSeconds);
        String endTimeFormatted = TimeConverter.secondsToFFmpegTime(endTimeSeconds);

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", inputFile,
                "-ss", startTimeFormatted,
                "-to", endTimeFormatted,
                "-c", "copy", // Sao chép codec, nhanh hơn
                "-y",         // Ghi đè file output nếu đã tồn tại
                "-hide_banner", // Ẩn thông tin banner của ffmpeg
                "-loglevel", "error", // Chỉ log lỗi
                outputFilePath
        );

        logger.accept("Đang cắt: " + inputFile + " từ " + startTimeFormatted + " đến " + endTimeFormatted);
        logger.accept("Lệnh FFMPEG: " + String.join(" ", pb.command()));


        try {
            Process process = pb.start();
            // Đọc output/error stream để tránh process bị block và để log
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), logger::accept);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), err -> logger.accept("FFMPEG_ERR: " + err));
            new Thread(outputGobbler).start();
            new Thread(errorGobbler).start();

            if (!process.waitFor(60, TimeUnit.SECONDS)) { // Timeout 60 giây
                 process.destroyForcibly();
                 logger.accept("FFMPEG Cắt file quá thời gian.");
                 return null;
            }

            if (process.exitValue() == 0) {
                logger.accept("Cắt thành công: " + outputFilePath);
                return outputFilePath;
            } else {
                logger.accept("FFMPEG Cắt file thất bại, exit code: " + process.exitValue());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            logger.accept("Lỗi khi chạy FFMPEG để cắt: " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public String concatAudios(List<String> segmentFilePaths, String outputDir, String finalOutputName, Consumer<String> logger) {
        File outDirFile = new File(outputDir);
         if (!outDirFile.exists()) {
            outDirFile.mkdirs();
        }

        String outputFilePath = new File(outputDir, finalOutputName).getAbsolutePath();
        File listFile = new File(outDirFile, "concat_list_" + UUID.randomUUID().toString() + ".txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(listFile))) {
            for (String segmentPath : segmentFilePaths) {
                // FFMPEG yêu cầu đường dẫn an toàn, đặc biệt trên Windows
                // và dấu nháy đơn cho đường dẫn có khoảng trắng
                writer.println("file '" + new File(segmentPath).getAbsolutePath().replace("\\", "/") + "'");
            }
        } catch (IOException e) {
            logger.accept("Lỗi tạo file list cho FFMPEG: " + e.getMessage());
            return null;
        }

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-f", "concat",
                "-safe", "0",
                "-i", listFile.getAbsolutePath(),
                "-c", "copy",
                "-y",
                "-hide_banner",
                "-loglevel", "error",
                outputFilePath
        );
        logger.accept("Đang ghép các đoạn thành: " + outputFilePath);
        logger.accept("Lệnh FFMPEG: " + String.join(" ", pb.command()));

        try {
            Process process = pb.start();
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), logger::accept);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), err -> logger.accept("FFMPEG_ERR: " + err));
            new Thread(outputGobbler).start();
            new Thread(errorGobbler).start();

            if (!process.waitFor(180, TimeUnit.SECONDS)) { // Timeout 3 phút
                 process.destroyForcibly();
                 logger.accept("FFMPEG Ghép file quá thời gian.");
                 return null;
            }

            if (process.exitValue() == 0) {
                logger.accept("Ghép thành công: " + outputFilePath);
                return outputFilePath;
            } else {
                logger.accept("FFMPEG Ghép file thất bại, exit code: " + process.exitValue());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            logger.accept("Lỗi khi chạy FFMPEG để ghép: " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (!listFile.delete()) {
                 logger.accept("Không thể xóa file tạm: " + listFile.getAbsolutePath());
            }
        }
    }
    // Lớp nội bộ để xử lý stream output/error của process
    static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
    }
}
package com.yourcompany.mp3joiner.service;

import com.yourcompany.mp3joiner.db.DatabaseManager;
import com.yourcompany.mp3joiner.db.SentenceInfo;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.prefs.Preferences;


public class AIService {
    private Model model;
    private final Gson gson = new Gson();
    private String modelPath; // = "src/main/resources/models/vosk-model-small-vi-0.4"; // Cần cho người dùng cấu hình
    private static final String PREF_VOSK_MODEL_PATH = "vosk_model_path";


    public AIService() {
        loadConfigAndModel();
    }

    private void loadConfigAndModel() {
        Preferences prefs = Preferences.userNodeForPackage(AIService.class);
        // Thử tìm trong thư mục làm việc của ứng dụng nếu người dùng copy vào đó
        File localModelDir = new File("models/vosk-model-small-vi-0.4"); // Tên model ví dụ
        String defaultModelPath = "";
        if(localModelDir.exists() && localModelDir.isDirectory()){
            defaultModelPath = localModelDir.getAbsolutePath();
        }

        this.modelPath = prefs.get(PREF_VOSK_MODEL_PATH, defaultModelPath);

        if (this.modelPath != null && !this.modelPath.isEmpty()) {
            try {
                LibVosk.setLogLevel(LogLevel.WARNINGS);
                File modelDir = new File(this.modelPath);
                if (modelDir.exists() && modelDir.isDirectory()) {
                    this.model = new Model(this.modelPath);
                    System.out.println("Vosk Model loaded successfully from: " + this.modelPath);
                } else {
                     System.err.println("Vosk Model path does not exist or is not a directory: " + this.modelPath);
                     this.model = null;
                }
            } catch (UnsatisfiedLinkError | Exception e) { // UnsatisfiedLinkError nếu native lib của Vosk không load được
                System.err.println("Lỗi tải Vosk Model hoặc thư viện native: " + e.getMessage());
                e.printStackTrace();
                this.model = null;
            }
        } else {
            System.err.println("Vosk Model path is not configured.");
            this.model = null;
        }
    }

    public void saveModelPath(String path) {
        Preferences prefs = Preferences.userNodeForPackage(AIService.class);
        prefs.put(PREF_VOSK_MODEL_PATH, path);
        loadConfigAndModel(); // Tải lại model
    }

    public String getModelPathFromConfig() {
         Preferences prefs = Preferences.userNodeForPackage(AIService.class);
         File localModelDir = new File("models/vosk-model-small-vi-0.4");
         String defaultModelPath = "";
         if(localModelDir.exists() && localModelDir.isDirectory()){
             defaultModelPath = localModelDir.getAbsolutePath();
         }
         return prefs.get(PREF_VOSK_MODEL_PATH, defaultModelPath);
    }


    public boolean isModelReady() {
        return this.model != null;
    }

    // Hàm này cần FFMPEG để convert MP3 sang WAV PCM 16kHz mono
    private File convertMp3ToWavPcm(String mp3FilePath, String tempDir, Consumer<String> logger) {
        AudioProcessor audioProcessor = new AudioProcessor(); // Sử dụng AudioProcessor đã có
        String tempWavName = "temp_vosk_input_" + UUID.randomUUID().toString() + ".wav";
        File tempWavFile = new File(tempDir, tempWavName);

        ProcessBuilder pb = new ProcessBuilder(
            audioProcessor.getFFmpegPathFromConfig().replace("ffmpeg.exe", "ffmpeg").replace("ffmpeg", "ffmpeg"), // Đảm bảo dùng ffmpeg
            "-i", mp3FilePath,
            "-ar", "16000", // Sample rate 16kHz
            "-ac", "1",     // Mono channel
            "-f", "s16le",  // Định dạng PCM 16-bit signed little-endian
            "-y",
            "-hide_banner",
            "-loglevel", "error",
            tempWavFile.getAbsolutePath()
        );
        logger.accept("Đang chuyển MP3 sang WAV cho AI: " + mp3FilePath);
        logger.accept("Lệnh FFMPEG: " + String.join(" ", pb.command()));
        try {
            Process process = pb.start();
            AudioProcessor.StreamGobbler outputGobbler = new AudioProcessor.StreamGobbler(process.getInputStream(), logger::accept);
            AudioProcessor.StreamGobbler errorGobbler = new AudioProcessor.StreamGobbler(process.getErrorStream(), err -> logger.accept("FFMPEG_CONVERT_ERR: " + err));
            new Thread(outputGobbler).start();
            new Thread(errorGobbler).start();

            if (!process.waitFor(120, TimeUnit.SECONDS)) { // Timeout
                process.destroyForcibly();
                logger.accept("FFMPEG chuyển đổi MP3 sang WAV quá thời gian.");
                return null;
            }
            if (process.exitValue() == 0) {
                logger.accept("Chuyển đổi MP3 sang WAV thành công: " + tempWavFile.getAbsolutePath());
                return tempWavFile;
            } else {
                logger.accept("FFMPEG chuyển đổi MP3 sang WAV thất bại.");
                return null;
            }
        } catch (IOException | InterruptedException e) {
            logger.accept("Lỗi khi chuyển đổi MP3 sang WAV: " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }


    public List<SentenceInfo> extractSentences(int fileId, String mp3FilePath, Consumer<String> progressConsumer) {
        List<SentenceInfo> extractedSentences = new ArrayList<>();
        if (!isModelReady()) {
            progressConsumer.accept("Lỗi: Vosk model chưa sẵn sàng. Vui lòng kiểm tra cấu hình đường dẫn model.");
            return extractedSentences;
        }

        File tempWavFile = null;
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            progressConsumer.accept("Chuẩn bị file âm thanh cho AI...");
            tempWavFile = convertMp3ToWavPcm(mp3FilePath, tempDir, progressConsumer);

            if (tempWavFile == null || !tempWavFile.exists()) {
                progressConsumer.accept("Lỗi: Không thể chuyển đổi MP3 sang định dạng WAV phù hợp cho AI.");
                return extractedSentences;
            }
            progressConsumer.accept("Bắt đầu trích xuất với AI...");

            try (InputStream ais = new BufferedInputStream(new FileInputStream(tempWavFile));
                 Recognizer recognizer = new Recognizer(model, 16000f)) { // 16kHz sample rate

                recognizer.setWords(true); // Yêu cầu thông tin từng từ (bao gồm start, end time)
                // recognizer.setPartialWords(true); // Nếu muốn lấy kết quả từng phần

                int nbytes;
                byte[] buffer = new byte[4096];
                long totalBytesRead = 0;
                long totalFileSize = tempWavFile.length();

                while ((nbytes = ais.read(buffer)) >= 0) {
                    if (recognizer.acceptWaveForm(buffer, nbytes)) {
                        String partialResultJson = recognizer.getResult(); //getResult trả về kết quả của đoạn đã xử lý
                        // System.out.println("Partial JSON: " + partialResultJson); // Debug
                        processVoskResult(fileId, partialResultJson, extractedSentences, false);
                    } else {
                        // String partial = recognizer.getPartialResult(); // Kết quả đang nhận dạng
                        // System.out.println("Current partial: " + partial); // Debug
                    }
                    totalBytesRead += nbytes;
                    if (totalFileSize > 0) {
                         int percent = (int) ((totalBytesRead * 100) / totalFileSize);
                         progressConsumer.accept("Đang xử lý AI: " + percent + "%");
                    }
                }
                String finalResultJson = recognizer.getFinalResult(); // Kết quả cuối cùng của toàn bộ file
                // System.out.println("Final JSON: " + finalResultJson); // Debug
                processVoskResult(fileId, finalResultJson, extractedSentences, true);
                progressConsumer.accept("AI hoàn tất trích xuất.");
            }

        } catch (Exception e) {
            progressConsumer.accept("Lỗi nghiêm trọng khi trích xuất âm thanh với AI: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (tempWavFile != null && tempWavFile.exists()) {
                if(!tempWavFile.delete()){
                    progressConsumer.accept("Không thể xóa file WAV tạm: " + tempWavFile.getAbsolutePath());
                }
            }
        }
        return extractedSentences;
    }

    private void processVoskResult(int fileId, String jsonResult, List<SentenceInfo> sentenceList, boolean isFinal) {
        if (jsonResult == null || jsonResult.trim().isEmpty()) return;

        try {
            JsonObject resultObj = JsonParser.parseString(jsonResult).getAsJsonObject();
            // Vosk có thể trả về một mảng "result" chứa các từ, hoặc chỉ một trường "text"
            // Nếu isFinal=true và có trường "text" thì đó là toàn bộ văn bản, ko có timestamp chi tiết cho câu
            // Ta cần mảng "result" để lấy timestamp từng từ và ghép thành câu.

            if (resultObj.has("result")) {
                JsonArray wordsArray = resultObj.getAsJsonArray("result");
                if (wordsArray.size() == 0) return;

                StringBuilder currentSentenceText = new StringBuilder();
                double sentenceStartTime = -1;
                double wordEndTime = 0;
                double lastWordConf = 0;

                for (int i = 0; i < wordsArray.size(); i++) {
                    JsonObject wordObj = wordsArray.get(i).getAsJsonObject();
                    String word = wordObj.get("word").getAsString();
                    double start = wordObj.get("start").getAsDouble();
                    double end = wordObj.get("end").getAsDouble();
                    // double conf = wordObj.get("conf").getAsDouble(); // Độ tin cậy

                    if (sentenceStartTime < 0) { // Bắt đầu câu mới
                        sentenceStartTime = start;
                    }
                    currentSentenceText.append(word).append(" ");
                    wordEndTime = end;
                    // lastWordConf = conf;

                    // Heuristic để xác định cuối câu:
                    // 1. Từ hiện tại là từ cuối cùng trong mảng wordsArray (nếu isFinal=true)
                    // 2. Khoảng lặng lớn giữa từ hiện tại và từ tiếp theo
                    // 3. Đã đạt một số lượng từ nhất định (ví dụ 15-20 từ) và từ hiện tại kết thúc bằng dấu câu (., ?, !)
                    //    (Vosk không phải lúc nào cũng nhận diện dấu câu tốt)

                    boolean endOfCurrentSentence = false;
                    if (isFinal && i == wordsArray.size() - 1) { // Từ cuối cùng của kết quả cuối
                        endOfCurrentSentence = true;
                    } else if (i < wordsArray.size() - 1) {
                        JsonObject nextWordObj = wordsArray.get(i + 1).getAsJsonObject();
                        double nextWordStart = nextWordObj.get("start").getAsDouble();
                        if (nextWordStart - end > 0.7) { // Khoảng lặng lớn hơn 0.7 giây -> coi như hết câu
                            endOfCurrentSentence = true;
                        }
                    }

                    // Đơn giản hơn: nếu isFinal=false, mỗi "result" là một câu/phân đoạn
                    if (!isFinal) endOfCurrentSentence = true;


                    if (endOfCurrentSentence && currentSentenceText.length() > 0) {
                        String finalSentence = currentSentenceText.toString().trim();
                        // Kiểm tra trùng lặp với câu cuối cùng trong list (nếu có)
                        // Đôi khi Vosk trả về các đoạn gối nhau hoặc lặp lại khi xử lý stream
                        if (sentenceList.isEmpty() || 
                           !sentenceList.get(sentenceList.size()-1).getSentenceText().endsWith(finalSentence.substring(0, Math.min(10, finalSentence.length()))) ) {
                            sentenceList.add(new SentenceInfo(fileId, sentenceStartTime, wordEndTime, finalSentence));
                        }
                        currentSentenceText.setLength(0); // Reset cho câu mới
                        sentenceStartTime = -1;
                    }
                }
            } else if (isFinal && resultObj.has("text")) { // Chỉ có text, không có word-level timestamps
                String fullText = resultObj.get("text").getAsString();
                if (!fullText.isEmpty()) {
                    // Lấy toàn bộ thời lượng file làm mốc (Cần truyền vào) hoặc 0 - (-1)
                    // Cách này không lý tưởng vì không có timestamp chi tiết
                    // Tạm thời có thể thêm 1 câu lớn, người dùng tự chia
                     double fileDuration = DatabaseManager.getInstance().getFileDuration(fileId);
                     if (fileDuration == 0) fileDuration = 3600; // Giả định 1h nếu ko lấy đc

                     sentenceList.add(new SentenceInfo(fileId, 0, fileDuration, "[TOÀN BỘ VĂN BẢN] " + fullText));
                     System.err.println("AI chỉ trả về toàn bộ text, không có timestamp chi tiết cho từng câu.");
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi xử lý JSON từ Vosk: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
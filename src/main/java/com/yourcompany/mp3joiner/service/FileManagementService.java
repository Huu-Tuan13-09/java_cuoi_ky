// package com.yourcompany.mp3joiner.service;

// import com.yourcompany.mp3joiner.db.DatabaseManager;
// import com.yourcompany.mp3joiner.db.FileInfo;
// import com.yourcompany.mp3joiner.db.SentenceInfo;
// import com.yourcompany.mp3joiner.util.TimeConverter;
// import com.yourcompany.mp3joiner.db.CompositionInfo;

// import java.io.File;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.function.Consumer;

// public class FileManagementService {
//     private DatabaseManager dbManager;
//     private AIService aiService;
//     private AudioProcessor audioProcessor;

//     public FileManagementService() {
//         this.dbManager = DatabaseManager.getInstance();
//         this.aiService = new AIService();
//         this.audioProcessor = new AudioProcessor();
//     }

//     public FileInfo addMp3File(String filePath) {
//         File file = new File(filePath);
//         if (!file.exists() || !file.isFile()) {
//             System.err.println("File không tồn tại: " + filePath);
//             return null;
//         }
//         String fileName = file.getName();
//         // Lấy thời lượng file bằng ffprobe
//         double duration = audioProcessor.getAudioDuration(filePath);
//         if (duration <= 0) {
//              System.err.println("Không thể lấy thời lượng hoặc file không hợp lệ: " + filePath);
//              // return null; // Hoặc cho phép thêm với duration = 0 và cập nhật sau
//         }

//         FileInfo fileInfo = new FileInfo(fileName, filePath, duration);
//         int id = dbManager.addFile(fileInfo);
//         if (id != -1) {
//             fileInfo.setId(id);
//             return fileInfo;
//         }
//         return null;
//     }

//     public List<FileInfo> getAllMp3Files() {
//         return dbManager.getAllFiles();
//     }

//     public boolean deleteMp3File(int fileId) {
//         // Cân nhắc xóa file vật lý hay không? Theo yêu cầu chỉ xóa link DB
//         return dbManager.deleteFile(fileId); // Sẽ cascade delete sentences
//     }

//     public List<SentenceInfo> extractSentencesFromFile(FileInfo fileInfo, Consumer<String> progressConsumer) {
//         // Xóa các câu cũ (nếu có) trước khi trích xuất lại
//         dbManager.deleteSentencesByFileId(fileInfo.getId());

//         List<SentenceInfo> sentences = aiService.extractSentences(fileInfo.getId(), fileInfo.getFilePath(), progressConsumer);
//         for (SentenceInfo sentence : sentences) {
//             dbManager.addSentence(sentence); // Lưu vào DB
//         }
//         return sentences; // Trả về danh sách đã có ID
//     }

//     public List<SentenceInfo> getSentencesForFile(int fileId) {
//         return dbManager.getSentencesByFileId(fileId);
//     }

//     public boolean updateSentence(SentenceInfo sentence) {
//         return dbManager.updateSentence(sentence);
//     }

//     public static class SegmentToMerge {
//         public String originalFilePath;
//         public double startTimeSeconds;
//         public double endTimeSeconds;

//         public SegmentToMerge(String originalFilePath, double startTimeSeconds, double endTimeSeconds) {
//             this.originalFilePath = originalFilePath;
//             this.startTimeSeconds = startTimeSeconds;
//             this.endTimeSeconds = endTimeSeconds;
//         }
//     }

//     public CompositionInfo mergeAudioSegments(List<SegmentToMerge> segments, String outputDir, String outputFileName, Consumer<String> logger) {
//         List<String> tempCutFiles = new ArrayList<>();
//         boolean success = true;

//         logger.accept("Bắt đầu quá trình ghép file...");
//         for (SegmentToMerge segment : segments) {
//             logger.accept("Đang chuẩn bị đoạn: " + new File(segment.originalFilePath).getName() +
//                           " từ " + TimeConverter.secondsToHmmss(segment.startTimeSeconds) +
//                           " đến " + TimeConverter.secondsToHmmss(segment.endTimeSeconds));
//             String cutFile = audioProcessor.cutAudio(
//                     segment.originalFilePath,
//                     segment.startTimeSeconds,
//                     segment.endTimeSeconds,
//                     outputDir, // Hoặc một thư mục tạm riêng
//                     logger
//             );
//             if (cutFile != null) {
//                 tempCutFiles.add(cutFile);
//             } else {
//                 logger.accept("Lỗi: Không thể cắt đoạn từ file " + segment.originalFilePath);
//                 success = false;
//                 break;
//             }
//         }

//         String finalMergedPath = null;
//         if (success && !tempCutFiles.isEmpty()) {
//             finalMergedPath = audioProcessor.concatAudios(tempCutFiles, outputDir, outputFileName, logger);
//         } else if (tempCutFiles.isEmpty()) {
//              logger.accept("Lỗi: Không có đoạn nào để ghép.");
//              success = false;
//         }


//         // Xóa các file tạm đã cắt
//         for (String tempFile : tempCutFiles) {
//             File f = new File(tempFile);
//             if (f.exists()) {
//                 if (!f.delete()) {
//                     logger.accept("Cảnh báo: Không thể xóa file tạm " + tempFile);
//                 }
//             }
//         }

//         if (finalMergedPath != null) {
//             logger.accept("Ghép thành công! File được lưu tại: " + finalMergedPath);
//             CompositionInfo compInfo = new CompositionInfo(outputFileName, finalMergedPath);
//             int compId = dbManager.addComposition(compInfo);
//             if (compId != -1) {
//                 compInfo.setId(compId);
//                 return compInfo;
//             } else {
//                 logger.accept("Lỗi: Lưu thông tin file ghép vào CSDL thất bại.");
//                 // Cân nhắc xóa file đã ghép nếu không lưu được vào DB?
//             }
//         } else {
//              logger.accept("Ghép file thất bại.");
//         }
//         return null;
//     }

//     public AIService getAiService() { return aiService; }
//     public AudioProcessor getAudioProcessor() { return audioProcessor; }

// }

package com.yourcompany.mp3joiner.service;

import com.yourcompany.mp3joiner.db.DatabaseManager;
import com.yourcompany.mp3joiner.db.FileInfo;
import com.yourcompany.mp3joiner.db.SentenceInfo;
import com.yourcompany.mp3joiner.util.TimeConverter;
import com.yourcompany.mp3joiner.db.CompositionInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FileManagementService {
    private DatabaseManager dbManager;
    private AIService aiService;
    private AudioProcessor audioProcessor;

    public FileManagementService() {
        this.dbManager = DatabaseManager.getInstance();
        this.aiService = new AIService();
        // Khởi tạo AudioProcessor ở đây sẽ gọi loadConfig() của nó,
        // đảm bảo ffmpegPath và ffprobePath được cập nhật từ Preferences hoặc giá trị mặc định.
        this.audioProcessor = new AudioProcessor();
    }

    public FileInfo addMp3File(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File không tồn tại: " + filePath);
            return null;
        }
        String fileName = file.getName();
        // Lấy thời lượng file bằng ffprobe
        // AudioProcessor đã được sửa để loadConfig() và có ffprobePath đúng
        double duration = audioProcessor.getAudioDuration(filePath); // Sử dụng ffprobePath đã được loadConfig() xử lý
        if (duration <= 0 && !filePath.toLowerCase().endsWith(".wav")) { // Cho phép file WAV có duration 0 nếu ffprobe không đọc được, nhưng MP3 thì nên có
             System.err.println("Cảnh báo: Không thể lấy thời lượng hoặc file không hợp lệ (thời lượng <=0): " + filePath + ". File vẫn được thêm.");
             // Không return null nữa, cho phép thêm file với duration = 0 hoặc giá trị mặc định
             // Bạn có thể quyết định không cho thêm file nếu duration <= 0
        }

        FileInfo fileInfo = new FileInfo(fileName, filePath, duration);
        int id = dbManager.addFile(fileInfo);
        if (id != -1) {
            fileInfo.setId(id);
            // Log ra đây hoặc trong MainFrame sau khi gọi hàm này
            System.out.println("Đã thêm file vào DB: " + fileName + " (ID: " + id + ", Thời lượng: " + TimeConverter.secondsToHmmss(duration) + ")");
            return fileInfo;
        }
        System.err.println("Lỗi: Không thể thêm file " + fileName + " vào CSDL.");
        return null;
    }

    public List<FileInfo> getAllMp3Files() {
        return dbManager.getAllFiles();
    }

    public boolean deleteMp3File(int fileId) {
        return dbManager.deleteFile(fileId);
    }

    public List<SentenceInfo> extractSentencesFromFile(FileInfo fileInfo, Consumer<String> progressConsumer) {
        dbManager.deleteSentencesByFileId(fileInfo.getId());

        List<SentenceInfo> sentences = aiService.extractSentences(fileInfo.getId(), fileInfo.getFilePath(), progressConsumer);
        // Lọc bỏ các câu có thời gian bắt đầu >= thời gian kết thúc TRƯỚC KHI LƯU VÀO DB
        List<SentenceInfo> validSentences = new ArrayList<>();
        for (SentenceInfo sentence : sentences) {
            if (sentence.getStartTimeSeconds() < sentence.getEndTimeSeconds()) {
                dbManager.addSentence(sentence);
                validSentences.add(sentence); // Chỉ thêm câu hợp lệ vào danh sách trả về
            } else {
                progressConsumer.accept("Cảnh báo: Bỏ qua câu không hợp lệ (thời gian bắt đầu >= kết thúc): '" + sentence.getSentenceText() + "' từ " + sentence.getStartTimeSeconds() + " đến " + sentence.getEndTimeSeconds());
            }
        }
        return validSentences; // Trả về danh sách các câu đã được lọc và lưu vào DB
    }

    public List<SentenceInfo> getSentencesForFile(int fileId) {
        return dbManager.getSentencesByFileId(fileId);
    }

    public boolean updateSentence(SentenceInfo sentence) {
        // Thêm kiểm tra thời gian hợp lệ ở đây trước khi update
        if (sentence.getStartTimeSeconds() >= sentence.getEndTimeSeconds()) {
            System.err.println("Lỗi: Không thể cập nhật câu. Thời gian bắt đầu phải nhỏ hơn thời gian kết thúc.");
            return false;
        }
        return dbManager.updateSentence(sentence);
    }

    public static class SegmentToMerge {
        public String originalFilePath;
        public double startTimeSeconds;
        public double endTimeSeconds;
        public String originalFileName; // Thêm để log dễ hiểu hơn

        public SegmentToMerge(String originalFilePath, double startTimeSeconds, double endTimeSeconds, String originalFileName) {
            this.originalFilePath = originalFilePath;
            this.startTimeSeconds = startTimeSeconds;
            this.endTimeSeconds = endTimeSeconds;
            this.originalFileName = originalFileName;
        }
    }

    public CompositionInfo mergeAudioSegments(List<SegmentToMerge> segments, String outputDir, String outputFileName, Consumer<String> logger) {
        List<String> tempCutFiles = new ArrayList<>();
        boolean overallSuccess = true; // Dùng biến này để theo dõi toàn bộ quá trình

        logger.accept("Bắt đầu quá trình ghép file...");
        if (segments.isEmpty()) {
            logger.accept("Lỗi: Danh sách đoạn cần ghép rỗng.");
            return null;
        }

        for (SegmentToMerge segment : segments) {
            logger.accept("Đang chuẩn bị đoạn: " + segment.originalFileName + // Sử dụng tên file gốc để log
                          " từ " + TimeConverter.secondsToHmmss(segment.startTimeSeconds) +
                          " đến " + TimeConverter.secondsToHmmss(segment.endTimeSeconds));

            // KIỂM TRA THỜI GIAN HỢP LỆ TRƯỚC KHI CẮT
            if (segment.startTimeSeconds >= segment.endTimeSeconds) {
                logger.accept("Lỗi: Đoạn cắt không hợp lệ (thời gian bắt đầu >= kết thúc). Bỏ qua đoạn này từ file " + segment.originalFileName);
                // Không đặt overallSuccess = false ở đây, chỉ bỏ qua đoạn không hợp lệ
                // Nếu tất cả các đoạn đều không hợp lệ, tempCutFiles sẽ rỗng và việc ghép sẽ thất bại ở dưới
                continue; // Bỏ qua đoạn này và xử lý đoạn tiếp theo
            }

            String cutFile = audioProcessor.cutAudio(
                    segment.originalFilePath,
                    segment.startTimeSeconds,
                    segment.endTimeSeconds,
                    outputDir,
                    logger
            );

            if (cutFile != null) {
                tempCutFiles.add(cutFile);
            } else {
                // Nếu audioProcessor.cutAudio trả về null, nghĩa là việc cắt đã thất bại
                logger.accept("Lỗi nghiêm trọng: Không thể cắt đoạn từ file " + segment.originalFileName + ". Quá trình ghép sẽ dừng lại.");
                overallSuccess = false; // Đánh dấu toàn bộ quá trình thất bại
                break; // Dừng vòng lặp nếu một đoạn không cắt được
            }
        }

        String finalMergedPath = null;
        // Chỉ tiến hành ghép nếu không có lỗi nghiêm trọng nào ở bước cắt VÀ có ít nhất một file tạm được tạo ra
        if (overallSuccess && !tempCutFiles.isEmpty()) {
            finalMergedPath = audioProcessor.concatAudios(tempCutFiles, outputDir, outputFileName, logger);
            if (finalMergedPath == null) { // Nếu concatAudios thất bại
                overallSuccess = false;
            }
        } else if (tempCutFiles.isEmpty()) {
            // Điều này xảy ra nếu tất cả các segment đều không hợp lệ hoặc danh sách segments ban đầu rỗng
            logger.accept("Lỗi: Không có đoạn hợp lệ nào để ghép.");
            overallSuccess = false;
        }
        // (Nếu overallSuccess là false do break ở vòng lặp cắt, tempCutFiles có thể không rỗng nhưng ta vẫn không ghép)


        // Xóa các file tạm đã cắt (luôn thực hiện, dù ghép thành công hay không)
        for (String tempFile : tempCutFiles) {
            File f = new File(tempFile);
            if (f.exists()) {
                if (!f.delete()) {
                    logger.accept("Cảnh báo: Không thể xóa file tạm " + tempFile);
                }
            }
        }

        if (overallSuccess && finalMergedPath != null) {
            logger.accept("Ghép thành công! File được lưu tại: " + finalMergedPath);
            CompositionInfo compInfo = new CompositionInfo(outputFileName, finalMergedPath);
            int compId = dbManager.addComposition(compInfo);
            if (compId != -1) {
                compInfo.setId(compId);
                return compInfo;
            } else {
                logger.accept("Lỗi: Lưu thông tin file ghép vào CSDL thất bại.");
                // Cân nhắc xóa file đã ghép nếu không lưu được vào DB?
                // Ví dụ: new File(finalMergedPath).delete();
                return null; // Trả về null nếu không lưu được vào DB
            }
        } else {
            // Log này đã được xử lý ở các nhánh trên (ví dụ: "Lỗi: Không có đoạn nào để ghép.")
            // hoặc "Lỗi nghiêm trọng: Không thể cắt đoạn..."
             logger.accept("Ghép file thất bại chung cuộc."); // Thêm một log chung
            return null;
        }
    }

    public AIService getAiService() { return aiService; }
    public AudioProcessor getAudioProcessor() { return audioProcessor; }
}
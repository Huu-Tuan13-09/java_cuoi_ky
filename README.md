# MP3Joiner - Chương trình Nối File MP3 Hỗ trợ AI

Ứng dụng Java Desktop cho phép quản lý các file MP3, trích xuất nội dung văn bản từ giọng nói bằng AI (Vosk), chỉnh sửa các đoạn trích xuất, và ghép nối chúng thành một file MP3 mới (sử dụng FFMPEG). Cơ sở dữ liệu được sử dụng là Microsoft SQL Server.

## Mục lục

1.  [Tính năng chính](#tính-năng-chính)
2.  [Cấu trúc thư mục dự án](#cấu-trúc-thư-mục-dự-án)
3.  [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
4.  [Hướng dẫn Cài đặt Chi Tiết](#hướng-dẫn-cài-đặt-chi-tiết)
    * [Bước 1: Cài đặt các phần mềm nền tảng (Nếu chưa có)](#bước-1-cài-đặt-các-phần-mềm-nền-tảng-nếu-chưa-có)
        * [Java Development Kit (JDK)](#java-development-kit-jdk)
        * [Apache Maven (Bắt buộc để build dự án)](#apache-maven-bắt-buộc-để-build-dự-án)
        * [Microsoft SQL Server](#microsoft-sql-server)
    * [Bước 2: Chuẩn bị các tài nguyên cho ứng dụng (Đã bao gồm trong dự án)](#bước-2-chuẩn-bị-các-tài-nguyên-cho-ứng-dụng-đã-bao-gồm-trong-dự-án)
        * [FFMPEG](#ffmpeg)
        * [Model AI Vosk (Tiếng Việt)](#model-ai-vosk-tiếng-việt)
    * [Bước 3: Thiết lập Cơ sở dữ liệu](#bước-3-thiết-lập-cơ-sở-dữ-liệu)
    * [Bước 4: Build Dự án (Tạo file JAR thực thi)](#bước-4-build-dự-án-tạo-file-jar-thực-thi)
    * [Bước 5: Chạy Ứng dụng](#bước-5-chạy-ứng-dụng)
5.  [Hướng dẫn Sử dụng Ứng dụng](#hướng-dẫn-sử-dụng-ứng-dụng)
    * [Cấu hình ban đầu trong ứng dụng](#cấu-hình-ban-đầu-trong-ứng-dụng)
    * [Quản lý File MP3](#quản-lý-file-mp3)
    * [Trích xuất Câu (AI)](#trích-xuất-câu-ai)
    * [Chỉnh sửa Câu thủ công](#chỉnh-sửa-câu-thủ-công)
    * [Ghép File MP3](#ghép-file-mp3)
    * [Đóng ứng dụng](#đóng-ứng-dụng)
6.  [Khắc phục sự cố thường gặp](#khắc-phục-sự-cố-thường-gặp)

---

## 1. Tính năng chính

* Quản lý danh sách các file MP3 từ ổ cứng, lưu trữ đường dẫn trong cơ sở dữ liệu SQL Server.
* Sử dụng thư viện AI Vosk để trích xuất nội dung giọng nói thành văn bản từ file MP3.
* Xác định thời gian bắt đầu và kết thúc (timestamp) cho từng câu/phân đoạn được AI trích xuất.
* Lưu trữ thông tin câu và timestamp vào CSDL.
* Cho phép người dùng chỉnh sửa thủ công nội dung câu và timestamp nếu AI nhận dạng chưa chính xác.
* Sử dụng FFMPEG để cắt các đoạn âm thanh từ các file MP3 gốc dựa trên timestamp đã chọn.
* Ghép các đoạn âm thanh đã cắt theo thứ tự mong muốn để tạo thành một file MP3 mới.
* Lưu trữ thông tin file MP3 đã ghép vào ổ cứng và đường dẫn vào CSDL.
* Giao diện người dùng đồ họa (GUI) Java Swing.

---

## 2. Cấu trúc thư mục dự án

Dự án được tổ chức theo cấu trúc chuẩn của Maven. Các tài nguyên quan trọng như FFMPEG và Model AI Vosk được đặt sẵn trong thư mục dự án để thuận tiện cho việc triển khai.

D:/JAVA_CUOI_KY/
|
+-- .vscode/
|   +-- launch.json
|
+-- ffmpeg/
|   +-- bin/
|       +-- ffmpeg.exe
|       +-- ffprobe.exe
|       +-- (và các file .dll khác)
|
+-- src/
|   +-- main## 2. Cấu trúc thư mục dự án

Dự án được tổ chức theo cấu trúc chuẩn của Maven. Các tài nguyên quan trọng như FFMPEG và Model AI Vosk được đặt sẵn trong thư mục dự án để thuận tiện cho việc triển khai.

```text
D:/JAVA_CUOI_KY/  (Thư mục gốc của dự án)
│
├─── .vscode/                      // Thư mục cài đặt riêng của VS Code cho dự án
│    └─── launch.json              // Cấu hình chạy và debug từ VS Code
│
├─── ffmpeg/                       // Thư mục chứa FFMPEG
│    └─── bin/                     // Chứa các file thực thi và thư viện của FFMPEG
│            ffmpeg.exe           // File thực thi FFMPEG
│            ffprobe.exe          // Công cụ phân tích media của FFMPEG
│            (và các file .dll cần thiết khác như avcodec-61.dll, avutil-59.dll, etc.)
│
├─── src/                          // Mã nguồn và tài nguyên của dự án
│    ├─── main/                    // Mã nguồn chính và tài nguyên
│    │    ├─── java/               // Thư mục gốc mã nguồn Java
│    │    │    └─── com/
│    │    │         └─── yourcompany/  // Thay "yourcompany" bằng groupId thực tế
│    │    │              └─── mp3joiner/ // Package gốc ứng dụng
│    │    │                   │   MainApp.java              // Điểm khởi chạy ứng dụng
│    │    │                   │
│    │    │                   ├─── db/  // Các lớp liên quan đến CSDL
│    │    │                   │    ├─── CompositionInfo.java
│    │    │                   │    ├─── DatabaseManager.java
│    │    │                   │    ├─── FileInfo.java
│    │    │                   │    └─── SentenceInfo.java
│    │    │                   │
│    │    │                   ├─── service/ // Các lớp logic nghiệp vụ
│    │    │                   │    ├─── AIService.java
│    │    │                   │    ├─── AudioProcessor.java
│    │    │                   │    └─── FileManagementService.java
│    │    │                   │
│    │    │                   ├─── ui/   // Các lớp giao diện người dùng
│    │    │                   │    ├─── MainFrame.java
│    │    │                   │    └─── SettingsDialog.java
│    │    │                   │
│    │    │                   └─── util/ // Các lớp tiện ích
│    │    │                        └─── TimeConverter.java
│    │    │
│    │    └─── resources/            // Tài nguyên không phải code Java
│    │         │   log4j2.xml        // (Tùy chọn) Cấu hình logging
│    │         │
│    │         └─── models/           // Chứa các model AI
│    │              └─── vosk-model-small-vn-0.4/ // Model Vosk tiếng Việt
│    │                   └─── ... (các file và thư mục con của model)
│    │
│    └─── test/                     // (Tùy chọn) Mã nguồn cho unit test (hiện có thể trống)
│
├─── target/                       // Thư mục do Maven tạo ra khi build
│    │   mp3joiner-1.0-SNAPSHOT-jar-with-dependencies.jar // File JAR thực thi
│    │   mp3joiner-1.0-SNAPSHOT.jar                     // File JAR không có thư viện
│    │   ... (các file và thư mục khác)
│
├─── MP3ManagerDB.sql              // Script SQL tạo bảng CSDL
├─── pom.xml                       // File cấu hình Maven
└─── README.md                     // File hướng dẫn này
## 3. Yêu cầu hệ thống

* **Hệ điều hành:** Windows (Ứng dụng được phát triển và thử nghiệm chủ yếu trên Windows).
* **Java:** Java Development Kit (JDK) hoặc Java Runtime Environment (JRE) phiên bản 11 trở lên. (Dự án được biên dịch với target Java 11 và đã được thử nghiệm chạy trên JDK 22).
* **Cơ sở dữ liệu:** Microsoft SQL Server (ví dụ: SQL Server Express Edition 2019 hoặc mới hơn).
* **Phần cứng:**
    * RAM: Tối thiểu 4GB (khuyến nghị 8GB trở lên, đặc biệt khi xử lý AI).
    * Dung lượng ổ cứng trống: Đủ để chứa dự án, các file MP3 và file tạm.

---

## 4. Hướng dẫn Cài đặt Chi Tiết

Vui lòng thực hiện theo các bước sau để cài đặt và chạy ứng dụng từ thư mục dự án `JAVA_CUOI_KY` này.

### Bước 1: Cài đặt các phần mềm nền tảng (Nếu chưa có)

#### Java Development Kit (JDK)
1.  Ứng dụng yêu cầu Java phiên bản 11 trở lên.
2.  Nếu chưa có, tải JDK từ [Oracle](https://www.oracle.com/java/technologies/downloads/) hoặc một nhà cung cấp OpenJDK như [Adoptium Temurin](https://adoptium.net/).
3.  Cài đặt JDK. Thiết lập biến môi trường `JAVA_HOME` và thêm `%JAVA_HOME%\bin` vào `PATH` hệ thống.
4.  Kiểm tra: Mở Command Prompt/PowerShell, gõ `java -version`.

#### Apache Maven (Bắt buộc để build dự án)
1.  Nếu chưa có, tải Apache Maven (ví dụ: phiên bản 3.9.x) từ [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) (chọn file "Binary zip archive").
2.  Giải nén vào một thư mục (ví dụ: `D:\CNPM\apache-maven-3.9.9`).
3.  Thiết lập biến môi trường `MAVEN_HOME` và thêm `%MAVEN_HOME%\bin` vào `PATH` hệ thống.
4.  Khởi động lại Command Prompt/PowerShell và kiểm tra: `mvn -version`.

#### Microsoft SQL Server
1.  Tải và cài đặt SQL Server (ví dụ: SQL Server Express Edition).
2.  Trong quá trình cài đặt, chọn **"Mixed Mode (SQL Server authentication and Windows authentication)"** và đặt mật khẩu cho tài khoản `sa`. **Ghi nhớ mật khẩu này.**
3.  Mở **SQL Server Configuration Manager**:
    * Trong `SQL Server Network Configuration` -> `Protocols for SQLEXPRESS` (hoặc tên instance của bạn).
    * Đảm bảo **TCP/IP** là **Enabled**.
    * Chuột phải vào TCP/IP -> Properties -> tab IP Addresses. Kéo xuống `IPAll`, đặt **TCP Port** là `1433` (xóa giá trị ở `TCP Dynamic Ports`).
    * **Quan trọng:** Khởi động lại dịch vụ `SQL Server (SQLEXPRESS)` trong `services.msc`.
4.  **(Tùy chọn) SQL Server Browser Service:** Trong `services.msc`, đảm bảo `SQL Server Browser` đang "Running" và "Startup Type" là "Automatic" (hữu ích nếu kết nối qua tên instance).

### Bước 2: Kiểm tra các tài nguyên đã có sẵn trong dự án

Thư mục `JAVA_CUOI_KY` bạn nộp đã bao gồm các tài nguyên cần thiết:

* **FFMPEG:** Nằm trong thư mục `JAVA_CUOI_KY\ffmpeg\bin\`. Ứng dụng đã được cấu hình để ưu tiên tìm `ffmpeg.exe` và `ffprobe.exe` tại đây.
    * *Người dùng không cần tải hay cài đặt FFMPEG riêng nếu các file thực thi và DLL cần thiết đã có trong thư mục này.*
* **Model AI Vosk (Tiếng Việt):** Nằm trong thư mục `JAVA_CUOI_KY\src\main\resources\models\vosk-model-small-vn-0.4\`. Ứng dụng đã được cấu hình để ưu tiên tìm model tại đây.
    * *Người dùng không cần tải model Vosk riêng nếu thư mục model này đã đầy đủ.*

### Bước 3: Thiết lập Cơ sở dữ liệu
1.  Mở SQL Server Management Studio (SSMS) và kết nối tới instance SQL Server của bạn (ví dụ: Server name là `localhost,1433` hoặc `localhost\SQLEXPRESS`, sử dụng SQL Server Authentication với user `sa` và mật khẩu đã đặt).
2.  Trong Object Explorer, chuột phải vào "Databases" và chọn "New Database...".
3.  Đặt tên database là `MP3ManagerDB` và nhấn "OK".
4.  Sau khi database `MP3ManagerDB` được tạo, chuột phải vào nó, chọn "New Query".
5.  Mở file `MP3ManagerDB.sql` (nằm trong thư mục gốc dự án `JAVA_CUOI_KY\`) bằng một trình soạn thảo văn bản.
6.  Copy toàn bộ nội dung của file `MP3ManagerDB.sql`.
7.  Dán nội dung script vào cửa sổ Query mới trong SSMS (đã kết nối với context là `MP3ManagerDB`).
8.  Nhấn nút **"Execute"** (hoặc phím `F5`) để chạy script và tạo các bảng (`tbl_files`, `tbl_sentences`, `tbl_compositions`). Kiểm tra cửa sổ "Messages" để đảm bảo không có lỗi và các bảng được tạo thành công.

### Bước 4: Build Dự án (Tạo file JAR thực thi)
1.  Mở Command Prompt hoặc PowerShell.
2.  Di chuyển đến thư mục gốc của dự án `JAVA_CUOI_KY` (ví dụ: `cd D:\JAVA_CUOI_KY`).
3.  Chạy lệnh Maven sau:
    ```bash
    mvn clean package
    ```
4.  Sau khi hoàn tất (`BUILD SUCCESS`), bạn sẽ tìm thấy file JAR trong thư mục `JAVA_CUOI_KY\target\` (ví dụ: `mp3joiner-1.0-SNAPSHOT-jar-with-dependencies.jar`).

### Bước 5: Chạy Ứng dụng
1.  Mở Command Prompt hoặc PowerShell.
2.  Di chuyển đến thư mục `target` của dự án (ví dụ: `cd D:\JAVA_CUOI_KY\target\`).
3.  Chạy ứng dụng bằng lệnh:
    ```bash
    java -jar mp3joiner-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```
    *(Thay thế bằng tên file JAR chính xác nếu có khác biệt).*
4.  Ứng dụng sẽ khởi động. Lần đầu chạy, hoặc nếu cấu hình chưa đúng, bạn sẽ cần thực hiện "Cấu hình ban đầu trong ứng dụng".

---

## 5. Hướng dẫn Sử dụng Ứng dụng

### Cấu hình ban đầu trong ứng dụng
Khi chạy ứng dụng lần đầu, hoặc nếu các đường dẫn/thông tin thay đổi:
1.  Trên giao diện chính, vào menu **`File` -> `Cài đặt...`**.
2.  Trong cửa sổ "Cài đặt":
    * **Đường dẫn FFMPEG (ffmpeg.exe):**
        * Ứng dụng sẽ cố gắng tự tìm trong thư mục `JAVA_CUOI_KY\ffmpeg\bin\`.
        * Nếu không tự tìm thấy, nhấn nút "..." và duyệt đến file `ffmpeg.exe` (ví dụ: `D:\JAVA_CUOI_KY\ffmpeg\bin\ffmpeg.exe`).
    * **Đường dẫn Vosk Model (thư mục):**
        * Ứng dụng sẽ cố gắng tự tìm trong thư mục `JAVA_CUOI_KY\src\main\resources\models\vosk-model-small-vn-0.4\`.
        * Nếu không tự tìm thấy, nhấn nút "..." và duyệt đến thư mục model Vosk đã giải nén (ví dụ: `D:\JAVA_CUOI_KY\src\main\resources\models\vosk-model-small-vn-0.4`).
    * **DB Server:** Nhập `localhost,1433` (khuyến nghị, nếu SQL Server của bạn đang lắng nghe trên cổng 1433).
    * **DB Name:** `MP3ManagerDB`.
    * **DB User:** Tên người dùng SQL Server (ví dụ: `sa`).
    * **DB Password:** Mật khẩu của người dùng đó.
3.  Nhấn nút **"Kiểm tra CSDL"**. Nếu báo "Kết nối CSDL thành công!" là tốt.
4.  Nhấn nút **"Lưu"**.
5.  **Quan trọng:** **Khởi động lại ứng dụng** (đóng hoàn toàn và chạy lại file JAR) để đảm bảo tất cả cài đặt mới được áp dụng.

### Quản lý File MP3
* **Thêm File:** Nhấn nút "Thêm File". Chọn file MP3. Thông tin file và thời lượng sẽ được lưu vào CSDL và hiển thị.
* **Xóa File:** Chọn file trong danh sách, nhấn "Xóa File". File sẽ bị xóa khỏi CSDL.

### Trích xuất Câu (AI)
1.  Chọn một file MP3 từ danh sách.
2.  Nhấn nút "Trích xuất Câu (AI)".
3.  Theo dõi tiến trình trong vùng "Log/Trạng thái".
4.  Các câu trích xuất (với thời gian bắt đầu/kết thúc) sẽ hiển thị trong bảng và lưu vào CSDL. Các câu có thời gian không hợp lệ (bắt đầu >= kết thúc) sẽ được lọc bỏ.

### Chỉnh sửa Câu thủ công
1.  Click đúp vào một câu trong bảng "Các câu đã trích xuất".
2.  Sửa thời gian (đảm bảo bắt đầu < kết thúc) hoặc nội dung.
3.  Nhấn "OK". Thay đổi được cập nhật vào CSDL.

### Ghép File MP3
1.  Từ bảng "Các câu đã trích xuất", chọn các câu/đoạn mong muốn.
2.  Nhấn "Thêm vào DS Ghép".
3.  Nhập tên file output (ví dụ: `final_mix.mp3`).
4.  Nhấn "Bắt đầu Ghép". Chọn thư mục lưu file.
5.  File MP3 mới được tạo và thông tin lưu vào CSDL. Chỉ các đoạn có thời gian hợp lệ được xử lý.

### Đóng ứng dụng
* Nhấn nút 'X' của cửa sổ hoặc menu `File -> Thoát`.

---

## 6. Khắc phục sự cố thường gặp

* **Lỗi `JFileChooser NullPointerException` khi nhấn nút "..." trong Cài đặt:**
    * Đây có thể là vấn đề với Windows Look and Feel trên một số phiên bản Java (ví dụ JDK 22).
    * **Giải pháp:** Mở file `src\main\java\com\yourcompany\mp3joiner\MainApp.java` (thay `yourcompany` bằng package của bạn), thay đổi `UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());` thành `UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());`. Build lại project (`mvn clean package`). Giao diện sẽ đổi sang "Metal" nhưng `JFileChooser` sẽ hoạt động ổn định.
* **Lỗi không tìm thấy FFMPEG/FFPROBE (`Cannot run program "ffmpeg"/"ffprobe"`):**
    * Đảm bảo đường dẫn đến `ffmpeg.exe` được cấu hình đúng trong Dialog Cài đặt của ứng dụng (trỏ đến file `ffmpeg.exe` trong thư mục `JAVA_CUOI_KY\ffmpeg\bin\`). Ứng dụng sẽ tự suy ra đường dẫn `ffprobe.exe`.
    * Kiểm tra xem các file DLL cần thiết có nằm cùng thư mục `bin` với `ffmpeg.exe` không (nếu bạn không dùng bản static).
* **Lỗi kết nối CSDL SQL Server (`SocketTimeoutException`, `Invalid object name 'tbl_xxx'`, `Login failed`):**
    * Kiểm tra kỹ các dịch vụ SQL Server (`SQL Server (SQLEXPRESS)`) có đang chạy.
    * Đảm bảo TCP/IP được bật cho instance SQL Server trên cổng 1433 và đã khởi động lại dịch vụ sau khi thay đổi.
    * Kiểm tra Firewall.
    * Đảm bảo thông tin kết nối (server `localhost,1433`, database name `MP3ManagerDB`, user, password) trong Dialog Cài đặt của ứng dụng là chính xác.
    * Đảm bảo đã chạy script `MP3ManagerDB.sql` để tạo các bảng trong đúng database `MP3ManagerDB`.
* **Thời lượng file MP3 hiển thị là 00:00:**
    * Thường do lỗi không chạy được `ffprobe.exe`. Xem lại mục khắc phục sự cố FFMPEG.
* **AI trích xuất câu không chính xác / thời gian không hợp lệ:**
    * Chất lượng phụ thuộc model AI và âm thanh. Sử dụng chức năng "Chỉnh sửa câu thủ công".
    * Ứng dụng đã có logic lọc bỏ các câu có thời gian không hợp lệ (bắt đầu >= kết thúc) trước khi lưu và khi chuẩn bị ghép.

---
*(Kết thúc README.md)*"# java_cuoi_ky" 

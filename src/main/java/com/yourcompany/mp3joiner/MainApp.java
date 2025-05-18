package com.yourcompany.mp3joiner;

import com.yourcompany.mp3joiner.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class MainApp {
    public static void main(String[] args) {    
        // (Tùy chọn) Set Look and Feel cho đẹp hơn
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Không thể set System Look and Feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}

// package com.yourcompany.mp3joiner; // Hoặc package của bạn nếu khác

// import com.yourcompany.mp3joiner.ui.MainFrame;
//  import javax.swing.SwingUtilities;
// import javax.swing.UIManager;

// public class MainApp {
//     public static void main(String[] args) {
//         try {
//             // --- THAY ĐỔI Ở ĐÂY ---
//             // Sử dụng CrossPlatformLookAndFeel (Metal) để kiểm tra
//             UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
//             System.out.println("Using Look and Feel: " + UIManager.getLookAndFeel().getName());

//             // Dòng code cũ của bạn sử dụng SystemLookAndFeel (WindowsLookAndFeel trên Windows):
//             // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//             // --- KẾT THÚC THAY ĐỔI ---

//         } catch (Exception e) {
//             System.err.println("Không thể set Look and Feel: " + e.getMessage());
//             // Nếu không set được, ứng dụng vẫn sẽ chạy với L&F mặc định của Java (thường là Metal)
//         }

//         SwingUtilities.invokeLater(() -> {
//             MainFrame mainFrame = new MainFrame();
//             mainFrame.setVisible(true);
//         });
//     }
// }
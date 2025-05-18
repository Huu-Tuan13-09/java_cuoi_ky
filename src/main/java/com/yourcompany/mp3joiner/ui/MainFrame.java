package com.yourcompany.mp3joiner.ui;

import com.yourcompany.mp3joiner.db.DatabaseManager;
import com.yourcompany.mp3joiner.db.FileInfo;
import com.yourcompany.mp3joiner.db.SentenceInfo;
import com.yourcompany.mp3joiner.service.FileManagementService;
import com.yourcompany.mp3joiner.util.TimeConverter;

// import main.java.com.yourcompany.mp3joiner.db.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainFrame extends JFrame {
    private FileManagementService fileService;

    private JList<FileInfo> fileList;
    private DefaultListModel<FileInfo> fileListModel;

    private JTable sentenceTable;
    private DefaultTableModel sentenceTableModel;

    private JList<SentenceInfo> mergeList; // Danh sách các câu/đoạn chọn để ghép
    private DefaultListModel<SentenceInfo> mergeListModel;

    private JTextArea logArea;

    private FileInfo currentSelectedFile = null;

    public MainFrame() {
        this.fileService = new FileManagementService();

        setTitle("Chương trình Nối File MP3 (có AI)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null); // Căn giữa màn hình

        initComponents();
        loadFilesFromDB(); // Tải danh sách file khi khởi động

        // Kiểm tra CSDL và Model AI khi khởi động
        if (!DatabaseManager.getInstance().testConnection()) {
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối tới CSDL. Vui lòng kiểm tra cài đặt (Menu -> Cài đặt).",
                    "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
        if (!fileService.getAiService().isModelReady()) {
            JOptionPane.showMessageDialog(this,
                    "Model AI (Vosk) chưa sẵn sàng. Vui lòng kiểm tra đường dẫn trong Cài đặt.",
                    "Lỗi Model AI", JOptionPane.WARNING_MESSAGE);
        }

    }

    private void initComponents() {
        // Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem addFileItem = new JMenuItem("Thêm File MP3...");
        addFileItem.addActionListener(e -> addFileAction());
        JMenuItem settingsItem = new JMenuItem("Cài đặt...");
        settingsItem.addActionListener(e -> openSettingsDialog());
        JMenuItem exitItem = new JMenuItem("Thoát");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(addFileItem);
        fileMenu.add(settingsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Main Panel chia làm nhiều phần
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. File List Panel (Bên trái)
        JPanel fileListPanel = new JPanel(new BorderLayout());
        fileListPanel.setBorder(BorderFactory.createTitledBorder("Danh sách File MP3"));
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                currentSelectedFile = fileList.getSelectedValue();
                if (currentSelectedFile != null) {
                    loadSentencesForFile(currentSelectedFile);
                } else {
                    sentenceTableModel.setRowCount(0); // Xóa bảng câu
                }
            }
        });
        // Double click vào file để trích xuất
        fileList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = fileList.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        FileInfo selected = fileListModel.getElementAt(index);
                        extractSentencesAction(selected);
                    }
                }
            }
        });

        JScrollPane fileListScrollPane = new JScrollPane(fileList);
        // Các nút cho File List
        JPanel fileButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddFile = new JButton("Thêm File");
        btnAddFile.addActionListener(e -> addFileAction());
        JButton btnDeleteFile = new JButton("Xóa File");
        btnDeleteFile.addActionListener(e -> deleteFileAction());
        JButton btnExtract = new JButton("Trích xuất Câu (AI)");
        btnExtract.addActionListener(e -> {
            if (currentSelectedFile != null)
                extractSentencesAction(currentSelectedFile);
            else
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một file.", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
        });
        fileButtonsPanel.add(btnAddFile);
        fileButtonsPanel.add(btnDeleteFile);
        fileButtonsPanel.add(btnExtract);
        fileListPanel.add(fileListScrollPane, BorderLayout.CENTER);
        fileListPanel.add(fileButtonsPanel, BorderLayout.SOUTH);

        // 2. Sentences Panel (Ở giữa, lớn)
        JPanel sentencesPanel = new JPanel(new BorderLayout());
        sentencesPanel.setBorder(BorderFactory.createTitledBorder("Các câu đã trích xuất (Click đúp để sửa)"));
        sentenceTableModel = new DefaultTableModel(new Object[] { "ID", "Bắt đầu", "Kết thúc", "Nội dung câu" }, 0) {
            @Override // Không cho sửa cột ID trực tiếp
            public boolean isCellEditable(int row, int column) {
                return column != 0; // Cho phép sửa cột 1, 2, 3
            }
        };
        sentenceTable = new JTable(sentenceTableModel);
        // Cho phép sửa trực tiếp và lưu khi focus lost hoặc enter
        sentenceTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        // Ẩn cột ID (cột 0) khỏi view nhưng vẫn dùng được data
        TableColumnModel tcm = sentenceTable.getColumnModel();
        tcm.getColumn(0).setMinWidth(0);
        tcm.getColumn(0).setMaxWidth(0);
        tcm.getColumn(0).setWidth(0);
        // Thiết lập độ rộng cột
        tcm.getColumn(1).setPreferredWidth(80); // Bắt đầu
        tcm.getColumn(2).setPreferredWidth(80); // Kết thúc
        tcm.getColumn(3).setPreferredWidth(400); // Nội dung

        sentenceTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double click
                    int selectedRow = sentenceTable.getSelectedRow();
                    if (selectedRow != -1) {
                        editSentence(selectedRow);
                    }
                }
            }
        });

        JScrollPane sentenceTableScrollPane = new JScrollPane(sentenceTable);
        // Các nút cho Sentences Panel
        JPanel sentenceButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddToMergeList = new JButton("Thêm vào DS Ghép");
        btnAddToMergeList.addActionListener(e -> addSelectedSentencesToMergeList());
        // JButton btnSaveChanges = new JButton("Lưu thay đổi câu"); // Sẽ làm tự động
        // khi sửa xong cell
        // btnSaveChanges.addActionListener(e -> saveSentenceChanges());
        sentenceButtonsPanel.add(btnAddToMergeList);
        // sentenceButtonsPanel.add(btnSaveChanges);

        sentencesPanel.add(sentenceTableScrollPane, BorderLayout.CENTER);
        sentencesPanel.add(sentenceButtonsPanel, BorderLayout.SOUTH);

        // 3. Merge List Panel (Bên phải)
        JPanel mergePanel = new JPanel(new BorderLayout());
        mergePanel.setBorder(BorderFactory.createTitledBorder("Danh sách đoạn sẽ ghép"));
        mergeListModel = new DefaultListModel<>();
        mergeList = new JList<>(mergeListModel);
        JScrollPane mergeListScrollPane = new JScrollPane(mergeList);
        // Các nút cho Merge List
        JPanel mergeButtonsPanel = new JPanel(new GridLayout(0, 1, 5, 5)); // Grid layout cho các nút dọc
        JTextField outputFileNameField = new JTextField("merged_output.mp3");
        outputFileNameField.setBorder(BorderFactory.createTitledBorder("Tên file output"));

        JButton btnRemoveFromMerge = new JButton("Xóa khỏi DS Ghép");
        btnRemoveFromMerge.addActionListener(e -> removeSelectedFromMergeList());
        JButton btnClearMergeList = new JButton("Xóa hết DS Ghép");
        btnClearMergeList.addActionListener(e -> mergeListModel.clear());
        JButton btnStartMerge = new JButton("Bắt đầu Ghép");
        btnStartMerge.addActionListener(e -> startMergeAction(outputFileNameField.getText()));

        mergeButtonsPanel.add(outputFileNameField);
        mergeButtonsPanel.add(btnRemoveFromMerge);
        mergeButtonsPanel.add(btnClearMergeList);
        mergeButtonsPanel.add(btnStartMerge);

        mergePanel.add(mergeListScrollPane, BorderLayout.CENTER);
        mergePanel.add(mergeButtonsPanel, BorderLayout.SOUTH);

        // SplitPane để chia khu vực
        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileListPanel, sentencesPanel);
        horizontalSplit.setDividerLocation(250); // Vị trí ban đầu của thanh chia

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, horizontalSplit, mergePanel);
        mainSplit.setDividerLocation(700); // Vị trí thanh chia giữa sentences và merge

        // 4. Log Panel (Phía dưới)
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log/Trạng thái"));
        logArea = new JTextArea(5, 0);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        mainPanel.add(mainSplit, BorderLayout.CENTER);
        mainPanel.add(logPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // Auto-scroll
        });
    }

    private void openSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(this);
        dialog.setVisible(true);
        // Sau khi dialog đóng, có thể cần refresh gì đó nếu cài đặt thay đổi
        // Ví dụ: nếu model AI thay đổi, kiểm tra lại isModelReady
        if (!fileService.getAiService().isModelReady()) {
            appendLog("Lỗi: Model AI (Vosk) chưa sẵn sàng sau khi thay đổi cài đặt.");
        }
        if (!DatabaseManager.getInstance().testConnection()) {
            appendLog("Lỗi: Không thể kết nối CSDL sau khi thay đổi cài đặt.");
        }
    }

    private void addFileAction() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file MP3");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 Files", "mp3"));
        fileChooser.setMultiSelectionEnabled(true); // Cho phép chọn nhiều file

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File selectedFile : selectedFiles) {
                appendLog("Đang thêm file: " + selectedFile.getAbsolutePath());
                // Chạy trên luồng riêng để không block UI, đặc biệt khi lấy duration
                new SwingWorker<FileInfo, Void>() {
                    @Override
                    protected FileInfo doInBackground() throws Exception {
                        return fileService.addMp3File(selectedFile.getAbsolutePath());
                    }

                    @Override
                    protected void done() {
                        try {
                            FileInfo addedFile = get();
                            if (addedFile != null) {
                                fileListModel.addElement(addedFile);
                                appendLog("Đã thêm: " + addedFile.getFileName() + " (Thời lượng: "
                                        + TimeConverter.secondsToHmmss(addedFile.getDurationSeconds()) + ")");
                            } else {
                                appendLog("Lỗi thêm file: " + selectedFile.getName());
                                JOptionPane.showMessageDialog(MainFrame.this,
                                        "Lỗi khi thêm file: " + selectedFile.getName() + ". Kiểm tra log.", "Lỗi",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            appendLog("Lỗi nghiêm trọng khi thêm file: " + e.getMessage());
                            JOptionPane.showMessageDialog(MainFrame.this,
                                    "Lỗi nghiêm trọng khi thêm file: " + e.getMessage(), "Lỗi",
                                    JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                        }
                    }
                }.execute();
            }
        }
    }

    private void deleteFileAction() {
        FileInfo selectedFile = fileList.getSelectedValue();
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một file để xóa.", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa file '" + selectedFile.getFileName()
                        + "' khỏi cơ sở dữ liệu?\n(File trên ổ cứng sẽ không bị xóa)",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (fileService.deleteMp3File(selectedFile.getId())) {
                fileListModel.removeElement(selectedFile);
                sentenceTableModel.setRowCount(0); // Xóa bảng câu
                mergeListModel.clear(); // Xóa danh sách ghép nếu có câu từ file này
                appendLog("Đã xóa file '" + selectedFile.getFileName() + "' khỏi CSDL.");
            } else {
                appendLog("Lỗi khi xóa file '" + selectedFile.getFileName() + "'.");
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa file.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadFilesFromDB() {
        fileListModel.clear();
        List<FileInfo> files = fileService.getAllMp3Files();
        for (FileInfo file : files) {
            fileListModel.addElement(file);
        }
        if (!files.isEmpty())
            appendLog("Đã tải " + files.size() + " file từ CSDL.");
    }

    private void editSentence(int rowIndex) {
        // Lấy dữ liệu từ bảng
        int sentenceId = (int) sentenceTableModel.getValueAt(rowIndex, 0); // ID ẩn
        String startTimeStr = (String) sentenceTableModel.getValueAt(rowIndex, 1);
        String endTimeStr = (String) sentenceTableModel.getValueAt(rowIndex, 2);
        String text = (String) sentenceTableModel.getValueAt(rowIndex, 3);

        // Tạo dialog để sửa
        JTextField startField = new JTextField(startTimeStr, 10);
        JTextField endField = new JTextField(endTimeStr, 10);
        JTextArea textField = new JTextArea(text, 5, 30);
        textField.setLineWrap(true);
        textField.setWrapStyleWord(true);
        JScrollPane textScrollPane = new JScrollPane(textField);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel timePanel = new JPanel(new FlowLayout());
        timePanel.add(new JLabel("Bắt đầu (HH:MM:SS hoặc MM:SS):"));
        timePanel.add(startField);
        timePanel.add(new JLabel("Kết thúc:"));
        timePanel.add(endField);
        panel.add(timePanel, BorderLayout.NORTH);
        panel.add(textScrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Chỉnh sửa câu (ID: " + sentenceId + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                double newStartTime = TimeConverter.hmmssToSeconds(startField.getText());
                double newEndTime = TimeConverter.hmmssToSeconds(endField.getText());
                String newText = textField.getText();

                if (newStartTime >= newEndTime) {
                    JOptionPane.showMessageDialog(this, "Thời gian bắt đầu phải nhỏ hơn thời gian kết thúc.",
                            "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Lấy file ID từ currentSelectedFile
                if (currentSelectedFile == null)
                    return; // Nên có kiểm tra kỹ hơn

                SentenceInfo updatedSentence = new SentenceInfo(sentenceId, currentSelectedFile.getId(), newStartTime,
                        newEndTime, newText, null); // lastModifiedAt sẽ được DB cập nhật

                if (fileService.updateSentence(updatedSentence)) {
                    appendLog("Đã cập nhật câu ID " + sentenceId);
                    // Cập nhật lại bảng
                    sentenceTableModel.setValueAt(TimeConverter.secondsToHmmss(newStartTime), rowIndex, 1);
                    sentenceTableModel.setValueAt(TimeConverter.secondsToHmmss(newEndTime), rowIndex, 2);
                    sentenceTableModel.setValueAt(newText, rowIndex, 3);
                } else {
                    appendLog("Lỗi cập nhật câu ID " + sentenceId);
                    JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật câu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi định dạng thời gian: " + ex.getMessage(), "Lỗi nhập liệu",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void extractSentencesAction(FileInfo fileInfo) {
        if (fileInfo == null)
            return;
        if (!fileService.getAiService().isModelReady()) {
            JOptionPane.showMessageDialog(this, "Model AI (Vosk) chưa sẵn sàng. Vui lòng kiểm tra Cài đặt.",
                    "Lỗi Model AI", JOptionPane.ERROR_MESSAGE);
            appendLog("Trích xuất thất bại: Model AI chưa sẵn sàng.");
            return;
        }

        // Vô hiệu hóa các nút để tránh thao tác khi đang xử lý
        // ... (thêm sau)
        appendLog("Bắt đầu trích xuất câu cho file: " + fileInfo.getFileName() + "...");
        sentenceTableModel.setRowCount(0); // Xóa bảng câu cũ

        // SwingWorker<List<SentenceInfo>, String> worker = new SwingWorker<>() {
        // @Override
        // protected List<SentenceInfo> doInBackground() throws Exception {
        // // Consumer để nhận thông điệp từ service và publish cho process
        // Consumer<String> progressConsumer = this::publish;
        // return fileService.extractSentencesFromFile(fileInfo, progressConsumer);
        // }

        // @Override
        // protected void process(List<String> chunks) {
        // // Cập nhật log/progress bar từ những gì đã publish
        // for (String message : chunks) {
        // appendLog(message);
        // }
        // }

        // @Override
        // protected void done() {
        // try {
        // List<SentenceInfo> sentences = get();
        // if (sentences != null && !sentences.isEmpty()) {
        // appendLog("Trích xuất hoàn tất. Tìm thấy " + sentences.size() + " câu.");
        // loadSentencesForFile(fileInfo); // Tải lại câu vào bảng
        // } else if (sentences != null && sentences.isEmpty()){
        // appendLog("Trích xuất hoàn tất. Không tìm thấy câu nào hoặc AI không trả về
        // kết quả.");
        // } else {
        // appendLog("Trích xuất thất bại hoặc không có câu nào được tìm thấy.");
        // }
        // } catch (InterruptedException | ExecutionException e) {
        // appendLog("Lỗi nghiêm trọng khi trích xuất: " + e.getMessage());
        // JOptionPane.showMessageDialog(MainFrame.this, "Lỗi trích xuất: " +
        // e.getMessage(), "Lỗi AI", JOptionPane.ERROR_MESSAGE);
        // e.printStackTrace();
        // }
        // // Kích hoạt lại các nút
        // // ...
        // }
        // };

        SwingWorker<List<SentenceInfo>, String> worker = new SwingWorker<List<SentenceInfo>, String>() { // Dòng mới -
                                                                                                         // Khai báo rõ
                                                                                                         // generic type
            @Override // Đảm bảo có @Override
            protected List<SentenceInfo> doInBackground() throws Exception {
                // Consumer để nhận thông điệp từ service và publish cho process
                Consumer<String> progressConsumer = this::publish;
                return fileService.extractSentencesFromFile(fileInfo, progressConsumer);
            }

            @Override // Đảm bảo có @Override
            protected void process(List<String> chunks) {
                // Cập nhật log/progress bar từ những gì đã publish
                for (String message : chunks) {
                    appendLog(message);
                }
            }

            @Override // Đảm bảo có @Override
            protected void done() {
                try {
                    List<SentenceInfo> sentences = get();
                    if (sentences != null && !sentences.isEmpty()) {
                        appendLog("Trích xuất hoàn tất. Tìm thấy " + sentences.size() + " câu.");
                        loadSentencesForFile(fileInfo); // Tải lại câu vào bảng
                    } else if (sentences != null && sentences.isEmpty()) {
                        appendLog("Trích xuất hoàn tất. Không tìm thấy câu nào hoặc AI không trả về kết quả.");
                    } else {
                        appendLog("Trích xuất thất bại hoặc không có câu nào được tìm thấy.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    appendLog("Lỗi nghiêm trọng khi trích xuất: " + e.getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this, "Lỗi trích xuất: " + e.getMessage(), "Lỗi AI",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
                // Kích hoạt lại các nút
                // ...
            }
        };
        worker.execute();

        worker.execute();
    }

    private void loadSentencesForFile(FileInfo fileInfo) {
        sentenceTableModel.setRowCount(0); // Xóa dữ liệu cũ
        if (fileInfo == null)
            return;

        List<SentenceInfo> sentences = fileService.getSentencesForFile(fileInfo.getId());
        for (SentenceInfo sentence : sentences) {
            sentenceTableModel.addRow(new Object[] {
                    sentence.getId(), // ID ẩn, dùng để update/delete
                    TimeConverter.secondsToHmmss(sentence.getStartTimeSeconds()),
                    TimeConverter.secondsToHmmss(sentence.getEndTimeSeconds()),
                    sentence.getSentenceText()
            });
        }
        appendLog("Đã tải " + sentences.size() + " câu cho file: " + fileInfo.getFileName());
    }

    private void addSelectedSentencesToMergeList() {
        if (currentSelectedFile == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn file MP3 gốc.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int[] selectedRows = sentenceTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ít nhất một câu từ bảng.", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (int rowIndex : selectedRows) {
            int sentenceId = (int) sentenceTableModel.getValueAt(rowIndex, 0);
            // Lấy thông tin SentenceInfo đầy đủ từ DB hoặc từ một list đã load trước đó
            // Ở đây, ta tạo lại từ bảng để đơn giản
            try {
                double startSec = TimeConverter.hmmssToSeconds((String) sentenceTableModel.getValueAt(rowIndex, 1));
                double endSec = TimeConverter.hmmssToSeconds((String) sentenceTableModel.getValueAt(rowIndex, 2));
                String text = (String) sentenceTableModel.getValueAt(rowIndex, 3);

                SentenceInfo sentenceToAdd = new SentenceInfo(sentenceId, currentSelectedFile.getId(), startSec, endSec,
                        "[" + currentSelectedFile.getFileName() + "] " + text, null); // Thêm tên file gốc vào text để
                                                                                      // dễ nhận
                // biết

                mergeListModel.addElement(sentenceToAdd);
            } catch (IllegalArgumentException e) {
                appendLog("Lỗi định dạng thời gian ở câu được chọn: " + e.getMessage());
            }
        }
        appendLog("Đã thêm " + selectedRows.length + " đoạn vào danh sách ghép.");
    }

    private void removeSelectedFromMergeList() {
        List<SentenceInfo> selectedItems = mergeList.getSelectedValuesList();
        if (selectedItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đoạn cần xóa khỏi danh sách ghép.", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (SentenceInfo item : selectedItems) {
            mergeListModel.removeElement(item);
        }
        appendLog("Đã xóa " + selectedItems.size() + " đoạn khỏi danh sách ghép.");
    }

    private void startMergeAction(String outputFileName) {
        if (mergeListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Danh sách ghép rỗng. Vui lòng thêm các đoạn cần ghép.", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (outputFileName == null || outputFileName.trim().isEmpty()
                || !outputFileName.toLowerCase().endsWith(".mp3")) {
            JOptionPane.showMessageDialog(this, "Tên file output không hợp lệ hoặc không phải .mp3", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn thư mục lưu file MP3 đã ghép");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false); // Không cho chọn "All Files"

        String outputDir;
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDir = chooser.getSelectedFile().getAbsolutePath();
        } else {
            appendLog("Hủy ghép file: Người dùng không chọn thư mục output.");
            return;
        }

        List<FileManagementService.SegmentToMerge> segmentsToMerge = new ArrayList<>();
        for (int i = 0; i < mergeListModel.getSize(); i++) {
            SentenceInfo sentenceInfo = mergeListModel.getElementAt(i);
            // Cần lấy đường dẫn file gốc của sentenceInfo.
            // Cách 1: Lưu FileInfo cùng với SentenceInfo trong mergeList (phức tạp hơn
            // JList Model)
            // Cách 2: Tìm lại FileInfo từ sentenceInfo.getFileId()
            // Cách 3: Nhúng filePath vào SentenceInfo (không tốt cho chuẩn hóa DB)
            // Ta sẽ dùng cách 2, nhưng cần hàm lấy FileInfo by ID trong
            // DBManager/FileService (chưa có)
            // Tạm thời, nếu currentSelectedFile chính là file của tất cả các câu trong
            // merge list thì dùng nó.
            // Đây là một điểm cần cải thiện: mergeList nên lưu thông tin đầy đủ hơn.
            // Giả định tạm: tất cả câu trong mergeList đều từ currentSelectedFile

            // *** Cải thiện: Lấy đường dẫn file gốc từ ID file của từng câu ***
            // Chúng ta cần một cách để lấy FileInfo từ fileId trong SentenceInfo
            // Ví dụ:
            // FileInfo originalFileInfo = dbManager.getFileById(sentenceInfo.getFileId());
            // String originalFilePath = originalFileInfo.getFilePath();
            // Hiện tại, chưa có hàm getFileById, nên sẽ dùng filePath từ FileInfo đang được
            // chọn
            // Đây là một hạn chế cần khắc phục nếu muốn ghép từ nhiều file khác nhau trong
            // mergeList.

            String originalFilePath = null;
            // Thử tìm FileInfo tương ứng với fileId của câu trong mergeList
            // Đây là cách không hiệu quả nếu danh sách file lớn, nên cache hoặc có hàm trực
            // tiếp
            for (int j = 0; j < fileListModel.getSize(); j++) {
                FileInfo fi = fileListModel.getElementAt(j);
                if (fi.getId() == sentenceInfo.getFileId()) {
                    originalFilePath = fi.getFilePath();
                    break;
                }
            }

            if (originalFilePath == null) {
                appendLog("Lỗi: Không tìm thấy file gốc cho đoạn: " + sentenceInfo.getSentenceText());
                JOptionPane.showMessageDialog(this,
                        "Lỗi nghiêm trọng: Không tìm thấy file gốc cho một đoạn trong danh sách ghép.", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // segmentsToMerge.add(new FileManagementService.SegmentToMerge(
            // originalFilePath,
            // sentenceInfo.getStartTimeSeconds(),
            // sentenceInfo.getEndTimeSeconds()));
            String originalFileNameForLog = "UnknownFile"; // Giá trị mặc định
            for (int j = 0; j < fileListModel.getSize(); j++) { // fileListModel là DefaultListModel<FileInfo>
                FileInfo fi = fileListModel.getElementAt(j);
                if (fi.getId() == sentenceInfo.getFileId()) {
                    originalFileNameForLog = fi.getFileName();
                    break;
                }
            }
            segmentsToMerge.add(new FileManagementService.SegmentToMerge(
                    originalFilePath,
                    sentenceInfo.getStartTimeSeconds(),
                    sentenceInfo.getEndTimeSeconds(),
                    originalFileNameForLog // Truyền tên file vào đây
            ));
        }
        appendLog("Chuẩn bị ghép " + segmentsToMerge.size() + " đoạn...");

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                fileService.mergeAudioSegments(segmentsToMerge, outputDir, outputFileName, this::publish);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    appendLog(message);
                }
            }

            @Override
            protected void done() {
                // Có thể thêm thông báo hoàn tất ở đây
                try {
                    get(); // Gọi get() để bắt exception nếu có từ doInBackground
                    appendLog("Hoàn tất quá trình ghép.");
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Quá trình ghép đã hoàn tất. Kiểm tra log và thư mục output.",
                            "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException | ExecutionException e) {
                    appendLog("Lỗi nghiêm trọng trong quá trình ghép: " + e.getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Lỗi nghiêm trọng trong quá trình ghép: " + e.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}
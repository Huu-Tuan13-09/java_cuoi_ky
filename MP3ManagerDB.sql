-- Đảm bảo bạn đang sử dụng đúng database
-- USE MP3ManagerDB;
-- GO

-- Bảng lưu thông tin file MP3 gốc
IF OBJECT_ID(N'[dbo].[tbl_files]', N'U') IS NULL
BEGIN
    CREATE TABLE tbl_files (
        id INT PRIMARY KEY IDENTITY(1,1),
        file_name NVARCHAR(255) NOT NULL,
        file_path NVARCHAR(450) NOT NULL UNIQUE, -- Thay đổi từ MAX sang 450
        duration_seconds FLOAT,
        created_at DATETIME2 DEFAULT GETDATE()
    );
    PRINT 'Table tbl_files created successfully.';
END
ELSE
BEGIN
    PRINT 'Table tbl_files already exists.';
END
GO

-- Bảng lưu các câu được trích xuất từ file MP3
IF OBJECT_ID(N'[dbo].[tbl_sentences]', N'U') IS NULL
BEGIN
    CREATE TABLE tbl_sentences (
        id INT PRIMARY KEY IDENTITY(1,1),
        file_id INT,
        start_time_seconds FLOAT NOT NULL,
        end_time_seconds FLOAT NOT NULL,
        sentence_text NVARCHAR(MAX) NOT NULL, -- NVARCHAR(MAX) ở đây vẫn ổn vì không có UNIQUE constraint trực tiếp
        last_modified_at DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_Sentences_Files FOREIGN KEY (file_id) REFERENCES tbl_files(id) ON DELETE CASCADE
    );
    PRINT 'Table tbl_sentences created successfully.';
END
ELSE
BEGIN
    PRINT 'Table tbl_sentences already exists.';
END
GO

-- Bảng lưu thông tin các file MP3 đã được ghép
IF OBJECT_ID(N'[dbo].[tbl_compositions]', N'U') IS NULL
BEGIN
    CREATE TABLE tbl_compositions (
        id INT PRIMARY KEY IDENTITY(1,1),
        composition_name NVARCHAR(255) NOT NULL,
        composition_path NVARCHAR(450) NOT NULL UNIQUE, -- Thay đổi từ MAX sang 450
        created_at DATETIME2 DEFAULT GETDATE()
    );
    PRINT 'Table tbl_compositions created successfully.';
END
ELSE
BEGIN
    PRINT 'Table tbl_compositions already exists.';
END
GO
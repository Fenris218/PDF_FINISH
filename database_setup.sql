CREATE DATABASE pdf_convertion CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pdf_convertion;

-- Tạo bảng users
CREATE TABLE users (
                       username VARCHAR(50) PRIMARY KEY,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(100),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng uploads
CREATE TABLE uploads (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         username VARCHAR(50) NOT NULL,
                         fileNameUpload VARCHAR(255) NOT NULL,
                         fileNameOutput VARCHAR(255) NOT NULL,
                         fileNameOutputInServer VARCHAR(255) NOT NULL,
                         date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         status VARCHAR(20) DEFAULT 'completed',
                         error_message TEXT,
                         FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE,
                         INDEX idx_uploads_username (username),
                         INDEX idx_uploads_status (status),
                         INDEX idx_uploads_username_status (username, status),
                         INDEX idx_uploads_date (date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo user mẫu
INSERT INTO users (username, password, email) VALUES
    ('admin', 'admin123', 'admin@example.com');
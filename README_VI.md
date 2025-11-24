# HỆ THỐNG CHUYỂN ĐỔI PDF SANG DOCX VỚI HÀNG ĐỢI XỬ LÝ

## Tổng quan dự án
Đây là ứng dụng web Java cho phép chuyển đổi file PDF sang định dạng DOCX với hệ thống xử lý hàng đợi bất đồng bộ.

---

## MỤC LỤC
1. [Kiến trúc chương trình](#kiến-trúc-chương-trình)
2. [Mô hình MVC](#mô-hình-mvc)
3. [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
4. [Hướng dẫn cài đặt](#hướng-dẫn-cài-đặt)
5. [Hướng dẫn sử dụng](#hướng-dẫn-sử-dụng)
6. [Các tính năng chính](#các-tính-năng-chính)
7. [Xử lý sự cố](#xử-lý-sự-cố)

---

## KIẾN TRÚC CHƯƠNG TRÌNH

### Kiến trúc tổng quan
```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT (Trình duyệt)                  │
│                     - index.jsp (Trang chủ)                  │
│                     - viewListConvert.jsp (Danh sách)        │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP Request/Response
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    CONTROLLER (Servlet)                      │
│   ┌──────────────────┐  ┌──────────────────┐               │
│   │ ConverterServlet │  │ ListConvertServlet│               │
│   │ - Nhận PDF       │  │ - Hiển thị danh   │               │
│   │ - Thêm vào queue │  │   sách chuyển đổi │               │
│   └────────┬─────────┘  └─────────┬─────────┘               │
│            │                       │                         │
└────────────┼───────────────────────┼─────────────────────────┘
             │                       │
             ▼                       ▼
┌─────────────────────────────────────────────────────────────┐
│                     BUSINESS LOGIC (BO)                      │
│   ┌──────────────────┐  ┌──────────────────┐               │
│   │ ConverterBO      │  │ ConversionQueue  │               │
│   │ - Xử lý logic    │  │ - Quản lý hàng   │               │
│   │ - Lưu lịch sử    │  │   đợi (Singleton)│               │
│   └────────┬─────────┘  └────────┬─────────┘               │
│            │                      │                          │
│            │         ┌────────────┴──────────┐              │
│            │         │  ConversionWorker     │              │
│            │         │  - Thread nền         │              │
│            │         │  - Xử lý file PDF     │              │
│            │         └───────────────────────┘              │
└────────────┼──────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│                    DATA ACCESS (DAO)                         │
│   ┌──────────────────┐  ┌──────────────────┐               │
│   │ ConverterDAO     │  │ LoginDAO         │               │
│   │ - Truy vấn DB    │  │ - Xác thực user  │               │
│   │ - Lưu trữ        │  │                  │               │
│   └────────┬─────────┘  └──────────────────┘               │
└────────────┼──────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│                    DATABASE (MySQL)                          │
│   ┌──────────────┐     ┌─────────────────┐                 │
│   │ Bảng users   │     │ Bảng uploads    │                 │
│   │ - username   │     │ - id            │                 │
│   │ - password   │     │ - username      │                 │
│   │ - email      │     │ - fileNameUpload│                 │
│   └──────────────┘     │ - status        │                 │
│                        │ - date          │                 │
│                        └─────────────────┘                 │
└─────────────────────────────────────────────────────────────┘
```

### Luồng xử lý hệ thống hàng đợi
```
1. User tải file PDF lên
         ↓
2. ConverterServlet nhận file
         ↓
3. Lưu thông tin vào Database với status="queued"
         ↓
4. Thêm task vào ConversionQueue
         ↓
5. Trả về ngay "Đang thực hiện" cho user
         ↓
6. ConversionWorker (thread nền) lấy task từ queue
         ↓
7. Cập nhật status="processing"
         ↓
8. Chuyển đổi PDF → DOCX
         ↓
9. Lưu file kết quả
         ↓
10. Cập nhật status="completed"
         ↓
11. User xem danh sách và tải file về
```

---

## MÔ HÌNH MVC

Ứng dụng được xây dựng theo mô hình **MVC (Model-View-Controller)** chuẩn:

### 1. MODEL (Mô hình dữ liệu)
**Vị trí**: `src/main/java/model/`

#### a) BEAN (Java Bean - Đối tượng dữ liệu)
- **User.java**: Đại diện cho tài khoản người dùng
  - Thuộc tính: username, password, email
  - Getter/Setter cho các thuộc tính
  
- **Upload.java**: Đại diện cho một lần chuyển đổi file
  - Thuộc tính: id, username, fileNameUpload, fileNameOutput, status, date
  - Lưu trữ thông tin về file PDF và DOCX
  
- **ConversionTask.java**: Đại diện cho một nhiệm vụ chuyển đổi
  - Thuộc tính: taskId, username, các đường dẫn file, status
  - Sử dụng trong hệ thống hàng đợi

#### b) DAO (Data Access Object - Truy cập dữ liệu)
- **LoginDAO.java**: Xử lý truy vấn liên quan đến đăng nhập
  - checkLogin(): Kiểm tra thông tin đăng nhập
  - addUser(): Thêm người dùng mới
  
- **ConverterDAO.java**: Xử lý truy vấn liên quan đến chuyển đổi
  - saveHistory(): Lưu lịch sử chuyển đổi
  - getListFileConvert(): Lấy danh sách file đã chuyển đổi
  - updateStatus(): Cập nhật trạng thái chuyển đổi

#### c) BO (Business Object - Đối tượng nghiệp vụ)
- **LoginBO.java**: Logic nghiệp vụ đăng nhập
  - Gọi LoginDAO để xác thực
  - Xử lý logic đăng ký, đăng nhập
  
- **ConverterBO.java**: Logic nghiệp vụ chuyển đổi
  - Gọi ConverterDAO để lưu trữ
  - Xử lý logic trước/sau khi chuyển đổi
  
- **ConversionQueue.java**: Quản lý hàng đợi chuyển đổi
  - Singleton pattern
  - Thread-safe BlockingQueue
  - Quản lý ConversionWorker
  
- **ConversionWorker.java**: Thread xử lý nền
  - Lấy task từ queue
  - Thực hiện chuyển đổi
  - Cập nhật trạng thái
  
- **PdfConvertionHelper.java**: Hỗ trợ chuyển đổi PDF
  - Sử dụng thư viện Spire.PDF và Spire.Doc
  - Xử lý việc đọc PDF và tạo DOCX

### 2. VIEW (Giao diện)
**Vị trí**: `src/main/webapp/`

- **index.jsp**: Trang chủ - Form upload file PDF
  - Cho phép user chọn và upload file
  - Hiển thị thông báo kết quả
  
- **viewListConvert.jsp**: Trang danh sách chuyển đổi
  - Hiển thị lịch sử chuyển đổi của user
  - Hiển thị trạng thái: Đang thực hiện, Hoàn thành, Thất bại
  - Tự động refresh mỗi 5 giây
  - Link download file DOCX khi hoàn thành
  
- **login-modal.jsp**: Modal đăng nhập
- **signup-modal.jsp**: Modal đăng ký
- **header.jsp**: Header chung của ứng dụng

**CSS**: `css/` - Styling cho giao diện
**JavaScript**: `js/` - Xử lý tương tác client-side

### 3. CONTROLLER (Điều khiển)
**Vị trí**: `src/main/java/controller/`

- **LoginServlet.java**: Xử lý đăng nhập/đăng ký
  - Endpoint: `/login`, `/signup`
  - Xác thực thông tin người dùng
  - Quản lý session
  
- **ConverterServlet.java**: Xử lý upload và chuyển đổi
  - Endpoint: `/converter`
  - Nhận file PDF từ user
  - Thêm vào hàng đợi xử lý
  - Trả về phản hồi ngay lập tức
  
- **ListConvertServlet.java**: Hiển thị danh sách
  - Endpoint: `/listconvert`
  - Lấy danh sách file đã chuyển đổi của user
  - Forward đến viewListConvert.jsp
  
- **DownloadFileServlet.java**: Xử lý tải file
  - Endpoint: `/download`
  - Stream file DOCX về cho user
  - Kiểm tra quyền truy cập

### Nguyên tắc hoạt động của MVC trong ứng dụng

```
User Request → Controller → Model (BO → DAO → Database)
                  ↓                      ↓
               Response ← View ← Data (từ Model)
```

**Ví dụ cụ thể: Quy trình upload file**
1. User submit form tại **index.jsp** (VIEW)
2. Request được gửi đến **ConverterServlet** (CONTROLLER)
3. Servlet gọi **ConverterBO** (MODEL - BO) để xử lý
4. BO gọi **ConverterDAO** (MODEL - DAO) để lưu vào database
5. DAO tương tác với **Database** (MODEL - Data)
6. Task được thêm vào **ConversionQueue** (MODEL - BO)
7. Servlet trả về response "Đang thực hiện" (CONTROLLER → VIEW)
8. **ConversionWorker** (MODEL - BO) xử lý task nền
9. User xem kết quả tại **viewListConvert.jsp** (VIEW)

---

## YÊU CẦU HỆ THỐNG

### 1. Java Development Kit (JDK)
- **Phiên bản**: JDK 17 hoặc cao hơn
- **Tải về**: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) hoặc [OpenJDK](https://openjdk.org/)

### 2. Apache Maven
- **Phiên bản**: Maven 3.6 hoặc cao hơn
- **Tải về**: [Apache Maven](https://maven.apache.org/download.cgi)
- **Mục đích**: Quản lý dependencies và build project

### 3. MySQL Database
- **Phiên bản**: MySQL 5.7 hoặc cao hơn
- **Tải về**: [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
- **Cấu hình**: 
  - Database name: `pdf_convertion`
  - Username/Password: Cấu hình trong `Utils.java`

### 4. Apache Tomcat
- **Phiên bản**: Tomcat 10 hoặc cao hơn (hỗ trợ Jakarta EE)
- **Tải về**: [Apache Tomcat](https://tomcat.apache.org/download-10.cgi)

### 5. Thư viện Spire (Quan trọng!)
Dự án sử dụng **Spire.PDF** và **Spire.Doc** - các thư viện độc quyền không có sẵn trên Maven Central.

**Cách cài đặt:**
1. Tải về Spire.PDF Free và Spire.Doc Free từ [trang chính thức](https://www.e-iceblue.com/)
2. Cài đặt vào local Maven repository:

```bash
mvn install:install-file -Dfile=spire.pdf.free-5.1.0.jar \
    -DgroupId=e-iceblue \
    -DartifactId=spire.pdf.free \
    -Dversion=5.1.0 \
    -Dpackaging=jar

mvn install:install-file -Dfile=spire.doc.free-5.2.0.jar \
    -DgroupId=e-iceblue \
    -DartifactId=spire.doc.free \
    -Dversion=5.2.0 \
    -Dpackaging=jar
```

**Lưu ý**: Phiên bản miễn phí có giới hạn số trang.

---

## HƯỚNG DẪN CÀI ĐẶT

### Bước 1: Clone Repository
```bash
git clone <repository-url>
cd PDF_CONVERTION
```

### Bước 2: Cài đặt Database

#### Tùy chọn 1: Cài đặt mới (Khuyên dùng)
Sử dụng file `setup.sql` để tạo database và bảng:

```bash
mysql -u root -p < setup.sql
```

File này sẽ tạo:
- Database: `pdf_convertion`
- Bảng `users` (tài khoản người dùng)
- Bảng `uploads` (lịch sử chuyển đổi)
- Indexes để tối ưu hiệu suất
- User mẫu: username=`admin`, password=`admin123`

#### Tùy chọn 2: Cài đặt thủ công
Nếu muốn tạo thủ công:

```sql
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
```

### Bước 3: Cấu hình kết nối Database
Mở file `src/main/java/utils/Utils.java` và cập nhật thông tin:

```java
conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/pdf_convertion?useSSL=false&allowPublicKeyRetrieval=true", 
    "root",           // Thay bằng MySQL username của bạn
    "your_password"   // Thay bằng MySQL password của bạn
);
```

### Bước 4: Cài đặt thư viện Spire
Xem mục "Yêu cầu hệ thống" → "Thư viện Spire" ở trên.

### Bước 5: Build Project
```bash
mvn clean package
```

Lệnh này sẽ:
- Download tất cả dependencies
- Compile source code
- Chạy tests
- Tạo file WAR trong thư mục `target/`

### Bước 6: Deploy lên Tomcat

**Cách 1: Copy WAR file**
```bash
cp target/PDF_CONVERTION-1.0-SNAPSHOT.war /path/to/tomcat/webapps/
```

**Cách 2: Deploy qua Tomcat Manager**
- Truy cập: http://localhost:8080/manager/html
- Chọn "WAR file to deploy"
- Upload file WAR
- Click "Deploy"

### Bước 7: Khởi động Tomcat
```bash
# Linux/Mac
/path/to/tomcat/bin/startup.sh

# Windows
C:\path\to\tomcat\bin\startup.bat
```

### Bước 8: Truy cập ứng dụng
Mở trình duyệt và truy cập:
```
http://localhost:8080/PDF_CONVERTION-1.0-SNAPSHOT/
```

**Đăng nhập với tài khoản mẫu:**
- Username: `admin`
- Password: `admin123`

---

## HƯỚNG DẪN SỬ DỤNG

### 1. Đăng nhập / Đăng ký

#### Đăng nhập
1. Truy cập trang chủ
2. Click nút **"Đăng nhập"** ở góc phải trên
3. Nhập **Username** và **Password**
4. Click **"Đăng nhập"**

#### Đăng ký tài khoản mới
1. Click nút **"Đăng ký"**
2. Nhập thông tin:
   - Username (tên đăng nhập)
   - Password (mật khẩu)
   - Email (địa chỉ email)
3. Click **"Đăng ký"**
4. Sau khi đăng ký thành công, sử dụng username/password để đăng nhập

### 2. Chuyển đổi file PDF sang DOCX

#### Bước 1: Chọn file PDF
1. Sau khi đăng nhập, bạn sẽ ở trang chủ
2. Click vào nút **"Chọn file PDF"** hoặc **"Choose PDF file"**
3. Chọn file PDF từ máy tính của bạn
4. Tên file sẽ hiện ra bên cạnh nút chọn file

#### Bước 2: Upload và chuyển đổi
1. Click nút **"Upload và chuyển đổi"** hoặc **"Convert"**
2. Hệ thống sẽ hiển thị thông báo: **"Đang thực hiện"**
3. Thông báo cho biết:
   - File đã được thêm vào hàng đợi xử lý
   - Mã số task (Task ID)
   - Link đến trang danh sách chuyển đổi

**Lưu ý quan trọng**: 
- Bạn KHÔNG cần chờ file chuyển đổi xong
- Hệ thống xử lý file ở chế độ nền (background)
- Bạn có thể thoát trang hoặc upload thêm file khác

### 3. Xem trạng thái chuyển đổi

#### Truy cập danh sách
1. Click vào link **"danh sách chuyển đổi"** trong thông báo
2. Hoặc click nút **"Xem danh sách đã chuyển đổi"** ở header
3. Hoặc truy cập trực tiếp: `/PDF_CONVERTION-1.0-SNAPSHOT/listconvert`

#### Hiểu ý nghĩa các trạng thái
Trang danh sách hiển thị các cột:
- **Tên file gốc**: Tên file PDF bạn đã upload
- **File đã chuyển đổi**: Tên file DOCX kết quả
- **Trạng thái**: Tình trạng xử lý
- **Ngày giờ**: Thời gian upload

**Các trạng thái có thể có:**

| Trạng thái | Màu sắc | Ý nghĩa | Hành động |
|-----------|---------|---------|-----------|
| **Đang thực hiện (queued)** | 🟠 Cam | File đang chờ trong hàng đợi | Chờ đợi |
| **Đang thực hiện (processing)** | 🔵 Xanh dương | File đang được chuyển đổi | Chờ đợi |
| **Hoàn thành** | 🟢 Xanh lá | Chuyển đổi thành công | Có thể tải về |
| **Thất bại** | 🔴 Đỏ | Chuyển đổi lỗi | Thử lại với file khác |

#### Tự động cập nhật
- Trang danh sách **tự động refresh mỗi 5 giây**
- Bạn sẽ thấy trạng thái thay đổi theo thời gian thực
- Không cần F5 để cập nhật

### 4. Tải file DOCX về máy

#### Khi nào có thể tải?
- Chỉ khi trạng thái là **"Hoàn thành"** (màu xanh lá)
- Link tải sẽ xuất hiện ở cột **"File đã chuyển đổi"**

#### Cách tải file
1. Tại trang danh sách, tìm file có trạng thái "Hoàn thành"
2. Click vào **tên file DOCX** ở cột "File đã chuyển đổi"
3. File sẽ tự động tải về máy tính của bạn
4. Mở file bằng Microsoft Word hoặc phần mềm tương thích

### 5. Upload nhiều file cùng lúc

Bạn có thể upload nhiều file PDF:
1. Upload file thứ nhất → Nhận thông báo "Đang thực hiện"
2. Quay lại trang chủ
3. Upload file thứ hai → Nhận thông báo "Đang thực hiện"
4. Tiếp tục với file thứ ba, thứ tư...

**Hệ thống sẽ:**
- Xử lý các file tuần tự (từng file một)
- File đầu tiên sẽ được xử lý trước (FIFO - First In First Out)
- Các file sau sẽ ở trạng thái "queued" (đang chờ)
- Khi file trước xong, file tiếp theo sẽ được xử lý

### 6. Xem chỉ file của bạn

**Bảo mật và riêng tư:**
- Mỗi user chỉ thấy file của mình
- Bạn KHÔNG thấy file của người khác
- Các file được liên kết với username của bạn

### 7. Đăng xuất

Khi hoàn thành:
1. Click nút **"Đăng xuất"** ở góc phải trên
2. Bạn sẽ được chuyển về trang đăng nhập
3. Dữ liệu của bạn vẫn được lưu trữ an toàn

---

## CÁC TÍNH NĂNG CHÍNH

### 1. Hệ thống hàng đợi bất đồng bộ ⭐
- **Không cần chờ đợi**: Upload file và nhận phản hồi ngay lập tức
- **Xử lý song song**: 6 worker threads xử lý đồng thời tối đa 6 file cùng lúc
- **Đa người dùng**: Nhiều người dùng có thể upload và xử lý file đồng thời
- **Xử lý nền**: File được chuyển đổi ở background bởi worker threads

### 2. Theo dõi trạng thái real-time 📊
- **4 trạng thái**: queued → processing → completed/failed
- **Màu sắc trực quan**: Dễ dàng phân biệt trạng thái
- **Tự động cập nhật**: Không cần refresh thủ công

### 3. Quản lý tài khoản 👤
- **Đăng ký/Đăng nhập**: Hệ thống xác thực người dùng
- **Session management**: Duy trì trạng thái đăng nhập
- **Bảo mật**: Mỗi user chỉ thấy dữ liệu của mình

### 4. Lịch sử chuyển đổi 📁
- **Lưu trữ vĩnh viễn**: Tất cả chuyển đổi được lưu trong database
- **Tra cứu dễ dàng**: Xem lại các file đã chuyển đổi trước đó
- **Thông tin chi tiết**: Tên file, ngày giờ, trạng thái

### 5. Giao diện thân thiện 🎨
- **Responsive**: Hoạt động tốt trên mọi kích thước màn hình
- **Đơn giản**: Dễ sử dụng, không cần hướng dẫn phức tạp
- **Tiếng Việt**: Giao diện và thông báo bằng tiếng Việt

### 6. Xử lý lỗi thông minh 🛡️
- **Phát hiện lỗi**: Tự động nhận diện file bị lỗi
- **Thông báo rõ ràng**: Status "Thất bại" khi có vấn đề
- **Không crash**: Lỗi một file không ảnh hưởng file khác

---

## XỬ LÝ SỰ CỐ

### Vấn đề 1: Không đăng nhập được

**Nguyên nhân có thể:**
- Sai username hoặc password
- Tài khoản chưa được tạo
- Database không kết nối

**Giải pháp:**
1. Kiểm tra lại username/password (phân biệt hoa thường)
2. Thử đăng ký tài khoản mới
3. Kiểm tra MySQL đã chạy chưa:
   ```bash
   # Linux/Mac
   sudo service mysql status
   
   # Windows
   services.msc → Tìm MySQL
   ```
4. Kiểm tra cấu hình database trong `Utils.java`

### Vấn đề 2: File upload nhưng không thấy trong danh sách

**Nguyên nhân:**
- Chưa đăng nhập
- Database không lưu được
- Lỗi quyền truy cập thư mục upload

**Giải pháp:**
1. Đảm bảo đã đăng nhập
2. Kiểm tra logs trong Tomcat:
   ```bash
   tail -f /path/to/tomcat/logs/catalina.out
   ```
3. Kiểm tra quyền thư mục upload:
   ```bash
   ls -la src/main/webapp/uploads/
   ```
4. Kiểm tra database:
   ```sql
   SELECT * FROM uploads WHERE username='your_username' ORDER BY date DESC;
   ```

### Vấn đề 3: Trạng thái luôn là "Đang thực hiện"

**Nguyên nhân:**
- Worker thread không chạy
- Lỗi trong quá trình chuyển đổi
- Thư viện Spire chưa cài đúng

**Giải pháp:**
1. Kiểm tra logs xem có lỗi không
2. Kiểm tra thư viện Spire đã cài đặt:
   ```bash
   ls ~/.m2/repository/e-iceblue/
   ```
3. Restart Tomcat:
   ```bash
   /path/to/tomcat/bin/shutdown.sh
   /path/to/tomcat/bin/startup.sh
   ```
4. Kiểm tra file PDF có bị hỏng không (thử với file khác)

### Vấn đề 4: Download file bị lỗi

**Nguyên nhân:**
- File chưa hoàn thành chuyển đổi
- File bị xóa khỏi server
- Lỗi quyền truy cập

**Giải pháp:**
1. Đảm bảo trạng thái là "Hoàn thành" (màu xanh)
2. Kiểm tra file có tồn tại trên server:
   ```bash
   ls -la src/main/webapp/uploads/*.docx
   ```
3. Thử upload và chuyển đổi lại file

### Vấn đề 5: Lỗi "Build failed" khi chạy Maven

**Nguyên nhân:**
- Thiếu thư viện Spire
- JDK version không đúng
- Internet không ổn định

**Giải pháp:**
1. Cài đặt thư viện Spire (xem phần Yêu cầu hệ thống)
2. Kiểm tra Java version:
   ```bash
   java -version  # Phải là 17 hoặc cao hơn
   ```
3. Update Maven:
   ```bash
   mvn clean install -U
   ```

### Vấn đề 6: Tomcat không start

**Nguyên nhân:**
- Port 8080 đã được sử dụng
- JDK chưa cài đặt đúng
- Thiếu quyền thực thi

**Giải pháp:**
1. Kiểm tra port 8080:
   ```bash
   # Linux/Mac
   lsof -i :8080
   
   # Windows
   netstat -ano | findstr :8080
   ```
2. Đổi port Tomcat trong `conf/server.xml`:
   ```xml
   <Connector port="8081" protocol="HTTP/1.1" />
   ```
3. Cấp quyền thực thi:
   ```bash
   chmod +x /path/to/tomcat/bin/*.sh
   ```

---

## GIỚI HẠN VÀ LƯU Ý

### Giới hạn của phiên bản Free Spire
- **Spire.PDF Free**: Tối đa 10 trang cho mỗi file PDF
- **Spire.Doc Free**: Tối đa 500 đoạn văn và 25 bảng
- Nếu vượt quá giới hạn, cần mua bản Pro

### Hiệu suất
- **Tốc độ**: Phụ thuộc vào kích thước file và số trang
- **File nhỏ** (1-5 trang): ~5-10 giây
- **File lớn** (10+ trang): Có thể lâu hơn
- **Xử lý đồng thời**: 6 worker threads xử lý song song tối đa 6 file cùng lúc
- **Thông lượng**: Tăng 6 lần so với xử lý tuần tự (tối đa 720-4320 file/giờ)

### Bảo mật
- **Mật khẩu**: Lưu plaintext (nên hash trong production)
- **Session**: Cookie-based, timeout sau 30 phút không hoạt động
- **File upload**: Lưu trữ local, không có scan virus

### Yêu cầu lưu trữ
- **Upload folder**: Cần đủ dung lượng cho file PDF và DOCX
- **Database**: Lưu trữ metadata, không lưu file
- **Backup**: Nên backup định kỳ thư mục uploads và database

---

## HỖ TRỢ VÀ LIÊN HỆ

### Tài liệu bổ sung
- **ARCHITECTURE.md**: Chi tiết kiến trúc hệ thống
- **IMPLEMENTATION_NOTES.md**: Ghi chú kỹ thuật
- **TESTING_GUIDE.md**: Hướng dẫn test hệ thống

### Báo lỗi
Nếu gặp vấn đề, vui lòng cung cấp:
1. Thông báo lỗi đầy đủ
2. File log từ Tomcat
3. Thông tin hệ thống (OS, Java version, MySQL version)
4. Các bước tái hiện lỗi

### Đóng góp
Mọi đóng góp đều được chào đón:
- Báo lỗi (bug reports)
- Đề xuất tính năng (feature requests)
- Pull requests
- Cải thiện tài liệu

---

## GIẤY PHÉP

[Thêm thông tin giấy phép của bạn ở đây]

---

**Phiên bản**: 2.0  
**Ngày cập nhật**: 2025-11-16  
**Tác giả**: PDF Conversion Team  
**Trạng thái**: Production Ready ✅

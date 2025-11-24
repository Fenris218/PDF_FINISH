# THIẾT KẾ MÔ HÌNH MVC - HỆ THỐNG CHUYỂN ĐỔI PDF SANG DOCX

## Sơ đồ kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                    BACKGROUND                                        │
│                                                                                      │
│  ┌───────────────────────────────────────────────────────────────────────────┐     │
│  │                          APPLICATION CONTAINER                             │     │
│  │                                                                            │     │
│  │  ┌─────────────────────────────────────────────────────────────────┐     │     │
│  │  │                    VIEW (JSP) - Presentation Layer               │     │     │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │     │     │
│  │  │  │  index.jsp   │  │viewListConvert│  │login-modal   │          │     │     │
│  │  │  │(Upload Form) │  │  .jsp         │  │  .jsp        │          │     │     │
│  │  │  │              │  │(Danh sách)    │  │(Đăng nhập)   │          │     │     │
│  │  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │     │     │
│  │  └─────────┼──────────────────┼──────────────────┼──────────────────┘     │     │
│  │            │                  │                  │                         │     │
│  │            │ HTTP POST        │ HTTP GET         │ HTTP POST               │     │
│  │            ▼                  ▼                  ▼                         │     │
│  │  ┌─────────────────────────────────────────────────────────────────┐     │     │
│  │  │              CONTROLLER (Servlet) - Request Handler              │     │     │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │     │     │
│  │  │  │Converter     │  │ListConvert   │  │Login         │          │     │     │
│  │  │  │Servlet       │  │Servlet       │  │Servlet       │          │     │     │
│  │  │  │              │  │              │  │              │          │     │     │
│  │  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │     │     │
│  │  └─────────┼──────────────────┼──────────────────┼──────────────────┘     │     │
│  │            │                  │                  │                         │     │
│  │            │ saveHistory()    │ getList()        │ checkLogin()            │     │
│  │            │ addTask()        │                  │ addUser()               │     │
│  │            ▼                  ▼                  ▼                         │     │
│  │  ┌──────────────────────────────────────────────────────────────────────┐ │     │
│  │  │                        MODEL - Business Layer                        │ │     │
│  │  │                                                                       │ │     │
│  │  │  ┌────────────────────────────────────────────────────────────┐     │ │     │
│  │  │  │           BEAN (Data Transfer Objects)                      │     │ │     │
│  │  │  │  ┌──────────┐  ┌──────────┐  ┌──────────────┐              │     │ │     │
│  │  │  │  │User.java │  │Upload    │  │ConversionTask│              │     │ │     │
│  │  │  │  │          │  │.java     │  │.java         │              │     │ │     │
│  │  │  │  └──────────┘  └──────────┘  └──────────────┘              │     │ │     │
│  │  │  └────────────────────────────────────────────────────────────┘     │ │     │
│  │  │                                                                       │ │     │
│  │  │  ┌────────────────────────────────────────────────────────────┐     │ │     │
│  │  │  │               BO (Business Objects)                         │     │ │     │
│  │  │  │  ┌──────────┐  ┌──────────┐  ┌───────────────┐             │     │ │     │
│  │  │  │  │LoginBO   │  │Converter │  │ConversionQueue│◄────┐       │     │ │     │
│  │  │  │  │          │  │BO        │  │(Singleton)    │     │       │     │ │     │
│  │  │  │  └────┬─────┘  └────┬─────┘  └───────┬───────┘     │       │     │ │     │
│  │  │  │       │             │                 │             │       │     │ │     │
│  │  │  │       │             │                 ▼             │       │     │ │     │
│  │  │  │       │             │        ┌───────────────┐      │       │     │ │     │
│  │  │  │       │             │        │Conversion     │      │       │     │ │     │
│  │  │  │       │             │        │Worker         │──────┘       │     │ │     │
│  │  │  │       │             │        │(Thread)       │              │     │ │     │
│  │  │  │       │             │        └───────┬───────┘              │     │ │     │
│  │  │  │       │             │                │                      │     │ │     │
│  │  │  │       │             │                ▼                      │     │ │     │
│  │  │  │       │             │        ┌───────────────┐              │     │ │     │
│  │  │  │       │             │        │PdfConvertion  │              │     │ │     │
│  │  │  │       │             │        │Helper         │              │     │ │     │
│  │  │  │       │             │        │(Spire lib)    │              │     │ │     │
│  │  │  │       │             │        └───────────────┘              │     │ │     │
│  │  │  └───────┼─────────────┼──────────────────────────────────────┘     │ │     │
│  │  │          │             │                                             │ │     │
│  │  │          ▼             ▼                                             │ │     │
│  │  │  ┌────────────────────────────────────────────────────────────┐     │ │     │
│  │  │  │               DAO (Data Access Objects)                     │     │ │     │
│  │  │  │  ┌──────────┐          ┌──────────┐                        │     │ │     │
│  │  │  │  │LoginDAO  │          │Converter │                        │     │ │     │
│  │  │  │  │          │          │DAO       │                        │     │ │     │
│  │  │  │  └────┬─────┘          └────┬─────┘                        │     │ │     │
│  │  │  └───────┼─────────────────────┼────────────────────────────  │     │ │     │
│  │  └──────────┼─────────────────────┼──────────────────────────────┘     │     │
│  │             │                     │                                     │     │
│  │             │ SQL: SELECT/INSERT  │ SQL: SELECT/INSERT/UPDATE           │     │
│  │             ▼                     ▼                                     │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
│                │                     │                                            │
│                ▼                     ▼                                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │                      DATABASE (MySQL)                                    │     │
│  │  ┌──────────────────────┐       ┌──────────────────────┐                │     │
│  │  │  users table         │       │  uploads table       │                │     │
│  │  │  ─────────────       │       │  ────────────────    │                │     │
│  │  │  • username (PK)     │  1:N  │  • id (PK)           │                │     │
│  │  │  • password          │◄──────│  • username (FK)     │                │     │
│  │  │  • email             │       │  • fileNameUpload    │                │     │
│  │  │  • created_at        │       │  • fileNameOutput    │                │     │
│  │  │                      │       │  • status            │                │     │
│  │  │                      │       │  • date              │                │     │
│  │  └──────────────────────┘       └──────────────────────┘                │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

## Chi tiết các thành phần

### 1. VIEW Layer (Lớp Giao diện)
**Vị trí:** `src/main/webapp/*.jsp`

| Tên File | Chức năng | Hiển thị |
|----------|-----------|----------|
| `index.jsp` | Trang chủ với form upload | Form chọn file PDF, nút upload |
| `viewListConvert.jsp` | Danh sách chuyển đổi | Bảng hiển thị lịch sử, trạng thái |
| `login-modal.jsp` | Modal đăng nhập | Form username/password |
| `signup-modal.jsp` | Modal đăng ký | Form đăng ký tài khoản |
| `header.jsp` | Header chung | Menu, user info |

**Trách nhiệm:**
- Hiển thị giao diện người dùng
- Thu thập input từ user
- Gửi HTTP request đến Controller
- Render dữ liệu từ Model

---

### 2. CONTROLLER Layer (Lớp Điều khiển)
**Vị trí:** `src/main/java/controller/*.java`

| Servlet | URL Pattern | Chức năng |
|---------|-------------|-----------|
| `ConverterServlet` | `/converter` | Nhận file upload, lưu file, tạo task, thêm vào queue |
| `ListConvertServlet` | `/listconvert` | Lấy danh sách chuyển đổi, forward đến JSP |
| `LoginServlet` | `/login`, `/signup` | Xác thực đăng nhập, đăng ký user |
| `DownloadFileServlet` | `/download` | Stream file DOCX về client |

**Trách nhiệm:**
- Xử lý HTTP request/response
- Validate input data
- Gọi Business Objects thực hiện logic
- Quản lý session
- Forward/redirect đến View

---

### 3. MODEL Layer (Lớp Mô hình)

#### 3.1. BEAN (Data Transfer Objects)
**Vị trí:** `src/main/java/model/BEAN/*.java`

##### User.java
```java
class User {
    private String username;     // Tên đăng nhập
    private String password;     // Mật khẩu
    private String email;        // Email
    // Getters & Setters
}
```

##### Upload.java
```java
class Upload {
    private int id;                        // ID tự tăng
    private String username;               // Username người upload
    private String fileNameUpload;         // Tên file PDF gốc
    private String fileNameOutput;         // Tên file DOCX (hiển thị)
    private String fileNameOutputInServer; // Tên file thực trên server
    private Timestamp date;                // Thời gian upload
    private String status;                 // Trạng thái: queued/processing/completed/failed
    // Getters & Setters
}
```

##### ConversionTask.java
```java
class ConversionTask {
    private int taskId;              // ID task
    private String username;         // Username
    private String inputFilePath;    // Đường dẫn file PDF
    private String outputFilePath;   // Đường dẫn file DOCX
    private int uploadId;            // ID trong database
    // Getters & Setters
}
```

**Trách nhiệm:**
- Lưu trữ dữ liệu
- Transfer data giữa các layer
- Không chứa business logic

---

#### 3.2. BO (Business Objects)
**Vị trí:** `src/main/java/model/BO/*.java`

##### LoginBO.java
```java
class LoginBO {
    private LoginDAO dao;
    
    public boolean checkLogin(String username, String password) {
        return dao.checkLogin(username, password);
    }
    
    public boolean addUser(User user) {
        return dao.addUser(user);
    }
}
```
**Chức năng:** Xử lý logic đăng nhập/đăng ký

##### ConverterBO.java
```java
class ConverterBO {
    private ConverterDAO dao;
    
    public int saveHistory(Upload upload) {
        return dao.saveHistory(upload);
    }
    
    public List<Upload> getListFileConvert(String username) {
        return dao.getListFileConvert(username);
    }
    
    public void updateStatus(int uploadId, String status) {
        dao.updateStatus(uploadId, status);
    }
}
```
**Chức năng:** Xử lý logic chuyển đổi, quản lý lịch sử

##### ConversionQueue.java (Singleton)
```java
class ConversionQueue {
    private static ConversionQueue instance;
    private BlockingQueue<ConversionTask> queue;
    private ExecutorService executorService;
    private AtomicInteger taskIdCounter;
    private static final int NUM_WORKERS = 6;
    
    private ConversionQueue() {
        queue = new LinkedBlockingQueue<>();
        taskIdCounter = new AtomicInteger(0);
        executorService = Executors.newFixedThreadPool(NUM_WORKERS);
        // Start 6 worker threads
        for (int i = 0; i < NUM_WORKERS; i++) {
            executorService.submit(new ConversionWorker(queue, i + 1));
        }
    }
    
    public static synchronized ConversionQueue getInstance() {
        if (instance == null) {
            instance = new ConversionQueue();
        }
        return instance;
    }
    
    public int addTask(ConversionTask task) {
        int taskId = taskIdCounter.incrementAndGet();
        task.setId(taskId);
        queue.put(task);
        return taskId;
    }
}
```
**Chức năng:** 
- Quản lý hàng đợi conversion
- Singleton pattern - chỉ 1 instance
- Thread-safe queue
- Quản lý pool 6 worker threads
- ExecutorService để quản lý vòng đời thread

##### ConversionWorker.java (Runnable)
```java
class ConversionWorker implements Runnable {
    private BlockingQueue<ConversionTask> queue;
    private ConverterBO converterBO;
    private int workerId;
    
    public void run() {
        String workerName = "ConversionWorker-" + workerId;
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ConversionTask task = queue.take(); // Blocking
                processTask(task, workerName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void processTask(ConversionTask task, String workerName) {
        // Update status to "processing"
        converterBO.updateStatus(task.getUsername(), 
                                task.getFileNameInServer(), "processing");
        
        try {
            // Convert PDF to DOCX using ConverterThread
            ConverterThread thread = new ConverterThread(task.getFilePathInServer());
            thread.start();
            thread.join();
            
            // Delete uploaded PDF file
            Utils.deleteFile(task.getFilePathInServer());
            
            // Update status to "completed"
            converterBO.updateStatus(task.getUsername(), 
                                    task.getFileNameInServer(), "completed");
        } catch (Exception e) {
            // Update status to "failed"
            converterBO.updateStatus(task.getUsername(), 
                                    task.getFileNameInServer(), "failed");
        }
    }
}
```
**Chức năng:** 
- Background thread xử lý conversion (implements Runnable)
- Lấy task từ queue (blocking)
- Sử dụng ConverterThread để chuyển đổi
- Xóa file PDF gốc sau khi xử lý
- Cập nhật status (queued → processing → completed/failed)
- Xử lý lỗi và logging với worker ID

##### PdfConvertionHelper.java
```java
class PdfConvertionHelper {
    private static final int MAX_PAGES_PER_FILE = 50;
    private static final int MAX_THREAD_POOL_SIZE = 12;
    private static final int RETRY_ATTEMPTS = 3;
    
    public static ConversionResult convertPdfToDoc(String fileInput) {
        // Validate file (exists, is PDF, size < 100MB)
        // Split PDF into chunks of 50 pages
        List<String> chunkPdfPaths = splitPdf(fileInput);
        
        // Convert chunks in parallel using 12-thread pool
        List<String> docxPaths = convertChunkPdfToDocx(chunkPdfPaths);
        
        // Combine DOCX files
        boolean combined = CombineDocx.combineFiles(docxPaths, outputPath);
        
        // Cleanup temporary files
        cleanupTempFiles(tempFiles);
        
        return ConversionResult.success(outputPath);
    }
    
    private static List<String> convertChunkPdfToDocx(List<String> chunks) {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE);
        // Submit conversion tasks with retry logic
        // Each chunk converted by PdfToDocxConverter
        // Wait with timeout (10 min per chunk, 30 min total)
        return docxPaths;
    }
}
```
**Chức năng:** 
- Chia PDF thành chunks (50 trang/chunk)
- Chuyển đổi song song với 12 threads
- Retry mechanism (3 lần, exponential backoff)
- Kết hợp DOCX chunks thành file cuối
- Quản lý timeout (10 phút/chunk, 30 phút tổng)
- Dọn dẹp file tạm tự động
- Validation: kiểm tra file tồn tại, định dạng PDF, kích thước < 100MB

**Trách nhiệm BO Layer:**
- Chứa business logic
- Điều phối giữa Controller và DAO
- Xử lý quy tắc nghiệp vụ
- Quản lý transaction

---

#### 3.3. DAO (Data Access Objects)
**Vị trí:** `src/main/java/model/DAO/*.java`

##### LoginDAO.java
```java
class LoginDAO {
    public boolean checkLogin(String username, String password) {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";
        // Execute query
        // Return true if user exists
    }
    
    public boolean addUser(User user) {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        // Execute insert
        // Return success/failure
    }
}
```

##### ConverterDAO.java
```java
class ConverterDAO {
    public int saveHistory(Upload upload) {
        String sql = "INSERT INTO uploads (username, fileNameUpload, " +
                     "fileNameOutput, fileNameOutputInServer, status) " +
                     "VALUES (?, ?, ?, ?, ?)";
        // Execute insert
        // Return generated ID
    }
    
    public List<Upload> getListFileConvert(String username) {
        String sql = "SELECT * FROM uploads WHERE username=? " +
                     "ORDER BY date DESC";
        // Execute query
        // Return list of Upload objects
    }
    
    public void updateStatus(int uploadId, String status) {
        String sql = "UPDATE uploads SET status=? WHERE id=?";
        // Execute update
    }
}
```

**Trách nhiệm:**
- Tương tác với database
- Thực thi SQL queries
- Map database records → Java objects
- Không chứa business logic

---

### 4. DATABASE Layer (Lớp Cơ sở dữ liệu)

#### users table
```sql
CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### uploads table
```sql
CREATE TABLE uploads (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    fileNameUpload VARCHAR(255) NOT NULL,
    fileNameOutput VARCHAR(255) NOT NULL,
    fileNameOutputInServer VARCHAR(255) NOT NULL,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'queued',
    error_message TEXT,
    FOREIGN KEY (username) REFERENCES users(username)
);
```

**Status values:**
- `queued`: Đang chờ trong hàng đợi
- `processing`: Đang xử lý
- `completed`: Hoàn thành
- `failed`: Thất bại

---

## Luồng xử lý chi tiết

### Luồng 1: Upload và Chuyển đổi File

```
┌─────────┐
│  USER   │
└────┬────┘
     │ 1. Chọn file PDF
     │
     ▼
┌────────────────┐
│  index.jsp     │ (VIEW)
└────┬───────────┘
     │ 2. Submit form (POST /converter)
     │
     ▼
┌─────────────────────┐
│ ConverterServlet    │ (CONTROLLER)
│                     │
│ 1. Validate session │
│ 2. Save file        │
│ 3. Create Upload    │
└────┬────────────────┘
     │ 3. saveHistory()
     │
     ▼
┌─────────────────────┐
│   ConverterBO       │ (MODEL - BO)
│                     │
│ 1. Validate data    │
└────┬────────────────┘
     │ 4. saveHistory()
     │
     ▼
┌─────────────────────┐
│   ConverterDAO      │ (MODEL - DAO)
│                     │
│ INSERT INTO uploads │
│ status='queued'     │
└────┬────────────────┘
     │ 5. Return upload ID
     │
     ├──────────────────┐
     │                  │
     ▼                  ▼
┌──────────┐    ┌──────────────────┐
│ Response │    │ ConversionQueue  │ (MODEL - BO)
│ to User  │    │ .addTask()       │
└──────────┘    └────┬─────────────┘
                     │ 6. Add to queue
                     │
                     ▼
              ┌─────────────────┐
              │ BlockingQueue   │
              │ [Task]          │
              └────┬────────────┘
                   │ 7. Worker takes task
                   │
                   ▼
              ┌─────────────────┐
              │ConversionWorker │ (MODEL - BO)
              │  (Thread)       │
              └────┬────────────┘
                   │
                   ├─► Update status='processing'
                   │   (via ConverterBO → ConverterDAO)
                   │
                   ├─► Convert PDF to DOCX
                   │   (via PdfConvertionHelper)
                   │
                   └─► Update status='completed'
                       (via ConverterBO → ConverterDAO)
                          │
                          ▼
                    [File sẵn sàng]
```

### Luồng 2: Xem Danh Sách

```
USER → viewListConvert.jsp (VIEW)
    │
    └─► HTTP GET /listconvert
           │
           ▼
       ListConvertServlet (CONTROLLER)
           │
           ├─► Get username from session
           │
           └─► getListFileConvert(username)
                  │
                  ▼
              ConverterBO (MODEL - BO)
                  │
                  └─► getListFileConvert(username)
                         │
                         ▼
                     ConverterDAO (MODEL - DAO)
                         │
                         ├─► SELECT * FROM uploads 
                         │   WHERE username=?
                         │   ORDER BY date DESC
                         │
                         └─► Return List<Upload>
                                │
                                ▼
                         ListConvertServlet
                                │
                                ├─► setAttribute("list", uploads)
                                │
                                └─► forward to viewListConvert.jsp
                                       │
                                       ▼
                                   Hiển thị bảng
                                   (Auto-refresh 5s)
```

### Luồng 3: Đăng nhập

```
USER → login-modal.jsp (VIEW)
    │
    └─► POST /login (username, password)
           │
           ▼
       LoginServlet (CONTROLLER)
           │
           └─► checkLogin(username, password)
                  │
                  ▼
              LoginBO (MODEL - BO)
                  │
                  └─► checkLogin(username, password)
                         │
                         ▼
                     LoginDAO (MODEL - DAO)
                         │
                         ├─► SELECT * FROM users
                         │   WHERE username=? AND password=?
                         │
                         └─► Return boolean
                                │
                                ▼
                         LoginServlet
                                │
                                ├─► If success:
                                │   session.setAttribute("username", username)
                                │   redirect to index.jsp
                                │
                                └─► If failure:
                                    forward to login with error
```

---

## Design Patterns được sử dụng

### 1. MVC Pattern
- **Model**: BEAN + BO + DAO + Database
- **View**: JSP pages
- **Controller**: Servlets
- **Lợi ích**: Tách biệt rõ ràng, dễ maintain

### 2. Singleton Pattern
- **Class**: ConversionQueue
- **Mục đích**: Chỉ 1 queue instance trong app
- **Implementation**: Double-checked locking

### 3. DAO Pattern
- **Classes**: LoginDAO, ConverterDAO
- **Mục đích**: Tách biệt data access logic
- **Lợi ích**: Dễ thay đổi database

### 4. Producer-Consumer Pattern
- **Producer**: ConverterServlet (tạo task)
- **Queue**: ConversionQueue (BlockingQueue)
- **Consumer**: ConversionWorker (xử lý task)
- **Lợi ích**: Xử lý bất đồng bộ

### 5. Thread Pool Pattern (Simplified)
- **Worker**: ConversionWorker thread
- **Current**: Single worker thread
- **Scalable**: Có thể mở rộng thành pool

---

## Đặc điểm kiến trúc

✅ **Layered Architecture**: View - Controller - Model - Database
✅ **Separation of Concerns**: Mỗi layer có trách nhiệm riêng
✅ **Asynchronous Processing**: Queue-based background processing
✅ **Thread Safety**: BlockingQueue, AtomicInteger, synchronized
✅ **Scalability**: Có thể mở rộng worker threads
✅ **Maintainability**: Code rõ ràng, dễ bảo trì
✅ **Testability**: Mỗi component test độc lập

---

## Công nghệ sử dụng

- **Language**: Java 17+
- **Web Framework**: Java Servlet & JSP (Jakarta EE 6.0)
- **App Server**: Apache Tomcat 10
- **Database**: MySQL 5.7+
- **Build Tool**: Apache Maven
- **PDF Library**: Apache PDFBox 2.0.30
- **Doc Library**: Apache POI 5.2.5
- **JDBC Driver**: MySQL Connector/J 9.5.0

---

**Tác giả**: PDF Conversion Development Team  
**Ngày tạo**: 2025-11-24  
**Phiên bản**: 1.0

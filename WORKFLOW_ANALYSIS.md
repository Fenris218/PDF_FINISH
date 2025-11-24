# PHÂN TÍCH LUỒNG HOẠT ĐỘNG CHI TIẾT CỦA CHƯƠNG TRÌNH
## Quy trình Upload File và Xử lý PDF Conversion

---

## MỤC LỤC
1. [Tổng quan hệ thống](#1-tổng-quan-hệ-thống)
2. [Luồng hoạt động tổng quan](#2-luồng-hoạt-động-tổng-quan)
3. [Phân tích chi tiết từng bước](#3-phân-tích-chi-tiết-từng-bước)
4. [Luồng xử lý bất đồng bộ](#4-luồng-xử-lý-bất-đồng-bộ)
5. [Các thành phần chính](#5-các-thành-phần-chính)
6. [Sơ đồ tuần tự](#6-sơ-đồ-tuần-tự)
7. [Quản lý trạng thái](#7-quản-lý-trạng-thái)
8. [Ví dụ thực tế](#8-ví-dụ-thực-tế)
9. [Xử lý lỗi và exceptions](#9-xử-lý-lỗi-và-exceptions)
10. [Tối ưu hóa và performance](#10-tối-ưu-hóa-và-performance)

---

## 1. TỔNG QUAN HỆ THỐNG

### 1.1. Kiến trúc tổng thể
Hệ thống PDF Conversion sử dụng **kiến trúc xử lý bất đồng bộ với hàng đợi** để chuyển đổi file PDF sang DOCX.

```
┌─────────────────────────────────────────────────────────────────┐
│                      KIẾN TRÚC TỔNG QUAN                         │
└─────────────────────────────────────────────────────────────────┘

    [User Browser]
         │
         │ HTTP Request (Multipart Form Data)
         │
         ▼
    ┌────────────────────┐
    │ ConverterServlet   │──┐
    │ (Controller)       │  │
    └────────────────────┘  │
         │                  │ Xử lý đồng bộ
         │ saveHistory()    │ (Immediate Response)
         ▼                  │
    ┌────────────────────┐  │
    │   ConverterBO      │  │
    │  (Business Logic)  │  │
    └────────────────────┘  │
         │                  │
         │ INSERT DB        │
         ▼                  │
    ┌────────────────────┐  │
    │  ConverterDAO      │  │
    │  (Data Access)     │  │
    └────────────────────┘  │
         │                  │
         ▼                  │
    ┌────────────────────┐  │
    │   MySQL Database   │  │
    │  (status=queued)   │  │
    └────────────────────┘  │
         │                  │
         │                  │
    ┌────┴────────────────┐ │
    │ ConversionQueue     │◄┘
    │ (Singleton)         │
    │ - BlockingQueue     │
    │ - TaskID Generator  │
    └─────────┬───────────┘
              │
              │ Background Processing
              ▼
    ┌──────────────────────┐
    │  ConversionWorker    │
    │  (Daemon Thread)     │
    │  - take() task       │
    │  - processTask()     │
    └──────────┬───────────┘
               │
               ├─► Update status: processing
               │
               ├─► ConverterThread.start()
               │        │
               │        └─► PdfConversionHelper
               │                 │
               │                 └─► Spire.PDF API
               │
               └─► Update status: completed/failed
```

### 1.2. Đặc điểm chính

**🔄 Xử lý bất đồng bộ (Asynchronous Processing)**
- User không phải chờ đợi quá trình chuyển đổi hoàn thành
- Servlet trả về response ngay lập tức sau khi thêm task vào queue
- Conversion xảy ra ở background thread

**📋 Hệ thống hàng đợi (Queue System)**
- Sử dụng `BlockingQueue` (thread-safe)
- FIFO (First In, First Out) processing
- Tự động xử lý task tuần tự

**📊 Theo dõi trạng thái (Status Tracking)**
- 4 trạng thái: `queued` → `processing` → `completed`/`failed`
- Lưu trữ trong database
- Real-time updates qua auto-refresh

---

## 2. LUỒNG HOẠT ĐỘNG TỔNG QUAN

### 2.1. Sơ đồ luồng tổng quan (High-level Flow)

```
┌──────────────┐
│   1. USER    │
│  Upload PDF  │
└──────┬───────┘
       │
       ▼
┌─────────────────────────────────────┐
│  2. CONTROLLER (ConverterServlet)   │
│  ─────────────────────────────────  │
│  • Nhận file từ HTTP request        │
│  • Lưu file vào server              │
│  • Tạo record trong DB (queued)     │
│  • Thêm task vào ConversionQueue    │
│  • Trả về "Đang thực hiện"          │
└──────┬──────────────────────────────┘
       │
       ├──────────────┬────────────────┐
       │              │                │
       ▼              ▼                ▼
┌──────────┐   ┌────────────┐   ┌──────────────┐
│ Response │   │  Database  │   │ Queue System │
│ to User  │   │ (status:   │   │ (Add task)   │
│ (HTML)   │   │  queued)   │   │              │
└──────────┘   └────────────┘   └──────┬───────┘
                                       │
                                       ▼
                        ┌──────────────────────────┐
                        │  3. BACKGROUND WORKER    │
                        │  ──────────────────────  │
                        │  • Take task from queue  │
                        │  • Update: processing    │
                        │  • Convert PDF → DOCX    │
                        │  • Update: completed     │
                        └──────────┬───────────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │  4. USER VIEW RESULT │
                        │  ─────────────────── │
                        │  • ListConvertServlet│
                        │  • Auto-refresh      │
                        │  • Download file     │
                        └──────────────────────┘
```

### 2.2. Timeline thực tế (Practical Timeline)

```
Time   | User Actions          | Server Processing           | Status
────────┼───────────────────────┼────────────────────────────┼──────────
00:00  │ Click "Choose file"   │                             │
00:01  │ Select document.pdf   │                             │
00:02  │ Click "Upload"        │                             │
       │                       │ → Servlet receives file     │
00:03  │ ← "Đang thực hiện"    │ → Save to /upload/          │ queued
       │    Task ID: 123       │ → INSERT DB (queued)        │
       │                       │ → Add to queue              │
00:04  │ View list             │ → Worker picks up task      │ queued
       │ Status: queued 🟠     │                             │
00:05  │ (Auto-refresh)        │ → Start processing          │ processing
       │ Status: processing 🔵 │ → PDF → DOCX conversion     │
00:06  │ (Auto-refresh)        │ → Converting...             │ processing
       │ Status: processing 🔵 │                             │
00:10  │ (Auto-refresh)        │ → Conversion complete       │ completed
       │ Status: completed 🟢  │ → UPDATE DB (completed)     │
00:11  │ Click download link   │ → Stream DOCX file          │
       │ ← document.docx       │                             │
```

---

## 3. PHÂN TÍCH CHI TIẾT TỪNG BƯỚC

### BƯỚC 1: User Upload File PDF

**Thành phần:** `index.jsp`

**Mô tả chi tiết:**
```html
<!-- Form upload trong index.jsp -->
<form action="./ConverterServlet" method="post" 
      enctype="multipart/form-data">
    <input type="file" name="pdfFile" accept=".pdf">
    <button type="submit">Upload và chuyển đổi</button>
</form>
```

**Những gì xảy ra:**
1. User chọn file PDF từ máy tính
2. Click nút upload
3. Browser gửi HTTP POST request với:
   - Content-Type: `multipart/form-data`
   - Body chứa binary data của file PDF
   - Headers chứa session cookie (username)

**Dữ liệu gửi đi:**
```
POST /ConverterServlet HTTP/1.1
Host: localhost:8080
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA
Cookie: JSESSIONID=ABC123...

------WebKitFormBoundary7MA
Content-Disposition: form-data; name="pdfFile"; filename="document.pdf"
Content-Type: application/pdf

[Binary PDF data...]
------WebKitFormBoundary7MA--
```

---

### BƯỚC 2: ConverterServlet nhận và xử lý request

**File:** `controller/ConverterServlet.java`

**Phân tích từng dòng code:**

```java
// Dòng 40: Lấy tất cả các parts từ multipart request
Collection<Part> parts = request.getParts();
```
- Servlet container (Tomcat) tự động parse multipart data
- `@MultipartConfig` annotation cho phép xử lý file upload
- Parts chứa thông tin file: name, content-type, size, input stream

```java
// Dòng 46-50: Xác định nơi lưu file
String folderUpload = request.getServletContext().getRealPath("/upload");
Date now = new Date();
fileNameUserUpload = of(part.getSubmittedFileName()).getFileName().toString();
fileNameInServer = now.getTime() + "_" + fileNameUserUpload;
```
- `getRealPath("/upload")`: Lấy đường dẫn tuyệt đối tới thư mục upload
  - Ví dụ: `/var/tomcat/webapps/PDF_CONVERTION/upload`
- `now.getTime()`: Timestamp milliseconds (ví dụ: 1700000000000)
- Tên file trên server: `1700000000000_document.pdf`
  - **Mục đích:** Tránh trùng lặp tên file, hỗ trợ multiple uploads

```java
// Dòng 51-53: Tạo thư mục nếu chưa tồn tại
if (!Files.exists(of(folderUpload))) {
    Files.createDirectory(of(folderUpload));
}
```
- Đảm bảo thư mục upload tồn tại
- Chỉ tạo khi lần đầu tiên deploy

```java
// Dòng 55-56: Lưu file lên server
filePathInServer = folderUpload + "/" + fileNameInServer;
part.write(filePathInServer);
```
- `part.write()`: Ghi binary data từ request stream vào file
- File được lưu tạm thời trên server để xử lý

```java
// Dòng 63: Lấy username từ session
String username = (String) request.getSession().getAttribute("username");
```
- Session được tạo khi user đăng nhập (LoginServlet)
- Username dùng để liên kết file với user cụ thể

---

### BƯỚC 3: Lưu thông tin vào Database với status="queued"

```java
// Dòng 67-68: Gọi Business Object để lưu lịch sử
ConverterBO converterBO = new ConverterBO();
converterBO.saveHistoryWithStatus(username, fileNameUserUpload, 
                                   fileNameInServer, "queued");
```

**Flow trong ConverterBO:**

```java
// File: model/BO/ConverterBO.java
public void saveHistoryWithStatus(String username, String fileNameUpload, 
                                   String fileNameInServer, String status) {
    // Trim whitespace (quan trọng để tránh lỗi path)
    fileNameUpload = fileNameUpload.trim();
    fileNameInServer = fileNameInServer.trim();
    
    // Tự động convert tên file output: .pdf → .docx
    converterDAO.saveHistoryWithStatus(
        username, 
        fileNameUpload,                              // document.pdf
        fileNameUpload.replace(".pdf", ".docx"),     // document.docx
        fileNameInServer.replace(".pdf", ".docx"),   // 1700000000000_document.docx
        status                                        // "queued"
    );
}
```

**Flow trong ConverterDAO:**

```java
// File: model/DAO/ConverterDAO.java
public void saveHistoryWithStatus(...) {
    String sql = "INSERT INTO uploads" +
                 "(username, fileNameUpload, fileNameOutput, " +
                 "fileNameOutputInServer, status) " +
                 "VALUES (?,?,?,?,?)";
    
    try (Connection connection = Utils.getConnection()) {
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, username);              // "johndoe"
            pst.setString(2, fileNameUpload);        // "document.pdf"
            pst.setString(3, fileNameOutput);        // "document.docx"
            pst.setString(4, fileNameOutputInServer); // "1700000000000_document.docx"
            pst.setString(5, status);                // "queued"
            pst.executeUpdate();
        }
    }
}
```

**Kết quả trong database:**

```sql
-- Table: uploads
+----+----------+---------------+---------------+---------------------------+---------------------+--------+
| id | username | fileNameUpload| fileNameOutput| fileNameOutputInServer     | date                | status |
+----+----------+---------------+---------------+---------------------------+---------------------+--------+
| 1  | johndoe  | document.pdf  | document.docx | 1700000000000_document.docx| 2024-11-23 10:00:00 | queued |
+----+----------+---------------+---------------+---------------------------+---------------------+--------+
```

---

### BƯỚC 4: Tạo ConversionTask và thêm vào Queue

```java
// Dòng 71-74: Tạo task object và thêm vào queue
ConversionTask task = new ConversionTask(
    0,                      // ID sẽ được gán bởi queue
    username,               // "johndoe"
    filePathInServer,       // "/var/tomcat/.../upload/1700000000000_document.pdf"
    fileNameUserUpload,     // "document.pdf"
    fileNameInServer        // "1700000000000_document.pdf"
);

ConversionQueue queue = ConversionQueue.getInstance();
int taskId = queue.addTask(task);
```

**Phân tích ConversionQueue (Singleton Pattern):**

```java
// File: model/BO/ConversionQueue.java

// 1. Singleton instance - chỉ có 1 queue trong toàn bộ application
private static ConversionQueue instance;

// 2. Thread-safe BlockingQueue - hỗ trợ concurrent access
private final BlockingQueue<ConversionTask> queue;

// 3. Atomic counter - thread-safe ID generation
private final AtomicInteger taskIdCounter;

// 4. Background worker thread
private ConversionWorker worker;

// Constructor: Khởi tạo queue và worker
private ConversionQueue() {
    this.queue = new LinkedBlockingQueue<>();  // Unbounded queue
    this.taskIdCounter = new AtomicInteger(0); // Start from 0
    this.worker = new ConversionWorker(queue); // Tạo worker thread
    this.worker.start();                       // Khởi động ngay
}

// Thread-safe singleton getInstance()
public static synchronized ConversionQueue getInstance() {
    if (instance == null) {
        instance = new ConversionQueue();
    }
    return instance;
}

// Thêm task vào queue
public int addTask(ConversionTask task) {
    // Generate unique task ID (thread-safe)
    int taskId = taskIdCounter.incrementAndGet();  // 1, 2, 3, ...
    task.setId(taskId);
    
    try {
        queue.put(task);  // Blocking method - đợi nếu queue đầy
        System.out.println("Task " + taskId + " added to queue. " +
                          "Queue size: " + queue.size());
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    
    return taskId;
}
```

**Trạng thái queue sau khi add task:**
```
ConversionQueue {
    queue: [Task#1{id=1, username="johndoe", file="document.pdf"}]
    taskIdCounter: 1
    worker: ConversionWorker@Thread-5 [RUNNING]
}
```

---

### BƯỚC 5: Trả về response "Đang thực hiện" cho client

```java
// Dòng 77-86: Tạo HTML response
response.setContentType("text/html; charset=UTF-8");
response.getWriter().println("<html><body>");
response.getWriter().println("<h2>Đang thực hiện</h2>");
response.getWriter().println("<p>File của bạn đã được thêm vào hàng đợi xử lý.</p>");
response.getWriter().println("<p>Mã công việc: " + taskId + "</p>");
response.getWriter().println(
    "<p>Bạn có thể xem kết quả xử lý tại " +
    "<a href='./ListConvertServlet'>danh sách chuyển đổi</a></p>");
response.getWriter().println("<p><a href='./index.jsp'>Quay lại trang chủ</a></p>");
response.getWriter().println("</body></html>");
```

**Response HTML nhận được bởi browser:**
```html
<html>
<body>
    <h2>Đang thực hiện</h2>
    <p>File của bạn đã được thêm vào hàng đợi xử lý.</p>
    <p>Mã công việc: 1</p>
    <p>Bạn có thể xem kết quả xử lý tại 
       <a href='./ListConvertServlet'>danh sách chuyển đổi</a>
    </p>
    <p><a href='./index.jsp'>Quay lại trang chủ</a></p>
</body>
</html>
```

**⏱️ Thời gian xử lý:** ~50-200ms (rất nhanh!)

**🎯 Điểm quan trọng:**
- Servlet KHÔNG chờ conversion hoàn thành
- Response được trả về NGAY SAU KHI add task vào queue
- User có thể tiếp tục sử dụng hệ thống mà không bị block

---

## 4. LUỒNG XỬ LÝ BẤT ĐỒNG BỘ

### BƯỚC 6: ConversionWorker lấy task từ queue

**File:** `model/BO/ConversionWorker.java`

```java
public class ConversionWorker extends Thread {
    private final BlockingQueue<ConversionTask> queue;
    private volatile boolean running = true;
    private final ConverterBO converterBO;
    
    public ConversionWorker(BlockingQueue<ConversionTask> queue) {
        this.queue = queue;
        this.converterBO = new ConverterBO();
        this.setDaemon(true);        // Daemon thread - không block JVM shutdown
        this.setName("ConversionWorker");
    }
    
    @Override
    public void run() {
        System.out.println("ConversionWorker started");
        
        while (running) {  // Infinite loop - chỉ dừng khi shutdown
            try {
                // BLOCKING CALL - đợi cho đến khi có task
                ConversionTask task = queue.take();
                processTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error processing task: " + e.getMessage());
            }
        }
        
        System.out.println("ConversionWorker stopped");
    }
}
```

**Giải thích `queue.take()`:**
- **Blocking method:** Thread sẽ SLEEP nếu queue rỗng
- **Hiệu quả CPU:** Không waste CPU cycles với busy waiting
- **Thread-safe:** Multiple threads có thể call take() an toàn
- **Wake up:** Tự động thức dậy khi có task mới được add

**Minh họa hoạt động:**
```
Time  | Queue State      | Worker State
──────┼──────────────────┼─────────────────────
00:00 │ []               │ Sleeping (waiting for task)
00:01 │ []               │ Sleeping...
00:02 │ []               │ Sleeping...
00:03 │ [Task#1] ← ADD   │ WAKE UP! Take Task#1
00:04 │ []               │ Processing Task#1...
00:05 │ []               │ Processing Task#1...
00:10 │ []               │ Processing Task#1...
00:15 │ []               │ Task#1 DONE! Back to sleep
00:16 │ []               │ Sleeping...
```

---

### BƯỚC 7: Cập nhật status thành "processing"

```java
private void processTask(ConversionTask task) {
    System.out.println("Processing task " + task.getId() + 
                      " for user " + task.getUsername());
    
    // Update status to processing
    task.setStatus("processing");
    converterBO.updateStatus(task.getUsername(), 
                            task.getFileNameInServer(), 
                            "processing");
    // ...
}
```

**SQL được thực thi:**
```sql
UPDATE uploads 
SET status = 'processing' 
WHERE username = 'johndoe' 
  AND fileNameOutputInServer = '1700000000000_document.docx';
```

**Database sau update:**
```sql
+----+----------+---------------+--------+---------------------+------------+
| id | username | fileNameUpload| ...    | date                | status     |
+----+----------+---------------+--------+---------------------+------------+
| 1  | johndoe  | document.pdf  | ...    | 2024-11-23 10:00:00 | processing |
+----+----------+---------------+--------+---------------------+------------+
```

**🔔 Lúc này nếu user refresh trang danh sách:**
- Status hiển thị: "Đang thực hiện (processing)" với icon xoay 🔵
- User biết file đang được xử lý

---

### BƯỚC 8: Thực hiện chuyển đổi PDF → DOCX

```java
try {
    // Perform the actual conversion
    ConverterThread thread = new ConverterThread(task.getFilePathInServer());
    thread.start();    // Start conversion thread
    thread.join();     // Wait for completion
    
    // Delete the uploaded PDF file
    Utils.deleteFile(task.getFilePathInServer());
    
    // Update status to completed
    task.setStatus("completed");
    converterBO.updateStatus(task.getUsername(), 
                            task.getFileNameInServer(), 
                            "completed");
    
    System.out.println("Task " + task.getId() + " completed successfully");
    
} catch (Exception e) {
    System.err.println("Task " + task.getId() + " failed: " + e.getMessage());
    task.setStatus("failed");
    converterBO.updateStatus(task.getUsername(), 
                            task.getFileNameInServer(), 
                            "failed");
}
```

**Phân tích ConverterThread:**

```java
// File: model/BO/ConverterThread.java
public class ConverterThread extends Thread {
    private String filePath;
    
    public ConverterThread(String filePath) {
        this.filePath = filePath;
    }
    
    @Override
    public void run() {
        PdfConvertionHelper.convertPdfToDoc(filePath);
    }
}
```

**Conversion process (PdfConversionHelper):**
1. Load PDF file using Spire.PDF library
2. Parse PDF structure (text, images, formatting)
3. Convert to Word document format
4. Save as DOCX file

**File system trước và sau:**
```
Trước conversion:
/upload/1700000000000_document.pdf    ← Input file (PDF)

Sau conversion:
/upload/1700000000000_document.docx   ← Output file (DOCX)
/upload/1700000000000_document.pdf    ← Bị XÓA để tiết kiệm dung lượng
```

**⏱️ Thời gian:** 5-30 giây (tùy kích thước file)

---

### BƯỚC 9: Cập nhật status thành "completed"

**SQL được thực thi:**
```sql
UPDATE uploads 
SET status = 'completed' 
WHERE username = 'johndoe' 
  AND fileNameOutputInServer = '1700000000000_document.docx';
```

**Database sau update:**
```sql
+----+----------+---------------+---------+---------------------+-----------+
| id | username | fileNameUpload| ...     | date                | status    |
+----+----------+---------------+---------+---------------------+-----------+
| 1  | johndoe  | document.pdf  | ...     | 2024-11-23 10:00:00 | completed |
+----+----------+---------------+---------+---------------------+-----------+
```

**Worker quay lại chờ task tiếp theo:**
```java
while (running) {
    task = queue.take();  // Block và chờ task mới
    processTask(task);
}
```

---

## 5. CÁC THÀNH PHẦN CHÍNH

### 5.1. ConversionTask (BEAN)

**Mục đích:** Đại diện cho một nhiệm vụ chuyển đổi

```java
public class ConversionTask {
    private int id;                    // Unique task ID
    private String username;           // Owner của task
    private String filePathInServer;   // Path đầy đủ tới file PDF
    private String fileNameUserUpload; // Tên file gốc user upload
    private String fileNameInServer;   // Tên file trên server (có timestamp)
    private String status;             // queued/processing/completed/failed
    private long createdAt;            // Timestamp tạo task
    
    // Constructor, getters, setters...
}
```

**Ví dụ instance:**
```java
ConversionTask {
    id: 1
    username: "johndoe"
    filePathInServer: "/var/tomcat/webapps/PDF_CONVERTION/upload/1700000000000_document.pdf"
    fileNameUserUpload: "document.pdf"
    fileNameInServer: "1700000000000_document.pdf"
    status: "processing"
    createdAt: 1700000000000
}
```

### 5.2. ConversionQueue (Singleton)

**Đặc điểm:**
- **Singleton pattern:** Chỉ có 1 instance trong toàn application
- **Thread-safe:** Sử dụng `synchronized` và `BlockingQueue`
- **Lazy initialization:** Tạo worker khi có task đầu tiên

**Responsibilities:**
1. Quản lý hàng đợi tasks
2. Generate unique task IDs
3. Khởi động và quản lý worker thread
4. Provide queue statistics (size, etc.)

**API:**
```java
// Get singleton instance
ConversionQueue queue = ConversionQueue.getInstance();

// Add task to queue (returns task ID)
int taskId = queue.addTask(task);

// Get current queue size
int size = queue.getQueueSize();

// Shutdown worker (for application shutdown)
queue.shutdown();
```

### 5.3. ConversionWorker (Background Thread)

**Đặc điểm:**
- **Daemon thread:** Không prevent JVM shutdown
- **Infinite loop:** Chạy liên tục cho đến khi shutdown
- **Blocking wait:** Sử dụng `queue.take()` để tiết kiệm CPU

**Lifecycle:**
```
Application Start
       │
       ▼
First task added
       │
       └─► ConversionQueue.getInstance()
                   │
                   └─► new ConversionWorker()
                              │
                              └─► worker.start()
                                         │
                                         ▼
                              ┌──────────────────┐
                              │  Thread RUNNING  │
                              │  while(running)  │
                              │  { take, process}│
                              └──────────────────┘
                                         │
                              Application Shutdown
```

### 5.4. ConverterBO (Business Logic)

**Responsibilities:**
1. Validate business rules
2. Coordinate between Servlet và DAO
3. Transform data (filename conversions)
4. Status management

**Methods:**
```java
// Save conversion history with status
void saveHistoryWithStatus(String username, String fileNameUpload, 
                          String fileNameInServer, String status)

// Update conversion status
void updateStatus(String username, String fileNameInServer, String status)

// Get user's conversion list
ArrayList<Upload> getListFileConvert(String username)
```

### 5.5. ConverterDAO (Data Access)

**Responsibilities:**
1. Execute SQL queries
2. Map ResultSet to Upload objects
3. Handle database connections
4. Manage transactions

**Database operations:**
```java
// INSERT new conversion record
INSERT INTO uploads (username, fileNameUpload, fileNameOutput, 
                     fileNameOutputInServer, status) 
VALUES (?, ?, ?, ?, ?)

// UPDATE status
UPDATE uploads 
SET status = ? 
WHERE username = ? AND fileNameOutputInServer = ?

// SELECT user's uploads
SELECT * FROM uploads 
WHERE username = ? 
ORDER BY date DESC
```

---

## 6. SƠ ĐỒ TUẦN TỰ

### 6.1. Sequence Diagram - Upload Flow

```
User       Browser    Servlet    BO      DAO     DB      Queue   Worker
 │           │          │         │       │       │        │       │
 │ Select    │          │         │       │       │        │       │
 │  File     │          │         │       │       │        │       │
 ├──────────>│          │         │       │       │        │       │
 │           │          │         │       │       │        │       │
 │ Click     │          │         │       │       │        │       │
 │ Upload    │          │         │       │       │        │       │
 ├──────────>│          │         │       │       │        │       │
 │           │  POST    │         │       │       │        │       │
 │           ├─────────>│         │       │       │        │       │
 │           │          │ Save    │       │       │        │       │
 │           │          │ File    │       │       │        │       │
 │           │          │ (disk)  │       │       │        │       │
 │           │          ├─────┐   │       │       │        │       │
 │           │          │<────┘   │       │       │        │       │
 │           │          │         │       │       │        │       │
 │           │          │ saveHistory     │       │        │       │
 │           │          ├────────>│       │       │        │       │
 │           │          │         │ INSERT        │        │       │
 │           │          │         ├──────>│       │        │       │
 │           │          │         │       │ INSERT │       │       │
 │           │          │         │       ├───────>│       │       │
 │           │          │         │       │  OK    │       │       │
 │           │          │         │       │<───────┤       │       │
 │           │          │         │  OK   │        │       │       │
 │           │          │         │<──────┤        │       │       │
 │           │          │  OK     │       │        │       │       │
 │           │          │<────────┤       │        │       │       │
 │           │          │         │       │        │       │       │
 │           │          │ addTask(task)   │        │       │       │
 │           │          ├────────────────────────────────>│       │
 │           │          │         │       │        │ put() │       │
 │           │          │         │       │        │<──────┤       │
 │           │          │  taskId │       │        │       │ take()│
 │           │          │<────────────────────────────────┤       │
 │           │  HTML    │         │       │        │       │ (wake)│
 │           │  Response│         │       │        │       ├──────>│
 │           │<─────────┤         │       │        │       │       │
 │  "Đang   │          │         │       │        │       │       │
 │  thực    │          │         │       │        │       │ process
 │  hiện"   │          │         │       │        │       │ (async)
 │<──────────┤          │         │       │        │       │       │
 │           │          │         │       │        │       │       │
```

### 6.2. Sequence Diagram - Background Processing

```
Worker          BO          DAO         DB          FileSystem
  │              │           │           │              │
  │ take()       │           │           │              │
  ├────────┐     │           │           │              │
  │        │ (blocked until task available)            │
  │<───────┘     │           │           │              │
  │              │           │           │              │
  │ updateStatus("processing")          │              │
  ├─────────────>│           │           │              │
  │              │ UPDATE    │           │              │
  │              ├──────────>│           │              │
  │              │           │  UPDATE   │              │
  │              │           ├──────────>│              │
  │              │           │  OK       │              │
  │              │           │<──────────┤              │
  │              │  OK       │           │              │
  │              │<──────────┤           │              │
  │  OK          │           │           │              │
  │<─────────────┤           │           │              │
  │              │           │           │              │
  │ ConverterThread.start() │           │              │
  ├─────────────────────────────────────────────────────>│
  │              │           │           │  Read PDF    │
  │              │           │           │  Convert     │
  │              │           │           │  Write DOCX  │
  │              │           │           │<─────────────┤
  │  join() - wait for completion        │              │
  ├──────────┐  │           │           │              │
  │          │  │           │           │              │
  │<─────────┘  │           │           │              │
  │              │           │           │              │
  │ deleteFile(PDF)          │           │              │
  ├─────────────────────────────────────────────────────>│
  │              │           │           │  Delete PDF  │
  │              │           │           │<─────────────┤
  │              │           │           │              │
  │ updateStatus("completed")│           │              │
  ├─────────────>│           │           │              │
  │              │ UPDATE    │           │              │
  │              ├──────────>│           │              │
  │              │           │  UPDATE   │              │
  │              │           ├──────────>│              │
  │              │           │  OK       │              │
  │              │           │<──────────┤              │
  │              │  OK       │           │              │
  │              │<──────────┤           │              │
  │  OK          │           │           │              │
  │<─────────────┤           │           │              │
  │              │           │           │              │
  │ Back to take() - wait for next task │              │
  ├────────┐     │           │           │              │
  │        │ (blocked)       │           │              │
  │        │     │           │           │              │
```

---

## 7. QUẢN LÝ TRẠNG THÁI

### 7.1. State Machine

```
                    ┌─────────────┐
                    │   UPLOAD    │
                    │  (Initial)  │
                    └──────┬──────┘
                           │
                           │ saveHistoryWithStatus()
                           │
                           ▼
                    ┌─────────────┐
              ┌────►│   QUEUED    │
              │     │  🟠 Orange   │
              │     └──────┬──────┘
              │            │
              │            │ Worker.processTask()
              │            │
              │            ▼
              │     ┌─────────────┐
              │     │ PROCESSING  │
              │     │  🔵 Blue     │
              │     └──────┬──────┘
              │            │
              │            ├──────────────┬──────────────┐
              │            │              │              │
              │            │ Success      │ Error        │
              │            │              │              │
              │            ▼              ▼              ▼
              │     ┌─────────────┐  ┌──────────┐  ┌──────────┐
              │     │ COMPLETED   │  │  FAILED  │  │ TIMEOUT  │
              │     │  🟢 Green    │  │  🔴 Red   │  │  ⚠️ Warn │
              │     └─────────────┘  └─────┬────┘  └────┬─────┘
              │                             │            │
              └─────────────────────────────┴────────────┘
                        (Retry mechanism - future enhancement)
```

### 7.2. Status Definitions

| Status       | Màu sắc | Icon | Ý nghĩa                                    | Action có thể thực hiện     |
|--------------|---------|------|--------------------------------------------|-----------------------------|
| `queued`     | 🟠 Cam  | ⏳   | Task đang trong queue, chờ được xử lý      | Chờ đợi                     |
| `processing` | 🔵 Xanh | ⚙️   | Đang được worker xử lý                     | Chờ đợi                     |
| `completed`  | 🟢 Lá   | ✓    | Chuyển đổi thành công, file đã sẵn sàng   | Download DOCX               |
| `failed`     | 🔴 Đỏ   | ✗    | Chuyển đổi thất bại (lỗi file hoặc system) | Xem lỗi, thử lại với file khác |

### 7.3. Transition Rules

**Chuyển đổi hợp lệ:**
```
queued → processing      ✅ (Worker bắt đầu xử lý)
processing → completed   ✅ (Conversion thành công)
processing → failed      ✅ (Conversion lỗi)
```

**Chuyển đổi KHÔNG hợp lệ:**
```
queued → completed       ❌ (Bỏ qua processing)
completed → processing   ❌ (Không rollback)
failed → completed       ❌ (Không tự sửa)
```

### 7.4. Status trong Database và UI

**Database representation:**
```sql
CREATE TABLE uploads (
    ...
    status VARCHAR(20) DEFAULT 'completed',
    ...
);

-- Values: 'queued', 'processing', 'completed', 'failed'
```

**UI representation (viewListConvert.jsp):**
```jsp
<%
String status = upload.getStatus();
if ("queued".equals(status)) {
%>
    <span class="status-badge" style="color: #FF9800;">
        <span class="processing-spinner"></span>
        Đang thực hiện (queued)
    </span>
<%
} else if ("processing".equals(status)) {
%>
    <span class="status-badge" style="color: #2196F3;">
        <span class="processing-spinner"></span>
        Đang thực hiện (processing)
    </span>
<%
} else if ("completed".equals(status)) {
%>
    <span class="status-badge" style="color: #4CAF50;">
        ✓ Hoàn thành
    </span>
    <!-- Show download link -->
    <a href="./DownloadFileServlet?...">Download</a>
<%
} else if ("failed".equals(status)) {
%>
    <span class="status-badge" style="color: #F44336;">
        ✗ Thất bại
    </span>
<%
}
%>
```

---

## 8. VÍ DỤ THỰC TẾ

### Case 1: Upload thành công

**Input:**
- User: johndoe
- File: monthly_report.pdf (2.5 MB, 15 pages)

**Timeline:**
```
00:00.000  User clicks upload
00:00.050  Browser sends HTTP POST
00:00.100  Servlet receives file
00:00.150  File saved to /upload/1700000000000_monthly_report.pdf
00:00.200  INSERT DB (status=queued)
00:00.250  Task added to queue (Task ID: 5)
00:00.300  Response "Đang thực hiện" sent to user
           ↓ User sees task ID and continues browsing
00:00.350  Worker takes task from queue
00:00.400  UPDATE DB (status=processing)
00:00.450  Start PDF conversion
00:12.000  Conversion in progress... (page 1-5)
00:25.000  Conversion in progress... (page 6-10)
00:38.000  Conversion in progress... (page 11-15)
00:40.000  Conversion completed
00:40.100  DOCX saved to /upload/1700000000000_monthly_report.docx
00:40.150  PDF file deleted
00:40.200  UPDATE DB (status=completed)
00:40.250  Worker waits for next task
```

**Database state changes:**
```sql
-- At 00:00.200
status = 'queued'

-- At 00:00.400
status = 'processing'

-- At 00:40.200
status = 'completed'
```

**User view changes:**
```
00:00 - 00:05   Auto-refresh #1: Status = queued 🟠
00:05 - 00:10   Auto-refresh #2: Status = processing 🔵
00:10 - 00:15   Auto-refresh #3: Status = processing 🔵
00:15 - 00:20   Auto-refresh #4: Status = processing 🔵
00:20 - 00:25   Auto-refresh #5: Status = processing 🔵
00:25 - 00:30   Auto-refresh #6: Status = processing 🔵
00:30 - 00:35   Auto-refresh #7: Status = processing 🔵
00:35 - 00:40   Auto-refresh #8: Status = processing 🔵
00:40 - 00:45   Auto-refresh #9: Status = completed 🟢 [Download]
```

---

### Case 2: Multiple concurrent uploads

**Scenario:**
- User A uploads file1.pdf
- User B uploads file2.pdf (1 second later)
- User C uploads file3.pdf (2 seconds later)

**Queue behavior:**
```
Time   | Queue State              | Worker Activity
────────┼──────────────────────────┼────────────────────────────
00:00  │ []                       │ Idle (sleeping)
00:01  │ [Task#1(A, file1.pdf)]   │ Wake up, take Task#1
00:02  │ []                       │ Processing Task#1...
00:03  │ [Task#2(B, file2.pdf)]   │ Still processing Task#1...
00:04  │ [Task#2]                 │ Still processing Task#1...
00:15  │ [Task#2]                 │ Task#1 complete!
00:16  │ [Task#2, Task#3(C)]      │ Take Task#2
00:17  │ [Task#3]                 │ Processing Task#2...
00:30  │ [Task#3]                 │ Task#2 complete!
00:31  │ []                       │ Take Task#3
00:32  │ []                       │ Processing Task#3...
00:45  │ []                       │ Task#3 complete! Back to idle
```

**Điểm quan trọng:**
- Tasks được xử lý **tuần tự** (FIFO)
- User A, B, C đều nhận response "Đang thực hiện" ngay lập tức
- File2 và file3 phải chờ file1 xong mới được xử lý

---

### Case 3: Xử lý lỗi

**Scenario:** File PDF bị corrupt

**Timeline:**
```
00:00  User uploads corrupted.pdf
00:01  Servlet: Save file, INSERT DB (queued), add to queue
00:02  Worker: Take task, UPDATE (processing)
00:03  Worker: Start conversion
00:04  PdfConversionHelper: Exception! Cannot parse PDF
00:05  Worker: Catch exception
00:06  Worker: UPDATE DB (status=failed)
00:07  Worker: Wait for next task
```

**Database final state:**
```sql
status = 'failed'
error_message = 'Cannot parse PDF: Invalid PDF structure'
```

**User view:**
```
Status: ✗ Thất bại (Red badge)
```

**Error handling code:**
```java
try {
    ConverterThread thread = new ConverterThread(filePath);
    thread.start();
    thread.join();
    // Success path...
} catch (Exception e) {
    System.err.println("Conversion failed: " + e.getMessage());
    task.setStatus("failed");
    converterBO.updateStatus(username, fileName, "failed");
    // Error được log nhưng KHÔNG crash worker
}
```

---

## 9. XỬ LÝ LỖI VÀ EXCEPTIONS

### 9.1. Exception Handling Strategy

**Nguyên tắc:**
1. **Catch exceptions tại mỗi layer**
2. **Log errors với đầy đủ context**
3. **Không để exceptions crash worker thread**
4. **Update status khi gặp lỗi**

### 9.2. Error Scenarios

#### Scenario 1: Database connection failed

**Nơi xảy ra:** ConverterDAO

```java
public void saveHistoryWithStatus(...) {
    try (Connection connection = Utils.getConnection()) {
        if (connection == null) {
            System.err.println("Failed to establish database connection");
            return;  // Fail silently
        }
        // Execute INSERT...
    } catch (Exception e) {
        System.err.println("saveHistoryWithStatus: " + e.getMessage());
        e.printStackTrace();
    }
}
```

**Hậu quả:**
- Record KHÔNG được lưu vào database
- Task vẫn được add vào queue
- Worker sẽ xử lý task nhưng không tìm thấy record để update
- **Risk:** Orphan task (không có database record)

**Giải pháp:** 
- Check database connection trước khi add task
- Rollback task nếu save DB fail

#### Scenario 2: File write permission denied

**Nơi xảy ra:** ConverterServlet

```java
try {
    part.write(filePathInServer);
} catch (IOException e) {
    System.err.println("Cannot write file: " + e.getMessage());
    response.sendError(500, "File upload failed");
    return;
}
```

**Hậu quả:**
- File không được lưu
- User nhận error 500
- Không add task vào queue

#### Scenario 3: Conversion library error

**Nơi xảy ra:** PdfConversionHelper

```java
public static void convertPdfToDoc(String filePath) {
    try {
        PdfDocument pdf = new PdfDocument();
        pdf.loadFromFile(filePath);
        pdf.saveToFile(outputPath, FileFormat.DOCX);
    } catch (Exception e) {
        System.err.println("Conversion error: " + e.getMessage());
        throw new RuntimeException("PDF conversion failed", e);
    }
}
```

**Handling trong Worker:**
```java
try {
    thread.start();
    thread.join();
    status = "completed";
} catch (Exception e) {
    System.err.println("Task failed: " + e.getMessage());
    status = "failed";
}
converterBO.updateStatus(username, fileName, status);
```

#### Scenario 4: Worker thread interrupted

**Nơi xảy ra:** ConversionWorker.run()

```java
while (running) {
    try {
        ConversionTask task = queue.take();
        processTask(task);
    } catch (InterruptedException e) {
        System.err.println("Worker interrupted");
        Thread.currentThread().interrupt();  // Restore interrupt flag
        break;  // Exit loop gracefully
    }
}
```

**Khi nào xảy ra:**
- Application shutdown
- Worker được shutdown() explicitly

### 9.3. Logging Strategy

**Console logging:**
```java
// Success
System.out.println("Task " + taskId + " added to queue. Queue size: " + size);
System.out.println("Processing task " + taskId + " for user " + username);
System.out.println("Task " + taskId + " completed successfully");

// Errors
System.err.println("Failed to establish database connection");
System.err.println("Task " + taskId + " failed: " + e.getMessage());
e.printStackTrace();  // Full stack trace
```

**Database logging (future enhancement):**
```sql
CREATE TABLE error_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT,
    error_type VARCHAR(50),
    error_message TEXT,
    stack_trace TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 10. TỐI ƯU HÓA VÀ PERFORMANCE

### 10.1. Current Performance Characteristics

**Throughput:**
- Sequential processing: 1 file at a time
- Average conversion time: 5-30 seconds per file
- Theoretical maximum: ~120-720 files/hour

**Latency:**
- User response time: 50-200ms (chỉ tính upload, không tính conversion)
- Conversion time: Tùy file size và complexity

**Resource usage:**
- Threads: 1 worker thread (minimal overhead)
- Memory: Depends on Spire library (typically 100-500MB per conversion)
- Disk I/O: Read PDF + Write DOCX + Delete PDF

### 10.2. Bottlenecks

**1. Single worker thread**
- Chỉ xử lý 1 file tại một thời điểm
- Large queue → long wait times

**2. Synchronous conversion trong worker**
- `thread.join()` blocks worker cho đến khi conversion xong
- Không thể xử lý file khác cùng lúc

**3. Database UPDATE for each status change**
- 2 UPDATEs per task (processing, completed)
- Có thể optimize bằng batch updates

### 10.3. Optimization Strategies

#### Strategy 1: Multiple worker threads

**Current:**
```java
private ConversionWorker worker;  // Single worker
```

**Optimized:**
```java
private final ExecutorService executorService;
private static final int NUM_WORKERS = 4;  // 4 parallel workers

private ConversionQueue() {
    this.queue = new LinkedBlockingQueue<>();
    this.executorService = Executors.newFixedThreadPool(NUM_WORKERS);
    
    // Start 4 workers
    for (int i = 0; i < NUM_WORKERS; i++) {
        executorService.submit(new ConversionWorker(queue));
    }
}
```

**Benefits:**
- 4x throughput increase
- Better CPU utilization
- Reduced wait times

**Trade-offs:**
- Higher memory usage
- More complex concurrency management

#### Strategy 2: Async conversion với callbacks

**Current:**
```java
thread.start();
thread.join();  // BLOCKING
```

**Optimized:**
```java
CompletableFuture.supplyAsync(() -> {
    return PdfConversionHelper.convertPdfToDoc(filePath);
}).thenAccept(result -> {
    converterBO.updateStatus(username, fileName, "completed");
}).exceptionally(e -> {
    converterBO.updateStatus(username, fileName, "failed");
    return null;
});
```

**Benefits:**
- Non-blocking worker
- Can process next task immediately
- Better resource utilization

#### Strategy 3: Database connection pooling

**Current:**
```java
Connection connection = Utils.getConnection();  // New connection mỗi lần
```

**Optimized:**
```java
// Using HikariCP
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mysql://localhost:3306/pdf_convertion");
config.setMaximumPoolSize(10);
HikariDataSource dataSource = new HikariDataSource(config);

// Reuse connections
Connection connection = dataSource.getConnection();
```

**Benefits:**
- Faster database operations
- Reduced connection overhead
- Better scalability

#### Strategy 4: Batch status updates

**Current:**
```java
// 2 separate UPDATE statements
updateStatus(username, fileName, "processing");
// ... conversion ...
updateStatus(username, fileName, "completed");
```

**Optimized:**
```java
// Single UPDATE at the end
Map<String, Object> updates = new HashMap<>();
updates.put("status", "completed");
updates.put("processing_time", duration);
updates.put("completed_at", timestamp);
batchUpdateStatus(updates);
```

### 10.4. Monitoring and Metrics

**Metrics to track:**
```java
public class ConversionMetrics {
    private final AtomicLong totalTasksProcessed = new AtomicLong(0);
    private final AtomicLong totalTasksFailed = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    public void recordTaskComplete(long processingTimeMs) {
        totalTasksProcessed.incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeMs);
    }
    
    public double getAverageProcessingTime() {
        long total = totalTasksProcessed.get();
        return total == 0 ? 0 : totalProcessingTime.get() / (double) total;
    }
    
    public double getSuccessRate() {
        long total = totalTasksProcessed.get();
        long failed = totalTasksFailed.get();
        return total == 0 ? 0 : (total - failed) / (double) total * 100;
    }
}
```

**Dashboard display:**
```
════════════════════════════════════════
       CONVERSION SYSTEM METRICS
════════════════════════════════════════
Total Tasks Processed:     1,247
Total Tasks Failed:           23
Success Rate:             98.16%
Average Processing Time:  12.5s
Current Queue Size:           3
Active Workers:               1/1
════════════════════════════════════════
```

---

## TÓM TẮT

### Luồng hoạt động tổng quát:

```
1. User upload PDF
   ↓
2. Servlet lưu file + INSERT DB (queued) + Add task to queue
   ↓
3. Response "Đang thực hiện" trả về ngay lập tức
   ↓
4. Worker (background) take task từ queue
   ↓
5. UPDATE status = processing
   ↓
6. Convert PDF → DOCX
   ↓
7. UPDATE status = completed/failed
   ↓
8. User view list và download
```

### Ưu điểm của kiến trúc này:

✅ **Non-blocking:** User không phải chờ conversion  
✅ **Scalable:** Có thể mở rộng với multiple workers  
✅ **Reliable:** Exceptions không crash system  
✅ **Traceable:** Status tracking cho phép monitor progress  
✅ **User-friendly:** Clear feedback với "Đang thực hiện"  

### Hạn chế và cải tiến:

⚠️ **Single worker:** Sequential processing  
→ Giải pháp: Multiple workers với thread pool

⚠️ **In-memory queue:** Mất data khi restart  
→ Giải pháp: Persistent queue (Redis, RabbitMQ)

⚠️ **No retry mechanism:** Failed tasks không tự động retry  
→ Giải pháp: Exponential backoff retry

⚠️ **Limited monitoring:** Thiếu metrics và dashboard  
→ Giải pháp: Add Prometheus metrics, Grafana dashboard

---

**Tài liệu phiên bản:** 1.0  
**Ngày tạo:** 2024-11-23  
**Tác giả:** PDF Conversion Development Team

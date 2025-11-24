# PDF Conversion System Architecture

## Table of Contents
1. [Overview](#overview)
2. [MVC Architecture](#mvc-architecture)
3. [System Components](#system-components)
4. [Queue Processing System](#queue-processing-system)
5. [Data Flow](#data-flow)
6. [Database Design](#database-design)
7. [Security Architecture](#security-architecture)
8. [Deployment Architecture](#deployment-architecture)

---

## Overview

The PDF Conversion System is a web-based application built using Java EE (Jakarta EE) that converts PDF files to DOCX format. The system implements an asynchronous queue-based processing architecture to handle conversion tasks efficiently without blocking user requests.

### Key Architectural Principles
- **MVC Pattern**: Clear separation of concerns between Model, View, and Controller
- **Asynchronous Processing**: Non-blocking conversion using queue and worker thread
- **Thread Safety**: Concurrent data structures for safe multi-threaded access
- **Database Persistence**: All conversion tasks and user data stored in MySQL
- **Session Management**: User authentication and authorization
- **Scalability**: Designed to handle multiple concurrent users

---

## MVC Architecture

The application follows the classic **Model-View-Controller (MVC)** architectural pattern.

```
┌─────────────────────────────────────────────────────────────────────┐
│                           MVC ARCHITECTURE                          │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│                 │         │                 │         │                 │
│     VIEW        │◄────────│   CONTROLLER    │────────►│     MODEL       │
│   (JSP Pages)   │         │   (Servlets)    │         │  (BEAN/BO/DAO)  │
│                 │         │                 │         │                 │
└────────┬────────┘         └────────┬────────┘         └────────┬────────┘
         │                           │                           │
         │ Displays data            │ User actions              │ Data access
         │                           │                           │
         ▼                           ▼                           ▼
   ┌──────────┐              ┌──────────────┐           ┌──────────────┐
   │ Browser  │              │  HTTP        │           │  Database    │
   │ (Client) │              │  Request     │           │  (MySQL)     │
   └──────────┘              └──────────────┘           └──────────────┘
```

### MVC Components Breakdown

#### 1. MODEL Layer
The Model layer represents the business logic and data management.

**Package Structure:**
```
model/
├── BEAN/              # Data objects (Plain Old Java Objects)
│   ├── User.java
│   ├── Upload.java
│   └── ConversionTask.java
│
├── BO/                # Business Objects (Business Logic)
│   ├── LoginBO.java
│   ├── ConverterBO.java
│   ├── ConversionQueue.java
│   ├── ConversionWorker.java
│   └── PdfConvertionHelper.java
│
└── DAO/               # Data Access Objects (Database Operations)
    ├── LoginDAO.java
    └── ConverterDAO.java
```

**Responsibilities:**
- **BEAN**: Define data structures (User, Upload, ConversionTask)
- **BO**: Implement business rules and logic
- **DAO**: Handle database CRUD operations

#### 2. VIEW Layer
The View layer handles the presentation logic.

**Package Structure:**
```
webapp/
├── index.jsp                  # Home page - Upload form
├── viewListConvert.jsp        # Conversion history list
├── login-modal.jsp            # Login modal dialog
├── signup-modal.jsp           # Signup modal dialog
├── header.jsp                 # Common header
│
├── css/                       # Stylesheets
│   ├── common.css
│   ├── login/
│   └── convertion/
│
└── js/                        # Client-side JavaScript
    ├── index.js
    └── signup.js
```

**Responsibilities:**
- Render HTML pages
- Display data from Model
- Capture user input
- Client-side validation

#### 3. CONTROLLER Layer
The Controller layer handles HTTP requests and coordinates between Model and View.

**Package Structure:**
```
controller/
├── LoginServlet.java          # Handles login/signup
├── ConverterServlet.java      # Handles PDF upload
├── ListConvertServlet.java    # Displays conversion list
└── DownloadFileServlet.java   # Handles file download
```

**Responsibilities:**
- Process HTTP requests
- Validate input
- Call appropriate Business Objects
- Select and forward to appropriate View
- Manage user sessions

### MVC Request Flow

```
1. USER ACTION (View)
   User clicks "Upload" button on index.jsp
   ↓

2. HTTP REQUEST (Browser)
   POST /converter
   Form data: PDF file + metadata
   ↓

3. CONTROLLER (Servlet)
   ConverterServlet receives request
   ├── Validates session (user logged in?)
   ├── Extracts file and parameters
   └── Calls Business Object
   ↓

4. BUSINESS LOGIC (BO)
   ConverterBO processes the request
   ├── Saves metadata to database (via DAO)
   ├── Adds task to ConversionQueue
   └── Returns task ID
   ↓

5. DATA ACCESS (DAO)
   ConverterDAO interacts with database
   ├── INSERT INTO uploads (...)
   └── Returns success/failure
   ↓

6. BACKGROUND PROCESSING (Worker)
   ConversionWorker picks up task
   ├── Updates status to "processing"
   ├── Converts PDF to DOCX
   └── Updates status to "completed"
   ↓

7. RESPONSE (Controller → View)
   Servlet forwards to success page
   ├── Sets message: "Đang thực hiện"
   └── Includes link to view status
   ↓

8. DISPLAY (View)
   JSP renders response to user
   Shows: "Your file is being processed"
```

---

## System Components

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  index.jsp   │  │viewList.jsp  │  │login-modal   │          │
│  │ (Upload UI)  │  │(Status List) │  │  (Auth UI)   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP Request/Response
┌───────────────────────────┴─────────────────────────────────────┐
│                         CONTROLLER LAYER                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │LoginServlet  │  │Converter     │  │ListConvert   │          │
│  │              │  │Servlet       │  │Servlet       │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└───────────────────────────┬─────────────────────────────────────┘
                            │ Method Calls
┌───────────────────────────┴─────────────────────────────────────┐
│                      BUSINESS LOGIC LAYER                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  LoginBO     │  │ ConverterBO  │  │Conversion    │          │
│  │              │  │              │  │Queue         │          │
│  └──────────────┘  └──────────────┘  └──────┬───────┘          │
│                                             │                    │
│  ┌──────────────────────────────────────────┘                   │
│  │                                                               │
│  │  ┌──────────────┐          ┌──────────────┐                 │
│  └─►│Conversion    │─────────►│PdfConversion │                 │
│     │Worker        │          │Helper        │                 │
│     │(Background)  │          │(Spire APIs)  │                 │
│     └──────────────┘          └──────────────┘                 │
└───────────────────────────┬─────────────────────────────────────┘
                            │ SQL Queries
┌───────────────────────────┴─────────────────────────────────────┐
│                      DATA ACCESS LAYER                           │
│  ┌──────────────┐  ┌──────────────┐                             │
│  │  LoginDAO    │  │ ConverterDAO │                             │
│  │              │  │              │                             │
│  └──────────────┘  └──────────────┘                             │
└───────────────────────────┬─────────────────────────────────────┘
                            │ JDBC
┌───────────────────────────┴─────────────────────────────────────┐
│                         DATABASE LAYER                           │
│  ┌──────────────┐  ┌──────────────┐                             │
│  │ users table  │  │uploads table │                             │
│  │              │  │              │                             │
│  └──────────────┘  └──────────────┘                             │
│                  MySQL Database                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Component Descriptions

#### 1. User (BEAN)
**File**: `model/BEAN/User.java`

```java
// Represents a user account
class User {
    - username: String
    - password: String
    - email: String
    + getters/setters
}
```

**Purpose**: Data container for user information

#### 2. Upload (BEAN)
**File**: `model/BEAN/Upload.java`

```java
// Represents a conversion task record
class Upload {
    - id: int
    - username: String
    - fileNameUpload: String
    - fileNameOutput: String
    - fileNameOutputInServer: String
    - date: Timestamp
    - status: String          // queued/processing/completed/failed
    + getters/setters
}
```

**Purpose**: Data container for conversion history

#### 3. ConversionTask (BEAN)
**File**: `model/BEAN/ConversionTask.java`

```java
// Represents an active conversion task in queue
class ConversionTask {
    - taskId: int
    - username: String
    - inputFilePath: String
    - outputFilePath: String
    - originalFileName: String
    - outputFileName: String
    - uploadId: int
    + getters/setters
}
```

**Purpose**: Task object passed through queue

#### 4. LoginBO (Business Object)
**File**: `model/BO/LoginBO.java`

```java
class LoginBO {
    - dao: LoginDAO
    
    + checkLogin(username, password): boolean
    + addUser(user): boolean
}
```

**Responsibilities**:
- Validate login credentials
- Handle user registration
- Business rules for authentication

#### 5. ConverterBO (Business Object)
**File**: `model/BO/ConverterBO.java`

```java
class ConverterBO {
    - dao: ConverterDAO
    
    + saveHistory(upload): int
    + saveHistoryWithStatus(upload, status): int
    + getListFileConvert(username): List<Upload>
    + updateStatus(uploadId, status): void
}
```

**Responsibilities**:
- Manage conversion workflow
- Coordinate between servlet and DAO
- Status management

#### 6. ConversionQueue (Singleton)
**File**: `model/BO/ConversionQueue.java`

```java
class ConversionQueue {
    - static instance: ConversionQueue
    - queue: BlockingQueue<ConversionTask>
    - executorService: ExecutorService
    - taskIdCounter: AtomicInteger
    - NUM_WORKERS: int = 3
    
    + static getInstance(): ConversionQueue
    + addTask(task): int
    + getQueueSize(): int
    + shutdown(): void
}
```

**Pattern**: Singleton  
**Thread Safety**: Yes (using AtomicInteger and BlockingQueue)
**Concurrency**: Fixed thread pool with 3 worker threads

**Responsibilities**:
- Maintain single queue instance
- Generate unique task IDs
- Manage worker thread pool lifecycle
- Add tasks to queue
- Process up to 3 files concurrently

#### 7. ConversionWorker (Runnable)
**File**: `model/BO/ConversionWorker.java`

```java
class ConversionWorker implements Runnable {
    - queue: BlockingQueue<ConversionTask>
    - converterBO: ConverterBO
    - workerId: int
    
    + run(): void
    - processTask(task, workerName): void
}
```

**Characteristics**:
- Runnable executed by ExecutorService
- 3 concurrent workers processing tasks in parallel
- Continuously polls queue for tasks
- Handles exceptions gracefully
- Each worker has unique ID for logging

**Responsibilities**:
- Take tasks from queue
- Update status to "processing"
- Call PDF conversion helper
- Update status to "completed" or "failed"
- Log errors with worker identification

#### 8. PdfConvertionHelper
**File**: `model/BO/PdfConvertionHelper.java`

**Dependencies**:
- Spire.PDF (com.spire.pdf)
- Spire.Doc (com.spire.doc)

**Responsibilities**:
- Load PDF file using Spire.PDF
- Convert PDF to DOC format
- Save as DOCX file
- Handle conversion errors

#### 9. LoginDAO
**File**: `model/DAO/LoginDAO.java`

```java
class LoginDAO {
    + checkLogin(username, password): boolean
    + addUser(user): boolean
    + userExists(username): boolean
}
```

**SQL Operations**:
- `SELECT * FROM users WHERE username=? AND password=?`
- `INSERT INTO users (username, password, email) VALUES (?, ?, ?)`
- `SELECT COUNT(*) FROM users WHERE username=?`

#### 10. ConverterDAO
**File**: `model/DAO/ConverterDAO.java`

```java
class ConverterDAO {
    + saveHistory(upload): int
    + saveHistoryWithStatus(upload, status): int
    + getListFileConvert(username): List<Upload>
    + updateStatus(uploadId, status, errorMsg): void
}
```

**SQL Operations**:
- `INSERT INTO uploads (...) VALUES (...)`
- `SELECT * FROM uploads WHERE username=? ORDER BY date DESC`
- `UPDATE uploads SET status=? WHERE id=?`

---

## Queue Processing System

### Queue Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    QUEUE PROCESSING FLOW                      │
└──────────────────────────────────────────────────────────────┘

User Upload (ConverterServlet)
         │
         ├─1. Save to DB (status='queued')
         │
         ├─2. Create ConversionTask
         │
         └─3. Add to Queue
                │
                ▼
         ┌────────────────────┐
         │ ConversionQueue    │
         │  (Singleton)       │
         │                    │
         │ BlockingQueue      │
         │ [Task1][Task2]...  │
         └──────────┬─────────┘
                    │ take()
                    ▼
         ┌────────────────────┐
         │ ConversionWorker   │
         │  (Daemon Thread)   │
         │                    │
         │  while(true) {     │
         │    task = take()   │
         │    process(task)   │
         │  }                 │
         └──────────┬─────────┘
                    │
                    ├─1. Update DB (status='processing')
                    │
                    ├─2. Convert PDF → DOCX
                    │
                    └─3. Update DB (status='completed')
                          │
                          ▼
                    User sees result
```

### State Machine

```
                    ┌─────────────┐
                    │   UPLOAD    │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
              ┌────►│   QUEUED    │
              │     └──────┬──────┘
              │            │
              │            ▼
              │     ┌─────────────┐
              │     │ PROCESSING  │
              │     └──────┬──────┘
              │            │
              │            ├──────────────┐
              │            │              │
              │            ▼              ▼
              │     ┌─────────────┐  ┌──────────┐
              │     │ COMPLETED   │  │  FAILED  │
              │     └─────────────┘  └─────┬────┘
              │                             │
              └─────────────────────────────┘
                    (Retry mechanism)
```

### Concurrency Model

**Thread Safety Mechanisms:**

1. **BlockingQueue**: Thread-safe queue implementation
   - Type: `LinkedBlockingQueue<ConversionTask>`
   - Capacity: Unbounded
   - Operations: `take()`, `put()` are blocking and thread-safe

2. **AtomicInteger**: Lock-free task ID generation
   - Type: `AtomicInteger taskIdCounter`
   - Operation: `getAndIncrement()` is atomic

3. **Synchronized getInstance()**: Singleton double-checked locking
   ```java
   private static volatile ConversionQueue instance;
   
   public static synchronized ConversionQueue getInstance() {
       if (instance == null) {
           instance = new ConversionQueue();
       }
       return instance;
   }
   ```

4. **Database Transactions**: SQL updates are atomic
   - Each status update is a single UPDATE statement
   - Auto-commit mode ensures consistency

### Worker Thread Lifecycle

```
Application Start
       │
       ▼
ConversionQueue.getInstance() (First Access)
       │
       ├─► Create BlockingQueue
       │
       ├─► Create ExecutorService (FixedThreadPool with 3 threads)
       │
       └─► Submit 3 ConversionWorker runnables
              │
              ├─► Worker-1 starts
              │      │
              │      └─► while(!interrupted) {
              │             task = queue.take();  // Blocks if empty
              │             processTask(task);
              │          }
              │
              ├─► Worker-2 starts (parallel)
              │      │
              │      └─► while(!interrupted) {
              │             task = queue.take();
              │             processTask(task);
              │          }
              │
              └─► Worker-3 starts (parallel)
                     │
                     └─► while(!interrupted) {
                            task = queue.take();
                            processTask(task);
                         }
       
(All 3 workers continue until ExecutorService shutdown)
```

**Key Points:**
- **3 worker threads**: Process up to 3 files concurrently
- **Thread pool**: Managed by ExecutorService for efficient resource usage
- **Eager initialization**: All workers start when queue is first accessed
- **Never stops**: Workers run for lifetime of application
- **Blocking**: Uses `queue.take()` which blocks when empty (efficient)
- **Concurrent processing**: Multiple users can have files processed simultaneously

---

## Data Flow

### Upload and Conversion Flow

```
┌─────────┐
│  USER   │
└────┬────┘
     │ 1. Select PDF file
     │
     ▼
┌──────────────────┐
│   index.jsp      │
│  (Upload Form)   │
└────┬─────────────┘
     │ 2. HTTP POST /converter
     │    (multipart/form-data)
     ▼
┌──────────────────────────┐
│   ConverterServlet       │
│  ┌────────────────────┐  │
│  │ 1. Check session   │  │
│  │ 2. Save file       │  │
│  │ 3. Create Upload   │  │
│  │ 4. Call BO         │  │
│  └────────────────────┘  │
└────┬─────────────────────┘
     │ 3. saveHistoryWithStatus()
     ▼
┌──────────────────────────┐
│     ConverterBO          │
│  ┌────────────────────┐  │
│  │ 1. Validate data   │  │
│  │ 2. Call DAO        │  │
│  │ 3. Get upload ID   │  │
│  │ 4. Create task     │  │
│  │ 5. Add to queue    │  │
│  └────────────────────┘  │
└────┬─────────────────────┘
     │ 4. Insert into DB
     ▼
┌──────────────────────────┐
│    ConverterDAO          │
│  ┌────────────────────┐  │
│  │ INSERT INTO        │  │
│  │ uploads            │  │
│  │ (status='queued')  │  │
│  └────────────────────┘  │
└────┬─────────────────────┘
     │ 5. Return upload ID
     │
     ├──────────────────────┐
     │                      │
     ▼                      ▼
┌──────────────┐    ┌──────────────────┐
│ Response to  │    │ ConversionQueue  │
│ User:        │    │  .addTask()      │
│ "Đang thực   │    └────┬─────────────┘
│  hiện"       │         │
└──────────────┘         │ 6. Queue task
                         ▼
                  ┌─────────────────┐
                  │ BlockingQueue   │
                  │ [ConversionTask]│
                  └────┬────────────┘
                       │ 7. Worker takes task
                       ▼
                  ┌─────────────────┐
                  │ConversionWorker │
                  │  .processTask() │
                  └────┬────────────┘
                       │
                       ├─► Update status='processing'
                       │
                       ├─► Convert PDF to DOCX
                       │
                       └─► Update status='completed'
                              │
                              ▼
                         [File ready]
```

### View Status Flow

```
┌─────────┐
│  USER   │
└────┬────┘
     │ 1. Click "View list"
     │
     ▼
┌──────────────────┐
│viewListConvert   │
│     .jsp         │
└────┬─────────────┘
     │ 2. HTTP GET /listconvert
     │
     ▼
┌──────────────────────────┐
│  ListConvertServlet      │
│  ┌────────────────────┐  │
│  │ 1. Get username    │  │
│  │ 2. Call BO         │  │
│  │ 3. Set attributes  │  │
│  │ 4. Forward to JSP  │  │
│  └────────────────────┘  │
└────┬─────────────────────┘
     │ 3. getListFileConvert(username)
     ▼
┌──────────────────────────┐
│     ConverterBO          │
│  ┌────────────────────┐  │
│  │ Call DAO           │  │
│  └────────────────────┘  │
└────┬─────────────────────┘
     │ 4. Query database
     ▼
┌──────────────────────────┐
│    ConverterDAO          │
│  ┌────────────────────┐  │
│  │ SELECT * FROM      │  │
│  │ uploads            │  │
│  │ WHERE username=?   │  │
│  │ ORDER BY date DESC │  │
│  └────────────────────┘  │
└────┬─────────────────────┘
     │ 5. Return List<Upload>
     │
     ▼
┌──────────────────────────┐
│  viewListConvert.jsp     │
│  ┌────────────────────┐  │
│  │ Render table       │  │
│  │ - Show status      │  │
│  │ - Color code       │  │
│  │ - Enable download  │  │
│  └────────────────────┘  │
└────┬─────────────────────┘
     │ 6. Auto-refresh after 5s
     │    (if not all completed)
     └───────┐
             │
             ▼
         [Repeat from step 2]
```

---

## Database Design

### ER Diagram

```
┌──────────────────────┐           ┌──────────────────────┐
│       users          │           │      uploads         │
├──────────────────────┤           ├──────────────────────┤
│ PK username (VARCHAR)│◄─────────│FK username (VARCHAR) │
│    password (VARCHAR)│    1   * │PK id (INT, AUTO_INC) │
│    email (VARCHAR)   │           │   fileNameUpload     │
│    created_at (TS)   │           │   fileNameOutput     │
└──────────────────────┘           │   fileNameOutputIn...│
                                   │   date (TIMESTAMP)   │
                                   │   status (VARCHAR)   │
                                   │   error_message(TEXT)│
                                   └──────────────────────┘
```

### Table: users

```sql
CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Fields:**
- `username`: Unique identifier, used for login
- `password`: User password (plaintext - should be hashed in production)
- `email`: User email address
- `created_at`: Account creation timestamp

**Indexes:**
- PRIMARY KEY on `username` (clustered index)
- INDEX on `email` for email lookups

### Table: uploads

```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Fields:**
- `id`: Auto-increment primary key
- `username`: Foreign key to users table
- `fileNameUpload`: Original PDF filename (user-visible)
- `fileNameOutput`: Output DOCX filename (user-visible)
- `fileNameOutputInServer`: Actual filename on server (with timestamp)
- `date`: Upload timestamp
- `status`: Conversion status (queued/processing/completed/failed)
- `error_message`: Error details if failed

**Indexes:**
- PRIMARY KEY on `id`
- FOREIGN KEY on `username` → `users.username`
- INDEX on `username` for user-specific queries
- INDEX on `status` for status filtering
- COMPOSITE INDEX on `(username, status)` for combined filtering
- INDEX on `date DESC` for chronological ordering

**Status Values:**
- `'queued'`: Waiting in queue
- `'processing'`: Being converted
- `'completed'`: Conversion successful
- `'failed'`: Conversion failed

### Database Queries

**Common Queries:**

1. **Get user's conversion history**:
```sql
SELECT * FROM uploads 
WHERE username = ? 
ORDER BY date DESC;
```

2. **Get active tasks (queued or processing)**:
```sql
SELECT * FROM uploads 
WHERE status IN ('queued', 'processing') 
ORDER BY date ASC;
```

3. **Update task status**:
```sql
UPDATE uploads 
SET status = ?, error_message = ? 
WHERE id = ?;
```

4. **Insert new conversion task**:
```sql
INSERT INTO uploads 
(username, fileNameUpload, fileNameOutput, 
 fileNameOutputInServer, status) 
VALUES (?, ?, ?, ?, ?);
```

---

## Security Architecture

### Authentication Flow

```
User Login Attempt
       │
       ├─► LoginServlet
       │      │
       │      ├─► LoginBO.checkLogin()
       │      │      │
       │      │      └─► LoginDAO.checkLogin()
       │      │             │
       │      │             ├─► Query: SELECT * FROM users
       │      │             │           WHERE username=? AND password=?
       │      │             │
       │      │             └─► Return boolean
       │      │
       │      ├─► If success:
       │      │      └─► session.setAttribute("username", username)
       │      │
       │      └─► If failure:
       │             └─► Return error message
       │
       └─► Redirect to home (if success) or login (if failure)
```

### Session Management

**Session Attributes:**
- `username`: Logged-in user's username
- Timeout: 30 minutes of inactivity (default Tomcat setting)

**Session Validation:**
```java
// In every servlet
String username = (String) session.getAttribute("username");
if (username == null) {
    response.sendRedirect("login.jsp");
    return;
}
```

### Access Control

**User Isolation:**
- Each user can only see their own uploads
- Query always filters by `username`:
  ```sql
  WHERE username = ?  -- From session
  ```

**File Access:**
- Download servlet validates ownership:
  ```java
  // Check if file belongs to logged-in user
  Upload upload = dao.getUploadById(id);
  if (!upload.getUsername().equals(sessionUsername)) {
      response.sendError(403, "Forbidden");
  }
  ```

### Security Considerations

**Current Implementation:**
- ⚠️ **Passwords stored in plaintext**: Should use bcrypt/PBKDF2
- ⚠️ **No CSRF protection**: Should use tokens for forms
- ⚠️ **No file type validation**: Should check MIME type
- ⚠️ **No file size limit**: Should enforce max size
- ✅ **SQL Injection protection**: Using PreparedStatement
- ✅ **Session-based auth**: Standard servlet sessions
- ✅ **User isolation**: Data filtered by username

**Recommended Improvements:**
1. Hash passwords using bcrypt
2. Implement CSRF tokens
3. Validate uploaded file types
4. Add file size limits
5. Implement rate limiting
6. Add HTTPS support
7. Implement password complexity rules
8. Add email verification

---

## Deployment Architecture

### Application Server

```
┌──────────────────────────────────────────────────────┐
│              Apache Tomcat 10                         │
│                                                       │
│  ┌────────────────────────────────────────────────┐  │
│  │         PDF_CONVERTION Web Application         │  │
│  │                                                 │  │
│  │  ┌──────────────┐      ┌──────────────┐       │  │
│  │  │  Servlets    │      │  JSP Pages   │       │  │
│  │  │  (*.class)   │      │  (*.jsp)     │       │  │
│  │  └──────────────┘      └──────────────┘       │  │
│  │                                                 │  │
│  │  ┌──────────────┐      ┌──────────────┐       │  │
│  │  │  BO/DAO      │      │  Static      │       │  │
│  │  │  (*.class)   │      │  (css/js)    │       │  │
│  │  └──────────────┘      └──────────────┘       │  │
│  │                                                 │  │
│  │  ┌──────────────┐                              │  │
│  │  │  Libraries   │                              │  │
│  │  │  (WEB-INF/   │                              │  │
│  │  │   lib/*.jar) │                              │  │
│  │  └──────────────┘                              │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

### File System Layout

```
/path/to/tomcat/
├── webapps/
│   └── PDF_CONVERTION-1.0-SNAPSHOT/
│       ├── WEB-INF/
│       │   ├── classes/              # Compiled Java classes
│       │   │   ├── controller/
│       │   │   ├── model/
│       │   │   └── utils/
│       │   ├── lib/                  # JAR dependencies
│       │   │   ├── mysql-connector.jar
│       │   │   ├── Spire.Pdf.jar
│       │   │   └── Spire.Doc.jar
│       │   └── web.xml               # Deployment descriptor
│       ├── uploads/                  # User uploaded files
│       │   ├── *.pdf
│       │   └── *.docx
│       ├── css/
│       ├── js/
│       ├── img/
│       ├── index.jsp
│       └── viewListConvert.jsp
└── logs/
    └── catalina.out                  # Application logs
```

### Network Architecture

```
┌──────────────┐
│   Browser    │
│   (Client)   │
└──────┬───────┘
       │ HTTPS (recommended) / HTTP
       │ Port: 8080 (default Tomcat)
       ▼
┌──────────────────────────────────┐
│     Apache Tomcat Server         │
│     ┌────────────────────────┐   │
│     │  PDF Conversion App    │   │
│     │  - Servlets            │   │
│     │  - Background Worker   │   │
│     └──────────┬─────────────┘   │
└────────────────┼──────────────────┘
                 │ JDBC
                 │ Port: 3306
                 ▼
┌─────────────────────────────────┐
│       MySQL Database             │
│   ┌─────────────────────────┐   │
│   │  pdf_convertion DB      │   │
│   │  - users table          │   │
│   │  - uploads table        │   │
│   └─────────────────────────┘   │
└──────────────────────────────────┘
```

### Scalability Considerations

**Current Implementation:**
- **3 concurrent worker threads**: Process up to 3 files simultaneously
- **Thread pool**: Fixed pool size of 3 workers for optimal performance
- **In-memory queue**: Thread-safe BlockingQueue (not persistent)
- **File storage**: Local filesystem
- **Concurrent users**: Multiple users can upload and process files simultaneously

**Current Capabilities:**
- Throughput: 3x improvement over single-threaded processing
- Can handle multiple users simultaneously
- Files processed in FIFO order by available workers
- Optimal for machines with 14+ threads (uses ~21% of available threads)

**Scaling Options:**

1. **Vertical Scaling**:
   - Increase server RAM/CPU
   - Increase worker thread count (currently set to 3)
   - Adjust NUM_WORKERS constant in ConversionQueue.java
   - Larger thread pool

2. **Horizontal Scaling** (requires architecture changes):
   - Replace in-memory queue with Redis/RabbitMQ
   - Shared file storage (NFS/S3)
   - Database connection pooling
   - Load balancer for multiple Tomcat instances

3. **Performance Optimization**:
   - Add caching layer (Redis)
   - Implement pagination for large lists
   - Optimize database queries
   - Add database indexes
   - Compress static assets

---

## Conclusion

The PDF Conversion System implements a clean MVC architecture with an asynchronous queue-based processing model. Key architectural strengths include:

- **Separation of Concerns**: Clear MVC layers
- **Asynchronous Processing**: Non-blocking user experience
- **Thread Safety**: Proper concurrent programming
- **Database Persistence**: Reliable data storage
- **Extensibility**: Easy to add new features

The system is production-ready for small to medium loads, with clear paths for scaling and security improvements.

---

**Document Version**: 1.0  
**Last Updated**: 2025-11-16  
**Author**: PDF Conversion Development Team

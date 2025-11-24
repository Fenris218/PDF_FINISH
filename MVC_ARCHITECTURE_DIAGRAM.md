# Thiết kế Mô hình MVC - PDF Conversion System

## Sơ đồ Kiến trúc MVC

```mermaid
graph TB
    subgraph Background["<b>Background</b>"]
        style Background fill:#f5f5f5,stroke:#333,stroke-width:2px
    end

    subgraph AppContainer["<b>Application Container</b>"]
        subgraph View["<b>View (JSP)</b>"]
            style View fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
            JSPIndex["index.jsp<br/>(Upload Form)"]
            JSPViewList["viewListConvert.jsp<br/>(Danh sách chuyển đổi)"]
            JSPLogin["login-modal.jsp<br/>(Đăng nhập)"]
            JSPSignup["signup-modal.jsp<br/>(Đăng ký)"]
        end

        subgraph Controller["<b>Controller (Servlet)</b>"]
            style Controller fill:#fff3e0,stroke:#f57c00,stroke-width:2px
            ConverterServlet["ConverterServlet<br/>(Upload & Queue)"]
            ListConvertServlet["ListConvertServlet<br/>(Hiển thị danh sách)"]
            LoginServlet["LoginServlet<br/>(Xác thực)"]
            DownloadFileServlet["DownloadFileServlet<br/>(Tải file)"]
        end

        subgraph Model["<b>Model</b>"]
            style Model fill:#e8f5e9,stroke:#388e3c,stroke-width:2px
            
            subgraph BEAN["<b>BEAN (Data Objects)</b>"]
                style BEAN fill:#c8e6c9,stroke:#2e7d32,stroke-width:1px
                User["User.java<br/>(username, password, email)"]
                Upload["Upload.java<br/>(id, filename, status, date)"]
                ConversionTask["ConversionTask.java<br/>(taskId, paths, status)"]
            end

            subgraph BO["<b>BO (Business Logic)</b>"]
                style BO fill:#fff9c4,stroke:#f9a825,stroke-width:1px
                LoginBO["LoginBO<br/>(Login logic)"]
                ConverterBO["ConverterBO<br/>(Conversion logic)"]
                ConversionQueue["ConversionQueue<br/>(Singleton queue)"]
                ConversionWorker["ConversionWorker<br/>(Background thread)"]
                PdfConvertionHelper["PdfConvertionHelper<br/>(PDF conversion)"]
            end

            subgraph DAO["<b>DAO (Data Access)</b>"]
                style DAO fill:#f3e5f5,stroke:#7b1fa2,stroke-width:1px
                LoginDAO["LoginDAO<br/>(User queries)"]
                ConverterDAO["ConverterDAO<br/>(Upload queries)"]
            end
        end
    end

    subgraph Database["<b>Database (MySQL)</b>"]
        style Database fill:#ffebee,stroke:#c62828,stroke-width:2px
        UsersTable["users table<br/>(username, password, email)"]
        UploadsTable["uploads table<br/>(id, status, files, date)"]
    end

    %% View to Controller connections
    JSPIndex -->|"HTTP POST<br/>(upload file)"| ConverterServlet
    JSPViewList -->|"HTTP GET<br/>(view list)"| ListConvertServlet
    JSPLogin -->|"HTTP POST<br/>(login)"| LoginServlet
    JSPSignup -->|"HTTP POST<br/>(register)"| LoginServlet

    %% Controller to View connections
    ConverterServlet -.->|"Forward<br/>(response)"| JSPIndex
    ListConvertServlet -.->|"Forward<br/>(data)"| JSPViewList
    LoginServlet -.->|"Redirect"| JSPIndex

    %% Controller to Model connections
    ConverterServlet -->|"saveHistory()"| ConverterBO
    ConverterServlet -->|"addTask()"| ConversionQueue
    ListConvertServlet -->|"getList()"| ConverterBO
    LoginServlet -->|"checkLogin()"| LoginBO
    LoginServlet -->|"addUser()"| LoginBO

    %% BO to DAO connections
    LoginBO -->|"Query"| LoginDAO
    ConverterBO -->|"Query"| ConverterDAO

    %% BO internal connections
    ConversionQueue -->|"Manage"| ConversionWorker
    ConversionWorker -->|"updateStatus()"| ConverterBO
    ConversionWorker -->|"convert()"| PdfConvertionHelper

    %% BO to BEAN connections
    ConverterBO -.->|"Use"| Upload
    ConverterBO -.->|"Use"| ConversionTask
    LoginBO -.->|"Use"| User
    ConversionQueue -.->|"Use"| ConversionTask

    %% DAO to Database connections
    LoginDAO -->|"SELECT/INSERT"| UsersTable
    ConverterDAO -->|"SELECT/INSERT/UPDATE"| UploadsTable

    %% Styling
    classDef viewStyle fill:#bbdefb,stroke:#1976d2,stroke-width:2px
    classDef controllerStyle fill:#ffe0b2,stroke:#f57c00,stroke-width:2px
    classDef beanStyle fill:#c8e6c9,stroke:#2e7d32,stroke-width:2px
    classDef boStyle fill:#fff59d,stroke:#f9a825,stroke-width:2px
    classDef daoStyle fill:#e1bee7,stroke:#7b1fa2,stroke-width:2px
    classDef dbStyle fill:#ffcdd2,stroke:#c62828,stroke-width:2px

    class JSPIndex,JSPViewList,JSPLogin,JSPSignup viewStyle
    class ConverterServlet,ListConvertServlet,LoginServlet,DownloadFileServlet controllerStyle
    class User,Upload,ConversionTask beanStyle
    class LoginBO,ConverterBO,ConversionQueue,ConversionWorker,PdfConvertionHelper boStyle
    class LoginDAO,ConverterDAO daoStyle
    class UsersTable,UploadsTable dbStyle
```

## Luồng xử lý chính

### 1. Luồng Upload File (Conversion Flow)
```
User (View) → index.jsp
    ↓ Submit form
ConverterServlet (Controller)
    ↓ Save file & create task
ConverterBO (Model - BO)
    ↓ Save to database
ConverterDAO (Model - DAO)
    ↓ INSERT query
Database (uploads table)
    ↓ Return upload ID
ConversionQueue (Model - BO)
    ↓ Add to queue
ConversionWorker (Background Thread)
    ↓ Process conversion
PdfConvertionHelper (Model - BO)
    ↓ Update status
ConverterDAO → Database
```

### 2. Luồng Xem Danh Sách (List View Flow)
```
User (View) → viewListConvert.jsp
    ↓ Request list
ListConvertServlet (Controller)
    ↓ Get username from session
ConverterBO (Model - BO)
    ↓ Query database
ConverterDAO (Model - DAO)
    ↓ SELECT query
Database (uploads table)
    ↓ Return list
ConverterDAO → ConverterBO → ListConvertServlet
    ↓ Forward with data
viewListConvert.jsp (View)
    ↓ Render list
User sees results
```

### 3. Luồng Đăng Nhập (Login Flow)
```
User (View) → login-modal.jsp
    ↓ Submit credentials
LoginServlet (Controller)
    ↓ Validate
LoginBO (Model - BO)
    ↓ Check credentials
LoginDAO (Model - DAO)
    ↓ SELECT query
Database (users table)
    ↓ Return user
LoginDAO → LoginBO → LoginServlet
    ↓ Create session & redirect
index.jsp (View)
```

## Chi tiết các thành phần

### VIEW Layer (Presentation)
- **index.jsp**: Trang chủ với form upload file PDF
- **viewListConvert.jsp**: Hiển thị danh sách file đã chuyển đổi
- **login-modal.jsp**: Modal đăng nhập
- **signup-modal.jsp**: Modal đăng ký tài khoản
- **header.jsp**: Header chung cho các trang

### CONTROLLER Layer (Request Handling)
- **ConverterServlet**: Xử lý upload file và thêm vào queue
- **ListConvertServlet**: Lấy và hiển thị danh sách chuyển đổi
- **LoginServlet**: Xử lý đăng nhập/đăng ký
- **DownloadFileServlet**: Xử lý download file DOCX

### MODEL Layer (Business Logic & Data)

#### BEAN (Data Transfer Objects)
- **User.java**: Đối tượng người dùng
  - username: String
  - password: String
  - email: String

- **Upload.java**: Đối tượng lịch sử chuyển đổi
  - id: int
  - username: String
  - fileNameUpload: String
  - fileNameOutput: String
  - status: String (queued/processing/completed/failed)
  - date: Timestamp

- **ConversionTask.java**: Đối tượng task trong queue
  - taskId: int
  - username: String
  - inputFilePath: String
  - outputFilePath: String

#### BO (Business Objects)
- **LoginBO**: Logic nghiệp vụ đăng nhập/đăng ký
- **ConverterBO**: Logic nghiệp vụ chuyển đổi file
- **ConversionQueue**: Quản lý hàng đợi (Singleton pattern)
- **ConversionWorker**: Thread xử lý conversion nền
- **PdfConvertionHelper**: Helper chuyển đổi PDF sang DOCX

#### DAO (Data Access Objects)
- **LoginDAO**: Truy vấn database cho user
  - checkLogin()
  - addUser()
  
- **ConverterDAO**: Truy vấn database cho uploads
  - saveHistory()
  - getListFileConvert()
  - updateStatus()

### DATABASE Layer
- **users table**: Lưu thông tin người dùng
- **uploads table**: Lưu lịch sử chuyển đổi

## Các Pattern được sử dụng

1. **MVC Pattern**: Tách biệt View, Controller, Model
2. **Singleton Pattern**: ConversionQueue
3. **DAO Pattern**: Tách biệt logic truy cập database
4. **Producer-Consumer Pattern**: ConversionQueue + ConversionWorker
5. **Thread Pool Pattern**: Background worker thread

## Đặc điểm kiến trúc

✅ **Tách biệt rõ ràng**: View - Controller - Model
✅ **Xử lý bất đồng bộ**: Queue-based processing
✅ **Thread-safe**: Sử dụng BlockingQueue và AtomicInteger
✅ **Scalable**: Có thể mở rộng thêm worker threads
✅ **Maintainable**: Dễ bảo trì và nâng cấp

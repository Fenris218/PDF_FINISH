# Hướng dẫn Sử dụng các Sơ đồ MVC

## Tổng quan

Đã tạo **4 tài liệu thiết kế mô hình MVC** cho hệ thống chuyển đổi PDF sang DOCX, tham khảo từ file "bản thiết kế mẫu mô hình MVC.png".

---

## Các file đã tạo

### 1. **THIET_KE_MO_HINH_MVC.md** ⭐ (KHUYẾN NGHỊ)
**File chính - Đọc trực tiếp trên GitHub**

📄 **Định dạng**: Markdown với ASCII diagram  
👁️ **Xem**: Đọc trực tiếp trên GitHub (hiển thị đẹp)  
📊 **Nội dung**:
- Sơ đồ kiến trúc tổng quan (ASCII art)
- Chi tiết từng thành phần (View, Controller, Model)
- 3 luồng xử lý chi tiết:
  - Upload và chuyển đổi file
  - Xem danh sách
  - Đăng nhập
- Code examples cho mỗi class
- Design patterns được sử dụng
- Đặc điểm kiến trúc
- Công nghệ stack

**✅ Ưu điểm**: 
- Đọc dễ nhất, không cần công cụ
- Hiển thị đẹp trên GitHub
- Chi tiết nhất
- Có code examples

**Cách sử dụng**: Mở file và đọc trực tiếp

---

### 2. **MVC_ARCHITECTURE_DIAGRAM.md**
**Sơ đồ Mermaid - Có thể render thành hình**

📄 **Định dạng**: Markdown với Mermaid diagram  
🖼️ **Render**: GitHub tự động render, hoặc dùng Mermaid Live Editor  
📊 **Nội dung**:
- Mermaid flowchart diagram
- Luồng xử lý chính
- Chi tiết các thành phần

**✅ Ưu điểm**: 
- Visual diagram đẹp
- GitHub tự động render
- Có màu sắc phân biệt layer

**Cách sử dụng**:
1. Xem trên GitHub (tự động render)
2. Hoặc copy code vào [Mermaid Live Editor](https://mermaid.live/)
3. Export thành PNG/SVG

**Link Mermaid Live Editor**: https://mermaid.live/

---

### 3. **MVC_Architecture.puml**
**PlantUML diagram - Chuyên nghiệp**

📄 **Định dạng**: PlantUML  
🖼️ **Render**: Cần PlantUML tool  
📊 **Nội dung**:
- Class diagram với relationships
- Đầy đủ attributes và methods
- Notes và annotations

**✅ Ưu điểm**: 
- Chuẩn UML
- Chi tiết technical nhất
- Phù hợp cho documentation chính thức

**Cách render**:

**Option 1: Online PlantUML Editor**
1. Truy cập: http://www.plantuml.com/plantuml/uml/
2. Copy nội dung file .puml
3. Paste và xem kết quả
4. Download PNG/SVG

**Option 2: VS Code Extension**
1. Cài extension "PlantUML"
2. Mở file .puml
3. Press Alt+D để preview
4. Right-click → Export

**Option 3: Command line**
```bash
# Install PlantUML
brew install plantuml  # Mac
apt install plantuml   # Linux

# Generate PNG
plantuml MVC_Architecture.puml

# Generate SVG
plantuml -tsvg MVC_Architecture.puml
```

---

### 4. **MVC_Architecture_Diagram.drawio**
**Draw.io diagram - Dễ chỉnh sửa**

📄 **Định dạng**: Draw.io XML  
🖼️ **Mở với**: Draw.io / diagrams.net  
📊 **Nội dung**:
- Visual diagram với boxes và arrows
- Màu sắc theo từng layer
- Notes và annotations

**✅ Ưu điểm**: 
- Dễ chỉnh sửa
- Visual đẹp
- Export nhiều format

**Cách sử dụng**:

**Option 1: Online (Khuyến nghị)**
1. Truy cập: https://app.diagrams.net/
2. File → Open from → Device
3. Chọn file MVC_Architecture_Diagram.drawio
4. Chỉnh sửa tùy ý
5. File → Export as → PNG/SVG/PDF

**Option 2: Desktop App**
1. Download Draw.io desktop: https://www.diagrams.net/
2. Mở file .drawio
3. Chỉnh sửa và export

**Option 3: VS Code Extension**
1. Cài extension "Draw.io Integration"
2. Mở file .drawio trong VS Code
3. Chỉnh sửa trực tiếp

---

## So sánh các file

| File | Format | Đọc dễ | Visual | Chi tiết | Chỉnh sửa | Khuyến nghị |
|------|--------|--------|--------|----------|-----------|-------------|
| THIET_KE_MO_HINH_MVC.md | Markdown | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ✅ **NHẤT** |
| MVC_ARCHITECTURE_DIAGRAM.md | Mermaid | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ✅ Visual đẹp |
| MVC_Architecture.puml | PlantUML | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ Professional |
| MVC_Architecture_Diagram.drawio | Draw.io | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ Dễ edit |

---

## Khuyến nghị sử dụng

### Cho Developer đọc hiểu hệ thống
👉 **Đọc file**: `THIET_KE_MO_HINH_MVC.md`
- Chi tiết nhất
- Có code examples
- Dễ đọc trên GitHub

### Cho Presentation / Meeting
👉 **Dùng file**: `MVC_Architecture_Diagram.drawio`
- Export thành PNG/PDF
- Visual đẹp, dễ hiểu
- Có màu sắc rõ ràng

### Cho Documentation chính thức
👉 **Dùng file**: `MVC_Architecture.puml`
- Chuẩn UML
- Professional
- Chi tiết technical

### Cho GitHub README
👉 **Dùng file**: `MVC_ARCHITECTURE_DIAGRAM.md`
- Mermaid tự động render
- Visual đẹp
- Không cần tool

---

## Cấu trúc MVC trong project

```
PDF_FINISH/
├── src/main/
│   ├── java/
│   │   ├── controller/        ← CONTROLLER Layer
│   │   │   ├── ConverterServlet.java
│   │   │   ├── ListConvertServlet.java
│   │   │   ├── LoginServlet.java
│   │   │   └── DownloadFileServlet.java
│   │   │
│   │   └── model/             ← MODEL Layer
│   │       ├── BEAN/          ← Data Objects
│   │       │   ├── User.java
│   │       │   ├── Upload.java
│   │       │   └── ConversionTask.java
│   │       │
│   │       ├── BO/            ← Business Logic
│   │       │   ├── LoginBO.java
│   │       │   ├── ConverterBO.java
│   │       │   ├── ConversionQueue.java
│   │       │   ├── ConversionWorker.java
│   │       │   └── PdfConvertionHelper.java
│   │       │
│   │       └── DAO/           ← Data Access
│   │           ├── LoginDAO.java
│   │           └── ConverterDAO.java
│   │
│   └── webapp/                ← VIEW Layer
│       ├── index.jsp
│       ├── viewListConvert.jsp
│       ├── login-modal.jsp
│       ├── signup-modal.jsp
│       └── header.jsp
│
├── THIET_KE_MO_HINH_MVC.md          ← 📚 File chính (Khuyến nghị)
├── MVC_ARCHITECTURE_DIAGRAM.md      ← 🎨 Mermaid diagram
├── MVC_Architecture.puml             ← 📐 PlantUML diagram
└── MVC_Architecture_Diagram.drawio   ← ✏️ Draw.io diagram
```

---

## Các thành phần chính

### 🎨 VIEW (JSP)
- `index.jsp` - Upload form
- `viewListConvert.jsp` - Danh sách chuyển đổi
- `login-modal.jsp` - Đăng nhập
- `signup-modal.jsp` - Đăng ký

### 🎮 CONTROLLER (Servlet)
- `ConverterServlet` - Xử lý upload
- `ListConvertServlet` - Hiển thị danh sách
- `LoginServlet` - Xác thực
- `DownloadFileServlet` - Download file

### 📦 MODEL

#### BEAN (Data Objects)
- `User` - Thông tin user
- `Upload` - Lịch sử chuyển đổi
- `ConversionTask` - Task trong queue

#### BO (Business Logic)
- `LoginBO` - Logic đăng nhập
- `ConverterBO` - Logic chuyển đổi
- `ConversionQueue` - Quản lý queue (Singleton)
- `ConversionWorker` - Background thread
- `PdfConvertionHelper` - Chuyển đổi PDF

#### DAO (Data Access)
- `LoginDAO` - Truy vấn users
- `ConverterDAO` - Truy vấn uploads

### 🗄️ DATABASE
- `users` table - Tài khoản
- `uploads` table - Lịch sử chuyển đổi

---

## Design Patterns

1. **MVC Pattern** - Tách biệt View-Controller-Model
2. **Singleton Pattern** - ConversionQueue
3. **DAO Pattern** - Data access layer
4. **Producer-Consumer** - Queue + Worker
5. **Thread Pool** - Background processing

---

## Quick Start

### Bước 1: Đọc tổng quan
```bash
# Đọc file chính
cat THIET_KE_MO_HINH_MVC.md
```

### Bước 2: Xem diagram
```bash
# Option 1: Mở trên GitHub (tự động render Mermaid)
# MVC_ARCHITECTURE_DIAGRAM.md

# Option 2: Render PlantUML
plantuml MVC_Architecture.puml

# Option 3: Mở Draw.io
# Vào https://app.diagrams.net/
# Open → MVC_Architecture_Diagram.drawio
```

### Bước 3: Export diagram cho presentation
```bash
# Export từ Draw.io
# File → Export as → PNG (300 DPI)

# Hoặc export từ PlantUML
plantuml -tpng MVC_Architecture.puml
plantuml -tsvg MVC_Architecture.puml
```

---

## Tips

### 📖 Để hiểu kiến trúc nhanh nhất
1. Đọc phần "Sơ đồ kiến trúc tổng quan" trong `THIET_KE_MO_HINH_MVC.md`
2. Xem "Luồng xử lý chi tiết" → Upload và chuyển đổi file
3. Đối chiếu với code trong project

### 🎨 Để tạo presentation
1. Mở `MVC_Architecture_Diagram.drawio` trong Draw.io
2. Chỉnh sửa màu sắc, layout theo ý muốn
3. Export thành PNG (300 DPI) hoặc PDF
4. Import vào PowerPoint/Google Slides

### 📐 Để cập nhật diagram
1. **Mermaid**: Edit trực tiếp trong `MVC_ARCHITECTURE_DIAGRAM.md`
2. **PlantUML**: Edit trong `MVC_Architecture.puml`, render lại
3. **Draw.io**: Mở trong Draw.io, edit visual, save

---

## Câu hỏi thường gặp

### Q: File nào nên đọc đầu tiên?
**A**: `THIET_KE_MO_HINH_MVC.md` - Chi tiết và dễ đọc nhất

### Q: Làm sao để xem diagram đẹp nhất?
**A**: Mở `MVC_ARCHITECTURE_DIAGRAM.md` trên GitHub (tự động render) hoặc export `MVC_Architecture_Diagram.drawio` thành PNG

### Q: File nào dùng cho báo cáo?
**A**: Export `MVC_Architecture.puml` hoặc `MVC_Architecture_Diagram.drawio` thành PDF

### Q: Muốn chỉnh sửa diagram?
**A**: Dùng `MVC_Architecture_Diagram.drawio` - dễ chỉnh sửa nhất

### Q: Diagram có giống file mẫu "bản thiết kế mẫu mô hình MVC.png"?
**A**: Có, đã tham khảo cấu trúc: View → Controller → Model (BO/DAO) → Database, với màu sắc và layout tương tự

---

## Tài liệu liên quan

- `README_VI.md` - Hướng dẫn sử dụng hệ thống
- `ARCHITECTURE.md` - Chi tiết kiến trúc (English)
- `WORKFLOW_ANALYSIS.md` - Phân tích workflow
- `database_setup.sql` - Cấu trúc database

---

**Tạo bởi**: GitHub Copilot Agent  
**Ngày**: 2025-11-24  
**Mục đích**: Thiết kế mô hình MVC cho hệ thống PDF Conversion

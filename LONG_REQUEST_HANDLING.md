# Xử lý Request Với Thời Gian Lớn - Tránh Request Timeout

## Cách thức xử lý

Để xử lý một request với thời gian lớn mà không bị "request timeout", ứng dụng sử dụng **mô hình xử lý bất đồng bộ với hàng đợi (Queue-based Asynchronous Processing)**:

1. **Phản hồi ngay lập tức**: Khi client gửi request upload file, server lưu thông tin vào database với trạng thái "queued" và thêm task vào hàng đợi, sau đó trả về response ngay mà không chờ xử lý xong.

2. **Xử lý nền (Background Processing)**: Các worker thread chạy nền sẽ lấy task từ hàng đợi và thực hiện việc chuyển đổi file một cách bất đồng bộ.

3. **Theo dõi trạng thái**: Client có thể kiểm tra trạng thái xử lý (queued → processing → completed/failed) thông qua trang danh sách.

---

## Trích dẫn code minh họa (tối đa 10 dòng)

```java
// [ConverterServlet.java] - Tạo task và thêm vào queue, trả response ngay lập tức
ConversionTask newTask = new ConversionTask(0, username, filePathInServer, fileNameUserUpload, fileNameInServer);
ConversionQueue queue = ConversionQueue.getInstance();  // Singleton pattern
int taskId = queue.addTask(newTask);  // Thêm vào hàng đợi, không chờ xử lý

// [ConversionQueue.java] - Sử dụng BlockingQueue và ExecutorService để xử lý bất đồng bộ
private final BlockingQueue<ConversionTask> taskQueue = new LinkedBlockingQueue<>();
private final ExecutorService executorService = Executors.newFixedThreadPool(NUM_WORKERS);

// [ConversionWorker.java] - Worker thread lấy và xử lý task từ queue ở background
ConversionTask queuedTask = queue.take();  // Lấy task từ hàng đợi (blocking)
processTask(queuedTask, workerName);       // Xử lý chuyển đổi file bất đồng bộ
```

**Kết quả**: Request được phản hồi trong vài giây, trong khi việc chuyển đổi file (có thể mất vài phút) được xử lý ở background mà không gây timeout.

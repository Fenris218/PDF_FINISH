# Xử Lý Đồng Thời Nhiều File PDF

## Tổng Quan

Hệ thống đã được cải tiến để hỗ trợ **xử lý đồng thời nhiều file PDF** từ nhiều người dùng khác nhau. Thay vì xử lý tuần tự từng file một, hệ thống hiện có thể xử lý **tối đa 3 file cùng lúc**.

## Kịch Bản Sử Dụng

### Kịch Bản 1: Một người dùng upload nhiều file
**Tình huống:** User A upload 5 file PDF

```
Thời gian    | File 1      | File 2      | File 3      | File 4   | File 5
─────────────┼─────────────┼─────────────┼─────────────┼──────────┼─────────
00:00        | Processing  | Processing  | Processing  | Queued   | Queued
00:10        | Completed   | Processing  | Processing  | Processing | Queued
00:20        | Completed   | Completed   | Processing  | Processing | Processing
00:30        | Completed   | Completed   | Completed   | Processing | Processing
00:40        | Completed   | Completed   | Completed   | Completed  | Processing
00:50        | Completed   | Completed   | Completed   | Completed  | Completed
```

**Kết quả:**
- 3 file đầu tiên được xử lý đồng thời (processing)
- 2 file còn lại chờ trong queue
- Tổng thời gian: ~50 giây (so với ~150 giây nếu xử lý tuần tự)

### Kịch Bản 2: Nhiều người dùng cùng upload
**Tình huống:**
- User A upload 2 file (file1.pdf, file2.pdf)
- User B upload 2 file (file3.pdf, file4.pdf)

```
Thời gian    | Worker-1     | Worker-2     | Worker-3     | Queue
─────────────┼──────────────┼──────────────┼──────────────┼─────────────
00:00        | File1 (A)    | File2 (A)    | File3 (B)    | [File4 (B)]
00:10        | Processing   | Processing   | Processing   | [File4 (B)]
00:20        | Completed    | Processing   | Processing   | [File4 (B)]
00:21        | File4 (B)    | Processing   | Processing   | []
00:30        | Processing   | Completed    | Processing   | []
00:40        | Processing   | Idle         | Completed    | []
00:50        | Completed    | Idle         | Idle         | []
```

**Kết quả:**
- File thứ 2 của User B (file4) phải chờ trong queue
- 3 file đầu tiên được xử lý song song
- User A và User B đều có file được xử lý đồng thời

### Kịch Bản 3: Ba người dùng mỗi người một file
**Tình huống:**
- User A upload file1.pdf
- User B upload file2.pdf
- User C upload file3.pdf

```
Thời gian    | Worker-1     | Worker-2     | Worker-3     | Queue
─────────────┼──────────────┼──────────────┼──────────────┼───────
00:00        | File1 (A)    | File2 (B)    | File3 (C)    | []
00:10        | Processing   | Processing   | Processing   | []
00:20        | Processing   | Processing   | Processing   | []
00:30        | Completed    | Completed    | Completed    | []
```

**Kết quả:**
- Cả 3 file được xử lý đồng thời
- Không có file phải chờ trong queue
- Thời gian xử lý tối ưu

## Cải Tiến Kỹ Thuật

### 1. Kiến Trúc Mới

**Trước đây (Single Worker):**
```
ConversionQueue
    │
    └── ConversionWorker (1 thread)
            └── Process 1 file at a time
```

**Hiện tại (Thread Pool):**
```
ConversionQueue
    │
    └── ExecutorService (Fixed Thread Pool)
            ├── Worker-1 (process file in parallel)
            ├── Worker-2 (process file in parallel)
            └── Worker-3 (process file in parallel)
```

### 2. Thay Đổi Code

#### ConversionQueue.java
```java
// Trước đây
private ConversionWorker worker;
this.worker = new ConversionWorker(queue);
this.worker.start();

// Hiện tại
private final ExecutorService executorService;
private static final int NUM_WORKERS = 3;

this.executorService = Executors.newFixedThreadPool(NUM_WORKERS);
for (int i = 0; i < NUM_WORKERS; i++) {
    executorService.submit(new ConversionWorker(queue, i + 1));
}
```

#### ConversionWorker.java
```java
// Trước đây
public class ConversionWorker extends Thread {
    private volatile boolean running = true;
    
    public void shutdown() {
        running = false;
        this.interrupt();
    }
}

// Hiện tại
public class ConversionWorker implements Runnable {
    private final int workerId;
    
    public ConversionWorker(BlockingQueue<ConversionTask> queue, int workerId) {
        this.queue = queue;
        this.workerId = workerId;
    }
    
    // Managed by ExecutorService, no manual shutdown needed
}
```

### 3. Thread Safety

Hệ thống đảm bảo thread-safe thông qua:

1. **BlockingQueue**: Thread-safe queue operations
   ```java
   queue.put(task);  // Thread-safe add
   queue.take();     // Thread-safe remove (blocking)
   ```

2. **AtomicInteger**: Atomic task ID generation
   ```java
   taskIdCounter.incrementAndGet();  // Atomic increment
   ```

3. **Database Updates**: Each worker updates independently
   - No race conditions on status updates
   - Each task has unique identifier

## Hiệu Suất

### So Sánh Throughput

| Metric                  | Single Worker | 3 Workers (Concurrent) | Improvement |
|------------------------|---------------|------------------------|-------------|
| Files processed/hour   | 120-720       | 360-2160              | 3x          |
| Concurrent files       | 1             | 3                     | 3x          |
| Queue wait time        | High          | Low                   | 67% faster  |
| User experience        | Sequential    | Parallel              | Much better |

### Tài Nguyên Hệ Thống

- **CPU Threads**: Sử dụng 3 threads (21% của 14 threads có sẵn)
- **Memory**: ~300-1500MB cho 3 conversions đồng thời
- **Disk I/O**: 3 files được đọc/ghi song song

## Cấu Hình

### Điều Chỉnh Số Worker Threads

Để thay đổi số lượng worker threads, chỉnh sửa trong `ConversionQueue.java`:

```java
private static final int NUM_WORKERS = 3;  // Thay đổi số này
```

**Khuyến nghị:**
- **Máy 14 threads**: NUM_WORKERS = 3-5 (optimal)
- **Máy 8 threads**: NUM_WORKERS = 2-3
- **Máy 4 threads**: NUM_WORKERS = 1-2

**Lưu ý:** Mỗi worker consume ~100-500MB RAM trong quá trình conversion.

## Logging

Workers có logging chi tiết với worker ID:

```
ConversionQueue initialized with 3 worker threads
ConversionWorker-1 started
ConversionWorker-2 started
ConversionWorker-3 started
Task 1 added to queue. Queue size: 1
ConversionWorker-1 processing task 1 for user johndoe
ConversionWorker-2 processing task 2 for user janedoe
ConversionWorker-3 processing task 3 for user bobsmith
ConversionWorker-1 completed task 1 successfully
```

## Giám Sát

Có thể theo dõi hoạt động của hệ thống qua:

1. **Console logs**: Xem worker nào đang xử lý task nào
2. **Database status**: Kiểm tra trạng thái file (queued/processing/completed)
3. **Queue size**: `ConversionQueue.getInstance().getQueueSize()`

## Tương Lai

Có thể mở rộng thêm:

1. **Dynamic Thread Pool**: Tự động điều chỉnh số workers dựa trên load
2. **Priority Queue**: Ưu tiên xử lý file của premium users
3. **Distributed Processing**: Sử dụng multiple servers với shared queue (Redis)
4. **Monitoring Dashboard**: Hiển thị real-time metrics

## Kết Luận

Với cải tiến xử lý đồng thời:
- ✅ Hỗ trợ nhiều người dùng cùng lúc
- ✅ Xử lý tối đa 3 file song song
- ✅ Tăng 3x throughput
- ✅ Giảm thời gian chờ đợi
- ✅ Thread-safe và ổn định
- ✅ Tối ưu cho máy 14 threads

Hệ thống hiện đã sẵn sàng phục vụ nhiều người dùng với hiệu suất cao hơn!

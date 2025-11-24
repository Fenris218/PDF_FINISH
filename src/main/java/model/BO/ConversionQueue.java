package model.BO;

import model.BEAN.ConversionTask;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConversionQueue {
	private static ConversionQueue instance;
	private final BlockingQueue<ConversionTask> queue;
	private final AtomicInteger taskIdCounter;
	private final ExecutorService executorService;
	private static final int NUM_WORKERS = 3; // Process 3 files concurrently

	private ConversionQueue() {
		this.queue = new LinkedBlockingQueue<>();
		this.taskIdCounter = new AtomicInteger(0);
		// Create thread pool with 3 worker threads
		this.executorService = Executors.newFixedThreadPool(NUM_WORKERS);
		// Start worker threads
		for (int i = 0; i < NUM_WORKERS; i++) {
			executorService.submit(new ConversionWorker(queue, i + 1));
		}
		System.out.println("ConversionQueue initialized with " + NUM_WORKERS + " worker threads");
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
		try {
			queue.put(task);
			System.out.println("Task " + taskId + " added to queue. Queue size: " + queue.size());
		} catch (InterruptedException e) {
			System.err.println("Error adding task to queue: " + e.getMessage());
			Thread.currentThread().interrupt();
		}
		return taskId;
	}

	public int getQueueSize() {
		return queue.size();
	}

	public void shutdown() {
		if (executorService != null) {
			executorService.shutdown();
		}
	}
}

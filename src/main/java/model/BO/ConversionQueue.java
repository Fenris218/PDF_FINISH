package model.BO;

import model.BEAN.ConversionTask;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConversionQueue {
	private static ConversionQueue instance;
	private final BlockingQueue<ConversionTask> queue;
	private final AtomicInteger taskIdCounter;
	private ConversionWorker worker;

	private ConversionQueue() {
		this.queue = new LinkedBlockingQueue<>();
		this.taskIdCounter = new AtomicInteger(0);
		// Start the background worker
		this.worker = new ConversionWorker(queue);
		this.worker.start();
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
		if (worker != null) {
			worker.shutdown();
		}
	}
}

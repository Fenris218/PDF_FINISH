package model.BO;

import model.BEAN.ConversionTask;
import utils.Utils;

import java.util.concurrent.BlockingQueue;

public class ConversionWorker extends Thread {
	private final BlockingQueue<ConversionTask> queue;
	private volatile boolean running = true;
	private final ConverterBO converterBO;

	public ConversionWorker(BlockingQueue<ConversionTask> queue) {
		this.queue = queue;
		this.converterBO = new ConverterBO();
		this.setDaemon(true); // Make it a daemon thread
		this.setName("ConversionWorker");
	}

	@Override
	public void run() {
		System.out.println("ConversionWorker started");
		while (running) {
			try {
				ConversionTask task = queue.take();
				processTask(task);
			} catch (InterruptedException e) {
				System.err.println("ConversionWorker interrupted: " + e.getMessage());
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				System.err.println("Error processing task: " + e.getMessage());
			}
		}
		System.out.println("ConversionWorker stopped");
	}

	private void processTask(ConversionTask task) {
		System.out.println("Processing task " + task.getId() + " for user " + task.getUsername());
		
		// Update status to processing
		task.setStatus("processing");
		converterBO.updateStatus(task.getUsername(), task.getFileNameInServer(), "processing");

		try {
			// Perform the actual conversion
			ConverterThread thread = new ConverterThread(task.getFilePathInServer());
			thread.start();
			thread.join();

			// Delete the uploaded file
			Utils.deleteFile(task.getFilePathInServer());

			// Update status to completed
			task.setStatus("completed");
			converterBO.updateStatus(task.getUsername(), task.getFileNameInServer(), "completed");

			System.out.println("Task " + task.getId() + " completed successfully");
		} catch (Exception e) {
			System.err.println("Task " + task.getId() + " failed: " + e.getMessage());
			task.setStatus("failed");
			converterBO.updateStatus(task.getUsername(), task.getFileNameInServer(), "failed");
		}
	}

	public void shutdown() {
		running = false;
		this.interrupt();
	}
}

package model.BO;

import model.BEAN.ConversionTask;
import utils.Utils;

import java.util.concurrent.BlockingQueue;

public class ConversionWorker implements Runnable {
	private final BlockingQueue<ConversionTask> queue;
	private final ConverterBO converterBO;
	private final int workerId;

	public ConversionWorker(BlockingQueue<ConversionTask> queue, int workerId) {
		this.queue = queue;
		this.converterBO = new ConverterBO();
		this.workerId = workerId;
	}

	@Override
	public void run() {
		String workerName = "ConversionWorker-" + workerId;
		System.out.println(workerName + " started");
		
		while (!Thread.currentThread().isInterrupted()) {
			try {
				ConversionTask task = queue.take();
				processTask(task, workerName);
			} catch (InterruptedException e) {
				System.out.println(workerName + " interrupted, shutting down");
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				System.err.println(workerName + " error processing task: " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		System.out.println(workerName + " stopped");
	}

	private void processTask(ConversionTask task, String workerName) {
		System.out.println(workerName + " processing task " + task.getId() + " for user " + task.getUsername());
		
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

			System.out.println(workerName + " completed task " + task.getId() + " successfully");
		} catch (Exception e) {
			System.err.println(workerName + " task " + task.getId() + " failed: " + e.getMessage());
			task.setStatus("failed");
			converterBO.updateStatus(task.getUsername(), task.getFileNameInServer(), "failed");
		}
	}
}

package com.fileconverter.bo;

import com.fileconverter.bean.ConversionTask;

public class ConversionTaskBO {
    
    public void updateTaskStatus(int taskId, ConversionTask.ConversionStatus status, int progress) {
        // Update task status in database or memory
        System.out.println("Task " + taskId + " - Status: " + status + ", Progress: " + progress + "%");
    }
    
    public void updateTaskFailed(int taskId, String errorMessage) {
        System.err.println("Task " + taskId + " failed: " + errorMessage);
    }
}

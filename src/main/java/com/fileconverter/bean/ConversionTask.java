package com.fileconverter.bean;

public class ConversionTask {
    private int taskId;
    private String username;
    private String inputFilePath;
    private String outputFilePath;
    private ConversionStatus status;
    private int progress;
    private String errorMessage;

    public enum ConversionStatus {
        QUEUED,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public ConversionTask(int taskId, String username, String inputFilePath) {
        this.taskId = taskId;
        this.username = username;
        this.inputFilePath = inputFilePath;
        this.status = ConversionStatus.QUEUED;
        this.progress = 0;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInputFilePath() {
        return inputFilePath;
    }

    public void setInputFilePath(String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public ConversionStatus getStatus() {
        return status;
    }

    public void setStatus(ConversionStatus status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

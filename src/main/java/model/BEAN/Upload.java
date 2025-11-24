package model.BEAN;

public class Upload {
	private String fileNameUpload;
	private String fileNameOutput;
	private String fileNameOutputInServer;
	private String date;
	private String status; // queued, processing, completed, failed

	public Upload(String fileNameUpload, String fileNameOutput, String fileNameOutputInServer, String date) {
		this.fileNameUpload = fileNameUpload;
		this.fileNameOutput = fileNameOutput;
		this.fileNameOutputInServer = fileNameOutputInServer;
		this.date = date;
		this.status = "completed"; // Default for backward compatibility
	}

	public Upload(String fileNameUpload, String fileNameOutput, String fileNameOutputInServer, String date, String status) {
		this.fileNameUpload = fileNameUpload;
		this.fileNameOutput = fileNameOutput;
		this.fileNameOutputInServer = fileNameOutputInServer;
		this.date = date;
		this.status = status;
	}

	public String getFileNameUpload() {
		return fileNameUpload;
	}

	public void setFileNameUpload(String fileNameUpload) {
		this.fileNameUpload = fileNameUpload;
	}

	public String getFileNameOutput() {
		return fileNameOutput;
	}

	public void setFileNameOutput(String fileNameOutput) {
		this.fileNameOutput = fileNameOutput;
	}

	public String getFileNameOutputInServer() {
		return fileNameOutputInServer;
	}

	public void setFileNameOutputInServer(String fileNameOutputInServer) {
		this.fileNameOutputInServer = fileNameOutputInServer;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}

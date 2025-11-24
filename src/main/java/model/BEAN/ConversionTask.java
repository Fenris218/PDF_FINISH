package model.BEAN;

public class ConversionTask {
	private int id;
	private String username;
	private String filePathInServer;
	private String fileNameUserUpload;
	private String fileNameInServer;
	private String status; // queued, processing, completed, failed
	private long createdAt;

	public ConversionTask(int id, String username, String filePathInServer, String fileNameUserUpload,
			String fileNameInServer) {
		this.id = id;
		this.username = username;
		this.filePathInServer = filePathInServer;
		this.fileNameUserUpload = fileNameUserUpload;
		this.fileNameInServer = fileNameInServer;
		this.status = "queued";
		this.createdAt = System.currentTimeMillis();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFilePathInServer() {
		return filePathInServer;
	}

	public void setFilePathInServer(String filePathInServer) {
		this.filePathInServer = filePathInServer;
	}

	public String getFileNameUserUpload() {
		return fileNameUserUpload;
	}

	public void setFileNameUserUpload(String fileNameUserUpload) {
		this.fileNameUserUpload = fileNameUserUpload;
	}

	public String getFileNameInServer() {
		return fileNameInServer;
	}

	public void setFileNameInServer(String fileNameInServer) {
		this.fileNameInServer = fileNameInServer;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}
}

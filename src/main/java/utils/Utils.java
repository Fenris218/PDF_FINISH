package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Utils {
	// Regex pattern for case-insensitive PDF extension matching
	public static final String PDF_EXTENSION_REGEX = "(?i)\\.pdf$";
	public static final String DOCX_EXTENSION = ".docx";
	
	public static Connection getConnection() {
		Connection conn = null;
		try {
            Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/pdf_convertion", "root", "Hoang8212005@");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return conn;
	}

	public static void redirectToPage(HttpServletRequest request, HttpServletResponse response, String destination) {
		try {
			response.sendRedirect(request.getContextPath() + destination);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Date convertStringToDate(String dateString) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		Date date = new Date();
		try {
			date = formatter.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public static void deleteFile(String filePath) {
		try {
			Path path = Paths.get(filePath);
			Files.deleteIfExists(path);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public static void downloadFile(HttpServletRequest request, HttpServletResponse response, String fileName) {
		try {
			// Trim whitespace from filename to avoid file not found errors
			fileName = fileName.trim();
			
			String fileNameOutput = fileName.substring(fileName.indexOf("_") + 1);
			System.out.println("Output:" + fileNameOutput);
			System.out.println("Downloading: " + fileName);

			String filePath = request.getServletContext().getRealPath("/upload") + "/" + fileName;
			
			// Check if file exists before attempting download
			java.io.File file = new java.io.File(filePath);
			if (!file.exists()) {
				System.err.println("File not found: " + filePath);
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
				return;
			}
			
			String mimeType = request.getServletContext().getMimeType(fileName);

			if (mimeType == null) {
				mimeType = "application/octet-stream";
			}

			// Thiết lập thông số của response để trình duyệt hiểu là file download
			response.setContentType(mimeType);
			
			// URL-encode filename to handle Vietnamese characters and special characters
			String encodedFileName = java.net.URLEncoder.encode(fileNameOutput, "UTF-8").replaceAll("\\+", "%20");
			
			// Use RFC 5987 encoding for better international character support
			response.setHeader("Content-Disposition", 
				"attachment; filename*=UTF-8''" + encodedFileName);

			// Đọc dữ liệu từ file và ghi vào OutputStream của response
			try (FileInputStream fileInputStream = new FileInputStream(filePath);
					OutputStream outputStream = response.getOutputStream()) {
				byte[] buffer = new byte[4096];
				int bytesRead = -1;
				long total = 0;
				while ((bytesRead = fileInputStream.read(buffer)) != -1) {
					total += bytesRead;
					outputStream.write(buffer, 0, bytesRead);
				}
				System.out.println("Tổng bytes:" + total);
			}
		} catch (Exception e) {
			System.err.println("Download error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

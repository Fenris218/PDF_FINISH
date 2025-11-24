package controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import model.BEAN.ConversionTask;
import model.BO.ConverterBO;
import model.BO.ConversionQueue;

import static java.nio.file.Path.of;

@WebServlet("/ConverterServlet")
@MultipartConfig
public class ConverterServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ConverterServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Lấy tất cả các phần (parts) được gửi từ yêu cầu
		Collection<Part> parts = request.getParts();
		String filePathInServer = "";
		String fileNameInServer = "";
		String fileNameUserUpload = "";

		for (Part part : parts) {
			String folderUpload = request.getServletContext().getRealPath("/upload");

			Date now = new Date();
			fileNameUserUpload = of(part.getSubmittedFileName()).getFileName().toString();
			fileNameInServer = now.getTime() + "_" + fileNameUserUpload;
			if (!Files.exists(of(folderUpload))) {
				Files.createDirectory(of(folderUpload));
			}

			filePathInServer = folderUpload + "/" + fileNameInServer;
			part.write(filePathInServer);
		}

		System.out.println("Filename user upload: " + fileNameUserUpload);
		System.out.println("File upload in server: " + filePathInServer);

		// Get username from session
		String username = (String) request.getSession().getAttribute("username");
		
		if (username != null) {
			// Save initial record with "queued" status
			ConverterBO converterBO = new ConverterBO();
			converterBO.saveHistoryWithStatus(username, fileNameUserUpload, fileNameInServer, "queued");

			// Create and add task to queue
			ConversionTask task = new ConversionTask(0, username, filePathInServer, fileNameUserUpload,
					fileNameInServer);
			ConversionQueue queue = ConversionQueue.getInstance();
			int taskId = queue.addTask(task);

			// Return response indicating task is queued
			response.setContentType("text/html; charset=UTF-8");
			response.getWriter().println("<html><body>");
			response.getWriter()
					.println("<h2>Đang thực hiện</h2>");
			response.getWriter().println("<p>File của bạn đã được thêm vào hàng đợi xử lý.</p>");
			response.getWriter().println("<p>Mã công việc: " + taskId + "</p>");
			response.getWriter().println(
					"<p>Bạn có thể xem kết quả xử lý tại <a href='./ListConvertServlet'>danh sách chuyển đổi</a></p>");
			response.getWriter().println("<p><a href='./index.jsp'>Quay lại trang chủ</a></p>");
			response.getWriter().println("</body></html>");
		} else {
			// If user is not logged in, use the old synchronous method
			response.setContentType("text/html; charset=UTF-8");
			response.getWriter().println("<html><body>");
			response.getWriter().println("<h2>Vui lòng đăng nhập</h2>");
			response.getWriter()
					.println("<p>Bạn cần đăng nhập để sử dụng tính năng chuyển đổi file với hàng đợi.</p>");
			response.getWriter().println("<p><a href='./index.jsp'>Quay lại trang chủ</a></p>");
			response.getWriter().println("</body></html>");
		}
	}
}

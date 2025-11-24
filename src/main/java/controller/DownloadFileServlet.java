package controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import utils.Utils;

@WebServlet("/DownloadFileServlet")
public class DownloadFileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public DownloadFileServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = (String) request.getParameter("action");
		if (action.equals("downloadfile")) {
			String fileName = request.getParameter("fileName");
			// URL decode the filename to handle special characters
			if (fileName != null) {
				try {
					fileName = java.net.URLDecoder.decode(fileName, "UTF-8");
				} catch (IllegalArgumentException e) {
					System.err.println("Invalid URL encoding in filename: " + e.getMessage());
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename encoding");
					return;
				}
			}
			Utils.downloadFile(request, response, fileName);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}

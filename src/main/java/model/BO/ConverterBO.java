package model.BO;

import java.util.ArrayList;

import model.BEAN.Upload;
import model.DAO.ConverterDAO;
import utils.Utils;

public class ConverterBO {
	private final ConverterDAO converterDAO;

	public ConverterBO() {
		this.converterDAO = new ConverterDAO();
	}

	public ArrayList<Upload> getListFileConvert(String username) {
		return converterDAO.getListFileConvert(username);
	}

	public void saveHistory(String username, String fileNameUpload, String fileNameInServer) {
		// Case-insensitive replacement of .pdf with .docx
		String fileNameOutput = fileNameUpload.replaceAll(Utils.PDF_EXTENSION_REGEX, Utils.DOCX_EXTENSION);
		String fileNameOutputInServer = fileNameInServer.replaceAll(Utils.PDF_EXTENSION_REGEX, Utils.DOCX_EXTENSION);
		
		converterDAO.saveHistory(username, fileNameUpload, fileNameOutput, fileNameOutputInServer);
	}

	public void saveHistoryWithStatus(String username, String fileNameUpload, String fileNameInServer, String status) {
		// Trim whitespace to avoid file path issues
		fileNameUpload = fileNameUpload.trim();
		fileNameInServer = fileNameInServer.trim();
		
		// Case-insensitive replacement of .pdf with .docx
		String fileNameOutput = fileNameUpload.replaceAll(Utils.PDF_EXTENSION_REGEX, Utils.DOCX_EXTENSION);
		String fileNameOutputInServer = fileNameInServer.replaceAll(Utils.PDF_EXTENSION_REGEX, Utils.DOCX_EXTENSION);
		
		converterDAO.saveHistoryWithStatus(username, fileNameUpload, fileNameOutput,
				fileNameOutputInServer, status);
	}

	public void updateStatus(String username, String fileNameInServer, String status) {
		converterDAO.updateStatus(username, fileNameInServer, status);
	}
}

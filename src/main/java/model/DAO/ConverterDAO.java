package model.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import model.BEAN.Upload;
import utils.Utils;

public class ConverterDAO {
	public ArrayList<Upload> getListFileConvert(String username) {
		ArrayList<Upload> uploads = new ArrayList<>();
		String query = "select * from uploads where username = ? order by date desc";
		try (Connection connection = Utils.getConnection()) {
			if (connection == null) {
				System.err.println("Failed to establish database connection");
				return uploads;
			}
			try (PreparedStatement pst = connection.prepareStatement(query)) {
				pst.setString(1, username);
				try (ResultSet rs = pst.executeQuery()) {
					while (rs.next()) {
						String status = "completed"; // Default value for backward compatibility
						try {
							status = rs.getString("status");
							if (status == null || status.isEmpty()) {
								status = "completed";
							}
						} catch (Exception e) {
							// Column might not exist in older schema
							status = "completed";
						}
						Upload upload = new Upload(rs.getString("fileNameUpload"), rs.getString("fileNameOutput"),
								rs.getString("fileNameOutputInServer"), rs.getString("date"), status);
						uploads.add(upload);
					}
				}
			}
		} catch (Exception e) {
			System.err.println("getListFileConvert: " + e.getMessage());
		}
		return uploads;
	}

	public void saveHistory(String username, String fileNameUpload, String fileNameOutput,
			String fileNameOutputInServer) {
		saveHistoryWithStatus(username, fileNameUpload, fileNameOutput, fileNameOutputInServer, "completed");
	}

	public void saveHistoryWithStatus(String username, String fileNameUpload, String fileNameOutput,
			String fileNameOutputInServer, String status) {
		String sql = "insert into uploads(username, fileNameUpload, fileNameOutput, fileNameOutputInServer, status) "
				+ "values(?,?,?,?,?)";
		try (Connection connection = Utils.getConnection()) {
			if (connection == null) {
				System.err.println("Failed to establish database connection");
				return;
			}
			try (PreparedStatement pst = connection.prepareStatement(sql)) {
				pst.setString(1, username);
				pst.setString(2, fileNameUpload);
				pst.setString(3, fileNameOutput);
				pst.setString(4, fileNameOutputInServer);
				pst.setString(5, status);
				pst.executeUpdate();
			}
		} catch (Exception e) {
			System.err.println("saveHistoryWithStatus: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void updateStatus(String username, String fileNameInServer, String status) {
		String sql = "update uploads set status = ? where username = ? and fileNameOutputInServer = ?";
		try (Connection connection = Utils.getConnection()) {
			if (connection == null) {
				System.err.println("Failed to establish database connection");
				return;
			}
			try (PreparedStatement pst = connection.prepareStatement(sql)) {
				pst.setString(1, status);
				pst.setString(2, username);
				// Case-insensitive replacement of .pdf with .docx
				pst.setString(3, fileNameInServer.replaceAll(Utils.PDF_EXTENSION_REGEX, Utils.DOCX_EXTENSION));
				int updated = pst.executeUpdate();
				System.out.println("Updated " + updated + " records with status: " + status);
			}
		} catch (Exception e) {
			System.err.println("updateStatus: " + e.getMessage());
		}
	}
}

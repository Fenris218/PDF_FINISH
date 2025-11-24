package model.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import model.BEAN.User;
import utils.Utils;

public class LoginDAO {
	public User checkLogin(String username, String password) {
		User user = null;
		String query = "select username from users where username = ? and password = ?";
		try (Connection connection = Utils.getConnection()) {
			if (connection == null) {
				System.err.println("Failed to establish database connection");
				return null;
			}
			try (PreparedStatement pst = connection.prepareStatement(query)) {
				pst.setString(1, username);
				pst.setString(2, password);
				try (ResultSet rs = pst.executeQuery()) {
					if (rs.next()) {
						user = new User();
						user.setUsername(rs.getString("username"));
					}
				}
			}
		} catch (Exception e) {
			System.err.println("checkLogin: " + e.getMessage());
		}
		return user;
	}

	public boolean createAccount(String username, String password) {
		String query = "insert into users values(?,?)";
		try (Connection connection = Utils.getConnection()) {
			if (connection == null) {
				System.err.println("Failed to establish database connection");
				return false;
			}
			try (PreparedStatement pst = connection.prepareStatement(query)) {
				pst.setString(1, username);
				pst.setString(2, password);
				return pst.executeUpdate() > 0;
			}
		} catch (Exception e) {
			System.err.println("createAccount: " + e.getMessage());
		}
		return false;
	}
}

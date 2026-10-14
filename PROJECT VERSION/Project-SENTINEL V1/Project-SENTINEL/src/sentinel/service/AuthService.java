package sentinel.service;

import sentinel.database.Database;
import sentinel.database.DatabaseConnection;
import sentinel.model.Admin;
import sentinel.model.Employee;
import sentinel.model.Technician;
import sentinel.model.User;
import sentinel.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Inloggen en wachtwoord wijzigen.
 */
public class AuthService {

    /** Inloggen met gebruikersnaam of e-mailadres. */
    public LoginResult login(String loginName, String password) throws SQLException {

        String sql = "SELECT u.user_id, u.username, "
                + "u.password_hash, u.full_name, "
                + "u.active, u.first_login_completed, "
                + "r.role_name "
                + "FROM users u "
                + "JOIN roles r ON u.role_id = r.role_id "
                + "WHERE u.username = ? OR u.email = ? "
                + "LIMIT 1";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, loginName);
            statement.setString(2, loginName);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return new LoginResult(LoginStatus.USER_NOT_FOUND);
                }

                if (!resultSet.getBoolean("active")) {
                    return new LoginResult(LoginStatus.ACCOUNT_INACTIVE);
                }

                if (!PasswordUtil.verifyPassword(password, resultSet.getString("password_hash"))) {
                    return new LoginResult(LoginStatus.WRONG_PASSWORD);
                }

                User user = createUser(
                        resultSet.getString("role_name"),
                        resultSet.getInt("user_id"),
                        resultSet.getString("full_name"),
                        resultSet.getBoolean("first_login_completed"));

                if (user == null) {
                    return new LoginResult(LoginStatus.UNKNOWN_ROLE);
                }

                return new LoginResult(LoginStatus.SUCCESS, user);
            }
        }
    }

    /** Slaat een nieuw wachtwoord op en markeert de eerste login als voltooid. */
    public void changePassword(int userId, String newPassword) throws SQLException {

        String sql = "UPDATE users "
                + "SET password_hash = ?, "
                + "first_login_completed = true "
                + "WHERE user_id = ?";

        Database.update(sql, PasswordUtil.hashPassword(newPassword), userId);
    }

    /**
     * De enige plek waar naar de rol als tekst wordt gekeken. Daarna werkt de
     * applicatie met het object zelf (polymorphism), zonder switch op de rol.
     */
    private User createUser(String role, int userId, String fullName, boolean firstLoginCompleted) {

        return switch (role.toUpperCase()) {
            case "ADMIN" -> new Admin(userId, fullName, firstLoginCompleted);
            case "TECHNICIAN" -> new Technician(userId, fullName, firstLoginCompleted);
            case "EMPLOYEE" -> new Employee(userId, fullName, firstLoginCompleted);
            default -> null;
        };
    }

    public enum LoginStatus {
        SUCCESS,
        USER_NOT_FOUND,
        WRONG_PASSWORD,
        ACCOUNT_INACTIVE,
        UNKNOWN_ROLE
    }

    /** Resultaat van een inlogpoging. De gebruiker is alleen gevuld bij SUCCESS. */
    public static class LoginResult {

        private final LoginStatus status;
        private final User user;

        public LoginResult(LoginStatus status) {
            this(status, null);
        }

        public LoginResult(LoginStatus status, User user) {
            this.status = status;
            this.user = user;
        }

        public LoginStatus getStatus() {
            return status;
        }

        public User getUser() {
            return user;
        }
    }
}

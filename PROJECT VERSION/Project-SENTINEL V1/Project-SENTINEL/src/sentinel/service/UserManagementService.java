package sentinel.service;

import sentinel.database.Database;
import sentinel.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Gebruikersbeheer voor de admin.
 */
public class UserManagementService {

    public void showUsers() throws SQLException {

        String sql = "SELECT u.user_id, u.username, u.full_name, "
                + "u.email, u.active, r.role_name "
                + "FROM users u "
                + "JOIN roles r ON u.role_id = r.role_id "
                + "ORDER BY u.user_id";

        List<String> lines = Database.queryList(sql, row -> {

            String active = row.getBoolean("active") ? "YES" : "NO";

            return row.getInt("user_id") + " | "
                    + row.getString("username") + " | "
                    + row.getString("full_name") + " | "
                    + row.getString("role_name") + " | "
                    + active;
        });

        System.out.println("ID | USERNAME | NAME | ROLE | ACTIVE");
        System.out.println("---------------------------------------------------------");

        for (String line : lines) {
            System.out.println(line);
        }
    }

    public boolean userExists(int userId) throws SQLException {

        String sql = "SELECT user_id "
                + "FROM users "
                + "WHERE user_id = ?";

        return Database.exists(sql, userId);
    }

    /** Toont de huidige gegevens van één gebruiker, bovenaan het wijzigscherm. */
    public void showUser(int userId) throws SQLException {

        String sql = "SELECT u.user_id, u.username, u.full_name, "
                + "u.email, u.active, r.role_name "
                + "FROM users u "
                + "JOIN roles r ON u.role_id = r.role_id "
                + "WHERE u.user_id = ?";

        // Generics: hier is T een String[] met de regels van het scherm.
        Optional<String[]> user = Database.queryFirst(sql, row -> new String[] {
                "USER #" + row.getInt("user_id"),
                "------------------------------------",
                "Username : " + row.getString("username"),
                "Naam     : " + row.getString("full_name"),
                "Email    : " + row.getString("email"),
                "Role     : " + row.getString("role_name"),
                "Actief   : " + (row.getBoolean("active") ? "YES" : "NO")
        }, userId);

        if (user.isEmpty()) {
            System.out.println("User niet gevonden.");
            return;
        }

        for (String line : user.get()) {
            System.out.println(line);
        }
    }

    /** Nieuwe gebruiker: actief, en moet bij de eerste login een eigen wachtwoord kiezen. */
    public void createUser(int roleId, String username, String email, String name, String password)
            throws SQLException {

        String sql = "INSERT INTO users "
                + "(role_id, username, email, password_hash, "
                + "full_name, active, first_login_completed, created_at) "
                + "VALUES (?, ?, ?, ?, ?, true, false, NOW())";

        Database.update(sql, roleId, username, email, PasswordUtil.hashPassword(password), name);
    }

    /** field: 1 = username, 2 = full_name, 3 = email. Een ander nummer doet niets. */
    public void updateUser(int userId, int field, String value) throws SQLException {

        String column = switch (field) {
            case 1 -> "username";
            case 2 -> "full_name";
            case 3 -> "email";
            default -> null;
        };

        if (column == null) {
            return;
        }

        Database.update("UPDATE users SET " + column + " = ? WHERE user_id = ?", value, userId);
    }

    public void updateRole(int userId, int roleId) throws SQLException {

        String sql = "UPDATE users "
                + "SET role_id = ? "
                + "WHERE user_id = ?";

        Database.update(sql, roleId, userId);
    }

    /** Tijdelijk wachtwoord: de gebruiker moet bij de volgende login een nieuw kiezen. */
    public void resetPassword(int userId, String password) throws SQLException {

        String sql = "UPDATE users "
                + "SET password_hash = ?, "
                + "first_login_completed = false "
                + "WHERE user_id = ?";

        Database.update(sql, PasswordUtil.hashPassword(password), userId);
    }

    public void setActive(int userId, boolean active) throws SQLException {

        String sql = "UPDATE users "
                + "SET active = ? "
                + "WHERE user_id = ?";

        Database.update(sql, active, userId);
    }
}

package sentinel.service;

import sentinel.database.Database;
import sentinel.database.DatabaseConnection;
import sentinel.model.DeviceInfo;
import sentinel.model.DiagnosticOutcome;
import sentinel.model.DiagnosticView;
import sentinel.model.TicketDetails;
import sentinel.model.TicketSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/**
 * Alles wat een employee doet met eigen devices en tickets.
 */
public class EmployeeService {

    /** True als de gebruiker actief is en de rol EMPLOYEE heeft. */
    public boolean isEmployee(int userId) throws SQLException {

        String sql = "SELECT r.role_name "
                + "FROM users u "
                + "JOIN roles r ON u.role_id = r.role_id "
                + "WHERE u.user_id = ? AND u.active = 1";

        Optional<String> role = Database.queryFirst(sql, row -> row.getString("role_name"), userId);

        return role.isPresent() && "EMPLOYEE".equalsIgnoreCase(role.get());
    }

    public String getEmployeeName(int userId) throws SQLException {

        String sql = "SELECT full_name "
                + "FROM users "
                + "WHERE user_id = ?";

        return Database.queryFirst(sql, row -> row.getString("full_name"), userId).orElse("Employee");
    }

    public List<DeviceInfo> getAssignedDevices(int userId) throws SQLException {

        String sql = "SELECT device_id, device_name, device_type, "
                + "brand, model, operating_system, serial_number, status "
                + "FROM devices "
                + "WHERE assigned_user_id = ? "
                + "ORDER BY device_name";

        // Generics: queryList geeft hier een List<DeviceInfo> terug.
        return Database.queryList(sql, row -> new DeviceInfo(
                row.getInt("device_id"),
                row.getString("device_name"),
                row.getString("device_type"),
                row.getString("brand"),
                row.getString("model"),
                row.getString("operating_system"),
                row.getString("serial_number"),
                row.getString("status")), userId);
    }

    /** Maakt een OPEN ticket en geeft het ticketnummer terug. deviceId mag null zijn. */
    public int createTicket(int userId, Integer deviceId, String title, String description,
                            String category, String priority) throws SQLException {

        String sql = "INSERT INTO tickets "
                + "(user_id, device_id, assigned_to, title, description, "
                + "category, priority, status, resolution, created_at, closed_at) "
                + "VALUES (?, ?, NULL, ?, ?, ?, ?, 'OPEN', NULL, NOW(), NULL)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, userId);

            if (deviceId == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, deviceId);
            }

            statement.setString(3, title);
            statement.setString(4, description);
            statement.setString(5, category);
            statement.setString(6, priority);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int ticketId = keys.getInt(1);
                    logAction(connection, userId, "TICKET_CREATED", "Ticket #" + ticketId + " aangemaakt.");
                    return ticketId;
                }
            }
        }

        throw new SQLException("Ticket werd opgeslagen maar het ticketnummer kon niet worden gelezen.");
    }

    public void saveDiagnosticResult(int ticketId, DiagnosticOutcome outcome) throws SQLException {

        String sql = "INSERT INTO diagnostic_results "
                + "(ticket_id, diagnostic_type, result, details, "
                + "recommendation, performed_at) "
                + "VALUES (?, ?, ?, ?, ?, NOW())";

        // Melding en details worden samen in de kolom details opgeslagen.
        String details = outcome.getMessage() + " | " + outcome.getDetails();

        Database.update(sql, ticketId, outcome.getType(), outcome.getStatus(), details, outcome.getRecommendation());
    }

    public void sendToTechnician(int ticketId, int userId) throws SQLException {

        String sql = "UPDATE tickets "
                + "SET status = 'TECHNICIAN_REQUIRED', "
                + "resolution = NULL, closed_at = NULL "
                + "WHERE ticket_id = ? AND user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, ticketId);
            statement.setInt(2, userId);
            statement.executeUpdate();

            logAction(connection, userId, "TECHNICIAN_REQUIRED", "Ticket #" + ticketId + " wacht op een technician.");
        }
    }

    public void resolveAutomatically(int ticketId, int userId, String resolution) throws SQLException {

        String sql = "UPDATE tickets "
                + "SET status = 'RESOLVED', "
                + "resolution = ?, closed_at = NOW() "
                + "WHERE ticket_id = ? AND user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, resolution);
            statement.setInt(2, ticketId);
            statement.setInt(3, userId);
            statement.executeUpdate();

            logAction(connection, userId, "AUTO_RESOLVED", "Ticket #" + ticketId + " automatisch afgerond.");
        }
    }

    public List<TicketSummary> getMyTickets(int userId) throws SQLException {

        String sql = "SELECT t.ticket_id, t.title, t.category, t.priority, "
                + "t.status, t.created_at, t.resolution, "
                + "d.device_name, "
                + "tech.full_name AS technician_name "
                + "FROM tickets t "
                + "LEFT JOIN devices d ON t.device_id = d.device_id "
                + "LEFT JOIN users tech ON t.assigned_to = tech.user_id "
                + "WHERE t.user_id = ? "
                + "ORDER BY t.created_at DESC";

        // Generics: dezelfde queryList, nu met T = TicketSummary.
        return Database.queryList(sql, row -> new TicketSummary(
                row.getInt("ticket_id"),
                row.getString("title"),
                row.getString("category"),
                row.getString("priority"),
                row.getString("status"),
                row.getTimestamp("created_at"),
                row.getString("device_name"),
                row.getString("technician_name"),
                row.getString("resolution")), userId);
    }

    /** Geeft null als het ticket niet bestaat of niet van deze employee is. */
    public TicketDetails getTicketDetails(int userId, int ticketId) throws SQLException {

        String ticketSql = "SELECT t.ticket_id, t.title, t.description, "
                + "t.category, t.priority, t.status, "
                + "t.created_at, t.closed_at, t.resolution, "
                + "d.device_name, "
                + "tech.full_name AS technician_name "
                + "FROM tickets t "
                + "LEFT JOIN devices d ON t.device_id = d.device_id "
                + "LEFT JOIN users tech ON t.assigned_to = tech.user_id "
                + "WHERE t.ticket_id = ? AND t.user_id = ?";

        Optional<TicketDetails> ticket = Database.queryFirst(ticketSql, row -> new TicketDetails(
                row.getInt("ticket_id"),
                row.getString("title"),
                row.getString("description"),
                row.getString("category"),
                row.getString("priority"),
                row.getString("status"),
                row.getTimestamp("created_at"),
                row.getTimestamp("closed_at"),
                row.getString("device_name"),
                row.getString("technician_name"),
                row.getString("resolution")), ticketId, userId);

        if (ticket.isEmpty()) {
            return null;
        }

        TicketDetails details = ticket.get();

        String diagnosticSql = "SELECT diagnostic_type, result, details, "
                + "recommendation, performed_at "
                + "FROM diagnostic_results "
                + "WHERE ticket_id = ? "
                + "ORDER BY performed_at";

        List<DiagnosticView> diagnostics = Database.queryList(diagnosticSql, row -> new DiagnosticView(
                row.getString("diagnostic_type"),
                row.getString("result"),
                row.getString("details"),
                row.getString("recommendation"),
                row.getTimestamp("performed_at")), details.getTicketId());

        for (DiagnosticView diagnostic : diagnostics) {
            details.addDiagnostic(diagnostic);
        }

        return details;
    }

    private void logAction(Connection connection, int userId, String action, String details) {

        String sql = "INSERT INTO activity_logs "
                + "(user_id, action, details, created_at) "
                + "VALUES (?, ?, ?, NOW())";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, action);
            statement.setString(3, details);
            statement.executeUpdate();

        } catch (SQLException ignored) {
            // Logging mag de hoofdactie niet laten mislukken.
        }
    }
}

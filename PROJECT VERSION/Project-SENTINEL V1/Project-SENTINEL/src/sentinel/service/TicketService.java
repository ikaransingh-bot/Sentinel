package sentinel.service;

import sentinel.database.Database;
import sentinel.database.DatabaseConnection;
import sentinel.model.DiagnosticView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Ticketbeheer voor technicians en admins.
 */
public class TicketService {

    /** Tickets zonder technician die nog wachten op behandeling. */
    public void showOpenTickets() throws SQLException {
        showTickets("WHERE t.assigned_to IS NULL "
                + "AND t.status IN "
                + "('OPEN','TECHNICIAN_REQUIRED')");
    }

    /** Onopgeloste tickets van deze technician. */
    public void showMyTickets(int userId) throws SQLException {
        showTickets("WHERE t.assigned_to = ? "
                + "AND t.status <> 'RESOLVED'", userId);
    }

    public void showAllTickets() throws SQLException {
        showTickets("");
    }

    /** Geeft false als het ticket niet (meer) beschikbaar is. */
    public boolean claimTicket(int ticketId, int userId) throws SQLException {

        String sql = "UPDATE tickets "
                + "SET assigned_to = ?, "
                + "status = 'IN_PROGRESS' "
                + "WHERE ticket_id = ? "
                + "AND assigned_to IS NULL "
                + "AND status IN "
                + "('OPEN','TECHNICIAN_REQUIRED')";

        return Database.update(sql, userId, ticketId) == 1;
    }

    /** Alleen de toegewezen technician kan een ticket oplossen. */
    public boolean resolveTicket(int ticketId, int userId, String resolution) throws SQLException {

        String sql = "UPDATE tickets "
                + "SET status = 'RESOLVED', "
                + "resolution = ?, "
                + "closed_at = NOW() "
                + "WHERE ticket_id = ? "
                + "AND assigned_to = ? "
                + "AND status <> 'RESOLVED'";

        return Database.update(sql, resolution, ticketId, userId) == 1;
    }

    public void showTicketDetails(int ticketId) throws SQLException {

        String sql = "SELECT t.*, "
                + "u.username AS reporter, "
                + "a.username AS technician, "
                + "d.device_name, d.device_type, "
                + "d.brand, d.model, d.operating_system, "
                + "d.serial_number "
                + "FROM tickets t "
                + "JOIN users u ON t.user_id = u.user_id "
                + "LEFT JOIN users a "
                + "ON t.assigned_to = a.user_id "
                + "LEFT JOIN devices d "
                + "ON t.device_id = d.device_id "
                + "WHERE t.ticket_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("Ticket niet gevonden.");
                    return;
                }

                System.out.println("Ticket ID   : " + rs.getInt("ticket_id"));
                System.out.println("Reporter    : " + rs.getString("reporter"));
                System.out.println("Title       : " + rs.getString("title"));
                System.out.println("Description : " + rs.getString("description"));
                System.out.println("Category    : " + rs.getString("category"));
                System.out.println("Priority    : " + rs.getString("priority"));
                System.out.println("Status      : " + rs.getString("status"));

                String technician = rs.getString("technician");
                System.out.println("Technician  : " + (technician == null ? "UNASSIGNED" : technician));

                String device = rs.getString("device_name");

                System.out.println();
                System.out.println("DEVICE");

                if (device == null) {
                    System.out.println("Geen specifiek device.");
                } else {
                    System.out.println("Name   : " + device);
                    System.out.println("Type   : " + rs.getString("device_type"));
                    System.out.println("Brand  : " + rs.getString("brand"));
                    System.out.println("Model  : " + rs.getString("model"));
                    System.out.println("OS     : " + rs.getString("operating_system"));
                    System.out.println("Serial : " + rs.getString("serial_number"));
                }

                String resolution = rs.getString("resolution");

                if (resolution != null) {
                    System.out.println();
                    System.out.println("Resolution: " + resolution);
                }
            }
        }

        showDiagnostics(ticketId);
    }

    private void showDiagnostics(int ticketId) throws SQLException {

        String sql = "SELECT diagnostic_type, result, "
                + "details, recommendation, performed_at "
                + "FROM diagnostic_results "
                + "WHERE ticket_id = ? "
                + "ORDER BY performed_at";

        List<DiagnosticView> diagnostics = Database.queryList(sql, row -> new DiagnosticView(
                row.getString("diagnostic_type"),
                row.getString("result"),
                row.getString("details"),
                row.getString("recommendation"),
                row.getTimestamp("performed_at")), ticketId);

        System.out.println();
        System.out.println("DIAGNOSTIC RESULTS");
        System.out.println("------------------------------------");

        for (DiagnosticView diagnostic : diagnostics) {
            System.out.println(diagnostic.getType() + " | " + diagnostic.getResult());
            System.out.println("Details: " + diagnostic.getDetails());
            System.out.println("Advice : " + diagnostic.getRecommendation());
            System.out.println();
        }

        if (diagnostics.isEmpty()) {
            System.out.println("Geen diagnostic resultaten.");
        }
    }

    /** Toont een ticketlijst; where is een WHERE-deel met ?-parameters voor values. */
    private void showTickets(String where, Object... values) throws SQLException {

        String sql = "SELECT t.ticket_id, t.title, "
                + "t.category, t.priority, t.status, "
                + "u.username AS reporter, "
                + "a.username AS technician, "
                + "d.device_name "
                + "FROM tickets t "
                + "JOIN users u ON t.user_id = u.user_id "
                + "LEFT JOIN users a "
                + "ON t.assigned_to = a.user_id "
                + "LEFT JOIN devices d "
                + "ON t.device_id = d.device_id "
                + where
                + " ORDER BY t.created_at DESC";

        // Generics: hier is T = String; elke rij wordt één regel tekst.
        List<String> lines = Database.queryList(sql, this::formatTicketLine, values);

        System.out.println("ID | TITLE | CATEGORY | PRIORITY | STATUS | DEVICE | REPORTER");
        System.out.println("----------------------------------------------------------------------------");

        for (String line : lines) {
            System.out.println(line);
        }

        if (lines.isEmpty()) {
            System.out.println("Geen tickets gevonden.");
        }
    }

    private String formatTicketLine(ResultSet row) throws SQLException {

        String device = row.getString("device_name");

        if (device == null) {
            device = "NO DEVICE";
        }

        return row.getInt("ticket_id")
                + " | " + row.getString("title")
                + " | " + row.getString("category")
                + " | " + row.getString("priority")
                + " | " + row.getString("status")
                + " | " + device
                + " | " + row.getString("reporter");
    }
}

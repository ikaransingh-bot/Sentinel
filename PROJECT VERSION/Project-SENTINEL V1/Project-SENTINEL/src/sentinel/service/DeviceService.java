package sentinel.service;

import sentinel.database.Database;
import sentinel.model.DeviceInfo;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Devices registreren, wijzigen en toewijzen aan employees.
 */
public class DeviceService {

    public void showDevices() throws SQLException {

        String sql = "SELECT d.device_id, d.device_name, d.device_type, "
                + "d.brand, d.model, d.serial_number, d.status, u.full_name "
                + "FROM devices d "
                + "LEFT JOIN users u ON d.assigned_user_id = u.user_id "
                + "ORDER BY d.device_id";

        List<String> lines = Database.queryList(sql, row -> {

            String employee = row.getString("full_name");

            if (employee == null) {
                employee = "UNASSIGNED";
            }

            return row.getInt("device_id") + " | "
                    + row.getString("device_name") + " | "
                    + row.getString("device_type") + " | "
                    + valueOrDash(row.getString("brand")) + " | "
                    + valueOrDash(row.getString("model")) + " | "
                    + valueOrDash(row.getString("serial_number")) + " | "
                    + row.getString("status") + " | "
                    + employee;
        });

        System.out.println("ID | NAME | TYPE | BRAND | MODEL | SERIAL | STATUS | EMPLOYEE");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (String line : lines) {
            System.out.println(line);
        }
    }

    /** Toont alleen actieve gebruikers met de rol EMPLOYEE. */
    public void showEmployees() throws SQLException {

        String sql = "SELECT u.user_id, u.username, u.full_name "
                + "FROM users u "
                + "JOIN roles r ON u.role_id = r.role_id "
                + "WHERE r.role_name = 'EMPLOYEE' "
                + "AND u.active = true "
                + "ORDER BY u.full_name";

        List<String> lines = Database.queryList(sql, row ->
                row.getInt("user_id") + " | "
                + row.getString("username") + " | "
                + row.getString("full_name"));

        System.out.println("ID | USERNAME | NAME");
        System.out.println("--------------------------------------------");

        for (String line : lines) {
            System.out.println(line);
        }
    }

    /** Eén device met alle velden, voor het wijzigscherm. */
    public Optional<DeviceInfo> findDevice(int deviceId) throws SQLException {

        String sql = "SELECT device_id, device_name, device_type, "
                + "brand, model, operating_system, serial_number, status "
                + "FROM devices "
                + "WHERE device_id = ?";

        return Database.queryFirst(sql, row -> new DeviceInfo(
                row.getInt("device_id"),
                row.getString("device_name"),
                row.getString("device_type"),
                row.getString("brand"),
                row.getString("model"),
                row.getString("operating_system"),
                row.getString("serial_number"),
                row.getString("status")), deviceId);
    }

    public boolean deviceExists(int deviceId) throws SQLException {
        return Database.exists("SELECT device_id FROM devices WHERE device_id = ?", deviceId);
    }

    public boolean employeeExists(int userId) throws SQLException {

        String sql = "SELECT u.user_id "
                + "FROM users u "
                + "JOIN roles r ON u.role_id = r.role_id "
                + "WHERE u.user_id = ? "
                + "AND r.role_name = 'EMPLOYEE' "
                + "AND u.active = true";

        return Database.exists(sql, userId);
    }

    /** Nieuw device krijgt status ACTIVE. Lege optionele velden worden NULL. */
    public void createDevice(String name, String type, String brand, String model, String os, String serial)
            throws SQLException {

        String sql = "INSERT INTO devices "
                + "(device_name, device_type, brand, model, "
                + "operating_system, serial_number, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', NOW())";

        Database.update(sql, name, type, emptyToNull(brand), emptyToNull(model), emptyToNull(os), emptyToNull(serial));
    }

    /**
     * field: 1 = naam, 2 = type, 3 = brand, 4 = model, 5 = OS, 6 = serienummer.
     * Een ander nummer doet niets.
     */
    public void updateDevice(int deviceId, int field, String value) throws SQLException {

        String column = switch (field) {
            case 1 -> "device_name";
            case 2 -> "device_type";
            case 3 -> "brand";
            case 4 -> "model";
            case 5 -> "operating_system";
            case 6 -> "serial_number";
            default -> null;
        };

        if (column == null) {
            return;
        }

        Database.update("UPDATE devices SET " + column + " = ? WHERE device_id = ?", emptyToNull(value), deviceId);
    }

    public void assignDevice(int deviceId, int userId) throws SQLException {

        String sql = "UPDATE devices "
                + "SET assigned_user_id = ? "
                + "WHERE device_id = ?";

        Database.update(sql, userId, deviceId);
    }

    public void unassignDevice(int deviceId) throws SQLException {

        String sql = "UPDATE devices "
                + "SET assigned_user_id = NULL "
                + "WHERE device_id = ?";

        Database.update(sql, deviceId);
    }

    public void setStatus(int deviceId, String status) throws SQLException {

        String sql = "UPDATE devices "
                + "SET status = ? "
                + "WHERE device_id = ?";

        Database.update(sql, status, deviceId);
    }

    /** Lege velden als "-" tonen in plaats van "null". */
    private String valueOrDash(String value) {

        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }

    private String emptyToNull(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}

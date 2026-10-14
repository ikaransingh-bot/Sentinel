package sentinel.menu;

import sentinel.model.DeviceInfo;
import sentinel.service.DeviceService;
import sentinel.util.ConsoleUtil;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Devices beheren: registreren, wijzigen, toewijzen en status wijzigen.
 * Bij elke vraag kan de gebruiker met 0 terug.
 *
 * Package-private: alleen AdminMenu en TechnicianMenu in deze package openen dit menu.
 */
class DeviceMenu extends Menu {

    private static final String[] OPTIONS = {
            "1. Devices bekijken",
            "2. Device registreren",
            "3. Device wijzigen",
            "4. Device assignen",
            "5. Device unassignen",
            "6. Device status wijzigen",
            "0. Terug"
    };

    private static final String[] FIELD_OPTIONS = {
            "1. Device naam",
            "2. Device type",
            "3. Brand",
            "4. Model",
            "5. Operating system",
            "6. Serial number"
    };

    private static final String[] STATUS_OPTIONS = {
            "1. ACTIVE",
            "2. INACTIVE",
            "3. REPAIR"
    };

    private final DeviceService deviceService = new DeviceService();

    @Override
    protected void printScreen() {
        printHeader("       DEVICES BEHEREN");
        printOptions(OPTIONS);
    }

    @Override
    protected boolean handleChoice(String choice) throws SQLException {

        switch (choice) {
            case "1" -> showDevices();
            case "2" -> createDevice();
            case "3" -> editDevice();
            case "4" -> assignDevice();
            case "5" -> unassignDevice();
            case "6" -> changeStatus();
            case "0" -> {
                return false;
            }
            default -> ConsoleUtil.message("Ongeldige keuze.");
        }

        return true;
    }

    private void showDevices() throws SQLException {
        ConsoleUtil.clearScreen();
        deviceService.showDevices();
        System.out.println();
        ConsoleUtil.pause();
    }

    private void createDevice() throws SQLException {

        ConsoleUtil.clearScreen();
        System.out.println(BACK_HINT);
        System.out.println();

        String name = readOrBack("Device naam: ");

        if (name == null) {
            return;
        }

        String type = readOrBack("Type: ");

        if (type == null) {
            return;
        }

        if (name.isBlank() || type.isBlank()) {
            ConsoleUtil.message("Naam en type zijn verplicht.");
            return;
        }

        String brand = readOrBack("Brand: ");

        if (brand == null) {
            return;
        }

        String model = readOrBack("Model: ");

        if (model == null) {
            return;
        }

        String os = readOrBack("Operating system: ");

        if (os == null) {
            return;
        }

        String serial = readOrBack("Serial number: ");

        if (serial == null) {
            return;
        }

        deviceService.createDevice(name, type.toUpperCase(), brand, model, os, serial);

        ConsoleUtil.message("Device geregistreerd.");
    }

    /** Kies een device en blijf op het wijzigscherm tot 0; na elke wijziging wordt het device opnieuw getoond. */
    private void editDevice() throws SQLException {

        int deviceId = chooseDevice();

        if (deviceId == 0) {
            return;
        }

        while (true) {
            ConsoleUtil.clearScreen();
            printDevice(deviceId);

            System.out.println();
            printOptions(FIELD_OPTIONS);
            System.out.println("0. Terug");

            int field = ConsoleUtil.readInt("Keuze: ");

            if (field == 0) {
                return;
            }

            if (field < 1 || field > FIELD_OPTIONS.length) {
                ConsoleUtil.message("Ongeldige keuze.");
                continue;
            }

            String value = readOrBack("Nieuwe waarde (0 = terug): ");

            if (value == null) {
                continue;
            }

            deviceService.updateDevice(deviceId, field, value);

            ConsoleUtil.message("Device gewijzigd.");
        }
    }

    private void assignDevice() throws SQLException {

        int deviceId = chooseDevice();

        if (deviceId == 0) {
            return;
        }

        ConsoleUtil.clearScreen();

        System.out.println("EMPLOYEES");
        System.out.println();
        deviceService.showEmployees();

        int employeeId = ConsoleUtil.readInt("\nEmployee ID (0 = terug): ");

        if (employeeId == 0) {
            return;
        }

        if (!deviceService.employeeExists(employeeId)) {
            ConsoleUtil.message("Employee bestaat niet of is inactive.");
            return;
        }

        deviceService.assignDevice(deviceId, employeeId);

        ConsoleUtil.message("Device toegewezen.");
    }

    private void unassignDevice() throws SQLException {

        int deviceId = chooseDevice();

        if (deviceId == 0) {
            return;
        }

        deviceService.unassignDevice(deviceId);

        ConsoleUtil.message("Assignment verwijderd.");
    }

    private void changeStatus() throws SQLException {

        int deviceId = chooseDevice();

        if (deviceId == 0) {
            return;
        }

        System.out.println();
        printOptions(STATUS_OPTIONS);
        System.out.println("0. Terug");

        String choice = ConsoleUtil.readLine("Keuze: ");

        if (choice.equals("0")) {
            return;
        }

        String status = switch (choice) {
            case "1" -> "ACTIVE";
            case "2" -> "INACTIVE";
            case "3" -> "REPAIR";
            default -> null;
        };

        if (status == null) {
            ConsoleUtil.message("Ongeldige status.");
            return;
        }

        deviceService.setStatus(deviceId, status);

        ConsoleUtil.message("Status gewijzigd.");
    }

    /**
     * Toont alle devices en vraagt een device ID.
     * Geeft 0 terug bij "terug" of als het device niet bestaat (na een melding).
     */
    private int chooseDevice() throws SQLException {

        ConsoleUtil.clearScreen();

        System.out.println("DEVICES");
        System.out.println();
        deviceService.showDevices();

        int deviceId = ConsoleUtil.readInt("\nDevice ID (0 = terug): ");

        if (deviceId == 0) {
            return 0;
        }

        if (!deviceService.deviceExists(deviceId)) {
            ConsoleUtil.message("Device bestaat niet.");
            return 0;
        }

        return deviceId;
    }

    private void printDevice(int deviceId) throws SQLException {

        Optional<DeviceInfo> found = deviceService.findDevice(deviceId);

        if (found.isEmpty()) {
            System.out.println("Device niet gevonden.");
            return;
        }

        DeviceInfo device = found.get();

        System.out.println("DEVICE #" + device.getDeviceId());
        System.out.println("------------------------------------");
        System.out.println("Naam   : " + valueOrDash(device.getDeviceName()));
        System.out.println("Type   : " + valueOrDash(device.getDeviceType()));
        System.out.println("Brand  : " + valueOrDash(device.getBrand()));
        System.out.println("Model  : " + valueOrDash(device.getModel()));
        System.out.println("OS     : " + valueOrDash(device.getOperatingSystem()));
        System.out.println("Serial : " + valueOrDash(device.getSerialNumber()));
        System.out.println("Status : " + valueOrDash(device.getStatus()));
    }

    private String valueOrDash(String value) {

        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }
}

package sentinel.menu;

import sentinel.model.Admin;
import sentinel.service.UserManagementService;
import sentinel.util.ConsoleUtil;

import java.sql.SQLException;

/**
 * Gebruikers bekijken, aanmaken en wijzigen (alleen admin).
 * Rolnummers: 1 = ADMIN, 2 = TECHNICIAN, 3 = EMPLOYEE.
 *
 * Package-private: alleen AdminMenu in deze package opent dit menu.
 */
class UserManagementMenu extends Menu {

    private static final String[] OPTIONS = {
            "1. Users bekijken",
            "2. User aanmaken",
            "3. User wijzigen",
            "0. Terug"
    };

    private static final String[] ROLE_OPTIONS = {
            "1. ADMIN",
            "2. TECHNICIAN",
            "3. EMPLOYEE"
    };

    private static final String[] EDIT_OPTIONS = {
            "1. Username wijzigen",
            "2. Naam wijzigen",
            "3. Email wijzigen",
            "4. Role wijzigen",
            "5. Password reset",
            "6. Account enable",
            "7. Account disable",
            "0. Terug"
    };

    private final Admin currentUser;

    private final UserManagementService service = new UserManagementService();

    UserManagementMenu(Admin currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    protected void printScreen() {
        printHeader("      USERS BEHEREN");
        printOptions(OPTIONS);
    }

    @Override
    protected boolean handleChoice(String choice) throws SQLException {

        switch (choice) {
            case "1" -> showUsers();
            case "2" -> createUser();
            case "3" -> editUser();
            case "0" -> {
                return false;
            }
            default -> ConsoleUtil.message("Ongeldige keuze.");
        }

        return true;
    }

    private void showUsers() throws SQLException {
        ConsoleUtil.clearScreen();
        service.showUsers();
        System.out.println();
        ConsoleUtil.pause();
    }

    private void createUser() throws SQLException {

        ConsoleUtil.clearScreen();
        System.out.println(BACK_HINT);
        System.out.println();

        printOptions(ROLE_OPTIONS);
        System.out.println("0. Terug");

        int role = ConsoleUtil.readInt("Role ID: ");

        if (role == 0) {
            return;
        }

        if (role < 1 || role > ROLE_OPTIONS.length) {
            ConsoleUtil.message("Ongeldige role.");
            return;
        }

        String username = readOrBack("Username: ");

        if (username == null) {
            return;
        }

        String name = readOrBack("Volledige naam: ");

        if (name == null) {
            return;
        }

        String email = readOrBack("Email: ");

        if (email == null) {
            return;
        }

        String password = readOrBack("Tijdelijk password: ");

        if (password == null) {
            return;
        }

        if (username.isBlank() || name.isBlank() || email.isBlank()) {
            ConsoleUtil.message("Velden mogen niet leeg zijn.");
            return;
        }

        if (password.length() < 6) {
            ConsoleUtil.message("Password minimaal 6 karakters.");
            return;
        }

        service.createUser(role, username, email, name, password);

        ConsoleUtil.message("User aangemaakt.");
    }

    /**
     * Wijzigscherm voor één gebruiker. Blijft open tot 0 en toont na elke wijziging
     * de actuele gegevens. Bij een databasefout wordt dit scherm verlaten en meldt
     * het gebruikersmenu "Database fout.".
     */
    private void editUser() throws SQLException {

        ConsoleUtil.clearScreen();
        service.showUsers();

        int userId = ConsoleUtil.readInt("\nUser ID (0 = terug): ");

        if (userId == 0) {
            return;
        }

        if (!service.userExists(userId)) {
            ConsoleUtil.message("User bestaat niet.");
            return;
        }

        boolean editing = true;

        while (editing) {
            ConsoleUtil.clearScreen();

            service.showUser(userId);
            System.out.println();

            printOptions(EDIT_OPTIONS);

            String choice = ConsoleUtil.readLine("Keuze: ");

            switch (choice) {
                case "1" -> changeField(userId, 1);
                case "2" -> changeField(userId, 2);
                case "3" -> changeField(userId, 3);
                case "4" -> changeRole(userId);
                case "5" -> resetPassword(userId);
                case "6" -> {
                    service.setActive(userId, true);
                    ConsoleUtil.message("Account enabled.");
                }
                case "7" -> disableUser(userId);
                case "0" -> editing = false;
                default -> ConsoleUtil.message("Ongeldige keuze.");
            }
        }
    }

    /** field: 1 = username, 2 = naam, 3 = email. */
    private void changeField(int userId, int field) throws SQLException {

        String value = readOrBack("Nieuwe waarde (0 = terug): ");

        if (value == null) {
            return;
        }

        if (value.isBlank()) {
            ConsoleUtil.message("Waarde mag niet leeg zijn.");
            return;
        }

        service.updateUser(userId, field, value);

        ConsoleUtil.message("User gewijzigd.");
    }

    private void changeRole(int userId) throws SQLException {

        if (userId == currentUser.getUserId()) {
            ConsoleUtil.message("Je kunt je eigen role niet wijzigen.");
            return;
        }

        printOptions(ROLE_OPTIONS);
        System.out.println("0. Terug");

        int role = ConsoleUtil.readInt("Nieuwe role: ");

        if (role == 0) {
            return;
        }

        if (role < 1 || role > ROLE_OPTIONS.length) {
            ConsoleUtil.message("Ongeldige role.");
            return;
        }

        service.updateRole(userId, role);

        ConsoleUtil.message("Role gewijzigd.");
    }

    private void resetPassword(int userId) throws SQLException {

        String password = readOrBack("Nieuw tijdelijk password (0 = terug): ");

        if (password == null) {
            return;
        }

        if (password.length() < 6) {
            ConsoleUtil.message("Password minimaal 6 karakters.");
            return;
        }

        service.resetPassword(userId, password);

        ConsoleUtil.message("Password gereset.");
    }

    private void disableUser(int userId) throws SQLException {

        if (userId == currentUser.getUserId()) {
            ConsoleUtil.message("Je kunt jezelf niet disablen.");
            return;
        }

        service.setActive(userId, false);

        ConsoleUtil.message("Account disabled.");
    }
}

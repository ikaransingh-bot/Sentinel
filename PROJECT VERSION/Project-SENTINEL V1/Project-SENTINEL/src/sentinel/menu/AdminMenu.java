package sentinel.menu;

import sentinel.model.Admin;
import sentinel.util.ConsoleUtil;

/**
 * Hoofdmenu voor de admin.
 */
public class AdminMenu extends Menu {

    private static final String[] OPTIONS = {
            "1. Tickets beheren",
            "2. Devices beheren",
            "3. Gebruikers beheren",
            "0. Uitloggen"
    };

    private final Admin admin;

    public AdminMenu(Admin admin) {
        this.admin = admin;
    }

    @Override
    protected void printScreen() {
        printHeader("          ADMIN MENU");
        System.out.println("Welkom, " + admin.getFullName());
        System.out.println();
        printOptions(OPTIONS);
    }

    @Override
    protected boolean handleChoice(String choice) {

        switch (choice) {
            case "1" -> new TicketMenu(admin).show();
            case "2" -> new DeviceMenu().show();
            case "3" -> new UserManagementMenu(admin).show();
            case "0" -> {
                return false;
            }
            default -> ConsoleUtil.message("Ongeldige keuze.");
        }

        return true;
    }
}

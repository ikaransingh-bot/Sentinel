package sentinel.menu;

import sentinel.model.Technician;
import sentinel.util.ConsoleUtil;

/**
 * Hoofdmenu voor de technician.
 */
public class TechnicianMenu extends Menu {

    private static final String[] OPTIONS = {
            "1. Tickets beheren",
            "2. Devices beheren",
            "0. Uitloggen"
    };

    private final Technician technician;

    public TechnicianMenu(Technician technician) {
        this.technician = technician;
    }

    @Override
    protected void printScreen() {
        printHeader("       TECHNICIAN MENU");
        System.out.println("Welkom, " + technician.getFullName());
        System.out.println();
        printOptions(OPTIONS);
    }

    @Override
    protected boolean handleChoice(String choice) {

        switch (choice) {
            case "1" -> new TicketMenu(technician).show();
            case "2" -> new DeviceMenu().show();
            case "0" -> {
                return false;
            }
            default -> ConsoleUtil.message("Ongeldige keuze.");
        }

        return true;
    }
}

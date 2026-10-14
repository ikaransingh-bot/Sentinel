package sentinel.menu;

import sentinel.util.ConsoleUtil;

/**
 * Startscherm: inloggen of afsluiten.
 */
public class MainMenu extends Menu {

    private static final String[] OPTIONS = {
            "1. Login",
            "2. Afsluiten"
    };

    @Override
    protected void printScreen() {
        System.out.println("====================================");
        System.out.println("          PROJECT SENTINEL");
        System.out.println("     IT Helpdesk & Diagnose");
        System.out.println("====================================");
        System.out.println();
        printOptions(OPTIONS);
        System.out.println("------------------------------------");
    }

    @Override
    protected String readChoice() {
        return ConsoleUtil.readLine("Maak een keuze: ");
    }

    @Override
    protected boolean handleChoice(String choice) {

        switch (choice) {
            case "1" -> new LoginMenu().show();
            case "2" -> {
                return false;
            }
            default -> ConsoleUtil.message("Ongeldige keuze.");
        }

        return true;
    }
}

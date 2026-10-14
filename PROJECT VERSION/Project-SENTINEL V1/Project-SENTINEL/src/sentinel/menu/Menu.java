package sentinel.menu;

import sentinel.util.ConsoleUtil;

import java.sql.SQLException;

/**
 * Basis voor elk menu in SENTINEL.
 *
 * Abstraction: Menu legt de vaste werking vast (scherm tonen, keuze lezen,
 * keuze afhandelen), maar niet WAT er op het scherm staat. Dat bepaalt elke
 * subclass met printScreen() en handleChoice().
 * Inheritance: AdminMenu, TicketMenu, EmployeeMenu, ... erven de lus in show(),
 * zodat die niet in elk menu opnieuw staat.
 * Polymorphism: show() roept printScreen() en handleChoice() aan; Java kiest
 * tijdens het runnen de versie van het echte menu.
 */
public abstract class Menu {

    protected static final String BACK_HINT = "Typ 0 om terug te gaan.";

    /** De menulus. Blijft draaien tot handleChoice() false teruggeeft. */
    public void show() {

        boolean running = true;

        while (running) {
            ConsoleUtil.clearScreen();
            printScreen();

            String choice = readChoice();

            try {
                running = handleChoice(choice);

            } catch (SQLException e) {
                ConsoleUtil.message("Database fout.");
            }
        }
    }

    /** Toont de kop en de opties van dit menu. */
    protected abstract void printScreen();

    /** Voert de gekozen optie uit. Geeft false terug om het menu te verlaten. */
    protected abstract boolean handleChoice(String choice) throws SQLException;

    /** Standaard: de keuze als tekst. Een subclass kan dit overschrijven. */
    protected String readChoice() {
        return ConsoleUtil.readLine("Keuze: ");
    }

    /** Kop van de beheermenu's. */
    protected void printHeader(String title) {
        System.out.println("==============================");
        System.out.println(title);
        System.out.println("==============================");
    }

    /** Array + loop: elke optie op een eigen regel. */
    protected void printOptions(String[] options) {
        for (String option : options) {
            System.out.println(option);
        }
    }

    /** Terug-optie: geeft null terug als de gebruiker 0 invoert, anders de ingevoerde tekst. */
    protected String readOrBack(String message) {

        String input = ConsoleUtil.readLine(message);

        if (input.equals("0")) {
            return null;
        }

        return input;
    }
}

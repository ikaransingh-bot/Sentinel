package sentinel.util;

import java.util.Scanner;

/**
 * Hulpmethodes voor invoer en uitvoer in de console.
 */
public final class ConsoleUtil {

    private static final Scanner scanner = new Scanner(System.in);

    private ConsoleUtil() {
    }

    /**
     * Toont een vraag en geeft de ingevoerde regel terug (zonder spaties aan de randen).
     * Als de vraag "password" bevat, wordt de invoer verborgen ingelezen.
     */
    public static String readLine(String message) {

        if (message != null && message.toLowerCase().contains("password")) {
            return PasswordReader.readPassword(message);
        }

        System.out.print(message);
        return scanner.nextLine().trim();
    }

    /** Blijft vragen tot er een geldig geheel getal is ingevoerd. */
    public static int readInt(String message) {

        while (true) {
            try {
                return Integer.parseInt(readLine(message));

            } catch (NumberFormatException e) {
                System.out.println("Voer een geldig nummer in.");
            }
        }
    }

    /** Toont een melding op een leeg scherm en wacht op ENTER. */
    public static void message(String message) {
        clearScreen();
        System.out.println(message);
        System.out.println();
        pause();
    }

    public static void pause() {
        System.out.print("Druk op ENTER om verder te gaan...");
        scanner.nextLine();
    }

    public static void clearScreen() {
        // ANSI-codes: cursor naar linksboven, scherm leegmaken en de scrollgeschiedenis wissen,
        // zodat je bij omhoog scrollen geen eerdere schermen meer ziet.
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
    }

    public static void close() {
        scanner.close();
    }
}

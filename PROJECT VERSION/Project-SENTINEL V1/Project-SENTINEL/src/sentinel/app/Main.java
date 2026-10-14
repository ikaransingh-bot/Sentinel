package sentinel.app;

import sentinel.database.DatabaseConnection;
import sentinel.menu.MainMenu;
import sentinel.menu.Menu;
import sentinel.util.ConsoleUtil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Startpunt van Project SENTINEL.
 * Wordt gestart door scripts/bootstrap.ps1 nadat Java en MySQL zijn voorbereid
 * en scripts/build.bat de code met javac heeft gecompileerd.
 */
public class Main {

    public static void main(String[] args) {

        // Zonder werkende database heeft de applicatie geen zin.
        if (!testDatabaseConnection()) {
            ConsoleUtil.close();
            return;
        }

        // Polymorphism: de variabele is een Menu, het object is een MainMenu.
        Menu menu = new MainMenu();
        menu.show();

        ConsoleUtil.clearScreen();
        System.out.println("Project SENTINEL wordt afgesloten.");
        ConsoleUtil.close();
    }

    private static boolean testDatabaseConnection() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return connection != null && !connection.isClosed();

        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] Databaseverbinding mislukt.");
            System.out.println("Reden: " + e.getMessage());
            return false;
        }
    }
}

package sentinel.menu;

import sentinel.model.User;
import sentinel.service.AuthService;
import sentinel.util.ConsoleUtil;

import java.sql.SQLException;

/**
 * Inlogscherm. Na een geslaagde login opent het menu van de rol;
 * na uitloggen verschijnt het inlogscherm opnieuw.
 *
 * Package-private (geen public): alleen MainMenu in deze package opent het inlogscherm.
 */
class LoginMenu extends Menu {

    private final AuthService authService = new AuthService();

    @Override
    protected void printScreen() {
        System.out.println("====================================");
        System.out.println("               LOGIN");
        System.out.println("====================================");
        System.out.println();
        System.out.println("Voer 0 in om terug te gaan.");
        System.out.println();
    }

    /** Op dit scherm is de "keuze" de gebruikersnaam. */
    @Override
    protected String readChoice() {
        return ConsoleUtil.readLine("Username of email: ");
    }

    @Override
    protected boolean handleChoice(String username) {

        if (username.equals("0")) {
            return false;
        }

        // Een echt password heeft minimaal 6 tekens, dus 0 kan veilig "terug" betekenen.
        String password = readOrBack("Password (0 = terug): ");

        if (password == null) {
            return true;
        }

        try {
            AuthService.LoginResult result = authService.login(username, password);

            switch (result.getStatus()) {
                case USER_NOT_FOUND -> ConsoleUtil.message("User not found.\nPlease contact your admin.");
                case WRONG_PASSWORD -> ConsoleUtil.message("Incorrect password.");
                case ACCOUNT_INACTIVE -> ConsoleUtil.message("Account is disabled.\nPlease contact your admin.");
                case UNKNOWN_ROLE -> ConsoleUtil.message("Onbekende user role.");
                case SUCCESS -> openMainMenu(result.getUser());
            }

        } catch (SQLException e) {
            ConsoleUtil.message("Database error:\n" + e.getMessage());
        }

        return true;
    }

    private void openMainMenu(User user) throws SQLException {

        // Zonder nieuw password geen toegang: bij 0 gaat de gebruiker terug naar het inlogscherm.
        if (!user.isFirstLoginCompleted() && !changeFirstPassword(user)) {
            return;
        }

        // Polymorphism: LoginMenu weet niet of dit een Admin, Technician of Employee is.
        // Java roept de createMainMenu() van het echte object aan.
        Menu menu = user.createMainMenu();
        menu.show();
    }

    /**
     * Nieuwe of gereset accounts moeten eerst zelf een wachtwoord kiezen.
     * Geeft false terug als de gebruiker met 0 terug gaat.
     */
    private boolean changeFirstPassword(User user) throws SQLException {

        boolean passwordChanged = false;

        while (!passwordChanged) {
            ConsoleUtil.clearScreen();

            System.out.println("FIRST LOGIN");
            System.out.println("------------------------------------");
            System.out.println("Maak eerst een nieuw password.");
            System.out.println(BACK_HINT);
            System.out.println();

            String password = readOrBack("Nieuw password: ");

            if (password == null) {
                return false;
            }

            String confirm = ConsoleUtil.readLine("Bevestig password: ");

            if (password.length() < 6) {
                ConsoleUtil.message("Password moet minimaal 6 karakters hebben.");
                continue;
            }

            if (!password.equals(confirm)) {
                ConsoleUtil.message("Passwords komen niet overeen.");
                continue;
            }

            authService.changePassword(user.getUserId(), password);
            passwordChanged = true;

            ConsoleUtil.message("Password succesvol gewijzigd.");
        }

        return true;
    }
}

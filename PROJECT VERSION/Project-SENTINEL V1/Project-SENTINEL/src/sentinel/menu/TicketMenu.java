package sentinel.menu;

import sentinel.model.User;
import sentinel.service.TicketService;
import sentinel.util.ConsoleUtil;

import java.sql.SQLException;

/**
 * Tickets oppakken, bekijken en oplossen (technician en admin).
 *
 * Package-private: alleen AdminMenu en TechnicianMenu in deze package openen dit menu.
 */
class TicketMenu extends Menu {

    private static final String[] OPTIONS = {
            "1. Open tickets",
            "2. Ticket oppakken",
            "3. Mijn actieve tickets",
            "4. Ticket details",
            "5. Ticket oplossen"
    };

    private final User user;

    private final TicketService ticketService = new TicketService();

    TicketMenu(User user) {
        this.user = user;
    }

    @Override
    protected void printScreen() {

        printHeader("          TICKETS");
        printOptions(OPTIONS);

        // Polymorphism: Admin geeft hier true terug, Technician false.
        if (user.canViewAllTickets()) {
            System.out.println("6. Alle tickets");
        }

        System.out.println("0. Terug");
    }

    @Override
    protected boolean handleChoice(String choice) throws SQLException {

        switch (choice) {
            case "1" -> showOpenTickets();
            case "2" -> claimTicket();
            case "3" -> showMyTickets();
            case "4" -> showDetails();
            case "5" -> resolveTicket();
            case "6" -> {
                if (user.canViewAllTickets()) {
                    showAllTickets();
                } else {
                    ConsoleUtil.message("Ongeldige keuze.");
                }
            }
            case "0" -> {
                return false;
            }
            default -> ConsoleUtil.message("Ongeldige keuze.");
        }

        return true;
    }

    private void showOpenTickets() throws SQLException {
        ConsoleUtil.clearScreen();
        ticketService.showOpenTickets();
        System.out.println();
        ConsoleUtil.pause();
    }

    private void showMyTickets() throws SQLException {
        ConsoleUtil.clearScreen();
        ticketService.showMyTickets(user.getUserId());
        System.out.println();
        ConsoleUtil.pause();
    }

    private void showAllTickets() throws SQLException {
        ConsoleUtil.clearScreen();
        ticketService.showAllTickets();
        System.out.println();
        ConsoleUtil.pause();
    }

    private void claimTicket() throws SQLException {

        ConsoleUtil.clearScreen();
        ticketService.showOpenTickets();

        int ticketId = ConsoleUtil.readInt("\nTicket ID (0 = terug): ");

        if (ticketId == 0) {
            return;
        }

        if (ticketService.claimTicket(ticketId, user.getUserId())) {
            ConsoleUtil.message("Ticket opgepakt.");
        } else {
            ConsoleUtil.message("Ticket kan niet worden opgepakt.");
        }
    }

    /** Lijst met alle tickets -> details van het gekozen ticket -> terug naar de lijst, tot 0. */
    private void showDetails() throws SQLException {

        while (true) {
            ConsoleUtil.clearScreen();
            ticketService.showAllTickets();

            int ticketId = ConsoleUtil.readInt("\nTicket ID (0 = terug): ");

            if (ticketId == 0) {
                return;
            }

            ConsoleUtil.clearScreen();
            ticketService.showTicketDetails(ticketId);
            System.out.println();
            ConsoleUtil.pause();
        }
    }

    private void resolveTicket() throws SQLException {

        ConsoleUtil.clearScreen();
        ticketService.showMyTickets(user.getUserId());

        int ticketId = ConsoleUtil.readInt("\nTicket ID (0 = terug): ");

        if (ticketId == 0) {
            return;
        }

        String solution = readOrBack("Oplossing (0 = terug): ");

        if (solution == null) {
            return;
        }

        if (solution.isBlank()) {
            ConsoleUtil.message("Oplossing mag niet leeg zijn.");
            return;
        }

        if (ticketService.resolveTicket(ticketId, user.getUserId(), solution)) {
            ConsoleUtil.message("Ticket opgelost.");
        } else {
            ConsoleUtil.message("Ticket kan niet worden opgelost.");
        }
    }
}

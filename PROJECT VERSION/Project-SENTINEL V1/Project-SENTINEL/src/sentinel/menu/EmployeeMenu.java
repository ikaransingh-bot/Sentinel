package sentinel.menu;

import sentinel.model.DeviceInfo;
import sentinel.model.DiagnosticOutcome;
import sentinel.model.DiagnosticView;
import sentinel.model.Employee;
import sentinel.model.TicketDetails;
import sentinel.model.TicketSummary;
import sentinel.service.EmployeeDiagnosticService;
import sentinel.service.EmployeeService;
import sentinel.util.ConsoleUtil;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Menu voor de employee: probleem melden (met automatische diagnose),
 * eigen tickets volgen en eigen devices bekijken.
 */
public class EmployeeMenu extends Menu {

    private static final String[] OPTIONS = {
            "1. Probleem melden",
            "2. Mijn tickets bekijken",
            "3. Ticket details bekijken",
            "4. Volledige controle",
            "5. Mijn devices bekijken",
            "0. Uitloggen"
    };

    // Kolommen van de ticketlijst: ticket, status, categorie, datum, titel.
    private static final String TICKET_ROW = "%-8s%-21s%-16s%-18s%s";

    // Kolommen van de diagnoseregels: type, resultaat, details.
    private static final String DIAGNOSTIC_ROW = "%-10s%-21s%s";

    // Uitkomsten van chooseDevice() naast een echt device id (device ids zijn altijd groter dan 0).
    private static final int BACK = -1;
    private static final int NO_DEVICE = 0;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final int userId;
    private final EmployeeService employeeService;
    private final EmployeeDiagnosticService diagnosticService;

    // categoryNames[i] wordt aan de employee getoond, categoryCodes[i] wordt opgeslagen.
    private final String[] categoryNames = {
            "Internet of netwerk",
            "Printer",
            "Computer is traag",
            "Opslag",
            "Windows updates",
            "Security",
            "Office of licentie",
            "Ander probleem"
    };

    private final String[] categoryCodes = {
            "NETWORK",
            "PRINTER",
            "SLOW",
            "STORAGE",
            "WINDOWS_UPDATE",
            "SECURITY",
            "OFFICE",
            "OTHER"
    };

    private final String[] priorities = {
            "LOW",
            "MEDIUM",
            "HIGH"
    };

    public EmployeeMenu(Employee employee) {
        this.userId = employee.getUserId();
        this.employeeService = new EmployeeService();
        this.diagnosticService = new EmployeeDiagnosticService();
    }

    /** Eerst de toegang controleren, daarna de gewone menulus uit Menu (super.show()). */
    @Override
    public void show() {

        // Controleer in de database of dit account echt een actieve employee is.
        try {
            if (!employeeService.isEmployee(userId)) {
                ConsoleUtil.message("Geen toegang tot het Employee menu.");
                return;
            }

        } catch (SQLException e) {
            ConsoleUtil.message("Employee account kon niet worden gecontroleerd:\n" + e.getMessage());
            return;
        }

        super.show();
    }

    @Override
    protected void printScreen() {

        printHeader("EMPLOYEE MENU");

        try {
            System.out.println("Ingelogd als: " + employeeService.getEmployeeName(userId));

        } catch (SQLException e) {
            System.out.println("Ingelogd als Employee");
        }

        System.out.println();
        printOptions(OPTIONS);
        System.out.println();
    }

    /** Dit menu leest een getal in plaats van tekst. */
    @Override
    protected String readChoice() {
        return String.valueOf(ConsoleUtil.readInt("Keuze: "));
    }

    @Override
    protected boolean handleChoice(String choice) {

        switch (choice) {
            case "1" -> reportProblem();
            case "2" -> showMyTickets();
            case "3" -> showTicketDetails();
            case "4" -> runFullCheck();
            case "5" -> showMyDevices();
            case "0" -> {
                return false;
            }
            default -> ConsoleUtil.message("Ongeldige keuze.");
        }

        return true;
    }

    /** Maakt een ticket, voert de passende diagnose uit en beslist over de vervolgstap. */
    private void reportProblem() {

        ConsoleUtil.clearScreen();
        printHeader("PROBLEEM MELDEN");
        System.out.println(BACK_HINT);

        try {
            // Bij 0 (terug) wordt er niets opgeslagen: het ticket ontstaat pas na de laatste vraag.
            int device = chooseDevice();

            if (device == BACK) {
                return;
            }

            System.out.println();

            String title = readRequiredOrBack("Korte titel van het probleem: ");

            if (title == null) {
                return;
            }

            String description = readRequiredOrBack("Beschrijf het probleem: ");

            if (description == null) {
                return;
            }

            String category = chooseCategory();

            if (category == null) {
                return;
            }

            String priority = choosePriority();

            if (priority == null) {
                return;
            }

            Integer deviceId = (device == NO_DEVICE) ? null : device;

            int ticketId = employeeService.createTicket(userId, deviceId, title, description, category, priority);

            List<DiagnosticOutcome> results = diagnosticService.runForCategory(category);

            saveResults(ticketId, results);

            ConsoleUtil.clearScreen();
            printHeader("DIAGNOSE RESULTAAT");

            System.out.println("Ticket #" + ticketId + " is aangemaakt.");

            showResults(results);

            // Ander probleem of harde fout: direct naar een technician.
            if ("OTHER".equals(category) || containsHardFailure(results)) {

                employeeService.sendToTechnician(ticketId, userId);

                System.out.println();
                System.out.println("Status: TECHNICIAN_REQUIRED");
                System.out.println("Het ticket wacht op een technician.");
                System.out.println();

                ConsoleUtil.pause();
                return;
            }

            System.out.println();

            String answer = readRequired("Is het probleem opgelost na het advies? (j/n): ");

            if ("j".equalsIgnoreCase(answer) || "ja".equalsIgnoreCase(answer)) {

                employeeService.resolveAutomatically(ticketId, userId,
                        "Opgelost na automatische diagnose en advies.");

                System.out.println();
                System.out.println("Ticket is als RESOLVED opgeslagen.");

            } else {

                employeeService.sendToTechnician(ticketId, userId);

                System.out.println();
                System.out.println("Status: TECHNICIAN_REQUIRED");
                System.out.println("Het ticket wacht op een technician.");
            }

            System.out.println();
            ConsoleUtil.pause();

        } catch (SQLException e) {
            ConsoleUtil.message("Databasefout:\n" + e.getMessage());
        }
    }

    /** Voert alle controles uit en slaat ze op in een nieuw ticket. */
    private void runFullCheck() {

        ConsoleUtil.clearScreen();
        printHeader("VOLLEDIGE CONTROLE");

        try {
            int device = chooseDevice();

            if (device == BACK) {
                return;
            }

            Integer deviceId = (device == NO_DEVICE) ? null : device;

            int ticketId = employeeService.createTicket(
                    userId,
                    deviceId,
                    "Volledige systeemcontrole",
                    "Volledige controle gestart door Employee.",
                    "FULL_CHECK",
                    "LOW");

            System.out.println();
            System.out.println("Volledige controle wordt uitgevoerd...");

            List<DiagnosticOutcome> results = diagnosticService.runFullCheck();

            saveResults(ticketId, results);

            ConsoleUtil.clearScreen();
            printHeader("VOLLEDIGE CONTROLE RESULTAAT");

            System.out.println("Ticket #" + ticketId + " bewaart de resultaten.");

            showResults(results);

            if (diagnosticService.needsTechnician(results)) {

                employeeService.sendToTechnician(ticketId, userId);

                System.out.println();
                System.out.println("Er is iets gevonden dat aandacht nodig heeft.");
                System.out.println("Status: TECHNICIAN_REQUIRED");
                System.out.println("Het ticket wacht op een technician.");

            } else {

                employeeService.resolveAutomatically(ticketId, userId,
                        "Volledige controle afgerond zonder duidelijk probleem.");

                System.out.println();
                System.out.println("Geen duidelijk probleem gevonden.");
                System.out.println("Ticket is als RESOLVED opgeslagen.");
            }

            System.out.println();
            ConsoleUtil.pause();

        } catch (SQLException e) {
            ConsoleUtil.message("Databasefout:\n" + e.getMessage());
        }
    }

    private void showMyTickets() {

        ConsoleUtil.clearScreen();
        printHeader("MIJN TICKETS");

        try {
            List<TicketSummary> tickets = employeeService.getMyTickets(userId);

            if (tickets.isEmpty()) {
                System.out.println("Je hebt nog geen tickets.");
                System.out.println();
                ConsoleUtil.pause();
                return;
            }

            printTicketTable(tickets, false);

            System.out.println();
            System.out.println("Kies 3 in het menu om de details van een ticket te bekijken.");
            System.out.println();
            ConsoleUtil.pause();

        } catch (SQLException e) {
            ConsoleUtil.message("Tickets konden niet worden geladen:\n" + e.getMessage());
        }
    }

    /** Lijst met eigen tickets -> details van het gekozen ticket -> terug naar de lijst, tot 0. */
    private void showTicketDetails() {

        try {
            while (true) {
                ConsoleUtil.clearScreen();
                printHeader("TICKET DETAILS");

                // De employee kiest uit de eigen tickets; een ticketnummer onthouden is niet nodig.
                List<TicketSummary> tickets = employeeService.getMyTickets(userId);

                if (tickets.isEmpty()) {
                    System.out.println("Je hebt nog geen tickets.");
                    System.out.println();
                    ConsoleUtil.pause();
                    return;
                }

                TicketSummary chosen = chooseTicket(tickets);

                if (chosen == null) {
                    return;
                }

                TicketDetails ticket = employeeService.getTicketDetails(userId, chosen.getTicketId());

                if (ticket == null) {
                    ConsoleUtil.message("Ticket niet gevonden of niet van deze Employee.");
                    continue;
                }

                printTicketDetails(ticket);
            }

        } catch (SQLException e) {
            ConsoleUtil.message("Ticket kon niet worden geladen:\n" + e.getMessage());
        }
    }

    /** Alleen de gegevens van dit ene ticket, inclusief de diagnoses. */
    private void printTicketDetails(TicketDetails ticket) {

        ConsoleUtil.clearScreen();
        printHeader("TICKET #" + ticket.getTicketId());

        System.out.println("Titel: " + ticket.getTitle());
        System.out.println("Beschrijving: " + ticket.getDescription());
        System.out.println("Categorie: " + ticket.getCategory());
        System.out.println("Prioriteit: " + ticket.getPriority());
        System.out.println("Status: " + ticket.getStatus());
        System.out.println("Device: " + valueOrDash(ticket.getDeviceName()));
        System.out.println("Technician: " + valueOrDash(ticket.getTechnicianName()));
        System.out.println("Gemaakt: " + formatDate(ticket.getCreatedAt()));

        if (ticket.getClosedAt() != null) {
            System.out.println("Afgerond: " + formatDate(ticket.getClosedAt()));
        }

        if (ticket.getResolution() != null && !ticket.getResolution().isBlank()) {
            System.out.println("Oplossing: " + ticket.getResolution());
        }

        System.out.println();

        List<DiagnosticView> diagnostics = ticket.getDiagnostics();

        if (diagnostics.isEmpty()) {
            System.out.println("Diagnose resultaten:");
            System.out.println("Nog geen diagnose resultaten.");

        } else {
            // Alle controles van een ticket draaien tegelijk; de tijd staat daarom één keer bovenaan.
            System.out.println("Diagnose resultaten (" + formatDate(diagnostics.get(0).getPerformedAt()) + "):");
            System.out.println("-".repeat(80));

            for (DiagnosticView diagnostic : diagnostics) {
                System.out.println(String.format(DIAGNOSTIC_ROW,
                        diagnostic.getType(), diagnostic.getResult(), diagnostic.getDetails()));
                System.out.println(String.format(DIAGNOSTIC_ROW,
                        "", "", "Advies: " + diagnostic.getRecommendation()));
            }
        }

        System.out.println();
        ConsoleUtil.pause();
    }

    private void showMyDevices() {

        ConsoleUtil.clearScreen();
        printHeader("MIJN DEVICES");

        try {
            List<DeviceInfo> devices = employeeService.getAssignedDevices(userId);

            if (devices.isEmpty()) {
                System.out.println("Er is nog geen device aan jouw account gekoppeld.");
                System.out.println();
                ConsoleUtil.pause();
                return;
            }

            for (DeviceInfo device : devices) {
                System.out.println();
                System.out.println("Device ID: " + device.getDeviceId());
                System.out.println("Naam: " + device.getDeviceName());
                System.out.println("Type: " + valueOrDash(device.getDeviceType()));
                System.out.println("Merk/model: " + valueOrDash(device.getBrand())
                        + " " + valueOrDash(device.getModel()));
                System.out.println("OS: " + valueOrDash(device.getOperatingSystem()));
                System.out.println("Serial: " + valueOrDash(device.getSerialNumber()));
                System.out.println("Status: " + device.getStatus());
                System.out.println("------------------------------------");
            }

            System.out.println();
            ConsoleUtil.pause();

        } catch (SQLException e) {
            ConsoleUtil.message("Devices konden niet worden geladen:\n" + e.getMessage());
        }
    }

    /** Laat de employee een eigen ticket kiezen uit de lijst. Geeft null bij 0 (terug). */
    private TicketSummary chooseTicket(List<TicketSummary> tickets) {

        printTicketTable(tickets, true);

        System.out.println();
        System.out.println("0. Terug");

        while (true) {
            int choice = ConsoleUtil.readInt("Kies een ticket: ");

            if (choice == 0) {
                return null;
            }

            if (choice >= 1 && choice <= tickets.size()) {
                return tickets.get(choice - 1);
            }

            System.out.println("Ongeldige keuze.");
        }
    }

    /** Eén regel per ticket. Met numbered = true staat er een keuzenummer voor elke regel. */
    private void printTicketTable(List<TicketSummary> tickets, boolean numbered) {

        String prefix = numbered ? String.format("%-5s", "Nr") : "";

        System.out.println(prefix + String.format(TICKET_ROW, "Ticket", "Status", "Categorie", "Gemaakt", "Titel"));
        System.out.println("-".repeat(prefix.length() + 80));

        for (int i = 0; i < tickets.size(); i++) {

            TicketSummary ticket = tickets.get(i);

            if (numbered) {
                prefix = String.format("%-5s", (i + 1) + ".");
            }

            System.out.println(prefix + String.format(TICKET_ROW,
                    "#" + ticket.getTicketId(),
                    ticket.getStatus(),
                    ticket.getCategory(),
                    formatDate(ticket.getCreatedAt()),
                    ticket.getTitle()));
        }
    }

    private String formatDate(Timestamp timestamp) {

        if (timestamp == null) {
            return "-";
        }

        return timestamp.toLocalDateTime().format(DATE_FORMAT);
    }

    /**
     * Laat de employee een eigen device kiezen.
     * Geeft BACK (terug), NO_DEVICE (geen specifiek device) of het id van het gekozen device.
     */
    private int chooseDevice() throws SQLException {

        List<DeviceInfo> devices = employeeService.getAssignedDevices(userId);

        System.out.println();
        System.out.println("Kies een eigen device.");
        System.out.println("0. Terug");
        System.out.println("1. Geen specifiek device");

        for (int i = 0; i < devices.size(); i++) {
            System.out.println((i + 2) + ". " + devices.get(i).getDisplayName());
        }

        while (true) {
            int choice = ConsoleUtil.readInt("Device: ");

            if (choice == 0) {
                return BACK;
            }

            if (choice == 1) {
                return NO_DEVICE;
            }

            if (choice >= 2 && choice <= devices.size() + 1) {
                return devices.get(choice - 2).getDeviceId();
            }

            System.out.println("Ongeldige keuze.");
        }
    }

    /** Geeft de categoriecode terug, of null bij 0 (terug). */
    private String chooseCategory() {

        System.out.println();
        System.out.println("Wat merk je ongeveer?");

        for (int i = 0; i < categoryNames.length; i++) {
            System.out.println((i + 1) + ". " + categoryNames[i]);
        }

        System.out.println("0. Terug");

        while (true) {
            int choice = ConsoleUtil.readInt("Categorie: ");

            if (choice == 0) {
                return null;
            }

            if (choice >= 1 && choice <= categoryCodes.length) {
                return categoryCodes[choice - 1];
            }

            System.out.println("Ongeldige keuze.");
        }
    }

    /** Geeft de prioriteit terug, of null bij 0 (terug). */
    private String choosePriority() {

        System.out.println();
        System.out.println("Prioriteit:");

        for (int i = 0; i < priorities.length; i++) {
            System.out.println((i + 1) + ". " + priorities[i]);
        }

        System.out.println("0. Terug");

        while (true) {
            int choice = ConsoleUtil.readInt("Prioriteit: ");

            if (choice == 0) {
                return null;
            }

            if (choice >= 1 && choice <= priorities.length) {
                return priorities[choice - 1];
            }

            System.out.println("Ongeldige keuze.");
        }
    }

    private void saveResults(int ticketId, List<DiagnosticOutcome> results) throws SQLException {
        for (DiagnosticOutcome result : results) {
            employeeService.saveDiagnosticResult(ticketId, result);
        }
    }

    private void showResults(List<DiagnosticOutcome> results) {

        for (DiagnosticOutcome result : results) {
            System.out.println();
            System.out.println(result.getType() + " -> " + result.getStatus());
            System.out.println(result.getMessage());
            System.out.println("Details: " + result.getDetails());
            System.out.println("Advies: " + result.getRecommendation());
            System.out.println("------------------------------------");
        }
    }

    /** ERROR of TECHNICIAN_REQUIRED kan de employee niet zelf oplossen. */
    private boolean containsHardFailure(List<DiagnosticOutcome> results) {

        for (DiagnosticOutcome result : results) {
            if (result.isHardFailure()) {
                return true;
            }
        }

        return false;
    }

    /** Blijft vragen tot er iets is ingevuld. Geeft null bij 0 (terug). */
    private String readRequiredOrBack(String message) {

        while (true) {
            String input = readOrBack(message);

            if (input == null) {
                return null;
            }

            if (!input.isBlank()) {
                return input;
            }

            System.out.println("Dit veld mag niet leeg zijn.");
        }
    }

    /** Blijft vragen tot er iets is ingevuld (zonder terug-optie, voor de j/n-vraag). */
    private String readRequired(String message) {

        while (true) {
            String input = ConsoleUtil.readLine(message);

            if (!input.isBlank()) {
                return input;
            }

            System.out.println("Dit veld mag niet leeg zijn.");
        }
    }

    private String valueOrDash(String value) {

        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }

    /** Overschrijft de kop uit Menu: de employee-schermen hebben een bredere kop. */
    @Override
    protected void printHeader(String title) {
        System.out.println("====================================");
        System.out.println("        " + title);
        System.out.println("====================================");
        System.out.println();
    }
}

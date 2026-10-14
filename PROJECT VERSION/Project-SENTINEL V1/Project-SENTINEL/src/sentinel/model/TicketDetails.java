package sentinel.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Volledig ticket van de employee, inclusief diagnoseresultaten.
 *
 * Inheritance: TicketDetails is een TicketSummary met extra gegevens
 * (beschrijving, afrondtijd en diagnoses). De gedeelde velden staan in
 * TicketSummary en worden via super(...) gevuld.
 */
public class TicketDetails extends TicketSummary {

    private final String description;
    private final Timestamp closedAt;
    private final List<DiagnosticView> diagnostics = new ArrayList<>();

    public TicketDetails(int ticketId, String title, String description, String category, String priority,
                         String status, Timestamp createdAt, Timestamp closedAt, String deviceName,
                         String technicianName, String resolution) {
        super(ticketId, title, category, priority, status, createdAt, deviceName, technicianName, resolution);
        this.description = description;
        this.closedAt = closedAt;
    }

    public String getDescription() {
        return description;
    }

    public Timestamp getClosedAt() {
        return closedAt;
    }

    public void addDiagnostic(DiagnosticView diagnostic) {
        diagnostics.add(diagnostic);
    }

    /** Encapsulation: alleen-lezen; toevoegen kan alleen via addDiagnostic(). */
    public List<DiagnosticView> getDiagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }
}

package sentinel.model;

import java.sql.Timestamp;

/**
 * Opgeslagen diagnoseresultaat, zoals getoond bij ticketdetails.
 */
public class DiagnosticView {

    private final String type;
    private final String result;
    private final String details;
    private final String recommendation;
    private final Timestamp performedAt;

    public DiagnosticView(String type, String result, String details, String recommendation,
                          Timestamp performedAt) {
        this.type = type;
        this.result = result;
        this.details = details;
        this.recommendation = recommendation;
        this.performedAt = performedAt;
    }

    public String getType() {
        return type;
    }

    public String getResult() {
        return result;
    }

    public String getDetails() {
        return details;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public Timestamp getPerformedAt() {
        return performedAt;
    }
}

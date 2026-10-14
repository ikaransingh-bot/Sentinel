package sentinel.model;

/**
 * Uitkomst van een diagnose (bijvoorbeeld NETWORK of STORAGE).
 * Status is OK, WARNING, ERROR of TECHNICIAN_REQUIRED.
 */
public class DiagnosticOutcome {

    private final String type;
    private final String status;
    private final String message;
    private final String details;
    private final String recommendation;

    public DiagnosticOutcome(String type, String status, String message, String details, String recommendation) {
        this.type = type;
        this.status = status;
        this.message = message;
        this.details = details;
        this.recommendation = recommendation;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public String getRecommendation() {
        return recommendation;
    }

    /** ERROR of TECHNICIAN_REQUIRED kan de employee niet zelf oplossen. */
    public boolean isHardFailure() {
        return "ERROR".equals(status) || "TECHNICIAN_REQUIRED".equals(status);
    }

    /** WARNING, ERROR of TECHNICIAN_REQUIRED: een technician moet ernaar kijken. */
    public boolean needsAttention() {
        return "WARNING".equals(status) || isHardFailure();
    }
}

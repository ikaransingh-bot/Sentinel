package sentinel.diagnostic;

import sentinel.model.DiagnosticOutcome;

/**
 * Controle die niets meet, maar eerlijk zegt dat een technician nodig is.
 * Voor "Ander probleem" en onbekende categorieën: SENTINEL gokt niet.
 */
public class ManualCheckDiagnostic extends Diagnostic {

    private final String message;
    private final String details;
    private final String recommendation;

    public ManualCheckDiagnostic(String type, String message, String details, String recommendation) {
        super(type);
        this.message = message;
        this.details = details;
        this.recommendation = recommendation;
    }

    @Override
    public DiagnosticOutcome run() {
        return outcome("TECHNICIAN_REQUIRED", message, details, recommendation);
    }
}

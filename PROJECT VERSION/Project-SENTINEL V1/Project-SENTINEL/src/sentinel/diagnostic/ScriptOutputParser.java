package sentinel.diagnostic;

import sentinel.model.DiagnosticOutcome;

import java.util.List;

/**
 * Leest de regels STATUS=, MESSAGE=, DETAIL= en RECOMMENDATION= uit de scriptuitvoer.
 *
 * Package-private (geen public): alleen ScriptDiagnostic in deze package
 * gebruikt deze class. Buiten sentinel.diagnostic is hij onzichtbaar.
 */
class ScriptOutputParser {

    private static final String[] VALID_STATUSES = {"OK", "WARNING", "ERROR", "TECHNICIAN_REQUIRED"};

    DiagnosticOutcome parse(String type, List<String> lines) {

        String status = "";
        String message = "";
        String details = "";
        String recommendation = "";

        for (String line : lines) {

            int index = line.indexOf('=');

            if (index <= 0) {
                continue;
            }

            String key = line.substring(0, index).trim();
            String value = line.substring(index + 1).trim();

            switch (key) {
                case "STATUS" -> status = value;
                case "MESSAGE" -> message = value;
                case "DETAIL" -> details = value;
                case "RECOMMENDATION" -> recommendation = value;
                default -> {
                    // Onbekende regels negeren.
                }
            }
        }

        if (!isValidStatus(status)) {
            return new DiagnosticOutcome(
                    type,
                    "TECHNICIAN_REQUIRED",
                    "SENTINEL kreeg geen geldige diagnose-uitkomst.",
                    String.join(" | ", lines),
                    "Laat een technician de controle verder uitvoeren.");
        }

        return new DiagnosticOutcome(
                type,
                status,
                fallback(message, "Controle afgerond."),
                fallback(details, "Geen extra details."),
                fallback(recommendation, "Geen extra advies beschikbaar."));
    }

    private boolean isValidStatus(String status) {

        for (String validStatus : VALID_STATUSES) {
            if (validStatus.equals(status)) {
                return true;
            }
        }

        return false;
    }

    private String fallback(String value, String fallback) {

        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }
}

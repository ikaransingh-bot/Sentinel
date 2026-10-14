package sentinel.diagnostic;

import sentinel.model.DiagnosticOutcome;

/**
 * Basis voor elke automatische controle.
 *
 * Abstraction: Diagnostic legt vast DAT elke controle een run() heeft,
 * maar niet HOE. Dat vult elke subclass zelf in.
 * Inheritance: StorageDiagnostic, ScriptDiagnostic en ManualCheckDiagnostic
 * erven het type en de hulpmethode outcome().
 * Polymorphism: EmployeeDiagnosticService roept run() aan zonder te weten
 * welke soort controle het is.
 */
public abstract class Diagnostic {

    private final String type;

    protected Diagnostic(String type) {
        this.type = type;
    }

    /** Bijvoorbeeld STORAGE, NETWORK of OTHER. */
    public String getType() {
        return type;
    }

    /** Voert de controle uit. Elke subclass doet dit op zijn eigen manier. */
    public abstract DiagnosticOutcome run();

    /** Voor subclasses: maakt een uitkomst met het type van deze controle. */
    protected DiagnosticOutcome outcome(String status, String message, String details, String recommendation) {
        return new DiagnosticOutcome(type, status, message, details, recommendation);
    }
}

package sentinel.service;

import sentinel.diagnostic.Diagnostic;
import sentinel.diagnostic.ManualCheckDiagnostic;
import sentinel.diagnostic.ScriptDiagnostic;
import sentinel.diagnostic.StorageDiagnostic;
import sentinel.model.DiagnosticOutcome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kiest welke controles bij een probleemcategorie horen en voert ze uit.
 *
 * Generics en Collections: een Map van categorie naar een List met Diagnostic-objecten.
 * Polymorphism: runAll() roept run() aan op elke Diagnostic; Java kiest zelf de
 * versie van StorageDiagnostic, ScriptDiagnostic of ManualCheckDiagnostic.
 */
public class EmployeeDiagnosticService {

    private final Map<String, List<Diagnostic>> diagnosticsByCategory = new HashMap<>();
    private final List<Diagnostic> fullCheck;

    private final Diagnostic unknownCategory = new ManualCheckDiagnostic(
            "UNKNOWN",
            "Onbekende diagnosecategorie.",
            "SENTINEL heeft geen vaste controle voor deze categorie.",
            "Laat een technician het probleem controleren.");

    public EmployeeDiagnosticService() {

        Diagnostic storage = new StorageDiagnostic();
        Diagnostic network = new ScriptDiagnostic("NETWORK");
        Diagnostic printer = new ScriptDiagnostic("PRINTER");
        Diagnostic system = new ScriptDiagnostic("SYSTEM");
        Diagnostic security = new ScriptDiagnostic("SECURITY");
        Diagnostic update = new ScriptDiagnostic("UPDATE");
        Diagnostic office = new ScriptDiagnostic("OFFICE");

        Diagnostic other = new ManualCheckDiagnostic(
                "OTHER",
                "Geen betrouwbare automatische diagnose beschikbaar.",
                "SENTINEL heeft alleen een algemene systeemcontrole uitgevoerd.",
                "Een technician moet dit probleem verder onderzoeken.");

        diagnosticsByCategory.put("NETWORK", List.of(network));
        diagnosticsByCategory.put("PRINTER", List.of(printer));
        diagnosticsByCategory.put("SLOW", List.of(storage, system));
        diagnosticsByCategory.put("STORAGE", List.of(storage));
        diagnosticsByCategory.put("WINDOWS_UPDATE", List.of(update));
        diagnosticsByCategory.put("SECURITY", List.of(security));
        diagnosticsByCategory.put("OFFICE", List.of(office));
        diagnosticsByCategory.put("OTHER", List.of(system, other));

        fullCheck = List.of(storage, network, printer, system, security, update, office);
    }

    /** Voert de controles uit die bij de gekozen probleemcategorie horen. */
    public List<DiagnosticOutcome> runForCategory(String category) {

        List<Diagnostic> diagnostics = diagnosticsByCategory.getOrDefault(category, List.of(unknownCategory));

        return runAll(diagnostics);
    }

    /** Voert alle controles uit. */
    public List<DiagnosticOutcome> runFullCheck() {
        return runAll(fullCheck);
    }

    /** True zodra één uitkomst niet OK is. */
    public boolean needsTechnician(List<DiagnosticOutcome> results) {

        for (DiagnosticOutcome result : results) {
            if (result.needsAttention()) {
                return true;
            }
        }

        return false;
    }

    private List<DiagnosticOutcome> runAll(List<Diagnostic> diagnostics) {

        List<DiagnosticOutcome> results = new ArrayList<>();

        for (Diagnostic diagnostic : diagnostics) {
            // Polymorphism: dezelfde aanroep, per soort controle ander gedrag.
            results.add(diagnostic.run());
        }

        return results;
    }
}

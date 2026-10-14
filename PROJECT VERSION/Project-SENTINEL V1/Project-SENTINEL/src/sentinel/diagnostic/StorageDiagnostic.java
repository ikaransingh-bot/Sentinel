package sentinel.diagnostic;

import sentinel.model.DiagnosticOutcome;

import java.io.File;

/**
 * Controleert de vrije ruimte op de Windows-systeemschijf, volledig in Java.
 */
public class StorageDiagnostic extends Diagnostic {

    private static final double ERROR_PERCENTAGE = 5.0;
    private static final double WARNING_PERCENTAGE = 15.0;
    private static final long BYTES_PER_GB = 1024L * 1024L * 1024L;

    public StorageDiagnostic() {
        super("STORAGE");
    }

    @Override
    public DiagnosticOutcome run() {

        String systemDrive = System.getenv("SystemDrive");

        if (systemDrive == null || systemDrive.isBlank()) {
            systemDrive = "C:";
        }

        File drive = new File(systemDrive + File.separator);

        // long: schijfgroottes in bytes passen niet in een int (max ongeveer 2 GB).
        long total = drive.getTotalSpace();
        long free = drive.getUsableSpace();

        if (total <= 0) {
            return outcome(
                    "TECHNICIAN_REQUIRED",
                    "Opslag kon niet betrouwbaar worden gelezen.",
                    "De totale opslagruimte is niet beschikbaar.",
                    "Laat een technician de opslag controleren.");
        }

        // Math operators: met 100.0 (een double) wordt het een kommagetal
        // in plaats van een afgeronde gehele deling.
        double freePercentage = (free * 100.0) / total;

        long totalGb = total / BYTES_PER_GB;
        long freeGb = free / BYTES_PER_GB;

        String details = "Totaal: " + totalGb
                + " GB, vrij: " + freeGb
                + " GB, vrij percentage: "
                + String.format("%.1f", freePercentage)
                + "%.";

        if (freePercentage < ERROR_PERCENTAGE) {
            return outcome(
                    "ERROR",
                    "Er is bijna geen vrije opslagruimte.",
                    details,
                    "Maak opslagruimte vrij of laat een technician controleren wat veilig verwijderd kan worden.");
        }

        if (freePercentage < WARNING_PERCENTAGE) {
            return outcome(
                    "WARNING",
                    "De vrije opslagruimte is laag.",
                    details,
                    "Maak indien mogelijk opslagruimte vrij.");
        }

        return outcome(
                "OK",
                "De vrije opslagruimte lijkt normaal.",
                details,
                "Geen directe opslagactie nodig.");
    }
}

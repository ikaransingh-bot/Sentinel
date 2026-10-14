package sentinel.diagnostic;

import sentinel.model.DiagnosticOutcome;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Controle via scripts/employee-diagnostics.ps1
 * (NETWORK, PRINTER, SYSTEM, SECURITY, UPDATE of OFFICE).
 * Het script krijgt alleen een vaste actienaam, nooit tekst van de gebruiker.
 */
public class ScriptDiagnostic extends Diagnostic {

    private static final int SCRIPT_TIMEOUT_SECONDS = 20;

    private final ScriptOutputParser parser = new ScriptOutputParser();

    public ScriptDiagnostic(String action) {
        super(action);
    }

    @Override
    public DiagnosticOutcome run() {

        String action = getType();
        Path script = getProjectRoot().resolve("scripts").resolve("employee-diagnostics.ps1");

        if (!Files.exists(script)) {
            return outcome(
                    "TECHNICIAN_REQUIRED",
                    "Diagnosescript ontbreekt.",
                    script.toAbsolutePath().toString(),
                    "Laat een technician de SENTINEL installatie controleren.");
        }

        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                script.toAbsolutePath().toString(),
                "-Action",
                action);

        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();

            boolean finished = process.waitFor(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();

                return outcome(
                        "TECHNICIAN_REQUIRED",
                        "De controle duurde te lang.",
                        "Timeout na " + SCRIPT_TIMEOUT_SECONDS + " seconden.",
                        "Laat een technician deze controle verder uitvoeren.");
            }

            List<String> lines = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            // Exitcode 0 = het script heeft een resultaat geschreven; anders faalde PowerShell zelf.
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                return outcome(
                        "TECHNICIAN_REQUIRED",
                        "Het diagnosescript is met een fout gestopt.",
                        "Exitcode " + exitCode + ": " + String.join(" | ", lines),
                        "Laat een technician de controle handmatig uitvoeren.");
            }

            return parser.parse(action, lines);

        } catch (Exception e) {
            return outcome(
                    "TECHNICIAN_REQUIRED",
                    "De controle kon niet worden uitgevoerd.",
                    e.getMessage() == null ? "Onbekende PowerShell fout." : e.getMessage(),
                    "Laat een technician de controle handmatig uitvoeren.");
        }
    }

    /** SENTINEL_ROOT wordt door bootstrap.ps1 gezet; anders de huidige map. */
    private Path getProjectRoot() {

        String root = System.getenv("SENTINEL_ROOT");

        if (root != null && !root.isBlank()) {
            return Paths.get(root);
        }

        return Paths.get("").toAbsolutePath().normalize();
    }
}

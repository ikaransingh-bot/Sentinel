package sentinel.util;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;

/**
 * Leest een wachtwoord in zonder dat de tekens zichtbaar zijn.
 * Op Windows via PowerShell, anders (of als dat mislukt) via de Java Console.
 */
public final class PasswordReader {

    private PasswordReader() {
    }

    public static String readPassword(String prompt) {

        if (isWindows()) {
            try {
                return readWindowsPassword(prompt);

            } catch (Exception e) {
                // Als PowerShell niet werkt, gebruiken we Java Console.
            }
        }

        Console console = System.console();

        if (console != null) {
            char[] password = console.readPassword("%s: ", cleanPrompt(prompt));

            if (password == null) {
                return "";
            }

            return new String(password);
        }

        return "";
    }

    private static String readWindowsPassword(String prompt) throws Exception {

        String cleanPrompt = cleanPrompt(prompt).replace("'", "''");

        // PowerShell vraagt het wachtwoord verborgen op en schrijft het als tekst terug.
        String command = "$secure = Read-Host -Prompt '" + cleanPrompt + "' -AsSecureString; "
                + "$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure); "
                + "try { "
                + "[Console]::Out.WriteLine([Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)) "
                + "} finally { "
                + "[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) "
                + "}";

        ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-Command", command);
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = builder.start();
        String password;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            password = reader.readLine();
        }

        int exitCode = process.waitFor();

        if (exitCode != 0 || password == null) {
            throw new RuntimeException("Password invoer mislukt.");
        }

        return password;
    }

    private static String cleanPrompt(String prompt) {

        if (prompt == null) {
            return "Password";
        }

        return prompt.replace(":", "").trim();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}

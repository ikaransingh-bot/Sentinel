package sentinel.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


public final class DatabaseConnection {

    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private DatabaseConnection() {
    }

    private static void loadProperties() {

        String sentinelRoot = System.getenv("SENTINEL_ROOT");
        Path configPath;

        if (sentinelRoot != null && !sentinelRoot.isBlank()) {
            configPath = Paths.get(sentinelRoot, "config", "database.properties");
        } else {
            configPath = Paths.get("config", "database.properties");
        }

        try (FileInputStream input = new FileInputStream(configPath.toFile())) {
            properties.load(input);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Database configuratie kon niet worden geladen: " + configPath.toAbsolutePath(), e);
        }
    }

    /** Eerst de omgevingsvariabele, ander d waarde uit database.properties. */
    private static String getValue(String environmentName, String propertyName) {

        String environmentValue = System.getenv(environmentName);

        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }

        String propertyValue = properties.getProperty(propertyName);

        if (propertyValue == null || propertyValue.isBlank()) {
            throw new IllegalStateException("Database instelling ontbreekt: " + propertyName);
        }

        return propertyValue.trim();
    }

    public static Connection getConnection() throws SQLException {

        String host = getValue("SENTINEL_DB_HOST", "db.host");
        String port = getValue("SENTINEL_DB_PORT", "db.port");
        String database = getValue("SENTINEL_DB_NAME", "db.name");
        String user = getValue("SENTINEL_DB_USER", "db.user");

        String password = properties.getProperty("db.password");

        if (password == null) {
            password = "";
        }

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&connectTimeout=5000";

        return DriverManager.getConnection(url, user, password);
    }
}

package sentinel.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generieke hulpmethodes voor JDBC, zodat de services niet steeds dezelfde
 * connectie-, statement- en ResultSet-code herhalen.
 *
 * Generics: de methodes werken voor elk type T. De RowMapper bepaalt hoe
 * één rij een T wordt. Alle queries gebruiken PreparedStatement met
 * ?-parameters, zodat invoer nooit als SQL wordt uitgevoerd.
 */
public final class Database {

    private Database() {
    }

    /** Alle rijen, elk omgezet naar een T. */
    public static <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... parameters) throws SQLException {

        List<T> results = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bind(statement, parameters);

            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    results.add(mapper.map(row));
                }
            }
        }

        return results;
    }

    /** Alleen de eerste rij; Optional.empty() als er geen rij is. */
    public static <T> Optional<T> queryFirst(String sql, RowMapper<T> mapper, Object... parameters) throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bind(statement, parameters);

            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) {
                    return Optional.ofNullable(mapper.map(row));
                }
            }
        }

        return Optional.empty();
    }

    /** True als de query minstens één rij oplevert. */
    public static boolean exists(String sql, Object... parameters) throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bind(statement, parameters);

            try (ResultSet row = statement.executeQuery()) {
                return row.next();
            }
        }
    }

    /** INSERT of UPDATE; geeft het aantal geraakte rijen terug. */
    public static int update(String sql, Object... parameters) throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bind(statement, parameters);

            return statement.executeUpdate();
        }
    }

    /** parameters is een array: element i hoort bij het (i + 1)-de vraagteken, want JDBC telt vanaf 1. */
    private static void bind(PreparedStatement statement, Object[] parameters) throws SQLException {

        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }
}

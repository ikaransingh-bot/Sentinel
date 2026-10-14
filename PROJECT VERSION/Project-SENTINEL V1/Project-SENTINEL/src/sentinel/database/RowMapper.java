package sentinel.database;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Zet één databaserij om naar een object van type T.
 *
 * Generics: Database weet niet welk soort object eruit moet komen. De service
 * bepaalt dat met een lambda, bijvoorbeeld:
 * row -> new DeviceInfo(row.getInt("device_id"), ...)
 * Zo werkt dezelfde code voor DeviceInfo, TicketSummary, String, enzovoort.
 */
@FunctionalInterface
public interface RowMapper<T> {

    T map(ResultSet row) throws SQLException;
}

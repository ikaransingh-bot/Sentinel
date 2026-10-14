package sentinel.model;

import java.sql.Timestamp;

/**
 * Eén regel in "Mijn tickets".
 *
 * Encapsulation: alle velden zijn private final; een ticket kan na het laden
 * niet per ongeluk van buitenaf worden gewijzigd.
 */
public class TicketSummary {

    private final int ticketId;
    private final String title;
    private final String category;
    private final String priority;
    private final String status;
    private final Timestamp createdAt;
    private final String deviceName;
    private final String technicianName;
    private final String resolution;

    public TicketSummary(int ticketId, String title, String category, String priority, String status,
                         Timestamp createdAt, String deviceName, String technicianName, String resolution) {
        this.ticketId = ticketId;
        this.title = title;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.deviceName = deviceName;
        this.technicianName = technicianName;
        this.resolution = resolution;
    }

    public int getTicketId() {
        return ticketId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public String getResolution() {
        return resolution;
    }
}

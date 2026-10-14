package sentinel.model;

import sentinel.menu.Menu;
import sentinel.menu.TechnicianMenu;

/**
 * Technician: pakt tickets op, lost ze op en beheert devices.
 */
public class Technician extends User {

    public Technician(int userId, String fullName, boolean firstLoginCompleted) {
        super(userId, fullName, firstLoginCompleted);
    }

    @Override
    public Menu createMainMenu() {
        return new TechnicianMenu(this);
    }
}

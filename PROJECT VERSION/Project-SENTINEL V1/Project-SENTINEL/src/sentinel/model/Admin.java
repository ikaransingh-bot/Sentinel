package sentinel.model;

import sentinel.menu.AdminMenu;
import sentinel.menu.Menu;

/**
 * Admin: alles van de technician, plus gebruikersbeheer en "Alle tickets".
 */
public class Admin extends User {

    public Admin(int userId, String fullName, boolean firstLoginCompleted) {
        super(userId, fullName, firstLoginCompleted);
    }

    @Override
    public Menu createMainMenu() {
        return new AdminMenu(this);
    }

    /** Overschrijft de standaard uit User: een admin ziet alle tickets. */
    @Override
    public boolean canViewAllTickets() {
        return true;
    }
}

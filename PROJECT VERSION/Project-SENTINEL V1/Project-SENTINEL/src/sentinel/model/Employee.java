package sentinel.model;

import sentinel.menu.EmployeeMenu;
import sentinel.menu.Menu;

/**
 * Medewerker: meldt problemen en volgt de eigen tickets.
 */
public class Employee extends User {

    public Employee(int userId, String fullName, boolean firstLoginCompleted) {
        super(userId, fullName, firstLoginCompleted);
    }

    @Override
    public Menu createMainMenu() {
        return new EmployeeMenu(this);
    }
}

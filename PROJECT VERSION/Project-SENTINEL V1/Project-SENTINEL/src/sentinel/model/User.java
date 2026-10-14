package sentinel.model;

import sentinel.menu.Menu;

/**
 * Een ingelogde gebruiker.
 *
 * Abstraction: een "gewone" gebruiker bestaat niet; het is altijd een Employee,
 * Technician of Admin. Daarom is User abstract: new User(...) kan niet.
 * Inheritance: de drie rollen erven id, naam en first-login-status.
 * Encapsulation: de velden zijn private final en alleen te lezen via getters.
 */
public abstract class User {

    private final int userId;
    private final String fullName;
    private final boolean firstLoginCompleted;

    // protected: alleen de subclasses roepen deze constructor aan via super(...).
    protected User(int userId, String fullName, boolean firstLoginCompleted) {
        this.userId = userId;
        this.fullName = fullName;
        this.firstLoginCompleted = firstLoginCompleted;
    }

    public int getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isFirstLoginCompleted() {
        return firstLoginCompleted;
    }

    /**
     * Polymorphism: elke rol geeft zijn eigen hoofdmenu terug.
     * LoginMenu roept alleen user.createMainMenu() aan, zonder switch op de rol.
     */
    public abstract Menu createMainMenu();

    /** Standaard niet; Admin overschrijft dit. */
    public boolean canViewAllTickets() {
        return false;
    }
}

// File: BlacklistProfileFilter.java
package cs151.application;

/**
 * Concrete filter: only include blacklist students.
 */
public class BlacklistProfileFilter implements ProfileFilter {

    @Override
    public boolean matches(StudentProfile profile) {
        return profile != null && profile.isBlacklist();
    }
}

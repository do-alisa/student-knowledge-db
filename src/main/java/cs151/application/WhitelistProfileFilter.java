// File: WhitelistProfileFilter.java
package cs151.application;

/**
 * Concrete filter: only include whitelist students.
 */
public class WhitelistProfileFilter implements ProfileFilter {

    @Override
    public boolean matches(StudentProfile profile) {
        return profile != null && profile.isWhitelist();
    }
}

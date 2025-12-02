// File: ProfileFilter.java
package cs151.application;

/**
 * Strategy/Filter for selecting which student profiles should be shown
 * in a report (e.g., whitelist vs blacklist).
 */
public interface ProfileFilter {
    /**
     * @return true if the given profile should be included in the report
     */
    boolean matches(StudentProfile profile);
}

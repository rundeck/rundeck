package com.dtolabs.rundeck.plugins.scm;

/**
 * Information about the user doing the export/import
 */
public interface ScmUserInfo {
    /**
     * @return the login name
     */
    public String getUserName();

    /**
     * @return the user's email if set in profile, or null
     */
    public String getEmail();

    /**
     * @return the user's name "firstname lastname" if set in profile, or null
     */
    public String getFullName();

    /**
     * @return the user's name "firstname" if set in profile, or null
     */
    public String getFirstName();

    /**
     * @return the user's name "lastname" if set in profile, or null
     */
    public String getLastName();
}

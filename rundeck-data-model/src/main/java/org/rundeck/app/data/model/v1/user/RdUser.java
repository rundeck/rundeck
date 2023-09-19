package org.rundeck.app.data.model.v1.user;

import java.util.Date;

public interface RdUser {
    Long getId();
    String getLogin();
    String getPassword();
    String getFirstName();
    String getLastName();
    String getEmail();
    Date getDateCreated();
    Date getLastUpdated();
    String getDashboardPref();
    String getFilterPref();
    Date getLastLogin();
    Date getLastLogout();
    String getLastSessionId();
    String getLastLoggedHostName();
    Object getReportfilters();

}

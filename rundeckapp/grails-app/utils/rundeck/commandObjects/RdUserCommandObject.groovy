package rundeck.commandObjects

import grails.validation.Validateable
import org.rundeck.app.data.model.v1.user.RdUser
import rundeck.User

class RdUserCommandObject implements RdUser, Validateable{
    Long id
    String login
    String password
    String firstName
    String lastName
    String email
    Date dateCreated
    Date lastUpdated
    String dashboardPref
    String filterPref
    Date lastLogin
    Date lastLogout
    String lastSessionId
    String lastLoggedHostName

    static constraints = {
        importFrom User
        id(nullable: true)
        dateCreated(nullable: true)
        lastUpdated(nullable: true)
    }

    @Override
    Long getId() {
        return id
    }

    @Override
    String getLogin() {
        return login
    }

    @Override
    String getPassword() {
        return password
    }

    @Override
    String getFirstName() {
        return firstName
    }

    @Override
    String getLastName() {
        return lastName
    }

    @Override
    String getEmail() {
        return email
    }

    @Override
    Date getDateCreated() {
        return dateCreated
    }

    @Override
    Date getLastUpdated() {
        return lastUpdated
    }

    @Override
    String getDashboardPref() {
        return dashboardPref
    }

    @Override
    String getFilterPref() {
        return filterPref
    }

    @Override
    Date getLastLogin() {
        return lastLogin
    }

    @Override
    Date getLastLogout() {
        return lastLogout
    }

    @Override
    String getLastSessionId() {
        return lastSessionId
    }

    @Override
    String getLastLoggedHostName() {
        return lastLoggedHostName
    }
}

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
    Object getJobfilters() {
        return null
    }

    @Override
    Object getNodefilters() {
        return null
    }

    @Override
    Object getReportfilters() {
        return null
    }
}

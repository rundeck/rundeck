package rundeck

import org.codehaus.groovy.grails.commons.ConfigurationHolder

class User {
    String login
    String password
    String firstName
    String lastName
    String email
    Date dateCreated
    Date lastUpdated
    
    static mapping = {
        def config = ConfigurationHolder.config
        if (config?.rundeck?.v14?.rdbsupport == 'true') {
            table "rduser"
        }
    }
    String dashboardPref
    String filterPref
    static hasMany = [reportfilters:ReportFilter,jobfilters:ScheduledExecutionFilter,nodefilters:NodeFilter]
    static constraints={
        firstName(nullable:true)
        lastName(nullable:true)
        email(nullable:true)
        password(nullable:true)
        dashboardPref(nullable:true)
        filterPref(nullable:true)
    }
}

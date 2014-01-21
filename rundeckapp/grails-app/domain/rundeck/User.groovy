package rundeck


class User {
    String login
    String password
    String firstName
    String lastName
    String email
    Date dateCreated
    Date lastUpdated
    
    static mapping = {
        table "rduser"
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

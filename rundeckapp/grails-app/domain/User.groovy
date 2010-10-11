class User {
    String login
    String password
    String firstName
    String lastName
    String email
    UserAuth authorization
    Date dateCreated
    Date lastUpdated
    
    String dashboardPref
    String filterPref
    static hasMany = [reportfilters:ReportFilter,jobfilters:ScheduledExecutionFilter,nodefilters:NodeFilter]
    static constraints={
		authorization(unique:true)
        firstName(nullable:true)
        lastName(nullable:true)
        email(nullable:true)
        password(nullable:true)
        dashboardPref(nullable:true)
        filterPref(nullable:true)
    }
}

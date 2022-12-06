package rundeck.data.validation.shared

import grails.validation.Validateable

class SharedJobScheduleConstraints implements Validateable {

    String minute
    String hour
    String dayOfMonth
    String month
    String dayOfWeek
    String seconds
    String year
    String crontabString

    static constraints = {
        seconds(nullable: true, matches: /^[0-9*\/,-]*$/)
        minute(nullable:true, matches: /^[0-9*\/,-]*$/ )
        hour(nullable:true, matches: /^[0-9*\/,-]*$/ )
        dayOfMonth(nullable:true, matches: /^[0-9*\/,?LW-]*$/ )
        month(nullable:true, matches: /^[0-9a-zA-z*\/,-]*$/ )
        dayOfWeek(nullable:true, matches: /^[0-9a-zA-z*\/?,L#-]*$/ )
        year(nullable:true, matches: /^[0-9*\/,-]*$/)
        crontabString(bindable: true,nullable: true)
    }
}

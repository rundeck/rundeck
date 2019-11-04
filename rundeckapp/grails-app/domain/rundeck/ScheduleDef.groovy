package rundeck

import com.dtolabs.rundeck.core.common.FrameworkResource

class ScheduleDef {

    static belongsTo = ScheduledExecution
    static hasMany = [scheduledExecutions:ScheduledExecution]

    String name
    String description
    String project
    String minute = "0"
    String hour = "0"
    String dayOfMonth = "?"
    String month = "*"
    String dayOfWeek = "*"
    String seconds = "0"
    String year = "*"
    String crontabString
    Date dateCreated
    Date lastUpdated
    String type

    static transients = ['crontabString']

    static mapping = {
        name type : 'string'
        description type : 'text'
        project type : 'string'
    }

    static constraints = {
        name(unique: ['project'], nullable:false, matches: "[^/]+", maxSize: 1024)
        description(nullable:true)
        project(nullable:false, blank: false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
        seconds(nullable: true, matches: /^[0-9*\/,-]*$/)
        minute(nullable:true, matches: /^[0-9*\/,-]*$/ )
        hour(nullable:true, matches: /^[0-9*\/,-]*$/ )
        dayOfMonth(nullable:true, matches: /^[0-9*\/,?LW-]*$/ )
        month(nullable:true, matches: /^[0-9a-zA-z*\/,-]*$/ )
        dayOfWeek(nullable:true, matches: /^[0-9a-zA-z*\/?,L#-]*$/ )
        year(nullable:true, matches: /^[0-9*\/,-]*$/)
        crontabString(bindable: true,nullable: true)
        type(nullable:false, matches:/^SIMPLE|CRON$/)
    }

    /**
     * parse the request parameters, and populate the dayOfWeek and month fields.
     * if 'everyDayOfWeek' is 'true', then dayOfWeek will be "*".
     * if 'everyMonth' is 'true', then month will be "*".
     * @param params the parameters
     *
     */
    def populateTimeDateFields(Map params) {
        def months ;
        def daysOfWeek;
        def daysOfMonth=params.dayOfMonth?:'?'

        if(params.crontabString && 'true'==params.useCrontabString){
            //parse the crontabString
            if(parseCrontabString(params.crontabString)){
                return
            }
        }
        def everyDay = params['everyDayOfWeek']
        if((everyDay instanceof Boolean && everyDay ) || (everyDay instanceof String && (everyDay=="true" || everyDay=="on"))){
            daysOfWeek="*"
        }else{
            daysOfWeek= parseCheckboxFieldFromParams("dayOfWeek",params,daysOfMonth=='?',ScheduledExecution.daysofweeklist)
        }
        def everyMonth = params['everyMonth']
        if((everyMonth instanceof Boolean && everyMonth ) || (everyMonth instanceof String && (everyMonth=="true" || everyMonth=="on"))){
            months="*"
        }else{
            months= parseCheckboxFieldFromParams("month",params,true, ScheduledExecution.monthsofyearlist)
        }
        this.month = months
        this.dayOfWeek = daysOfWeek?daysOfWeek:'?'
        this.dayOfMonth = daysOfMonth
        this.seconds = params.seconds?params.seconds:"0"
        this.year = params.year?params.year:"*"
        this.crontabString=null
    }

    /**
     * parse the parameters from checkbox fields, and return a crontab field expression.
     * @param field the name of the crontab field
     * @param params the paramters
     * @param defaultToAsterisk if true, return "*" when nothing matches, otherwise return null
     * @param all the list of all possible checkbox name components
     * @return the crontab expression, or null if nothing was matched
     */
    def parseCheckboxFieldFromParams(String field, Map params, boolean defaultToAsterisk, List all) {
        def list = []
        def lmap = [:]
        def input = filterCrontabParams(field,params)
        input.each { key, val ->
            if((val == "true" || val=="on") && all.contains(key.toUpperCase())) {
                list << key.toUpperCase()
                lmap[key.toUpperCase()]=true
            }
        }
        if (list.size() < 1 && defaultToAsterisk) {
            return "*"
        }else if (list.size() == all.size()){
            def notfound = all.grep{ val ->
                !lmap[val.toUpperCase()]
            }
            if(notfound.size()==0){
                return "*"
            }else{
                return list.sort().join(",")
            }
        } else if (list.size() > 0) {
            return list.sort().join(",")
        }
        return null
    }

    Map filterCrontabParams(String field, Map params) {
        def result = [ : ]
        def crontabpatt = '^crontab\\.'+field+'\\.(.*)$'
        params.each { key, val ->
            def matcher = key =~ crontabpatt
            if (matcher.matches()) {
                def crontabname = matcher[0][1]
                if(val instanceof List){
                    result[crontabname] = val[0] // val seems to be a one element list
                }else if(val instanceof String){
                    result[crontabname] = val
                }
            }
        }
        return result
    }

    // various utility methods to the process crontab entry data
    def String generateCrontabExression() {
        return [seconds?seconds:'0',minute,hour,dayOfMonth.toUpperCase(),month.toUpperCase(),dayOfMonth=='?'?dayOfWeek.toUpperCase():'?',year?year:'*'].join(" ")
    }

    /**
     * attempt to parse the string into 6-7 components, and fill the properties
     * of the ScheduledExecution appropriately.  Returns false if the size
     * is wrong
     */
     boolean parseCrontabString(String crontabString){
        def arr=crontabString.split(" ")
        if(arr.size()>7){
            arr=arr[0..<7]
        }
        if(arr.size()<6 || arr.size()>7){
            return false
        }
        this.seconds=arr[0]
        this.minute=arr[1]
        this.hour=arr[2]
        this.dayOfMonth=arr[3]
        this.month=arr[4]
        this.dayOfWeek=arr[5]
        this.year=arr.size()>6?arr[6]:'*'
        return true
    }

    static ScheduleDef fromMap(Map data){
        ScheduleDef sd = new ScheduleDef()
        sd.crontabString = data.cronString
        sd.parseCrontabString(sd.crontabString)
        sd.name = data.name
        sd.description = data.description
        sd.type = data.type
        sd.project = data.project
        return sd
    }

    Map toMap(){
        HashMap map = new HashMap()
        map.id = id
        map.name = name
        map.description = description
        map.project = project
        map.type = type
        map.cronString = generateCrontabExression()
        map.schedule = [hour:hour,minute:minute,seconds:seconds,month:month,year:year,dayOfMonth:dayOfMonth,dayOfWeek:dayOfWeek]

        if(scheduledExecutions){
            map.scheduledExecutions = []
            scheduledExecutions.sort().each { ScheduledExecution scheduledExecution ->
                def map1 = scheduledExecution.toMap()
                map.scheduledExecutions.add(map1 + [name: map1.name])
            }
        }
        return map
    }

}
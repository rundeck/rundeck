import org.codehaus.groovy.grails.commons.ConfigurationHolder

class ScheduledExecution extends ExecutionContext {
    SortedSet options
    static hasMany = [executions:Execution,options:Option,notifications:Notification]

    String groupPath
    String userRoleList
    String jobName
    String description
    String minute = "0"
    String hour = "0"
    String dayOfMonth = "?"
    String month = "*"
    String dayOfWeek = "*"
    String seconds = "0"
    String year = "*"
    String crontabString
    String uuid;

    Date nextExecution
    boolean scheduled = false
    Long totalTime=0
    Long execCount=0
    String adhocExecutionType
    Date dateCreated
    Date lastUpdated
    String notifySuccessRecipients
    String notifyFailureRecipients
    String notifySuccessUrl
    String notifyFailureUrl
    static transients = ['adhocExecutionType','notifySuccessRecipients','notifyFailureRecipients', 'notifySuccessUrl', 'notifyFailureUrl','crontabString']

    static constraints = {
        workflow(nullable:true)
        options(nullable:true)
        jobName(blank:false)
        groupPath(nullable:true)
        nextExecution(nullable:true)
        nodeKeepgoing(nullable:true)
        doNodedispatch(nullable:true)
        nodeInclude(nullable:true)
        nodeExclude(nullable:true)
        nodeIncludeName(nullable:true)
        nodeExcludeName(nullable:true)
        nodeIncludeTags(nullable:true)
        nodeExcludeTags(nullable:true)
        nodeIncludeOsName(nullable:true)
        nodeExcludeOsName(nullable:true)
        nodeIncludeOsFamily(nullable:true)
        nodeExcludeOsFamily(nullable:true)
        nodeIncludeOsArch(nullable:true)
        nodeExcludeOsArch(nullable:true)
        nodeIncludeOsVersion(nullable:true)
        nodeExcludeOsVersion(nullable:true)
        nodeExcludePrecedence(nullable:true)
        user(nullable:true)
        userRoleList(nullable:true)
        loglevel(nullable:true)
        totalTime(nullable:true)
        execCount(nullable:true)
        nodeThreadcount(nullable:true)
        project(nullable:false,blank:false)
        argString(nullable:true)
        seconds(nullable:true)
        year(nullable:true)
        description(nullable:true)
        adhocExecution(nullable:true)
        adhocRemoteString(nullable:true, blank:true)
        adhocLocalString(nullable:true, blank:true)
        adhocFilepath(nullable:true, blank:true)
        uuid(unique: true, nullable:true, blank:false)
    }
    static mapping = {
        def config = ConfigurationHolder.config
        if (config.rundeck.v14.rdbsupport == 'true') {
            user column: "rduser"
            argString type: 'text'
            
            notifySuccessRecipients type: 'text'
            notifyFailureRecipients type: 'text'
            notifySuccessUrl type: 'text'
            notifyFailureUrl type: 'text'
        }
    }


    public static final daysofweeklist = ['SUN','MON','TUE','WED','THU','FRI','SAT'];
    public static final monthsofyearlist = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];

    String toString() { "$jobName - $description" }
    Map toMap(){
        HashMap map = new HashMap()
        map.name=jobName
        if(groupPath){
            map.group=groupPath
        }
        if(uuid){
            map.uuid=uuid
            map.id=uuid
        }else if (id) {
            map.id = id
        }
        map.description=description
        map.loglevel=loglevel
        map.project=project

        if(options){
            map.options=[:]
            options.each{Option option->
                map.options[option.name]=option.toMap()
            }
        }

        map.sequence=workflow.toMap()

        if(scheduled){
            map.schedule=[time:[hour:hour,minute:minute,seconds:seconds],month:month,year:year]
            if(dayOfMonth!='?'){
                map.schedule.dayofmonth=[day:dayOfMonth ]
            }else{
                map.schedule.weekday=[day:dayOfWeek ]
            }
        }
        if(doNodedispatch){
            def yfilters=["":"hostname"]
            map.nodefilters=[dispatch:[threadcount:nodeThreadcount,keepgoing:nodeKeepgoing,excludePrecedence:nodeExcludePrecedence]]
            final Collection inclFilters = BaseNodeFilters.filterKeys.keySet().findAll {this["nodeInclude"+filterKeys[it]]}
            if(inclFilters){
                map.nodefilters.include=[:]
                inclFilters.each{ ek->
                    map.nodefilters.include[yfilters[ek]?:ek]=this["nodeInclude${filterKeys[ek]}"]
                }
            }
            final Collection exclFilters = BaseNodeFilters.filterKeys.keySet().findAll {this["nodeExclude"+filterKeys[it]]}
            if(exclFilters){
                map.nodefilters.exclude=[:]
                exclFilters.each{ ek->
                    map.nodefilters.exclude[yfilters[ek]?:ek]=this["nodeExclude${filterKeys[ek]}"]
                }
            }
        }
        if(notifications){
            map.notification=[:]
            notifications.each{
                if(!map.notification[it.eventTrigger]){
                    map.notification[it.eventTrigger]=[:]
                }
                map.notification[it.eventTrigger].putAll(it.toMap())
            }
        }
        return map
    }
    static ScheduledExecution fromMap(Map data){
        ScheduledExecution se = new ScheduledExecution()
        se.jobName=data.name
        se.groupPath=data['group']?data['group']:null
        se.description=data.description
        se.loglevel=data.loglevel?data.loglevel:'INFO'
        se.project=data.project
        if (data.uuid) {
            se.uuid = data.uuid
        }
        if(data.options){
            TreeSet options=new TreeSet()
            data.options.keySet().each{optname->
                Option opt = Option.fromMap(optname,data.options[optname])
                options<<opt
            }
            se.options=options
        }
        if(data.sequence){
            Workflow wf = Workflow.fromMap(data.sequence as Map)
            se.workflow=wf
        }
        if(data.schedule){
            se.scheduled=true
            if(data.schedule.crontab){
                    //
                se.crontabString = data.schedule.crontab
            }else{
                if(null!=data.schedule.time.seconds){
                    se.seconds=data.schedule.time.seconds
                }
                if(null!=data.schedule.time.minute){
                    se.minute=data.schedule.time.minute
                }
                if(null!=data.schedule.time.hour){
                    se.hour=data.schedule.time.hour
                }
                if(null!=data.schedule.month){
                    se.month=data.schedule.month
                }
                if(null!=data.schedule.year){
                    se.year=data.schedule.year
                }
                if(data.schedule.dayofmonth){
                    se.dayOfMonth = data.schedule.dayofmonth.day
                    se.dayOfWeek = '?'
                }else if(data.schedule.weekday){
                    se.dayOfWeek=data.schedule.weekday.day
                    se.dayOfMonth = '?'
                }
            }
        }
        if(data.nodefilters){

            se.nodeThreadcount = data.nodefilters.dispatch.threadcount
            if(data.nodefilters.dispatch.containsKey('keepgoing')){
                se.nodeKeepgoing = data.nodefilters.dispatch.keepgoing
            }
            if(data.nodefilters.dispatch.containsKey('excludePrecedence')){
                se.nodeExcludePrecedence = data.nodefilters.dispatch.excludePrecedence
            }
            if(data.nodefilters.include){
                se.doNodedispatch = true
                data.nodefilters.include.keySet().each{ inf->
                    if(null!=filterKeys[inf]){
                        se["nodeInclude${filterKeys[inf]}"]=data.nodefilters.include[inf]
                    }
                }

            }
            if(data.nodefilters.exclude){
                se.doNodedispatch = true
                data.nodefilters.exclude.keySet().each{ inf->
                    if(null!=filterKeys[inf]){
                        se["nodeExclude${filterKeys[inf]}"]=data.nodefilters.exclude[inf]
                    }
                }
            }
        }
        if(data.notification){
            def nots=[]
            ['onsuccess','onfailure'].each{ name->
                if(data.notification[name]){
                    ['urls','recipients'].each{ subkey->
                        if(data.notification[name][subkey]){
                            nots << Notification.fromMap(name, [(subkey):data.notification[name][subkey]])
                        }
                    }
                }
            }
            se.notifications=nots
        }
        return se
    }

    public clearFilterFields(){
        this.doNodedispatch = false
        filterKeys.keySet().each{ k->
            this["nodeInclude${filterKeys[k]}"]=null
        }
        filterKeys.keySet().each{ k->
            this["nodeExclude${filterKeys[k]}"]=null
        }
    }

    public setUserRoles(List l){
        setUserRoleList(l.join(","))
    }
    public List getUserRoles(){
        if(userRoleList){
            return Arrays.asList(userRoleList.split(/,/))
        }else{
            return []
        }
    }


    def String generateJobScheduledName(){
        return [id,jobName].join(":")
    }
     // generate a Quartz jobGroupName identification string suitable for use with the scheduler
    def String generateJobGroupName() {
        return [project, jobName,groupPath?groupPath: ''].join(":")
    }

    // various utility methods to the process crontab entry data
    def String generateCrontabExression() {
        return [seconds?seconds:'0',minute,hour,dayOfMonth.toUpperCase(),month.toUpperCase(),dayOfMonth=='?'?dayOfWeek.toUpperCase():'?',year?year:'*'].join(" ")
    }

    /**
     * Return full name with group path
     */
    def String generateFullName(){
        return generateFullName(groupPath,jobName)
    }


    /**
     * Return full name for group and path
     * @param group group path, no leading or trailing slash character
     * @param jobname job name
     */
    static String generateFullName(String group,String jobname){
        return [group?:'',jobname].join("/")
    }

    /**
     * attempt to parse the string into 6-7 components, and fill the properties
     * of the ScheduledExecution appropriately.  Returns false if the size
     * is wrong
     */
    def boolean parseCrontabString(String crontabString){
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


    /**
     * Return true if the schedule properties have values that mean it
     * should be modified as a crontab string instead of using a simplified form
     */
    def boolean shouldUseCrontabString(){
        if('0'!=seconds || '*'!=year || [minute,hour,dayOfMonth,dayOfWeek].find {crontabSpecialValue(it)} || crontabSpecialMonthValue(month)){
            return true
        }
        return false
    }



    /**
    * Return true if the crontab item string uses special crontab chars
     */
    public static boolean crontabSpecialValue(String str){
        if(str=~'[-/#]|[LW]$'){
            return true;
        }
        return false
    }


    /**
    * Return tru if the crontab month string uses special crontab chars
     */
    public static boolean crontabSpecialMonthValue(String str){
        if(str=~'[-/]'){
            return true;
        }
        return false
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
        def daysOfMonth=params.dayOfMonth?params.dayOfMonth:'?'

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
            daysOfWeek= parseCheckboxFieldFromParams("dayOfWeek",params,daysOfMonth=='?',daysofweeklist)
        }
        def everyMonth = params['everyMonth']
        if((everyMonth instanceof Boolean && everyMonth ) || (everyMonth instanceof String && (everyMonth=="true" || everyMonth=="on"))){
            months="*"
        }else{
            months= parseCheckboxFieldFromParams("month",params,true,monthsofyearlist)
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
    def Map filterCrontabParams(String field, Map params) {
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


        def parseRangeForList = {String input, List list, key ->
            def rangpat = /^(.+)-(.+)$/
            def aprop = [:]
            def inputl = Arrays.asList(input.split(','))
            def uplist = list.collect {it.toUpperCase()}
            inputl.each {String dayv ->
                def mat = dayv =~ rangpat
                if (mat.matches()) {
                    String m1 = mat.group(1)
                    String m2 = mat.group(2)

                    def a = uplist.indexOf(m1.toUpperCase())
                    def b = uplist.indexOf(m2.toUpperCase())
                    //increment index if found
                    if(a>=0){
                        a++
                    }
                    if(b>=0){
                        b++
                    }

                    if(a<0 || b<0){
                        try{
                            a=m1.toInteger()
                            b=m2.toInteger()
                        }catch(NumberFormatException){

                        }
                    }
                    if(a>0 && b>0 && a<=list.size() && b<=list.size()){
                        def rang = a..b
                        rang.each {
                            def name = list[it - 1]
                            aprop["${key}.${name}"] = "true"
                        }
                    }
                } else {
                    def a = uplist.indexOf(dayv.toUpperCase())
                    //increment index if found
                    if(a>=0){
                        a++
                    }
                    if(a<0){
                        try{
                        a = dayv.toInteger()
                        }catch(NumberFormatException e){

                        }
                    }
                    if (a > 0 && a <= list.size()) {
                        def name = list[a - 1]
                        aprop["${key}.${name}"] = "true"
                    }
                }
            }
            return aprop
        }
    
  def Map timeAndDateAsBooleanMap() {
      def result = [ : ]
      if (!this.month.equals("*") && !crontabSpecialValue(this.month.replaceAll(/-/,''))) {
          def map = parseRangeForList(this.month,monthsofyearlist,"month")
          result.putAll(map)
//          this.month.split(",").each {
//              if(monthsofyearlist.contains(it.toUpperCase())){
//                result["month."+it.toUpperCase()]="true"
//              }else if(it=~/^\d+$/){
//                  def i=Integer.parseInt(it)
//                  if(i>=0&&i<monthsofyearlist.size()){
//                      result["month."+monthsofyearlist[i]]="true"
//                  }
//              }
//          }
      }
      if (!this.dayOfWeek.equals("*") && !crontabSpecialValue(this.dayOfWeek.replaceAll(/-/,''))) {
          def map = parseRangeForList(this.dayOfWeek,daysofweeklist,"dayOfWeek")
          result.putAll(map)
//          this.dayOfWeek.split(",").each {
//              if(daysofweeklist.contains(it.toUpperCase())){
//                result["dayOfWeek."+it.toUpperCase()]="true"
//              }else if(it=~/^\d+$/){
//                  def i=Integer.parseInt(it)
//                  if(i>=0&&i<daysofweeklist.size()) {
//                      result["dayOfWeek." + daysofweeklist[i]] = "true"
//                  }
//              }
//          }
      }
      return result;
  }

    def Notification findNotification(String trigger, String type){
        if(this.notifications){
            return this.notifications.find{it.eventTrigger==trigger && it.type==type}
        }else{
            return null
        }
    }

    def getExtid(){
        return this.uuid?:this.id.toString()
    }
}


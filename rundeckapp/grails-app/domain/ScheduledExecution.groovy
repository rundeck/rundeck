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

    Date nextExecution
    boolean scheduled = false
    Long totalTime=0
    Long execCount=0
    String adhocExecutionType
    Date dateCreated
    Date lastUpdated
    String notifySuccessRecipients
    String notifyFailureRecipients
    static transients = ['adhocExecutionType','notifySuccessRecipients','notifyFailureRecipients','crontabString']

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
        nodeIncludeType(nullable:true)
        nodeExcludeType(nullable:true)
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
        description(nullable:false,blank:true)
        adhocExecution(nullable:true)
        adhocRemoteString(nullable:true, blank:true)
        adhocLocalString(nullable:true, blank:true)
        adhocFilepath(nullable:true, blank:true)
    }

    public static final daysofweeklist = ['MON','TUE','WED','THU','FRI','SAT','SUN'];
    public static final monthsofyearlist = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];

    String toString() { "$jobName - $description" }

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

  def Map timeAndDateAsBooleanMap() {
      def result = [ : ]
      if (!this.month.equals("*") && !crontabSpecialValue(this.month)) {
          this.month.split(",").each {
              if(monthsofyearlist.contains(it.toUpperCase())){
                result["month."+it.toUpperCase()]="true"
              }else if(it=~/^\d+$/){
                  def i=Integer.parseInt(it)
                  if(i>=0&&i<monthsofyearlist.size()){
                      result["month."+monthsofyearlist[i]]="true"
                  }
              }
          }
      }
      if (!this.dayOfWeek.equals("*") && !crontabSpecialValue(this.dayOfWeek)) {
          this.dayOfWeek.split(",").each {
              if(daysofweeklist.contains(it.toUpperCase())){
                result["dayOfWeek."+it.toUpperCase()]="true"
              }else if(it=~/^\d+$/){
                  def i=Integer.parseInt(it)
                  if(i>=0&&i<daysofweeklist.size()) {
                      result["month." + daysofweeklist[i]] = "true"
                  }
              }
          }
      }
      return result;
  }

    def Notification findNotification(String trigger){
        if(this.id){
            return Notification.findByScheduledExecutionAndEventTrigger(this,trigger)
        }else{
            return null
        }
    }
}


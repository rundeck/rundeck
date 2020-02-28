/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck

import com.dtolabs.rundeck.app.domain.EmbeddedJsonData
import com.dtolabs.rundeck.app.support.DomainIndexHelper
import com.dtolabs.rundeck.app.support.ExecutionContext
import com.dtolabs.rundeck.core.common.FrameworkResource
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.plugins.PluginConfigSet
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.JobOptionImpl
import com.google.gson.Gson
import groovy.json.JsonOutput
import org.quartz.Calendar
import org.quartz.TriggerUtils
import org.quartz.impl.calendar.BaseCalendar
import org.rundeck.util.Sizes

class ScheduledExecution extends ExecutionContext implements EmbeddedJsonData {
    static final String RUNBOOK_MARKER='---'
    Long id
    SortedSet<Option> options
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
    String logOutputThreshold;
    String logOutputThresholdAction;
    String logOutputThresholdStatus;

    Workflow workflow

    Date nextExecution
    boolean scheduled = false
    Boolean nodesSelectedByDefault = true
    Long totalTime=0
    Long execCount=0
    String adhocExecutionType
    Date dateCreated
    Date lastUpdated
    String notifySuccessRecipients
    String notifyFailureRecipients
    String notifyStartRecipients
    String notifySuccessUrl
    String notifyFailureUrl
    String notifyStartUrl
    String notifyAvgDurationRecipients
    String notifyAvgDurationUrl
    String notifyRetryableFailureRecipients
    String notifyRetryableFailureUrl
    String notifySuccessAttach
    String notifyFailureAttach
    String notifyRetryableFailureAttach
    Boolean multipleExecutions = false
    Orchestrator orchestrator
    String serverNodeUUID

    String notifyAvgDurationThreshold

    String timeZone

    Boolean scheduleEnabled = true
    Boolean executionEnabled = true

    Integer nodeThreadcount=1
    String nodeThreadcountDynamic
    Long refExecCount=0

    String defaultTab

    String maxMultipleExecutions
    String pluginConfig

    static transients = ['userRoles', 'adhocExecutionType', 'notifySuccessRecipients', 'notifyFailureRecipients',
                         'notifyStartRecipients', 'notifySuccessUrl', 'notifyFailureUrl', 'notifyStartUrl',
                         'crontabString', 'averageDuration', 'notifyAvgDurationRecipients', 'notifyAvgDurationUrl',
                         'notifyRetryableFailureRecipients', 'notifyRetryableFailureUrl', 'notifyFailureAttach',
                         'notifySuccessAttach', 'notifyRetryableFailureAttach',
                         'pluginConfigMap']

    static constraints = {
        project(nullable:false, blank: false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
        workflow(nullable:true)
        options(nullable:true)
        jobName(blank: false, nullable: false, matches: "[^/]+", maxSize: 1024)
        groupPath(nullable:true, maxSize: 2048)
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
        filter(nullable:true)
        user(nullable:true)
        userRoleList(nullable:true)
        loglevel(nullable:true)
        totalTime(nullable:true)
        execCount(nullable:true)
        nodeThreadcount(nullable:true)
        refExecCount(nullable:true)
        nodeRankOrderAscending(nullable:true)
        nodeRankAttribute(nullable:true)
        argString(nullable:true)
        seconds(nullable: true, matches: /^[0-9*\/,-]*$/)
        minute(nullable:true, matches: /^[0-9*\/,-]*$/ )
        hour(nullable:true, matches: /^[0-9*\/,-]*$/ )
        dayOfMonth(nullable:true, matches: /^[0-9*\/,?LW-]*$/ )
        month(nullable:true, matches: /^[0-9a-zA-z*\/,-]*$/ )
        dayOfWeek(nullable:true, matches: /^[0-9a-zA-z*\/?,L#-]*$/ )
        year(nullable:true, matches: /^[0-9*\/,-]*$/)
        description(nullable:true)
        uuid(unique: true, nullable:true, blank:false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
        orchestrator(nullable:true)
        multipleExecutions(nullable: true)
        serverNodeUUID(maxSize: 36, size: 36..36, blank: true, nullable: true, validator: { val, obj ->
            if (null == val) return true;
            try { return null != UUID.fromString(val) } catch (IllegalArgumentException e) {
                return false
            }
        })
        timeout(maxSize: 256, blank: true, nullable: true,)
        retry(maxSize: 256, blank: true, nullable: true,validator: { val, obj ->
            if (null == val) return true;
            if (val.indexOf('${')>=0) return true;
            try { return Integer.parseInt(val)>=0 } catch (NumberFormatException e) {
                return false
            }
        })
        crontabString(bindable: true,nullable: true)
        nodesSelectedByDefault(nullable: true)
        scheduleEnabled(nullable: true)
        executionEnabled(nullable: true)
        nodeFilterEditable(nullable: true)
        logOutputThreshold(maxSize: 256, blank:true, nullable: true)
        logOutputThresholdAction(maxSize: 256, blank:true, nullable: true,inList: ['halt','truncate'])
        logOutputThresholdStatus(maxSize: 256, blank:true, nullable: true)
        timeZone(maxSize: 256, blank: true, nullable: true)
        retryDelay(nullable:true)
        successOnEmptyNodeFilter(nullable: true)
        nodeThreadcountDynamic(nullable: true)
        notifyAvgDurationThreshold(nullable: true)
        defaultTab(maxSize: 256, blank: true, nullable: true)
        maxMultipleExecutions(maxSize: 256, blank: true, nullable: true)
        pluginConfig(nullable: true)
    }

    static mapping = {
        user column: "rduser"
        nodeInclude(type: 'text')
        nodeExclude(type: 'text')
        nodeIncludeName(type: 'text')
        nodeExcludeName(type: 'text')
        nodeIncludeTags(type: 'text')
        nodeExcludeTags(type: 'text')
        nodeIncludeOsName(type: 'text')
        nodeExcludeOsName(type: 'text')
        nodeIncludeOsFamily(type: 'text')
        nodeExcludeOsFamily(type: 'text')
        nodeIncludeOsArch(type: 'text')
        nodeExcludeOsArch(type: 'text')
        nodeIncludeOsVersion(type: 'text')
        nodeExcludeOsVersion(type: 'text')
        filter(type: 'text')
        userRoleList(type: 'text')
        jobName type: 'string'
        argString type: 'text'
        description type: 'text'
        groupPath type: 'string'
//        orchestrator type: 'text'
        //options lazy: false
        timeout(type: 'text')
        retry(type: 'text')
        retryDelay(type: 'text')
        notifyAvgDurationThreshold(type: 'text')
        serverNodeUUID(type: 'string')
        pluginConfig(type: 'text')

        DomainIndexHelper.generate(delegate) {
            index 'JOB_IDX_PROJECT', ['project']
        }
    }

    static namedQueries = {
		scheduledJobs {
			eq 'scheduled', true
		}
		withServerUUID { uuid ->
			eq 'serverNodeUUID', uuid
		}
		withoutServerUUID { uuid ->
			ne 'serverNodeUUID', uuid
		}
		withAdHocScheduledExecutions {
			executions {
				eq 'status', 'scheduled'
			}
		}
        withProject { project ->
            eq 'project', project
        }
    }


    public static final daysofweeklist = ['SUN','MON','TUE','WED','THU','FRI','SAT'];
    public static final monthsofyearlist = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];

    String toString() { generateFullName()+" - $description" }

    Map getPluginConfigMap() {
        pluginConfig ? asJsonMap(pluginConfig) : [:]
    }


    void setPluginConfigMap(Map config) {
        pluginConfig = config ? serializeJsonMap(config) : null
    }

    void setPluginConfigVal(String key, Object val) {
        Map map = pluginConfigMap
        map[key] = val
        setPluginConfigMap(map)
    }

    Map refreshOptions(){

        HashMap map = new HashMap()
        if(options){
            map.options = []
            options.sort().each{Option option->

                def map1 = option.toMap()
                map1.remove('sortIndex')
                map.options.add(map1 + [name: option.name])
            }
        }
        return map
    }

    Map toMap(){
        HashMap map = new HashMap()
        map.name=jobName

        map.scheduleEnabled = hasScheduleEnabled()
        map.executionEnabled = hasExecutionEnabled()
        map.nodeFilterEditable = hasNodeFilterEditable()

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
        if(logOutputThreshold){
            map.loglimit=logOutputThreshold
            map.loglimitAction=logOutputThresholdAction
            if(logOutputThresholdStatus){
                map.loglimitStatus=logOutputThresholdStatus
            }
        }
        //don't include project
        if(timeout){
            map.timeout=timeout
        }

        if(retry && retryDelay){
            map.retry = [retry:retry, delay: retryDelay]
        }else if(retry){
            map.retry=retry
        }
        if(orchestrator){
            map.orchestrator=orchestrator.toMap();
        }
        if(timeZone){
            map.timeZone=timeZone
        }
        if(defaultTab){
            map.defaultTab = defaultTab
        }

        if(options){
            map.options = []
            options.sort().each{Option option->

                def map1 = option.toMap()
                map1.remove('sortIndex')
                map.options.add(map1 + [name: option.name])
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
        if(multipleExecutions){
            map.multipleExecutions=true
        }
        if(maxMultipleExecutions){
            map.maxMultipleExecutions = maxMultipleExecutions
        }
        if(doNodedispatch){
            map.nodesSelectedByDefault = hasNodesSelectedByDefault()
            map.nodefilters=[dispatch:[threadcount:rawThreadCountValue(),
                                       keepgoing:nodeKeepgoing?true:false,
                                       successOnEmptyNodeFilter:successOnEmptyNodeFilter?true:false,
                                       excludePrecedence:nodeExcludePrecedence?true:false]]
            if(nodeRankAttribute){
                map.nodefilters.dispatch.rankAttribute= nodeRankAttribute
            }
            map.nodefilters.dispatch.rankOrder= (null== nodeRankOrderAscending || nodeRankOrderAscending)?'ascending':'descending'
            if(this.filter){
                map.nodefilters.filter = this.filter
            }else{
                map.nodefilters.filter = asFilter()
            }

            if(this.filterExclude){
                map.nodefilters.filterExclude = this.filterExclude

                if(this.excludeFilterUncheck){
                    map.excludeFilterUncheck = true
                }
            }
        }
        if(notifications){
            map.notification=[:]
            notifications.each{
                if(!map.notification[it.eventTrigger]){
                    map.notification[it.eventTrigger]=[:]
                }
                def trigger= map.notification[it.eventTrigger]
                def map1 = it.toMap()
                if(map1.type){
                    //plugin notification with a type
                    if(!trigger['plugin']){
                        trigger['plugin']=map1
                    }else if(trigger['plugin'] instanceof Map){
                        trigger['plugin']=[trigger.remove('plugin'),map1]
                    }else if(trigger['plugin'] instanceof Collection){
                        trigger['plugin'] << map1
                    }
                }else{
                    //built-in notification, urls or recipients subelements
                    trigger.putAll(map1)
                }
            }
            notifications.each {
                if (map.notification[it.eventTrigger].plugin instanceof Collection) {
                    map.notification[it.eventTrigger].plugin =
                            map.notification[it.eventTrigger].plugin.sort { a, b -> a.type <=> b.type }
                }
            }

            map.notifyAvgDurationThreshold = notifyAvgDurationThreshold
        }
        def config = pluginConfigMap
        if (config) {
            map.plugins = config
        }
        return map
    }
    static ScheduledExecution fromMap(Map data){
        ScheduledExecution se = new ScheduledExecution()
        se.jobName=data.name
        se.groupPath=data['group']?data['group']:null
        se.description=data.description
        if(data.orchestrator){
            se.orchestrator=Orchestrator.fromMap(data.orchestrator);
        }
        
        se.scheduleEnabled = data['scheduleEnabled'] == null || data['scheduleEnabled']
        se.executionEnabled = data['executionEnabled'] == null || data['executionEnabled']
        se.nodeFilterEditable = data['nodeFilterEditable'] == null || data['nodeFilterEditable']
        se.excludeFilterUncheck = data.excludeFilterUncheck?data.excludeFilterUncheck:false
        
        se.loglevel=data.loglevel?data.loglevel:'INFO'

        if(data.loglimit){
            se.logOutputThreshold=data.loglimit
            se.logOutputThresholdAction = data.loglimitAction
            se.logOutputThresholdStatus = data.loglimitStatus?:'failed'
        }
        se.project=data.project
        if (data.uuid) {
            se.uuid = data.uuid
        }
        se.timeout = data.timeout?data.timeout.toString():null
        if(data.retry instanceof Map){
            se.retry = data.retry.retry?.toString()
            se.retryDelay = data.retry.delay?.toString()
        }else{
            se.retry = data.retry?.toString()
            se.retryDelay = data.retryDelay?.toString()
        }
        se.timeZone = data.timeZone?data.timeZone.toString():null
        se.defaultTab = data.defaultTab?data.defaultTab.toString():null
        if(data.options){
            TreeSet options=new TreeSet()
            if(data.options instanceof Map) {
                data.options.keySet().each { optname ->
                    Option opt = Option.fromMap(optname, data.options[optname])
                    options << opt
                    opt.scheduledExecution=se
                }
            }else if(data.options instanceof Collection){
                int sortIndex=0
                data.options.each { optdata ->
                    Option opt = Option.fromMap(optdata.name, optdata)
                    opt.sortIndex=sortIndex++
                    options << opt
                    opt.scheduledExecution=se
                }
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
                se.parseCrontabString(data.schedule.crontab)
            }else{
                if(data.schedule.time && data.schedule.time instanceof Map){
                    if(null!=data.schedule.time.seconds){
                        se.seconds=data.schedule.time.seconds
                    }
                    if(null!=data.schedule.time.minute){
                        se.minute=data.schedule.time.minute
                    }
                    if(null!=data.schedule.time.hour){
                        se.hour=data.schedule.time.hour
                    }
                }
                if(null!=data.schedule.month){
                    se.month=data.schedule.month
                } else {
                    se.month = '*'
                }
                if(null!=data.schedule.year){
                    se.year=data.schedule.year
                } else {
                    se.year = '*'
                }
                if(data.schedule.dayofmonth && data.schedule.dayofmonth instanceof Map
                        && null !=data.schedule.dayofmonth.day && '?' !=data.schedule.dayofmonth.day){
                    se.dayOfMonth = data.schedule.dayofmonth.day
                    se.dayOfWeek = '?'
                }else if(data.schedule.weekday && data.schedule.weekday instanceof Map
                        && null!=data.schedule.weekday.day){
                    se.dayOfWeek=data.schedule.weekday.day
                    se.dayOfMonth = '?'
                }else{
                    se.dayOfMonth='?'
                    se.dayOfWeek='*'
                }
            }
        }
        if(data.multipleExecutions){
            se.multipleExecutions=data.multipleExecutions?true:false
        }
        if(data.maxMultipleExecutions){
            se.maxMultipleExecutions=data.maxMultipleExecutions
        }
        if(data.nodefilters){
            se.nodesSelectedByDefault = null!=data.nodesSelectedByDefault?(data.nodesSelectedByDefault?true:false):true
            if(data.nodefilters.dispatch){
                se.nodeThreadcountDynamic = data.nodefilters.dispatch.threadcount ?: "1"
                if(data.nodefilters.dispatch.containsKey('keepgoing')){
                    se.nodeKeepgoing = data.nodefilters.dispatch.keepgoing
                }
                if(data.nodefilters.dispatch.containsKey('excludePrecedence')){
                    se.nodeExcludePrecedence = data.nodefilters.dispatch.excludePrecedence
                }
                if(data.nodefilters.dispatch.containsKey('rankAttribute')){
                    se.nodeRankAttribute = data.nodefilters.dispatch.rankAttribute
                }
                if(data.nodefilters.dispatch.containsKey('rankOrder')){
                    se.nodeRankOrderAscending = data.nodefilters.dispatch.rankOrder=='ascending'
                }
                if(data.nodefilters.dispatch.containsKey('successOnEmptyNodeFilter')){
                    se.successOnEmptyNodeFilter = data.nodefilters.dispatch.successOnEmptyNodeFilter
                }
            }
            if(data.nodefilters.filter){
                se.doNodedispatch=true
                se.filter= data.nodefilters.filter
            }else{
                def map = [include: [:], exclude: [:]]
                if (data.nodefilters.include) {
                    se.doNodedispatch = true
                    data.nodefilters.include.keySet().each { inf ->
                        if (null != filterKeys[inf]) {
                            map.include[inf] = data.nodefilters.include[inf]
                        }
                    }

                }
                if (data.nodefilters.exclude) {
                    se.doNodedispatch = true
                    data.nodefilters.exclude.keySet().each { inf ->
                        if (null != filterKeys[inf]) {
                            map.exclude[inf] = data.nodefilters.exclude[inf]
                        }
                    }
                }
                se.filter = asFilter(map)
            }

            if(data.nodefilters.filterExclude){
                se.filterExclude= data.nodefilters.filterExclude
            }
        }
        if(data.notification){
            def nots=[]
            data.notification.keySet().findAll{it.startsWith('on')}.each{ name->
                if(data.notification[name]){
                        //support for built-in notification types
                    ['urls','email'].each{ subkey->
                        if(data.notification[name][subkey]){
                            nots << Notification.fromMap(name, [(subkey):data.notification[name][subkey]])
                        }
                    }
                    if(data.notification[name]['plugin']){
                        def pluginElement=data.notification[name]['plugin']
                        def plugins=[]
                        if(pluginElement instanceof Map){
                            plugins=[pluginElement]
                        }else if(pluginElement instanceof Collection){
                            plugins= pluginElement
                        }else{

                        }
                        plugins.each{ plugin->
                            def n=Notification.fromMap(name, plugin)
                            if(n){
                                nots << n
                            }
                        }
                    }
                }
            }
            se.notifications=nots

            if(null!=data.notifyAvgDurationThreshold){
                se.notifyAvgDurationThreshold = data.notifyAvgDurationThreshold
            }

        }
        if (data.plugins instanceof Map) {
            se.pluginConfigMap = data.plugins
        }
        return se
    }

    /**
     * @return boolean value for nodesSelectedByDefault, defaults to true if null
     */
    public boolean hasNodesSelectedByDefault() {
        null == nodesSelectedByDefault || nodesSelectedByDefault
    }

    /**
     * Parse the logOutputThreshold setting
     * @return map indicating the threshold values: [perNode:true/false, maxLines:Long, maxSizeBytes:Long]
     */
    public static Map parseLogOutputThreshold(String logOutputThreshold){
        def map = null
        def units = [g: 1024 * 1024 * 1024, k: 1024, m: 1024 * 1024, b: 1]
        if (logOutputThreshold) {
            def m = logOutputThreshold =~ /(\d+)((?i)[gmk]?b?)?(\/node)?/
            if (m.matches()) {
                def count = m.group(1)
                def unit = m.group(2)
                def node = m.group(3)
                def multi = unit ? units[unit[0]?.toLowerCase()] ?: 1 : 1
                def value = 0
                try {
                    value = Long.parseLong(count) * multi
                } catch (NumberFormatException e) {
                    return null
                }
                if (unit) {

                    map = [
                            maxSizeBytes: value
                    ]
                } else {
                    map = [
                            perNode : node == '/node',
                            maxLines: value
                    ]
                }
            }
        }
        map
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
        def json = JsonOutput.toJson(l)
        setUserRoleList(json)
    }

    public List getUserRoles(){
        if(userRoleList){
            //check if the string is a valid JSON
            try {
                Gson gson = new Gson()
                return gson.fromJson(userRoleList, List.class)
            } catch(com.google.gson.JsonSyntaxException ex) {
                return Arrays.asList(userRoleList.split(/,/))
            }

        }else{
            return []
        }
    }

    def boolean hasScheduleEnabled() {
        return (null == scheduleEnabled || scheduleEnabled)
    }

    def boolean shouldScheduleExecution() {
        return scheduled && hasExecutionEnabled() && hasScheduleEnabled()
    }

    def boolean hasExecutionEnabled() {
        return (null == executionEnabled || executionEnabled)
    }

    def boolean hasNodeFilterEditable() {
        return (null == nodeFilterEditable || nodeFilterEditable)
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
        if ('0' != seconds
                || '*' != year
                || ('*' in [minute, hour])
                || [minute, hour].any { it.contains(',') }
                || [minute, hour, dayOfMonth, dayOfWeek].any { crontabSpecialValue(it) }
                || crontabSpecialMonthValue(month)) {
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
    public static String zeroPaddedString(int max,String value){
        if(value && value=~/^\d+$/){
            return String.format("%0${max}d",Integer.parseInt(value))
        }
        return value;
    }
    /**
     * Return evaluated timeout duration, or -1 if not set
     * @return
     */
    public long getTimeoutDuration(){
        timeout? Sizes.parseTimeDuration(timeout):-1
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
            crontabString = params.crontabString
            parseCrontabString(crontabString)
            return
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

    /**
     * Find all ScheduledExecutions with the given group, name and project
     * @param group
     * @param name
     * @param project
     * @return
     */
    static List findAllScheduledExecutions(String group, String name, String project){
        def c = ScheduledExecution.createCriteria()
        def schedlist = c.list {
            and {
                eq('jobName', name)
                if (!group) {
                    or {
                        eq('groupPath', '')
                        isNull('groupPath')
                    }
                } else {
                    eq('groupPath', group)
                }
                eq('project', project)
            }
        }
        return schedlist
    }

    /**
     * Find all ScheduledExecutions with the given uuid
     * @param uuid
     * @return
     */
    static List findAllScheduledExecutions(String uuid){
        def c = ScheduledExecution.createCriteria()
        def schedlist = c.list {
            and {
                eq('uuid', uuid)
            }
        }
        return schedlist
    }

    /**
     * Find a ScheduledExecution by UUID or ID.  Checks if the
     * input value is a Long, if so finds the ScheduledExecution with that ID.
     * If it is a String it attempts to parse the String as a Long and if it is
     * valid it finds the ScheduledExecution by ID. Otherwise it attempts to find the ScheduledExecution with that
     * UUID.
     * @param anid
     * @return ScheduledExecution found or null
     */
    static ScheduledExecution getByIdOrUUID(anid){
        def found = null
        if (anid instanceof Long) {
            return ScheduledExecution.get(anid)
        } else if (anid instanceof String) {
            //attempt to parse as long id
            try {
                def long idlong = Long.parseLong(anid)
                found = ScheduledExecution.get(idlong)
            } catch (NumberFormatException e) {
            }
            if (!found) {
                found = ScheduledExecution.findByUuid(anid)
            }
        }
        return found
    }

    /**
     * Find the only ScheduledExecution with the given group, name and project
     * @param group
     * @param name
     * @param project
     * @return
     */
    static ScheduledExecution findScheduledExecution(String group, String name, String project, String extid=null) {
        def schedlist
        if(extid){
            schedlist = ScheduledExecution.findAllByUuid(extid)
        }else{
            schedlist = ScheduledExecution.findAllScheduledExecutions(group,name,project)
        }
        if(schedlist && 1 == schedlist.size()){
            return schedlist[0]
        }else{
            return null
        }
    }

    /**
     * Return the defined option with the given name, if available
     * @param name
     * @return
     */
    Option findOption(String name) {
        options.find { it.name == name }
    }
    /**
     * Return the defined option with the given name, if available
     * @param name
     * @return
     */
    List<Option> listFileOptions() {
        options.findAll { it.typeFile } as List
    }

    long getAverageDuration() {
        def stats = getStats()
        def statsContent= stats?.getContentMap()
        if (statsContent && statsContent.totalTime && statsContent.execCount) {
            return Math.floor(statsContent.totalTime / statsContent.execCount)
        }
        return 0;
    }

    //new threadcount value that can be defined using an option value
    Integer getNodeThreadcount() {
        if(null!=nodeThreadcount && null==nodeThreadcountDynamic){
            return nodeThreadcount
        }

        def nodeThreadcountValue=nodeThreadcountDynamic

        if (nodeThreadcountDynamic?.contains('${')) {
            //replace data references
            if (options) {
                def defaultoptions=[:]
                options.each {Option opt ->
                    if (opt.defaultValue) {
                        defaultoptions[opt.name]=opt.defaultValue
                    }
                }

                nodeThreadcountValue = DataContextUtils.replaceDataReferencesInString(nodeThreadcountDynamic, DataContextUtils.addContext("option", defaultoptions, null)).trim()
            }
        }

        if(null!=nodeThreadcountValue){
            if(nodeThreadcountValue.isInteger()){
                return Integer.valueOf(nodeThreadcountValue)
            }else{
                return null
            }
        }else{
            return null
        }

    }

    String rawThreadCountValue() {
        if(null!=nodeThreadcount && null==nodeThreadcountDynamic){
            return nodeThreadcount.toString()
        }else{
            if(null==nodeThreadcountDynamic){
                return "1"
            }else{
                return nodeThreadcountDynamic
            }
        }
    }

    ScheduledExecutionStats getStats() {
        def stats
        if(this.id) {
            stats = ScheduledExecutionStats.findBySe(this)
            if (!stats) {
                def content = [execCount   : this.execCount,
                               totalTime   : this.totalTime,
                               refExecCount: this.refExecCount]

                stats = new ScheduledExecutionStats(se: this, contentMap: content).save()
            }
        }
        stats
    }

    Long getRefExecCountStats(){
        def stats = this.getStats()
        def statsContent= stats?.getContentMap()
        if (statsContent?.refExecCount) {
            return statsContent.refExecCount
        }
        return 0;
    }

    Long getTotalTimeStats(){
        def stats = this.getStats()
        def statsContent= stats?.getContentMap()
        if (statsContent?.totalTime) {
            return statsContent.totalTime
        }
        return 0;
    }

    Long getExecCountStats(){
        def stats = this.getStats()
        def statsContent= stats?.getContentMap()
        if (statsContent?.execCount) {
            return statsContent.execCount
        }
        return 0;
    }

    /**
     *
     * @return model values
     */
    SortedSet<JobOption> jobOptionsSet() {
        new TreeSet<>(options.collect{it.toJobOption()})
    }
}


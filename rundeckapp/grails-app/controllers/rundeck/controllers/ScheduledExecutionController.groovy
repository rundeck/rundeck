package rundeck.controllers

import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.app.api.ApiBulkJobDeleteRequest
import com.dtolabs.rundeck.app.api.ApiRunAdhocRequest
import com.dtolabs.rundeck.app.support.ExtraCommand
import com.dtolabs.rundeck.app.support.RunJobCommand
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.converters.JSON
import groovy.xml.MarkupBuilder
import org.apache.commons.collections.list.TreeList
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.params.HttpClientParams
import org.apache.commons.httpclient.params.HttpMethodParams
import org.apache.commons.httpclient.util.DateParseException
import org.apache.commons.httpclient.util.DateUtil
import org.apache.log4j.Logger
import org.apache.log4j.MDC
import org.codehaus.groovy.grails.web.json.JSONElement
import org.quartz.CronExpression
import org.quartz.Scheduler
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.commons.CommonsMultipartFile
import rundeck.*
import rundeck.codecs.JobsXMLCodec
import rundeck.codecs.JobsYAMLCodec
import rundeck.filters.ApiRequestFilters
import rundeck.services.*

import javax.servlet.http.HttpServletResponse
import java.util.regex.Pattern

class ScheduledExecutionController  extends ControllerBase{
    public static final String NOTIFY_ONSUCCESS_EMAIL = 'notifyOnsuccessEmail'
    public static final String NOTIFY_ONFAILURE_EMAIL = 'notifyOnfailureEmail'
    public static final String NOTIFY_ONSTART_EMAIL = 'notifyOnstartEmail'
    public static final String NOTIFY_START_RECIPIENTS = 'notifyStartRecipients'
    public static final String NOTIFY_START_SUBJECT = 'notifyStartSubject'
    public static final String NOTIFY_ONSUCCESS_URL = 'notifyOnsuccessUrl'
    public static final String NOTIFY_SUCCESS_URL = 'notifySuccessUrl'
    public static final String NOTIFY_FAILURE_RECIPIENTS = 'notifyFailureRecipients'
    public static final String NOTIFY_FAILURE_SUBJECT= 'notifyFailureSubject'
    public static final String NOTIFY_FAILURE_ATTACH= 'notifyFailureAttach'
    public static final String NOTIFY_SUCCESS_RECIPIENTS = 'notifySuccessRecipients'
    public static final String NOTIFY_SUCCESS_SUBJECT= 'notifySuccessSubject'
    public static final String NOTIFY_SUCCESS_ATTACH= 'notifySuccessAttach'
    public static final String NOTIFY_FAILURE_URL = 'notifyFailureUrl'
    public static final String NOTIFY_ONFAILURE_URL = 'notifyOnfailureUrl'
    public static final String NOTIFY_ONSTART_URL = 'notifyOnstartUrl'
    public static final String NOTIFY_START_URL = 'notifyStartUrl'
    public static final String ONSUCCESS_TRIGGER_NAME = 'onsuccess'
    public static final String ONFAILURE_TRIGGER_NAME = 'onfailure'
    public static final String ONSTART_TRIGGER_NAME = 'onstart'
    public static final String EMAIL_NOTIFICATION_TYPE = 'email'
    public static final String WEBHOOK_NOTIFICATION_TYPE = 'url'
    public static final ArrayList<String> NOTIFICATION_ENABLE_FIELD_NAMES = [
            NOTIFY_ONFAILURE_URL,
            NOTIFY_ONFAILURE_EMAIL,
            NOTIFY_ONSUCCESS_EMAIL,
            NOTIFY_ONSUCCESS_URL,
            NOTIFY_ONSTART_EMAIL,
            NOTIFY_ONSTART_URL
    ]

    def Scheduler quartzScheduler
    def ExecutionService executionService
    def FrameworkService frameworkService
    def ScheduledExecutionService scheduledExecutionService
    def OrchestratorPluginService orchestratorPluginService
	def NotificationService notificationService
    def ApiService apiService
    def UserService userService
    def ScmService scmService


    def index = { redirect(controller:'menu',action:'jobs',params:params) }

    // the delete, save and update actions only
    // accept POST requests
    def static allowedMethods = [
            delete: ['POST','GET'],
            deleteBulk: 'POST',
            flipExecutionDisabledBulk:'POST',
            flipExecutionEnabledBulk:'POST',
            flipScheduleDisabledBulk:'POST',
            flipScheduleEnabledBulk:'POST',
            flipScheduleEnabled:'POST',
            flipExecutionEnabled: 'POST',
            runJobInline: 'POST',
            runJobNow: 'POST',
            runAdhocInline: 'POST',
            save: 'POST',
            saveAndExec: 'POST',
            update: 'POST',
            upload: 'GET',
            uploadPost: ['POST'],
            apiFlipExecutionEnabled: 'POST',
            apiFlipExecutionEnabledBulk: 'POST',
            apiFlipScheduleEnabled: 'POST',
            apiFlipScheduleEnabledBulk: 'POST',
            apiJobCreateSingle: 'POST',
            apiJobRun: ['POST','GET'],
            apiJobsImport: 'POST',
            apiJobsImportv14: 'POST',
            apiJobDelete: 'DELETE',
            apiRunScript: 'POST',
            apiRunScriptv14: 'POST',
            apiRunScriptUrl: ['POST','GET'],
            apiRunScriptUrlv14: ['POST','GET'],
            apiRunCommand: ['POST','GET'],
            apiRunCommandv14: ['POST','GET'],
            apiJobDeleteBulk: ['DELETE', 'POST'],
            apiJobClusterTakeoverSchedule: 'PUT',
            apiJobUpdateSingle: 'PUT'
    ]

    def cancel (){
        //clear session workflow data
        if(session.editWF ){
            session.removeAttribute('editWF');
            session.removeAttribute('undoWF');
            session.removeAttribute('redoWF');
        }
        if(session.editWF ){
            session.removeAttribute('editOPTS');
            session.removeAttribute('undoOPTS');
            session.removeAttribute('redoOPTS');
        }
        if(params.id && params.id!=''){
            redirect(action:'show',params:[id:params.id])
        }else{
            redirect(action:'index',params: [project:params.project])
        }
    }
    def list = {redirect(action:index,params:params) }

    def groupTreeFragment = {
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)
        def tree = scheduledExecutionService.getGroupTree(params.project,authContext)
        render(template:"/menu/groupTree",model:[jobgroups:tree,jscallback:params.jscallback])
    }

    def xmlerror={
        render(contentType:"text/xml",encoding:"UTF-8"){
            result(error:"true"){
                delegate.'error'{
                    if(flash.error){
                        response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,flash.error)
                        delegate.'message'(flash.error)
                    }
                    if(flash.errors){
                        def p = delegate
                        flash.errors.each{ msg ->
                            p.'message'(msg)
                        }
                    }
                }
            }
        }
    }
    def xmlsuccess={
        render(contentType:"text/xml",encoding:"UTF-8"){
            delegate.'result'(error:"false"){
                delegate.'success'{
                    if(flash.message){
                        response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,flash.message)
                        delegate.'message'(flash.message)
                    }
                    if(flash.messages){
                        def p = delegate
                        flash.messages.each{ msg ->
                            p.'message'(msg)
                        }
                    }
                }
            }
        }
    }

    /**
     * used by jobs page, displays actions for the job as li's
     */
    def actionMenuFragment(){
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (!unauthorizedResponse(
                frameworkService.authorizeProjectJobAll(
                        authContext,
                        scheduledExecution,
                        [AuthConstants.ACTION_READ],
                        scheduledExecution.project
                ),
                AuthConstants.ACTION_READ,
                'Job',
                params.id
            )
        ) {

            def model=[
                    scheduledExecution: scheduledExecution,
                    hideJobDelete     : params.hideJobDelete,
                    jobDeleteSingle   : params.jobDeleteSingle,
            ]

            if (frameworkService.authorizeApplicationResourceAny(authContext,
                                                                 frameworkService.authResourceForProject(params.project),
                                                                 [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_EXPORT])) {
                if(scmService.projectHasConfiguredExportPlugin(params.project)) {
                    model.scmExportEnabled = true
                    model.scmExportStatus = scmService.exportStatusForJobs([scheduledExecution])
                    model.scmExportRenamedPath=scmService.getRenamedJobPathsForProject(params.project)?.get(scheduledExecution.extid)
                }
            }
            if (frameworkService.authorizeApplicationResourceAny(authContext,
                                                                 frameworkService.authResourceForProject(params.project),
                                                                 [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_IMPORT])) {
                if(scmService.projectHasConfiguredPlugin('import',params.project)) {
                    model.scmImportEnabled = true
                    model.scmImportStatus = scmService.importStatusForJobs([scheduledExecution])
                }
            }
            render(template: '/scheduledExecution/jobActionButtonMenuContent', model: model)
        }
    }

    private def jobDetailData() {
        Framework framework = frameworkService.getRundeckFramework()
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        def crontab = scheduledExecution.timeAndDateAsBooleanMap()
        //list executions using query params and pagination params

        def executions=Execution.findAllByScheduledExecution(scheduledExecution,[offset: params.offset?params.offset:0, max: params.max?params.max:10, sort:'dateStarted', order:'desc'])

        def total = Execution.countByScheduledExecution(scheduledExecution)

        def remoteClusterNodeUUID = null
        if (scheduledExecution.scheduled && frameworkService.isClusterModeEnabled()
                && scheduledExecution.serverNodeUUID != frameworkService.getServerUUID()) {
            remoteClusterNodeUUID = scheduledExecution.serverNodeUUID
        }

        [scheduledExecution:scheduledExecution, crontab:crontab, params:params,
            executions:executions,
            total:total,
            nextExecution:scheduledExecutionService.nextExecutionTime(scheduledExecution),
            remoteClusterNodeUUID: remoteClusterNodeUUID,
            max: params.max?params.max:10,
            notificationPlugins: notificationService.listNotificationPlugins(),
            orchestratorPlugins: orchestratorPluginService.listOrchestratorPlugins(),
            offset:params.offset?params.offset:0]
    }
    def detailFragment () {
        log.debug("ScheduledExecutionController: detailFragment : params: " + params)
        Framework framework = frameworkService.getRundeckFramework()
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if(notFoundResponse(scheduledExecution,'Job',params.id)){
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if(unauthorizedResponse(frameworkService.authorizeProjectJobAll(authContext, scheduledExecution,
                [AuthConstants.ACTION_READ], scheduledExecution.project), AuthConstants.ACTION_READ,'Job',params.id)){
            return
        }
        def model=jobDetailData()

        return render(view:'jobDetailFragment',model: model)
    }
    def detailFragmentAjax () {
        log.debug("ScheduledExecutionController: detailFragmentAjax : params: " + params)
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if(notFoundResponse(scheduledExecution,'Job',params.id)){
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if(unauthorizedResponse(frameworkService.authorizeProjectJobAll(authContext, scheduledExecution,
                [AuthConstants.ACTION_READ], scheduledExecution.project), AuthConstants.ACTION_READ,'Job',params.id)){
            return
        }
        def model=jobDetailData()
        def se = model.scheduledExecution
        render(contentType: 'application/json') {
            total = model.total
            nextExecution = model.nextExecution
            max = model.max
            job(
                    id: se.extid,
                    name: (se.jobName),
                    group: (se.groupPath),
                    project: (se.project),
                    description: (se.description),
                    href: apiService.apiHrefForJob(se),
                    permalink: apiService.guiHrefForJob(se),
                    filter: se.filter?:'',
                    doNodeDispatch: se.doNodedispatch
            )
        }
    }
    def show = {
        log.debug("ScheduledExecutionController: show : params: " + params)
        def crontab = [:]
        def framework = frameworkService.getRundeckFramework()
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (unauthorizedResponse(frameworkService.authorizeProjectJobAll(authContext, scheduledExecution,
                [AuthConstants.ACTION_READ], scheduledExecution.project), AuthConstants.ACTION_READ, 'Job', params.id)) {
            return
        }

        if (!params.project || params.project != scheduledExecution.project) {
            return redirect(controller: 'scheduledExecution', action: 'show',
                    params: [id: params.id, project: scheduledExecution.project])
        }
        request.project=scheduledExecution.project
        crontab = scheduledExecution.timeAndDateAsBooleanMap()
        //list executions using query params and pagination params

        def total = Execution.countByScheduledExecution(scheduledExecution)

        def remoteClusterNodeUUID=null
        if (scheduledExecution.scheduled && frameworkService.isClusterModeEnabled()) {
            remoteClusterNodeUUID = scheduledExecution.serverNodeUUID
        }


        def dataMap= [
                scheduledExecution: scheduledExecution,
                crontab: crontab,
                params: params,
                total: total,
                nextExecution: scheduledExecutionService.nextExecutionTime(scheduledExecution),
                remoteClusterNodeUUID: remoteClusterNodeUUID,
                serverNodeUUID: frameworkService.isClusterModeEnabled()?frameworkService.serverUUID:null,
                notificationPlugins: notificationService.listNotificationPlugins(),
				orchestratorPlugins: orchestratorPluginService.listOrchestratorPlugins(),
                max: params.int('max') ?: 10,
                offset: params.int('offset') ?: 0] + _prepareExecute(scheduledExecution, framework,authContext)

        //add scm export status
        def projectResource = frameworkService.authResourceForProject(params.project)
        if (frameworkService.authorizeApplicationResourceAny(authContext,
                                                             projectResource,
                                                             [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_EXPORT])) {
            if(scmService.projectHasConfiguredExportPlugin(params.project)){
                dataMap.scmExportEnabled = true
                dataMap.scmExportStatus = scmService.exportStatusForJobs([scheduledExecution])
                dataMap.scmExportRenamedPath=scmService.getRenamedJobPathsForProject(params.project)?.get(scheduledExecution.extid)
            }
        }
        if (frameworkService.authorizeApplicationResourceAny(authContext,
                                                             projectResource,
                                                             [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_IMPORT])) {
            if(scmService.projectHasConfiguredPlugin('import',params.project)) {
                dataMap.scmImportEnabled = true
                dataMap.scmImportStatus = scmService.importStatusForJobs([scheduledExecution])
            }
        }
        withFormat{
            html{
                dataMap
            }
            yaml{
                render(text:JobsYAMLCodec.encode([scheduledExecution] as List),contentType:"text/yaml",encoding:"UTF-8")
            }

            xml{
                def fname=scheduledExecution.jobName.replaceAll(' ','_')
                fname=fname.replaceAll('"','_')
                fname=fname.replaceAll('\\\\','_')
                final Pattern s = Pattern.compile("[\\r\\n]")
                fname=fname.replaceAll(s,'_')
                if(fname.size()>74){
                    fname = fname.substring(0,74)
                }
                response.setHeader("Content-Disposition","attachment; filename=\"${fname}.xml\"")
                response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,"Jobs found: 1")

                def writer = new StringWriter()
                def xml = new MarkupBuilder(writer)
                JobsXMLCodec.encodeWithBuilder([scheduledExecution],xml)
                writer.flush()
                render(text:writer.toString(),contentType:"text/xml",encoding:"UTF-8")
            }
        }
        dataMap
    }


    /**
     * check crontabString parameter if it is a valid crontab, and render any syntax warnings
     */
    def checkCrontab={
        if(!params.crontabString){
            request.error="crontabString parameter is required"
        }else{
            if(!CronExpression.isValidExpression(params.crontabString)){
                def x = params.crontabString.split(" ")
                if(x && x.size()>6 && x [3] != '?' && x [5]!='?'){
                    request.warn="day of week or day of month must be '?'"
                }else{
                    request.warn="Format invalid"
                }
            }
        }
        render(template:'/common/messages')
    }

    /**
     * This action loads the JSON data from the URL specified in
     * an option's "valueSrc" property, and renders the optionValuesSelect template
     * using the data.
     */
    def loadRemoteOptionValues={
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (unauthorizedResponse(frameworkService.authorizeProjectJobAll(authContext, scheduledExecution,
                [AuthConstants.ACTION_READ], scheduledExecution.project), AuthConstants.ACTION_READ, 'Job', params.id)) {
            return
        }
        if(!params.option){
            log.error("option missing")
            return renderErrorFragment("option missing")
        }

        //see if option specified, and has url
        if (scheduledExecution.options && scheduledExecution.options.find {it.name == params.option}) {
            def Option opt = scheduledExecution.options.find {it.name == params.option}
            def values=[]
            if (opt.realValuesUrl) {
                //load expand variables in URL source

                def realUrl = opt.realValuesUrl.toExternalForm()
                String srcUrl = expandUrl(opt, realUrl, scheduledExecution, params.extra?.option,realUrl.matches(/(?i)^https?:.*$/))
                String cleanUrl=srcUrl.replaceAll("^(https?://)([^:@/]+):[^@/]*@",'$1$2:****@');
                def remoteResult=[:]
                def result=null
                def remoteStats=[startTime: System.currentTimeMillis(), httpStatusCode: "", httpStatusText: "", contentLength: "", url: srcUrl,durationTime:"",finishTime:"", lastModifiedDateTime:""]
                def err = [:]
                int timeout=10
                int contimeout=0
                int retryCount=5
                if(grailsApplication.config.rundeck?.jobs?.options?.remoteUrlTimeout){
                    try {
                        timeout = Integer.parseInt(
                                grailsApplication?.config?.rundeck?.jobs?.options?.remoteUrlTimeout?.toString()
                        )
                    }catch(NumberFormatException e) {
                        log.warn(
                                "Configuration value rundeck.jobs.options.remoteUrlTimeout is not a valid integer: "
                                        + e.message
                        )
                    }
                }
                if(grailsApplication.config.rundeck?.jobs?.options?.remoteUrlConnectionTimeout){
                    try {
                        contimeout = Integer.parseInt(
                                grailsApplication?.config?.rundeck?.jobs?.options?.remoteUrlConnectionTimeout?.toString()
                        )
                    }catch(NumberFormatException e) {
                        log.warn(
                                "Configuration value rundeck.jobs.options.remoteUrlConnectionTimeout is not a valid integer: "
                                        + e.message
                        )
                    }
                }
                if(grailsApplication.config.rundeck?.jobs?.options?.remoteUrlRetry){
                    try {
                        retryCount = Integer.parseInt(
                                grailsApplication?.config?.rundeck?.jobs?.options?.remoteUrlRetry?.toString()
                        )
                    }catch(NumberFormatException e) {
                        log.warn(
                                "Configuration value rundeck.jobs.options.remoteUrlRetry is not a valid integer: "
                                        + e.message
                        )
                    }
                }
                if(srcUrl.indexOf('#')>=0 &&srcUrl.indexOf('#')<srcUrl.size()-1){
                    def urlanchor=new HashMap<String,String>()
                    def anchor=srcUrl.substring(srcUrl.indexOf('#')+1)
                    def parts=anchor.split(";")
                    parts.each{s->
                        def subpart=s.split("=",2)
                        if(subpart && subpart.length==2 && subpart[0] && subpart[1]){
                            urlanchor[subpart[0]]=subpart[1]
                        }
                    }
                    if(urlanchor['timeout']){
                        try {
                            timeout = Integer.parseInt(urlanchor['timeout'])
                        }catch(NumberFormatException e) {
                            log.warn(
                                    "URL timeout ${urlanchor['timeout']} is not a valid integer: "
                                            + e.message
                            )
                        }
                    }
                    if(urlanchor['contimeout']){
                        try {
                            contimeout = Integer.parseInt(urlanchor['contimeout'])
                        }catch(NumberFormatException e) {
                            log.warn(
                                    "URL contimeout ${urlanchor['contimeout']} is not a valid integer: "
                                            + e.message
                            )
                        }
                    }
                    if(urlanchor['retry']){
                        try {
                            retryCount = Integer.parseInt(urlanchor['retry'])
                        }catch(NumberFormatException e) {
                            log.warn(
                                    "URL retry ${urlanchor['retry']} is not a valid integer: "
                                            + e.message
                            )
                        }
                    }
                }
                try {
                    remoteResult = getRemoteJSON(srcUrl, timeout, contimeout, retryCount)
                    result=remoteResult.json
                    if(remoteResult.stats){
                        remoteStats.putAll(remoteResult.stats)
                    }
                } catch (Exception e) {
                    err.message = "Failed loading remote option values"
                    err.exception = e
                    err.srcUrl = cleanUrl
                    log.error("getRemoteJSON error: URL ${cleanUrl} : ${e.message}");
                    e.printStackTrace()
                    remoteStats.finishTime=System.currentTimeMillis()
                    remoteStats.durationTime= remoteStats.finishTime- remoteStats.startTime
                }
                if(remoteResult.error){
                    err.message = "Failed loading remote option values"
                    err.exception = new Exception(remoteResult.error)
                    err.srcUrl = cleanUrl
                    log.error("getRemoteJSON error: URL ${cleanUrl} : ${remoteResult.error}");
                }
                logRemoteOptionStats(remoteStats,[jobName:scheduledExecution.generateFullName(),id:scheduledExecution.extid, jobProject:scheduledExecution.project,optionName:params.option,user:session.user])
                //validate result contents
                boolean valid = true;
                def validationerrors=[]
                if(result){
                    if( result instanceof Collection){
                        result.eachWithIndex { entry,i->
                            if(entry instanceof org.codehaus.groovy.grails.web.json.JSONObject){
                                if(!entry.name){
                                    validationerrors<<"Item: ${i} has no 'name' entry"
                                    valid=false;
                                }
                                if(!entry.value){
                                    validationerrors<<"Item: ${i} has no 'value' entry"
                                    valid = false;
                                }
                            }else if(!(entry instanceof String)){
                                valid = false;
                                validationerrors << "Item: ${i} expected string or map like {name:\"..\",value:\"..\"}"
                            }
                        }
                    } else if (result instanceof org.codehaus.groovy.grails.web.json.JSONObject) {
                        org.codehaus.groovy.grails.web.json.JSONObject jobject = result
                        result = []
                        jobject.keys().sort().each {k ->
                            result << [name: k, value: jobject.get(k)]
                        }
                    }else{
                        validationerrors << "Expected top-level list with format: [{name:\"..\",value:\"..\"},..], or ['value','value2',..] or simple object with {name:\"value\",...}"
                        valid=false
                    }
                    if(!valid){
                        result=null
                        err.message="Failed parsing remote option values: ${validationerrors.join('\n')}"
                    }
                }else if(!err){
                    err.message = "Empty result"
                    err.code='empty'
                }
                def model= [optionSelect: opt, values: result, srcUrl: cleanUrl, err: err, fieldPrefix: params.fieldPrefix, selectedvalue: params.selectedvalue]
                if(params.extra?.option?.get(opt.name)){
                    model.selectedoptsmap=[(opt.name):params.extra.option.get(opt.name)]
                }
                return render(template: "/framework/optionValuesSelect", model: model);
            } else {
                return renderErrorFragment("not a url option: " + params.option)
            }
        }else{
            return renderErrorFragment("option not found: "+params.option)
        }

    }
    static Logger optionsLogger = Logger.getLogger("com.dtolabs.rundeck.remoteservice.http.options")
    private logRemoteOptionStats(stats,jobdata){
        stats.keySet().each{k->
            def v= stats[k]
            if(v instanceof Date){
                //TODO: reformat date
                MDC.put(k,v.toString())
                MDC.put("${k}Time",v.time.toString())
            }else if(v instanceof String){
                MDC.put(k,v?v:"-")
            }else{
                final string = v.toString()
                MDC.put(k, string?string:"-")
            }
        }
        jobdata.keySet().each{k->
            final var = jobdata[k]
            MDC.put(k,var?var:'-')
        }
        optionsLogger.info(stats.httpStatusCode + " " + stats.httpStatusText+" "+stats.contentLength+" "+stats.url)
        stats.keySet().each {k ->
            if (stats[k] instanceof Date) {
                //reformat date
                MDC.remove(k+'Time')
            }
            MDC.remove(k)
        }
        jobdata.keySet().each {k ->
            MDC.remove(k)
        }
    }

    /**
     * Map of descriptive property name to ScheduledExecution domain class property names
     * used by expandUrl for embedded property references in remote options URL
     */
    private static jobprops=[
        name:'jobName',
        group:'groupPath',
        description:'description',
        project:'project',
    ]
    /**
     * Map of descriptive property name to Option domain class property names
     * used by expandUrl for embedded property references in remote options URL
     */
    private static optprops=[
        name:'name',

    ]
    /**
     * Expand the URL string's embedded property references of the form
     * ${job.PROPERTY} and ${option.PROPERTY}.  available properties are
     * limited
     */
    protected String expandUrl(Option opt, String url, ScheduledExecution scheduledExecution,selectedoptsmap=[:],boolean isHttp=true) {
        def invalid = []
        def rundeckProps=[
                'nodename':frameworkService.getFrameworkNodeName(),
                'serverUUID':frameworkService.serverUUID?:''
        ]
        if(!isHttp) {
            rundeckProps.basedir= frameworkService.getRundeckBase()
        }
        def extraJobProps=[
                'user.name': (session?.user?: "anonymous"),
        ]
        extraJobProps.putAll rundeckProps.collectEntries {['rundeck.'+it.key,it.value]}

        def replacement= { Object[] group ->
            if (group[2] == 'job' && jobprops[group[3]] && scheduledExecution.properties.containsKey(jobprops[group[3]])) {
                scheduledExecution.properties.get(jobprops[group[3]]).toString()
            } else if (group[2] == 'job' && null != extraJobProps[group[3]]) {
                def value = extraJobProps[group[3]]
                value.toString()
            }else if (group[2] == 'rundeck' && null != rundeckProps[group[3]]) {
                def value = rundeckProps[group[3]]
                value.toString()
            } else if (group[2] == 'option' && optprops[group[3]] && opt.properties.containsKey(optprops[group[3]])) {
                opt.properties.get(optprops[group[3]]).toString()
            } else if (group[2] == 'option' && group[4] == '.value') {
                def optname = group[3].substring(0, group[3].length() - '.value'.length())
                def value = selectedoptsmap && selectedoptsmap instanceof Map ? selectedoptsmap[optname] : null
                //find option with name
                def Option expopt = scheduledExecution.options.find { it.name == optname }
                if (value && expopt?.multivalued && (value instanceof Collection || value instanceof String[])) {
                    value = value.join(expopt.delimiter)
                }
                (value ?: '')
            } else {
                null
            }
        }
        //replace variables in the URL, using appropriate encoding before/after the URL parameter '?' separator
        def arr=url.split(/\?/,2)
        def codecs=['URIComponent','URL']
        def result=[]
        arr.eachWithIndex { String entry, int i ->
            result<<entry.replaceAll(/(\$\{(job|option|rundeck)\.([^}]+?(\.value)?)\})/) { Object[] group ->
                def val = replacement(group)
                 if (null != val) {
                     if(!isHttp){
                         return val
                     }
                     val."encodeAs${codecs[i]}"()
                 } else {
                     invalid << group[0]
                     group[0]
                 }
             }
        }
        String srcUrl = result.join('?')
        if (invalid) {
            log.error("invalid expansion: " + invalid);
        }
        return srcUrl
    }

    /**
     * Make a remote URL request and return the parsed JSON data and statistics for http requests in a map.
     * if an error occurs, a map with a single 'error' entry will be returned.
     * the stats data contains:
     *
     * url: requested url
     * startTime: start time epoch ms
     * httpStatusCode: http status code (int)
     * httpStatusText: http status text
     * finishTime: finish time epoch ms
     * durationTime: duration time in ms
     * contentLength: response content length bytes (long)
     * lastModifiedDate: Last-Modified header (Date)
     * contentSHA1: SHA1 hash of the content
     *
     * @param url URL to request
     * @param timeout request timeout in seconds
     * @return Map of data, [json: parsed json or null, stats: stats data, error: error message]
     *
     */
    private Object getRemoteJSON(String url, int timeout, int contimeout, int retry=5){
        log.warn("getRemoteJSON: "+url+", timeout: "+timeout+", retry: "+retry)
        //attempt to get the URL JSON data
        def stats=[:]
        if(url.startsWith("http:") || url.startsWith("https:")){
            final HttpClientParams params = new HttpClientParams()
            params.setConnectionManagerTimeout(timeout*1000L)
            params.setSoTimeout(timeout*1000)
            if(contimeout>0){
                params.setIntParameter('http.connection.timeout',contimeout*1000)
            }
            if(retry>0) {
                def myretryhandler = new DefaultHttpMethodRetryHandler(retry, false)
                params.setParameter(HttpMethodParams.RETRY_HANDLER, myretryhandler);
            }
            def HttpClient client= new HttpClient(params)
            def URL urlo
            def AuthScope authscope=null
            def UsernamePasswordCredentials cred=null
            boolean doauth=false
            String cleanUrl = url.replaceAll("^(https?://)([^:@/]+):[^@/]*@", '$1$2:****@');
            try{
                urlo = new URL(url)
                if(urlo.userInfo){
                    doauth = true
                    authscope = new AuthScope(urlo.host,urlo.port>0? urlo.port:urlo.defaultPort,AuthScope.ANY_REALM,"BASIC")
                    cred = new UsernamePasswordCredentials(urlo.userInfo)
                    url = new URL(urlo.protocol, urlo.host, urlo.port, urlo.file).toExternalForm()
                }
            }catch(MalformedURLException e){
                throw new Exception("Failed to configure base URL for authentication: "+e.getMessage(),e)
            }
            if(doauth){
                client.getParams().setAuthenticationPreemptive(true);
                client.getState().setCredentials(authscope,cred)
            }
            def HttpMethod method = new GetMethod(url)
            method.setFollowRedirects(true)
            method.setRequestHeader("Accept","application/json")
            stats.url = cleanUrl;
            stats.startTime = System.currentTimeMillis();
            def resultCode = client.executeMethod(method);
            stats.httpStatusCode = resultCode
            stats.httpStatusText = method.getStatusText()
            stats.finishTime = System.currentTimeMillis()
            stats.durationTime=stats.finishTime-stats.startTime
            stats.contentLength = method.getResponseContentLength()
            final header = method.getResponseHeader("Last-Modified")
            if(null!=header){
                try {
                    stats.lastModifiedDate= DateUtil.parseDate(header.getValue())
                } catch (DateParseException e) {
                }
            }else{
                stats.lastModifiedDate=""
                stats.lastModifiedDateTime=""
            }
            try{
                def reasonCode = method.getStatusText();
                if(resultCode>=200 && resultCode<=300){
                    def expectedContentType="application/json"
                    def resultType=''
                    if (null != method.getResponseHeader("Content-Type")) {
                        resultType = method.getResponseHeader("Content-Type").getValue();
                    }
                    String type = resultType;
                    if (type.indexOf(";") > 0) {
                        type = type.substring(0, type.indexOf(";")).trim();
                    }

                    if (expectedContentType.equals(type)) {
                        final stream = method.getResponseBodyAsStream()
                        final writer = new StringWriter()
                        int len=copyToWriter(new BufferedReader(new InputStreamReader(stream, method.getResponseCharSet())),writer)
                        stream.close()
                        writer.flush()
                        final string = writer.toString()
                        def json=grails.converters.JSON.parse(string)
                        if(string){
                            stats.contentSHA1=string.encodeAsSHA1()
                            if(stats.contentLength<0){
                                stats.contentLength= len
                            }
                        }else{
                            stats.contentSHA1=""
                        }
                        return [json:json,stats:stats]
                    }else{
                        return [error:"Unexpected content type received: "+resultType,stats:stats]
                    }
                }else{
                    stats.contentSHA1 = ""
                    return [error:"Server returned an error response: ${resultCode} ${reasonCode}",stats:stats]
                }
            } finally {
                method.releaseConnection();
            }
        }else if (url.startsWith("file:")) {
            stats.url=url
            def File srfile = new File(new URI(url))
            final writer = new StringWriter()
            final stream= new FileInputStream(srfile)

            stats.startTime = System.currentTimeMillis();
            int len = copyToWriter(new BufferedReader(new InputStreamReader(stream)), writer)
            stats.finishTime = System.currentTimeMillis()
            stats.durationTime = stats.finishTime - stats.startTime
            stream.close()
            writer.flush()
            final string = writer.toString()
            final JSONElement parse = grails.converters.JSON.parse(string)
            if (string) {
                stats.contentSHA1 = string.encodeAsSHA1()
            }else{
                stats.contentSHA1 = ""
            }
            stats.contentLength=srfile.length()
            stats.lastModifiedDate=new Date(srfile.lastModified())
            stats.lastModifiedDateTime=srfile.lastModified()
            return [json:parse,stats:stats]
        } else {
            throw new Exception("Unsupported protocol: " + url)
        }
    }

    static int copyToWriter(Reader read, Writer writer){
        char[] chars = new char[1024];
        int len=0;
        int size=read.read(chars,0,chars.length)
        while(-1!=size){
            len+=size;
            writer.write(chars,0,size)
            size = read.read(chars, 0, chars.length)
        }
        return len;
    }

    def flipScheduleEnabled() {
        withForm{
        if (!params.id) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorView(g.message(code: 'api.error.parameter.required', args: ['id']))
        }

        def jobid = params.id

            def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(jobid)
            if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
                return
            }

            Framework framework = frameworkService.getRundeckFramework()
            UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(
                    session.subject,
                    scheduledExecution.project
            )
            def changeinfo = [method: 'update', change: 'modify', user: session.user]

            //pass session-stored edit state in params map
            transferSessionEditState(session, params, params.id)

            String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")

            def payload = [id: params.id, scheduleEnabled: params.scheduleEnabled]
            def result = scheduledExecutionService._doUpdateExecutionFlags(payload, session.user, roleList, framework, authContext, changeinfo)
            if(!result.success){
                flash.error=result.message
            }
            if(params.returnToJob=='true'){
                return redirect(controller: 'scheduledExecution', action: 'show', params: [project: params.project,id:scheduledExecution.extid])
            }
            redirect(controller: 'menu', action: 'jobs', params: [project: params.project])

        }.invalidToken{
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.errorCode = 'request.error.invalidtoken.message'
            return renderErrorView([:])
        }
    }

    def flipExecutionEnabled() {
        withForm{

            if (!params.id) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                return renderErrorView(g.message(code: 'api.error.parameter.required', args: ['id']))
            }

            def jobid = params.id
            def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(jobid)
            if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
                return
            }

            Framework framework = frameworkService.getRundeckFramework()
            UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(
                    session.subject,
                    scheduledExecution.project
            )
            def changeinfo = [method: 'update', change: 'modify', user: authContext.username]

            //pass session-stored edit state in params map
            transferSessionEditState(session, params, params.id)

            String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")

            def payload = [id: params.id, executionEnabled: params.executionEnabled]
            def result = scheduledExecutionService._doUpdateExecutionFlags(payload, session.user, roleList, framework, authContext, changeinfo)
            if(!result.success){
                flash.error=result.message
            }
            if(params.returnToJob=='true'){
                return redirect(controller: 'scheduledExecution', action: 'show', params: [project: params.project,id:scheduledExecution.extid])
            }
            redirect(controller: 'menu', action: 'jobs', params: [project: params.project])

        }.invalidToken{
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.errorCode = 'request.error.invalidtoken.message'
            return renderErrorView([:])
        }
    }

    def apiFlipExecutionEnabled() {
        if (!apiService.requireApi(request, response)) {
            return
        }

        if (!apiService.requireVersion(request, response, ApiRequestFilters.V14)) {
            return
        }


        log.debug("ScheduledExecutionController: apiFlipExecutionEnabled" + params)

        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }

        def scheduledExecution = ScheduledExecution.getByIdOrUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', params.id])) {
            //job does not exist
            return
        }

        def Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, scheduledExecution.project)

        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_TOGGLE_EXECUTION],
                scheduledExecution.project)) {
            def error = [status: HttpServletResponse.SC_FORBIDDEN, code  : 'api.error.item.unauthorized', args: ['Toggle Execution', 'Job ID', params.id]]
            return apiService.renderErrorFormat(response, error)
        }

        def changeinfo = [method: 'update', change: 'modify', user: session.user]
        String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")

        def payload = [id: params.id, executionEnabled: params.status]
        def result = scheduledExecutionService._doUpdateExecutionFlags(payload, session.user, roleList, framework, authContext, changeinfo)

        if (result && result.success) {
            return withFormat {
                xml {
                    render(text: "<success>true</success>",contentType:"text/xml",encoding:"UTF-8")
                }

                json {
                    render ([success: true] as JSON)
                }
            }
        } else {
            return apiService.renderErrorFormat(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
        }
    }

    def apiFlipScheduleEnabled() {
        if (!apiService.requireApi(request, response)) {
            return
        }

        if (!apiService.requireVersion(request, response, ApiRequestFilters.V14)) {
            return
        }

        log.debug("ScheduledExecutionController: apiFlipScheduleEnabled" + params)

        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }

        def scheduledExecution = ScheduledExecution.getByIdOrUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', params.id])) {
            //job does not exist
            return
        }

        def Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, scheduledExecution.project)

        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_TOGGLE_SCHEDULE],
                scheduledExecution.project)) {
            def error = [status: HttpServletResponse.SC_FORBIDDEN, code  : 'api.error.item.unauthorized', args: ['Toggle Schedule', 'Job ID', params.id]]
            return apiService.renderErrorFormat(response, error)
        }

        def changeinfo = [method: 'update', change: 'modify', user: session.user]
        String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")

        def payload = [id: params.id, scheduleEnabled: params.status]
        def result = scheduledExecutionService._doUpdateExecutionFlags(payload, session.user, roleList, framework, authContext, changeinfo)

        if (result && result.success) {
            return withFormat {
                xml {
                    render(text: "<success>true</success>", contentType:"text/xml", encoding:"UTF-8")
                }

                json {
                    render ([success: true] as JSON)
                }
            }
        } else {
            return apiService.renderErrorFormat(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
    */
    def delete(){
        if (!params.id) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorView(g.message(code: 'api.error.parameter.required', args: ['id']))
        }
        def jobid=params.id

        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)

        if (unauthorizedResponse(
                frameworkService.authorizeProjectResource(
                        authContext,
                        AuthConstants.RESOURCE_TYPE_JOB,
                        AuthConstants.ACTION_DELETE,
                        scheduledExecution.project
                ) && frameworkService.authorizeProjectJobAll(
                        authContext,
                        scheduledExecution,
                        [AuthConstants.ACTION_DELETE],
                        scheduledExecution.project
                ),
                AuthConstants.ACTION_DELETE,
                'Job',
                params.id
        )) {
            return
        }
        if(request.method=='POST') {
            withForm {
                def result = scheduledExecutionService.deleteScheduledExecutionById(
                        jobid,
                        authContext,
                        params.deleteExecutions == 'true',
                        session.user,
                        'delete'
                )
                if (!result.success) {
                    return renderErrorView(result.error.message)
                } else {
                    def project = result.success.job ? result.success.job.project : params.project
                    flash.bulkJobResult = [success: [result.success]]
                    redirect(controller: 'menu', action: 'jobs', params: [project: project])
                }
            }.invalidToken {
                response.status = HttpServletResponse.SC_BAD_REQUEST
                request.errorCode = 'request.error.invalidtoken.message'
                return renderErrorView([:])
            }
        }else{
            return [scheduledExecution: scheduledExecution]
        }

    }
    /**
     * Enable execution for a set of jobs.
     * Only allowed via POST http method
     */
    def flipExecutionEnabledBulk (ApiBulkJobDeleteRequest deleteRequest) {
        log.debug("ScheduledExecutionController: flipExecutionEnabledBulk : params: " + params)
        return handleFormFlipJobFlagBulk(deleteRequest, 'flipExecutionEnabledBulk', [executionEnabled: true], 'api.success.job.execution.enabled')
    }

    private def handleFormFlipJobFlagBulk(ApiBulkJobDeleteRequest deleteRequest, String methodName, Map flags, String successCode) {
        if (deleteRequest.hasErrors()) {
            flash.bulkJobResult = [success: false, errors: deleteRequest.errors]
            return redirect(controller: 'menu', action: 'jobs', params: [project: params.project])
        }
        withForm {
            if (!params.ids && !params.idlist) {
                flash.error = g.message(code: 'ScheduledExecutionController.bulkUpdate.empty')
                return redirect(controller: 'menu', action: 'jobs', params: [project: params.project])
            }
            flash.bulkJobResult = performFlipJobFlagBulk(deleteRequest,methodName,flags, successCode)
            redirect(controller: 'menu', action: 'jobs', params: [project: params.project])
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.errorCode = 'request.error.invalidtoken.message'
            return renderErrorView([:])
        }
    }

    private def performFlipJobFlagBulk(ApiBulkJobDeleteRequest deleteRequest,String methodName,Map flags, String successCode) {

        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def ids = deleteRequest.generateIdSet()

        def successful = []
        def errs = []
        def changeinfo = [method: methodName, change: 'modify', user: authContext.username]
        def framework = frameworkService.getRundeckFramework()
        ids.sort().each { jobid ->

            def result = scheduledExecutionService._doUpdateExecutionFlags(
                    [id: jobid] + flags,
                    authContext.username,
                    authContext.roles.join(','),
                    framework,
                    authContext,
                    changeinfo
            )
            if (!result.success) {
                if (result.unauthorized) {
                    errs << [id       : jobid,
                             errorCode: result.errorCode,
                             message  : result.message ?:
                                     g.message(code: result.errorCode, args: ['Job', "{{Job " + jobid + "}}"])
                    ]
                } else if (result.errorCode) {
                    errs << [id     : jobid, errorCode: result.errorCode,
                             message: result.message ?:
                                     g.message(code: result.errorCode, args: ['Job', "{{Job " + jobid + "}}"]
                                     )]
                } else if (result.error) {
                    errs << result.error
                } else {
                    errs << [id: jobid, message: result.message]
                }
            } else {
                def jobtitle = "{{Job " + result.scheduledExecution.extid + "}}"
                successful << [message: g.message(code: successCode, args: [jobtitle]),id:jobid]
            }
        }
        return [success: successful, errors: errs]
    }

    /**
     * Disable execution for a set of jobs.
     * Only allowed via POST http method
     */
    def flipExecutionDisabledBulk (ApiBulkJobDeleteRequest deleteRequest) {
        log.debug("ScheduledExecutionController: flipExecutionDisabledBulk : params: " + params)
        return handleFormFlipJobFlagBulk(deleteRequest, 'flipExecutionDisabledBulk', [executionEnabled: false], 'api.success.job.execution.disabled')

    }
    /**
     * Enable schedule for a set of jobs.
     * Only allowed via POST http method
     */
    def flipScheduleEnabledBulk (ApiBulkJobDeleteRequest deleteRequest) {
        log.debug("ScheduledExecutionController: flipScheduleEnabledBulk : params: " + params)
        return handleFormFlipJobFlagBulk(deleteRequest, 'flipScheduleEnabledBulk', [scheduleEnabled: true], 'api.success.job.schedule.enabled')
    }
    /**
     * Disable schedule for a set of jobs.
     * Only allowed via POST http method
     */
    def flipScheduleDisabledBulk (ApiBulkJobDeleteRequest deleteRequest) {
        log.debug("ScheduledExecutionController: flipScheduleDisabledBulk : params: " + params)
        return handleFormFlipJobFlagBulk(deleteRequest, 'flipScheduleDisabledBulk', [scheduleEnabled: false], 'api.success.job.schedule.disabled')

    }
    /**
     * Delete a set of jobs as specified in the idlist parameter.
     * Only allowed via POST http method
     */
    def deleteBulk (ApiBulkJobDeleteRequest deleteRequest) {
        if(deleteRequest.hasErrors()){
            flash.errors = deleteRequest.errors
            return redirect(controller: 'menu', action: 'jobs')
        }
        log.debug("ScheduledExecutionController: deleteBulk : params: " + params)
        withForm{
            if (!params.ids && !params.idlist) {
                flash.error = g.message(code: 'ScheduledExecutionController.bulkDelete.empty')
                return redirect(controller: 'menu', action: 'jobs')
            }
            AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
            def ids = deleteRequest.generateIdSet()

            def successful = []
            def deleteerrs = []
            ids.sort().each {jobid ->
                def result = scheduledExecutionService.deleteScheduledExecutionById(jobid, authContext,
                        params.deleteExecutions == 'true', session.user, 'deleteBulk')
                if (result.errorCode) {
                    deleteerrs << [id: jobid, errorCode: result.errorCode, message: g.message(code: result.errorCode, args: ['Job ID', jobid])]
                }else if (result.error) {
                    deleteerrs << result.error
                } else {
                    successful << result.success
                }
            }
            flash.bulkJobResult = [success: successful, errors: deleteerrs]
            redirect(controller: 'menu', action: 'jobs',params:[project:params.project])
        }.invalidToken{
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.errorCode = 'request.error.invalidtoken.message'
            return renderErrorView([:])
        }
    }
    def apiFlipExecutionEnabledBulk(ApiBulkJobDeleteRequest deleteRequest) {
        if(!apiService.requireVersion(request,response,ApiRequestFilters.V16)){
            return
        }
        if (deleteRequest.hasErrors()) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.invalid.request',
                                                           args: [deleteRequest.errors.allErrors.collect { g.message(error: it) }.join("; ")]])
        }
        log.debug("ScheduledExecutionController: apiFlipExecutionEnabledBulk : params: " + params)

        def ids = deleteRequest.generateIdSet()
        if(!ids) {
            if (!apiService.requireAnyParameters(params, response, ['ids', 'idlist','id'])) {
                return
            }
        }
        def result = performFlipJobFlagBulk(
                deleteRequest,
                'apiFlipExecutionEnabledBulk',
                [executionEnabled: params.status],
                'api.success.job.execution.'+(params.status?'enabled':'disabled')
        )
        def successful = result.success
        def errors=result.errors

        withFormat{
            xml{
                return apiService.renderSuccessXml(request,response) {
                    delegate.'toggleExecution'(
                            enabled: params.status,
                            requestCount: ids.size(),
                            allsuccessful: (successful.size() == ids.size())
                    ) {
                        if (successful) {
                            delegate.'succeeded'(count: successful.size()) {
                                successful.each { del ->
                                    delegate.'toggleExecutionResult'(id: del.id,) {
                                        delegate.'message'(del.message)
                                    }
                                }
                            }
                        }
                        if (errors) {
                            delegate.'failed'(count: errors.size()) {
                                errors.each { del ->
                                    delegate.'toggleExecutionResult'(id: del.id, errorCode: del.errorCode) {
                                        delegate.'error'(del.message)
                                    }
                                }
                            }
                        }
                    }
                }

            }
            json{
                return apiService.renderSuccessJson(response) {
                    requestCount= ids.size()
                    enabled=params.status
                    allsuccessful=(successful.size()==ids.size())
                    if(successful){
                        delegate.'succeeded'=array {
                            successful.each{del->
                                delegate.'element'(id:del.id,message:del.message)
                            }
                        }
                    }
                    if(errors){
                        delegate.'failed'=array {
                            errors.each{del->
                                delegate.'element'(id:del.id,errorCode:del.errorCode,message:del.message)
                            }
                        }
                    }
                }
            }
        }
    }
    def apiFlipScheduleEnabledBulk(ApiBulkJobDeleteRequest deleteRequest) {
        if(!apiService.requireVersion(request,response,ApiRequestFilters.V16)){
            return
        }
        if (deleteRequest.hasErrors()) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.invalid.request',
                                                           args: [deleteRequest.errors.allErrors.collect { g.message(error: it) }.join("; ")]])
        }
        log.debug("ScheduledExecutionController: apiFlipScheduleEnabledBulk : params: " + params)

        def ids = deleteRequest.generateIdSet()
        if(!ids) {
            if (!apiService.requireAnyParameters(params, response, ['ids', 'idlist','id'])) {
                return
            }
        }
        def result = performFlipJobFlagBulk(
                deleteRequest,
                'apiFlipScheduleEnabledBulk',
                [scheduleEnabled: params.status],
                'api.success.job.schedule.'+(params.status?'enabled':'disabled')
        )
        def successful = result.success
        def errors=result.errors

        withFormat{
            xml{
                return apiService.renderSuccessXml(request,response) {
                    delegate.'toggleSchedule'(
                            enabled: params.status,
                            requestCount: ids.size(),
                            allsuccessful: (successful.size() == ids.size())
                    ) {
                        if (successful) {
                            delegate.'succeeded'(count: successful.size()) {
                                successful.each { del ->
                                    delegate.'toggleScheduleResult'(id: del.id,) {
                                        delegate.'message'(del.message)
                                    }
                                }
                            }
                        }
                        if (errors) {
                            delegate.'failed'(count: errors.size()) {
                                errors.each { del ->
                                    delegate.'toggleScheduleResult'(id: del.id, errorCode: del.errorCode) {
                                        delegate.'error'(del.message)
                                    }
                                }
                            }
                        }
                    }
                }

            }
            json{
                return apiService.renderSuccessJson(response) {
                    requestCount= ids.size()
                    enabled=params.status
                    allsuccessful=(successful.size()==ids.size())
                    if(successful){
                        delegate.'succeeded'=array {
                            successful.each{del->
                                delegate.'element'(id:del.id,message:del.message)
                            }
                        }
                    }
                    if(errors){
                        delegate.'failed'=array {
                            errors.each{del->
                                delegate.'element'(id:del.id,errorCode:del.errorCode,message:del.message)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * POST a single job definition to a
     * @return
     */
    def apiJobCreateSingle(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        log.debug("ScheduledExecutionController: apiJobUpdateSingle " + params)
        def fileformat = params.format ?: 'xml'
        def parseresult

        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }
        if(ScheduledExecution.getByIdOrUUID(params.id)){
            //job already exists, cannot create
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_CONFLICT,
                    code: 'api.error.jobs.create.exists', args: [params.id]])
        }
        if (request.contentType.contains('text/xml')) {
            //read input stream
            parseresult = scheduledExecutionService.parseUploadedFile(request.getInputStream(), fileformat)
        } else {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.jobs.import.missing-file', args: null])
        }
        if (parseresult.errorCode) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: parseresult.errorCode, args: parseresult.args])
        }

        if (parseresult.error) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.jobs.import.invalid', args: [fileformat, parseresult.error]])
        }
        def jobset = parseresult.jobset
        if (jobset.size() != 1) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.jobs.update.incorrect-document-content'])
        }
        if (params.project) {
            jobset*.project = params.project
        }
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)

        if (!frameworkService.authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_TYPE_JOB,
                [AuthConstants.ACTION_CREATE], jobset[0].project)) {

            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Create Job', 'Project', jobset[0].project]])
        }

        jobset*.uuid = params.id
        def changeinfo = [user: session.user, method: 'apiJobCreateSingle']
        String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")
        def loadresults = scheduledExecutionService.loadJobs(jobset, 'create', 'preserve', changeinfo, authContext)
        scheduledExecutionService.issueJobChangeEvents(loadresults.jobChangeEvents)

        def jobs = loadresults.jobs
        def jobsi = loadresults.jobsi
        def msgs = loadresults.msgs
        def errjobs = loadresults.errjobs
        def skipjobs = loadresults.skipjobs

        if (jobs) {
            response.addHeader('Location', apiService.apiHrefForJob(jobs[0]))
            return apiService.renderSuccessXml(HttpServletResponse.SC_CREATED, false, request, response) {
                renderJobsImportApiXML(jobs, jobsi, errjobs, skipjobs, delegate)
            }
        } else {
            return apiService.renderSuccessXml(HttpServletResponse.SC_BAD_REQUEST, false, request, response) {
                renderJobsImportApiXML(jobs, jobsi, errjobs, skipjobs, delegate)
            }
        }
    }
    /**
     * Update a job via PUT
     * @return
     */
    def apiJobUpdateSingle(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        log.debug("ScheduledExecutionController: apiJobUpdateSingle " + params)
        def fileformat = params.format ?: 'xml'
        def parseresult
        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }
        def scheduledExecution = ScheduledExecution.getByIdOrUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution,['Job ID',params.id])) {
            //job does not exist
            return
        }
        def Framework framework = frameworkService.getRundeckFramework()
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_UPDATE],
                scheduledExecution.project)) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Update', 'Job ID', params.id]])
        }
        if(request.contentType.contains('text/xml')){
            //read input stream
            parseresult = scheduledExecutionService.parseUploadedFile(request.getInputStream(), fileformat)
        } else {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.jobs.import.missing-file', args: null])
        }
        if (parseresult.errorCode) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: parseresult.errorCode, args: parseresult.args])
        }

        if (parseresult.error) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.jobs.import.invalid', args: [fileformat, parseresult.error]])
        }
        def jobset = parseresult.jobset
        if(jobset.size()!=1){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.jobs.update.incorrect-document-content'])
        }
        if (params.project) {
            jobset*.project = params.project
        }
        jobset*.uuid=params.id
        def changeinfo = [user: session.user, method: 'apiJobUpdateSingle']
        String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")
        def loadresults = scheduledExecutionService.loadJobs(jobset, 'update', 'preserve', changeinfo, authContext)
        scheduledExecutionService.issueJobChangeEvents(loadresults.jobChangeEvents)

        def jobs = loadresults.jobs
        def jobsi = loadresults.jobsi
        def msgs = loadresults.msgs
        def errjobs = loadresults.errjobs
        def skipjobs = loadresults.skipjobs


        if (jobs) {
            return apiService.renderSuccessXmlWrap(request,response) {
                delegate.'link'(href: apiService.apiHrefForJob(jobs[0]), rel: 'get')
                success {
                    delegate.'message'(g.message(code: 'api.success.job.create.message', args: [params.id]))
                }
                renderJobsImportApiXML(jobs, jobsi, errjobs, skipjobs, delegate)
            }
        } else {
            return apiService.renderErrorXml(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response) {
                renderJobsImportApiXML(jobs, jobsi, errjobs, skipjobs, delegate)
            }
        }
    }
    /**
     * Delete a set of jobs as specified in the idlist parameter.
     * Only allowed via DELETE http method
     * API: DELETE job definitions: /api/5/jobs/delete, version 5
    */
    def apiJobDeleteBulk(ApiBulkJobDeleteRequest deleteRequest) {
        if (!apiService.requireApi(request, response)) {
            return
        }
        if (deleteRequest.hasErrors()) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.invalid.request', args: [deleteRequest.errors.allErrors.collect { g.message(error: it) }.join("; ")]])
        }
        log.debug("ScheduledExecutionController: apiJobDeleteBulk : params: " + params)
        if(!deleteRequest.ids && !deleteRequest.idlist) {
            if (!apiService.requireAnyParameters(params, response, ['ids', 'idlist'])) {
                return
            }
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        def ids = new HashSet<String>()
        if(params.id){
            ids.add(params.id)
        }else{
            if (deleteRequest.ids){
                ids.addAll(deleteRequest.ids)
            }
            if(deleteRequest.idlist){
                ids.addAll(deleteRequest.idlist.split(','))
            }
        }

        def successful = []
        def deleteerrs=[]
        ids.sort().each{jobid->
            def result = scheduledExecutionService.deleteScheduledExecutionById(jobid,authContext, false, session.user, 'apiJobDeleteBulk')
            if (result.errorCode) {
                deleteerrs << [id:jobid,errorCode:result.errorCode,message: g.message(code: result.errorCode, args: ['Job ID', jobid])]
            }else if (result.error) {
                deleteerrs<< result.error
            } else {
                successful<< result.success
            }
        }

        if (request.api_version < ApiRequestFilters.V14 && !(response.format in ['all','xml'])) {
            return apiService.renderErrorXml(response,[
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: [response.format]
            ])
        }
        withFormat{
            xml{
                return apiService.renderSuccessXml(request,response) {
                    delegate.'deleteJobs'(requestCount: ids.size(), allsuccessful:(successful.size()==ids.size())){
                        if(successful){
                            delegate.'succeeded'(count:successful.size()) {
                                successful.each{del->
                                    delegate.'deleteJobResult'(id:del.job.extid,){
                                        delegate.'message'(del.message)
                                    }
                                }
                            }
                        }
                        if(deleteerrs){
                            delegate.'failed'(count: deleteerrs.size()) {
                                deleteerrs.each{del->
                                    delegate.'deleteJobResult'(id:del.id,errorCode:del.errorCode){
                                        delegate.'error'(del.message)
                                    }
                                }
                            }
                        }
                    }
                }

            }
            json{
                return apiService.renderSuccessJson(response) {
                    requestCount= ids.size()
                    allsuccessful=(successful.size()==ids.size())
                    if(successful){
                        delegate.'succeeded'=array {
                            successful.each{del->
                                delegate.'element'(id:del.job.extid,message:del.message)
                            }
                        }
                    }
                    if(deleteerrs){
                        delegate.'failed'=array {
                            deleteerrs.each{del->
                                delegate.'element'(id:del.id,errorCode:del.errorCode,message:del.message)
                            }
                        }
                    }
                }
            }
        }
    }

    def edit (){
        log.debug("ScheduledExecutionController: edit : params: " + params)
        def scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if(!scheduledExecution) {
            flash.message = "ScheduledExecution not found with id ${params.id}"
            return redirect(action:index, params:params)
        }

        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (unauthorizedResponse(frameworkService.authorizeProjectJobAll(authContext, scheduledExecution,
                [AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_READ], scheduledExecution.project),
                AuthConstants.ACTION_UPDATE, 'Job', params.id)) {
            return
        }
        if (!params.project || params.project != scheduledExecution.project) {
            return redirect(controller: 'scheduledExecution', action: 'edit',
                    params: [id: params.id, project: scheduledExecution.project])
        }
        //clear session workflow
        if(session.editWF ){
            session.removeAttribute('editWF');
            session.removeAttribute('undoWF');
            session.removeAttribute('redoWF');
        }
        //clear session opts
        if(session.editOPTS ){
            session.removeAttribute('editOPTS');
            session.removeAttribute('undoOPTS');
            session.removeAttribute('redoOPTS');
        }
        def nodeStepTypes = frameworkService.getNodeStepPluginDescriptions()
        def stepTypes = frameworkService.getStepPluginDescriptions()
        def crontab = scheduledExecution.timeAndDateAsBooleanMap()
        return [ scheduledExecution:scheduledExecution, crontab:crontab,params:params,
                notificationPlugins: notificationService.listNotificationPlugins(),
                orchestratorPlugins: orchestratorPluginService.listDescriptions(),
                nextExecutionTime:scheduledExecutionService.nextExecutionTime(scheduledExecution),
                authorized:scheduledExecutionService.userAuthorizedForJob(request,scheduledExecution,authContext),
                nodeStepDescriptions: nodeStepTypes,
                stepDescriptions:stepTypes]
    }



    public def update (){
        withForm{
        Framework framework=frameworkService.getRundeckFramework()
        def changeinfo=[method:'update',change:'modify',user:session.user]

        //pass session-stored edit state in params map
        transferSessionEditState(session, params,params.id)
        def found = scheduledExecutionService.getByIDorUUID( params.id )
        if(!found) {
            flash.message = "ScheduledExecution not found with id ${params.id}"
            return redirect(action:index, params:params)
        }

        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,found.project)
        def result = scheduledExecutionService._doupdate(params, authContext, changeinfo)
        def scheduledExecution=result.scheduledExecution
        def success = result.success
        if(!scheduledExecution){
            flash.message = "ScheduledExecution not found with id ${params.id}"
            log.warn("update: there was no object by id: " +params.id+". redirecting to edit.")
            redirect(controller:'menu',action:'jobs')
        }else if(result.unauthorized){
            return renderUnauthorized(result.message)
        }else if (!success){
            if(scheduledExecution.errors){
                log.debug scheduledExecution.errors.allErrors.collect {g.message(error: it)}.join(", ")
            }
            request.message="Error updating Job "
            if(result.message){
                request.message+=": "+result.message
            }

//            if(!scheduledExecution.isAttached()) {
//                scheduledExecution.attach()
//            }else{
//                scheduledExecution.refresh()
//            }
            //update notification checkbox values
            NOTIFICATION_ENABLE_FIELD_NAMES.each{
                if(params[it]!='true'){
                    params[it]='false'
                }
            }
            def nodeStepTypes = frameworkService.getNodeStepPluginDescriptions()
            def stepTypes = frameworkService.getStepPluginDescriptions()
            return render(view:'edit', model: [scheduledExecution:scheduledExecution,
                       nextExecutionTime:scheduledExecutionService.nextExecutionTime(scheduledExecution),
                    notificationValidation: params['notificationValidation'],
                    nodeStepDescriptions: nodeStepTypes,
                    stepDescriptions: stepTypes,
                    notificationPlugins: notificationService.listNotificationPlugins(),
                    orchestratorPlugins: orchestratorPluginService.listDescriptions(),
                    params:params
                   ])
        }else{

            scheduledExecutionService.issueJobChangeEvent(result.jobChangeEvent)

            clearEditSession('_new')
            clearEditSession(scheduledExecution.id.toString())
            flash.savedJob=scheduledExecution
            flash.savedJobMessage="Saved changes to Job"
            scheduledExecutionService.logJobChange(changeinfo,scheduledExecution.properties)
            redirect(controller: 'scheduledExecution', action: 'show', params: [id: scheduledExecution.extid])
        }
        }.invalidToken{
            request.errorCode='request.error.invalidtoken.message'
            renderErrorView([:])
        }
    }

    def copy = {
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        //authorize
        if(unauthorizedResponse(frameworkService.authorizeProjectResourceAll(authContext,
                AuthConstants.RESOURCE_TYPE_JOB, [AuthConstants.ACTION_CREATE], params.project),
                AuthConstants.ACTION_CREATE,
                'New Job'
        )){
            return
        }
        log.debug("ScheduledExecutionController: create : params: " + params)

        if (unauthorizedResponse(frameworkService.authorizeProjectJobAll(authContext, scheduledExecution,
                [AuthConstants.ACTION_READ], scheduledExecution.project), AuthConstants.ACTION_READ, 'Job', params.id)) {
            return
        }

        def newScheduledExecution = new ScheduledExecution()

        def origprops = [:]+scheduledExecution.properties
        ScheduledExecution.transients.each{origprops.remove(it)}
        newScheduledExecution.properties = origprops
        if (newScheduledExecution.hasErrors()) {
            newScheduledExecution.errors.allErrors.each{log.warn("job copy data binding: "+it)}
        }
        newScheduledExecution.id=null
        newScheduledExecution.uuid=null
        newScheduledExecution.nextExecution=null
        //set session new workflow
        WorkflowController.getSessionWorkflow(session,null,new Workflow(scheduledExecution.workflow))
        if(scheduledExecution.options){
            def editopts = [:]

            scheduledExecution.options.each {Option opt ->
                editopts[opt.name] = opt.createClone()
            }
            EditOptsController.getSessionOptions(session,null,editopts)
        }
        def crontab = [:]
        if(newScheduledExecution.scheduled){
            crontab=newScheduledExecution.timeAndDateAsBooleanMap()
        }
        def nodeStepTypes = frameworkService.getNodeStepPluginDescriptions()
        def stepTypes = frameworkService.getStepPluginDescriptions()
		
        render(view:'create',model: [ scheduledExecution:newScheduledExecution, crontab:crontab,params:params,
                iscopy:true,
                authorized:scheduledExecutionService.userAuthorizedForJob(request,scheduledExecution,authContext),
                nodeStepDescriptions: nodeStepTypes,
                stepDescriptions: stepTypes,
                notificationPlugins: notificationService.listNotificationPlugins(),
                orchestratorPlugins: orchestratorPluginService.listDescriptions()])

    }
    /**
     * action to populate the Create form with execution info from a previous (transient) execution
     */
    def createFromExecution={

        log.debug("ScheduledExecutionController: create : params: " + params)
        Execution execution = Execution.get(params.executionId)

        if (notFoundResponse(execution, 'Execution', params.executionId)) {
            return
        }

        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,execution.project)

        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthConstants.RESOURCE_TYPE_JOB,
                        [AuthConstants.ACTION_CREATE],
                        params.project
                ),
                AuthConstants.ACTION_CREATE,
                'New Job'
        )) {
            return
        }
        if (unauthorizedResponse(frameworkService.authorizeProjectExecutionAll(authContext, execution,
                [AuthConstants.ACTION_READ]), AuthConstants.ACTION_READ, 'Execution',
                params.executionId)) {
            return
        }
        def props=[:]
        props.putAll(execution.properties)
        props.workflow=new Workflow(execution.workflow)
        if(params.failedNodes && 'true'==params.failedNodes){
            //replace the node filter with the failedNodeList from the execution
            props = props.findAll{!(it.key=~/^node(In|Ex)clude.*$/)}
            props.nodeIncludeName=execution.failedNodeList
        }
        params.putAll(props)
        //clear session workflow
        if(session.editWF ){
            session.removeAttribute('editWF');
            session.removeAttribute('undoWF');
            session.removeAttribute('redoWF');
        }
        if(session.editOPTS ){
            session.removeAttribute('editOPTS');
            session.removeAttribute('undoOPTS');
            session.removeAttribute('redoOPTS');
        }
        //store workflow in session
        def wf=WorkflowController.getSessionWorkflow(session,null,props.workflow)
        session.editWFPassThru=true

        def model=create.call()
        render(view:'create',model:model)
    }
    def create = {

        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)
        //authorize
        if (unauthorizedResponse(frameworkService.authorizeProjectResourceAll(authContext,
                AuthConstants.RESOURCE_TYPE_JOB, [AuthConstants.ACTION_CREATE],
                params.project),
                AuthConstants.ACTION_CREATE, 'New Job')) {
            return
        }

        log.debug("ScheduledExecutionController: create : params: " + params)
        def scheduledExecution = new ScheduledExecution()
        scheduledExecution.loglevel = servletContext.getAttribute("LOGLEVEL_DEFAULT")?servletContext.getAttribute("LOGLEVEL_DEFAULT"):"WARN"
        scheduledExecution.properties = params

        scheduledExecution.jobName = (params.command) ? params.command + " Job" : ""
        def cal = java.util.Calendar.getInstance()
        scheduledExecution.minute = String.valueOf(cal.get(java.util.Calendar.MINUTE))
        scheduledExecution.hour = String.valueOf(cal.get(java.util.Calendar.HOUR_OF_DAY))
        scheduledExecution.user = authContext.username
        scheduledExecution.userRoleList = authContext.roles.join(",")
        if(params.project ){

            if(!frameworkService.existsFrameworkProject(params.project) ) {
                scheduledExecution.errors.rejectValue('project','scheduledExecution.project.message',[params.project].toArray(),'FrameworkProject was not found: {0}')
            }
            scheduledExecution.argString=params.argString
        }
        if(params.filterName){
            if (params.filterName) {
                def User u = userService.findOrCreateUser(authContext.username)
                //load a named filter and create a query from it
                if (u) {
                    NodeFilter filter = NodeFilter.findByNameAndUser(params.filterName, u)
                    if (filter) {
                        def query2 = filter.createExtNodeFilters()
                        params.put('filter', query2.asFilter())
                    }
                }
            }
        }
        if (params.filter){
            scheduledExecution.filter=params.filter
            scheduledExecution.doNodedispatch=true
        }
        //clear session workflow
        if(session.editWFPassThru){
            //do not clear the session's editWF , as this action was called by createFromExecution
            session.removeAttribute('editWFPassThru')
        }else if (session.editWF ){
            session.removeAttribute('editWF');
            session.removeAttribute('undoWF');
            session.removeAttribute('redoWF');
        }//clear session workflow
        if(session.editOPTSPassThru){
            //do not clear the session's editWF , as this action was called by createFromExecution
            session.removeAttribute('editOPTSPassThru')
        }else if (session.editOPTS ){
            session.removeAttribute('editOPTS');
            session.removeAttribute('undoOPTS');
            session.removeAttribute('redoOPTS');
        }

        def nodeStepTypes = frameworkService.getNodeStepPluginDescriptions()
        def stepTypes = frameworkService.getStepPluginDescriptions()
        log.debug("ScheduledExecutionController: create : now returning model data to view...")
        return ['scheduledExecution':scheduledExecution,params:params,crontab:[:],
                nodeStepDescriptions: nodeStepTypes, stepDescriptions: stepTypes,
                notificationPlugins: notificationService.listNotificationPlugins(),
                orchestratorPlugins: orchestratorPluginService.listDescriptions()]
    }

    private clearEditSession(id='_new'){
        clearOPTSEditSession(id)
        clearWFEditSession(id)
    }
    private clearOPTSEditSession(id){
        session.editOPTS?.remove(id)
        session.undoOPTS?.remove(id)
        session.redoOPTS?.remove(id)
    }

    private clearWFEditSession(id) {
        session.editWF?.remove(id)
        session.undoWF?.remove(id)
        session.redoWF?.remove(id)
    }

    static void transferSessionEditState(session,params,id){
        //pass session-stored edit state in params map
        if ((params['_sessionwf'] in ['true',true]) && session.editWF && null != session.editWF[id]) {
            params['_sessionEditWFObject'] = session.editWF[id]
        }
        if ((params['_sessionopts'] in ['true',true]) && session.editOPTS && null != session.editOPTS[id]) {
            params['_sessionEditOPTSObject'] = session.editOPTS[id]
        }
    }

    /**
     * execute the job defined via input parameters, but do not store it.
     */
    def runAdhocInline(ApiRunAdhocRequest apiRunAdhocRequest){
        def results=[:]
        withForm{
            apiRunAdhocRequest.script=null
            apiRunAdhocRequest.url=null
            results=runAdhoc(apiRunAdhocRequest)
            if(results.failed){
                results.error=results.message
            } else {
                log.debug("ExecutionController: immediate execution scheduled (${results.id})")
            }
            g.refreshFormTokensHeader()
        }.invalidToken{
            results.error=g.message(code:'request.error.invalidtoken.message')
        }
        return render(contentType:'text/json'){
            if(results.error){
                delegate['error']=results.error
            }else{
                success='true'
                id=results.id
            }
        }
    }
    private def runAdhoc(ApiRunAdhocRequest runAdhocRequest){
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,runAdhocRequest.project)
        params["user"] = authContext.username
        params.request = request
        params.jobName='Temporary_Job'
        params.groupPath='adhoc'

        if (runAdhocRequest.asUser && apiService.requireVersion(request,response,ApiRequestFilters.V5)) {
            //authorize RunAs User
            if (!frameworkService.authorizeProjectResource(authContext, AuthConstants.RESOURCE_ADHOC,
                    AuthConstants.ACTION_RUNAS, runAdhocRequest.project)) {

                def msg = g.message(code: "api.error.item.unauthorized", args: ['Run as User', 'Run', 'Adhoc'])
                return [failed:true,error: 'unauthorized', message: msg]
            }
            params['user'] = runAdhocRequest.asUser
        }
        if(runAdhocRequest.exec){
            params.workflow = new Workflow(commands: [new CommandExec(adhocRemoteString: runAdhocRequest.exec, adhocExecution: true)])
        }else if(runAdhocRequest.script){
            params.workflow = new Workflow(commands: [new CommandExec(adhocLocalString: runAdhocRequest.script,
                                                                      adhocExecution: true,
                                                                      argString: runAdhocRequest.argString,
                                                                      scriptInterpreter: runAdhocRequest.scriptInterpreter,
                                                                      interpreterArgsQuoted: runAdhocRequest.interpreterArgsQuoted,
                                                                      fileExtension:runAdhocRequest.fileExtension)])
        }else if(runAdhocRequest.url){
            params.workflow = new Workflow(commands: [new CommandExec(adhocFilepath: runAdhocRequest.url, adhocExecution: true,
                                                                      argString: runAdhocRequest.argString,
                                                                      scriptInterpreter: runAdhocRequest.scriptInterpreter,
                                                                      interpreterArgsQuoted: runAdhocRequest.interpreterArgsQuoted,
                                                                      fileExtension:runAdhocRequest.fileExtension)])
        }else{

            def msg = g.message(code: "api.error.parameter.required", args: ["url, script, or exec"])
            return [failed:true,error: 'invalid', message: msg]
        }
        params.project=runAdhocRequest.project
        params.nodeKeepgoing= runAdhocRequest.nodeKeepgoing!=null?runAdhocRequest.nodeKeepgoing:true
        params.nodeThreadcount= runAdhocRequest.nodeThreadcount?:1
        params.description = runAdhocRequest.description ?: ""
        if (params.filterName) {
            def User u = userService.findOrCreateUser(authContext.username)
            //load a named filter and create a query from it
            if (u) {
                NodeFilter filter = NodeFilter.findByNameAndUser(params.filterName, u)
                if (filter) {
                    def query2 = filter.createExtNodeFilters()
                    params.put('filter',query2.asFilter())
                }
            }
        }

        //pass session-stored edit state in params map
        transferSessionEditState(session, params,'_new')
        def result= scheduledExecutionService._dovalidate(params,authContext)
        def ScheduledExecution scheduledExecution=result.scheduledExecution
        def failed=result.failed
        if(!failed){
            return _transientExecute(scheduledExecution,params,authContext)
        }else{
            return [success:false,failed:true,invalid:true,message:'Job configuration was incorrect.',scheduledExecution:scheduledExecution,params:params]
        }
    }

    /**
    * Execute a transient ScheduledExecution and return execution data: [execution:Execution,id:Long]
     * if there is an error, return [error:'type',message:errormesg,...]
     */
    private Map _transientExecute(ScheduledExecution scheduledExecution, Map params, AuthContext authContext){
        def object
        def isauth = scheduledExecutionService.userAuthorizedForAdhoc(params.request,scheduledExecution,authContext)
        if (!isauth){
            def msg=g.message(code:'unauthorized.job.run.user',args:[params.user])
            return [success:false,error:'unauthorized',message:msg]
        }

        if(!executionService.executionsAreActive){
            def msg=g.message(code:'disabled.execution.run')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        if (!scheduledExecution.hasExecutionEnabled()) {
            def msg=g.message(code:'scheduleExecution.execution.disabled')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        params.workflow=new Workflow(scheduledExecution.workflow)
        params.argString=scheduledExecution.argString
        params.doNodedispatch=scheduledExecution.doNodedispatch
        params.filter=scheduledExecution.asFilter()

        def Execution e
        try {
            e = executionService.createExecutionAndPrep(params, params.user)
        } catch (ExecutionServiceException exc) {
            return [success:false,error:'failed',message:exc.getMessage()]
        }

        return scheduledExecutionService.scheduleTempJob(authContext, e);
    }



    def save () {
        withForm{
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)
        def changeinfo=[user:session.user,change:'create',method:'save']

        //pass session-stored edit state in params map
        transferSessionEditState(session, params,'_new')
        def result = scheduledExecutionService._dosave(params, authContext, changeinfo)
        scheduledExecutionService.issueJobChangeEvent(result.jobChangeEvent)
        def scheduledExecution = result.scheduledExecution
        if(result.success && scheduledExecution.id){
            clearEditSession()
            flash.savedJob=scheduledExecution
            flash.savedJobMessage="Created new Job"
            scheduledExecutionService.logJobChange(changeinfo,scheduledExecution.properties)
            return redirect(controller:'scheduledExecution',action:'show',params:[id:scheduledExecution.extid])
        }else{
            if(scheduledExecution){
                scheduledExecution.errors.allErrors.each { log.warn(it.defaultMessage) }
                if(!scheduledExecution.jobName){
                    scheduledExecution.jobName=''
                }
            }
            if (result.unauthorized){
                request.message = result.error
            }else{
                request.message=g.message(code:'ScheduledExecutionController.save.failed')
            }
        }

        def nodeStepTypes = frameworkService.getNodeStepPluginDescriptions()
        def stepTypes = frameworkService.getStepPluginDescriptions()
        render(view: 'create', model: [scheduledExecution: scheduledExecution, params: params,
                                       nodeStepDescriptions: nodeStepTypes,
                stepDescriptions: stepTypes,
                notificationPlugins: notificationService.listNotificationPlugins(),
                orchestratorPlugins: orchestratorPluginService.listDescriptions(),
                notificationValidation:params['notificationValidation']
        ])
        }.invalidToken{
            request.errorCode='request.error.invalidtoken.message'
            renderErrorView([:])
        }
    }


    def upload(){

    }
    def uploadPost ={
        log.debug("ScheduledExecutionController: upload " + params)
        withForm{
        Framework framework = frameworkService.getRundeckFramework()
            UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)
        
        def fileformat = params.fileformat ?: 'xml'
        def parseresult
        if(params.xmlBatch && params.xmlBatch instanceof String) {
            String fileContent = params.xmlBatch
            parseresult = scheduledExecutionService.parseUploadedFile(fileContent, fileformat)
        } else if(params.xmlBatch && params.xmlBatch instanceof CommonsMultipartFile) {
            InputStream fileContent = params.xmlBatch.inputStream
            parseresult = scheduledExecutionService.parseUploadedFile(fileContent, fileformat)
        } else if (request instanceof MultipartHttpServletRequest) {
            def file = request.getFile("xmlBatch")
            if (!file || file.empty) {
                request.message = "No file was uploaded."
                return render(view: 'upload')
            }
            parseresult = scheduledExecutionService.parseUploadedFile(file.getInputStream(), fileformat)
        } else {
            request.message = "No file was uploaded."
            return render(view:'upload')
        }
        def jobset
        if(parseresult.errorCode){
            parseresult.error=message(code:parseresult.errorCode,args:parseresult.args)
        }
        if(parseresult.error){
            if(params.xmlreq){
                flash.error = parseresult.error
                return xmlerror()
            }else{
                request.error=parseresult.error
                return render(view:'upload')
            }
        }
        jobset=parseresult.jobset
        jobset*.project=params.project
        def changeinfo = [user: session.user,method:'upload']
        String roleList = request.subject.getPrincipals(Group.class).collect {it.name}.join(",")
        def loadresults = scheduledExecutionService.loadJobs(jobset, params.dupeOption, params.uuidOption,
                 changeinfo,authContext)
            scheduledExecutionService.issueJobChangeEvents(loadresults.jobChangeEvents)


        def jobs = loadresults.jobs
        def jobsi = loadresults.jobsi
        def msgs = loadresults.msgs
        def errjobs = loadresults.errjobs
        def skipjobs = loadresults.skipjobs

        if(!params.xmlreq){
            return render(view: 'upload',model: [jobs: jobs, errjobs: errjobs, skipjobs: skipjobs,
                nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), 
                messages: msgs,
                didupload: true])
        }else{
            //TODO: update commander's jobs upload task to submit XML content directly instead of via uploaded file, and use proper
            //TODO: grails content negotiation
            response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,"Jobs Uploaded. Succeeded: ${jobs.size()}, Failed: ${errjobs.size()}, Skipped: ${skipjobs.size()}")
                render(contentType:"text/xml"){
                    result(error:false){
                        renderJobsImportApiXML(jobs, jobsi, errjobs, skipjobs, delegate)
                    }
                }
        }
        }.invalidToken{
            request.warn=g.message(code:'request.error.invalidtoken.message')
            render(view: 'upload',params: [project:params.project])
        }
    }

    def execute = {
        return redirect(action: 'show',params:params)
    }

    private _prepareExecute(ScheduledExecution scheduledExecution, final def framework, final AuthContext authContext){
        def model=[scheduledExecution:scheduledExecution]
        model.authorized=true
        //test nodeset to make sure there are matches
        if(scheduledExecution.doNodedispatch){
            NodeSet nset = ExecutionService.filtersAsNodeSet(scheduledExecution)
            model.nodefilter=scheduledExecution.asFilter()
            //check nodeset filters for variable expansion
            def varfound = scheduledExecution.asFilter().contains("\${")
            if (varfound) {
                model.nodesetvariables = true
            }
            if (params.retryFailedExecId) {
                Execution e = Execution.get(params.retryFailedExecId)
                if (e && e.scheduledExecution?.id == scheduledExecution.id) {
                    model.failedNodes = e.failedNodeList
                    if(varfound){
                        nset = ExecutionService.filtersAsNodeSet([filter: OptsUtil.join("name:", e.failedNodeList)])
                    }
                    model.nodesetvariables = false
                }
            }
            def nodes = frameworkService.filterAuthorizedNodes(
                    scheduledExecution.project,
                    new HashSet<String>(["read", "run"]),
                    frameworkService.filterNodeSet(nset, scheduledExecution.project),
                    authContext).nodes;

            if(!nodes || nodes.size()<1){
                //error
                model.nodesetempty=true
            }
            else if(grailsApplication.config.gui.execution.summarizedNodes != 'false') {
                model.nodes=nodes
                model.nodemap=[:]
                model.tagsummary=[:]
                model.grouptags=[:]
                model.nodesSelectedByDefault=scheduledExecution.hasNodesSelectedByDefault()
                if (!model.nodesSelectedByDefault) {
                    model.selectedNodes = ""
                }
                //summarize node groups
                def namegroups=[other: new TreeList()]
                nodes.each{INodeEntry node->
                    def name=node.nodename
                    model.nodemap[name]=node
                    def matcher = (name=~ /^(.*\D)(\d+)/)
                    def matcher2 = (name =~ /^(\d+)(\D.*)/)
                    def groupname
                    if(matcher.matches()){
                        def pat=matcher.group(1)
                        def num = matcher.group(2)
                        groupname= pat + '.*'

                    }else if(matcher2.matches()){
                        def pat = matcher2.group(2)
                        def num = matcher2.group(1)
                        groupname= '.*' + pat

                    }else{
                        groupname='other'
                    }
                    if (!namegroups[groupname]) {
                        namegroups[groupname] = new TreeList()
                    }
                    namegroups[groupname] << name
                    //summarize tags
                    def tags = node.getTags()
                    if (tags) {
                        tags.each { tag ->
                            if (!model.tagsummary[tag]) {
                                model.tagsummary[tag] = 1
                            } else {
                                model.tagsummary[tag]++
                            }
                            if(!model.grouptags[groupname]){
                                model.grouptags[groupname]=[(tag):1]
                            }else if (!model.grouptags[groupname][tag]) {
                                model.grouptags[groupname][tag]=1
                            }else{
                                model.grouptags[groupname][tag]++
                            }
                        }
                    }
                }
                def singles=[]
                namegroups.keySet().grep {it!='other'&&namegroups[it].size() == 1}.each{
                    namegroups['other'].addAll(namegroups[it])
                    model.grouptags[it]?.each{tag,v->
                        if (!model.grouptags['other']) {
                            model.grouptags['other'] = [(tag): v]
                        } else if (!model.grouptags['other'][tag]) {
                            model.grouptags['other'][tag] = v
                        } else {
                            model.grouptags['other'][tag] += v
                        }
                    }
                    singles<<it
                }
                singles.each{
                    namegroups.remove(it)
                    model.grouptags.remove(it)
                }
                if(!namegroups['other']){
                    namegroups.remove('other')
                }

                model.namegroups=namegroups
            }else{
                model.nodes = nodes
            }

        }

        if(params.retryExecId){
            Execution e = Execution.get(params.retryExecId)
            if(e && e.scheduledExecution?.id == scheduledExecution.id){
                model.selectedoptsmap=FrameworkService.parseOptsFromString(e.argString)
                if (e.filter != scheduledExecution.filter) {

                    def retryNodes = frameworkService.filterAuthorizedNodes(
                            scheduledExecution.project,
                            new HashSet<String>([AuthConstants.ACTION_READ, AuthConstants.ACTION_RUN]),
                            frameworkService.filterNodeSet(
                                    ExecutionService.filtersAsNodeSet([filter:e.filter]),
                                    scheduledExecution.project
                            ),
                            authContext).nodes;

                    model.selectedNodes = retryNodes*.nodename.join(',')
                }
            }
        }else if(params.argString){
            model.selectedoptsmap = FrameworkService.parseOptsFromString(params.argString)
        }
        model.localNodeName=framework.getFrameworkNodeName()

        //determine option dependencies based on valuesURl embedded references
        //map of option name to list of option names which depend on it
        def depopts=[:]
        //map of option name to list of option names it depends on
        def optdeps=[:]
        boolean explicitOrdering=false
        def optionSelections=[:]
        scheduledExecution.options.each { Option opt->
            optionSelections[opt.name]=opt
            if(opt.sortIndex!=null){
                explicitOrdering=true
            }
            if(opt.realValuesUrl){
                (opt.realValuesUrl=~/\$\{option\.([^.}\s]+?)\.value\}/ ).each{match,oname->
                    if(oname==opt.name){
                        return
                    }
                    //add opt to list of dependents of oname
                    if(!depopts[oname]){
                        depopts[oname]=[opt.name]
                    }else{
                        depopts[oname] << opt.name
                    }
                    //add oname to list of dependencies of opt
                    if(!optdeps[opt.name]){
                        optdeps[opt.name]=[oname]
                    }else{
                        optdeps[opt.name] << oname
                    }
                }
            }
        }
        model.dependentoptions=depopts
        model.optiondependencies=optdeps

        //Option sort order will use sortIndex if set
        model.optionordering = scheduledExecution.options*.name

        //topo sort the dependencies
        def toporesult = toposort(scheduledExecution.options*.name, depopts, optdeps)
        if (scheduledExecution.options && !toporesult.result) {
            log.warn("Cyclic dependency for options for job ${scheduledExecution.extid}: (${toporesult.cycle})")
            model.optionsDependenciesCyclic = true
        }
        if (!explicitOrdering && toporesult.result) {
            model.optionordering = toporesult.result
        }


        //prepare dataset used by option view
        //includes dependency information, auto reload and for remote options, selected values
        def remoteOptionData = [:]
        (model.optionordering).each{optName->
            Option opt = optionSelections[optName]
            def optData = [
                    'optionDependencies': model.optiondependencies[optName],
                    'optionDeps': model.dependentoptions[optName],
                    optionAutoReload: model.dependentoptions[optName] && opt.enforced || model.selectedoptsmap && model.selectedoptsmap[optName]
            ];
            if (opt.realValuesUrl != null) {
                optData << [
                        'hasUrl': true,
                        'scheduledExecutionId': scheduledExecution.extid,
                        'selectedOptsMap': model.selectedoptsmap ? model.selectedoptsmap[optName] : '',
                        'loadonstart': !model.optiondependencies[optName] || model.optionsDependenciesCyclic,
                        'optionAutoReload': (model.dependentoptions[optName] || model.selectedoptsmap && model.selectedoptsmap[optName]) && !model.optionsDependenciesCyclic
                ]
            } else {
                optData['localOption'] = true;
            }
            remoteOptionData[optName] = optData
        }
        model.remoteOptionData=remoteOptionData

        return model
    }
    private deepClone(Map map) {
        def copy = [:]
        map.each { k, v ->
            if (v instanceof List){
                copy[k] = v.clone()
            }
            else {
                copy[k] = v
            }
        }
        return copy
    }

    /**
     * Return topo sorted list of nodes, if acyclic, preserving
     * order of input node list for independent nodes
     * @param nodes
     * @param oedgesin
     * @param iedgesin
     * @return
     */
    private toposort(List nodes,Map oedgesin,Map iedgesin){
        def Map oedges = deepClone(oedgesin)
        def Map iedges = deepClone(iedgesin)
        def l = new ArrayList()
        def s = new ArrayList(nodes.findAll {!iedges[it]})
        while(s){
            def n = s.first()
            s.remove(n)
            l.add(n)
            //for each node dependent on n
            def edges = new ArrayList()
            if(oedges[n]){
                edges.addAll(oedges[n])
            }
            edges.each{p->
                oedges[n].remove(p)
                iedges[p].remove(n)
                if(!iedges[p]){
                    s<<p
                }
            }
        }
        if (iedges.any {it.value} || oedges.any{it.value}){
            //cyclic graph
            return [cycle: iedges]
        }else{
            return [result:l]
        }
    }
    public def executeFragment(RunJobCommand runParams, ExtraCommand extra) {
        if ([runParams, extra].any { it.hasErrors() }) {
            request.errors = [runParams, extra].find { it.hasErrors() }.errors
        }
        Framework framework = frameworkService.getRundeckFramework()
        def scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if(unauthorizedResponse(frameworkService.authorizeProjectJobAll(authContext, scheduledExecution,
                [AuthConstants.ACTION_RUN], scheduledExecution.project), AuthConstants.ACTION_RUN,
                'Job',params.id,true
        )){
            return
        }
        def model = _prepareExecute(scheduledExecution, framework,authContext)
        model.nextExecution = scheduledExecutionService.nextExecutionTime(scheduledExecution)
        if(params.dovalidate){
            model.jobexecOptionErrors=session.jobexecOptionErrors
            model.selectedoptsmap=session.selectedoptsmap
            session.jobexecOptionErrors=null
            session.selectedoptsmap=null
            model.options=null
        }
        render(template:'execOptionsForm',model:model)
    }

    /**
     * Execute job specified by parameters, and return json results
     */
    public def runJobInline(RunJobCommand runParams, ExtraCommand extra) {
        def results=[:]
        withForm{
            if ([runParams, extra].any { it.hasErrors() }) {
                request.errors = [runParams, extra].find { it.hasErrors() }.errors
                return render(contentType: 'application/json') {
                    delegate.error='invalid'
                    delegate.message = "Invalid parameters: " + request.errors.allErrors.collect { g.message(error: it) }.join(", ")
                }
            }
            results = runJob()

            if(results.error=='invalid'){
                session.jobexecOptionErrors=results.errors
                session.selectedoptsmap=results.options
            }
        }.invalidToken{
            results.failed=true
            results.error='request.error.invalidtoken.message'
            results.message=g.message(code:'request.error.invalidtoken.message')
        }
        return render(contentType:'application/json'){
            if(results.failed){
                delegate.error=results.error
                delegate.message=results.message
            }else{
                delegate.success=true
                delegate.id=results.id
                delegate.href=createLink(controller: "execution",action: "follow",id: results.id)
                delegate.follow=(params.follow == 'true')
            }
        }
    }
    public def runJobNow(RunJobCommand runParams, ExtraCommand extra){
        if ([runParams, extra].any{it.hasErrors()}) {
            request.errors= [runParams, extra].find { it.hasErrors() }.errors
            def model = show()
            return render(view: 'show', model: model)
        }
        def results=[:]
        withForm{
            results = runJob()
        }.invalidToken{
            results.error="Invalid request token"
            results.code= HttpServletResponse.SC_BAD_REQUEST
            request.errorCode='request.error.invalidtoken.message'
        }
        if(results.failed){
            log.error(results.message)
            if(results.error=='unauthorized'){
                return render(view:"/common/execUnauthorized",model:results)
            }else {
                def model=show()
                results.error = results.remove('message')
                results.jobexecOptionErrors=results.errors
                results.selectedoptsmap=results.options
                results.putAll(model)
                results.options=null
                return render(view:'show',model:results)
            }
        }else if (results.error){
            log.error(results.error)
            if(results.code){
                response.setStatus (results.code)
            }
            return renderErrorView(results)
        }else if(params.follow=='true'){
            redirect(controller:"execution", action:"follow",id:results.id)
        }else {
            redirect(controller: "scheduledExecution", action: "show", id: params.id)
        }
    }
    private Map runJob () {
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if (!scheduledExecution) {
//            response.setStatus (404)
            return [error:"No Job found for id: " + params.id,code:404]
        }
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_RUN],
            scheduledExecution.project)) {
            return [success:false,failed:true,error:'unauthorized',message: "Unauthorized: Execute Job ${scheduledExecution.extid}"]
        }

        if(!executionService.executionsAreActive){
            def msg=g.message(code:'disabled.execution.run')
            return [success:false,failed:true,error:'disabled',message: msg]
        }

        if (!scheduledExecution.hasExecutionEnabled()) {
            def msg=g.message(code:'scheduleExecution.execution.disabled')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        if(params.extra?.debug=='true'){
            params.extra.loglevel='DEBUG'
        }
        Map inputOpts=[:]
        //add any option.* values, or nodeInclude/nodeExclude filters
        if(params.extra){
            inputOpts.putAll(params.extra.subMap(['nodeIncludeName', 'loglevel',/*'argString',*/ 'optparams', 'option', '_replaceNodeFilters', 'filter']).findAll { it.value })
            inputOpts.putAll(params.extra.findAll{it.key.startsWith('option.')||it.key.startsWith('nodeInclude')|| it.key.startsWith('nodeExclude')}.findAll { it.value })
        }
        def result = executionService.executeJob(scheduledExecution, authContext,session.user, inputOpts)

        if (result.error){
            result.failed=true
            return result
        }else{
            log.debug("ExecutionController: immediate execution scheduled")
//            redirect(controller:"execution", action:"follow",id:result.executionId)
            return [success:true, message:"immediate execution scheduled", id:result.executionId]
        }
    }


    /**
    * API Actions
     */


    /**
     * Utility, render content for jobs/import response
     */
    def renderJobsImportApiXML={jobs,jobsi,errjobs,skipjobs, delegate->
        delegate.'succeeded'(count:jobs.size()){
            jobsi.each { Map job ->
                delegate.'job'(index: job.entrynum,href: apiService.apiHrefForJob(job.scheduledExecution)) {
                    id(job.scheduledExecution.extid)
                    name(job.scheduledExecution.jobName)
                    group(job.scheduledExecution.groupPath ?: '')
                    project(job.scheduledExecution.project)
                    permalink(apiService.guiHrefForJob(job.scheduledExecution))
                }
            }
        }
        delegate.failed(count:errjobs.size()){
            errjobs.each{ Map job ->
                def jmap=[index:job.entrynum]
                if(job.scheduledExecution.id){
                    jmap.href=apiService.apiHrefForJob(job.scheduledExecution)
                }
                delegate.'job'(jmap){
                    if(job.scheduledExecution.id){
                        id(job.scheduledExecution.extid)
                        permalink(apiService.guiHrefForJob(job.scheduledExecution))
                    }
                    name(job.scheduledExecution.jobName)
                    group(job.scheduledExecution.groupPath?:'')
                    project(job.scheduledExecution.project)
                    StringBuffer sb = new StringBuffer()
                    job.scheduledExecution?.errors?.allErrors?.each{err->
                        if(sb.size()>0){
                            sb<<"\n"
                        }
                        sb << g.message(error:err)
                    }
                    if(job.errmsg){
                        if(sb.size()>0){
                            sb<<"\n"
                        }
                        sb<<job.errmsg
                    }
                    delegate.'error'(sb.toString())
                }
            }
        }
        delegate.skipped(count:skipjobs.size()){

            skipjobs.each{ Map job ->
                def jmap = [index: job.entrynum]
                if (job.scheduledExecution.id) {
                    jmap.href = apiService.apiHrefForJob(job.scheduledExecution)
                }
                delegate.'job'(jmap){
                    if(job.scheduledExecution.id){
                        id(job.scheduledExecution.extid)
                        permalink(apiService.guiHrefForJob(job.scheduledExecution))
                    }
                    name(job.scheduledExecution.jobName)
                    group(job.scheduledExecution.groupPath?:'')
                    project(job.scheduledExecution.project)
                    StringBuffer sb = new StringBuffer()
                    if(job.errmsg){
                        if(sb.size()>0){
                            sb<<"\n"
                        }
                        sb<<job.errmsg
                    }
                    delegate.'error'(sb.toString())
                }
            }
        }
    }/**
     * Utility, render content for jobs/import response
     */
    private def renderJobsImportApiJson(jobs,jobsi,errjobs,skipjobs, delegate){
        delegate.'succeeded'=delegate.array{
            jobsi.each { Map job ->
                delegate.element(
                        index: job.entrynum,
                        href: apiService.apiHrefForJob(job.scheduledExecution),
                        id:job.scheduledExecution.extid,
                        name:job.scheduledExecution.jobName,
                        group:job.scheduledExecution.groupPath ?: '',
                        project:job.scheduledExecution.project,
                        permalink:apiService.guiHrefForJob(job.scheduledExecution)
                )
            }
        }
        delegate.failed=delegate.array{
            errjobs.each{ Map job ->
                def jmap=[index:job.entrynum]
                if(job.scheduledExecution.id){
                    jmap.href=apiService.apiHrefForJob(job.scheduledExecution)
                    jmap.id=job.scheduledExecution.extid
                    jmap.permalink=apiService.guiHrefForJob(job.scheduledExecution)
                }
                StringBuffer sb = new StringBuffer()
                job.scheduledExecution?.errors?.allErrors?.each{err->
                    if(sb.size()>0){
                        sb<<"\n"
                    }
                    sb << g.message(error:err)
                }
                if(job.errmsg){
                    if(sb.size()>0){
                        sb<<"\n"
                    }
                    sb<<job.errmsg
                }
                jmap.'error'=(sb.toString())
                delegate.element(jmap + [name:(job.scheduledExecution.jobName),
                                           group:(job.scheduledExecution.groupPath?:''),
                                           project:(job.scheduledExecution.project)])
            }
        }
        delegate.skipped=delegate.array{

            skipjobs.each{ Map job ->
                def jmap = [index: job.entrynum]
                if (job.scheduledExecution.id) {
                    jmap.href = apiService.apiHrefForJob(job.scheduledExecution)
                    jmap.id=(job.scheduledExecution.extid)
                    jmap.permalink=apiService.guiHrefForJob(job.scheduledExecution)
                }
                StringBuffer sb = new StringBuffer()
                if(job.errmsg){
                    if(sb.size()>0){
                        sb<<"\n"
                    }
                    sb<<job.errmsg
                }
                jmap.'error'=(sb.toString())
                jmap.name=(job.scheduledExecution.jobName)
                jmap.group=(job.scheduledExecution.groupPath?:'')
                jmap.project=(job.scheduledExecution.project)
                delegate.element(jmap)
            }
        }
    }
    /**
     * API: /api/14/project/NAME/jobs/import
     */
    def apiJobsImportv14(){
        if(!apiService.requireVersion(request,response,ApiRequestFilters.V14)){
            return
        }
        return apiJobsImport()
    }
    /**
     * API: /jobs/import, version 1, deprecated since v14
     */
    def apiJobsImport(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        log.debug("ScheduledExecutionController: upload " + params)
        def fileformat = params.format ?: 'xml'
        def parseresult
        if(request.api_version >= ApiRequestFilters.V14 && request.format=='xml'){
            //xml input
            parseresult = scheduledExecutionService.parseUploadedFile(request.getInputStream(), 'xml')
        }else if(request.api_version >= ApiRequestFilters.V14 && request.format=='yaml'){
            //yaml input
            parseresult = scheduledExecutionService.parseUploadedFile(request.getInputStream(), 'yaml')
        }else if (!apiService.requireParameters(params,response,['xmlBatch'])) {
            return
        }else if (request instanceof MultipartHttpServletRequest) {
            def file = request.getFile("xmlBatch")
            if (!file) {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.jobs.import.missing-file', args: null])
            }
            parseresult = scheduledExecutionService.parseUploadedFile(file.getInputStream(), fileformat)
        }else if (params.xmlBatch) {
            String fileContent = params.xmlBatch
            parseresult = scheduledExecutionService.parseUploadedFile(fileContent, fileformat)
        }else{
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.jobs.import.missing-file', args: null])
        }
        if (parseresult.errorCode) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: parseresult.errorCode, args: parseresult.args])
        }

        if (parseresult.error) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.jobs.import.invalid', args: [fileformat,parseresult.error]])
        }
        def jobset = parseresult.jobset
        if(request.api_version >= ApiRequestFilters.V14){
            //require project parameter
            if(!apiService.requireParameters(params,response, ['project'])){
                return
            }
        }
        if(request.api_version >= ApiRequestFilters.V8){
            //v8 override project using parameter
            if(params.project){
                jobset*.project=params.project
            }
        }
        def changeinfo = [user: session.user,method:'apiJobsImport']
        def Framework framework = frameworkService.getRundeckFramework()
        //nb: loadJobs will get correct project auth context
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        String roleList = request.subject.getPrincipals(Group.class).collect {it.name}.join(",")
        def option = params.uuidOption
        if (request.api_version < ApiRequestFilters.V9) {
            option = null
        }
        def loadresults = scheduledExecutionService.loadJobs(jobset,params.dupeOption, option, changeinfo, authContext)
        scheduledExecutionService.issueJobChangeEvents(loadresults.jobChangeEvents)

        def jobs = loadresults.jobs
        def jobsi = loadresults.jobsi
        def msgs = loadresults.msgs
        def errjobs = loadresults.errjobs
        def skipjobs = loadresults.skipjobs

        //force hibernate session flush
        ScheduledExecution.withSession { session->
            session.flush()
        }
        withFormat{
            xml{

                apiService.renderSuccessXmlWrap(request,response){
                    renderJobsImportApiXML(jobs, jobsi, errjobs, skipjobs, delegate)
                }
            }
            json{

                apiService.renderSuccessJson(response){
                    renderJobsImportApiJson(jobs, jobsi, errjobs, skipjobs, delegate)
                }
            }
        }
    }

    /**
     * API: export job definition: /job/{id}, version 1
     */
    def apiJobExport(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        log.debug("ScheduledExecutionController: /api/job GET : params: " + params)
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if (!apiService.requireExists(response, scheduledExecution,['Job ID',params.id])) {
            return
        }
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_READ], scheduledExecution.project)) {
            return apiService.renderErrorXml(response,[status:HttpServletResponse.SC_FORBIDDEN,
                    code:'api.error.item.unauthorized',args:['Read','Job ID',params.id]])
        }
        if(!(response.format in ['all','xml','yaml'])){
            return apiService.renderErrorXml(response,[status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                                                       code:'api.error.item.unsupported-format',args:[response.format]])
        }
        withFormat{
            xml{
                def writer = new StringWriter()
                def xml = new MarkupBuilder(writer)
                JobsXMLCodec.encodeWithBuilder([scheduledExecution],xml)
                writer.flush()
                render(text:writer.toString(),contentType:"text/xml",encoding:"UTF-8")
            }
            yaml{
                render(text:JobsYAMLCodec.encode([scheduledExecution] as List),contentType:"text/yaml",encoding:"UTF-8")
            }
        }
    }
    /**
     * API: Run a job immediately: /job/{id}/run, version 1
     */
    def apiJobRun() {
        if (!apiService.requireApi(request, response)) {
            return
        }
        //require POST for api v14
        if (request.method == 'GET' && request.api_version >= ApiRequestFilters.V14) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
            return
        }
        def jobid=params.id
        def jobAsUser,jobArgString,jobLoglevel,jobFilter
        if(request.format=='json' ){
            def data= request.JSON
            jobAsUser = data?.asUser
            jobArgString = data?.argString
            jobLoglevel = data?.loglevel
            jobFilter = data?.filter
        }else{
            jobAsUser=params.asUser
            jobArgString=params.argString
            jobLoglevel=params.loglevel
        }
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(jobid)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', jobid])) {
            return
        }
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)

        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_RUN],
            scheduledExecution.project)) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Run', 'Job ID', jobid]])
        }
        def username=session.user
        if(jobAsUser && apiService.requireVersion(request,response,ApiRequestFilters.V5)){
            //authorize RunAs User
            if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_RUNAS],
                    scheduledExecution.project)) {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                        code: 'api.error.item.unauthorized', args: ['Run as User', 'Job ID', jobid]])
            }
            username= jobAsUser
        }
        def inputOpts = [:]

        if (jobArgString) {
            inputOpts["argString"] = jobArgString
        }
        if (jobLoglevel) {
            inputOpts["loglevel"] = jobLoglevel
        }
        //convert api parameters to node filter parameters
        def filters = jobFilter?[filter:jobFilter]:FrameworkController.extractApiNodeFilterParams(params)
        if (filters) {
            inputOpts['_replaceNodeFilters']='true'
            inputOpts['doNodedispatch']=true
            filters.each {k, v ->
                inputOpts[k] = v
            }
            if(null== inputOpts['nodeExcludePrecedence']){
                inputOpts['nodeExcludePrecedence'] = true
            }
        }


        if (request.api_version < ApiRequestFilters.V14 && !(response.format in ['all','xml'])) {
            return apiService.renderErrorXml(response,[
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: [response.format]
            ])
        }

        def result = executionService.executeJob(scheduledExecution, authContext, username, inputOpts)
        if(!result.success){
            if(result.error=='unauthorized'){
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                        code: 'api.error.item.unauthorized', args: ['Execute', 'Job ID', jobid]])
            }else if(result.error=='invalid'){
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.job.options-invalid', args: [result.message]])
            }else if(result.error=='conflict'){
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_CONFLICT,
                        code: 'api.error.execution.conflict', args: [result.message]])
            }else{
                //failed
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        code: 'api.error.execution.failed', args: [result.message]])
            }
        }
        def e = result.execution
        withFormat{
            xml{
                return executionService.respondExecutionsXml(request,response,[e])
            }
            json{
                return executionService.respondExecutionsJson(request,response,[e],[single:true])
            }
        }
    }

    /**
     * API: DELETE job definition: /job/{id}, version 1
     */
    def apiJobDelete() {
        if (!apiService.requireApi(request, response)) {
            return
        }
        log.debug("ScheduledExecutionController: /api/job DELETE : params: " + params)
        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', params.id])) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_DELETE],
                scheduledExecution.project)) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Delete', 'Job ID', params.id]])
        }
        def result = scheduledExecutionService.deleteScheduledExecutionById(params.id, authContext,
                false, session.user, 'apiJobDelete')
        if (!result.success) {
            if (result.error?.errorCode == 'notfound') {
                apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND, code: 'api.error.item.doesnotexist',
                        args: ['Job ID', params.id]])
            } else if (result.error?.errorCode == 'unauthorized') {
                apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                        code: 'api.error.item.unauthorized',
                        args: ['Delete', 'Job ID', params.id]]
                )
            } else {
                apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_CONFLICT,
                        code: 'api.error.job.delete.failed',
                        args: [result.error.message]]
                )
            }
        } else {
            //return 204 no content
            return render(status: HttpServletResponse.SC_NO_CONTENT)
        }
    }

    /**
     * API: DELETE /job/{id}/executions, version 12
     * delete all executions for a job
     */
    def apiJobExecutionsDelete(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        log.debug("ScheduledExecutionController: /api/job DELETE : params: " + params)
        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', params.id])) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (!frameworkService.authorizeApplicationResourceAny(authContext,
                frameworkService.authResourceForProject(scheduledExecution.project),
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN])) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Delete Execution', 'Project',
                    scheduledExecution.project]])
        }
        def result = scheduledExecutionService.deleteJobExecutions(scheduledExecution, authContext, session.user)
        executionService.renderBulkExecutionDeleteResult(request,response,result)
    }
    /**
     * API: run simple exec: /api/14/project/PROJECT/run/command
     */
    def apiRunCommandv14(ApiRunAdhocRequest runAdhocRequest){
        if(!apiService.requireVersion(request,response,ApiRequestFilters.V14)){
            return
        }
        return apiRunCommand(runAdhocRequest)
    }
    /**
     * API: run simple exec: /api/run/command, version 1
     */
    def apiRunCommand(ApiRunAdhocRequest runAdhocRequest){
        if(runAdhocRequest.hasErrors()){
            return apiService.renderErrorFormat(
                    response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: 'api.error.invalid.request',
                            args: [runAdhocRequest.errors.allErrors.collect { g.message(error: it) }.join("; ")]
                    ]
            )
        }
        if (!apiService.requireApi(request, response)) {
            return
        }
        if (null==runAdhocRequest.exec || null==runAdhocRequest.project){
            if(!apiService.requireParameters(params, response, ['project','exec'])) {
                return
            }
        }
        def project=runAdhocRequest.project?:params.project
        runAdhocRequest.project=project
        //test valid project
        def exists=frameworkService.existsFrameworkProject(project)
        if (!apiService.requireExists(response, exists, ['project', project])) {
            return
        }

        //remote any input parameters that should not be used when creating the execution
        ['options','scheduled'].each{params.remove(it)}

        //convert api parameters to node filter parameters
        def filters = runAdhocRequest.filter ? [filter: runAdhocRequest.filter] :
                FrameworkController.extractApiNodeFilterParams(params)
        if(filters){
            filters['doNodedispatch']=true
            filters.each{k,v->
                params[k]=v
            }
            if (null == params['nodeExcludePrecedence']) {
                params['nodeExcludePrecedence'] = true
            }
        }

        def results=runAdhoc(runAdhocRequest)
        return apiResponseAdhoc(results)
    }


    /**
     * API: run script: /api/14/project/PROJECT/run/script
     */
    def apiRunScriptv14(ApiRunAdhocRequest runAdhocRequest){
        if(!apiService.requireVersion(request,response,ApiRequestFilters.V14)){
            return
        }
        return apiRunScript(runAdhocRequest)
    }
    /**
     * API: run script: /api/run/script, version 1
     */
    def apiRunScript(ApiRunAdhocRequest runAdhocRequest){
        if (!apiService.requireApi(request, response)) {
            return
        }
        if(null==runAdhocRequest.project || null==runAdhocRequest.script) {
            if (!apiService.requireParameters(params, response, ['project', 'scriptFile'])) {
                return
            }
        }
        def project=runAdhocRequest.project?:params.project
        runAdhocRequest.project=project
        //test valid project

        def exists=frameworkService.existsFrameworkProject(project)
        if (!apiService.requireExists(response, exists, ['project', project])) {
            return
        }

        def script
        //read attached script content
        if(runAdhocRequest.script){

        }else if (request instanceof MultipartHttpServletRequest) {
            def file = request.getFile("scriptFile")
            if(!file) {
                return apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.run-script.upload.missing',args:['scriptFile']])
            }else if(file.empty) {
                return apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.run-script.upload.is-empty'])
            }
            runAdhocRequest.script = new String(file.bytes)
        }else if(params.scriptFile){
            runAdhocRequest.script=params.scriptFile
        }



        //remote any input parameters that should not be used when creating the execution
        ['options','scheduled'].each{params.remove(it)}
//        def scriptInterpreter = null
//        def interpreterArgsQuoted = false
//        if (request.api_version >= ApiRequestFilters.V8) {
//            scriptInterpreter = params.scriptInterpreter ?: null
//            interpreterArgsQuoted = Boolean.parseBoolean(params.interpreterArgsQuoted?.toString())
//        }


        //convert api parameters to node filter parameters
        def filters = runAdhocRequest.filter ? [filter: runAdhocRequest.filter] :
                FrameworkController.extractApiNodeFilterParams(params)
        if(filters){
            filters['doNodedispatch']=true
            filters.each{k,v->
                params[k]=v
            }
            if (null == params['nodeExcludePrecedence']) {
                params['nodeExcludePrecedence'] = true
            }
        }

        def results=runAdhoc(runAdhocRequest)
        return apiResponseAdhoc(results)
    }

    private apiResponseAdhoc(results){
        if (results.failed) {
            results.error = results.message
        }
        if (!results.success) {
            def errors = [results.error]
            if (results.scheduledExecution) {
                errors = []
                results.scheduledExecution.errors.allErrors.each {
                    errors << g.message(error: it)
                }
            }
            if (results.error == 'unauthorized') {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                        code: 'api.error.item.unauthorized', args: ['Execute', 'Adhoc', 'Command']])
            } else if (results.error == 'invalid') {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.execution.invalid', args: [errors.join(", ")]])
            } else {
                //failed
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        code: 'api.error.execution.failed', args: [errors.join(", ")]])
            }
        } else {
            if (request.api_version < ApiRequestFilters.V14 && !(response.format in ['all','xml'])) {
                return apiService.renderErrorFormat(response,[
                        status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                        code: 'api.error.item.unsupported-format',
                        args: [response.format]
                ])
            }
            withFormat{
                xml{

                    return apiService.renderSuccessXml(request,response) {
                        if (apiService.doWrapXmlResponse(request)) {
                            delegate.'success' {
                                message("Immediate execution scheduled (${results.id})")
                            }
                        }
                        delegate.'execution'(
                                id: results.id,
                                href: apiService.apiHrefForExecution(results.execution),
                                permalink: apiService.guiHrefForExecution(results.execution)
                        )
                    }
                }
                json{
                    return apiService.renderSuccessJson(response) {
                        delegate.'message'=("Immediate execution scheduled (${results.id})")
                        delegate.'execution' = [
                                id       : results.id,
                                href     : apiService.apiHrefForExecution(results.execution),
                                permalink: apiService.guiHrefForExecution(results.execution)
                        ]
                    }
                }
            }
        }
    }

    /**
     * API: run script: /api/14/project/PROJECT/run/url
     */
    def apiRunScriptUrlv14 (ApiRunAdhocRequest runAdhocRequest){
        if(!apiService.requireVersion(request,response,ApiRequestFilters.V14)){
            return
        }
        return apiRunScriptUrl(runAdhocRequest)
    }

    /**
     * API: run script: /api/run/url, version 4
     */
    def apiRunScriptUrl (ApiRunAdhocRequest runAdhocRequest){
        if (!apiService.requireApi(request, response)) {
            return
        }
        if (!apiService.requireVersion(request,response,ApiRequestFilters.V4)) {
            return
        }
        if(null==runAdhocRequest.project || null==runAdhocRequest.url) {
            if (!apiService.requireParameters(params, response, ['project', 'scriptURL'])) {
                return
            }
        }
        def project=runAdhocRequest.project?:params.project
        runAdhocRequest.project=project
        //test valid project
        def exists=frameworkService.existsFrameworkProject(project)
        if (!apiService.requireExists(response, exists, ['project', project])) {
            return
        }

        //remote any input parameters that should not be used when creating the execution
        ['options', 'scheduled'].each {params.remove(it)}

        if(null==runAdhocRequest.url && params.scriptURL){
            runAdhocRequest.url=params.scriptURL
        }


        //convert api parameters to node filter parameters
        def filters = runAdhocRequest.filter ? [filter: runAdhocRequest.filter] :
                FrameworkController.extractApiNodeFilterParams(params)
        if (filters) {
            filters['doNodedispatch'] = true
            filters.each {k, v ->
                params[k] = v
            }
            if (null == params['nodeExcludePrecedence']) {
                params['nodeExcludePrecedence'] = true
            }
        }

        def results = runAdhoc(runAdhocRequest)
        return apiResponseAdhoc(results)
    }
    /**
     * API: /api/job/{id}/executions , version 1
     */
    def apiJobExecutions (){
        if (!apiService.requireApi(request, response)) {
            return
        }
        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', params.id])) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)

        if (!frameworkService.authorizeProjectJobAll(
                authContext,
                scheduledExecution,
                [AuthConstants.ACTION_READ],
                scheduledExecution.project
        )
        ) {

            return apiService.renderErrorFormat(
                    response,
                    [
                            status: HttpServletResponse.SC_FORBIDDEN,
                            code: 'api.error.item.unauthorized',
                            args: ['Read', 'Job ID', params.id]
                    ]
            )
        }

        if (request.api_version < ApiRequestFilters.V14 && !(response.format in ['all','xml'])) {
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: [response.format]
            ])
        }

        def result = executionService.queryJobExecutions(
                scheduledExecution,
                params['status'],
                params.offset ? params.int('offset') : 0,
                params.max  ? params.int('max') : -1

        )
        def resOffset = params.offset ? params.int('offset') : 0
        def resMax = params.max ?
                params.int('max') :
                grailsApplication.config.rundeck?.pagination?.default?.max ?
                        grailsApplication.config.rundeck.pagination.default.max.toInteger() :
                        20
        def total=result.total
        withFormat{
            xml{
                return executionService.respondExecutionsXml(request,response,result.result,[total:total,offset:resOffset,max:resMax])
            }
            json{
                return executionService.respondExecutionsJson(request,response,result.result,[total:total,offset:resOffset,max:resMax])
            }
        }

    }
    /**
     * API: /api/14/scheduler/takeover
     */
    def apiJobClusterTakeoverSchedule (){
        if (!apiService.requireVersion(request,response,ApiRequestFilters.V14)) {
            return
        }
        def api17 = request.api_version >= ApiRequestFilters.V17

        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        //test valid project

        if (!frameworkService.authorizeApplicationResource(authContext, AuthConstants.RESOURCE_TYPE_JOB,
                AuthConstants.ACTION_ADMIN)) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized',
                    args: [
                            'Reschedule Jobs (admin)',
                            'Server',
                            frameworkService.getServerUUID()
                    ]
            ])
        }
        if (!frameworkService.isClusterModeEnabled()) {
            withFormat {
                xml {
                    return apiService.renderSuccessXmlWrap(request, response) {
                        delegate.'message'("No action performed, cluster mode is not enabled.")
                    }
                }
                json {

                    return apiService.renderSuccessJson(response) {
                        delegate.'message'=("No action performed, cluster mode is not enabled.")
                        success=true
                        apiversion=ApiRequestFilters.API_CURRENT_VERSION
                        self=[server:[uuid:frameworkService.getServerUUID()]]
                    }
                }
            }
        }

        String serverUUID=null
        boolean serverAll=false
        String project=null
        def jobid=null
        if(request.format=='json' ){
            def data= request.JSON
            serverUUID = data?.server?.uuid?:null
            serverAll = data?.server?.all?true:false
            project = data?.project?:null
            jobid = data?.job?.id?:null
        }else if(request.format=='xml' || !request.format){
            def data= request.XML
            if(data.name()=='server'){
                serverUUID = data.'@uuid'?.text()?:null
                serverAll = data.'@all'?.text()=='true'
            }else if(data.name()=='takeoverSchedule'){
                serverUUID = data.server?.'@uuid'?.text()?:null
                serverAll = data.server?.'@all'?.text()=='true'
                project = data.project?.'@name'?.text()?:null
                jobid = data.job?.'@id'?.text()?:null
            }
        }else{
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.invalid.request',
                    args: ['Expected content of type text/xml or text/json, content was of type: ' + request.format]])
        }
        if (!serverUUID && !serverAll&& !jobid) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.invalid.request', args: ['Expected server.uuid or server.all or job.id in request.']])
        }

        def reclaimMap=scheduledExecutionService.reclaimAndScheduleJobs(serverUUID,serverAll,project,jobid)
        def successCount=reclaimMap.findAll {it.value.success}.size()
        def failedCount = reclaimMap.size() - successCount
        //TODO: retry for failed reclaims?

        def jobData = { entry ->
            def dat=[
                    id: entry.key,
                    href: apiService.apiHrefForJob(entry.value.job),
                    permalink:apiService.guiHrefForJob(entry.value.job),

            ]
            if(api17){
                dat['previous-owner']=entry.value.previous
            }
            dat
        }
        def jobLink={ delegate, entry->
            delegate.'job'(jobData(entry))
        }
        def successMessage= "Schedule Takeover successful for ${successCount}/${reclaimMap.size()} Jobs."
        withFormat {
            xml{
                return apiService.renderSuccessXml(request,response) {
                    if (apiService.doWrapXmlResponse(request)) {
                        delegate.'message'(successMessage)
                        delegate.'self'{
                            delegate.'server'(uuid:frameworkService.getServerUUID())
                        }
                    }
                    delegate.'takeoverSchedule'{
                        if(!apiService.doWrapXmlResponse(request)){
                            delegate.'self' {
                                delegate.'server'(uuid: frameworkService.getServerUUID())
                            }
                        }
                        if(!serverAll) {
                            delegate.'server'(uuid: serverUUID)
                        }else{
                            delegate.'server'(all: true)
                        }
                        if(project){
                            delegate.'project'(name:project)
                        }
                        if(jobid){
                            delegate.'job'(id:jobid)
                        }
                        delegate.'jobs'(total: reclaimMap.size()){
                            delegate.'successful'(count: successCount) {
                                reclaimMap.findAll { it.value.success }.each(jobLink.curry(delegate))
                            }
                            delegate.'failed'(count: failedCount) {
                                reclaimMap.findAll { !it.value.success }.each(jobLink.curry(delegate))
                            }
                        }
                    }
                }
            }
            json{
                def datamap=serverAll?[server:[all:true]]:[server:[uuid: serverUUID]]
                if(project){
                    datamap.project=project
                }
                render(contentType: "application/json",text: [
                    success:true,
                    apiversion:ApiRequestFilters.API_CURRENT_VERSION,
                    message: successMessage,
                    self:[server:[uuid:frameworkService.getServerUUID()]],
                    takeoverSchedule:datamap+[
                        jobs:[
                            total:reclaimMap.size(),
                            successful:reclaimMap.findAll { it.value.success }.collect (jobData),
                            failed:reclaimMap.findAll { !it.value.success }.collect (jobData)
                        ]
                    ]
                ] as JSON)
            }
        }
    }
}

class JobXMLException extends Exception{

    public JobXMLException() {
        super();
    }

    public JobXMLException(String s) {
        super(s);
    }

    public JobXMLException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public JobXMLException(Throwable throwable) {
        super(throwable);
    }

}

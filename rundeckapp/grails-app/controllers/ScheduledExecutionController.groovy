import org.quartz.*

import com.dtolabs.rundeck.core.common.Framework
import java.text.SimpleDateFormat

import org.springframework.web.multipart.MultipartHttpServletRequest
import java.util.regex.Pattern
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.params.HttpClientParams

import org.codehaus.groovy.grails.web.json.JSONElement
import com.dtolabs.rundeck.core.utils.NodeSet
import groovy.xml.MarkupBuilder
import com.dtolabs.client.utils.Constants
import org.apache.log4j.Logger

import org.apache.commons.httpclient.util.DateUtil
import org.apache.commons.httpclient.util.DateParseException
import org.apache.log4j.MDC

import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.UsernamePasswordCredentials

import javax.security.auth.Subject
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.common.INodeEntry
import org.apache.commons.collections.list.TreeList
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import com.dtolabs.rundeck.app.api.ApiBulkJobDeleteRequest
import org.apache.commons.httpclient.SimpleHttpConnectionManager

class ScheduledExecutionController  {
    def Scheduler quartzScheduler
    def ExecutionService executionService
    def FrameworkService frameworkService
    def ScheduledExecutionService scheduledExecutionService

 
    def index = { redirect(controller:'menu',action:'jobs',params:params) }

    // the delete, save and update actions only
    // accept POST requests
    def static allowedMethods = [delete:'POST',
        save:'POST',
        update:'POST',
        apiJobsImport:'POST',
        apiJobDelete:'DELETE',
        apiJobAction:['GET','DELETE'],
        apiRunScript:'POST',
        apiJobDeleteBulk:['DELETE','POST']
    ]

    def cancel = {
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
            redirect(action:show,params:[id:params.id])
        }else{
            redirect(action:index)
        }
    }
    def list = {redirect(action:index,params:params) }

    def groupTreeFragment = {
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def tree = scheduledExecutionService.getGroupTree(params.project,framework)
        render(template:"/menu/groupTree",model:[jobgroups:tree,jscallback:params.jscallback])
    }

    def error={
        withFormat{
            html{
                return render(template:"/common/error")
            }
            xml {
                return xmlerror.call()
            }
        }
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
    def detailFragment = {
//        def model=show()

        log.debug("ScheduledExecutionController: show : params: " + params)
        def crontab = [:]
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if (!scheduledExecution) {
            log.error("No Job found for id: " + params.id)
            flash.error="No Job found for id: " + params.id
            response.setStatus (404)
            return error.call()
        }
        crontab = scheduledExecution.timeAndDateAsBooleanMap()
        def User user = User.findByLogin(session.user)
        //list executions using query params and pagination params

        def executions=Execution.findAllByScheduledExecution(scheduledExecution,[offset: params.offset?params.offset:0, max: params.max?params.max:10, sort:'dateStarted', order:'desc'])

        def total = Execution.countByScheduledExecution(scheduledExecution)

        //todo: authorize job for workflow_read



        return render(view:'jobDetailFragment',model: [scheduledExecution:scheduledExecution, crontab:crontab, params:params,
            executions:executions,
            total:total,
            nextExecution:scheduledExecutionService.nextExecutionTime(scheduledExecution),
            max: params.max?params.max:10,
            offset:params.offset?params.offset:0])
    }
    def show = {
        log.debug("ScheduledExecutionController: show : params: " + params)
        def crontab = [:]
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if (!scheduledExecution) {
            log.error("No Job found for id: " + params.id)
            flash.error="No Job found for id: " + params.id
            response.setStatus (404)
            return error.call()
        }
        if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_READ], scheduledExecution.project)) {
            return unauthorized("Read Job ${params.id}")
        }
        crontab = scheduledExecution.timeAndDateAsBooleanMap()
        def User user = User.findByLogin(session.user)
        //list executions using query params and pagination params

        def executions=Execution.findAllByScheduledExecution(scheduledExecution,[offset: params.offset?params.offset:0, max: params.max?params.max:10, sort:'dateStarted', order:'desc'])

        def total = Execution.countByScheduledExecution(scheduledExecution)

        //todo: authorize job for workflow_read


        withFormat{
            html{
                [scheduledExecution:scheduledExecution, crontab:crontab, params:params,
            executions:executions,
            total:total,
            nextExecution:scheduledExecutionService.nextExecutionTime(scheduledExecution),
            max: params.max?params.max:10,
            offset:params.offset?params.offset:0]

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
        if (!scheduledExecution) {
            log.error("No Job found for id: " + params.id)
            flash.error="No Job found for id: " + params.id
            response.setStatus (404)
            return error.call()
        }
        if(!params.option){
            log.error("option missing")
            flash.error="option missing"
            response.setStatus (404)
            return error.call()
        }
        
        //see if option specified, and has url
        if (scheduledExecution.options && scheduledExecution.options.find {it.name == params.option}) {
            def Option opt = scheduledExecution.options.find {it.name == params.option}
            def values=[]
            if (opt.realValuesUrl) {
                //load expand variables in URL source
                String srcUrl = expandUrl(opt, opt.realValuesUrl.toExternalForm(), scheduledExecution,params.extra?.option)
                String cleanUrl=srcUrl.replaceAll("^(https?://)([^:@/]+):[^@/]*@",'$1$2:****@');
                def remoteResult=[:]
                def result=null
                def remoteStats=[startTime: System.currentTimeMillis(), httpStatusCode: "", httpStatusText: "", contentLength: "", url: srcUrl,durationTime:"",finishTime:"", lastModifiedDateTime:""]
                def err = [:]
                try {
                    remoteResult = getRemoteJSON(srcUrl, 10)
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
                return error.call()
            }
        }else{
            return error.call()
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
        argString:'argString',
        adhoc:'adhocExecution'
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
    String expandUrl(Option opt, String url, ScheduledExecution scheduledExecution,selectedoptsmap=[:]) {
        def invalid = []
        String srcUrl = url.replaceAll(/(\$\{(job|option)\.([^\.}]+?(\.value)?)\})/,
            {Object[] group ->
                if(group[2]=='job' && jobprops[group[3]] && scheduledExecution.properties.containsKey(jobprops[group[3]])) {
                    scheduledExecution.properties.get(jobprops[group[3]]).toString().encodeAsURL()
                }else if(group[2]=='option' && optprops[group[3]] && opt.properties.containsKey(optprops[group[3]])) {
                    opt.properties.get(optprops[group[3]]).toString().encodeAsURL()
                }else if(group[2]=='option' && group[4]=='.value' ) {
                    def optname= group[3].substring(0, group[3].length() - '.value'.length())
                    def value=selectedoptsmap&& selectedoptsmap instanceof Map?selectedoptsmap[optname]:null
                    //find option with name
                    def Option expopt = scheduledExecution.options.find {it.name == optname}
                    if(value && expopt.multivalued && (value instanceof Collection || value instanceof String[])){
                        value = value.join(expopt.delimiter)
                    }
                    value?:''
                } else {
                    invalid << group[0]
                    group[0]
                }
            }
        )
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
    def Object getRemoteJSON(String url, int timeout){
        //attempt to get the URL JSON data
        def stats=[:]
        if(url.startsWith("http:") || url.startsWith("https:")){
            final HttpClientParams params = new HttpClientParams()
            params.setConnectionManagerTimeout(timeout*1000)
            params.setSoTimeout(timeout*1000)
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
            if(!parse ){
                throw new Exception("JSON was empty")
            }
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

    /**
    */
    def delete = {
        log.debug("ScheduledExecutionController: delete : params: " + params)
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if(scheduledExecution) {
            if(!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_DELETE],
                scheduledExecution.project)){
                return unauthorized("Delete Job: ${params.id}")
            }
            def changeinfo=[user:session.user,method:'delete',change:'delete']
            def jobdata=scheduledExecution.properties
            def jobtitle=scheduledExecution.jobName
            def result = scheduledExecutionService.deleteScheduledExecution(scheduledExecution)

            if (!result.success) {
                flash.error = result.error
                return redirect(action: show, id: params.id)
            }else{
                logJobChange(changeinfo, jobdata)
                flash.message = "Job '${jobtitle}' was successfully deleted."
                redirect(action:index, params:[:])
            }
        } else {
            flash.message = "ScheduledExecution not found with id ${params.id}"
            redirect(action:index, params:params)
        }
    }

    /**
     * Delete a set of jobs as specified in the idlist parameter.
     * Only allowed via DELETE http method
     * API: DELETE job definitions: /api/5/jobs/delete, version 5
    */
    def apiJobDeleteBulk = {ApiBulkJobDeleteRequest deleteRequest->
        log.debug("ScheduledExecutionController: apiJobDeleteBulk : params: " + params)
        if(!deleteRequest.ids && !deleteRequest.idlist){
            request.error = g.message(code: "api.error.parameter.required", args: ['ids or idlist'])
            return new ApiController().error()
        }
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)

        def ids = new HashSet<String>()
        if(deleteRequest.ids){
            ids.addAll(deleteRequest.ids)
        }
        if(deleteRequest.idlist){
            ids.addAll(deleteRequest.idlist.split(','))
        }

        def successful = []
        def deleteerrs=[]
        ids.sort().each{jobid->

            def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(jobid)
            if (!scheduledExecution) {
                deleteerrs << [
                        message: g.message(code: "api.error.item.doesnotexist", args: ['Job ID', jobid]),
                        errorCode: 'notfound',
                        id: jobid
                ]
                return
            }
            if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_DELETE], scheduledExecution.project)) {
                deleteerrs << [
                        message:g.message(code: 'api.error.item.unauthorized', args: ['Delete', 'Job ID', scheduledExecution.extid]),
                        errorCode: 'unauthorized',
                        id: scheduledExecution.extid,
                        job:scheduledExecution
                ]
                return
            }
            def changeinfo = [user: session.user, method: 'apiJobDeleteBulk', change: 'delete']
            def jobdata = scheduledExecution.properties
            def jobtitle = "[" + scheduledExecution.extid + "] " + scheduledExecution.generateFullName()
            def result = scheduledExecutionService.deleteScheduledExecution(scheduledExecution)
            if (!result.success) {
                deleteerrs<< [message:result.error,job:scheduledExecution,errorCode:'failed',id:scheduledExecution.extid]
            } else {
                logJobChange(changeinfo, jobdata)
                successful<< [message:g.message(code: 'api.success.job.delete.message', args: [jobtitle]),job:scheduledExecution]
            }
        }
        return new ApiController().success { delegate ->
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

    def edit = {
        log.debug("ScheduledExecutionController: edit : params: " + params)
        def scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def crontab = [:]
        if(!scheduledExecution) {
            flash.message = "ScheduledExecution not found with id ${params.id}"
            return redirect(action:index, params:params)
        }

        if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_READ], scheduledExecution.project)) {
            return unauthorized("Update Job ${params.id}")
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
        crontab = scheduledExecution.timeAndDateAsBooleanMap()
        return [ scheduledExecution:scheduledExecution, crontab:crontab,params:params,
            nextExecutionTime:scheduledExecutionService.nextExecutionTime(scheduledExecution),
            authorized:scheduledExecutionService.userAuthorizedForJob(request,scheduledExecution,framework), projects: frameworkService.projects(framework)]
    }

    static Logger jobChangeLogger = Logger.getLogger("com.dtolabs.rundeck.data.jobs.changes")

    private logJobChange(data, jobdata) {
        data.keySet().each {k ->
            def v = data[k]
            if (v instanceof Date) {
                //TODO: reformat date
                MDC.put(k, v.toString())
                MDC.put("${k}Time", v.time.toString())
            } else if (v instanceof String) {
                MDC.put(k, v ? v : "-")
            } else {
                final string = v.toString()
                MDC.put(k, string ? string : "-")
            }
        }
        ['id','jobName','groupPath','project'].each {k ->
            final var = jobdata[k]
            MDC.put(k, var ? var : '-')
        }
        if(jobdata.uuid){
            MDC.put('id',jobdata.uuid)
        }
        final msg = data.user + " " + data.change.toUpperCase() + " [" + (jobdata.uuid?:jobdata.id) + "] "+jobdata.project+" \"" + (jobdata.groupPath ? jobdata.groupPath : '') + "/" + jobdata.jobName + "\" (" + data.method+")"
        jobChangeLogger.info(msg)
        data.keySet().each {k ->
            if (data[k] instanceof Date) {
                //reformat date
                MDC.remove(k + 'Time')
            }
            MDC.remove(k)
        }
        ['id', 'jobName', 'groupPath', 'project'].each {k ->
            MDC.remove(k)
        }
    }

    def update = {
        def changeinfo=[method:'update',change:'modify',user:session.user]
        def result = _doupdate(params,changeinfo)
        def scheduledExecution=result.scheduledExecution
        def success = result.success
        if(!scheduledExecution){
            flash.message = "ScheduledExecution not found with id ${params.id}"
            log.warn("update: there was no object by id: " +params.id+". redirecting to edit.")
            redirect(controller:'menu',action:'jobs')
        }else if(result.unauthorized){
            return unauthorized(result.message)
        }else if (!success){
            if(scheduledExecution.errors){
                log.debug scheduledExecution.errors.allErrors.collect {g.message(error: it)}.join(", ")
            }
            flash.message="Error updating scheduled command "
            if(result.message >2){
                flash.message= flash.message+": "+result.message
            }
            log.debug("update operation failed. redirecting to edit ...")

            if(!scheduledExecution.isAttached()) {
                scheduledExecution.attach()
            }else{
                scheduledExecution.refresh()
            }
            Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
            render(view:'edit',model:[scheduledExecution:scheduledExecution,
                       nextExecutionTime:scheduledExecutionService.nextExecutionTime(scheduledExecution), projects: frameworkService.projects(framework)],
                   params:[project:params.project])
        }else{
            flash.savedJob=scheduledExecution
            flash.savedJobMessage="Saved changes to Job"
            logJobChange(changeinfo,scheduledExecution.properties)
            redirect(controller: 'scheduledExecution', action: 'show', params: [id: scheduledExecution.extid])
        }
    }
    def _doupdate = { params, changeinfo=[:] ->
        log.debug("ScheduledExecutionController: update : attempting to update: "+params.id +
                 ". params: " + params)
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def user = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []
        /**
         * stores info about change for logging purposes
         */
        if(params.groupPath ){
            def re = /^\/*(.+?)\/*$/
            def matcher = params.groupPath =~ re
            if(matcher.matches()){
                params.groupPath=matcher.group(1);
                log.debug("params.groupPath updated: ${params.groupPath}")
            }else{
                log.debug("params.groupPath doesn't match: ${params.groupPath}")
            }
        }
        boolean failed=false
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_UPDATE], scheduledExecution.project)) {
            return [success:false,scheduledExecution:scheduledExecution,message:"Update Job ${scheduledExecution.extid}",unauthorized:true]
        }

        def crontab = [:]
        if(!scheduledExecution) {
            return [success:false]
        }
        def oldjobname = scheduledExecution.generateJobScheduledName()
        def oldjobgroup = scheduledExecution.generateJobGroupName()
        def oldsched = scheduledExecution.scheduled
        def optparams = params.findAll { it.key.startsWith("option.")}
        def nonopts = params.findAll { !it.key.startsWith("option.") && it.key!='workflow' && it.key!='options'&& it.key!='notifications'}
        if(scheduledExecution.uuid){
            nonopts.uuid=scheduledExecution.uuid//don't modify uuid if it exists
        }else if (!nonopts.uuid) {
            //set UUID if not submitted
            nonopts.uuid = UUID.randomUUID().toString()
        }
        if (nonopts.uuid != scheduledExecution.uuid) {
            changeinfo.extraInfo = " (internalID:${scheduledExecution.id})"
        }
        scheduledExecution.properties = nonopts

        //fix potential null/blank issue after upgrading rundeck to 1.3.1/1.4
        if (!scheduledExecution.description) {
            scheduledExecution.description = ''
        }

        final Map oldopts = params.findAll{it.key=~/^(name|command|type|adhocExecution|adhocFilepath|adhoc.*String)$/}
        if(oldopts && !params.workflow){
            //construct workflow with one item from these options
            oldopts.project=scheduledExecution.project
            if(optparams){
                def optsmap = ExecutionService.filterOptParams(optparams)
                if (optsmap) {
                    def optsmap2 = [:]
                    optsmap.each{k,v->
                        optsmap2[k]='${option.'+k+'}'
                    }
                    oldopts.argString = ExecutionService.generateArgline(optsmap2)
                }
            }
            if(oldopts.command && oldopts.type && !oldopts.adhocRemoteString){
                //convert old defined command to ctl dispatch
                if(oldopts.name){
                    oldopts.adhocRemoteString = "ctl -p ${oldopts.project} -t ${oldopts.type} -r ${oldopts.name} -c ${oldopts.command} -- ${oldopts.argString}"
                }else{
                    oldopts.adhocRemoteString = "ctl -p ${oldopts.project} -m ${oldopts.type} -c ${oldopts.command} -- ${oldopts.argString}"
                }
            }
            params.workflow=["commands[0]":oldopts]
            params.workflow.threadcount=1
            params.workflow.keepgoing=true
            params['_workflow_data']=true
        }
        //clear old mode job properties
        scheduledExecution.adhocExecution=false;
        scheduledExecution.adhocRemoteString=null
        scheduledExecution.adhocLocalString=null
        scheduledExecution.adhocFilepath=null

        if(!scheduledExecution.validate()){
            failed=true
        }
        if(scheduledExecution.scheduled){
            scheduledExecution.populateTimeDateFields(params)
            scheduledExecution.user = user
            scheduledExecution.userRoleList = request.subject.getPrincipals(Group.class).collect {it.name}.join(",")
            if(!CronExpression.isValidExpression(params.crontabString?params.crontabString:scheduledExecution.generateCrontabExression())){
                failed=true;
                scheduledExecution.errors.rejectValue('crontabString','scheduledExecution.crontabString.invalid.message')
            }else{
                //test for valid schedule
                CronExpression c = new CronExpression(params.crontabString?params.crontabString:scheduledExecution.generateCrontabExression())
                def next=c.getNextValidTimeAfter(new Date());
                if(!next){
                    failed=true;
                    scheduledExecution.errors.rejectValue('crontabString','scheduledExecution.crontabString.noschedule.message')
                }
            }
        }else{
            //set nextExecution of non-scheduled job to be far in the future so that query results can sort correctly
            scheduledExecution.nextExecution=new Date(ScheduledExecutionService.TWO_HUNDRED_YEARS)
        }

        def boolean renamed = oldjobname!=scheduledExecution.generateJobScheduledName() || oldjobgroup!=scheduledExecution.generateJobGroupName()
        if(renamed){
            changeinfo.rename=true
            changeinfo.origName=oldjobname
            changeinfo.origGroup=oldjobgroup
        }


        if(!frameworkService.existsFrameworkProject(scheduledExecution.project,framework)){
            failed=true
            scheduledExecution.errors.rejectValue('project','scheduledExecution.project.invalid.message',[scheduledExecution.project].toArray(),'Project was not found: {0}')
        }

        if(scheduledExecution.workflow && params['_sessionwf'] && session.editWF && session.editWF[scheduledExecution.id.toString()]){
            //load the session-stored modified workflow and replace the existing one
            def Workflow wf = session.editWF[scheduledExecution.id.toString()]
            if(!wf.commands || wf.commands.size()<1){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.empty.message')
            }else{
                def wfitemfailed=false
                def failedlist=[]
                def i=1;
                wf.commands.each{cexec->
                    WorkflowController._validateCommandExec(cexec)
                    if(cexec.errors.hasErrors()){
                        wfitemfailed=true
                       failedlist<<i
                    }
                    i++
                }
                if(!wfitemfailed){
                    def oldwf=scheduledExecution.workflow
                    final Workflow newworkflow = new Workflow(wf)
                    scheduledExecution.workflow=newworkflow
                    if(oldwf){
                            oldwf.delete()
                    }
                    wf.discard()
                }else{
                    failed=true
                    scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.invalidstepslist.message',[failedlist.toString()].toArray(),"Invalid workflow steps: {0}")
                }

            }
        }else if(params.workflow && params['_workflow_data']){
            //use the input params to define the workflow
            //create workflow and CommandExecs
            def Workflow workflow = new Workflow(threadcount:params.workflow.threadcount?params.workflow.threadcount:1,keepgoing:null!=params.workflow.keepgoing?params.workflow.keepgoing:false,scheduledExecution:scheduledExecution)
            def i=0;
            def wfitemfailed=false
            def failedlist=[]
            while(params.workflow["commands[${i}]"]){
                def Map cmdparams=params.workflow["commands[${i}]"]
                def cexec
                if(cmdparams.jobName){
                    cexec = new JobExec()
                }else{
                    cexec = new CommandExec()
                }
                if(!cmdparams.project){
                    cmdparams.project=scheduledExecution.project
                }
                cexec.properties=cmdparams
                workflow.addToCommands(cexec)
                WorkflowController._validateCommandExec(cexec)
                if(cexec.errors.hasErrors()){
                    wfitemfailed=true
                    failedlist<<(i+1)
                }
                i++
            }
            scheduledExecution.workflow=workflow

            if(wfitemfailed){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.invalidstepslist.message',[failedlist.toString()].toArray(),"Invalid workflow steps: {0}")
            }
            if(!workflow.commands || workflow.commands.size()<1){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.empty.message')
            }
        }else if(!scheduledExecution.workflow || !scheduledExecution.workflow.commands || scheduledExecution.workflow.commands.size()<1){
            failed=true
            scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.empty.message')
        }
        if((params.options || params['_nooptions']) && scheduledExecution.options){
            def todelete=[]
            scheduledExecution.options.each{
                todelete<<it
            }
            todelete.each{
                it.delete()
                scheduledExecution.removeFromOptions(it)
            }
            scheduledExecution.options=null
        }
        if( params['_sessionopts'] && session.editOPTS && null!=session.editOPTS[scheduledExecution.id.toString()]){
            def optsmap=session.editOPTS[scheduledExecution.id.toString()]

            def optfailed=false
            optsmap.values().each{Option opt->
                EditOptsController._validateOption(opt)
                if(opt.errors.hasErrors()){
                    optfailed=true
                }
            }
            if(!optfailed){
                def todelete=[]
                todelete.addAll(scheduledExecution.options)
                scheduledExecution.options=null
                todelete.each{oldopt->
                    oldopt.delete()
                }
                optsmap.values().each{Option opt->
                    opt.convertValuesList()
                    Option newopt = opt.createClone()
                    scheduledExecution.addToOptions(newopt)
                }
            }else{
                failed=true
                scheduledExecution.errors.rejectValue('options','scheduledExecution.options.invalid.message')
            }
        }else if (params.options){

            //set user options:
            def i=0;
            while(params.options["options[${i}]"]){
                def Map optdefparams=params.options["options[${i}]"]
                def Option theopt = new Option(optdefparams)
                scheduledExecution.addToOptions(theopt)
                EditOptsController._validateOption(theopt)
                if (theopt.errors.hasErrors() || !theopt.validate() ) {
                    failed = true
                    theopt.discard()
                    def errmsg = optdefparams.name + ": " + theopt.errors.allErrors.collect {g.message(error: it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                           'options',
                           'scheduledExecution.options.invalid.message',
                           [errmsg] as Object[],
                           'Invalid Option definition: {0}'
                     )
                }
                theopt.scheduledExecution=scheduledExecution
                i++
            }
            
        }

        if(!params.notifications && (params.notifyOnsuccess || params.notifyOnfailure || params.notifyOnsuccessUrl || params.notifyOnfailureUrl) ){
            def nots=[]
            if('true'==params.notifyOnsuccess){
                nots << [eventTrigger:'onsuccess',type:'email',content:params.notifySuccessRecipients]
            }
            if('true'==params.notifyOnsuccessUrl){
                nots << [eventTrigger: 'onsuccess', type: 'url', content: params.notifySuccessUrl]
            }
            if('true'==params.notifyOnfailure){
                nots << [eventTrigger: 'onfailure', type: 'email', content: params.notifyFailureRecipients]
            }
            if('true'==params.notifyOnfailureUrl){
                nots << [eventTrigger: 'onfailure', type: 'url', content: params.notifyFailureUrl]
            }
            params.notifications=nots
        }
        if(!params.notifications){
            params.notified='false'
        }
        def todiscard=[]
        if(scheduledExecution.notifications){
            def todelete=[]
            scheduledExecution.notifications.each{Notification note->
                todelete<<note
            }
            todelete.each{
                it.delete()
                scheduledExecution.removeFromNotifications(it)
                todiscard<<it
            }
            scheduledExecution.notifications=null
        }
        if(params.notifications && 'false'!=params.notified){
            //create notifications
            failed=_updateNotifications(params, scheduledExecution)
        }

        //try to save workflow
        if(!failed && null!=scheduledExecution.workflow){
            if(!scheduledExecution.workflow.validate()){
                log.error("unable to save workflow: "+scheduledExecution.workflow.errors.allErrors.collect{g.message(error:it)}.join("\n"))
                failed=true;
            }else{
                scheduledExecution.workflow.save(flush:true)
            }
        }
        if(!failed){
            if(!scheduledExecution.validate()){
                failed=true
            }
        }
        if(!failed && scheduledExecution.save(true)) {

            if(scheduledExecution.scheduled){
                def nextdate=null
                try{
                    nextdate=scheduledExecutionService.scheduleJob(scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null);
                }catch (SchedulerException e){
                    log.error("Unable to schedule job: ${scheduledExecution.extid}: ${e.message}")
                }
                def newsched = ScheduledExecution.get(scheduledExecution.id)
                newsched.nextExecution=nextdate
                if(!newsched.save()){
                    log.error("Unable to save second change to scheduledExec.")
                }
            }else if(oldsched && oldjobname && oldjobgroup){
                scheduledExecutionService.deleteJob(oldjobname,oldjobgroup)
            }
            log.debug("update : save operation succeeded. redirecting to show...")
            session.editOPTS?.remove(scheduledExecution.id.toString())
            session.undoOPTS?.remove(scheduledExecution.id.toString())
            session.redoOPTS?.remove(scheduledExecution.id.toString())

            session.editWF?.remove(scheduledExecution.id.toString())
            session.undoWF?.remove(scheduledExecution.id.toString())
            session.redoWF?.remove(scheduledExecution.id.toString())
            return [success:true,scheduledExecution:scheduledExecution]
        } else {
            todiscard.each{
                it.discard()
            }
            scheduledExecution.discard()
            return [success:false, scheduledExecution: scheduledExecution]
        }

    }
    public List _doupdateJob (id, ScheduledExecution params, changeinfo=[:]){
        log.debug("ScheduledExecutionController: update : attempting to update: "+id +
                 ". params: " + params)
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def user = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []

        if(params.groupPath ){
            def re = /^\/*(.+?)\/*$/
            def matcher = params.groupPath =~ re
            if(matcher.matches()){
                params.groupPath=matcher.group(1);
                log.debug("params.groupPath updated: ${params.groupPath}")
            }else{
                log.debug("params.groupPath doesn't match: ${params.groupPath}")
            }
        }
        boolean failed=false
        def ScheduledExecution scheduledExecution = ScheduledExecution.get( id )

        def crontab = [:]
        if(!scheduledExecution) {
            return [false,null]
        }
        def oldjobname = scheduledExecution.generateJobScheduledName()
        def oldjobgroup = scheduledExecution.generateJobGroupName()
        def oldsched = scheduledExecution.scheduled
        scheduledExecution.properties =null
        final Collection foundprops = params.properties.keySet().findAll {it != 'lastUpdated' && it != 'dateCreated' && (params.properties[it] instanceof String || params.properties[it] instanceof Boolean) }
        final Map newprops = foundprops ? params.properties.subMap(foundprops) : [:]
        if (scheduledExecution.uuid) {
            newprops.uuid = scheduledExecution.uuid//don't modify uuid if it exists
        } else if (!newprops.uuid) {
            //set UUID if not submitted
            newprops.uuid = UUID.randomUUID().toString()
        }
        if(newprops.uuid!=scheduledExecution.uuid){
            changeinfo.extraInfo = " (internalID:${scheduledExecution.id})"
        }
        //clear filter params
        scheduledExecution.clearFilterFields()
        scheduledExecution.properties = newprops

        //fix potential null/blank issue after upgrading rundeck to 1.3.1/1.4
        if (!scheduledExecution.description) {
            scheduledExecution.description = ''
        }


        //clear old mode job properties
        scheduledExecution.adhocExecution=false;
        scheduledExecution.adhocRemoteString=null
        scheduledExecution.adhocLocalString=null
        scheduledExecution.adhocFilepath=null

        if(!scheduledExecution.validate()){
            failed=true
        }
        if(scheduledExecution.scheduled){
            scheduledExecution.user = user
            scheduledExecution.userRoleList = request.subject.getPrincipals(Group.class).collect {it.name}.join(",")

            if(scheduledExecution.crontabString && (!CronExpression.isValidExpression(scheduledExecution.crontabString)
                                                    || !scheduledExecution.parseCrontabString(scheduledExecution.crontabString))){
                failed=true;
                scheduledExecution.errors.rejectValue('crontabString','scheduledExecution.crontabString.invalid.message')
            }
            if(!CronExpression.isValidExpression(scheduledExecution.generateCrontabExression())){
                failed=true;
                scheduledExecution.errors.rejectValue('crontabString','scheduledExecution.crontabString.invalid.message')
            }else{
                //test for valid schedule
                CronExpression c = new CronExpression(scheduledExecution.generateCrontabExression())
                def next=c.getNextValidTimeAfter(new Date());
                if(!next){
                    failed=true;
                    scheduledExecution.errors.rejectValue('crontabString','scheduledExecution.crontabString.noschedule.message')
                }
            }
        }else{
            //set nextExecution of non-scheduled job to be far in the future so that query results can sort correctly
            scheduledExecution.nextExecution=new Date(ScheduledExecutionService.TWO_HUNDRED_YEARS)
        }

        def boolean renamed = oldjobname!=scheduledExecution.generateJobScheduledName() || oldjobgroup!=scheduledExecution.generateJobGroupName()


        if(scheduledExecution.project && !frameworkService.existsFrameworkProject(scheduledExecution.project,framework)){
            failed=true
            scheduledExecution.errors.rejectValue('project','scheduledExecution.project.invalid.message',[scheduledExecution.project].toArray(),'Project was not found: {0}')
        }

        if(params.workflow ){
            //use the input params to define the workflow
            //create workflow and CommandExecs
            def Workflow workflow = new Workflow(params.workflow)
            def i=0;
            def wfitemfailed=false
            def failedlist=[]
            workflow.commands.each{CommandExec cmdparams->
                if(!cmdparams.project){
                    cmdparams.project=scheduledExecution.project
                }
                WorkflowController._validateCommandExec(cmdparams)
                if(cmdparams.errors.hasErrors()){
                    wfitemfailed=true
                    failedlist<<(i+1)
                }
                i++
            }
            scheduledExecution.workflow=workflow

            if(wfitemfailed){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.invalidstepslist.message',[failedlist.toString()].toArray(),"Invalid workflow steps: {0}")
            }
            if(!workflow.commands || workflow.commands.size()<1){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.empty.message')
            }
        }else if(!scheduledExecution.workflow || !scheduledExecution.workflow.commands || scheduledExecution.workflow.commands.size()<1){
            failed=true
            scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.empty.message')
        }
        if(scheduledExecution.options){
            def todelete=[]
            scheduledExecution.options.each{
                todelete<<it
            }
            todelete.each{
                it.delete()
                scheduledExecution.removeFromOptions(it)
            }
            scheduledExecution.options=null
        }
        if (params.options){

            //set user options:
            def i=0;
            params.options.each{Option theopt->
                scheduledExecution.addToOptions(theopt)
                EditOptsController._validateOption(theopt)
                if (theopt.errors.hasErrors()|| !theopt.validate()) {
                    failed = true
                    theopt.discard()
                    def errmsg = theopt.name + ": " + theopt.errors.allErrors.collect {g.message(error: it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                           'options',
                           'scheduledExecution.options.invalid.message',
                           [errmsg] as Object[],
                           'Invalid Option definition: {0}'
                     )
                }
                theopt.scheduledExecution=scheduledExecution
                i++
            }

        }

        def todiscard=[]
        if(scheduledExecution.notifications){
            def todelete=[]
            scheduledExecution.notifications.each{Notification note->
                todelete<<note
            }
            todelete.each{
                it.delete()
                scheduledExecution.removeFromNotifications(it)
                todiscard<<it
            }
            scheduledExecution.notifications=null
        }
        if(params.notifications){
            //create notifications
            failed=_updateNotifications(params, scheduledExecution)
        }

        //try to save workflow
        if(!failed && null!=scheduledExecution.workflow){
            if(!scheduledExecution.workflow.validate()){
                log.error("unable to save workflow: "+scheduledExecution.workflow.errors.allErrors.collect{g.message(error:it)}.join("\n"))
                failed=true;
            }else{
                scheduledExecution.workflow.save(flush:true)
            }
        }
        if(!failed){
            if(!scheduledExecution.validate()){
                failed=true
            }
        }
        if(!failed && scheduledExecution.save(true)) {

            if(scheduledExecution.scheduled){
                def nextdate=null
                try{
                    nextdate=scheduledExecutionService.scheduleJob(scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null);
                }catch (SchedulerException e){
                    log.error("Unable to schedule job: ${scheduledExecution.extid}: ${e.message}")
                }
                def newsched = ScheduledExecution.get(scheduledExecution.id)
                newsched.nextExecution=nextdate
                if(!newsched.save()){
                    log.error("Unable to save second change to scheduledExec.")
                }
            }else if(oldsched && oldjobname && oldjobgroup){
                scheduledExecutionService.deleteJob(oldjobname,oldjobgroup)
            }
            log.debug("update : save operation succeeded. redirecting to show...")
            session.editOPTS?.remove(scheduledExecution.id.toString())
            session.undoOPTS?.remove(scheduledExecution.id.toString())
            session.redoOPTS?.remove(scheduledExecution.id.toString())

            session.editWF?.remove(scheduledExecution.id.toString())
            session.undoWF?.remove(scheduledExecution.id.toString())
            session.redoWF?.remove(scheduledExecution.id.toString())
            return [true,scheduledExecution]
        } else {
            todiscard.each{
                it.discard()
            }
            scheduledExecution.discard()
            return [false, scheduledExecution]
        }

    }

    def copy = {
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        //authorize
        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'job'], [AuthConstants.ACTION_CREATE], session.project)) {
            return unauthorized("Create a Job")
        }
        def user = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []
        log.debug("ScheduledExecutionController: create : params: " + params)

        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if(!scheduledExecution){
            flash.message = "ScheduledExecution not found with id ${params.id}"
            log.debug("update: there was no object by id: " +params.id+". redirecting to menu.")
            redirect(action:index)
            return;
        }
        if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_READ], scheduledExecution.project)) {
            return unauthorized("Read Job ${params.id}")
        }
        def newScheduledExecution = new ScheduledExecution()
        newScheduledExecution.properties = new java.util.HashMap(scheduledExecution.properties)
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
        render(view:'create',model: [ scheduledExecution:newScheduledExecution, crontab:crontab,params:params, iscopy:true, authorized:scheduledExecutionService.userAuthorizedForJob(request,scheduledExecution,framework), projects: frameworkService.projects(framework)])

    }
    /**
     * action to populate the Create form with execution info from a previous (transient) execution
     */
    def createFromExecution={

        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'job'], [AuthConstants.ACTION_CREATE],session.project) && !frameworkService.authorizeProjectResource(framework,[type:'adhoc'], AuthConstants.ACTION_RUN,session.project)) {
            return unauthorized("Create a Job")
        }
        log.debug("ScheduledExecutionController: create : params: " + params)
        Execution execution = Execution.get(params.executionId)
        if(!execution){
            flash.message = "Execution not found with id ${params.executionId}"
            redirect(controller:'execution',action:'follow',id:params.executionId)
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
    private  unauthorized(String action, boolean fragment=false){
        if(!fragment){
            response.setStatus(403)
        }
        flash.title = "Unauthorized"
        flash.error = "${request.remoteUser} is not authorized to: ${action}"
        response.setHeader(Constants.X_RUNDECK_ACTION_UNAUTHORIZED_HEADER, flash.error)
        render(template: fragment?'/common/errorFragment':'/common/error', model: [:])
    }
    def create = {

        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        //authorize
        if(!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'job'], [AuthConstants.ACTION_CREATE], session.project) && !frameworkService.authorizeProjectResource(framework, [type: 'adhoc'], AuthConstants.ACTION_RUN, session.project)){
            return unauthorized("Create a Job")
        }

        def projects = frameworkService.projects(framework)
        def user = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []
        log.debug("ScheduledExecutionController: create : params: " + params)
        def scheduledExecution = new ScheduledExecution()
        scheduledExecution.loglevel = servletContext.getAttribute("LOGLEVEL_DEFAULT")?servletContext.getAttribute("LOGLEVEL_DEFAULT"):"WARN"
        scheduledExecution.properties = params

        scheduledExecution.jobName = (params.command) ? params.command + " Job" : ""
        def cal = java.util.Calendar.getInstance()
        scheduledExecution.minute = String.valueOf(cal.get(java.util.Calendar.MINUTE))
        scheduledExecution.hour = String.valueOf(cal.get(java.util.Calendar.HOUR_OF_DAY))
        scheduledExecution.user = user
        scheduledExecution.userRoleList = request.subject.getPrincipals(Group.class).collect {it.name}.join(",")
        if(params.project ){

            if(!frameworkService.existsFrameworkProject(params.project,framework) ) {
                scheduledExecution.errors.rejectValue('project','scheduledExecution.project.message',[params.project].toArray(),'FrameworkProject was not found: {0}')
            }
            scheduledExecution.argString=params.argString
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

        log.debug("ScheduledExecutionController: create : now returning model data to view...")
        return ['scheduledExecution':scheduledExecution,params:params,crontab:[:],projects:projects]
    }


    def saveAndExec = {
        log.debug("ScheduledExecutionController: saveAndExec : params: " + params)
        def changeinfo = [user: session.user, change: 'create', method: 'saveAndExec']
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'job'], [AuthConstants.ACTION_CREATE], session.project)) {
            unauthorized("Create a Job")
            return [success: false]
        }
        def result = _dosave(params,changeinfo)
        def scheduledExecution = result.scheduledExecution
        if(result.success && scheduledExecution && scheduledExecution.id){
            params.id=scheduledExecution.extid
            logJobChange(changeinfo, scheduledExecution.properties)
            if(!scheduledExecution.scheduled){
                return redirect(action:execute,id:scheduledExecution.extid)
            }else{
                return redirect(action:show,id:scheduledExecution.extid)
            }
        }else{

            if(scheduledExecution){
                scheduledExecution.errors.allErrors.each { log.warn(it.defaultMessage) }
            }
            flash.message=g.message(code:'ScheduledExecutionController.save.failed')
            return render(view:'create',model:[scheduledExecution:scheduledExecution,params:params, projects: frameworkService.projects(framework)])
        }
    }
    /**
     * Action to upload jobs.xml and execute it immediately.
     */
    def uploadAndExecute = {
        log.debug("ScheduledExecutionController: upload " + params)
        def fileformat = params.fileformat ?: 'xml'
        def parseresult
        if (request instanceof MultipartHttpServletRequest) {
            def file = request.getFile("xmlBatch")
            if (!file || file.empty) {
                flash.message = "No file was uploaded."
                return
            }
            parseresult = parseUploadedFile(file.getInputStream(), fileformat)
        } else if (params.xmlBatch) {
            String fileContent = params.xmlBatch
            parseresult = parseUploadedFile(fileContent, fileformat)
        } else {
            return
        }
        def jobset

        if (parseresult.error) {
            flash.error = parseresult.error
            if (params.xmlreq) {
                return xmlerror()
            } else {
                render(view: 'upload')
                return
            }
        }
        jobset = parseresult.jobset
        def changeinfo = [user: session.user, method: 'upload']
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def loadresults = loadJobs(jobset, params.dupeOption, changeinfo, framework)

        def jobs = loadresults.jobs
        def jobsi = loadresults.jobsi
        def msgs = loadresults.msgs
        def errjobs = loadresults.errjobs
        def skipjobs = loadresults.skipjobs

        def reserrors = []
        def ressuccess=[]

        if(!errjobs){
            //run the jobs and forward to nowrunning
            jobsi.each{ Map map->
                def ScheduledExecution scheduledExecution=map.scheduledExecution
                def entrynum=map.entrynum
                def properties=[:]
                properties.putAll(scheduledExecution.properties)
                properties.user=params.user
                properties.request = request
                def execresults = executionService.executeScheduledExecution(scheduledExecution,framework,request.subject,[user:request.remoteUser])
    //            System.err.println("transient execute result: ${execresults}");
                execresults.entrynum=entrynum
                if(execresults.error || !execresults.success){
                    reserrors<<execresults
                } else {
                    ressuccess<<execresults
                }
            }
        }

        if (!params.xmlreq) {

            render(view: 'upload',model:[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, execerrors:reserrors,execsuccess:ressuccess,
                nextExecutions: scheduledExecutionService.nextExecutionTimes(jobs.grep { it.scheduled }),
                messages: msgs,
                didupload: true])
        } else {
            //TODO: update commander's jobs upload task to submit XML content directly instead of via uploaded file, and use proper
            //TODO: grails content negotiation
            response.setHeader(Constants.X_RUNDECK_RESULT_HEADER, "Jobs Uploaded. Succeeded: ${jobs.size()}, Failed: ${errjobs.size()}, Skipped: ${skipjobs.size()}")
            render(contentType: "text/xml") {
                result(error: false) {
                    renderJobsImportApiXML(jobs, jobsi, errjobs, skipjobs, delegate)
                }
            }
        }

    }
    
    /**
     * execute the job defined via input parameters, but do not store it.
     */
    def runAdhocInline = {
        def results=runAdhoc()
        if(results.failed){
            results.error=results.message
        } else {
            log.debug("ExecutionController: immediate execution scheduled (${results.id})")
        }
        return render(contentType:'application/json'){
            if(results.error){
                delegate.'error'(results.error)
            }else{
                success(true)
                id(results.id)
            }
        }
    }
    def runAdhoc={
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        params["user"] = (session?.user) ? session.user : "anonymous"
        params.request = request
        params.jobName='Temporary_Job'
        params.groupPath='adhoc'
        def result= _dovalidate(params)
        def ScheduledExecution scheduledExecution=result.scheduledExecution
        def failed=result.failed
        if(!failed){
            return _transientExecute(scheduledExecution,params,framework,request.subject)
        }else{
            return [failed:true,message:'Job configuration was incorrect.',scheduledExecution:scheduledExecution,params:params]
        }
    }
    /**
     * execute the job defined via input parameters, but do not store it.
     */
    def execAndForget = {
        def results = runAdhoc()
        if(results.error=='unauthorized'){
            log.error(results.message)
            flash.error=results.message
            render(view:"/common/execUnauthorized",model:[scheduledExecution:results.scheduledExecution])
            return
        }else if(results.error){
            log.error(results.message)
            flash.error=results.message
            render(view:"/common/error",model:[error:results.message])
            return
        }else if(results.failed){
            def scheduledExecution=results.scheduledExecution
            scheduledExecution.jobName = ''
            scheduledExecution.errors.allErrors.each { log.warn(it.defaultMessage) }
            flash.message=results.message
            Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
            render(view:'create',model:[scheduledExecution:scheduledExecution,params:params, projects: frameworkService.projects(framework)])
        } else {
            log.debug("ExecutionController: immediate execution scheduled (${results.id})")
            redirect(controller:"execution", action:"follow",id:results.id)
        }
    }
    
    /**
    * Execute a transient ScheduledExecution and return execution data: [execution:Execution,id:Long]
     * if there is an error, return [error:'type',message:errormesg,...]
     */
    Map _transientExecute(ScheduledExecution scheduledExecution, Map params, Framework framework, Subject subject){
        def object
        def isauth = scheduledExecutionService.userAuthorizedForAdhoc(params.request,scheduledExecution,framework)
        if (!isauth){
            def msg=g.message(code:'unauthorized.job.run.user',args:[params.user])
            return [error:'unauthorized',message:msg]
        }
        params.workflow=new Workflow(scheduledExecution.workflow)
        params.argString=scheduledExecution.argString

        def Execution e
        try {
            e = executionService.createExecutionAndPrep(params, framework, params.user)
        } catch (ExecutionServiceException exc) {
            return [error:'failed',message:exc.getMessage()]
        }

        def eid = scheduledExecutionService.scheduleTempJob(params.user, subject,params,e);
        return [execution:e,id:eid]
    }

    def _dovalidate = { Map params ->
        log.debug("ScheduledExecutionController: save : params: " + params)
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def user = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []
        boolean failed=false;
        def scheduledExecution = new ScheduledExecution()
        def optparams = params.findAll {it.key.startsWith("option.")}
        final Map nonopts = params.findAll {!it.key.startsWith("option.") && it.key != 'workflow'&& it.key != 'options'&& it.key != 'notifications'}
        final Map oldopts = params.findAll{it.key=~/^(name|command|type|adhocExecution|adhocFilepath|adhoc.*String)$/}
        scheduledExecution.properties = nonopts

        //fix potential null/blank issue after upgrading rundeck to 1.3.1/1.4
        if(!scheduledExecution.description){
            scheduledExecution.description=''
        }

        if(oldopts && !params.workflow){
            //construct workflow with one item from these options
            oldopts.project=scheduledExecution.project
            if(optparams){
                def optsmap = ExecutionService.filterOptParams(optparams)
                if (optsmap) {
                    def optsmap2 = [:]
                    optsmap.each{k,v->
                        optsmap2[k]='${option.'+k+'}'
                    }
                    oldopts.argString = ExecutionService.generateArgline(optsmap2)
                }
            }
            if(oldopts.command && oldopts.type && !oldopts.adhocRemoteString){
                //convert old defined command to ctl dispatch
                if(oldopts.name){
                    oldopts.adhocRemoteString = "ctl -p ${oldopts.project} -t ${oldopts.type} -r ${oldopts.name} -c ${oldopts.command} -- ${oldopts.argString}"
                }else{
                    oldopts.adhocRemoteString = "ctl -p ${oldopts.project} -m ${oldopts.type} -c ${oldopts.command} -- ${oldopts.argString}"
                }
            }
            params.workflow=["commands[0]":oldopts]
            params.workflow.threadcount=1
            params.workflow.keepgoing=true
        }
        //clear old mode job properties
        scheduledExecution.adhocExecution=false;
        scheduledExecution.adhocRemoteString=null
        scheduledExecution.adhocLocalString=null
        scheduledExecution.adhocFilepath=null

        def valid= scheduledExecution.validate()
        if(scheduledExecution.scheduled){
            scheduledExecution.user = user
            scheduledExecution.userRoleList = request.subject.getPrincipals(Group.class).collect{it.name}.join(",")

            scheduledExecution.populateTimeDateFields(params)

            if(!CronExpression.isValidExpression(params.crontabString?params.crontabString:scheduledExecution.generateCrontabExression())){
                failed=true;
                scheduledExecution.errors.rejectValue('crontabString','scheduledExecution.crontabString.invalid.message')
            }else{
                //test for valid schedule
                CronExpression c = new CronExpression(params.crontabString?params.crontabString:scheduledExecution.generateCrontabExression())
                def next=c.getNextValidTimeAfter(new Date());
                if(!next){
                    failed=true;
                    scheduledExecution.errors.rejectValue('crontabString','scheduledExecution.crontabString.noschedule.message')
                }
            }
        }else{
            //set nextExecution of non-scheduled job to be far in the future so that query results can sort correctly
            scheduledExecution.nextExecution=new Date(ScheduledExecutionService.TWO_HUNDRED_YEARS)
        }

        if(scheduledExecution.project && !frameworkService.existsFrameworkProject(scheduledExecution.project,framework)){
            failed=true
            scheduledExecution.errors.rejectValue('project','scheduledExecution.project.invalid.message',[scheduledExecution.project].toArray(),'Project does not exist: {0}')
        }
        if(params['_sessionwf']=='true' && session.editWF && session.editWF['_new']){
            //use session-stored workflow
            def Workflow wf = session.editWF['_new']
            wf.keepgoing=params.workflow.keepgoing=='true'
            wf.strategy=params.workflow.strategy
            if(!wf.commands || wf.commands.size()<1){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.empty.message')
            }else{

                def wfitemfailed=false
                def i=1
                def failedlist=[]
                wf.commands.each{cexec->
                    WorkflowController._validateCommandExec(cexec)
                    if(cexec.errors.hasErrors()){
                        wfitemfailed=true
                        failedlist<<i
                    }
                    i++
                }
                if(!wfitemfailed){
                    final Workflow workflow = new Workflow(wf)
                    scheduledExecution.workflow=workflow
                    wf.discard()
                }else{
                    failed=true
                    scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.invalidstepslist.message',[failedlist.toString()].toArray(),"Invalid workflow steps: {0}")
                }
            }
        }else if (params.workflow && params.workflow instanceof Workflow){
            def Workflow workflow = new Workflow(params.workflow)
            def i=0;
            def wfitemfailed=false
            def failedlist=[]
            workflow.commands.each{CommandExec cmdparams ->
                if(!cmdparams.project){
                    cmdparams.project=scheduledExecution.project
                }
                WorkflowController._validateCommandExec(cmdparams)
                if(cmdparams.errors.hasErrors()){
                    wfitemfailed=true
                    failedlist<<(i+1)
                }
                i++
            }
            scheduledExecution.workflow=workflow

            if(wfitemfailed){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.invalidstepslist.message',[failedlist.toString()].toArray(),"Invalid workflow steps: {0}")
            }
            if(!workflow.commands || workflow.commands.size()<1){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.empty.message')
            }
        }else if (params.workflow){
            //use input parameters to define workflow
            //create workflow and CommandExecs
            def Workflow workflow = new Workflow(threadcount:params.workflow.threadcount,keepgoing:params.workflow.keepgoing,scheduledExecution:scheduledExecution)
            def i=0;
            def wfitemfailed=false
            def failedlist=[]
            while(params.workflow["commands[${i}]"]){
                def Map cmdparams=params.workflow["commands[${i}]"]
                def cexec
                if(cmdparams.jobName){
                    cexec = new JobExec()
                }else{
                    cexec = new CommandExec()
                }

                if(!cmdparams.project){
                    cmdparams.project=scheduledExecution.project
                }
                cexec.properties=cmdparams
                workflow.addToCommands(cexec)
                WorkflowController._validateCommandExec(cexec)
                if(cexec.errors.hasErrors()){
                    wfitemfailed=true
                    failedlist<<(i+1)
                }
                i++
            }
            scheduledExecution.workflow=workflow

            if(wfitemfailed){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.invalidstepslist.message',[failedlist.toString()].toArray(),"Invalid workflow steps: {0}")
            }
            if(!workflow.commands || workflow.commands.size()<1){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.empty.message')
            }
        }else if (!scheduledExecution.workflow || !scheduledExecution.workflow.commands || scheduledExecution.workflow.commands.size()<1){
            failed=true
            scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.empty.message')
        }

        if(scheduledExecution.argString){
            try{
                scheduledExecution.argString.replaceAll(/\$\{DATE:(.*)\}/,{ all,tstamp ->
                    new SimpleDateFormat(tstamp).format(new Date())
                })
            }catch(IllegalArgumentException e){
                failed=true;
                scheduledExecution.errors.rejectValue('argString','scheduledExecution.argString.datestamp.invalid',[e.getMessage()].toArray(),'datestamp format is invalid: {0}')
                log.error(e)
            }
        }

        if( params['_sessionopts'] && session.editOPTS && session.editOPTS['_new']){
            def optsmap=session.editOPTS['_new']

            def optfailed=false
            optsmap.values().each{Option opt->
                EditOptsController._validateOption(opt)
                if(opt.errors.hasErrors()){
                    optfailed=true
                }
            }
            if(!optfailed){
                optsmap.values().each{Option opt->
                    opt.convertValuesList()
                    Option newopt = opt.createClone()
                    scheduledExecution.addToOptions(newopt)
                }
            }else{
                failed=true
                scheduledExecution.errors.rejectValue('options','scheduledExecution.options.invalid.message')
            }
        }else if (params.options){
            //set user options:
            def i=0;
            if(params.options instanceof Collection ){
                params.options.each{ origopt->
                    def Option theopt = origopt.createClone()
                    scheduledExecution.addToOptions(theopt)
                    EditOptsController._validateOption(theopt)

                    if (theopt.errors.hasErrors() || !theopt.validate()) {
                        failed = true
                        theopt.discard()
                        def errmsg = theopt.name + ": " + theopt.errors.allErrors.collect {g.message(error: it)}.join(";")
                        scheduledExecution.errors.rejectValue(
                               'options',
                               'scheduledExecution.options.invalid.message',
                               [errmsg] as Object[],
                               'Invalid Option definition: {0}'
                         )
                    }
                    i++
                }
            }else if (params.options instanceof Map){
                while(params.options["options[${i}]"]){
                    def Map optdefparams=params.options["options[${i}]"]
                    def Option theopt = new Option(optdefparams)
                    scheduledExecution.addToOptions(theopt)
                    EditOptsController._validateOption(theopt)
                    if (theopt.errors.hasErrors() || !theopt.validate()) {
                        failed = true
                        theopt.discard()
                        def errmsg = optdefparams.name + ": " + theopt.errors.allErrors.collect {g.message(error: it)}.join(";")
                        scheduledExecution.errors.rejectValue(
                               'options',
                               'scheduledExecution.options.invalid.message',
                               [errmsg] as Object[],
                               'Invalid Option definition: {0}'
                         )
                    }
                    theopt.scheduledExecution=scheduledExecution
                    i++
                }
            }
        }
        if(!params.notifications && (params.notifyOnsuccess || params.notifyOnfailure || params.notifyOnsuccessUrl || params.notifyOnfailureUrl)){
            def nots=[]
            if('true'==params.notifyOnsuccess){
                nots<<[eventTrigger:'onsuccess',type:'email',content:params.notifySuccessRecipients]
            }
            if('true'==params.notifyOnsuccessUrl){
                nots << [eventTrigger: 'onsuccess', type: 'url', content:params.notifySuccessUrl]
            }
            if('true'==params.notifyOnfailure){
                nots << [eventTrigger: 'onfailure', type: 'email', content:params.notifyFailureRecipients]
            }
            if('true'==params.notifyOnfailureUrl){
                nots << [eventTrigger: 'onfailure', type: 'url', content:params.notifyFailureUrl]
            }
            params.notifications=nots
        }
        if(params.notifications){
            //create notifications
            failed=_updateNotifications(params, scheduledExecution)
        }
        if(scheduledExecution.doNodedispatch){
            if(!scheduledExecution.nodeInclude
                && !scheduledExecution.nodeExclude
             && !scheduledExecution.nodeIncludeName
                && !scheduledExecution.nodeExcludeName
             && !scheduledExecution.nodeIncludeTags
                && !scheduledExecution.nodeExcludeTags
             && !scheduledExecution.nodeIncludeOsName
                && !scheduledExecution.nodeExcludeOsName
             && !scheduledExecution.nodeIncludeOsFamily
                && !scheduledExecution.nodeExcludeOsFamily
             && !scheduledExecution.nodeIncludeOsArch
                && !scheduledExecution.nodeExcludeOsArch
             && !scheduledExecution.nodeIncludeOsVersion
                && !scheduledExecution.nodeExcludeOsVersion){
                scheduledExecution.errors.rejectValue('nodeInclude','scheduledExecution.nodeIncludeExclude.blank.message')
                scheduledExecution.errors.rejectValue('nodeExclude','scheduledExecution.nodeIncludeExclude.blank.message')
                failed=true
            }
        }
        failed= failed || !valid
        return [failed:failed,scheduledExecution:scheduledExecution]
    }
    
    /**
     * Update ScheduledExecution notification definitions based on input params.
     *
     * expected params: [notifications: [<eventTrigger>:[email:<content>]]]
     */
    private boolean _updateNotifications(Map params,ScheduledExecution scheduledExecution) {
        boolean failed=false
        def fieldNames=[onsuccess:'notifySuccessRecipients',onfailure:'notifyFailureRecipients']
        def fieldNamesUrl=[onsuccess:'notifySuccessUrl',onfailure:'notifyFailureUrl']
        params.notifications.each {notif ->
            def trigger=notif.eventTrigger
            if (notif && notif.type=='email' && notif.content) {
                def arr=notif.content.split(",")
                arr.each{email->
                    if(email && !org.apache.commons.validator.EmailValidator.getInstance().isValid(email)){
                        failed=true
                         scheduledExecution.errors.rejectValue(
                            fieldNames[trigger],
                            'scheduledExecution.notifications.invalidemail.message',
                            [email] as Object[],
                            'Invalid email address: {0}'
                        )
                    }
                }
                if(failed){
                    return
                }
                def addrs = arr.findAll{it.trim()}.join(",")
                Notification n = new Notification(eventTrigger: trigger, type: 'email', content: addrs)
                scheduledExecution.addToNotifications(n)
                if (!n.validate()) {
                    failed = true
                    n.discard()
                    def errmsg = trigger + " notification: " + n.errors.allErrors.collect {g.message(error: it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                        fieldNames[trigger],
                        'scheduledExecution.notifications.invalid.message',
                        [errmsg] as Object[],
                        'Invalid notification definition: {0}'
                    )
                }
                n.scheduledExecution = scheduledExecution
            }else if (notif && notif.type=='url' && notif.content) {
                def arr=notif.content.split(",")

                arr.each{String url ->
                    boolean valid=false
                    try{
                        new URL(url)
                        valid=true
                    }catch(MalformedURLException e){
                        valid=false
                    }
                    if(url && !valid){
                        failed=true
                         scheduledExecution.errors.rejectValue(
                             fieldNamesUrl[trigger],
                            'scheduledExecution.notifications.invalidurl.message',
                            [url] as Object[],
                            'Invalid URL: {0}'
                        )
                    }
                }
                if(failed){
                    return
                }
                def addrs = arr.findAll{it.trim()}.join(",")
                Notification n = new Notification(eventTrigger: trigger, type: 'url', content: addrs)
                scheduledExecution.addToNotifications(n)
                if (!n.validate()) {
                    failed = true
                    n.discard()
                    def errmsg = trigger + " notification: " + n.errors.allErrors.collect {g.message(error: it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                        fieldNamesUrl[trigger],
                        'scheduledExecution.notifications.invalid.message',
                        [errmsg] as Object[],
                        'Invalid notification definition: {0}'
                    )
                }
                n.scheduledExecution = scheduledExecution
            }
        }
        return failed
    }

    /**
     * Update ScheduledExecution notification definitions based on input params.
     *
     * expected params: [notifications: [<eventTrigger>:[email:<content>]]]
     */
    private boolean _updateNotifications(ScheduledExecution params,ScheduledExecution scheduledExecution) {
        boolean failed=false
        def fieldNames=[onsuccess:'notifySuccessRecipients',onfailure:'notifyFailureRecipients']
        def fieldNamesUrl=[onsuccess:'notifySuccessUrl',onfailure:'notifyFailureUrl']
        params.notifications.each {notif ->
            def trigger = notif.eventTrigger
            if (notif && notif.type=='email' && notif.content) {
                def arr=notif.content.split(",")
                arr.each{email->
                    if(email && !org.apache.commons.validator.EmailValidator.getInstance().isValid(email)){
                        failed=true
                         scheduledExecution.errors.rejectValue(
                            fieldNames[trigger],
                            'scheduledExecution.notifications.invalidemail.message',
                            [email] as Object[],
                            'Invalid email address: {0}'
                        )
                    }
                }
                if(failed){
                    return
                }
                def addrs = arr.findAll{it.trim()}.join(",")
                Notification n = new Notification(eventTrigger: trigger, type: 'email', content: addrs)
                scheduledExecution.addToNotifications(n)
                if (!n.validate()) {
                    failed = true
                    n.discard()
                    def errmsg = trigger + " notification: " + n.errors.allErrors.collect {g.message(error: it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                        fieldNames[trigger],
                        'scheduledExecution.notifications.invalid.message',
                        [errmsg] as Object[],
                        'Invalid notification definition: {0}'
                    )
                }
                n.scheduledExecution = scheduledExecution
            }else if (notif && notif.type=='url' && notif.content) {
                def arr=notif.content.split(",")
                arr.each{String url ->
                    boolean valid = false
                    try {
                        new URL(url)
                        valid = true
                    } catch (MalformedURLException e) {
                        valid = false
                    }
                    if (url && !valid) {
                        failed=true
                         scheduledExecution.errors.rejectValue(
                             fieldNamesUrl[trigger],
                            'scheduledExecution.notifications.invalidurl.message',
                            [url] as Object[],
                            'Invalid URL: {0}'
                        )
                    }
                }
                if(failed){
                    return
                }
                def addrs = arr.findAll{it.trim()}.join(",")
                Notification n = new Notification(eventTrigger: trigger, type: 'url', content: addrs)
                scheduledExecution.addToNotifications(n)
                if (!n.validate()) {
                    failed = true
                    n.discard()
                    def errmsg = trigger + " notification: " + n.errors.allErrors.collect {g.message(error: it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                        fieldNamesUrl[trigger],
                        'scheduledExecution.notifications.invalid.message',
                        [errmsg] as Object[],
                        'Invalid notification definition: {0}'
                    )
                }
                n.scheduledExecution = scheduledExecution
            }
        }
        return failed
    }

    /**
     * Update ScheduledExecution notification definitions based on input params.
     *
     * expected params: [notifications: [<eventTrigger>:[email:<content>]]]
     */
    private boolean _validateNotifications(Map params,ScheduledExecution scheduledExecution) {
        boolean failed=false
        def fieldNames=[onsuccess:'notifySuccessRecipients',onfailure:'notifyFailureRecipients']
        ['onsuccess', 'onfailure'].each {trigger ->
            def notif = params.notifications[trigger]
            if (notif && notif.email) {
                def arr=notif.email.split(",")
                arr.each{email->
                    if(email && !org.apache.commons.validator.EmailValidator.getInstance().isValid(email)){
                        failed=true
                         scheduledExecution.errors.rejectValue(
                            fieldNames[trigger],
                            'scheduledExecution.notifications.invalidemail.message',
                            [email] as Object[],
                            'Invalid email address: {0}'
                        )
                    }
                }
                if(failed){
                    return
                }
                def addrs = arr.findAll{it.trim()}.join(",")
                Notification n = new Notification(eventTrigger: trigger, type: 'email', content: addrs)
                if (!n.validate()) {
                    failed = true
                    def errmsg = trigger + " notification: " + n.errors.allErrors.collect {g.message(error: it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                        fieldNames[trigger],
                        'scheduledExecution.notifications.invalid.message',
                        [errmsg] as Object[],
                        'Invalid notification definition: {0}'
                    )
                }
                n.discard()
            }
        }
        return failed
    }

    /**
    */
    public Map _dosave ( params, changeinfo=[:]){
        log.debug("ScheduledExecutionController: save : params: " + params)
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def user = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []
        boolean failed=false;
        if(params.groupPath ){
            def re = /^\/*(.+?)\/*$/
            def matcher = params.groupPath =~ re
            if(matcher.matches()){
                params.groupPath=matcher.group(1);
                log.debug("params.groupPath updated: ${params.groupPath}")
            }else{
                log.debug("params.groupPath doesn't match: ${params.groupPath}")
            }
        }
        if(!params.jobName){
            //TODO: finalize format
            if(params.adhocRemoteString){
                params.jobName="Remote Script Job"
            }else if(params.adhocLocalString){
                params.jobName="Inline Script Job"
            }
        }

        def result= _dovalidate(params instanceof ScheduledExecution?params.properties:params)
        def scheduledExecution=result.scheduledExecution
        failed=result.failed
        //try to save workflow

        if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_CREATE], scheduledExecution.project)) {
            scheduledExecution.discard()
            return [success: false, error: "Unauthorized: Create Job ${scheduledExecution.generateFullName()}", unauthorized: true, scheduledExecution:scheduledExecution]
        }
        if(!failed && null!=scheduledExecution.workflow){
            if(!scheduledExecution.workflow.save(flush:true)){
                log.error(scheduledExecution.workflow.errors.allErrors.collect{g.message(error:it)}.join("\n"))
                failed=true;
            }
        }

        //set UUID if not submitted
        if(!scheduledExecution.uuid){
            scheduledExecution.uuid=UUID.randomUUID().toString()
        }
        if(!failed && scheduledExecution.save(true)){
            if(scheduledExecution.scheduled){
                def nextdate=null
                try{
                    nextdate=scheduledExecutionService.scheduleJob(scheduledExecution,null,null);
                }catch (SchedulerException e){
                    log.error("Unable to schedule job: ${scheduledExecution.extid}: ${e.message}")
                }
                def newsched = ScheduledExecution.get(scheduledExecution.id)
                newsched.nextExecution=nextdate
                if(!newsched.save()){
                    log.error("Unable to save second change to scheduledExec.")
                }
            }
            session.editOPTS?.remove('_new')
            session.undoOPTS?.remove('_new')
            session.redoOPTS?.remove('_new')

            session.editWF?.remove('_new')
            session.undoWF?.remove('_new')
            session.redoWF?.remove('_new')
            return [success:true,scheduledExecution: scheduledExecution]

        } else {
            scheduledExecution.discard()
            return [success:false,scheduledExecution: scheduledExecution]
        }
    }

    def save = {
        def changeinfo=[user:session.user,change:'create',method:'save']
        def result = _dosave(params,changeinfo)
        def scheduledExecution = result.scheduledExecution
        if(result.success && scheduledExecution.id){
            flash.savedJob=scheduledExecution
            flash.savedJobMessage="Created new Job"
            logJobChange(changeinfo,scheduledExecution.properties)
            redirect(controller:'scheduledExecution',action:'show',params:[id:scheduledExecution.extid])
        }else if(result.unauthorized){
            if(scheduledExecution){
                scheduledExecution.errors.allErrors.each { log.warn(it.defaultMessage) }
            }
            request.message=result.error
            Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
            render(view:'create',model:[scheduledExecution:scheduledExecution,params:params, projects: frameworkService.projects(framework)])
        }else{
            if(scheduledExecution){
                scheduledExecution.errors.allErrors.each { log.warn(it.defaultMessage) }
            }
            request.message=g.message(code:'ScheduledExecutionController.save.failed')
            Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
            render(view:'create',model:[scheduledExecution:scheduledExecution,params:params, projects: frameworkService.projects(framework)])
        }
    }
    /**
     * Parse some kind of job input request using the specified format
     * @param input either an inputStream, a File, or a String
     */
    private def parseUploadedFile={  input, fileformat->
        def jobset
        if('xml'==fileformat){
            try{
                jobset= input.decodeJobsXML()
            }catch(Exception e){
                return [error:"${e}"]
            }
        }else if ('yaml'==fileformat){

            try{
                //load file into string
                jobset = input.decodeJobsYAML()
            }catch (Exception e){
                return [error:"${e}"]
            }
        }else{
            return [error:g.message(code:'api.error.jobs.import.format.unsupported',args:[fileformat])]
        }
        if(null==jobset){
            return [error:g.message(code:'api.error.jobs.import.empty')]
        }
        return [jobset:jobset]
    }
    /**
     * Given list of imported jobs, create, update or skip them as defined by the dupeOption parameter.
     */
    private def loadJobs={ jobset, option, changeinfo=[:], Framework framework->
        def jobs=[]
        def jobsi=[]
        def i=1
        def msgs = []
        def errjobs = []
        def skipjobs = []
        jobset.each{ jobdata ->
            log.debug("saving job data: ${jobdata}")
            def ScheduledExecution scheduledExecution
            def jobchange=new HashMap(changeinfo)
            if(option=="update" || option=="skip"){
                //look for dupe by name and group path and project
                def schedlist
                //first look for uuid
                if(jobdata.uuid){
                    schedlist = ScheduledExecution.findAllByUuid(jobdata.uuid)
                }else{
                    schedlist = ScheduledExecution.findAllScheduledExecutions(jobdata.groupPath, jobdata.jobName, jobdata.project)
                }
                if(schedlist && 1==schedlist.size()){
                    scheduledExecution=schedlist[0]
                }
            }
            if(option == "skip" && scheduledExecution){
                jobdata.id=scheduledExecution.id
                skipjobs <<[scheduledExecution:jobdata,entrynum:i,errmsg:"A Job named '${jobdata.jobName}' already exists"]
            }
            else if(option == "update" && scheduledExecution){
                def success=false
                def errmsg
                jobchange.change = 'modify'
                if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_UPDATE],scheduledExecution.project)) {
                    errmsg = "Unauthorized: Update Job ${scheduledExecution.id}"
                }else{
                    try{
                        def result
                        if(jobdata instanceof ScheduledExecution){
                            //xxx:try/catch the update
                            result = _doupdateJob(scheduledExecution.id,jobdata,jobchange)
                            success = result[0]
                            scheduledExecution = result[1]
                        }else{
                            jobdata.id=scheduledExecution.uuid?:scheduledExecution.id
                            result = _doupdate(jobdata, jobchange)
                            success=result.success
                            scheduledExecution=result.scheduledExecution
                        }

                        if(!success && scheduledExecution && scheduledExecution.hasErrors()){
                            errmsg="Validation errors"
                        }else{
                            logJobChange(jobchange, scheduledExecution.properties)
                        }
                    }catch(Exception e){
                        errmsg=e.getMessage()
                        System.err.println("caught exception: "+errmsg);
                        e.printStackTrace()
                    }
                }
                if(!success){
                    errjobs<<[scheduledExecution:scheduledExecution,entrynum:i,errmsg:errmsg]
                }else{
                    jobs<<scheduledExecution
                    jobsi<<[scheduledExecution:scheduledExecution, entrynum:i]
                }
            }else if(option=="create" || !scheduledExecution){
                def errmsg

                if (!frameworkService.authorizeProjectResourceAll(framework, [type:'resource',kind:'job'],
                    [AuthConstants.ACTION_CREATE],jobdata.project)) {
                    errmsg="Unauthorized: Create Job"
                    errjobs << [scheduledExecution: jobdata, entrynum: i, errmsg: errmsg]
                }else{
                    try{
                        jobchange.change='create'
                        def result = _dosave(jobdata, jobchange)
                        scheduledExecution = result.scheduledExecution
                        if(!result.success && scheduledExecution && scheduledExecution.hasErrors()){
                            errmsg = "Validation errors"
                        }else if(!result.success){
                            errmsg = result.error?:"Failed to save job"
                        }else{
                            logJobChange(jobchange, scheduledExecution.properties)
                        }
                    }catch(Exception e){
                        System.err.println("caught exception");
                        e.printStackTrace()
                        scheduledExecution=jobdata
                        errmsg=e.getMessage()
                    }
                    if(scheduledExecution && !scheduledExecution.id){
                        errjobs<<[scheduledExecution:scheduledExecution,entrynum:i,errmsg:errmsg]
                    }else if (!scheduledExecution ){
                        errjobs<<[scheduledExecution:jobdata,entrynum:i,errmsg:errmsg]
                    }else{
                        jobs<<scheduledExecution
                        jobsi<<[scheduledExecution:scheduledExecution, entrynum:i]
                    }
                }
            }

            i++

        }
        return [jobs:jobs,jobsi:jobsi,msgs:msgs,errjobs:errjobs,skipjobs:skipjobs]
    }
    def upload ={
        log.debug("ScheduledExecutionController: upload " + params)

        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        
        def fileformat = params.fileformat ?: 'xml'
        def parseresult
        if(request instanceof MultipartHttpServletRequest){
            def file = request.getFile("xmlBatch")
            if (!file || file.empty) {
                flash.message = "No file was uploaded."
                return
            }
            parseresult = parseUploadedFile(file.getInputStream(), fileformat)
        }else if(params.xmlBatch){
            String fileContent=params.xmlBatch
            parseresult = parseUploadedFile(fileContent, fileformat)
        }else{
            return
        }
        def jobset

        if(parseresult.error){
            if(params.xmlreq){
                flash.error = parseresult.error
                return xmlerror()
            }else{
                render(view:'upload',model:[errors:parseresult.error])
                return
            }
        }
        jobset=parseresult.jobset
        def changeinfo = [user: session.user,method:'upload']
        def loadresults = loadJobs(jobset,params.dupeOption,changeinfo,framework)

        def jobs = loadresults.jobs
        def jobsi = loadresults.jobsi
        def msgs = loadresults.msgs
        def errjobs = loadresults.errjobs
        def skipjobs = loadresults.skipjobs

        if(!params.xmlreq){
            return [jobs: jobs, errjobs: errjobs, skipjobs: skipjobs,
                nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), 
                messages: msgs,
                didupload: true]
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
    }


    def execute = {

        if (!params.id) {
            log.error("Parameter id is required")
            flash.error = "Parameter id is required"
            response.setStatus(500)
            return error.call()
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!scheduledExecution) {
            log.error("No Job found for id: " + params.id)
            flash.error = "No Job found for id: " + params.id
            response.setStatus(404)
            return error.call()
        }
        if(!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_RUN], scheduledExecution.project)){
            return unauthorized("Execute Job ${scheduledExecution.extid}")
        }
        def model = _prepareExecute(scheduledExecution, framework)
        return model
    }

    private _prepareExecute(ScheduledExecution scheduledExecution, final def framework){
        def model=[scheduledExecution:scheduledExecution]
        model.authorized=true
        //test nodeset to make sure there are matches
        if(scheduledExecution.doNodedispatch){
            NodeSet nset = ExecutionService.filtersAsNodeSet(scheduledExecution)
            def project=frameworkService.getFrameworkProject(scheduledExecution.project, framework)
//            def nodes=project.getNodes().filterNodes(nset)
            def nodes= com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(nset, project.getNodeSet()).nodes
            if(!nodes || nodes.size()<1){
                //error
                model.nodesetempty=true
            }
            else if(ConfigurationHolder.config.gui.execution.summarizedNodes != 'false') {
                model.nodes=nodes
                model.nodemap=[:]
                model.tagsummary=[:]
                model.grouptags=[:]
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
            //check nodeset filters for variable expansion
            def varfound=NodeSet.FILTER_ENUM.find{filter->
                (nset.include?filter.value(nset.include)?.contains("\${"):false)||(nset.exclude?filter.value(nset.exclude)?.contains("\${"):false)
            }
            if(varfound){
                model.nodesetvariables=true
            }

        }

        if(params.failedNodes){
            model.failedNodes=params.failedNodes
        }
        if(params.retryFailedExecId){
            Execution e = Execution.get(params.retryFailedExecId)
            if(e){
                model.failedNodes=e.failedNodeList
            }
        }
        if(params.retryExecId){
            Execution e = Execution.get(params.retryExecId)
            if(e){
                model.selectedoptsmap=frameworkService.parseOptsFromString(e.argString)
            }
        }else if(params.argString){
            model.selectedoptsmap = frameworkService.parseOptsFromString(params.argString)
        }
        model.localNodeName=framework.getFrameworkNodeName()

        //determine option dependencies based on valuesURl embedded references
        //map of option name to list of option names which depend on it
        def depopts=[:]
        //map of option name to list of option names it depends on
        def optdeps=[:]
        scheduledExecution.options.each { Option opt->
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
        //topo sort the dependencies
        def toporesult = toposort(scheduledExecution.options*.name, depopts, optdeps)
        model.optionordering= toporesult.result
        if(scheduledExecution.options && !toporesult.result){
            log.warn("Cyclic dependency for options for job ${scheduledExecution.extid}: (${toporesult.cycle})")
            model.optionsDependenciesCyclic=true
        }

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
     * Return topo sorted list of nodes, if acyclic
     * @param nodes
     * @param oedgesin
     * @param iedgesin
     * @return
     */
    private toposort(List nodes,Map oedgesin,Map iedgesin){
        def Map oedges = deepClone(oedgesin)
        def Map iedges = deepClone(iedgesin)
        def l = new ArrayList()
        def s = new TreeSet(nodes.findAll {!iedges[it]})
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
    def executeFragment = {
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_RUN], scheduledExecution.project)) {
            return unauthorized("Execute Job ${scheduledExecution.extid}",true)
        }
        def model = _prepareExecute(scheduledExecution, framework)
        if(params.dovalidate){
            model.jobexecOptionErrors=session.jobexecOptionErrors
            model.selectedoptsmap=session.selectedoptsmap
            session.jobexecOptionErrors=null
            session.selectedoptsmap=null
            model.options=null
        }
        render(template:'execOptionsForm',model:model)
    }

    def runJobByName = {
        //lookup job
        if(!(params.jobName  && params.project || params.id)){
            flash.error="jobName or id is required"
            response.setStatus (404)
            return error()
        }
        def jobs
        if(params.id){
            final def get = scheduledExecutionService.getByIDorUUID(params.id)
            if(!get){
                log.error("No Job found for id: " + params.id)
                flash.error="No Job found for id: " + params.id
                response.setStatus (404)
                return error()
            }
            jobs = [get]
        }else{
            jobs = ScheduledExecution.findAllScheduledExecutions(params.groupPath, params.jobName, params.project)
        }
        if(!jobs || jobs.size()<1 || jobs.size()>1){
            flash.error="No unique job matched the input: ${params.jobName}, ${params.groupPath}. found (${jobs.size()})"
            response.setStatus (404)
            return error()
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        params["user"] = (session?.user) ? session.user : "anonymous"
        def scheduledExecution = jobs[0]
        def result = executionService.executeScheduledExecution(scheduledExecution,framework,request.subject,params)
        if(result.error){
            flash.error=result.message
            return error()
        }else{
            withFormat{
                html{
                    redirect(controller:"execution", action:"follow",id:result.executionId)
                }
                xml {
                    response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,"Execution started: ${result.executionId}")
                    render(contentType:"text/xml"){
                        delegate.'result'(success:true){
                            success{
                                message("Execution started: ${result.executionId}")
                            }
                            succeeded(count:1){
                                execution(index:0){
                                    id(result.executionId)
                                    name(result.name)
                                    url(g.createLink(controller:'execution',action:'follow',id:result.executionId))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * Execute job specified by parameters, and return json results
     */
    def runJobInline = {
        def results = runJob()

        if(results.error=='invalid'){
            session.jobexecOptionErrors=results.errors
            session.selectedoptsmap=results.options
        }
        return render(contentType:'application/json'){
            if(results.failed){
                delegate.'error'(results.error)
                message(results.message)
            }else{
                success(true)
                id(results.id)
            }
        }
    }
    def runJobNow = {
        return executeNow()
    }
    def executeNow = {
        def results = runJob()
        if(results.failed){
            log.error(results.message)
            if(results.error=='unauthorized'){
                return render(view:"/common/execUnauthorized",model:results)
            }else if(results.error=='invalid'){
                def model=execute.call()

                results.jobexecOptionErrors=results.errors
                results.selectedoptsmap=results.options
                results.putAll(model)
                results.options=null
                return render(view:'execute',model:results)
            }else{
                results.error= results.message
                return render(template:"/common/error",model:results)
            }
        }else if (results.error){
            log.error(results.error)
            if(results.code){
                response.setStatus (results.code)
            }
            return render(template:"/common/error",model:results)
        }else{
            redirect(controller:"execution", action:"follow",id:results.id)
        }
    }
    def runJob = {
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        params["user"] = (session?.user) ? session.user : "anonymous"
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if (!scheduledExecution) {
//            response.setStatus (404)
            return [error:"No Job found for id: " + params.id,code:404]
        }
        if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_RUN],
            scheduledExecution.project)) {
            return [success:false,failed:true,error:'unauthorized',message: "Unauthorized: Execute Job ${scheduledExecution.extid}"]
        }
        def result = executionService.executeScheduledExecution(scheduledExecution,framework, request.subject,params)

        if (result.error){
            result.failed=true
            flash.error = result.message
            return result
        }else{
            log.debug("ExecutionController: immediate execution scheduled")
//            redirect(controller:"execution", action:"follow",id:result.executionId)
            return [success:true, message:"immediate execution scheduled", id:result.executionId]
        }
    }

    public Map lookupLastExecutions(List scheduledExecutions) {
        def map = [ : ]
        log.debug("looking up lastExecutions for ["+scheduledExecutions.size()+ "] objects")
        scheduledExecutions.each {             
            def last = lookupLastExecutions(it)
            log.debug("lookupLastExecutions : found ["+last.size()+"] executions for id: "+it.id )
            if (last.size() > 0) {
                map[it.id] = last[0]
            } 
        }
        return map
    }

    public lookupLastExecutions(ScheduledExecution se) {
        def executions = []
        def criteria = Execution.createCriteria()
        def results = criteria.list {
//            like('scheduledExecutionId',String.valueOf(se.id))
            scheduledExecution{
                eq('id',se.id)
            }
            maxResults(1)
            order("dateCompleted", "desc")
        }
        log.debug("lookupLastExecutions: results count " + results.count())
        results.each {
            log.debug("Execution added to result: " + it)
            executions << it
        }
        return executions
    }

    def fetchExecutionService() {
        if (!executionService) throw new IllegalStateException("ExecutionService bean not found. Not injected?")
        return executionService
    }

    // Various methods to interact with the Scheduler

   def fetchScheduler() {
        if (!quartzScheduler) throw new IllegalStateException("Quartz Scheduler bean not found. Not injected?")
        return quartzScheduler
    }






    def Date nextExecutionTime(ScheduledExecution se) {
        def trigger = fetchScheduler().getTrigger(se.generateJobScheduledName(), se.generateJobGroupName())
        if(trigger){
            return trigger.getNextFireTime()
        }else{
            return null;
        }
    }


    /**
    * API Actions
     */

    /**
     * common action for delete or get, which will pass through to apiJobDelete or apiJobExport
     */
    def apiJobAction = {
        //switch on method
        if('DELETE'==request.method){
            return apiJobDelete()
        }else{
            return apiJobExport()
        }
    }

    /**
     * Utility, render content for jobs/import response
     */
    def renderJobsImportApiXML={jobs,jobsi,errjobs,skipjobs, delegate->
        delegate.'succeeded'(count:jobs.size()){
            jobsi.each{ Map job ->
                delegate.'job'(index:job.entrynum){
                    id(job.scheduledExecution.extid)
                    name(job.scheduledExecution.jobName)
                    group(job.scheduledExecution.groupPath?:'')
                    project(job.scheduledExecution.project)
                    url(g.createLink(action:'show',id: job.scheduledExecution.extid))
                }
            }
        }
        delegate.failed(count:errjobs.size()){
            errjobs.each{ Map job ->
                delegate.'job'(index:job.entrynum){
                    if(job.scheduledExecution.id){
                        id(job.scheduledExecution.extid)
                        url(g.createLink(action:'show',id: job.scheduledExecution.extid))
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
                delegate.'job'(index:job.entrynum){
                    if(job.scheduledExecution.id){
                        id(job.scheduledExecution.extid)
                        url(g.createLink(action:'show',id: job.scheduledExecution.extid))
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
    }
    /**
     * API: /jobs/import, version 1
     */
    def apiJobsImport= {
        log.debug("ScheduledExecutionController: upload " + params)
        def fileformat = params.format ?: 'xml'
        def parseresult
        if (!params.xmlBatch) {
            flash.error = g.message(code: 'api.error.parameter.required', args: ['xmlBatch'])
            return chain(controller: 'api', action: 'error')
        }
        if (request instanceof MultipartHttpServletRequest) {
            def file = request.getFile("xmlBatch")
            if (!file) {
                flash.errorCode = "api.error.jobs.import.missing-file"
                return chain(controller: 'api', action: 'renderError')
            }
            parseresult = parseUploadedFile(file.getInputStream(), fileformat)
        }else if (params.xmlBatch) {
            String fileContent = params.xmlBatch
            parseresult = parseUploadedFile(fileContent, fileformat)
        }else{
            flash.errorCode = "api.error.jobs.import.missing-file"
            return chain(controller: 'api', action: 'renderError')
        }

        if (parseresult.error) {
            flash.error = parseresult.error
            return chain(controller: 'api', action: 'error')
        }
        def jobset = parseresult.jobset
        def changeinfo = [user: session.user,method:'apiJobsImport']
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def loadresults = loadJobs(jobset,params.dupeOption,changeinfo,framework)

        def jobs = loadresults.jobs
        def jobsi = loadresults.jobsi
        def msgs = loadresults.msgs
        def errjobs = loadresults.errjobs
        def skipjobs = loadresults.skipjobs


        return new ApiController().success {delegate ->
            renderJobsImportApiXML(jobs, jobsi, errjobs, skipjobs, delegate)
        }
    }

    /**
     * API: export job definition: /job/{id}, version 1
     */
    def apiJobExport={
        log.debug("ScheduledExecutionController: /api/job GET : params: " + params)
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if (!scheduledExecution) {
            flash.errorCode = "api.error.item.doesnotexist"
            flash.errorArgs = ['Job ID',params.id]
            return chain(controller: 'api', action: 'renderError')
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_READ], scheduledExecution.project)) {
            request.errorCode = "api.error.item.unauthorized"
            request.errorArgs = ['Read','Job ID', params.id]
            return new ApiController().renderError()
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
    def apiJobRun = {
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!scheduledExecution) {
            flash.errorCode = "api.error.item.doesnotexist"
            flash.errorArgs = ['Job ID', params.id]
            return chain(controller: 'api', action: 'renderError')
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)

        if (!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_RUN],
            scheduledExecution.project)) {
            request.errorCode = "api.error.item.unauthorized"
            request.errorArgs = ['Run', 'Job ID', params.id]
            return new ApiController().renderError()
        }
        def inparams = [extra:[:]]
        inparams["user"] = (session?.user) ? session.user : "anonymous"

        if (params.argString) {
            inparams.extra["argString"] = params.argString
        }
        if (params.loglevel) {
            inparams.extra["loglevel"] = params.loglevel
        }
        //convert api parameters to node filter parameters
        def filters = FrameworkController.extractApiNodeFilterParams(params)
        if (filters) {
            inparams.extra['_replaceNodeFilters']='true'
            inparams.extra['doNodedispatch']=true
            filters.each {k, v ->
                inparams.extra[k] = v
            }
            if(null==inparams.extra['nodeExcludePrecedence']){
                inparams.extra['nodeExcludePrecedence'] = true
            }
        }

        def result = executionService.executeScheduledExecution(scheduledExecution, framework, request.subject, inparams)
        if (result.error) {
            flash.error = result.message
            return chain(controller: "api", action: "error")
        }
        return new ExecutionController().renderApiExecutionListResultXML([result.execution])
    }

    /**
     * API: DELETE job definition: /job/{id}, version 1
     */
    def apiJobDelete={
        log.debug("ScheduledExecutionController: /api/job DELETE : params: " + params)
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if (!scheduledExecution) {
            flash.error = g.message(code:"api.error.item.doesnotexist",args:['Job ID',params.id])
            return chain(controller: 'api', action: 'error')
        }
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if(!frameworkService.authorizeProjectJobAll(framework, scheduledExecution, [AuthConstants.ACTION_DELETE],
            scheduledExecution.project)){
            request.errorCode = "api.error.item.unauthorized"
            request.errorArgs = ['Delete','Job ID', params.id]
            return new ApiController().renderError()
        }
        def changeinfo = [user: session.user, method: 'apiJobDelete', change: 'delete']
        def jobdata = scheduledExecution.properties
        def jobtitle = "[" + params.id + "] " + scheduledExecution.generateFullName()
        def result=scheduledExecutionService.deleteScheduledExecution(scheduledExecution)


        if (!result.success) {
            flash.error = result.error
            return new ApiController().error()
        }else{
            logJobChange(changeinfo,jobdata)
            def resmsg= g.message(code:'api.success.job.delete.message',args:[jobtitle])

            return new ApiController().success{ delegate->
                delegate.'success'{
                    message(resmsg)
                }
            }
        }
    }



    /**
     * API: run simple exec: /api/run/command, version 1
     */
    def apiRunCommand={

        if(!params.project){
            flash.error=g.message(code:'api.error.parameter.required',args:['project'])
            return chain(controller:'api',action:'error')
        }
        if(!params.exec){
            flash.error=g.message(code:'api.error.parameter.required',args:['exec'])
            return chain(controller:'api',action:'error')
        }
        //test valid project
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)

        def exists=frameworkService.existsFrameworkProject(params.project,framework)
        if(!exists){
            flash.error=g.message(code:'api.error.item.doesnotexist',args:['project',params.project])
            return chain(controller:'api',action:'error')
        }

        //remote any input parameters that should not be used when creating the execution
        ['options','scheduled'].each{params.remove(it)}
        params.workflow=new Workflow(commands:[new CommandExec(adhocRemoteString:params.remove('exec'), adhocExecution:true)])
        params.description=params.description?:""

        //convert api parameters to node filter parameters
        def filters=FrameworkController.extractApiNodeFilterParams(params)
        if(filters){
            filters['doNodedispatch']=true
            filters.each{k,v->
                params[k]=v
            }
            if (null == params['nodeExcludePrecedence']) {
                params['nodeExcludePrecedence'] = true
            }
        }

        def results=runAdhoc()
        if(results.failed){
            results.error=results.message
        }
        if(results.error){
            flash.error=results.error
            if(results.scheduledExecution){
                flash.errors=[]
                results.scheduledExecution.errors.allErrors.each{
                    flash.errors<<g.message(error:it)
                }
            }
            return chain(controller:'api',action:'error')
        }else{
            return new ApiController().success{ delegate ->
                delegate.'success'{
                    message("Immediate execution scheduled (${results.id})")
                }
                delegate.'execution'(id:results.id)
            }
        }
    }


    /**
     * API: run script: /api/run/script, version 1
     */
    def apiRunScript={

        if(!params.project){
            flash.error=g.message(code:'api.error.parameter.required',args:['project'])
            return chain(controller:'api',action:'error')
        }
        if(!params.scriptFile){
            flash.error=g.message(code:'api.error.parameter.required',args:['scriptFile'])
            return chain(controller:'api',action:'error')
        }
        //test valid project
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)

        def exists=frameworkService.existsFrameworkProject(params.project,framework)
        if(!exists){
            flash.error=g.message(code:'api.error.item.doesnotexist',args:['project',params.project])
            return chain(controller:'api',action:'error')
        }

        //read attached script content
        def file = request.getFile("scriptFile")
        if(file.empty) {
            flash.error=g.message(code:'api.error.run-script.upload.is-empty')
            return chain(controller:'api',action:'error')
        }

        def script=new String(file.bytes)

        //remote any input parameters that should not be used when creating the execution
        ['options','scheduled'].each{params.remove(it)}
        params.workflow=new Workflow(commands:[new CommandExec(adhocLocalString:script, adhocExecution:true, argString:params.argString)])
        params.description=params.description?:""

        //convert api parameters to node filter parameters
        def filters=FrameworkController.extractApiNodeFilterParams(params)
        if(filters){
            filters['doNodedispatch'] = true
            filters.each{k,v->
                params[k]=v
            }
            if (null == params['nodeExcludePrecedence']) {
                params['nodeExcludePrecedence'] = true
            }
        }

        def results=runAdhoc()
        if(results.failed){
            results.error=results.message
        }
        if(results.error){
            flash.error=results.error
            if(results.scheduledExecution){
                flash.errors=[]
                results.scheduledExecution.errors.allErrors.each{
                    flash.errors<<g.message(error:it)
                }
            }
            return chain(controller:'api',action:'error')
        }else{
            return new ApiController().success{ delegate ->
                delegate.'success'{
                    message("Immediate execution scheduled (${results.id})")
                }
                delegate.'execution'(id:results.id)
            }
        }
    }

    /**
     * API: run script: /api/run/url, version 4
     */
    def apiRunScriptUrl = {
        if (!new ApiController().requireVersion(ApiRequestFilters.V4)) {
            return
        }
        if (!params.project) {
            flash.error = g.message(code: 'api.error.parameter.required', args: ['project'])
            return chain(controller: 'api', action: 'error')
        }
        if (!params.scriptURL) {
            flash.error = g.message(code: 'api.error.parameter.required', args: ['scriptURL'])
            return chain(controller: 'api', action: 'error')
        }
        //test valid project
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)

        def exists = frameworkService.existsFrameworkProject(params.project, framework)
        if (!exists) {
            flash.error = g.message(code: 'api.error.item.doesnotexist', args: ['project', params.project])
            return chain(controller: 'api', action: 'error')
        }

        //remote any input parameters that should not be used when creating the execution
        ['options', 'scheduled'].each {params.remove(it)}
        params.workflow = new Workflow(commands: [new CommandExec(adhocFilepath: params.scriptURL, adhocExecution: true, argString: params.argString)])
        params.description = params.description ?: ""

        //convert api parameters to node filter parameters
        def filters = FrameworkController.extractApiNodeFilterParams(params)
        if (filters) {
            filters['doNodedispatch'] = true
            filters.each {k, v ->
                params[k] = v
            }
            if (null == params['nodeExcludePrecedence']) {
                params['nodeExcludePrecedence'] = true
            }
        }

        def results = runAdhoc()
        if (results.failed) {
            results.error = results.message
        }
        if (results.error) {
            flash.error = results.error
            if (results.scheduledExecution) {
                flash.errors = []
                results.scheduledExecution.errors.allErrors.each {
                    flash.errors << g.message(error: it)
                }
            }
            return chain(controller: 'api', action: 'error')
        } else {
            return new ApiController().success { delegate ->
                delegate.'success' {
                    message("Immediate execution scheduled (${results.id})")
                }
                delegate.'execution'(id: results.id)
            }
        }
    }
    /**
     * API: /api/job/{id}/executions , version 1
     */
    def apiJobExecutions = {
        if (!params.id) {
            flash.error = g.message(code: 'api.error.parameter.required', args: ['id'])
            return chain(controller: 'api', action: 'error')
        }
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!scheduledExecution) {
            flash.errorCode = "api.error.item.doesnotexist"
            flash.errorArgs = ['Job ID', params.id]
            return chain(controller: 'api', action: 'renderError')
        }

        def state=params['status']
        final statusList = [ExecutionController.EXECUTION_RUNNING, ExecutionController.EXECUTION_ABORTED, ExecutionController.EXECUTION_FAILED, ExecutionController.EXECUTION_SUCCEEDED]
        final domainStatus=[(ExecutionController.EXECUTION_FAILED):'false',
            (ExecutionController.EXECUTION_SUCCEEDED):'true']
        if(state && !(state in statusList)){
            flash.errorCode = "api.error.parameter.not.inList"
            flash.errorArgs = [params.status, 'status',statusList]
            return chain(controller: 'api', action: 'renderError')
        }
        def c = Execution.createCriteria()
        def result=c.list{
            delegate.'scheduledExecution'{
                eq('id', scheduledExecution.id)
            }
            if(state== ExecutionController.EXECUTION_RUNNING){
                isNull('dateCompleted')
            }else if(state==ExecutionController.EXECUTION_ABORTED){
                isNotNull('dateCompleted')
                eq('cancelled',true)
            }else if(state){
                isNotNull('dateCompleted')
                eq('cancelled', false)
                eq('status',domainStatus[state])
            }

            if(params.offset){
                firstResult(params.int('offset'))
            }
            if(params.max){
                maxResults(params.int('max'))
            }
            and {
                order('dateCompleted', 'desc')
                order('dateStarted', 'desc')
            }
        }

        return new ExecutionController().renderApiExecutionListResultXML(result)
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

package rundeck.controllers

import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.ReverseSeekingStreamingLogReader
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.server.authorization.AuthConstants
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.filters.ApiRequestFilters
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.LoggingService
import rundeck.services.ScheduledExecutionService
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState

import java.text.ParseException
import java.text.SimpleDateFormat

/**
* ExecutionController
*/
class ExecutionController {

    FrameworkService frameworkService
    ExecutionService executionService
    LoggingService loggingService
    ScheduledExecutionService scheduledExecutionService


    def index ={
        redirect(controller:'menu',action:'index')
    }
    def follow ={
        return render(view:'show',model:show())
    }
    def followFragment ={
        return render(view:'showFragment',model:show())
    }
    def show ={
        def Execution e = Execution.get(params.id)
        if(!e){
            log.error("Execution not found for id: "+params.id)
            flash.error = "Execution not found for id: "+params.id
            return render(template:"/common/error")
        }
        def filesize=-1
        if(null!=e.outputfilepath){
            def file = new File(e.outputfilepath)
            if (file.exists()) {
                filesize = file.length()
            }
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        if(e.scheduledExecution){
            def ScheduledExecution scheduledExecution = e.scheduledExecution //ScheduledExecution.get(e.scheduledExecutionId)

            return [scheduledExecution: scheduledExecution, execution:e, filesize:filesize]
        }else{
            return [execution:e, filesize:filesize]
        }
    }
    def mail ={
        def Execution e = Execution.get(params.id)
        if(!e){
            log.error("Execution not found for id: "+params.id)
            flash.error = "Execution not found for id: "+params.id
            return render(template:"/common/error")
        }
        def file = new File(e.outputfilepath)
        def filesize=-1
        if (file.exists()) {
            filesize = file.length()
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        if(e.scheduledExecution){
            def ScheduledExecution se = e.scheduledExecution //ScheduledExecution.get(e.scheduledExecutionId)
            return render(view:"mailNotification/status" ,model: [scheduledExecution: se, execution:e, filesize:filesize])
        }else{
            return render(view:"mailNotification/status" ,model:  [execution:e, filesize:filesize])
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
    def cancelExecution = {
        def Execution e = Execution.get(params.id)
        if(!e){
            log.error("Execution not found for id: "+params.id)
            flash.error = "Execution not found for id: "+params.id
            return withFormat {
                json{
                    render(contentType:"text/json"){
                        delegate.cancelled(false)
                        delegate.status(statusStr?statusStr:(didcancel?'killed':'failed'))
                    }
                }
                xml {
                    xmlerror.call()
                }
            }
        }
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def ScheduledExecution se = e.scheduledExecution
        def abortresult=executionService.abortExecution(se, e, session.user, framework)


        def didcancel=abortresult.abortstate in [ABORT_ABORTED,ABORT_PENDING]

        def reasonstr=abortresult.failedreason
        withFormat{
            json{
                render(contentType:"text/json"){
                    delegate.cancelled(didcancel)
                    delegate.status(abortresult.statusStr?abortresult.statusStr:(didcancel?'killed':'failed'))
                    if(reasonstr){
                        delegate.'reason'(reasonstr)
                    }
                }
            }
            xml {
                render(contentType:"text/xml",encoding:"UTF-8"){
                    result(error:false,success:didcancel){
                        success{
                            message("Job status: ${abortresult.statusStr?abortresult.statusStr:(didcancel?'killed': 'failed')}")
                        }
                    }
                }
            }
        }
    }

    def downloadOutput = {
        Execution e = Execution.get(Long.parseLong(params.id))
        if(!e){
            log.error("Execution with id "+params.id+" not found")
            flash.error="No Execution found for id: " + params.id
            flash.message="No Execution found for id: " + params.id
            return
        }

        def jobcomplete = e.dateCompleted!=null
        def reader = loggingService.getLogReader(e)
        if (reader.state==ExecutionLogState.NOT_FOUND) {
            response.setStatus(404)
            log.error("Output file not found")
            return
        }else if (reader.state != ExecutionLogState.AVAILABLE) {
            //TODO: handle other states
            response.setStatus(404)
            log.error("Output file not available")
            return
        }
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US);
        def dateStamp= dateFormater.format(e.dateStarted);
        response.setContentType("text/plain")
        if("inline"!=params.view){
            response.setHeader("Content-Disposition","attachment; filename=\"${e.scheduledExecution?e.scheduledExecution.jobName:'adhoc'}-${dateStamp}.txt\"")
        }
        def isFormatted = "true"==servletContext.getAttribute("output.download.formatted")
        if(params.formatted){
            isFormatted = "true"==params.formatted
        }

        SimpleDateFormat logFormater = new SimpleDateFormat("HH:mm:ss", Locale.US);
        logFormater.timeZone= TimeZone.getTimeZone("GMT")
        def iterator = reader.reader
        iterator.openStream(0)
        def lineSep=System.getProperty("line.separator")
        iterator.each{ LogEvent msgbuf ->
            response.outputStream << (isFormatted?"${logFormater.format(msgbuf.datetime)} [${msgbuf.metadata?.user}@${msgbuf.metadata?.node} ${msgbuf.metadata?.context} ${msgbuf.metadata?.command}][${msgbuf.loglevel}] ${msgbuf.message}" : msgbuf.message)
            response.outputStream<<lineSep
        }
        iterator.close()
    }
    /**
     * API: /api/execution/{id}/output, version 5
     */
    def apiExecutionOutput = {
        if (!new ApiController().requireVersion(ApiRequestFilters.V5)) {
            return
        }
        return tailExecutionOutput()
    }
    /**
     * tailExecutionOutput action, used by execution/show.gsp view to display output inline
     * Also used by apiExecutionOutput for API response
     */
    def tailExecutionOutput = {
        log.debug("tailExecutionOutput: ${params}, format: ${request.format}")
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        Execution e = Execution.get(Long.parseLong(params.id))
        def api= new ApiController()
        def reqError=false
        if(!e){
            reqError=true
            response.setStatus(404)
            request.error=g.message(code: 'api.error.item.doesnotexist', args: ['execution', params.id])
        }
        if(e && !frameworkService.authorizeProjectExecutionAll(framework,e,[AuthConstants.ACTION_READ])){
            reqError=true
            response.setStatus(403)
            request.error = g.message(code: 'api.error.item.unauthorized', args: [AuthConstants.ACTION_READ, "Execution",params.id])
        }
        def apiError={

            withFormat {
                xml {
                    api.error()
                }
                json {
                    render(contentType: "text/json") {
                        error(request.error)
                        id(params.id.toString())
                        offset("0")
                        completed(false)
                        entries() {
                        }
                    }
                }
                text {
                    render(contentType: "text/plain", text: request.error)
                }
            }
        }
        if (reqError){
            apiError();
            return
        }

        def jobcomplete = e.dateCompleted != null
        def hasFailedNodes = e.failedNodeList ? true : false
        def execState = getExecutionState(e)
        def execDuration = 0L
        execDuration = (e.dateCompleted ? e.dateCompleted.getTime() : System.currentTimeMillis()) - e.dateStarted.getTime()

        ExecutionLogReader reader
        def error=null
        reader = loggingService.getLogReader(e)
        log.debug("Reader, state: ${reader.state}, reader: ${reader.reader}")
        if (null == reader  || reader.state == ExecutionLogState.NOT_FOUND) {
            def errmsg = "No output"
            //execution has not be started yet
            def renderclos = { delegate ->
                if (e.dateCompleted) {
                    delegate.error(errmsg)
                } else {
                    delegate.message(errmsg)
                }
                delegate.empty(true)
                delegate.id(params.id.toString())
                delegate.offset("0")
                delegate.completed(jobcomplete)
                delegate.execCompleted(jobcomplete)
                delegate.hasFailedNodes(hasFailedNodes)
                delegate.execState(execState)
                delegate.execDuration(execDuration)
                delegate.entries() {
                }
            }
            withFormat {
                xml {
                    api.success({ del ->
                        del.'output' {
                            renderclos(del)
                        }
                    })
                }
                json {
                    render(contentType: "text/json") {
                        renderclos(delegate)
                    }
                }
                text {
                    response.addHeader('X-Rundeck-ExecOutput-Offset', "0")
                    if (e.dateCompleted) {
                        response.addHeader('X-Rundeck-ExecOutput-Error', errmsg)
                    } else {
                        response.addHeader('X-Rundeck-ExecOutput-Message', errmsg)
                    }
                    response.addHeader('X-Rundeck-ExecOutput-Empty', 'true')
                    response.addHeader('X-Rundeck-ExecOutput-Completed', jobcomplete.toString())
                    response.addHeader('X-Rundeck-Exec-Completed', jobcomplete.toString())
                    response.addHeader('X-Rundeck-Exec-State', execState)
                    response.addHeader('X-Rundeck-Exec-Duration', execDuration.toString())
                    render(contentType: "text/plain") {
                        ''
                    }
                }
            }
            return;
        }
        else if (null == reader || reader.state in [ExecutionLogState.PENDING_LOCAL, ExecutionLogState.PENDING_REMOTE, ExecutionLogState.WAITING]) {
            //pending data
            def renderclos = { delegate ->
                delegate.message("Pending")
                delegate.pending(g.message(code:'execution.log.storage.state.'+reader.state,default: "Pending"))
                delegate.id(params.id.toString())
                delegate.offset(params.offset ? params.offset.toString() : "0")
                delegate.completed(false)
                delegate.execCompleted(jobcomplete)
                delegate.hasFailedNodes(hasFailedNodes)
                delegate.execState(execState)
                delegate.execDuration(execDuration)
                delegate.entries() {
                }
            }
            withFormat {
                xml {
                    api.success({ del ->
                        del.'output' {
                            renderclos(del)
                        }
                    })
                }
                json {
                    render(contentType: "text/json") {
                        renderclos(delegate)
                    }
                }
                text {
                    response.addHeader('X-Rundeck-ExecOutput-Message', 'Pending')
                    response.addHeader('X-Rundeck-ExecOutput-Pending', g.message(code: 'execution.log.storage.state.' + reader.state, default: "Pending"))
                    response.addHeader('X-Rundeck-ExecOutput-Offset', params.offset ? params.offset.toString() : "0")
                    response.addHeader('X-Rundeck-ExecOutput-Completed', "false")
                    response.addHeader('X-Rundeck-Exec-Completed', jobcomplete.toString())
                    response.addHeader('X-Rundeck-Exec-State', execState)
                    response.addHeader('X-Rundeck-Exec-Duration', execDuration.toString())
                    render(contentType: "text/plain") {
                        ''
                    }
                }
            }
            return
        }
        def StreamingLogReader logread=reader.reader

        def Long offset = 0
        if(params.offset){

            try {
                offset= Long.parseLong(params.offset)
            } catch (NumberFormatException exc) {
                reqError = true
            }
            if(offset<0){
                reqError=true
            }
            if(reqError){
                request.error = g.message(code: 'api.error.parameter.invalid', args: [params.offset, 'offset', 'Not an integer offset'])
                apiError()
                return
            }
        }

        def totsize = logread.getTotalSize()
        long lastmodl = logread.lastModified?.time

        if(params.lastmod && lastmodl>0){
            def ll = 0
            if (params.lastmod) {

                try {
                    ll = Long.parseLong(params.lastmod)
                } catch (NumberFormatException exc) {
                    reqError = true
                }
                if (ll < 0) {
                    reqError = true
                }
                if (reqError) {
                    request.error = g.message(code: 'api.error.parameter.invalid', args: [params.lastmod, 'lastmod', 'Not a millisecond modification time'])
                    apiError()
                    return
                }
            }

            if (lastmodl <= ll && (offset==0 || totsize <= offset)) {

                def renderclos = {delegate ->
                    delegate.message("Unmodified")
                    delegate.unmodified(true)
                    delegate.id(params.id.toString())
                    delegate.offset(params.offset ? params.offset.toString() : "0")
                    delegate.completed(jobcomplete)
                    delegate.execCompleted(jobcomplete)
                    delegate.hasFailedNodes(hasFailedNodes)
                    delegate.execState(execState)
                    delegate.lastModified(lastmodl.toString())
                    delegate.execDuration(execDuration)
                    delegate.totalSize(totsize)
                    delegate.entries() {
                    }
                }
                withFormat {
                    xml {
                        api.success({del ->
                            del.'output' {
                                renderclos(del)
                            }
                        })
                    }
                    json {
                        render(contentType: "text/json") {
                            renderclos(delegate)
                        }
                    }
                    text {
                        response.addHeader('X-Rundeck-ExecOutput-Message', 'Unmodified')
                        response.addHeader('X-Rundeck-ExecOutput-Unmodified', 'true')
                        response.addHeader('X-Rundeck-ExecOutput-Offset', params.offset ? params.offset.toString() : "0")
                        response.addHeader('X-Rundeck-ExecOutput-Completed', jobcomplete.toString())
                        response.addHeader('X-Rundeck-Exec-Completed', jobcomplete.toString())
                        response.addHeader('X-Rundeck-Exec-State', execState)
                        response.addHeader('X-Rundeck-Exec-Duration', execDuration.toString())
                        response.addHeader('X-Rundeck-ExecOutput-LastModifed', lastmodl.toString())
                        response.addHeader('X-Rundeck-ExecOutput-TotalSize', totsize.toString())
                        render(contentType: "text/plain") {
                            ''
                        }
                    }
                }
                return
            }
        }
        def storeoffset=offset
        def entry=[]
        def completed=false
        def max= 0
        def lastlinesSupported= (ReverseSeekingStreamingLogReader.isInstance(logread))
        if(params.lastlines && lastlinesSupported){
            def ReverseSeekingStreamingLogReader reversing= (ReverseSeekingStreamingLogReader) logread
            def lastlines = Long.parseLong(params.lastlines)
            reversing.openStreamFromReverseOffset(lastlines)
            //load only the last X lines of the file, by going to the end and searching backwards for the
            max=lastlines+1
        }else{
            logread.openStream(offset)

            if (null != params.maxlines) {
                max = Integer.parseInt(params.maxlines)
            }
        }

        def String bufsizestr= servletContext.getAttribute("execution.follow.buffersize");
        def Long bufsize= (bufsizestr?bufsizestr.toInteger():0);
        if(bufsize<(25*1024)){
            bufsize=25*1024
        }

        for(LogEvent data : logread){
            log.debug("read stream event: ${data}")
            def logdata= [mesg: data.message, time: data.datetime, level: data.loglevel.toString()] + (data.metadata?:[:])
            entry<<logdata
            if (!(0 == max || entry.size() < max)){
                break
            }
        }
        storeoffset= logread.offset
        completed = logread.complete || (jobcomplete && storeoffset==totsize)
        log.debug("finish stream iterator, offset: ${storeoffset}, completed: ${completed}")

        if("true" == servletContext.getAttribute("output.markdown.enabled") && !params.disableMarkdown){
            entry.each{
                if(it.mesg){
                    try{
                        it.loghtml = it.mesg.decodeMarkdown()
                    }catch (Exception exc){
                        log.error("Markdown error: "+exc.getMessage(),exc)
                    }
                }
            }
        }else if (params.markdown=='group'){
            def ctx=[:]
            def newe=[]
            def buf=[]
            entry.each {et->
                if(et.command!=ctx.command || et.node!=ctx.node){
                    if (newe){
                        //push buf
                        ctx.loghtml=buf.join("\n").decodeMarkdown()
                        buf = []
                    }
                    ctx = et
                    newe<< et
                }
                buf<< et.mesg
            }
            ctx.loghtml = buf.join("\n").decodeMarkdown()
            entry=newe
        }
        long marktime=System.currentTimeMillis()
        def percent=100.0 * (((float)storeoffset)/((float)totsize))
        log.debug("percent: ${percent}, store: ${storeoffset}, total: ${totsize} lastmod : ${lastmodl}")
        //via http://benjchristensen.com/2008/02/07/how-to-strip-invalid-xml-characters/
        String invalidXmlPattern = "[^" + "\\u0009\\u000A\\u000D" + "\\u0020-\\uD7FF" +
                "\\uE000-\\uFFFD" + "\\u10000-\\u10FFFF" + "]+";

        def idstr=e.id.toString()
        def renderclos ={ outf, delegate->
            delegate.id(idstr)
            delegate.offset(storeoffset.toString())
            delegate.completed(completed)
            delegate.execCompleted(jobcomplete)
            delegate.hasFailedNodes(hasFailedNodes)
            delegate.execState(execState)
            delegate.lastModified(lastmodl.toString())
            delegate.execDuration(execDuration)
            delegate.percentLoaded(percent)
            delegate.totalSize(totsize)
            delegate.lastlinesSupported(lastlinesSupported)

            def timeFmt = new SimpleDateFormat("HH:mm:ss")
            delegate.entries(){
                entry.each{
                    def datamap = [
                        time: timeFmt.format(it.time),
                        absolute_time: g.w3cDateValue([date:it.time]),
                        level: it.level,
                        log: it.mesg?.replaceAll(/\r?\n$/,''),
                        user: it.user,
//                        module: it.module,
                        command: it.command,
                        node: it.node,
//                        context: it.context
                    ]
                    if(it.loghtml){
                        datamap.loghtml=it.loghtml
                    }
                    if(outf=='json'){
                        delegate.'entries'(datamap)
                    }else{
                        datamap.log=datamap.log.replaceAll(invalidXmlPattern,'')
                        //xml
                        if(request.api_version <= ApiRequestFilters.V5){
                            def text= datamap.remove('log')
                            delegate.'entry'(datamap,text)
                        }else{
                            delegate.'entry'(datamap)
                        }
                    }
                }
            }
        }

        withFormat {
            xml {
                api.success({del ->
                    del.'output' {
                        renderclos('xml',del)
                    }
                })
            }
            json {
                render(contentType: "text/json") {
                    renderclos('json',delegate)
                }
            }
            text{
                response.addHeader('X-Rundeck-ExecOutput-Offset', storeoffset.toString())
                response.addHeader('X-Rundeck-ExecOutput-Completed', completed.toString())
                response.addHeader('X-Rundeck-Exec-Completed', jobcomplete.toString())
                response.addHeader('X-Rundeck-Exec-State', execState)
                response.addHeader('X-Rundeck-Exec-Duration', execDuration.toString())
                response.addHeader('X-Rundeck-ExecOutput-LastModifed', lastmodl.toString())
                response.addHeader('X-Rundeck-ExecOutput-TotalSize', totsize.toString())
                response.addHeader('X-Rundeck-ExecOutput-LastLinesSupported', lastlinesSupported.toString())
                def lineSep = System.getProperty("line.separator")
                render(contentType:"text/plain"){
                    entry.each{
                        out<<it.mesg+lineSep
                    }
                }
            }
        }
    }



    /**
    * API actions
     */

    public static String EXECUTION_RUNNING = "running"
    public static String EXECUTION_SUCCEEDED = "succeeded"
    public static String EXECUTION_FAILED = "failed"
    public static String EXECUTION_ABORTED = "aborted"
    public static String getExecutionState(Execution e){
        return null==e.dateCompleted?EXECUTION_RUNNING:"true"==e.status?EXECUTION_SUCCEEDED:e.cancelled?EXECUTION_ABORTED:EXECUTION_FAILED
    }
    /**
     * Render execution list xml given a List of executions, and a builder delegate
     */
    public def renderApiExecutions= { execlist, paging=[:],delegate ->
        def execAttrs=[count: execlist.size()]
        if(paging){
            execAttrs.putAll(paging)
        }
        delegate.'executions'(execAttrs) {
            execlist.each {Execution e ->
                e = Execution.get(e.id)
                execution(
                    /** attributes   **/
                    id: e.id,
                    href: g.createLink(controller: 'execution', action: 'follow', id: e.id, absolute: true),
                    status: getExecutionState(e)
                ) {
                    /** elements   */
                    user(e.user)
                    delegate.'date-started'(unixtime: e.dateStarted.time, g.w3cDateValue(date: e.dateStarted))
                    if (null != e.dateCompleted) {
                        delegate.'date-ended'(unixtime: e.dateCompleted.time, g.w3cDateValue(date: e.dateCompleted))
                    }
                    if (e.cancelled) {
                        abortedby(e.abortedby ? e.abortedby : e.user)
                    }
                    if (e.scheduledExecution) {
                        def jobparams= [id: e.scheduledExecution.extid]
                        if(e.scheduledExecution.totalTime>=0 && e.scheduledExecution.execCount>0){
                            def long avg= Math.floor(e.scheduledExecution.totalTime / e.scheduledExecution.execCount)
                            jobparams.averageDuration=avg
                        }
                        job(jobparams) {
                            name(e.scheduledExecution.jobName)
                            group(e.scheduledExecution.groupPath ?: '')
                            project(e.scheduledExecution.project)
                            description(e.scheduledExecution.description)
                        }
                    }
                    description(executionService.summarizeJob(e.scheduledExecution, e))
                    argstring(e.argString)
                }
            }
        }
    }

    /**
     * Render execution list into a map data structure
     */
    public List<Map> exportExecutionData(List<Execution> execlist){
        def executions= execlist.collect { Execution e ->
            e = Execution.get(e.id)
            def emap =[
                id: e.id,
                href: g.createLink(controller: 'execution', action: 'follow', id: e.id, absolute: true),
                status: getExecutionState(e),
                user:e.user,
                dateStarted: e.dateStarted,
                'dateStartedUnixtime': e.dateStarted.time,
                'dateStartedW3c':g.w3cDateValue(date: e.dateStarted),
                description:executionService.summarizeJob(e.scheduledExecution, e),
                argstring:e.argString,
                project: e.project,
                failedNodeListString:  e.failedNodeList,
                failedNodeList:  e.failedNodeList?.split(",") as List,
                loglevel : ExecutionService.textLogLevels[e.loglevel] ?: e.loglevel
            ]
            if (null != e.dateCompleted) {
                emap.dateEnded= e.dateCompleted
                emap['dateEndedUnixtime']= e.dateCompleted.time
                emap['dateEndedW3c']=g.w3cDateValue(date: e.dateCompleted)
            }
            if (e.cancelled) {
                emap['abortedby']=e.abortedby
            }
            if (e.scheduledExecution) {
                emap.job = [
                    id: e.scheduledExecution.extid,
                    href: g.createLink(controller: 'scheduledExecution', action: 'show', id: e.scheduledExecution.extid, absolute: true),
                    name: e.scheduledExecution.jobName,
                    group: e.scheduledExecution.groupPath ?: '',
                    project: e.scheduledExecution.project,
                    description: e.scheduledExecution.description
                ]
                if (e.scheduledExecution.totalTime >= 0 && e.scheduledExecution.execCount > 0) {
                    def long avg = Math.floor(e.scheduledExecution.totalTime / e.scheduledExecution.execCount)
                    emap.job.averageDuration = avg
                }
            }
            emap
        }
        executions
    }
    /**
     * Utility, render xml response for a list of executions
     */
    public def renderApiExecutionListResultXML={execlist,paging=[:] ->
        return new ApiController().success(renderApiExecutions.curry(execlist,paging))
    }
    /**
     * API: /api/execution/{id} , version 1
     */
    def apiExecution={
        def Execution e = Execution.get(params.id)
        if (!e) {
            flash.errorCode = "api.error.item.doesnotexist"
            flash.errorArgs = ['Execution ID',params.id]
            return chain(controller: 'api', action: 'renderError')
        }
        def filesize=-1
        if(null!=e.outputfilepath){
            def file = new File(e.outputfilepath)
            if (file.exists()) {
                filesize = file.length()
            }
        }
        return renderApiExecutionListResultXML([e])
    }

    public static String ABORT_PENDING="pending"
    public static String ABORT_ABORTED="aborted"
    public static String ABORT_FAILED="failed"
    /**
     * API: /api/execution/{id}/abort, version 1
     */
    def apiExecutionAbort={
        def Execution e = Execution.get(params.id)
        if (!e) {
            flash.errorCode = "api.error.item.doesnotexist"
            flash.errorArgs = ['Execution ID',params.id]
            return chain(controller: 'api', action: 'renderError')
        }
        def ScheduledExecution se = e.scheduledExecution
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def user=session.user
        def killas=null
        if (params.asUser && new ApiController().requireVersion(ApiRequestFilters.V5)) {
            //authorized within service call
            killas= params.asUser
        }
        def abortresult=executionService.abortExecution(se, e, user, framework, killas)

        def reportstate=[status: abortresult.abortstate]
        if(abortresult.failedreason){
            reportstate.reason= abortresult.failedreason
        }
        return new ApiController().success{ delegate->
            delegate.'success'{
                message("Execution status: ${abortresult.statusStr?abortresult.statusStr:abortresult.jobstate}")
            }
            delegate.'abort'(reportstate){
                execution(id:params.id, status: abortresult.jobstate)
            }

        }
    }

    /**
     * API: /api/executions query interface, version 5
     */
    def apiExecutionsQuery = {ExecutionQuery query->
        if (!new ApiController().requireVersion(ApiRequestFilters.V5)) {
            return
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if(query.hasErrors()){
            request.errorCode = "api.error.parameter.error"
            request.errorArgs = [query.errors.allErrors.collect{message(error: it)}.join("; ")]
            return new ApiController().renderError()
        }
        if (!params.project) {
            request.error = g.message(code: 'api.error.parameter.required', args: ['project'])
            return new ApiController().error()
        }
        query.projFilter=params.project
        if (null != query) {
            query.configureFilter()
        }

        //attempt to parse/bind "end" and "begin" parameters
        if (params.begin) {
            try {
                query.endafterFilter = ReportsController.parseDate(params.begin)
                query.doendafterFilter = true
            } catch (ParseException e) {
                flash.error = g.message(code: 'api.error.history.date-format', args: ['begin', params.begin])
                return chain(controller: 'api', action: 'error')
            }
        }
        if (params.end) {
            try {
                query.endbeforeFilter = ReportsController.parseDate(params.end)
                query.doendbeforeFilter = true
            } catch (ParseException e) {
                flash.error = g.message(code: 'api.error.history.date-format', args: ['end', params.end])
                return chain(controller: 'api', action: 'error')
            }
        }


        def state = query.statusFilter
        def txtfilters = ScheduledExecutionQuery.TEXT_FILTERS
        def eqfilters = ScheduledExecutionQuery.EQ_FILTERS
        def boolfilters = ScheduledExecutionQuery.BOOL_FILTERS
        def filters = ScheduledExecutionQuery.ALL_FILTERS
        def excludeTxtFilters=['excludeJob':'jobName']
        def excludeEqFilters=['excludeJobExact':'jobName']

        def jobqueryfilters=['jobListFilter','jobIdListFilter','excludeJobListFilter','excludeJobIdListFilter','jobFilter','jobExactFilter','groupPath','groupPathExact','descFilter','excludeGroupPath','excludeGroupPathExact','excludeJobFilter','excludeJobExactFilter']

        def convertids = {String s->
            try {
                return Long.valueOf(s)
            } catch (NumberFormatException e) {
                return s
            }
        }
        def idlist = query.jobIdListFilter?.collect(convertids)
        def xidlist = query.excludeJobIdListFilter?.collect(convertids)

        def hasJobFilters= jobqueryfilters.any {query[it]}
        if(hasJobFilters && query.adhoc){
            flash.errorCode = "api.error.parameter.error"
            flash.errorArgs = [message(code:'api.executions.jobfilter.adhoc.conflict')]
            return chain(controller: 'api', action: 'renderError')
        }
        def resOffset=params.offset?params.int('offset'):0
        def resMax=params.max?params.int('max'):20
        def criteriaClos={ isCount->
            if(query.adhoc){
                isNull('scheduledExecution')
            }else if(null!=query.adhoc || hasJobFilters){
                isNotNull('scheduledExecution')
            }
            if(!query.adhoc && hasJobFilters){
                delegate.'scheduledExecution' {
                    //begin related ScheduledExecution query
                    if (idlist) {
                        def idq= {
                            idlist.each { theid ->
                                if (theid instanceof Long) {
                                    eq("id", theid)
                                } else {
                                    eq("uuid", theid)
                                }
                            }
                        }
                        if(idlist.size()>1){

                            or{
                                idq.delegate=delegate
                                idq()
                            }
                        }else{
                            idq.delegate = delegate
                            idq()
                        }
                    }
                    if (xidlist) {
                        not{
                            xidlist.each { theid ->
                                if (theid instanceof Long) {
                                    eq("id", theid)
                                } else {
                                    eq("uuid", theid)
                                }
                            }
                        }
                    }
                    if (query.jobListFilter || query.excludeJobListFilter) {
                        if (query.jobListFilter) {
                            or {
                                query.jobListFilter.each {
                                    def z=it.split("/") as List
                                    if(z.size()>1){
                                        and{
                                            eq('jobName', z.pop())
                                            eq('groupPath', z.join("/"))
                                        }
                                    }else{
                                        and{
                                            eq('jobName', z.pop())
                                            or{
                                                eq('groupPath', "")
                                                isNull('groupPath')
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (query.excludeJobListFilter) {
                            not {
                                or {
                                    query.excludeJobListFilter.each {
                                        def z = it.split("/") as List
                                        if (z.size() > 1) {
                                            and{
                                                eq('jobName', z.pop())
                                                eq('groupPath', z.join("/"))
                                            }
                                        } else {
                                            and{
                                                eq('jobName', z.pop())
                                                or {
                                                    eq('groupPath', "")
                                                    isNull('groupPath')
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }

                    txtfilters.each { key, val ->
                        if (query["${key}Filter"]) {
                            ilike(val, '%' + query["${key}Filter"] + '%')
                        }
                    }

                    eqfilters.each { key, val ->
                        if (query["${key}Filter"]) {
                            eq(val, query["${key}Filter"])
                        }
                    }
                    boolfilters.each { key, val ->
                        if (null != query["${key}Filter"]) {
                            eq(val, query["${key}Filter"])
                        }
                    }
                    excludeTxtFilters.each { key, val ->
                        if (query["${key}Filter"]) {
                            not {
                                ilike(val, '%' + query["${key}Filter"] + '%')
                            }
                        }
                    }
                    excludeEqFilters.each { key, val ->
                        if (query["${key}Filter"]) {
                            not {
                                eq(val, query["${key}Filter"])
                            }
                        }
                    }


                    if('-'==query['groupPath']){
                        or {
                            eq("groupPath", "")
                            isNull("groupPath")
                        }
                    } else if (query["groupPath"] && '*' != query["groupPath"]) {
                        or {
                            like("groupPath", query["groupPath"] + "/%")
                            eq("groupPath", query['groupPath'])
                        }
                    }
                    if('-'==query['excludeGroupPath']){
                        not{
                            or {
                                eq("groupPath", "")
                                isNull("groupPath")
                            }
                        }
                    } else if (query["excludeGroupPath"]) {
                        not{
                            or {
                                like("groupPath", query["excludeGroupPath"] + "/%")
                                eq("groupPath", query['excludeGroupPath'])
                            }
                        }
                    }
                    if (query["groupPathExact"]) {
                        if ("-" == query["groupPathExact"]) {
                            or {
                                eq("groupPath", "")
                                isNull("groupPath")
                            }
                        } else {
                            eq("groupPath", query['groupPathExact'])
                        }
                    }
                    if (query["excludeGroupPathExact"]) {
                        if ("-" == query["excludeGroupPathExact"]) {
                            not{
                                or {
                                    eq("groupPath", "")
                                    isNull("groupPath")
                                }
                            }
                        } else {
                            or {
                                ne("groupPath", query['excludeGroupPathExact'])
                                isNull("groupPath")
                            }
                        }
                    }

                    //end related ScheduledExecution query
                }
            }
            eq('project',query.projFilter)
            if(query.userFilter){
                eq('user',query.userFilter)
            }
            if (state == ExecutionController.EXECUTION_RUNNING) {
                isNull('dateCompleted')
            } else if (state == ExecutionController.EXECUTION_ABORTED) {
                isNotNull('dateCompleted')
                eq('cancelled', true)
            } else if (state) {
                isNotNull('dateCompleted')
                eq('cancelled', false)
                eq('status', state== ExecutionController.EXECUTION_FAILED?'false':'true')
            }
            if(query.abortedbyFilter){
                eq('abortedby',query.abortedbyFilter)
            }
            if (query.dostartafterFilter && query.dostartbeforeFilter && query.startbeforeFilter && query.startafterFilter) {
                between('dateStarted', query.startafterFilter, query.startbeforeFilter)
            }
            else if (query.dostartbeforeFilter && query.startbeforeFilter) {
                le('dateStarted', query.startbeforeFilter)
            } else if (query.dostartafterFilter && query.startafterFilter) {
                ge('dateStarted', query.startafterFilter)
            }

            if (query.doendafterFilter && query.doendbeforeFilter && query.endafterFilter && query.endbeforeFilter) {
                between('dateCompleted', query.endafterFilter, query.endbeforeFilter)
            }
            else if (query.doendbeforeFilter && query.endbeforeFilter) {
                le('dateCompleted', query.endbeforeFilter)
            }
            if (query.doendafterFilter && query.endafterFilter) {
                ge('dateCompleted', query.endafterFilter)
            }

            if (!isCount) {
                if (resOffset) {
                    firstResult(resOffset)
                }
                if (resMax) {
                    maxResults(resMax)
                }
                and {
                    order('dateCompleted', 'desc')
                    order('dateStarted', 'desc')
                }
            }
        }
        def result = Execution.createCriteria().list(criteriaClos.curry(false))
        def total = Execution.createCriteria().count(criteriaClos.curry(true))

        //filter query results to READ authorized executions
        def filtered = frameworkService.filterAuthorizedProjectExecutionsAll(framework,result,[AuthConstants.ACTION_READ])


        return renderApiExecutionListResultXML(filtered,[total:total,offset:resOffset,max:resMax])
    }
}

    

package rundeck.controllers

import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.app.support.BuilderUtil
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.ReverseSeekingStreamingLogReader
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.server.authorization.AuthConstants
import rundeck.Execution
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.filters.ApiRequestFilters
import rundeck.services.ApiService
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.LoggingService
import rundeck.services.ScheduledExecutionService
import rundeck.services.WorkflowService
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState
import rundeck.services.workflow.StateMapping

import javax.servlet.http.HttpServletResponse
import java.text.ParseException
import java.text.SimpleDateFormat

/**
* ExecutionController
*/
class ExecutionController extends ControllerBase{

    FrameworkService frameworkService
    ExecutionService executionService
    LoggingService loggingService
    ScheduledExecutionService scheduledExecutionService
    ApiService apiService
    WorkflowService workflowService


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
        if(notFoundResponse(e,'Execution ID',params.id)){
            return
        }
        def filesize=-1
        if(null!=e.outputfilepath){
            def file = new File(e.outputfilepath)
            if (file.exists()) {
                filesize = file.length()
            }
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (unauthorizedResponse(frameworkService.authorizeProjectExecutionAll(authContext, e,
                [AuthConstants.ACTION_READ]), AuthConstants.ACTION_READ,'Execution',params.id)) {
            return
        }
        if(!params.project || params.project!=e.project) {
            return redirect(controller: 'execution', action: 'show', params: [id: params.id, project: e.project])
        }
        params.project=e.project
        request.project=e.project

        def enext,eprev
        def result= Execution.withCriteria {
            gt('dateStarted', e.dateStarted)
            if (e.scheduledExecution) {
                eq('scheduledExecution',e.scheduledExecution)
            }else{
                isNull('scheduledExecution')
            }
            eq('project',e.project)
            maxResults(1)
            order('dateStarted','asc')
        }
        enext=result?result[0]:null
        result = Execution.withCriteria {
            lt('dateStarted', e.dateStarted)
            if (e.scheduledExecution) {
                eq('scheduledExecution', e.scheduledExecution)
            }else{
                isNull('scheduledExecution')
            }
            eq('project', e.project)
            maxResults(1)
            order('dateStarted', 'desc')
        }
        eprev = result ? result[0] : null
        //load plugins for WF steps
        def pluginDescs=[node:[:],workflow:[:]]
        e.workflow.commands.findAll{it.instanceOf(PluginStep)}.each{PluginStep step->
            if(!pluginDescs[step.nodeStep?'node':'workflow'][step.type]){
                def description = frameworkService.getPluginDescriptionForItem(step)
                if (description) {
                    pluginDescs[step.nodeStep ? 'node' : 'workflow'][step.type]=description
                }
            }
        }
//        def state = workflowService.readWorkflowStateForExecution(e)
//        if(!state){
////            state= workflowService.previewWorkflowStateForExecution(e)
//        }
        return [scheduledExecution: e.scheduledExecution?:null,execution:e, filesize:filesize,
                enext: enext, eprev: eprev,stepPluginDescriptions: pluginDescs, ]
    }
    def ajaxExecState={
        def Execution e = Execution.get(params.id)
        if (!e) {
            log.error("Execution not found for id: " + params.id)
            flash.error = "Execution not found for id: " + params.id
            return render(contentType: 'application/json'){
                delegate.'error'('not found')
            }
        }

        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (e && !frameworkService.authorizeProjectExecutionAll(authContext, e, [AuthConstants.ACTION_READ])) {
            return render(contentType: 'application/json') {
                delegate.'error'("Unauthorized: Read Execution ${params.id}")
            }
        }

        def jobcomplete = e.dateCompleted != null
        def hasFailedNodes = e.failedNodeList ? true : false
        def execState = executionService.getExecutionState(e)
        def execDuration = 0L
        execDuration = (e.dateCompleted ? e.dateCompleted.getTime() : System.currentTimeMillis()) - e.dateStarted.getTime()
        def jobAverage=-1L
        if (e.scheduledExecution && e.scheduledExecution.totalTime >= 0 && e.scheduledExecution.execCount > 0) {
            def long avg = Math.floor(e.scheduledExecution.totalTime / e.scheduledExecution.execCount)
            jobAverage = avg
        }
        def data=[
            completed:jobcomplete,
            execDuration: execDuration,
            executionState:execState.toUpperCase(),
            jobAverageDuration: jobAverage,
            startTime:StateMapping.encodeDate(e.dateStarted),
            endTime: StateMapping.encodeDate(e.dateCompleted),
        ]
        def loader = workflowService.requestState(e)
        if (loader.state == ExecutionLogState.AVAILABLE) {
            data.state = loader.workflowState
        }else if(loader.state in [ExecutionLogState.NOT_FOUND]) {
            data.state = [error: 'not found',
                    errorMessage: g.message(code: 'execution.state.storage.state.' + loader.state,
                            default: "Not Found")]
        }else if(loader.state in [ExecutionLogState.ERROR]) {
            data.state = [error: 'error', errorMessage: g.message(code: loader.errorCode, args: loader.errorData)]
        }else if (loader.state in [ ExecutionLogState.PENDING_LOCAL, ExecutionLogState.WAITING,
                ExecutionLogState.AVAILABLE_REMOTE, ExecutionLogState.PENDING_REMOTE]) {
            data.state = [error: 'pending',
                    errorMessage: g.message(code: 'execution.state.storage.state.' + loader.state, default: "Pending")]
        }
        return render(contentType: 'application/json', text: data.encodeAsJSON())
    }
    def mail ={
        def Execution e = Execution.get(params.id)
        if (notFoundResponse(e, 'Execution ID', params.id)) {
            return
        }
        def file = loggingService.getLogFileForExecution(e)
        def filesize=-1
        if (file.exists()) {
            filesize = file.length()
        }
        final state = ExecutionService.getExecutionState(e)
        if(e.scheduledExecution){
            def ScheduledExecution se = e.scheduledExecution //ScheduledExecution.get(e.scheduledExecutionId)
            return render(view:"mailNotification/status" ,model: [execstate: state, scheduledExecution: se, execution:e, filesize:filesize])
        }else{
            return render(view:"mailNotification/status" ,model:  [execstate: state, execution:e, filesize:filesize])
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
                        delegate.cancelled=false
                        delegate.status=(statusStr?statusStr:(didcancel?'killed':'failed'))
                    }
                }
                xml {
                    xmlerror.call()
                }
            }
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def ScheduledExecution se = e.scheduledExecution
        def abortresult=executionService.abortExecution(se, e, session.user,authContext)


        def didcancel=abortresult.abortstate in [ExecutionService.ABORT_ABORTED, ExecutionService.ABORT_PENDING]

        def reasonstr=abortresult.failedreason
        withFormat{
            json{
                render(contentType:"text/json"){
                    delegate.cancelled=didcancel
                    delegate.status=(abortresult.statusStr?abortresult.statusStr:(didcancel?'killed':'failed'))
                    if(reasonstr){
                        delegate.'reason'=reasonstr
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
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
            log.error("Output file not found")
            return
        }else if (reader.state == ExecutionLogState.ERROR) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            def msg= g.message(code: reader.errorCode, args: reader.errorData)
            log.error("Output file reader error: ${msg}")
            response.outputStream << msg
            return
        }else if (reader.state != ExecutionLogState.AVAILABLE) {
            //TODO: handle other states
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
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
        iterator.findAll{it.eventType==LogUtil.EVENT_TYPE_LOG}.each{ LogEvent msgbuf ->
                response.outputStream << (isFormatted?"${logFormater.format(msgbuf.datetime)} [${msgbuf.metadata?.user}@${msgbuf.metadata?.node} ${msgbuf.metadata?.stepctx?:'_'}][${msgbuf.loglevel}] ${msgbuf.message}" : msgbuf.message)
                response.outputStream<<lineSep
        }
        iterator.close()
    }
    /**
     * API: /api/execution/{id}/output, version 5
     */
    def apiExecutionOutput = {
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V5)) {
            return
        }
        params.stateOutput=false

        if (request.api_version < ApiRequestFilters.V9) {
            params.nodename = null
            params.stepctx = null
        }
        return tailExecutionOutput()
    }
    static final String invalidXmlPattern = "[^" + "\\u0009\\u000A\\u000D" + "\\u0020-\\uD7FF" +
            "\\uE000-\\uFFFD" + "\\u10000-\\u10FFFF" + "]+";

    /**
     * Use a builder delegate to render tailExecutionOutput result in XML or JSON
     */
    private def renderOutputClosure= {String outf, Map data, List outputData, apiVersion,delegate, stateoutput=false ->
        def keys= ['id','offset','completed','empty','unmodified', 'error','message','execCompleted', 'hasFailedNodes',
                'execState', 'lastModified', 'execDuration', 'percentLoaded', 'totalSize', 'lastLinesSupported']
        def setProp={k,v->
            if(outf=='json'){
                delegate[k]=v
            }else{
                delegate."${k}"(v)
            }
        }
        keys.each{
            if(null!=data[it]){
                setProp(it,data[it])
            }
        }
        def filterparms = [:]
        ['nodename', 'stepctx'].each {
            if (data[it]) {
                filterparms[it] = data[it]
            }
        }
        if (filterparms) {
            delegate.filter(filterparms)
        }


        def timeFmt = new SimpleDateFormat("HH:mm:ss")
        def dataClos= {
            outputData.each {
                def datamap = stateoutput?(it + [
                        time: timeFmt.format(it.time),
                        absolute_time: g.w3cDateValue([date: it.time]),
                        log: it.mesg?.replaceAll(/\r?\n$/, ''),
                ]):([
                        time: timeFmt.format(it.time),
                        absolute_time: g.w3cDateValue([date: it.time]),
                        log: it.mesg?.replaceAll(/\r?\n$/, ''),
                ]+it.subMap(['level','user','command','stepctx','node']))
                datamap.remove('mesg')
                if (it.loghtml) {
                    datamap.loghtml = it.loghtml
                }
                if (outf == 'json') {
                    delegate.'entries'(datamap)
                } else {
                    datamap.log = datamap.log.replaceAll(invalidXmlPattern, '')
                    //xml
                    if (apiVersion <= ApiRequestFilters.V5) {
                        def text = datamap.remove('log')
                        delegate.'entry'(datamap, text)
                    } else {
                        delegate.'entry'(datamap)
                    }
                }
            }
        }
        if(outf=='json'){
            delegate.'entries' = delegate.array(dataClos)
        }else{
            delegate.entries(dataClos)
        }
    }
    /**
     * API: /api/execution/{id}/output/state, version ?
     */
    def apiExecutionStateOutput = {
        if (!apiService.requireVersion(request,response,ApiRequestFilters.V10)) {
            return
        }
        params.stateOutput = true
        return tailExecutionOutput()
    }
    /**
     * tailExecutionOutput action, used by execution/show.gsp view to display output inline
     * Also used by apiExecutionOutput for API response
     */
    def tailExecutionOutput = {
        log.debug("tailExecutionOutput: ${params}, format: ${request.format}")
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        Execution e = Execution.get(Long.parseLong(params.id))
        def reqError=false

        def apiError = { String code, List args, int status = 0 ->
            def message=code?g.message(code:code,args:args):'Unknown error'
            withFormat {
                xml {
                    apiService.renderErrorXml(response,[code:code,args:args,status:status])
                }
                json {
                    if (status > 0) {
                        response.setStatus(status)
                    }
                    render(contentType: "application/json") {
                        renderOutputClosure('json', [
                                error: message,
                                id: params.id.toString(),
                                offset: "0",
                                completed: false
                        ], [], request.api_version, delegate)
                    }
                }
                text {
                    if (status > 0) {
                        response.setStatus(status)
                    }
                    render(contentType: "text/plain", text: message)
                }
            }
        }
        if(!e){
            return apiError('api.error.item.doesnotexist', ['execution', params.id], HttpServletResponse.SC_NOT_FOUND);
        }
        if(e && !frameworkService.authorizeProjectExecutionAll(authContext,e,[AuthConstants.ACTION_READ])){
            return apiError('api.error.item.unauthorized', [AuthConstants.ACTION_READ, "Execution", params.id], HttpServletResponse.SC_FORBIDDEN);
        }
        if (params.stepctx && !(params.stepctx ==~ /^(\d+e?\/?)+$/)) {
            return apiError("api.error.parameter.invalid",[params.stepctx,'stepctx',"Invalid stepctx filter"],HttpServletResponse.SC_BAD_REQUEST)
        }

        def jobcomplete = e.dateCompleted != null
        def hasFailedNodes = e.failedNodeList ? true : false
        def execState = executionService.getExecutionState(e)
        def execDuration = 0L
        execDuration = (e.dateCompleted ? e.dateCompleted.getTime() : System.currentTimeMillis()) - e.dateStarted.getTime()

        ExecutionLogReader reader
        reader = loggingService.getLogReader(e)
        def error = reader.state == ExecutionLogState.ERROR
        log.debug("Reader, state: ${reader.state}, reader: ${reader.reader}")
        if(error) {
            return apiError(reader.errorCode, reader.errorData, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        if (null == reader  || reader.state == ExecutionLogState.NOT_FOUND ) {
            def errmsg = g.message(code: "execution.log.storage.state.NOT_FOUND")
            //execution has not be started yet
            def dataMap= [
                    empty:true,
                    id: params.id.toString(),
                    offset: "0",
                    completed: jobcomplete,
                    execCompleted: jobcomplete,
                    hasFailedNodes: hasFailedNodes,
                    execState: execState,
                    execDuration: execDuration
            ]
            if (e.dateCompleted) {
                dataMap.error=errmsg
            } else {
                dataMap.message=errmsg
            }
            withFormat {
                xml {
                    apiService.renderSuccessXml(response) {
                        output{
                            renderOutputClosure('xml', dataMap, [], request.api_version, delegate)
                        }
                    }
                }
                json {
                    render(contentType: "application/json") {
                        renderOutputClosure('json', dataMap, [], request.api_version, delegate)
                    }
                }
                text {
                    response.addHeader('X-Rundeck-ExecOutput-Offset', "0")
                    if (e.dateCompleted) {
                        response.addHeader('X-Rundeck-ExecOutput-Error', errmsg.toString())
                    } else {
                        response.addHeader('X-Rundeck-ExecOutput-Message', errmsg.toString())
                    }
                    response.addHeader('X-Rundeck-ExecOutput-Empty', dataMap.empty.toString())
                    response.addHeader('X-Rundeck-ExecOutput-Completed', dataMap.execCompleted.toString())
                    response.addHeader('X-Rundeck-Exec-Completed', dataMap.completed.toString())
                    response.addHeader('X-Rundeck-Exec-State', dataMap.execState.toString())
                    response.addHeader('X-Rundeck-Exec-Duration', dataMap.execDuration.toString())
                    render(contentType: "text/plain") {
                        ''
                    }
                }
            }
            return;
        }
        else if (null == reader || reader.state in [ExecutionLogState.PENDING_LOCAL, ExecutionLogState.PENDING_REMOTE, ExecutionLogState.WAITING]) {
            //pending data
            def dataMap=[
                    message:"Pending",
                    pending: g.message(code: 'execution.log.storage.state.' + reader.state, default: "Pending"),
                    id:params.id.toString(),
                    offset: params.offset ? params.offset.toString() : "0",
                    completed:false,
                    execCompleted:jobcomplete,
                    hasFailedNodes:hasFailedNodes,
                    execState:execState,
                    execDuration:execDuration
            ]
            withFormat {
                xml {
                    apiService.renderSuccessXml(response) {
                        output {
                            renderOutputClosure('xml', dataMap, [], request.api_version, delegate)
                        }
                    }
                }
                json {
                    render(contentType: "application/json") {
                        renderOutputClosure('json', dataMap, [], request.api_version, delegate)
                    }
                }
                text {
                    response.addHeader('X-Rundeck-ExecOutput-Message', dataMap.message.toString())
                    response.addHeader('X-Rundeck-ExecOutput-Pending', dataMap.pending.toString())
                    response.addHeader('X-Rundeck-ExecOutput-Offset', dataMap.offset.toString())
                    response.addHeader('X-Rundeck-ExecOutput-Completed', dataMap.execCompleted.toString())
                    response.addHeader('X-Rundeck-Exec-Completed', dataMap.completed.toString())
                    response.addHeader('X-Rundeck-Exec-State', dataMap.execState.toString())
                    response.addHeader('X-Rundeck-Exec-Duration', dataMap.execDuration.toString())
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
                return apiError('api.error.parameter.invalid', [params.offset, 'offset', 'Not an integer offset'],
                        HttpServletResponse.SC_BAD_REQUEST)
            }
        }

        def totsize = logread.getTotalSize()
        long lastmodl = logread.lastModified?.time
        long reqlastmod=0

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
                    return apiError('api.error.parameter.invalid',
                            [params.lastmod, 'lastmod', 'Not a millisecond modification time'],
                            HttpServletResponse.SC_BAD_REQUEST)
                }
            }
            reqlastmod=ll

            if (lastmodl <= ll && (offset==0 || totsize <= offset)) {
                def dataMap=[
                        message:"Unmodified",
                        unmodified:true,
                        id:params.id.toString(),
                        offset:params.offset ? params.offset.toString() : "0",
                        completed:jobcomplete,
                        execCompleted:jobcomplete,
                        hasFailedNodes:hasFailedNodes,
                        execState:execState,
                        lastModified:lastmodl.toString(),
                        execDuration:execDuration,
                        totalSize:totsize
                ]

                withFormat {
                    xml {
                        apiService.renderSuccessXml(response) {
                            output {
                                renderOutputClosure('xml', dataMap, [], request.api_version, delegate)
                            }
                        }
                    }
                    json {
                        render(contentType: "application/json") {
                            renderOutputClosure('json', dataMap, [], request.api_version, delegate)
                        }
                    }
                    text {
                        response.addHeader('X-Rundeck-ExecOutput-Message', dataMap.message.toString())
                        response.addHeader('X-Rundeck-ExecOutput-Unmodified', dataMap.unmodified.toString())
                        response.addHeader('X-Rundeck-ExecOutput-Offset', dataMap.offset.toString())
                        response.addHeader('X-Rundeck-ExecOutput-Completed', dataMap.execCompleted.toString())
                        response.addHeader('X-Rundeck-Exec-Completed', dataMap.completed.toString())
                        response.addHeader('X-Rundeck-Exec-State', dataMap.execState.toString())
                        response.addHeader('X-Rundeck-Exec-Duration', dataMap.execDuration.toString())
                        response.addHeader('X-Rundeck-ExecOutput-LastModifed', dataMap.lastModified.toString())
                        response.addHeader('X-Rundeck-ExecOutput-TotalSize', dataMap.totalSize.toString())
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
        def stateoutput = params.stateOutput in [true,'true']
        def stateonly = params.stateOnly in [true,'true']

        def filter={ LogEvent data ->
            if (!stateoutput && data.eventType != LogUtil.EVENT_TYPE_LOG) {
                return false
            }
            if (stateoutput && stateonly && data.eventType == LogUtil.EVENT_TYPE_LOG) {
                return false
            }
            if(params.nodename && data.metadata.node != params.nodename){
                return false
            }
            if(params.stepctx && params.stepctx==~/^(\d+e?\/?)+$/){
                if(params.stepctx.endsWith("/")){
                    def pref= params.stepctx[0..-2]
                    if(data.metadata.stepctx?.startsWith(params.stepctx)
                            || data.metadata.stepctx == pref
                            || data.metadata.stepctx == pref + 'e'){
                        return data
                    }else{
                        return false
                    }
                }else if(!params.stepctx.endsWith("/") && !(data.metadata.stepctx == params.stepctx
                        || (data.metadata.stepctx) == params.stepctx + 'e')){
                    return false
                }
            }
            data
        }
        for(LogEvent data : logread){
            if(!filter(data)){
                continue
            }
            log.debug("read stream event: ${data}")
            def logdata= (data.metadata ?: [:]) + [mesg: data.message, time: data.datetime, level: data.loglevel.toString(),type:data.eventType]
            entry<<logdata
            if (!(0 == max || entry.size() < max)){
                break
            }
        }
        storeoffset= logread.offset
        completed = logread.complete || (jobcomplete && storeoffset==totsize)
        log.debug("finish stream iterator, offset: ${storeoffset}, completed: ${completed}")
        if (storeoffset == offset) {
            //don't change last modified unless new data has been read
            lastmodl = reqlastmod
        }

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
                if(et.stepctx!=ctx.stepctx || et.node!=ctx.node){
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

        def resultData= [
                id: e.id.toString(),
                offset: storeoffset.toString(),
                completed: completed,
                execCompleted: jobcomplete,
                hasFailedNodes: hasFailedNodes,
                execState: execState,
                lastModified: lastmodl.toString(),
                execDuration: execDuration,
                percentLoaded: percent,
                totalSize: totsize,
                lastlinesSupported: lastlinesSupported,
                nodename:params.nodename,
                stepctx:params.stepctx
        ]
        withFormat {
            xml {
                apiService.renderSuccessXml(response) {
                    output {
                        renderOutputClosure('xml', resultData, entry, request.api_version, delegate, stateoutput)
                    }
                }
            }
            json {
                render(contentType: "application/json") {
                    renderOutputClosure('json', resultData, entry, request.api_version, delegate, stateoutput)
                }
            }
            text{
                response.addHeader('X-Rundeck-ExecOutput-Offset', storeoffset.toString())
                response.addHeader('X-Rundeck-ExecOutput-Completed', completed.toString())
                response.addHeader('X-Rundeck-Exec-Completed', jobcomplete.toString())
                response.addHeader('X-Rundeck-Exec-State', execState.toString())
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

    public String createExecutionUrl(def id,def project) {
        return g.createLink(controller: 'execution', action: 'follow', id: id, absolute: true,
                params: [project: project])
    }
    public String createServerUrl() {
        return g.createLink(controller: 'menu', action: 'index', absolute: true)
    }
    /**
     * Render execution list xml given a List of executions, and a builder delegate
     */
    public def renderApiExecutions= { List execlist, paging=[:],delegate ->
        apiService.renderExecutionsXml(execlist.collect{ Execution e->
            [
                execution:e,
                href: g.createLink(controller: 'execution', action: 'follow', id: e.id, absolute: true,
                        params: [project: e.project]),
                status: executionService.getExecutionState(e),
                summary: executionService.summarizeJob(e.scheduledExecution, e)
            ]
        },paging,delegate)
    }

    /**
     * Render execution list into a map data structure
     */
    protected List<Map> exportExecutionData(List<Execution> execlist){
        def executions= execlist.collect { Execution e ->
            e = Execution.get(e.id)
            def emap =[
                id: e.id,
                href: g.createLink(controller: 'execution', action: 'follow', id: e.id, absolute: true,
                        params: [project: e.project]),
                status: executionService.getExecutionState(e),
                user:e.user,
                dateStarted: e.dateStarted,
                'dateStartedUnixtime': e.dateStarted.time,
                'dateStartedW3c':g.w3cDateValue(date: e.dateStarted),
                description:executionService.summarizeJob(e.scheduledExecution, e),
                argstring:e.argString,
                project: e.project,
                failedNodeListString:  e.failedNodeList,
                failedNodeList:  e.failedNodeList?.split(",") as List,
                succeededNodeListString:  e.succeededNodeList,
                succeededNodeList:  e.succeededNodeList?.split(",") as List,
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
                    href: g.createLink(controller: 'scheduledExecution', action: 'show',
                            id: e.scheduledExecution.extid, absolute: true,
                            params: [project: e.project]),
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
     * API: /api/execution/{id} , version 1
     */
    def apiExecution={
        def Execution e = Execution.get(params.id)
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!e) {
            return apiService.renderErrorXml(response,
                    [status: HttpServletResponse.SC_NOT_FOUND,code: "api.error.item.doesnotexist", args: ['Execution ID', params.id]])
        } else if (!frameworkService.authorizeProjectExecutionAll(authContext,e,[AuthConstants.ACTION_READ])){
            return apiService.renderErrorXml(response,
                    [
                            status: HttpServletResponse.SC_FORBIDDEN,
                            code: "api.error.item.unauthorized",
                            args: [AuthConstants.ACTION_READ, "Execution", params.id]
                    ])
        }

        return executionService.respondExecutionsXml(response, [e])
    }
    /**
     * API: /api/execution/{id}/state , version 10
     */
    def apiExecutionState= {
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V10)) {
            return
        }
        def Execution e = Execution.get(params.id)
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!e) {
            def errormap= [status: HttpServletResponse.SC_NOT_FOUND, code: "api.error.item.doesnotexist", args: ['Execution ID', params.id]]
            withFormat {
                json{
                    return apiService.renderErrorJson(response, errormap)
                }
                xml{
                    return apiService.renderErrorXml(response, errormap)
                }
            }

        } else if (!frameworkService.authorizeProjectExecutionAll(authContext, e, [AuthConstants.ACTION_READ])) {
            def errormap = [status: HttpServletResponse.SC_FORBIDDEN,
                    code: "api.error.item.unauthorized", args: [AuthConstants.ACTION_READ, "Execution", params.id]]
            withFormat {
                json {
                    return apiService.renderErrorJson(response, errormap)
                }
                xml {
                    return apiService.renderErrorXml(response, errormap)
                }
            }
        }

        def loader = workflowService.requestState(e)
        def state= loader.workflowState
        if(!loader.workflowState){
            if(loader.state in [ExecutionLogState.WAITING, ExecutionLogState.AVAILABLE_REMOTE,
                    ExecutionLogState.PENDING_LOCAL, ExecutionLogState.PENDING_REMOTE]) {
                state = [error: 'pending']
            }else{
                def errormap=[:]
                if (loader.state in [ExecutionLogState.NOT_FOUND]) {
                    errormap = [status: HttpServletResponse.SC_NOT_FOUND, code: "api.error.item.doesnotexist",
                            args: ['Execution State ID', params.id]]
                }else {
                    errormap = [status: HttpServletResponse.SC_NOT_FOUND, code: loader.errorCode, args: loader.errorData]
                }
                    withFormat {
                        json {
                            return apiService.renderErrorJson(response, errormap)
                        }
                        xml {
                            return apiService.renderErrorXml(response, errormap)
                        }
                    }
                return
            }
        }
        def convertNodeList={Collection tnodes->
            def tnodemap = []
            tnodes.each { anode ->
                if(anode instanceof String){
                    tnodemap << [(BuilderUtil.ATTR_PREFIX + 'name'): anode]
                }else if(anode instanceof Map.Entry){
                    tnodemap << [(BuilderUtil.ATTR_PREFIX + 'name'): anode.key] + anode.value
                }
            }
            tnodemap
        }
        def convertXml;
        convertXml={Map map->
            Map newmap=[:]+map
            //for each step
            newmap.steps=map.steps.collect{Map step->
                Map newstep=[:] + step
                if(step.workflow){
                    //convert sub workflow
                    newstep.workflow=convertXml(newstep.workflow)
                }
                newstep[BuilderUtil.asAttributeName('stepctx')]= newstep.remove('stepctx')
                BuilderUtil.makeAttribute(newstep,'id')
                if(step.nodeStates){
                    newstep.nodeStates=step['nodeStates'].collect {String node,Map nodestate->
                        def nmap= [name: node] + nodestate
                        BuilderUtil.makeAttribute(nmap,'name')
                        nmap
                    }
                    BuilderUtil.makePlural(newstep,'nodeStates')
                }
                if (step.stepTargetNodes) {
                    newstep.stepTargetNodes = [(BuilderUtil.pluralize('nodes')):convertNodeList(step['stepTargetNodes'])]
                }
                newstep
            }
            if(newmap.steps){
                //make steps into a <steps><step/><step/>..</steps>
                BuilderUtil.makePlural(newmap,'steps')
            }
            if (map.targetNodes) {
                newmap.targetNodes = [(BuilderUtil.pluralize('nodes')):convertNodeList(map['targetNodes'])]
            }
            if (map.allNodes) {
                newmap.allNodes = [(BuilderUtil.pluralize('nodes')):convertNodeList(map['allNodes'])]
            }
            if (map.nodes) {
                def nodesteps = [:]
                newmap.remove('nodes').each{
                    nodesteps[(it.key)]= [(BuilderUtil.pluralize('steps')): it.value]
                }
                newmap[(BuilderUtil.pluralize('nodes'))] = convertNodeList(nodesteps.entrySet())
            }
            newmap
        }
        withFormat {
            json{
                return render(contentType: "application/json", encoding: "UTF-8",text:state.encodeAsJSON())
            }
            xml{
                return render(contentType: "text/xml", encoding: "UTF-8") {
                    result(success: "true", apiversion: ApiRequestFilters.API_CURRENT_VERSION) {
                        executionState(id:params.id){
                            new BuilderUtil().mapToDom(convertXml(state), delegate)
                        }
                    }
                }
            }
        }
    }

    /**
     * API: /api/execution/{id}/abort, version 1
     */
    def apiExecutionAbort={
        def Execution e = Execution.get(params.id)
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!e) {
            return apiService.renderErrorXml(response,
                        [
                                status: HttpServletResponse.SC_NOT_FOUND,
                                code: "api.error.item.doesnotexist",
                                args: ['Execution ID', params.id]
                        ])
        } else if (!frameworkService.authorizeProjectExecutionAll(authContext,e,[AuthConstants.ACTION_KILL])){
            return apiService.renderErrorXml(response,
                    [
                            status: HttpServletResponse.SC_FORBIDDEN,
                            code: "api.error.item.unauthorized",
                            args: [AuthConstants.ACTION_KILL, "Execution", params.id]
                    ])
        }
        def ScheduledExecution se = e.scheduledExecution
        def user=session.user
        def killas=null
        if (params.asUser && apiService.requireVersion(request,response,ApiRequestFilters.V5)) {
            //authorized within service call
            killas= params.asUser
        }
        def abortresult = executionService.abortExecution(se, e, user, authContext, killas)

        def reportstate=[status: abortresult.abortstate]
        if(abortresult.failedreason){
            reportstate.reason= abortresult.failedreason
        }
        apiService.renderSuccessXml(response) {
            success {
                message("Execution status: ${abortresult.statusStr ? abortresult.statusStr : abortresult.jobstate}")
            }
            abort(reportstate) {
                execution(id: params.id, status: abortresult.jobstate)
            }
        }
    }

    /**
     * API: /api/executions query interface, version 5
     */
    def apiExecutionsQuery = {ExecutionQuery query->
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V5)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(query?.hasErrors()){
            return apiService.renderErrorXml(response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: "api.error.parameter.error",
                            args: [query.errors.allErrors.collect { message(error: it) }.join("; ")]
                    ])
        }
        if (!params.project) {
            return apiService.renderErrorXml(response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: "api.error.parameter.required",
                            args: ['project']
                    ])
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
                return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST, code: 'api.error.history.date-format', args: ['begin', params.begin]])
            }
        }
        if (params.end) {
            try {
                query.endbeforeFilter = ReportsController.parseDate(params.end)
                query.doendbeforeFilter = true
            } catch (ParseException e) {
                return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST, code: 'api.error.history.date-format', args: ['end', params.end]])
            }
        }
        def resOffset = params.offset ? params.int('offset') : 0
        def resMax = params.max ? params.int('max') : 20
        def results = executionService.queryExecutions(query, resOffset, resMax)
        def result=results.result
        def total=results.total
        //filter query results to READ authorized executions
        def filtered = frameworkService.filterAuthorizedProjectExecutionsAll(authContext,result,[AuthConstants.ACTION_READ])


        return executionService.respondExecutionsXml(response,filtered,[total:total,offset:resOffset,max:resMax])
    }
}

    

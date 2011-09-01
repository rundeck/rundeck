import com.dtolabs.rundeck.core.common.Framework
import java.text.SimpleDateFormat
import grails.converters.JSON
import com.dtolabs.client.utils.Constants




/**
* ExecutionController
*/
class ExecutionController {

    FrameworkService frameworkService
    ExecutionService executionService
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
        def ScheduledExecution se = e.scheduledExecution

        def abortresult=executionService.abortExecution(se,e,session.user)

        def didcancel=abortresult.abortstate in [ABORT_ABORTED,ABORT_PENDING]
        withFormat{
            json{
                render(contentType:"text/json"){
                    delegate.cancelled(didcancel)
                    delegate.status(abortresult.statusStr?abortresult.statusStr:(didcancel?'killed':'failed'))
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
        def file = new File(e.outputfilepath)
        if (! file.exists()) {
            log.error("File not found: "+e.outputfilepath)

            return
        } else if (file.size() < 1) {
            log.error("File is empty")

            return
        }
        def offset = 0
        def initoffset=offset
        def lastoff=offset
        def lastmesgoff=offset
        def storeoffset=offset
        def entry=[]
        StringBuffer buf = new StringBuffer();
        def completed=false
        def lines=0
        def chars=0;
        def tot=file.length()
        //start at offset byte.
        RandomAccessFile raf = new RandomAccessFile(file,"r")
        def totsize = raf.getChannel().size()
        def size = raf.getChannel().size()
        def tstart=System.currentTimeMillis();
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

        def Map result = parseOutput(file,0,-1,null){ Map msgbuf ->
            response.outputStream << (isFormatted?"${msgbuf.time} [${msgbuf.user}@${msgbuf.node} ${msgbuf.context} ${msgbuf.command}][${msgbuf.level}] ${msgbuf.mesg}" : msgbuf.mesg)
            true
        }

        storeoffset=result.storeoffset
        completed = result.completed
    }
    /**
     * tailExecutionOutput action, used by execution/show.gsp view to display output inline
     */
    def tailExecutionOutput = {
        Execution e = Execution.get(Long.parseLong(params.id))
        if(!e){
             render(contentType:"text/json"){
                error("Execution with id "+params.id+" not found")
                id(params.id.toString())
                dataoffset("0")
                iscompleted(false)
                entries(){
                }
            }
            return;
        }
        def long start = System.currentTimeMillis()

        def jobcomplete = e.dateCompleted!=null
        def failednodes = e.failedNodeList?true:false
        def jobstat = e.status
        def jobcanc = e.cancelled
        def execDuration = 0
        if(null==e.outputfilepath){
            //execution has not be started yet
            render(contentType:"text/json"){
                delegate.message("Unmodified")
                delegate.id(params.id.toString())
                delegate.dataoffset(params.offset.toString())
                delegate.iscompleted(false)
                delegate.jobcompleted(jobcomplete)
                delegate.failednodes(failednodes)
                delegate.jobstatus(jobstat)
                delegate.jobcancelled(jobcanc)
                delegate.duration(execDuration)
                delegate.entries(){
                }
            }
            return;
        }
        def file = new File(e.outputfilepath)
        execDuration=(e.dateCompleted?e.dateCompleted.getTime():System.currentTimeMillis()) - e.dateStarted.getTime()

        if (! file.exists()) {
            log.error("File not found: "+e.outputfilepath)

            render(contentType:"text/json"){
                if(e.dateCompleted){
                    delegate.error("File not found: "+e.outputfilepath)
                }else{
                    delegate.message("File not found: "+e.outputfilepath)
                }
                delegate.id(params.id.toString())
                delegate.dataoffset("0")
                delegate.iscompleted(false)
                delegate.jobcompleted(jobcomplete)
                delegate.failednodes(failednodes)
                delegate.jobstatus(jobstat)
                delegate.jobcancelled(jobcanc)
                delegate.duration(execDuration)
                delegate.entries(){
                }
            }
            return
        } else if (file.size() < 1) {
            render(contentType:"text/json"){
                if(e.dateCompleted){
                    delegate.error("File is empty")
                }else{
                    delegate.message("File is empty")
                }
                delegate.id(params.id.toString())
                delegate.dataoffset("0")
                delegate.iscompleted(false)
                delegate.jobcompleted(jobcomplete)
                delegate.failednodes(failednodes)
                delegate.jobstatus(jobstat)
                delegate.jobcancelled(jobcanc)
                delegate.duration(execDuration)
                delegate.entries(){
                }
            }
            return
        }
        if(params.lastmod){
            def ll = Long.parseLong(params.lastmod);
            if(file.lastModified() <= ll){

                render(contentType:"text/json"){
                    delegate.message("Unmodified")
                    delegate.id(params.id.toString())
                    delegate.dataoffset(params.offset.toString())
                    delegate.iscompleted(false)
                    delegate.jobcompleted(jobcomplete)
                    delegate.failednodes(failednodes)
                    delegate.jobstatus(jobstat)
                    delegate.jobcancelled(jobcanc)
                    delegate.duration(execDuration)
                    delegate.entries(){
                    }
                }
            }
        }
        def Long offset = ((params.offset) ? Long.parseLong(params.offset) : 0)
        def storeoffset=offset
        def entry=[]
        def completed=false
        def totsize=file.length()
        def isfollowoption=false
        def lastlines=0
        def max=0
        if(params.lastlines){
            //load only the last X lines of the file, by going to the end and searching backwards for the
            //line-end textual format X times, then reset the offset to that point.
            //we actually search for X+1 line-ends to find the one prior to the Xth line back
            //and we add 1 more to account for the final ^^^END^^^\n line.
            //TODO: modify seekBack to allow rewind beyond the ^^^END^^^\n at the end prior to doing reverse search
            isfollowoption=true
            lastlines=Integer.parseInt(params.lastlines)
            def String lSep = System.getProperty("line.separator");
            def seekoffset = com.dtolabs.rundeck.core.utils.Utility.seekBack(file,lastlines+2,"^^^${lSep}")
            if(seekoffset>=0){
                //we found X+2 line ends.  now skip past the line-end at seekoffset, to get to the start
                //of the next line (Xth message).
                if(seekoffset>0){
                    seekoffset+="^^^${lSep}".length()
                }
                offset=seekoffset
                max=lastlines+1
            }
            

        }

        def String bufsizestr= servletContext.getAttribute("execution.follow.buffersize");
        def Long bufsize= (bufsizestr?bufsizestr.toInteger():0);
        if(bufsize<(25*1024)){
            bufsize=25*1024
        }
        def Map result = parseOutput(file,offset,bufsize,null){ data ->
            entry << data
            (0==max || entry.size()<max)
        }
        storeoffset=result.storeoffset
        completed = result.completed || (jobcomplete && storeoffset==totsize)
        
        if("true" == servletContext.getAttribute("output.markdown.enabled") && !params.disableMarkdown){
            entry.each{
                if(it.mesg){
                    try{
                        it.mesghtml = it.mesg.decodeMarkdown()
                    }catch (Exception exc){
                        log.error("Markdown error: "+exc.getMessage(),exc)
                    }
                }
            }
        }
        long marktime=System.currentTimeMillis()
        def lastmodl = file.lastModified()
        def percent=100.0 * (((float)storeoffset)/((float)totsize))
        render(contentType:"text/json"){
            delegate.id(e.id.toString())
            delegate.dataoffset(storeoffset.toString())
            delegate.iscompleted(completed)
            delegate.jobcompleted(jobcomplete)
            delegate.failednodes(failednodes)
            delegate.jobstatus(jobstat)
            delegate.jobcancelled(jobcanc)
            delegate.lastmod(lastmodl.toString())
            delegate.duration(execDuration)
            delegate.percentLoaded(percent)
            delegate.totalsize(totsize)
            delegate.entries(){
                entry.each{
                    if(it.mesghtml){
                        delegate.entries(time: it.time.toString(), level:it.level, mesghtml:it.mesghtml, user:it.user, module:it.module, command:it.command, node:it.node, context:it.context)
                    }else{
                        delegate.entries(time: it.time.toString(), level:it.level, mesg:it.mesg, user:it.user, module:it.module, command:it.command, node:it.node, context:it.context)
                    }
                }
            }
        }
    }
    /**
     * parseOutput parses the output file starting at the given offset and with the given buffersize
     * Calls the callback closure with a map of data each time an entry is completed.
     * @param file the file to parse
     * @param offset offset to start from
     * @param bufsize maximum amount of data to read if greater than 0, otherwise read until EOF
     * @param callback a closure that has 1 parameter, the map of data to read.  if it returns false, reading will stop
     * @return Map containing two keys, 'storeoffset': next offset to start at, if buffersize is defined, and 'completed': boolean value if the log file has been completely read
     */
     public Map parseOutput ( File file, Long offset, Long bufsize = -1 ,def String encoding=null, Closure callback){


        def initoffset=offset
        def lastoff=offset
        def lastmesgoff=offset
        def Long storeoffset=offset
        def entry=[]
        def msgbuf =[:]
        StringBuffer buf = new StringBuffer();
        def completed=false
        def lines=0
        def chars=0;
        def tot=file.length()
        //start at offset byte.

         //we use RandomAccessFile to call readLine and record the byte-oriented offset
        RandomAccessFile raf = new RandomAccessFile(file,'r')

         //we use InputStreamReader to read bytes to chars given encoding option
         //in this case readLine may buffer bytes for decoding purposes, so the offset is not correct
        final FileInputStream stream = new FileInputStream(file)
        InputStreamReader fr
         if(null!=encoding){
             fr= new InputStreamReader(stream,encoding)
         }else{
             //use default encoding
             fr= new InputStreamReader(stream)
         }

        def totsize = stream.getChannel().size()
        def size = stream.getChannel().size()
        if(offset>0){
            stream.getChannel().position(offset)
            raf.seek(offset)
        }
//        log.info("starting tailExecutionOutput: offset: "+offset+", completed: "+completed)
        def String lSep = System.getProperty("line.separator");
        def tstart=System.currentTimeMillis();
        def String line = fr.readLine()
        def oline=raf.readLine() //discarded content
        def diff = System.currentTimeMillis()-tstart;

        if(bufsize > 0  && size-offset > bufsize){
            size = offset+bufsize
        }
        boolean done=false
        while(offset<size && null != line  && !done){
//            log.info("readLine waited: "+diff);
            lastoff=offset
            offset=raf.getFilePointer()
            if(line =~ /^\^\^\^END\^\^\^/){
                completed=true;
                storeoffset=offset;
                if(msgbuf){
                    msgbuf.mesg+=buf.toString()
                    if(!callback(msgbuf)){
                        done=true
                    }
                    buf = new StringBuffer()
                    msgbuf=[:]
                }
            }
            if (line =~ /^\^\^\^/){
                if(msgbuf){
                    msgbuf.mesg+=buf.toString()
                    if(!callback(msgbuf)){
                        done=true
                    }
                    buf = new StringBuffer()
                    msgbuf=[:]
                }
                def temp = line.substring(3,line.length())
                def boolean full=false;
                if(temp =~ /\^\^\^$/ || temp == ""){
                    if(temp.length()>=3){
                        temp = temp.substring(0,temp.length()-3)
                    }
                    full=true
                }
                def arr = temp.split("\\|",ExecutionService.EXEC_FORMAT_SEQUENCE.size()+1)
                if (arr.size() >= 3) {
                    def time = arr[0].trim()
                    def level = arr[1].trim()
                    def mesg;
                    def list = []
                    list.addAll(Arrays.asList(arr))
                    if(list.size()>=ExecutionService.EXEC_FORMAT_SEQUENCE.size()){
                        if(list.size() > ExecutionService.EXEC_FORMAT_SEQUENCE.size()){
                            //join last sections into one message.
                            def sb = new StringBuffer()
                            sb.append(list.get(ExecutionService.EXEC_FORMAT_SEQUENCE.size()).trim())
                            for(def i=ExecutionService.EXEC_FORMAT_SEQUENCE.size()+1;i<list.size();i++){
                                sb.append('|')
                                sb.append(list.get(i).trim())
                            }
                            mesg = sb.toString()
                        }else{
                            mesg=''
                            list<<mesg
                        }
                    }else if(list.size()>3 && list.size()<ExecutionService.EXEC_FORMAT_SEQUENCE.size()){
                        def sb = new StringBuffer()
                        sb.append(list.get(2).trim())
                        for(def i=3;i<list.size();i++){
                            sb.append('|')
                            sb.append(list.get(i).trim())
                        }
                        mesg = sb.toString()
                    }else{
                        mesg = list[list.size()-1].trim()
                    }
                    msgbuf= [time:time, level:level, mesg:mesg+lSep]
                    if(list.size()>=ExecutionService.EXEC_FORMAT_SEQUENCE.size() ){
                        for(def i=2;i<list.size()-1;i++){
                            msgbuf[ExecutionService.EXEC_FORMAT_SEQUENCE[i]]=list[i].trim()
                        }
                    }
                    if(full){
                        if(!callback(msgbuf)){
                            done=true
                        }
                        msgbuf=[:]
                        buf=new StringBuffer()
                    }
                    lastmesgoff=lastoff
                }
                if(full){
                    storeoffset=offset
                }
            }else if(line=~/\^\^\^$/ && msgbuf){
                def temp = line.substring(0,line.length()-3)
                buf << temp + lSep
                msgbuf.mesg+=buf.toString()
                    if(!callback(msgbuf)){
                        done=true
                    }
                buf = new StringBuffer()
                msgbuf=[:]
                storeoffset=offset
            }else{
                buf << line + lSep
            }
            tstart=System.currentTimeMillis();
            line = fr.readLine()//kept
            oline=raf.readLine()//discarded
            diff=System.currentTimeMillis()-tstart;
        }
        fr.close()
        raf.close()
        if(msgbuf){
            //incomplete message entry.  We leave it until next time unless completed==true
            if(completed){
                msgbuf.mesg+=buf.toString()
                callback(msgbuf)
                buf = new StringBuffer()
            }
        }
        long parsetime=System.currentTimeMillis()
        return [storeoffset:storeoffset, completed:completed]
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
    public def renderApiExecutions= { execlist, delegate ->
        delegate.'executions'(count: execlist.size()) {
            execlist.each {Execution e ->
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
                        job(jobparams) {
                            name(e.scheduledExecution.jobName)
                            group(e.scheduledExecution.groupPath ?: '')
                            project(e.scheduledExecution.project)
                            description(e.scheduledExecution.description)
                        }
                    }
                    description(ExecutionService.summarizeJob(e.scheduledExecution, e))
                    argstring(e.argString)
                }
            }
        }
    }

    /**
     * Utility, render xml response for a list of executions
     */
    public def renderApiExecutionListResultXML={execlist ->
        return new ApiController().success(renderApiExecutions.curry(execlist))
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

        def abortresult=executionService.abortExecution(se,e,session.user)

        return new ApiController().success{ delegate->
            delegate.'success'{
                message("Execution status: ${abortresult.statusStr?abortresult.statusStr:abortresult.jobstate}")
            }
            delegate.'abort'(status:abortresult.abortstate){
                execution(id:params.id, status: abortresult.jobstate)
            }

        }
    }

}

    

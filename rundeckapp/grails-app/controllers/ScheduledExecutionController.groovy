import org.quartz.*
import java.text.ParseException
import com.dtolabs.rundeck.core.common.Framework
import java.text.SimpleDateFormat
import groovy.xml.QName
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.util.regex.Pattern
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.params.HttpClientParams
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement
import com.dtolabs.rundeck.core.utils.NodeSet
import groovy.xml.MarkupBuilder
import com.dtolabs.client.utils.Constants

class ScheduledExecutionController  {
    def Scheduler quartzScheduler
    def ExecutionService executionService
    def FrameworkService frameworkService
    def ScheduledExecutionService scheduledExecutionService

 
    def index = { redirect(controller:'menu',action:'list',params:params) }

    // the delete, save and update actions only
    // accept POST requests
    def static allowedMethods = [delete:'POST',
        save:'POST',
        update:'POST',
        deleteBulk:'DELETE'
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
            redirect(controller:'menu',action:'list')
        }
    }
    def list = {redirect(controller:'menu',action:'list',params:params) }

    def groupTreeFragment = {
        def tree = scheduledExecutionService.getGroupTree()
        render(template:"groupTree",model:[groupTree:tree,jscallback:params.jscallback])
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
    def show = {
        withFormat{
            html{
                redirect(controller:'menu',action:'jobs',params:[idlist:params.id])
            }
            xml{
                showx.call()
            }
        }
    }
    def showx = {
        log.info("ScheduledExecutionController: show : params: " + params)
        def crontab = [:]
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def ScheduledExecution scheduledExecution = ScheduledExecution.get( params.long('id') )
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

        def boolean objexists = false
        def boolean auth = false
        if(scheduledExecution.workflow){
            auth=user && user.authorization.workflow_run
        }else if (scheduledExecution.adhocExecution){
            auth = frameworkService.userAuthorizedForScript(session.user,scheduledExecution.project,scheduledExecution.adhocRemoteString?scheduledExecution.adhocRemoteString:scheduledExecution.adhocLocalString,framework)
        }


        withFormat{
            html{
                [scheduledExecution:scheduledExecution, crontab:crontab, params:params,
            executions:executions,
            objexists:objexists,
            authorized:auth,
            total:total,
            nextExecution:scheduledExecutionService.nextExecutionTime(scheduledExecution),
            max: params.max?params.max:10,
            offset:params.offset?params.offset:0]

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
        def ScheduledExecution scheduledExecution = ScheduledExecution.get( params.long('id') )
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
            if (opt.valuesUrl) {
                //load expand variables in URL source
                String srcUrl = expandUrl(opt, opt.valuesUrl.toExternalForm(), scheduledExecution)
                def result
                def err = [:]
                try {
                    result = getRemoteJSON(srcUrl, 10)
                } catch (Exception e) {
                    err.message = "Failed loading remote option values"
                    err.exception = e
                    err.srcUrl = srcUrl
                    log.error("getRemoteJSON error: URL ${srcUrl} : ${e.message}");
                }
                return render(template: "/framework/optionValuesSelect", model: [optionSelect: opt, values: result, srcUrl: srcUrl, err: err,fieldPrefix:params.fieldPrefix,selectedvalue:params.selectedvalue]);
            } else {
                return error.call()
            }
        }else{
            return error.call()
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
    String expandUrl(Option opt, String url, ScheduledExecution scheduledExecution) {
        def invalid = []
        String srcUrl = url.replaceAll(/(\$\{(job|option)\.(.+?)\})/,
            {Object[] group ->
                if(group[2]=='job' && jobprops[group[3]] && scheduledExecution.properties.containsKey(jobprops[group[3]])) {
                    scheduledExecution.properties.get(jobprops[group[3]]).toString().encodeAsURL()
                }else if(group[2]=='option' && optprops[group[3]] && opt.properties.containsKey(optprops[group[3]])) {
                    opt.properties.get(optprops[group[3]]).toString().encodeAsURL()
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
     * Make a remote URL request and return the parsed JSON data
     * @param url URL to request
     * @param timeout request timeout in seconds
     */
    def Object getRemoteJSON(String url, int timeout){
        //attempt to get the URL JSON data
        if(url.startsWith("http:") || url.startsWith("https:")){
            final HttpClientParams params = new HttpClientParams()
            params.setConnectionManagerTimeout(timeout*1000)
            params.setSoTimeout(timeout*1000)
            def HttpClient client= new HttpClient(params)
            def HttpMethod method = new GetMethod(url)
            method.setFollowRedirects(true)
            method.setRequestHeader("Accept","application/json")
            def resultCode = client.executeMethod(method);
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
                        return grails.converters.JSON.parse(new InputStreamReader(method.getResponseBodyAsStream(),method.getResponseCharSet()))
                    }else{
                        throw new Exception("Unexpected content type received: "+resultType)
                    }
                }else{
                    throw new Exception("Server returned an error response: ${resultCode}:${reasonCode}")
                }
            } finally {
                method.releaseConnection();
            }
        }else if (url.startsWith("file:")) {
            def File srfile = new File(new URI(url))
            final JSONElement parse = grails.converters.JSON.parse(new InputStreamReader(new FileInputStream(srfile)))
            if(!parse ){
                throw new Exception("JSON was empty")
            }
            return parse
        } else {
            throw new Exception("Unsupported protocol: " + url)
        }
    }

    /**
    */
    def delete = {
        log.info("ScheduledExecutionController: delete : params: " + params)
        def ScheduledExecution scheduledExecution = ScheduledExecution.get( params.id )
        if(scheduledExecution) {
            def jobname = scheduledExecution.generateJobScheduledName()
            def groupname = scheduledExecution.generateJobGroupName()
            def jobtitle=scheduledExecution.jobName
            //unlink any Execution records
            def torem=[]
            def execs = scheduledExecution.executions
            execs.each{Execution exec->
                torem<<exec
            }
            torem.each{Execution exec->
                scheduledExecution.removeFromExecutions(exec)
                exec.scheduledExecution=null
            }
            scheduledExecution.delete(flush:true)
            scheduledExecutionService.deleteJob(jobname,groupname)
            flash.message = "Job '${jobtitle}' was successfully deleted."
            redirect(controller:'menu',action:'list', params:[:])
        } else {
            flash.message = "ScheduledExecution not found with id ${params.id}"
            redirect(controller:'menu',action:'list', params:params)
        }
    }

    /**
     * Delete a set of jobs as specified in the idlist parameter.
     * Only allowed via DELETE http method
    */
    def deleteBulk = {
        log.info("ScheduledExecutionController: deleteBulk : params: " + params)
        def list=[]
        if(!params.idlist){
            flash.error = "idlist parameter is required"
            return error.call()
        }
        def ids=params.idlist.split(",")
        def errs=[]
        ids.each{
            def ScheduledExecution scheduledExecution = ScheduledExecution.get( it )
            if(scheduledExecution){
                list<<scheduledExecution
            }else{
                errs<<"No Job found with id ${it}."
            }
        }
        if(errs.size()){
            flash.error=errs.join("\n")
            return error.call()
        }
        def msgs=[]
        list.each{scheduledExecution->
            def jobname = scheduledExecution.generateJobScheduledName()
            def groupname = scheduledExecution.generateJobGroupName()
            def jobtitle=scheduledExecution.jobName
            if(params.deleteAffirm){
                scheduledExecution.delete()
                scheduledExecutionService.deleteJob(jobname,groupname)
            }
            msgs << "Job '${jobtitle}' was successfully deleted."
        }
        flash.message="Deleted ${list.size()} Jobs: [${params.idlist}]"
        flash.messages=msgs
        return xmlsuccess.call()

    }

    def edit = {
        log.info("ScheduledExecutionController: edit : params: " + params)
        def scheduledExecution = ScheduledExecution.get( params.id )
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def crontab = [:]
        if(!scheduledExecution) {
            flash.message = "ScheduledExecution not found with id ${params.id}"
            return redirect(controller:'menu',action:'list', params:params)
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
            authorized:scheduledExecutionService.userAuthorizedForJob(request,scheduledExecution,framework)]
    }

    def renderEditFragment = {
        render(template:'editForm', model:edit(params))
    }

    def update = {
        def result = _doupdate(params)
        def scheduledExecution=result[1]
        def success = result[0]
        if(!scheduledExecution){
            flash.message = "ScheduledExecution not found with id ${params.id}"
            log.info("update: there was no object by id: " +params.id+". redirecting to edit.")
            redirect(controller:'menu',action:'jobs')
        }else if (!success){
            log.debug scheduledExecution.errors.allErrors.collect {g.message(error: it)}.join(", ")
            request.message="Error updating scheduled command "
            log.debug("update operation failed. redirecting to edit ...")

            if(!scheduledExecution.isAttached()) {
                scheduledExecution.attach()
            }else{
                scheduledExecution.refresh()
            }
            render(view:'edit',model:[scheduledExecution:scheduledExecution,
                       nextExecutionTime:scheduledExecutionService.nextExecutionTime(scheduledExecution)],
                   params:[project:params.project])
        }else{
            flash.savedJob=scheduledExecution
            flash.savedJobMessage="Saved changes to Job"
            redirect(controller:'menu',action:'jobs')
        }
    }
    def _doupdate = { params ->
        log.debug("ScheduledExecutionController: update : attempting to update: "+params.id +
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
        def ScheduledExecution scheduledExecution = ScheduledExecution.get( params.id )

        def crontab = [:]
        if(!scheduledExecution) {
            return [false,null]
        }
        def oldjobname = scheduledExecution.generateJobScheduledName()
        def oldjobgroup = scheduledExecution.generateJobGroupName()
        def oldsched = scheduledExecution.scheduled
        def optparams = params.findAll { it.key.startsWith("command.option.")}
        def nonopts = params.findAll { !it.key.startsWith("command.option.") && it.key!='workflow' && it.key!='options'&& it.key!='notifications'}
        scheduledExecution.properties = nonopts
        
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
//                if(!scheduledExecution.user){
                scheduledExecution.user = user
                scheduledExecution.userRoles = rolelist
//                }else{/
                //TODO: determine rolelist for selected user
//                    if(params.user==user){
//                        scheduledExecution.userRoles=rolelist
//                    }else{
//                        scheduledExecution.userRoles=[]
//                    }
//                }
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
                wf.commands.each{cexec->
                    WorkflowController._validateCommandExec(cexec)
                    if(cexec.errors.hasErrors()){
                        wfitemfailed=true
                    }
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
                    scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.invalid.message')
                }

            }
        }else if(params.workflow && params['_workflow_data']){
            //use the input params to define the workflow
            //create workflow and CommandExecs
            def Workflow workflow = new Workflow(threadcount:params.workflow.threadcount?params.workflow.threadcount:1,keepgoing:null!=params.workflow.keepgoing?params.workflow.keepgoing:false,scheduledExecution:scheduledExecution)
            def i=0;
            def wfitemfailed=false
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
                }
                i++
            }
            scheduledExecution.workflow=workflow

            if(wfitemfailed){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.invalid.message')
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
        if( params['_sessionopts'] && session.editOPTS && session.editOPTS[scheduledExecution.id.toString()]){
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
                if (!theopt.validate()) {
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

        if(!params.notifications && (params.notifyOnsuccess || params.notifyOnfailure)){
            def nots=[exists:true]
            if('true'==params.notifyOnsuccess){
                nots['onsuccess']=[email:params.notifySuccessRecipients]
            }
            if('true'==params.notifyOnfailure){
                nots['onfailure']=[email:params.notifyFailureRecipients]
            }
            params.notifications=nots
        }
        def todiscard=[]
        if(params.notifications && scheduledExecution.notifications){
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
                    log.error("Unable to schedule job: ${scheduledExecution.id}: ${e.message}")
                }
                def newsched = ScheduledExecution.get(scheduledExecution.id)
                newsched.nextExecution=nextdate
                if(!newsched.save()){
                    log.error("Unable to save second change to scheduledExec.")
                }
            }else if(oldsched && oldjobname && oldjobgroup){
                scheduledExecutionService.deleteJob(oldjobname,oldjobgroup)
            }
            log.info("update : save operation succeeded. redirecting to show...")
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
        def user = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []
        log.info("ScheduledExecutionController: create : params: " + params)

        def ScheduledExecution scheduledExecution = ScheduledExecution.get( params.id )
        if(!scheduledExecution){
            flash.message = "ScheduledExecution not found with id ${params.id}"
            log.info("update: there was no object by id: " +params.id+". redirecting to menu.")
            redirect(controller:'menu',action:'list')
            return;
        }
        def newScheduledExecution = new ScheduledExecution()
        newScheduledExecution.properties = new java.util.HashMap(scheduledExecution.properties)
        newScheduledExecution.id=null
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
        render(view:'create',model: [ scheduledExecution:newScheduledExecution, crontab:crontab,params:params, iscopy:true, authorized:scheduledExecutionService.userAuthorizedForJob(request,scheduledExecution,framework)])

    }
    /**
     * action to populate the Create form with execution info from a previous (transient) execution
     */
    def createFromExecution={
        log.info("ScheduledExecutionController: create : params: " + params)
        Execution execution = Execution.get(params.executionId)
        if(!execution){
            flash.message = "Execution not found with id ${params.executionId}"
            redirect(controller:'execution',action:'follow',id:params.executionId)
        }
        def props=[:]
        props.putAll(execution.properties)
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
        if(params.workflow){
            //store workflow in session
            def wf=WorkflowController.getSessionWorkflow(session,params,params.workflow)
            session.editWFPassThru=true
        }

        def model=create.call()
        render(view:'create',model:model)
    }
    def create = {

        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def projects = frameworkService.projects(framework)
        session.projects=projects
        def user = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []
        log.info("ScheduledExecutionController: create : params: " + params)
        def scheduledExecution = new ScheduledExecution()
        scheduledExecution.loglevel = servletContext.getAttribute("LOGLEVEL_DEFAULT")?servletContext.getAttribute("LOGLEVEL_DEFAULT"):"WARN"
        scheduledExecution.properties = params

        scheduledExecution.jobName = (params.command) ? params.command + " Job" : ""
        def cal = java.util.Calendar.getInstance()
        scheduledExecution.minute = String.valueOf(cal.get(java.util.Calendar.MINUTE))
        scheduledExecution.hour = String.valueOf(cal.get(java.util.Calendar.HOUR_OF_DAY))
        scheduledExecution.user = user
        scheduledExecution.userRoles = rolelist
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

        log.info("ScheduledExecutionController: create : now returning model data to view...")
        return ['scheduledExecution':scheduledExecution,params:params,crontab:[:]]
    }


    def renderCreateFragment = {
        render(template:'createForm', model:create(params))
    }

    def saveAndExec = {
        log.info("ScheduledExecutionController: saveAndExec : params: " + params)
        def scheduledExecution = _dosave(params)
        if(scheduledExecution.id){
            params.id=scheduledExecution.id
            if(!scheduledExecution.scheduled){
                return redirect(action:executeNow,id:scheduledExecution.id)
            }else{
                return redirect(action:show,id:scheduledExecution.id)
            }
        }else{

            scheduledExecution.errors.allErrors.each { log.warn(it.defaultMessage) }
            flash.message=g.message(code:'ScheduledExecutionController.save.failed')
            return render(view:'create',model:[scheduledExecution:scheduledExecution,params:params])
        }
    }
    /**
     * Action to upload jobs.xml and execute it immediately.
     */
    def uploadAndExecute = {
        log.info("ScheduledExecutionController: uploadAndExecute " + params)

        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        params["user"] = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []

        if(!(request instanceof MultipartHttpServletRequest)){
            return
        }
        def file=request.getFile("xmlBatch")
        if(!file){
            flash.message="No file was uploaded."
            return
        }
        def results
        try{
            results= _parseJobsFile(file);
        }catch(Exception e){
            if(!params.xmlreq){
                render(view:'upload')
                return;
            }else{
                return xmlerror.call();
            }
        }
        if(!results){
            flash.error="No jobs definitions found"
            if(!params.xmlreq){
                render(view:'upload')
                return;
            }else{
                return xmlerror.call();
            }
        }
        //results is a collection of Maps, each defining a job.

        def jobs=[]
        def jobsi=[]
        def i=1
        def msgs = []
        def errjobs = []
        def skipjobs = []
        flash.errors=[]

        results.each{jobdata->
            def ScheduledExecution scheduledExecution

            def errmsg
            def failed
            try{
                def result= _dovalidate(jobdata)
                scheduledExecution=result.scheduledExecution
                failed=result.failed
                if(failed){
                    errmsg = scheduledExecution.errors?scheduledExecution.errors.allErrors.collect {err-> g.message(error:err)}.join("<br>"):'Failed to validate job definition'
                    log.error(errmsg)
                }
            }catch(Exception e){
                scheduledExecution=jobdata
                errmsg=e.getMessage()
                log.error(e.getMessage())
            }
            if(failed || errmsg){
                errjobs<<[scheduledExecution:scheduledExecution,entrynum:i,errmsg:errmsg]
                flash.errors<<"Job #${i}: "+errmsg
            }else{
                jobs<<scheduledExecution
                jobsi<<[scheduledExecution:scheduledExecution, entrynum:i]
            }
            i++
        }
        def reserrors=[]
        def ressuccess=[]
        if(errjobs){
            if(!params.xmlreq){
                return render(view:'upload',model:[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true])
            }else{
                return xmlerror.call();
            }
        }
        //run the jobs and forward to nowrunning
        jobsi.each{ Map map->
            def ScheduledExecution scheduledExecution=map.scheduledExecution
            def entrynum=map.entrynum
            def properties=[:]
            properties.putAll(scheduledExecution.properties)
            properties.user=params.user
            properties.request = request
            def execresults = _transientExecute(scheduledExecution,properties,framework,rolelist)
//            System.err.println("transient execute result: ${execresults}");
            execresults.entrynum=entrynum
            if(execresults.error){
                reserrors<<execresults
            } else {
                ressuccess<<execresults
            }
        }
        

        if(!params.xmlreq){
            return render(view:'upload', model:[execerrors:reserrors,execsuccess:ressuccess, errjobs: errjobs, messages: msgs, didupload: true])
        }else{
            //TODO: update jobs upload task to submit XML content directly instead of via uploaded file, and use proper
            //TODO: grails content negotiation

//            response.setHeader("Content-Disposition","attachment; filename=\"upload-result.xml\"")
            response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,"Jobs XML Uploaded. Succeeded: ${ressuccess.size()}, Failed: ${reserrors.size()}")
                render(contentType:"text/xml"){
                    result{
                        succeeded(count:ressuccess.size()){
                            ressuccess.each{ Map job ->
                                delegate.'execution'(index:job.entrynum){
                                    id(job.id.toString())
                                    name(job.execution.toString())
                                    url(g.createLink(controller:'execution',action:'follow',id:job.id))
                                }
                            }
                        }
                        failed(count:reserrors.size()){

                            reserrors.each{ Map job ->
                                delegate.'execution'(index:job.entrynum){
                                    delegate.'error'(job.error.toString())
                                    delegate.'message'(job.message.toString())
                                }
                            }
                        }
                    }
                }
        }

    }
    /**
     * Parse an uploaded file and return the Collection of jobs data as parsed.
     * @throws Exception if an error occurs, and sets the flash.error message
     */
    def _parseJobsFile={file->

        def XmlSlurper parser = new XmlSlurper()
        def  doc
        try{
            doc = parser.parse(file.getInputStream())
        }catch(Exception e){
            flash.error="Unable to parse file: ${e}"
            throw e
        }
        if(!doc){
            final String errmsg = "XML Document could not be parsed."
            flash.error=errmsg
            throw new Exception(errmsg)
        }

        if(!doc.job){
            final String errmsg = "Jobs XML Document was not valid: 'job' element not found."
            flash.error=errmsg
            throw new Exception(errmsg)
        }
        def jobset
        try{
            jobset = doc.decodeJobsXML()
        }catch (JobXMLException e){
            final String errmsg = "Jobs XML Document was not valid: ${e}"
            flash.error=errmsg
            throw new Exception(errmsg)
        }
        if(null==jobset){
            final String errmsg = "Jobs XML Document was not valid"
            flash.error=errmsg
            throw new Exception(errmsg)
        }

        return jobset
    }
    /**
     * execute the job defined via input parameters, but do not store it.
     */
    def execAndForget = {
        log.info("ScheduledExecutionController: execAndForget : params: " + params)


        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        params["user"] = (session?.user) ? session.user : "anonymous"
        params.request = request
        def rolelist = (session?.roles) ? session.roles : []
        params.jobName='Temporary_Job'
        params.groupPath='adhoc'
        def result= _dovalidate(params)
        def ScheduledExecution scheduledExecution=result.scheduledExecution
        def failed=result.failed
        if(!failed){
            def results = _transientExecute(scheduledExecution,params,framework,rolelist)
            if(results.error){
                if(results.error=='unauthorized'){
                    log.error(results.message)
                    flash.error=results.message
                    render(view:"/common/execUnauthorized",model:[scheduledExecution:scheduledExecution])
                    return
                }else{
                    log.error(results.message)
                    flash.error=results.message
                    render(view:"/common/error",model:[error:results.message])
                    return
                }
            } else {
                log.info("ExecutionController: immediate execution scheduled (${results.id})")
                redirect(controller:"execution", action:"follow",id:results.id)
            }
        }else{
            scheduledExecution.jobName = ''
            scheduledExecution.errors.allErrors.each { log.warn(it.defaultMessage) }
            flash.message="Job configuration was incorrect."
            render(view:'create',model:[scheduledExecution:scheduledExecution,params:params])
        }
    }
    
    /**
    * Execute a transient ScheduledExecution and return execution data: [execution:Execution,id:Long]
     * if there is an error, return [error:'type',message:errormesg,...]
     */
    def _transientExecute={ScheduledExecution scheduledExecution, Map params, Framework framework, List rolelist->
        def object
        def isauth = scheduledExecutionService.userAuthorizedForJob(params.request,scheduledExecution,framework)
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

        def eid = scheduledExecutionService.scheduleTempJob(params.user,rolelist,params,e);
        return [execution:e,id:eid]
    }

    def _dovalidate = { Map params ->
        log.debug("ScheduledExecutionController: save : params: " + params)
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def user = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []
        boolean failed=false;
        def scheduledExecution = new ScheduledExecution()
        def optparams = params.findAll {it.key.startsWith("command.option.")}
        final Map nonopts = params.findAll {!it.key.startsWith("command.option.") && it.key != 'workflow'&& it.key != 'options'&& it.key != 'notifications'}
        final Map oldopts = params.findAll{it.key=~/^(name|command|type|adhocExecution|adhocFilepath|adhoc.*String)$/}
        scheduledExecution.properties = nonopts
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
//            if(!scheduledExecution.user){
                scheduledExecution.user = user
                scheduledExecution.userRoles = rolelist
//            }else{
                //TODO: allow other users name and determine rolelist for selected user
//            }

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

        if(!frameworkService.existsFrameworkProject(scheduledExecution.project,framework)){
            failed=true
            scheduledExecution.errors.rejectValue('project','scheduledExecution.project.invalid.message',[scheduledExecution.project].toArray(),'Project was not found: {0}')
        }
        if(params['_sessionwf']=='true' && session.editWF && session.editWF['_new']){
            //use session-stored workflow
            def Workflow wf = session.editWF['_new']
            if(!wf.commands || wf.commands.size()<1){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.empty.message')
            }else{

                def wfitemfailed=false
                wf.commands.each{cexec->
                    WorkflowController._validateCommandExec(cexec)
                    if(cexec.errors.hasErrors()){
                        wfitemfailed=true
                    }
                }
                if(!wfitemfailed){
                    final Workflow workflow = new Workflow(wf)
                    scheduledExecution.workflow=workflow
                    wf.discard()
                }else{
                    failed=true
                    scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.invalid.message')
                }
            }
        }else if (params.workflow){
            //use input parameters to define workflow
            //create workflow and CommandExecs
            def Workflow workflow = new Workflow(threadcount:params.workflow.threadcount,keepgoing:params.workflow.keepgoing,scheduledExecution:scheduledExecution)
            def i=0;
            def wfitemfailed=false
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
                }
                i++
            }
            scheduledExecution.workflow=workflow

            if(wfitemfailed){
                failed=true
                scheduledExecution.errors.rejectValue('workflow','scheduledExecution.workflow.invalid.message')
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
            while(params.options["options[${i}]"]){
                def Map optdefparams=params.options["options[${i}]"]
                def Option theopt = new Option(optdefparams)
                scheduledExecution.addToOptions(theopt)
                if (!theopt.validate()) {
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
        if(!params.notifications && (params.notifyOnsuccess || params.notifyOnfailure)){
            def nots=[:]
            if('true'==params.notifyOnsuccess){
                nots['onsuccess']=[email:params.notifySuccessRecipients]
            }
            if('true'==params.notifyOnfailure){
                nots['onfailure']=[email:params.notifyFailureRecipients]
            }
            params.notifications=nots
        }
        if(params.notifications){
            //create notifications
            def i=0;
            failed=_updateNotifications(params, scheduledExecution)
        }
        if(scheduledExecution.doNodedispatch){
            if(!scheduledExecution.nodeInclude
                && !scheduledExecution.nodeExclude
             && !scheduledExecution.nodeIncludeName
                && !scheduledExecution.nodeExcludeName
             && !scheduledExecution.nodeIncludeType
                && !scheduledExecution.nodeExcludeType
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
    def _dosave = { params ->
        log.info("ScheduledExecutionController: save : params: " + params)
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
            if(params.name){
                params.jobName=params.name+" - "+params.command+" Job"
            }else if(params.command){
                params.jobName=params.command+" Job"
            }else if(params.adhocRemoteString){
                params.jobName="Remote Script Job"
            }else if(params.adhocLocalString){
                params.jobName="Inline Script Job"
            }
        }
        def result= _dovalidate(params)
        def scheduledExecution=result.scheduledExecution
        failed=result.failed
        //try to save workflow
        if(!failed && null!=scheduledExecution.workflow){
            if(!scheduledExecution.workflow.save(flush:true)){
                log.error(scheduledExecution.workflow.errors.allErrors.collect{g.message(error:it)}.join("\n"))
                failed=true;
            }
        }
        if(!failed && scheduledExecution.save(true)){
            if(scheduledExecution.scheduled){
                def nextdate=null
                try{
                    nextdate=scheduledExecutionService.scheduleJob(scheduledExecution,null,null);
                }catch (SchedulerException e){
                    log.error("Unable to schedule job: ${scheduledExecution.id}: ${e.message}")
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
            return scheduledExecution

        } else {
            scheduledExecution.discard()
            return scheduledExecution
        }
    }

    def save = {
        def scheduledExecution = _dosave(params)
        if(scheduledExecution.id){
            flash.savedJob=scheduledExecution
            flash.savedJobMessage="Created new Job"
            redirect(controller:'menu',action:'jobs')
        }else{
            scheduledExecution.errors.allErrors.each { log.warn(it.defaultMessage) }
            request.message=g.message(code:'ScheduledExecutionController.save.failed')
            render(view:'create',model:[scheduledExecution:scheduledExecution,params:params])
        }
    }
    def upload ={
        log.info("ScheduledExecutionController: upload " + params)
        if(!(request instanceof MultipartHttpServletRequest)){
            return
        }
        def file = request.getFile("xmlBatch")

        if(!file){
            flash.message="No file was uploaded."
            return
        }
        def XmlSlurper parser = new XmlSlurper()
        def  doc
        try{
            doc = parser.parse(file.getInputStream())
        }catch(Exception e){
            flash.message="Unable to parse file: ${e}"
            flash.error="Unable to parse file: ${e}"
            if(!params.xmlreq){
                render(view:'upload')
                return;
            }else{
                return xmlerror.call();
            }
        }
        if(!doc){
            flash.message="XML Document could not be parsed."
            render(view:'upload')
            return;
        }

        if(!doc.job){
            flash.error="Jobs XML Document was not valid: 'job' element not found."
            flash.message="Jobs XML Document was not valid: 'job' element not found."
            if(!params.xmlreq){
                render(view:'upload')
                return;
            }else{
                return xmlerror.call();
            }
        }
        def jobset
        try{
            jobset = doc.decodeJobsXML()
        }catch (JobXMLException e){
            flash.error="Jobs XML Document was not valid: ${e}"
            flash.message="Jobs XML Document was not valid: ${e}"
            if(!params.xmlreq){
                render(view:'upload')
                return;
            }else{
                return xmlerror.call();
            }
        }
        if(null==jobset){
            flash.error="Jobs XML Document was not valid"
            flash.message="Jobs XML Document was not valid"
            if(!params.xmlreq){
                render(view:'upload')
                return;
            }else{
                return xmlerror.call();
            }

        }

        def jobs=[]
        def jobsi=[]
        def i=1
        def msgs = []
        def errjobs = []
        def skipjobs = []
        jobset.each{ jobdata ->
            log.debug("saving job data: ${jobdata}")
            def ScheduledExecution scheduledExecution
            if(params.dupeOption=="update" || params.dupeOption=="skip"){
                //look for dupe by name and group path
                def sched = ScheduledExecution.findByJobNameAndGroupPath(jobdata.jobName,jobdata.groupPath)
                if(sched){
                    scheduledExecution=sched
                }
            }
            if(params.dupeOption == "skip" && scheduledExecution){
                jobdata.id=scheduledExecution.id
                jobdata.origDescription=scheduledExecution.description
                skipjobs <<[scheduledExecution:jobdata,entrynum:i,errmsg:"A Job named '${jobdata.jobName}' already exists"]
            }
            else if(params.dupeOption == "update" && scheduledExecution){
                jobdata.id=scheduledExecution.id
                def success=false
                def errmsg
                try{
                    def result = _doupdate(jobdata)

                    success = result[0]
                    scheduledExecution=result[1]
                    if(!success && scheduledExecution && scheduledExecution.hasErrors()){
                        errmsg=scheduledExecution.errors.allErrors.collect {g.message(error: it)}.join("\n")
                    }
                }catch(Exception e){
                    errmsg=e.getMessage()
                }
                if(!success){
                    errjobs<<[scheduledExecution:scheduledExecution,entrynum:i,errmsg:errmsg]
                }else{
                    jobs<<scheduledExecution
                    jobsi<<[scheduledExecution:scheduledExecution, entrynum:i]
                }
            }else if(params.dupeOptions=="create" || !scheduledExecution){
                def errmsg
                try{
                    scheduledExecution = _dosave(jobdata)
                    if(scheduledExecution && scheduledExecution.hasErrors()){
                        errmsg=scheduledExecution.errors.allErrors.collect {g.message(error: it)}.join("\n")
                    }
                }catch(Exception e){
                    System.err.println("caught exception");
                    e.printStackTrace()
                    scheduledExecution=jobdata
                    errmsg=e.getMessage()
                }
                if(!scheduledExecution.id){
                    errjobs<<[scheduledExecution:scheduledExecution,entrynum:i,errmsg:errmsg]
                }else{
                    jobs<<scheduledExecution
                    jobsi<<[scheduledExecution:scheduledExecution, entrynum:i]
                }
            }


            i++


        }
        if(!params.xmlreq){
            return [jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        }else{
            //TODO: update commander's jobs upload task to submit XML content directly instead of via uploaded file, and use proper
            //TODO: grails content negotiation
            response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,"Jobs XML Uploaded. Succeeded: ${jobs.size()}, Failed: ${errjobs.size()}, Skipped: ${skipjobs.size()}")
                render(contentType:"text/xml"){
                    result(error:false){
                        succeeded(count:jobs.size()){
                            jobsi.each{ Map job ->
                                delegate.'job'(index:job.entrynum){
                                    id(job.scheduledExecution.id.toString())
                                    name(job.scheduledExecution.jobName)
                                    url(g.createLink(action:'show',id:job.scheduledExecution.id))
                                }
                            }
                        }
                        failed(count:errjobs.size()){

                            errjobs.each{ Map job ->
                                delegate.'job'(index:job.entrynum){
                                    if(job.scheduledExecution.id){
                                        id(job.scheduledExecution.id.toString())
                                        url(g.createLink(action:'show',id:job.scheduledExecution.id))
                                    }
                                    name(job.scheduledExecution.jobName)
                                    StringBuffer sb = new StringBuffer()
                                    job.scheduledExecution.errors.allErrors.each{err->
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
                        skipped(count:skipjobs.size()){

                            skipjobs.each{ Map job ->
                                delegate.'job'(index:job.entrynum){
                                    if(job.scheduledExecution.id){
                                        id(job.scheduledExecution.id.toString())
                                        url(g.createLink(action:'show',id:job.scheduledExecution.id))
                                    }
                                    name(job.scheduledExecution.jobName)
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
                }
        }
    }


    def execute = {

        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def model=edit(params)

        def ScheduledExecution scheduledExecution = model.scheduledExecution

        //test nodeset to make sure there are matches
        if(scheduledExecution.doNodedispatch){
            NodeSet nset = ExecutionService.filtersAsNodeSet(scheduledExecution)
            def project=frameworkService.getFrameworkProject(scheduledExecution.project,framework)
            def nodes=project.getNodes().filterNodes(nset)
            if(!nodes || nodes.size()<1){
                //error
                model.nodesetempty=true
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
        model
    }
    def executeInline = {
        def model = execute()
        render(template:'execOptionsForm',model:model)
    }

    def runJobByName = {
        //lookup job
        if(!params.jobName && !params.id){
            flash.error="jobName or id is required"
            response.setStatus (404)
            return error()
        }
        def jobs
        if(params.id){
            final def get = ScheduledExecution.get(params.id)
            if(!get){
                log.error("No Job found for id: " + params.id)
                flash.error="No Job found for id: " + params.id
                response.setStatus (404)
                return error()
            }
            jobs = [get]
        }else if (params.groupPath) {
            jobs = ScheduledExecution.findAllByJobNameAndGroupPath(params.jobName, params.groupPath)
        }else{
            jobs = ScheduledExecution.findAllByJobName(params.jobName)
        }
        if(!jobs || jobs.size()<1 || jobs.size()>1){
            flash.error="No unique job matched the input: ${params.jobName}, ${params.groupPath}. found (${jobs.size()})"
            response.setStatus (404)
            return error()
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        params["user"] = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []
        def scheduledExecution = jobs[0]
        def result = executeScheduledExecution(scheduledExecution,framework,rolelist,params)
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
    def runJobNow = {
        return executeNow()
    }
    def executeNow = {
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        params["user"] = (session?.user) ? session.user : "anonymous"
        def rolelist = (session?.roles) ? session.roles : []
        def ScheduledExecution scheduledExecution = ScheduledExecution.get( params.id )
        if (!scheduledExecution) {
            log.error("No Job found for id: " + params.id)
            response.setStatus (404)
            return render(template:"/common/error",model:[error:"No Job found for id: " + params.id])
        }
        def result = executeScheduledExecution(scheduledExecution,framework,rolelist,params)
        if(result.error){
            log.error(result.message)
            if(result.error=='unauthorized'){
                return render(view:"/common/execUnauthorized",model:[scheduledExecution:scheduledExecution,error:result.message,context:[scheduledExecution.project]])
            }else if(result.error=='invalid'){
                def model=execute.call()
                model.jobexecOptionErrors=result.errors
                model.selectedoptsmap=result.options
                return render(view:'execute',model:model)
            }else{
                return render(template:"/common/error",model:[error:result.message])
            }
        }else{

            log.info("ExecutionController: immediate execution scheduled")
            redirect(controller:"execution", action:"follow",id:result.executionId)
        }
    }
    def executeScheduledExecution = {ScheduledExecution scheduledExecution, Framework framework, List rolelist,params->
        def User user = User.findByLogin(params.user)
        if(!user || !user.authorization.workflow_run){
            def msg = g.message(code:'unauthorized.job.run.user',args:[params.user])
            log.error(msg)
            flash.error=msg
            return [error:'unauthorized',message:msg]
        }
        if(scheduledExecution.adhocExecution){
            if(! frameworkService.userAuthorizedForScript(params.user,
                scheduledExecution.project,
                scheduledExecution.adhocRemoteString?scheduledExecution.adhocRemoteString:scheduledExecution.adhocLocalString,
                framework)){
                def msg = g.message(code:'unauthorized.job.run.script',args:[params.user,scheduledExecution.project])
                log.error(msg)
                flash.error=msg
                return [error:'unauthorized',message:msg]
            }
        }

        def extra = [:]

        params.each{ key, value ->
            def matcher= key =~ /^extra\.(.*)$/
            if(matcher.matches()){
                extra[matcher.group(1)]=value
            }
        }
        def Execution e
        def eid
        try{
            e= executionService.createExecution(scheduledExecution,framework,params.user,extra)
            eid=scheduledExecutionService.scheduleTempJob(scheduledExecution,params.user,rolelist,e);
            return [executionId:eid,name:scheduledExecution.jobName]
        }catch(ExecutionServiceValidationException exc){
            return [error:'invalid',message:exc.getMessage(),options:exc.getOptions(),errors:exc.getErrors()]
        }catch(ExecutionServiceException exc){
            def msg = exc.getMessage()
            log.error("exception: "+exc)
            return [error:'failed',message:msg]
        }
    }

    def Map lookupLastExecutions(List scheduledExecutions) {
        def map = [ : ]
        log.info("looking up lastExecutions for ["+scheduledExecutions.size()+ "] objects")
        scheduledExecutions.each {             
            def last = lookupLastExecutions(it)
            log.info("lookupLastExecutions : found ["+last.size()+"] executions for id: "+it.id )
            if (last.size() > 0) {
                map[it.id] = last[0]
            } 
        }
        return map
    }

    def lookupLastExecutions(ScheduledExecution se) {
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
        log.info("lookupLastExecutions: results count " + results.count())
        results.each {
            log.info("Execution added to result: " + it)
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

package rundeck.services
import com.dtolabs.rundeck.app.support.BuilderUtil
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.util.XmlParserUtil
import com.dtolabs.rundeck.util.ZipBuilder
import com.dtolabs.rundeck.util.ZipReader
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalNotification
import grails.async.Promises
import groovy.xml.MarkupBuilder
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.InitializingBean
import com.dtolabs.rundeck.core.common.IRundeckProject
import org.springframework.transaction.TransactionStatus
import rundeck.BaseReport
import rundeck.ExecReport
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.codecs.JobsXMLCodec
import rundeck.controllers.JobXMLException

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectService implements InitializingBean{
    def grailsApplication
    def scheduledExecutionService
    def executionService
    def loggingService
    def logFileStorageService
    def workflowService
    static transactional = false

    private exportJob(ScheduledExecution job, Writer writer)
        throws ProjectServiceException {
        //convert map to xml
        def xml = new MarkupBuilder(writer)
        JobsXMLCodec.encodeWithBuilder([job], xml)
    }

    def exportHistoryReport(ZipBuilder zip, BaseReport report, String name) throws ProjectServiceException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        def dateConvert = {
            sdf.format(it)
        }
        BuilderUtil builder = new BuilderUtil(converters:[(Date): dateConvert, (java.sql.Timestamp): dateConvert])
        def map = report.toMap()
        if(map.jcJobId){
            //convert internal job ID to extid
            def se
            try{
                se = ScheduledExecution.get(Long.parseLong(map.jcJobId))
                if(se){
                    map.jcJobId=se.extid
                }
            }catch(NumberFormatException e){

            }
        }
        //convert map to xml
        zip.file("$name"){ Writer writer ->
            def xml = new MarkupBuilder(writer)
            builder.objToDom("report", map, xml)
        }
    }

    def exportExecution(ZipBuilder zip, Execution exec, String name) throws ProjectServiceException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        def dateConvert = {
            sdf.format(it)
        }
        BuilderUtil builder = new BuilderUtil()
        builder.converters= [(Date): dateConvert, (java.sql.Timestamp): dateConvert]

        def map = exec.toMap()
        BuilderUtil.makeAttribute(map, 'id')
        def File outfile = loggingService.getLogFileForExecution(exec)
        if (outfile && outfile.isFile()) {
            //change entry to point to local file
            map.outputfilepath = "output-${exec.id}.rdlog"
        }
        JobsXMLCodec.convertWorkflowMapForBuilder(map.workflow)
        //convert map to xml
        zip.file("$name") { Writer writer ->
            def xml = new MarkupBuilder(writer)
            builder.objToDom("executions", [execution:map], xml)
        }
        if (outfile && outfile.isFile()) {
            zip.file "output-${exec.id}.rdlog", outfile
        }
        def File statefile = workflowService.getStateFileForExecution(exec)
        if (statefile && statefile.isFile()) {
            zip.file "state-${exec.id}.state.json", statefile
        }
    }

    /**
     * Parse XML and return a ExecReport/BaseReport object
     * @param xmlinput xml source
     * @param execIdMap map of old execution IDs to new Ids
     * @param jobsByOldIdMap map of old Job IDs to new Job entries
     * @return Report object with remapped exec/job ID values
     * @throws ProjectServiceException
     */
    def loadHistoryReport(xmlinput, Map execIdMap=null, Map jobsByOldIdMap =null, identity=null) throws ProjectServiceException {
        Node doc = parseXml(xmlinput)
        if (!doc) {
            throw new ProjectServiceException("XML Document could not be parsed.")
        }
        if (doc.name() != 'report') {
            throw new ProjectServiceException("Document root tag was not 'executions': '${doc.name()}'")
        }

        //load doc as report
        def object = XmlParserUtil.toObject(doc)
        if (object instanceof Map) {
            //remap job id if necessary
            if (object.jcJobId && jobsByOldIdMap && jobsByOldIdMap[object.jcJobId]) {
                object.jcJobId= jobsByOldIdMap[object.jcJobId].id
            }
            //remap exec id if necessary
            if (object.jcExecId && execIdMap && execIdMap[object.jcExecId]) {
                object.jcExecId= execIdMap[object.jcExecId]
            }else {
                //skip report for exec id that cannot be found
                return null
            }
            //convert dates
            convertStringsToDates(object, ['dateStarted', 'dateCompleted'],"Report ${identity}")
            if (!(object.dateCompleted instanceof Date)) {
                object.dateCompleted = new Date()
            }
            def report
            try {
                report = ExecReport.fromMap(object)
            } catch (Throwable e) {
                throw new ProjectServiceException("Unable to create Report: " + e.getMessage(), e)
            }
            return report
        } else {
            throw new ProjectServiceException("Unexpected data type for Report: " + object.class.name)
        }
    }

    private void convertStringsToDates(object, final ArrayList<String> properties, identity=null) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        properties.each {dname ->
            def dobj = object[dname]
            if (dobj && dobj instanceof String) {
                try {
                    object[dname] = sdf.parse(dobj)
                } catch (ParseException e) {
                    throw new ProjectServiceException("Unable to parse date in W3C format ('${dname}' for ${identity})", e)
                }
            } else if (dobj && !(dobj instanceof Date)) {
                throw new ProjectServiceException("Expected a date format: ('${dname}' for ${identity})")
            }
        }
    }
    /**
     * Parse Execution objects from an XML file, and return the set of parsed executions
     * @param xmlfile input file
     * @param jobIdMap map of UUID/ScheduledExecution IDs to new UUIDs for reassigning execution to jobs
     * @param skipJobIds list of UUID/job id to skip execution import
     * @return map data: 'executions' list of Executions that were parsed, 'execidmap' map of new Executions to the
     * input IDs from the XML, 'retryidmap' map of new Executions to old the 'retry' execution ID
     * @throws ProjectServiceException if an error occurs
     */
    def loadExecutions(xmlinput,Map jobIdMap=null, skipJobIds = []) throws ProjectServiceException {
        Node doc = parseXml(xmlinput)
        if (!doc) {
            throw new ProjectServiceException("XML Document could not be parsed.")
        }
        if (doc.name() != 'executions') {
            throw new ProjectServiceException("Document root tag was not 'executions': '${doc.name()}'")
        }
        if (!doc.execution || doc.execution.size() < 1) {
            throw new ProjectServiceException("No 'executions/execution' element was found")
        }

        def execlist=[]
        def execidmap=[:]
        def retryidmap=[:]
        def ecount=0
        doc.execution.each{ enode->
            def object = XmlParserUtil.toObject(enode,false)
            if (object instanceof Map) {
                JobsXMLCodec.convertXmlWorkflowToMap(object.workflow)
                //remap job id if necessary
                def se=null
                if(object.jobId && jobIdMap && jobIdMap[object.jobId]){
                    se=scheduledExecutionService.getByIDorUUID(jobIdMap[object.jobId])
                }else if(object.jobId && skipJobIds && skipJobIds.contains(object.jobId)){
                    log.debug("Execution skipped ${object.id} for job ${object.jobId}")
                    return
                }
                if(object.id){
                    object.id=XmlParserUtil.stringToInt(object.id,-1)
                }
                //convert dates
                convertStringsToDates(object, ['dateStarted', 'dateCompleted'], "Execution($ecount) ID ${object.id}")
                if(!(object.dateCompleted instanceof Date)){
                    object.dateCompleted=new Date()
                    object.status='false'
                    object.cancelled=true
                    object.abortedby='system'
                }
                def retryExecId= XmlParserUtil.stringToInt(object.remove('retryExecutionId'),0)
                try {
                    def newexec = Execution.fromMap(object, se)
                    execidmap[newexec]=object.id
                    if(retryExecId){
                        retryidmap[newexec]=retryExecId
                    }
                    execlist << newexec
                } catch (Throwable e) {
                    throw new ProjectServiceException("Unable to create Execution($ecount): " + e.getMessage(), e)
                }
                ecount++
            } else {
                throw new ProjectServiceException("Unexpected data type for Execution($ecount) in file (${xmlfile}): " + object.class.name)
            }
        }
        [executions:execlist,execidmap:execidmap, retryidmap: retryidmap]
    }

    private Node parseXml(xmlinput) {
        def XmlParser parser = new XmlParser()
        def doc
        def reader
        if (xmlinput instanceof File || xmlinput instanceof InputStream) {
            reader = xmlinput
        } else if (xmlinput instanceof String) {
            reader = new StringReader(xmlinput)
        } else {
            throw new IllegalArgumentException("Unexpected input: ${xmlinput}")
        }

        try {
            doc = parser.parse(reader)
        } catch (Exception e) {
            throw new ProjectServiceException("Unable to parse xml: ${e}")
        }
        doc
    }

    /**
     * Cache of async project export promises
     */
    def Cache<String, ArchiveRequest> asyncExportResults
    /**
     * retains the requests until they can be left to expire in the cache
     */
    def Map<String, ArchiveRequest> asyncExportRequests

    @Override
    void afterPropertiesSet() throws Exception {

        asyncExportRequests= Collections.synchronizedMap(new HashMap<String,ArchiveRequest>())

        def spec=grailsApplication.config.rundeck?.projectService?.projectExportCache?.spec?: "expireAfterAccess=30m"

        asyncExportResults=
                CacheBuilder.
                        from(spec).
                        removalListener({ RemovalNotification<String,ArchiveRequest> notification->
                        //when cached item is removed, delete the file if the requests map is not retaining it
                            if(!asyncExportRequests.containsKey(notification.key)){
                                log.debug("Cache expired for project archive request ${notification.key}, deleting file: ${notification.value.file}")
                                notification.value.file?.delete()
                            }
                        } ).
                        build({
                         //load the request via the requests map, otherwise it does not exist
                            def request = asyncExportRequests.get(it);
                            if (null == request) {
                                throw new Exception("Invalid key: ${it}")
                            }
                            request
                        } )
    }

    /**
     * Begin asynchronous export request, return a token String to identify
     * @param project project
     * @param framework framework
     * @param ident username or identify of requestor
     * @return token string to identify the new request
     */
    def exportProjectToFileAsync(IRundeckProject project, Framework framework, String ident){
        final def summary=new ArchiveRequestProgress()
        final String token = UUID.randomUUID().toString()
        final def request=new ArchiveRequest(summary:summary,token:token)

        Promises.<File>task {
            ScheduledExecution.withNewSession {
                exportProjectToFile(project,framework,summary)
            }
        }.onComplete { File file->
            log.debug("Async archive request with token ${token} finished successfully")
            request.file=file
        }.onError {Throwable t->
            log.error("Async archive request with token ${token} failed: ${t}",t)
            request.exception=t
        }
        //store in map
        asyncExportRequests.put(ident+'/'+token,request)
        token
    }
    /**
     * Attempt to get the request from the cache, or return null if it is not available
     * @param key key
     * @return request
     */
    private def ArchiveRequest getRequest(String key){
        try{
            return asyncExportResults.get(key)
        }catch (Exception e){
            return null
        }
    }
    /**
     * Return true if the request is still available
     * @param ident
     * @param token
     * @return
     */
    def boolean hasPromise(String ident,String token){
        return getRequest(ident+'/'+token)!=null
    }
    /**
     * Return summary of progress for the request, or null
     * @param ident
     * @param token
     * @return
     */
    def ProgressSummary promiseSummary(String ident,String token){
        getRequest(ident+'/'+token)?.summary
    }
    /**
     *
     * @param ident
     * @param token
     * @return result file, or null
     */
    def File promiseReady(String ident,String token){
        getRequest(ident+'/'+token)?.file
    }
    /**
     *
     * @param ident
     * @param token
     * @return date of request, or null
     */
    def Date promiseRequestStarted(String ident,String token){
        getRequest(ident+'/'+token)?.dateStarted
    }
    /**
     *
     * @param ident
     * @param token
     * @return exception thrown, or null
     */
    def Throwable promiseError(String ident,String token){
        return getRequest(ident+'/'+token)?.exception
    }
    /**
     * marks the request to be expired
     * @param ident
     * @param token
     * @return
     */
    def void releasePromise(String ident,String token){
        //remove from map, allow to expire via the cache
        asyncExportRequests.remove(ident+'/'+token)
    }
    /**
     * Export the project to a temp file jar
     * @param project
     * @param framework
     * @return
     * @throws ProjectServiceException
     */
    def exportProjectToFile(IRundeckProject project, Framework framework, ProgressListener listener=null) throws ProjectServiceException{
        def outfile
        try {
            outfile = File.createTempFile("export-${project.name}", ".jar")
        } catch (IOException exc) {
            throw new ProjectServiceException("Could not create temp file for archive: " + exc.message, exc)
        }
        outfile.withOutputStream { output ->
            exportProjectToOutputStream(project, framework, output, listener)
        }
        outfile.deleteOnExit()
        outfile
    }
    /**
     * Export the project to an outputstream
     * @param project
     * @param framework
     * @return
     * @throws ProjectServiceException
     */
    def exportProjectToOutputStream(IRundeckProject project, Framework framework,
                                    OutputStream stream, ProgressListener listener=null) throws ProjectServiceException{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        def Manifest manifest = new Manifest()
        manifest.mainAttributes.put(Attributes.Name.MANIFEST_VERSION,'1.0')
        manifest.mainAttributes.putValue('Rundeck-Application-Version', grailsApplication.metadata['app.version'])
        manifest.mainAttributes.putValue('Rundeck-Archive-Format-Version', '1.0')
        manifest.mainAttributes.putValue('Rundeck-Archive-Project-Name', project.name)
        manifest.mainAttributes.putValue('Rundeck-Archive-Export-Date', sdf.format(new Date()))

        def zip = new JarOutputStream(stream,manifest)
        exportProjectToStream(project, framework, zip, listener)
        zip.close()
    }

    def exportProjectToStream(
            IRundeckProject project,
            Framework framework,
            ZipOutputStream output,
            ProgressListener listener = null
    ) throws ProjectServiceException
    {
        ZipBuilder zip = new ZipBuilder(output)
//        zip.debug = true
        String projectName = project.name

        listener?.total(
                'export',
                ScheduledExecution.countByProject(projectName)+
                        3 * Execution.countByProject(projectName)+
                        BaseReport.countByCtxProject(projectName)
        )

        zip.dir("rundeck-${projectName}/") {
            //export jobs
            def jobs = ScheduledExecution.findAllByProject(projectName)
            dir('jobs/') {
                jobs.each { ScheduledExecution job ->
                    zip.file("job-${job.extid.encodeAsURL()}.xml") { Writer writer ->
                        exportJob job, writer
                        listener?.inc('export',1)
                    }
                }
            }

            def execs = Execution.findAllByProject(projectName)
            dir('executions/') {
                //export executions
                //export execution logs
                execs.each { Execution exec ->
                    exportExecution zip, exec, "execution-${exec.id}.xml"
                    listener?.inc('export',3)
                }
            }
            //export history
            def reports = BaseReport.findAllByCtxProject(projectName)
            dir('reports/') {
                reports.each { BaseReport report ->
                    exportHistoryReport zip, report, "report-${report.id}.xml"
                    listener?.inc('export',1)
                }
            }
        }
        listener?.done()

    }

    /**
     * Import a zip project archive to the project
     * @param project project
     * @param  user username of job owner
     * @param roleList role list string for scheduled jobs
     * @param framework framework
     * @param authContext authentication context
     * @param input input stream of zip data
     * @param options import options, [jobUUIDBehavior: (replace/preserve), executionImportBehavior: (import/skip)]
     */
    def importToProject(IRundeckProject project, String user, String roleList, Framework framework,
                        AuthContext authContext, InputStream input, Map options) throws ProjectServiceException {
        ZipReader zip = new ZipReader(new ZipInputStream(input))
//        zip.debug=true
        def jobxml=[]
        def jobxmlmap=[:]
        def execxml=[]
        def Map<String,File> execout=[:]
        def reportxml=[]
        def reportxmlnames=[:]
        def executionImportBehavior = options.executionImportBehavior ?: 'import'
        def importExecutions = executionImportBehavior == 'import'
        zip.read{
            '*/'{ //rundeck-<projectname>/
                'jobs/'{
                    'job-.*\\.xml'{path,name,inputs->
                        def tempfile = copyToTemp()
                        jobxml<< tempfile
                        jobxmlmap[tempfile]=[path:path,name:name]
                    }
                }
                if(importExecutions){
                    'executions/'{
                        'execution-.*\\.xml' {path, name, inputs ->
                            execxml << copyToTemp()
                        }
                        'output-.*\\.(txt|rdlog)|state-.*\\.state.json' {path, name, inputs ->
                            execout[name]= copyToTemp()
                        }
                    }
                    'reports/'{
                        'report-.*\\.xml' {path, name, inputs ->
                            reportxml<< copyToTemp()
                            reportxmlnames[reportxml[-1]]=name
                        }
                    }
                }
            }
        }
        //have files in dir
        (jobxml+execxml+execout.values()+reportxml).each {it.deleteOnExit()}

        def loadjobresults=[]
        def loadjoberrors=[]
        def jobIdMap=[:]
        def jobsByOldId=[:]
        def skipJobIds=[]
        def projectName= project.name
        //load jobs
        jobxml.each { File jxml->
            def path=jobxmlmap[jxml].path
            def name=jobxmlmap[jxml].name
            def jobset
            jxml.withInputStream {
                try {
                    jobset = it.decodeJobsXML()
                } catch (JobXMLException e) {
                    log.error("Failed parsing jobs from XML at archive path: ${path}${name}")
                    loadjoberrors << "Job XML file at archive path: ${path}${name} had errors: ${e.message}"
                    return
                }
                if (null == jobset) {
                    log.error("failed decoding jobs xml from zip: ${path}${name}")
                    return [errorCode: 'api.error.jobs.import.empty']
                }
                //contains list of old extids in input order
                def oldids=jobset.collect{it.extid}
                //change project name to the current project
                jobset*.project= projectName
                //remove uuid to reset it
                def uuidBehavior=options.jobUUIDBehavior?:'preserve'
                switch (uuidBehavior){
                    case 'remove':
                        jobset*.uuid = null
                        break;
                    case 'preserve':
                        //no-op, leave UUIDs and attempt to import
                        break;
                    break;
                }
                def results=scheduledExecutionService.loadJobs(jobset,'update',null,user,roleList,[:],framework,authContext)
                if(results.errjobs){
                    log.error("Failed loading (${results.errjobs.size()}) jobs from XML at archive path: ${path}${name}")
                    results.errjobs.each {
                        loadjoberrors<< "Job at index [${it.entrynum}] at archive path: ${path}${name} had errors: ${it.errmsg}"
                        log.error("Job at index [${it.entrynum}] had errors: ${it.errmsg}")
                        if(it.entrynum!=null && oldids[it.entrynum-1]){
                            skipJobIds<< oldids[it.entrynum - 1]
                        }
                    }
                }
                loadjobresults.addAll(results.jobs)
                results.jobsi.each{jobi->
                    if(jobi.entrynum!=null && oldids[jobi.entrynum-1]){
                        jobIdMap[oldids[jobi.entrynum-1]]=jobi.scheduledExecution.extid
                        jobsByOldId[oldids[jobi.entrynum - 1]]= jobi.scheduledExecution
                    }
                }
            }
        }

        log.info("Loaded ${loadjobresults.size()} jobs")

        if(importExecutions){
            Map execidmap = importExecutionsToProject(execxml, execout, projectName, framework, jobIdMap,skipJobIds)
            //load reports
            importReportsToProject(reportxml, jobsByOldId, reportxmlnames, execidmap, projectName)
        }
        (jobxml + execxml + execout.values() + reportxml).each { it.delete() }
        return [success:loadjoberrors?false:true,joberrors: loadjoberrors]
    }

    /**
     * Import reports, and generate new reports for any executions with a missing report.
     * @param reportxml
     * @param jobsByOldId
     * @param reportxmlnames
     * @param execidmap
     * @param projectName
     */
    private void importReportsToProject(ArrayList reportxml, jobsByOldId, reportxmlnames, Map execidmap, projectName) {
        def loadedreports = []
        def execids = new ArrayList<Long>(execidmap.values())
        reportxml.each { rxml ->
            def report = loadHistoryReport(rxml, execidmap, jobsByOldId, reportxmlnames[rxml])
            if(!report){
                log.debug("Report skipped: no matching execution imported. (file ${rxml})")
                return
            }
            report.ctxProject = projectName
            if (!report.save()) {
                log.error("Unable to save report: ${report.errors} (file ${rxml})")
                return
            }
            execids.remove(Long.parseLong(report.jcExecId))
            loadedreports << report
        }
        //generate reports for executions without matching reports
        execids.each { eid ->
            Execution newe = Execution.get(eid)
            def report = ExecReport.fromExec(newe)
            if (!report.save()) {
                log.error("Unable to save generated report: ${report.errors} (execution ${eid})")
                return
            }
            loadedreports << report
        }
        log.info("Loaded ${loadedreports.size()} reports")
    }

    /**
     * import executions, return a map from old execution ID to new ID
     * @param execxml
     * @param execout
     * @param projectName
     * @param framework
     * @param jobIdMap
     * @param skipJobIds list of Job IDs to skip execution import
     * @return map from old execution ID to new ID
     */
    private Map importExecutionsToProject(ArrayList execxml, Map<String, File> execout, projectName, Framework framework, jobIdMap, skipJobIds=[]) {
        // map from old execution ID to new ID
        def execidmap = [:]
        def oldidtoexec = [:]
        def retryexecs= [:]
        def loadexecresults = []
        //load executions, and move/rewrite outputfile names
        execxml.each { File exml ->
            def results = loadExecutions(exml, jobIdMap,skipJobIds)
            def execlist = results.executions
            def oldids = results.execidmap
            retryexecs.putAll(results.retryidmap)
            execlist.each { Execution e ->
                e.project = projectName
                if (e.workflow && !e.workflow.save()) {
                    log.error("Unable to save workflow for execution: ${e.workflow.errors} (file ${exml})")
                    return
                }
                if (!e.save()) {
                    log.error("Unable to save new execution: ${e.errors} (file ${exml})")
                    return
                }
                loadexecresults << e
                if (oldids[e]) {
                    execidmap[oldids[e]] = e.id
                    oldidtoexec[oldids[e]]=e
                }
                //check outputfile exists in mapping
                if (e.outputfilepath && execout[e.outputfilepath]) {
                    File oldfile = execout[e.outputfilepath]
                    //move to appropriate location and update outputfilepath
                    String filename = logFileStorageService.getFileForExecutionFiletype(e,
                            LoggingService.LOG_FILE_FILETYPE, false)
                    File newfile = new File(filename)
                    try{
                        FileUtils.moveFile(oldfile, newfile)
                    }catch (IOException exc) {
                        log.error("Failed to move temp log file to destination: ${newfile.absolutePath} (old id ${oldids[e]})", exc)
                    }
                    e.outputfilepath = newfile.absolutePath
                } else {
                    log.error("New execution ${e.id}, NO matching outfile: ${e.outputfilepath}")
                }

                //copy state.json file
                if(execout["state-${oldids[e]}.state.json"]){
                    File statefile= execout["state-${oldids[e]}.state.json"]
                    String filename = logFileStorageService.getFileForExecutionFiletype(e,
                            WorkflowService.STATE_FILE_FILETYPE, false)
                    File newfile = new File(filename)
                    try {
                        FileUtils.moveFile(statefile, newfile)
                    } catch (IOException exc) {
                        log.error("Failed to move temp state file to destination: ${newfile.absolutePath} (old id ${oldids[e]})", exc)
                    }
                }
            }
        }
        //reassign retry execution links
        loadexecresults.each { Execution e ->
            if (retryexecs[e]) {
                Execution retryExec = oldidtoexec[retryexecs[e]]
                if (retryExec) {
                    e.retryExecution = retryExec
                    if (!e.save()) {
                        log.error("Unable to update execution retry link: ${e.errors} (file ${exml})")
                        return
                    }
                }else{
                    log.error("Failed to link retry for ${e.id} to ${retryexecs[e]}")
                }
            }
        }
        log.info("Loaded ${loadexecresults.size()} executions, map: ${execidmap}")
        execidmap
    }

    /**
     * Delete a project completely
     * @param project framework project
     * @param framework frameowkr
     * @return map [success:true/false, error: (String errorMessage)]
     */
    def deleteProject(IRundeckProject project, Framework framework, AuthContext authContext, String username){
        def result = [success: false]
        BaseReport.withTransaction { TransactionStatus status ->

            try {
                //delete all reports
                BaseReport.findAllByCtxProject(project.name).each { e ->
                    e.delete(flush: true)
                }
                ExecReport.findAllByCtxProject(project.name).each { e ->
                    e.delete(flush: true)
                }
                def files=[]
                //delete all jobs with their executions
                ScheduledExecution.findAllByProject(project.name).each{ se->
                    def sedresult=scheduledExecutionService.deleteScheduledExecution(se, true, authContext,username)
                    if(!sedresult.success){
                        throw new Exception(sedresult.error)
                    }
                }
                //delete all remaining executions
                def allexecs= Execution.findAllByProject(project.name)
                def other=allexecs.size()
                executionService.deleteBulkExecutionIds(allexecs*.id, authContext, username)

                log.error("${other} other executions deleted")
                //delete all files
                def deletedfiles=0
                files.each{file->
                    if (null != file && file.exists() && !FileUtils.deleteQuietly(file)) {
                        log.warn("Failed to delete file while deleting project ${project.name}: ${file.absolutePath}")
                    }else{
                        deletedfiles++
                    }
                }
                log.error("${deletedfiles} files removed")
                result = [success: true]
            } catch (Exception e) {
                status.setRollbackOnly()
                log.error("Failed to delete project ${project.name}", e)
                result = [error: "Failed to delete project ${project.name}: ${e.message}", success: false]
            }
        }
        //if success, delete framework dir
        if(result.success){
            framework.getFrameworkProjectMgr().removeFrameworkProject(project.name)
        }
        return result
    }
}
class ArchiveRequest {
    String token
    Date dateStarted=new Date()
    Throwable exception
    ProgressSummary summary
    File file
}
interface ProgressListener {
    void total(String key,long total)
    void inc(String key,long count)
    void done()
}
interface ProgressSummary {
    int percent()
}
class ArchiveRequestProgress implements ProgressSummary,ProgressListener{
    Map<String,Long> totals=new HashMap<String,Long>()
    Map<String,Long> counts=new HashMap<String,Long>()


    @Override
    void total(final String key, final long total) {
        this.totals[key]=total
        if(counts[key]==null){
            counts[key]=0
        }
    }

    @Override
    void inc(final String key, final long count) {
        this.counts[key]=this.counts[key]?this.counts[key]+count:count
    }

    @Override
    void done() {
        counts.putAll(totals)
    }

    @Override
    int percent() {
        Double sum=totals.keySet().inject(0){a,k->
            a + ( counts[k]!=null  ? ( (counts[k]/totals[k]) / totals.size() ) : 0d)
        }
        return Math.floor(100*sum)
    }
}

class ProjectServiceException extends Exception {
    ProjectServiceException() {
    }

    ProjectServiceException(String s) {
        super(s)
    }

    ProjectServiceException(String s, Throwable throwable) {
        super(s, throwable)
    }

    ProjectServiceException(Throwable throwable) {
        super(throwable)
    }
}

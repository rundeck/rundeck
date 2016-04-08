package rundeck.services
import com.dtolabs.rundeck.app.support.BuilderUtil
import com.dtolabs.rundeck.app.support.ProjectArchiveImportRequest
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.core.common.Framework
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
import rundeck.services.logging.ExecutionFile
import rundeck.services.logging.ExecutionFileDeletePolicy
import rundeck.services.logging.ExecutionFileProducer
import rundeck.services.logging.ProducedExecutionFile

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectService implements InitializingBean, ExecutionFileProducer{
    public static final String EXECUTION_XML_LOG_FILETYPE = 'execution.xml'
    final String executionFileType = EXECUTION_XML_LOG_FILETYPE

    def grailsApplication
    def scheduledExecutionService
    def executionService
    def loggingService
    def logFileStorageService
    def workflowService
    def authorizationService
    def scmService
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

    @Override
    boolean isExecutionFileGenerated() {
        return true
    }

    @Override
    ExecutionFile produceStorageFileForExecution(final Execution e) {
        File localfile = getExecutionXmlFileForExecution(e)

        new ProducedExecutionFile(localFile: localfile, fileDeletePolicy: ExecutionFileDeletePolicy.ALWAYS)
    }

    /**
     * Write execution.xml file to a temp file and return
     * @param exec execution
     * @return file containing execution.xml
     */
    File getExecutionXmlFileForExecution(Execution execution){
        File executionXmlTempfile = File.createTempFile("execution-${execution.id}", ".xml")
        executionXmlTempfile.withWriter("UTF-8") { Writer writer ->
            exportExecutionXml(
                    execution,
                    writer,
                    "output-${execution.id}.rdlog"
            )
        }
        executionXmlTempfile.deleteOnExit()
        executionXmlTempfile
    }
    /**
     * Write execution.xml file to the writer
     * @param exec execution
     * @param writer writer
     * @param logfilepath optional new outputfilepath to set for the xml
     * @return
     */
    def exportExecutionXml(Execution exec, Writer writer, String logfilepath =null){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        def dateConvert = {
            sdf.format(it)
        }
        BuilderUtil builder = new BuilderUtil()
        builder.converters = [(Date): dateConvert, (java.sql.Timestamp): dateConvert]
        def map = exec.toMap()
        BuilderUtil.makeAttribute(map, 'id')
        if (logfilepath) {
            //change entry to point to local file
            map.outputfilepath = logfilepath
        }
        JobsXMLCodec.convertWorkflowMapForBuilder(map.workflow)
        def xml = new MarkupBuilder(writer)
        builder.objToDom("executions", [execution: map], xml)
    }
    def exportExecution(ZipBuilder zip, Execution exec, String name) throws ProjectServiceException {

        def File logfile = loggingService.getLogFileForExecution(exec)
        String logfilepath=null
        if (logfile && logfile.isFile()) {
            logfilepath = "output-${exec.id}.rdlog"
        }
        //convert map to xml
        zip.file("$name") { Writer writer ->
            exportExecutionXml(exec, writer, logfilepath)
        }
        if (logfile && logfile.isFile()) {
            zip.file logfilepath, logfile
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
    def loadExecutions(xmlinput, String projectName, Map jobIdMap=null, skipJobIds = []) throws ProjectServiceException {
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
                }else if(object.jobId) {
                    //look for same ID
                    def found = scheduledExecutionService.getByIDorUUID(object.jobId)
                    if(found && found.project==projectName){
                        se=found
                    }

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
                throw new ProjectServiceException("Unexpected data type for Execution($ecount): " + object.class.name)
            }
        }
        [executions:execlist,execidmap:execidmap, retryidmap: retryidmap]
    }

    private Node parseXml(xmlinput) {
        def XmlParser parser = new XmlParser()
        def doc
        def reader
        def filestream
        if (xmlinput instanceof File ) {
            filestream=new FileInputStream(xmlinput)
            reader = new InputStreamReader(filestream,"UTF-8")
        } else if (xmlinput instanceof InputStream) {
            reader = new InputStreamReader(xmlinput,"UTF-8")
        } else if (xmlinput instanceof String) {
            reader = new StringReader(xmlinput)
        } else {
            throw new IllegalArgumentException("Unexpected input: ${xmlinput}")
        }

        try {
            doc = parser.parse(reader)
        } catch (Exception e) {
            throw new ProjectServiceException("Unable to parse xml: ${e.message}",e)
        }finally{
            if(null!=filestream){
                filestream.close()
            }
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
    def exportProjectToFileAsync(IRundeckProject project, Framework framework, String ident, boolean aclReadAuth){
        final def summary=new ArchiveRequestProgress()
        final String token = UUID.randomUUID().toString()
        final def request=new ArchiveRequest(summary:summary,token:token)

        Promises.<File>task {
            ScheduledExecution.withNewSession {
                exportProjectToFile(project,framework,summary,aclReadAuth)
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
     * @param listener a progress listener
     * @param aclReadAuth true if ACL read access is granted, will include ACLs
     * @return
     * @throws ProjectServiceException
     */
    def exportProjectToFile(IRundeckProject project, Framework framework, ProgressListener listener=null,
                            boolean aclReadAuth=false) throws ProjectServiceException{
        def outfile
        try {
            outfile = File.createTempFile("export-${project.name}", ".jar")
        } catch (IOException exc) {
            throw new ProjectServiceException("Could not create temp file for archive: " + exc.message, exc)
        }
        outfile.withOutputStream { output ->
            exportProjectToOutputStream(project, framework, output, listener,aclReadAuth)
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
    def exportProjectToOutputStream(IRundeckProject project,
                                    Framework framework,
                                    OutputStream stream,
                                    ProgressListener listener=null,
                                    boolean aclReadAuth=false,
                                    ArchiveOptions options=null
    ) throws ProjectServiceException
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        def Manifest manifest = new Manifest()
        manifest.mainAttributes.put(Attributes.Name.MANIFEST_VERSION,'1.0')
        manifest.mainAttributes.putValue('Rundeck-Application-Version', grailsApplication.metadata['app.version'])
        manifest.mainAttributes.putValue('Rundeck-Archive-Format-Version', '1.0')
        manifest.mainAttributes.putValue('Rundeck-Archive-Project-Name', project.name)
        manifest.mainAttributes.putValue('Rundeck-Archive-Export-Date', sdf.format(new Date()))

        def zip = new JarOutputStream(stream,manifest)
        exportProjectToStream(project, framework, zip, listener, aclReadAuth,options)
        zip.close()
    }

    def exportProjectToStream(
            IRundeckProject project,
            Framework framework,
            ZipOutputStream output,
            ProgressListener listener = null,
            boolean aclReadAuth=false,
            ArchiveOptions options=null
    ) throws ProjectServiceException
    {
        ZipBuilder zip = new ZipBuilder(output)
//        zip.debug = true
        String projectName = project.name
        if(!options ||options.all) {
            listener?.total(
                    'export',
                    ScheduledExecution.countByProject(projectName) +
                            3 * Execution.countByProject(projectName) +
                            BaseReport.countByCtxProject(projectName) +
                            4 //properties and other files
            )
        }else if(options.executionsOnly) {
            listener?.total(
                    'export',
                    4 * options.executionIds.size()
            )
        }

        zip.dir("rundeck-${projectName}/") {
            //export jobs
            if(!options ||options.all) {
                def jobs = ScheduledExecution.findAllByProject(projectName)
                dir('jobs/') {
                    jobs.each { ScheduledExecution job ->
                        zip.file("job-${job.extid.encodeAsURL()}.xml") { Writer writer ->
                            exportJob job, writer
                            listener?.inc('export', 1)
                        }
                    }
                }
            }

            List<Execution> execs=[]
            List<BaseReport> reports=[]
            if(!options || options.all){
                execs = Execution.findAllByProject(projectName)
                reports = BaseReport.findAllByCtxProject(projectName)
            }else if(options.executionsOnly){
                //find execs
                List<Long> execIds = []
                List<String> execIdStrings = []
                options.executionIds.each {
                    if(it instanceof Long){
                        execIds<<it
                        execIdStrings<<it.toString()
                    }else if(it instanceof String){
                        execIds<<Long.parseLong(it)
                        execIdStrings<<it
                    }
                }
                execs = Execution.findAllByProjectAndIdInList(projectName,execIds)
                reports=ExecReport.findAllByCtxProjectAndJcExecIdInList(projectName,execIdStrings)
            }

            dir('executions/') {
                //export executions
                //export execution logs
                execs.each { Execution exec ->
                    exportExecution zip, exec, "execution-${exec.id}.xml"
                    listener?.inc('export',3)
                }
            }
            //export history

            dir('reports/') {
                reports.each { BaseReport report ->
                    exportHistoryReport zip, report, "report-${report.id}.xml"
                    listener?.inc('export',1)
                }
            }

            //export config
            if(!options || options.all) {
                dir('files/') {
                    dir('etc/') {
                        zip.file('project.properties') { Writer writer ->
                            def map = project.getProjectProperties()
                            map = replaceRelativePathsForProjectProperties(project, framework, map, '%PROJECT_BASEDIR%')
                            def projectProps = map as Properties
                            def sw = new StringWriter()
                            projectProps.store(sw, "Exported configuration")
                            def projectPropertiesText = sw.toString().
                                    split(Pattern.quote(System.getProperty("line.separator"))).
                                    sort().
                                    join(System.getProperty("line.separator"))
                            writer.write(projectPropertiesText)

                            listener?.inc('export', 1)
                        }
                    }
                    ['readme.md', 'motd.md'].each { filename ->
                        if (project.existsFileResource(filename)) {
                            zip.fileStream(filename) { OutputStream stream ->
                                project.loadFileResource(filename, stream)
                                listener?.inc('export', 1)
                            }
                        } else {
                            listener?.inc('export', 1)
                        }
                    }
                    if (aclReadAuth) {
                        //acls
                        def policies = project.listDirPaths('acls/').grep(~/^.*\.aclpolicy$/)
                        if (policies) {
                            def count = policies.size()
                            dir('acls/') {
                                policies.each { path ->
                                    def fname = path.substring('acls/'.length())
                                    zip.fileStream(fname) { OutputStream stream ->
                                        project.loadFileResource(path, stream)
                                        count--
                                        if (count == 0) {
                                            listener?.inc('export', 1)
                                        }
                                    }
                                }
                            }
                        } else {
                            listener?.inc('export', 1)
                        }
                    } else {
                        listener?.inc('export', 1)
                    }

                }
            }
        }
        listener?.done()

    }

    Map<String, String> replaceRelativePathsForProjectProperties(
            final IRundeckProject project,
            final Framework framework,
            final Map<String, String> projectProperties,
            String placeholder
    )
    {
        def Map<String, String> newmap = [:]
        def basepath=new File(framework.getFrameworkProjectsBaseDir(), project.name).absolutePath
        projectProperties.each{k,v->
            if(v.startsWith(basepath)){
                newmap[k]= v.replaceFirst(Pattern.quote(basepath), Matcher.quoteReplacement(placeholder))
            }else{
                newmap[k]=v
            }
        }
        newmap
    }

    Map<String, String> replacePlaceholderForProjectProperties(
            final IRundeckProject project,
            final Framework framework,
            final Map<String, String> projectProperties,
            String placeholder
    )
    {
        def Map<String, String> newmap = [:]
        def basepath=new File(framework.getFrameworkProjectsBaseDir(), project.name).absolutePath
        projectProperties.each{k,v->
            if(v.startsWith(placeholder)){
                newmap[k]= v.replaceFirst(Pattern.quote(placeholder), Matcher.quoteReplacement(basepath))
            }else{
                newmap[k]=v
            }
        }
        newmap
    }
    /**
     * Import a zip project archive to the project
     * @param project project
     * @param  user username of job owner
     * @param roleList role list string for scheduled jobs
     * @param framework framework
     * @param authContext authentication context
     * @param input input stream of zip data
     * @param options import options, [jobUUIDBehavior: (replace/preserve), importExecutions: (true/false)]
     */
    def importToProject(
            IRundeckProject project,
            Framework framework,
            UserAndRolesAuthContext authContext,
            InputStream input,
            ProjectArchiveImportRequest options
    ) throws ProjectServiceException
    {
        ZipReader zip = new ZipReader(new ZipInputStream(input))
//        zip.debug=true
        def jobxml = []
        def jobxmlmap = [:]
        def execxml = []
        def execxmlmap = [:]
        def Map<String, File> execout = [:]
        def reportxml = []
        def reportxmlnames = [:]
        boolean importExecutions = options.importExecutions
        boolean importConfig = options.importConfig
        boolean importACL = options.importACL
        File configtemp = null
        Map<String, File> mdfilestemp = [:]
        Map<String, File> aclfilestemp = [:]
        zip.read {
            '*/' { //rundeck-<projectname>/
                'jobs/' {
                    'job-.*\\.xml' { path, name, inputs ->
                        def tempfile = copyToTemp()
                        jobxml << tempfile
                        jobxmlmap[tempfile] = [path: path, name: name]
                    }
                }
                if (importExecutions) {
                    'executions/' {
                        'execution-.*\\.xml' { path, name, inputs ->
                            execxml << copyToTemp()
                            execxmlmap[execxml[-1]] = name
                        }
                        'output-.*\\.(txt|rdlog)|state-.*\\.state.json' { path, name, inputs ->
                            execout[name] = copyToTemp()
                        }
                    }
                    'reports/' {
                        'report-.*\\.xml' { path, name, inputs ->
                            reportxml << copyToTemp()
                            reportxmlnames[reportxml[-1]] = name
                        }
                    }
                }

                'files/' {
                    if (importConfig) {
                        'etc/' {
                            'project.properties' { path, name, inputs ->
                                configtemp = copyToTemp()
                            }
                        }
                        '(readme|motd)\\.md' { path, name, inputs ->
                            mdfilestemp[name] = copyToTemp()
                        }
                    }
                    if (importACL) {
                        'acls/' {
                            '.*\\.aclpolicy' { path, name, inputs ->
                                aclfilestemp[name] = copyToTemp()
                            }
                        }
                    }
                }
            }
        }
        //have files in dir
        (jobxml + execxml + execout.values() + reportxml + [configtemp] + mdfilestemp.values() + aclfilestemp.values()).
                each { it?.deleteOnExit() }

        def loadjobresults = []
        def loadjoberrors = []
        def execerrors = []
        def jobIdMap = [:]
        def jobsByOldId = [:]
        def skipJobIds = []
        def projectName = project.name
        //load jobs
        jobxml.each { File jxml ->
            def path = jobxmlmap[jxml].path
            def name = jobxmlmap[jxml].name
            def jobset
            jxml.withInputStream {
                try {
                    def reader = new InputStreamReader(it, "UTF-8")
                    jobset = reader.decodeJobsXML()
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
                def oldids = jobset.collect { it.extid }
                //change project name to the current project
                jobset*.project = projectName
                //remove uuid to reset it
                def uuidBehavior = options.jobUuidOption ?: 'preserve'
                switch (uuidBehavior) {
                    case 'remove':
                        jobset*.uuid = null
                        break;
                    case 'preserve':
                        //no-op, leave UUIDs and attempt to import
                        break;
                        break;
                }
                def results = scheduledExecutionService.loadJobs(
                        jobset,
                        'update',
                        null,
                        [:],
                        authContext
                )

                scheduledExecutionService.issueJobChangeEvents(results.jobChangeEvents)

                if (results.errjobs) {
                    log.error(
                            "Failed loading (${results.errjobs.size()}) jobs from XML at archive path: ${path}${name}"
                    )
                    results.errjobs.each {
                        loadjoberrors << "Job at index [${it.entrynum}] at archive path: ${path}${name} had errors: ${it.errmsg}"
                        log.error("Job at index [${it.entrynum}] had errors: ${it.errmsg}")
                        if (it.entrynum != null && oldids[it.entrynum - 1]) {
                            skipJobIds << oldids[it.entrynum - 1]
                        }
                    }
                }
                loadjobresults.addAll(results.jobs)
                results.jobsi.each { jobi ->
                    if (jobi.entrynum != null && oldids[jobi.entrynum - 1]) {
                        jobIdMap[oldids[jobi.entrynum - 1]] = jobi.scheduledExecution.extid
                        jobsByOldId[oldids[jobi.entrynum - 1]] = jobi.scheduledExecution
                    }
                }
            }
        }

        log.info("Loaded ${loadjobresults.size()} jobs")

        if (importExecutions) {
            Map execidmap = importExecutionsToProject(
                    execxml,
                    execout,
                    projectName,
                    framework,
                    jobIdMap,
                    skipJobIds,
                    execxmlmap,
                    execerrors
            )
            //load reports
            importReportsToProject(reportxml, jobsByOldId, reportxmlnames, execidmap, projectName, execerrors)
        }

        if (importConfig && configtemp) {

            importProjectConfig(configtemp, project, framework)
            log.debug("${project.name}: Loaded project configuration from archive")
        }
        if (importConfig && mdfilestemp) {
            importProjectMdFiles(mdfilestemp, project)
        }
        def aclerrors = []
        if (importACL && aclfilestemp) {
            aclerrors = importProjectACLPolicies(aclfilestemp, project)
        }

        (jobxml + execxml + execout.values() + reportxml + [configtemp] + mdfilestemp.values() + aclfilestemp.values()).
                each { it?.delete() }
        return [success: (loadjoberrors) ? false :
                true, joberrors: loadjoberrors, execerrors: execerrors, aclerrors: aclerrors]
    }

    private List<String> importProjectACLPolicies(Map<String, File> aclfilestemp, project) {
        def errors=[]
        aclfilestemp.each { String k, File v ->

            Validation validation = authorizationService.validateYamlPolicy(project.name, 'files/acls/'+k, v)
            if(!validation.valid){
                errors<<"files/acls/${k}: "+validation.toString()
                log.debug("${project.name}: Import failed for acls/${k}: "+validation)
                return
            }
            v.withInputStream { inputs ->
                project.storeFileResource('acls/' + k, inputs)
                log.debug("${project.name}: Loaded project ACLPolicy file acl/${k} from archive")
            }
        }
        errors
    }

    private void importProjectMdFiles(Map<String, File> mdfilestemp, project) {
        mdfilestemp.each { String k, File v ->
            v.withInputStream { inputs ->
                project.storeFileResource(k, inputs)
                log.debug("${project.name}: Loaded project file ${k} from archive")
            }
        }
    }

    /**
     * Import a config file to the project, loaded from an archive
     * @param configtemp temp file containing config properties
     * @param project project
     * @param framework framework
     */
    private void importProjectConfig(File configtemp, IRundeckProject project, Framework framework) {
        def inputProps = new Properties()
        configtemp.withReader { Reader reader ->
            inputProps.load(reader)
        }
        def map = replacePlaceholderForProjectProperties(project, framework, inputProps, '%PROJECT_BASEDIR%')
        def newprops = new Properties()
        newprops.putAll(map)
        project.setProjectProperties(newprops)
    }

    /**
     * Import reports, and generate new reports for any executions with a missing report.
     * @param reportxml
     * @param jobsByOldId
     * @param reportxmlnames
     * @param execidmap
     * @param projectName
     */
    private void importReportsToProject(ArrayList reportxml, jobsByOldId, reportxmlnames, Map execidmap, projectName,loadjoberrors) {
        def loadedreports = []
        def execids = new ArrayList<Long>(execidmap.values())
        reportxml.each { rxml ->
            def report
            try {
                report = loadHistoryReport(rxml, execidmap, jobsByOldId, reportxmlnames[rxml])
            } catch (ProjectServiceException e) {
                loadjoberrors<<"[${reportxmlnames[rxml]}] ${e.message}"
                log.debug("[${reportxmlnames[rxml]}] ${e.message}",e)
                log.error("[${reportxmlnames[rxml]}] ${e.message}")
                return
            }
            if(!report){
                log.debug("[${reportxmlnames[rxml]}] Report skipped: no matching execution imported.")
                return
            }
            report.ctxProject = projectName
            if (!report.save()) {
                log.error("[${reportxmlnames[rxml]}] Unable to save report: ${report.errors}")
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
    private Map importExecutionsToProject(ArrayList execxml, Map<String, File> execout, projectName,
                                          Framework framework, jobIdMap, skipJobIds, Map execxmlmap, execerrors = [] )
    {
        // map from old execution ID to new ID
        def execidmap = [:]
        def oldidtoexec = [:]
        def retryexecs= [:]
        def loadexecresults = []
        //load executions, and move/rewrite outputfile names
        execxml.each { File exml ->
            def results
            try {
                results = loadExecutions(exml,projectName, jobIdMap,skipJobIds)
            } catch (ProjectServiceException e) {
                log.debug("[${execxmlmap[exml]}] ${e.message}",e)
                execerrors<<"[${execxmlmap[exml]}] ${e.message}"
                return
            }
            def execlist = results.executions
            def oldids = results.execidmap
            retryexecs.putAll(results.retryidmap)
            execlist.each { Execution e ->
                e.project = projectName
                if (e.orchestrator && !e.orchestrator.save()) {
                    execerrors<<"[${execxmlmap[exml]}] Unable to save orchestrator for execution: ${e.orchestrator.errors}"
                    log.error("[${execxmlmap[exml]}] Unable to save orchestrator for execution: ${e.orchestrator.errors}")
                    return
                }
                if (e.workflow && !e.workflow.save()) {
                    execerrors<<"[${execxmlmap[exml]}] Unable to save workflow for execution: ${e.workflow.errors}"
                    log.error("[${execxmlmap[exml]}] Unable to save workflow for execution: ${e.workflow.errors}")
                    return
                }
                if (!e.save()) {
                    execerrors<<"[${execxmlmap[exml]}] Unable to save new execution: ${e.errors}"
                    log.error("[${execxmlmap[exml]}] Unable to save new execution: ${e.errors}")
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
                        execerrors<<"Failed to move temp log file to destination: ${newfile.absolutePath} (old id ${oldids[e]}): ${exc.message}"
                        log.error("Failed to move temp log file to destination: ${newfile.absolutePath} (old id ${oldids[e]})", exc)
                    }
                    e.outputfilepath = newfile.absolutePath
                } else {
                    execerrors<<"New execution ${e.id}, NO matching outfile: ${e.outputfilepath}"
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
                        execerrors<<"Failed to move temp state file to destination: ${newfile.absolutePath} (old id ${oldids[e]}): ${exc.message}"
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
                        execerrors<<"Unable to update execution retry link: ${e.errors} (Execution ${e.id})"
                        log.error("Unable to update execution retry link: ${e.errors} (Execution ${e.id})")
                        return
                    }
                }else{
                    execerrors<<"Failed to link retry for ${e.id} to ${retryexecs[e]}"
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

        //disable scm
        scmService.removeAllPluginConfiguration(project.name)

        BaseReport.withTransaction { TransactionStatus status ->

            try {
                //delete all reports
                BaseReport.findAllByCtxProject(project.name).each { e ->
                    e.delete(flush: true)
                }
                ExecReport.findAllByCtxProject(project.name).each { e ->
                    e.delete(flush: true)
                }
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

                log.debug("${other} other executions deleted")

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
class ArchiveOptions{
    Set executionIds=null
    boolean executionsOnly=false
    boolean all=true
    def parseExecutionsIds(execidsparam){
        if(execidsparam instanceof String){
            executionIds=new HashSet(execidsparam.split(',') as List)
        }else {
            executionIds=new HashSet([execidsparam].flatten())
        }
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
            a + ( ( totals[k]>0 ? ( (counts[k]!=null?counts[k]:0d)/totals[k] ) : 1d) / totals.size() )
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

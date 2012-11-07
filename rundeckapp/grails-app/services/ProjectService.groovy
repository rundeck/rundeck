import java.util.zip.ZipOutputStream
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject

import groovy.xml.MarkupBuilder
import com.dtolabs.rundeck.util.ZipBuilder
import java.util.zip.ZipInputStream
import com.dtolabs.rundeck.util.ZipReader
import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.jar.Attributes
import com.dtolabs.rundeck.app.support.BuilderUtil
import com.dtolabs.rundeck.util.XmlParserUtil
import rundeck.ScheduledExecution
import rundeck.BaseReport
import rundeck.Execution
import rundeck.ExecReport

class ProjectService {
    def grailsApplication
    def scheduledExecutionService
    def executionService
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
        def File outfile = exec.outputfilepath?new File(exec.outputfilepath):null
        if (outfile && outfile.isFile()) {
            //change entry to point to local file
            map.outputfilepath = "output-${exec.id}.txt"
        }
        JobsXMLCodec.convertWorkflowMapForBuilder(map.workflow)
        //convert map to xml
        zip.file("$name") { Writer writer ->
            def xml = new MarkupBuilder(writer)
            builder.objToDom("executions", [execution:map], xml)
        }
        if (outfile && outfile.isFile()) {
            zip.file "output-${exec.id}.txt", outfile
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
            }
            //convert dates
            convertStringsToDates(object, ['dateStarted', 'dateCompleted'],"Report ${identity}")
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
     * @return map data: 'executions' list of Executions that were parsed, 'execidmap' map of new Executions to the input IDs from the XML
     * @throws ProjectServiceException if an error occurs
     */
    def loadExecutions(xmlinput,Map jobIdMap=null) throws ProjectServiceException {
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
        def ecount=0
        doc.execution.each{ enode->
            def object = XmlParserUtil.toObject(enode)
            if (object instanceof Map) {
                JobsXMLCodec.convertXmlWorkflowToMap(object.workflow)
                //remap job id if necessary
                def se=null
                if(object.jobId && jobIdMap && jobIdMap[object.jobId]){
                    se=scheduledExecutionService.getByIDorUUID(jobIdMap[object.jobId])
                }
                //convert dates
                convertStringsToDates(object, ['dateStarted', 'dateCompleted'], "Execution($ecount) ID ${object.id}")
                try {
                    def newexec = Execution.fromMap(object, se)
                    execidmap[newexec]=object.id
                    execlist << newexec
                } catch (Throwable e) {
                    throw new ProjectServiceException("Unable to create Execution($ecount): " + e.getMessage(), e)
                }
                ecount++
            } else {
                throw new ProjectServiceException("Unexpected data type for Execution($ecount) in file (${xmlfile}): " + object.class.name)
            }
        }
        [executions:execlist,execidmap:execidmap]
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
     * Export the project to a temp file jar
     * @param project
     * @param framework
     * @return
     * @throws ProjectServiceException
     */
    def exportProjectToFile(FrameworkProject project, Framework framework) throws ProjectServiceException{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        def outfile=File.createTempFile("export-${project.name}", ".jar")
        def Manifest manifest = new Manifest()
        manifest.mainAttributes.put(Attributes.Name.MANIFEST_VERSION,'1.0')
        manifest.mainAttributes.putValue('Rundeck-Application-Version', grailsApplication.metadata['app.version'])
        manifest.mainAttributes.putValue('Rundeck-Archive-Format-Version', '1.0')
        manifest.mainAttributes.putValue('Rundeck-Archive-Project-Name', project.name)
        manifest.mainAttributes.putValue('Rundeck-Archive-Export-Date', sdf.format(new Date()))
        outfile.withOutputStream { output ->
            def zip = new JarOutputStream(output,manifest)
            exportProjectToStream(project, framework, zip)
            zip.close()
        }
        outfile.deleteOnExit()
        outfile
    }
    def exportProjectToStream(FrameworkProject project, Framework framework, ZipOutputStream output) throws ProjectServiceException {
        ZipBuilder zip = new ZipBuilder(output)
//        zip.debug = true
        String projectName = project.name

        zip.dir("rundeck-${projectName}/") {
            //export jobs
            def jobs = ScheduledExecution.findAllByProject(projectName)
            dir('jobs/') {
                jobs.each{ScheduledExecution job->
                    zip.file("job-${job.extid.encodeAsURL()}.xml") { Writer writer ->
                        exportJob job, writer
                    }
                }
            }

            def execs = Execution.findAllByProject(projectName)
            dir('executions/'){
                //export executions
                //export execution logs
                execs.each { Execution exec ->
                    exportExecution zip, exec, "execution-${exec.id}.xml"
                }
            }
            //export history
            def reports = BaseReport.findAllByCtxProject(projectName)
            dir('reports/') {
                reports.each { BaseReport report ->
                    exportHistoryReport zip, report, "report-${report.id}.xml"
                }
            }
        }

    }
    def importToProject(FrameworkProject project,String user, String roleList, Framework framework, ZipInputStream input) throws ProjectServiceException{
        ZipReader zip = new ZipReader(input)
//        zip.debug=true
        def jobxml=[]
        def jobxmlmap=[:]
        def execxml=[]
        def Map<String,File> execout=[:]
        def reportxml=[]
        def reportxmlnames=[:]
        zip.read{
            '*/'{ //rundeck-<projectname>/
                'jobs/'{
                    'job-.*\\.xml'{path,name,inputs->
                        def tempfile = copyToTemp()
                        jobxml<< tempfile
                        jobxmlmap[tempfile]=[path:path,name:name]
                    }
                }
                'executions/'{
                    'execution-.*\\.xml' {path, name, inputs ->
                        execxml << copyToTemp()
                    }
                    'output-.*\\.txt' {path, name, inputs ->
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
        //have files in dir
        (jobxml+execxml+execout.values()+reportxml).each {it.deleteOnExit()}

        def loadjobresults=[]
        def loadjoberrors=[]
        def jobIdMap=[:]
        def jobsByOldId=[:]
        def projectName= project.name
        //load jobs
        jobxml.each { File jxml->
            def path=jobxmlmap[jxml].path
            def name=jobxmlmap[jxml].name
            def jobset
            jxml.withInputStream {
                jobset = it.decodeJobsXML()
                if (null == jobset) {
                    log.error("failed decoding jobs xml from zip: ${path}${name}")
                    return [errorCode: 'api.error.jobs.import.empty']
                }
                //contains list of old extids in input order
                def oldids=jobset.collect{it.extid}
                //change project name to the current project
                jobset*.project= projectName
                //remove uuid to reset it
                jobset*.uuid=null
                def results=scheduledExecutionService.loadJobs(jobset,'update',user,roleList,[:],framework)
                if(results.errjobs){
                    log.warn("Failed loading some jobs in xml from zip: ${path}${name}")
                    loadjoberrors.addAll(results.errjobs)
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

        // map from old execution ID to new ID
        def execidmap=[:]
        def loadexecresults=[]
        //load executions, and move/rewrite outputfile names
        execxml.each { File exml->
            def results=loadExecutions(exml,jobIdMap)
            def execlist=results.executions
            def oldids=results.execidmap
            //check outputfile exists in mapping
            execlist.each{Execution e->
                e.project=projectName
                if (e.workflow && !e.workflow.save()) {
                    log.error("Unable to save workflow for execution: ${e.workflow.errors} (file ${exml})")
                    return
                }
                if(!e.save()){
                    log.error("Unable to save new execution: ${e.errors} (file ${exml})")
                    return
                }
                loadexecresults<<e
                if(oldids[e]){
                    execidmap[oldids[e]]=e.id
                }
                if(e.outputfilepath && execout[e.outputfilepath]){
                    File oldfile = execout[e.outputfilepath]
                    //move to appropriate location and update outputfilepath
                    String filename=executionService.createOutputFilepathForExecution(e,framework)
                    File newfile = new File(filename)
                    if(!oldfile.renameTo(newfile)){
                        log.error("Unable to move temp log file to destination: ${newfile.absolutePath} (old id ${oldids[e]})")
                    }else{
                        e.outputfilepath=newfile.absolutePath
                    }
                }else{
                    log.error("New execution ${e.id}, NO matching outfile: ${e.outputfilepath}")
                }
            }
        }
        log.info("Loaded ${loadexecresults.size()} executions, map: ${execidmap}")
        //load reports
        def loadedreports=[]
        reportxml.each{rxml->
            def report=loadHistoryReport(rxml,execidmap, jobsByOldId,reportxmlnames[rxml])
            report.ctxProject = projectName
            if (!report.save()) {
                log.error("Unable to save report: ${report.errors} (file ${rxml})")
                return
            }
            loadedreports<<report
        }
        log.info("Loaded ${loadedreports.size()} reports")
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

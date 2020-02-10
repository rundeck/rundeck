/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.components

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.app.components.jobs.JobDefinitionException
import org.rundeck.app.components.jobs.JobDefinitionManager
import org.rundeck.app.components.jobs.JobFormat
import org.rundeck.app.components.jobs.JobDefinitionComponent
import org.rundeck.app.components.jobs.UnsupportedFormatException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.ScheduledExecution
import rundeck.services.JobFromMapSerializer

/**
 * Handles job definition import/export processing
 */
@CompileStatic
class RundeckJobDefinitionManager implements JobDefinitionManager, ApplicationContextAware {
    ApplicationContext applicationContext

    private Map<String, JobFormat> defaultFormats = new HashMap<>(xml: new JobXMLFormat(), yaml: new JobYAMLFormat())

    RundeckJobDefinitionManager() {
    }

    /**
     * @return JobDefinitionComponents keyed by name
     */
    public Map<String, JobDefinitionComponent> getJobDefinitionComponents() {
        def beans = applicationContext?.getBeansOfType(JobDefinitionComponent)
        beans?.values()?.collectEntries { [it.name, it] }
    }

    /**
     * @return JobFormats keyed by format name
     */
    public Map<String, JobFormat> getJobFormats() {
        def beans = applicationContext?.getBeansOfType(JobFormat)
        beans?.values()?.collectEntries { [it.format, it] }
    }


    /**
     * Duplicate a job definition
     * @param from
     * @return
     */
    ImportedJob copy(ScheduledExecution from) {
        jobFromMap(jobToMap(from))
    }

    /**
     * Validate imported component associations
     * @param importedJob
     * @return true if valid
     */
    ReportSet validateImportedJob(ImportedJob<ScheduledExecution> importedJob) {
        ReportSet reports = new ReportSet(validations: new HashMap<String, Validator.Report>(), valid: true)
        jobDefinitionComponents?.each { String name, JobDefinitionComponent jobImport ->
            def report = jobImport.validateImported(importedJob.job, importedJob.associations[jobImport.name])
            if (report && !report.valid) {
                reports.valid = false
            }
            if (report != null) {
                reports.validations[jobImport.name] = report
            }
        }
        return reports
    }


    /**
     * Persist component associations via JobImport beans
     * @param importedJob
     * @param authContext
     * @return
     */
    void persistComponents(ImportedJob<ScheduledExecution> importedJob, UserAndRolesAuthContext authContext) {
        jobDefinitionComponents?.each { String name, JobDefinitionComponent jobImport ->
            jobImport.persist(importedJob.job, importedJob.associations[jobImport.name], authContext)
        }
    }
    /**
     * Callback to indicate persistence completed
     * @param importedJob
     * @param authContext
     * @return
     */
    void waspersisted(ImportedJob<ScheduledExecution> importedJob, UserAndRolesAuthContext authContext) {
        jobDefinitionComponents?.each { String name, JobDefinitionComponent jobImport ->
            jobImport.wasPersisted(importedJob.job, importedJob.associations[jobImport.name], authContext)
        }
    }

    /**
     * Callback to indicate job delete will begin
     * @param importedJob
     * @param authContext
     * @return
     */
    void beforeDelete(ScheduledExecution job, AuthContext authContext) {
        jobDefinitionComponents?.each { String name, JobDefinitionComponent jobImport ->
            jobImport.willDeleteJob(job, authContext)
        }
    }

    /**
     * Callback to indicate job delete will begin
     * @param importedJob
     * @param authContext
     * @return
     */
    void afterDelete(ScheduledExecution job, AuthContext authContext) {
        jobDefinitionComponents?.each { String name, JobDefinitionComponent jobImport ->
            jobImport.didDeleteJob(job, authContext)
        }
    }

    /**
     * Find specified format
     * @param format
     * @return
     * @throws JobDefinitionException
     */
    JobFormat getFormat(String format) throws UnsupportedFormatException {
        def found = jobFormats?.get(format)
        if (!found) {
            if (defaultFormats[format]) {
                return defaultFormats[format]
            }
            throw new UnsupportedFormatException("Format not found: $format")
        }
        found
    }

    /**
     * Create a single Job definition from canonical map
     * @param map
     * @return
     */
    ImportedJob<ScheduledExecution> jobFromMap(Map map) {
        def job = ScheduledExecution.fromMap(map)
        def Map<String, Object> associates = [:]
        jobDefinitionComponents?.each { String name, JobDefinitionComponent jobImport ->
            def result = jobImport.importCanonicalMap(job, map)
            if (result) {
                associates[jobImport.name] = result
            }
        }
        importedJob(job, associates)
    }

    /**
     * Update job definition
     * @param job job to update
     * @param importedJob an imported job definition used for updating
     * @param params request params map
     * @return imported job contains job definition and associations map
     */
    ImportedJob<ScheduledExecution> updateJob(ScheduledExecution job, ImportedJob<ScheduledExecution> importedJob, Map params) {
        def Map<String, Object> associates = importedJob?.associations ?: [:]
        jobDefinitionComponents?.each { String name, JobDefinitionComponent jobImport ->
            def jcParams = getParamsForJobComponent(params, jobImport)
            def result = jobImport.updateJob(job, importedJob?.job, associates[jobImport.name], jcParams ?: params)
            if (result) {
                associates[jobImport.name] = result
            }
        }
        RundeckJobDefinitionManager.importedJob(job, associates)
    }

    static String getFormFieldPrefixForJobComponent(String name) {
        return "jobComponent.${name}.configMap."
    }
    static String getMessagesTypeForJobComponent(String name) {
        return "jobComponent.${name}"
    }

    public Map getParamsForJobComponent(Map params, JobDefinitionComponent jobImport) {
        if (params.get('jobComponent') instanceof Map) {
            Map jc = (Map) params['jobComponent']
            if (jc.get(jobImport.name) instanceof Map) {
                Map jc1 = (Map) jc[jobImport.name]
                if (jc1.get('configMap') instanceof Map) {
                    return (Map) jc1['configMap']
                }
            }
        }
        return null
    }
    Map<String,Map> getJobDefinitionComponentValues(ScheduledExecution job){
        Map<String,Map> jobComponentValues=[:]
        jobDefinitionComponents?.each{name,comp->
            if(comp.inputProperties){
                jobComponentValues[name]=comp.getInputPropertyValues(job,null)
            }
        }
        jobComponentValues
    }

    /**
     * Create jobs from collection of canonical job Maps
     * @param list
     * @return
     * @throws Exception if
     */
    List<ImportedJob<ScheduledExecution>> createJobs(Collection<?> dataset) throws JobDefinitionException {
        ArrayList list = new ArrayList()
        if (dataset instanceof Collection) {
            //iterate through list of jobs
            dataset.each { jobobj ->
                if (jobobj instanceof Map) {
                    try {
                        list << jobFromMap(jobobj)
                    } catch (Exception e) {
                        throw new JobDefinitionException("Unable to create Job: " + e.getMessage(), e)
                    }
                } else {
                    throw new JobDefinitionException("Unexpected data type: " + jobobj.getClass().name)
                }
            }
        } else {
            throw new JobDefinitionException("Unexpected data type: " + dataset.class.name)
        }
        return list
    }

    /**
     * Create a job serializer
     * @param job
     * @return
     */
    JobSerializer createJobSerializer(ScheduledExecution job) {
        new JobFromMapSerializer(this, jobToMap(job))
    }

    /**
     * Create canonical map from a job defintion
     * @param job
     * @return
     */
    Map jobToMap(ScheduledExecution job) {
        def oMap = job.toMap()
        jobDefinitionComponents?.each { String name, JobDefinitionComponent export ->
            def vMap = export.exportCanonicalMap(oMap)
            if (vMap) {
                oMap = vMap
            }
        }
        return oMap
    }

    /**
     * Convert canonical map to Xmap
     * @param map canonical map
     * @param preserveUuid if true, preserve job uuid
     * @param replaceId replacement ID
     * @param stripJobRef 'name' or 'uuid', strips this value from job references
     * @return
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Map jobMapToXMap(
            Map map,
            boolean preserveUuid = true,
            String replaceId = null,
            String stripJobRef = null
    ) {
        //nb: used when exporting execution xml and including job definition, we hardcode reference to JobXmlFormat
        JobXMLFormat jobXmlFormat = (JobXMLFormat) getFormat('xml')
        jobXmlFormat.jobMapToXMap(map, preserveUuid, replaceId, stripJobRef)
    }

    /**
     * Serialize job list as xml
     * @param list
     * @return
     */
    String exportAsXml(List<ScheduledExecution> list) {
        exportAs('xml', list)
    }

    /**
     * Serialize job list as yaml
     * @param list
     * @return
     */
    String exportAsYaml(List<ScheduledExecution> list) {
        exportAs('yaml', list)
    }
    /**
     * Serialize job list as yaml
     * @param list
     * @return
     */
    String exportAs(String format, List<ScheduledExecution> list) {
        def writer = new StringWriter()
        exportAs(format, list, writer)
        return writer.toString()
    }
    /**
     * Serialize job list as format
     * @param list
     * @return
     */
    void exportAs(String format, List<ScheduledExecution> list, Writer writer) {
        exportAs(format, list, JobFormat.defaultOptions(), writer)
    }
    /**
     * Serialize job list as format
     * @param list
     * @return
     */
    void exportAs(String format, List<ScheduledExecution> list, JobFormat.Options options, Writer writer) {
        def mapList = list.collect { jobToMap(it) }
        getFormat(format).encode(mapList, options, writer)
    }


    /**
     * Decode Job XML from a file into a list of Jobs
     * @param file file containing XML
     * @return List of jobs
     * @throws JobDefinitionException
     */
    List<ImportedJob<ScheduledExecution>> decodeXml(File file) throws JobDefinitionException {
        decodeFormat('xml', file)
    }

    /**
     * Decode Job XML into a list of Jobs
     * @param reader input XML
     * @return List of jobs
     * @throws JobDefinitionException
     */
    List<ImportedJob<ScheduledExecution>> decodeXml(Reader reader) throws JobDefinitionException {
        decodeFormat('xml', reader)
    }

    /**
     * Decode YAML job defintion from a file
     * @param file yaml content file
     * @return list of jobs
     */
    List<ImportedJob<ScheduledExecution>> decodeYaml(File file) throws JobDefinitionException {
        decodeFormat('yaml', file)
    }

    /**
     * Decode YAML from a reader
     * @param reader
     * @return list of jobs
     */
    List<ImportedJob<ScheduledExecution>> decodeYaml(Reader reader) throws JobDefinitionException {
        decodeFormat('yaml', reader)
    }

    /**
     * Decode job defintion from a file
     * @param format job format
     * @param file yaml content file
     * @return list of jobs
     */
    List<ImportedJob<ScheduledExecution>> decodeFormat(String format, File file) throws JobDefinitionException {
        file.withReader('UTF-8') {
            decodeFormat(format, it)
        }
    }

    /**
     * Decode job defintion from a stream
     * @param format job format
     * @param inputStream input
     * @return list of jobs
     */
    List<ImportedJob<ScheduledExecution>> decodeFormat(String format, InputStream inputStream)
            throws JobDefinitionException {
        inputStream.withReader('UTF-8') {
            decodeFormat(format, it)
        }
    }

    /**
     * Decode job defintion from a string
     * @param format job format
     * @param content formatted content
     * @return list of jobs
     */
    List<ImportedJob<ScheduledExecution>> decodeFormat(String format, String content) throws JobDefinitionException {
        decodeFormat(format, new StringReader(content))
    }

    /**
     * Decode Job format into a list of Jobs
     * @param reader input XML
     * @return List of jobs
     * @throws JobDefinitionException
     */
    List<ImportedJob<ScheduledExecution>> decodeFormat(String format, Reader reader) throws JobDefinitionException {
        def jobMaps = getFormat(format).decode(reader)
        createJobs(jobMaps)
    }

    /**
     * Create an imported job from job and associations
     * @param job
     * @param associations
     * @return
     */
    static ImportedJob<ScheduledExecution> importedJob(ScheduledExecution job, Map<String, Object> associations = [:]) {
        new ImportedJobDefinition(job: job, associations: associations)
    }

    static class ImportedJobDefinition implements ImportedJob<ScheduledExecution> {
        ScheduledExecution job
        Map<String, Object> associations = [:]
    }
    static class ReportSet{
        boolean valid
        Map<String,Validator.Report> validations
    }
}


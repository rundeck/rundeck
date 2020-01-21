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


import com.dtolabs.rundeck.plugins.scm.JobSerializer
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.rundeck.app.components.jobs.JobDefinitionException
import org.rundeck.app.components.jobs.JobDefinitionManager
import org.rundeck.app.components.jobs.JobExport
import org.rundeck.app.components.jobs.JobFormat
import org.rundeck.app.components.jobs.JobImport
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
 * Duplicate a job definition
 * @param from
 * @return
 */
    ScheduledExecution copy(ScheduledExecution from) {
        jobFromMap(jobToMap(from))
    }

    /**
     * Find specified format
     * @param format
     * @return
     * @throws JobDefinitionException
     */
    JobFormat getFormat(String format) throws UnsupportedFormatException {

        def type = applicationContext?.getBeansOfType(JobFormat)
        def found = type?.find { String bean, JobFormat export ->
            export.format == format
        }?.value
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
    ScheduledExecution jobFromMap(Map map) {
        def job = ScheduledExecution.fromMap(map)
        applicationContext?.getBeansOfType(JobImport)?.each { String bean, JobImport export ->
            export.importCanonicalMap(job, map)
        }
        job
    }

    /**
     * Create jobs from collection of canonical job Maps
     * @param list
     * @return
     * @throws Exception if
     */
    List<ScheduledExecution> createJobs(Collection<?> dataset) throws JobDefinitionException {
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
        applicationContext?.getBeansOfType(JobExport)?.each { String bean, JobExport export ->
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
     * Encode canonical maps into XML
     * @param list
     * @param xml
     * @param preserveUuid
     * @param replaceIds
     * @param stripJobRef
     * @return
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    void encodeJobMapsAsXml(
            List<Map> list,
            Writer writer,
            boolean preserveUuid = true,
            Map<String, String> replaceIds = [:],
            String stripJobRef = null
    ) {

        getFormat('xml').encode(this, list, JobFormat.options(preserveUuid, replaceIds, stripJobRef), writer)
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
    List<ScheduledExecution> decodeXml(File file) throws JobDefinitionException {
        decodeFormat('xml', file)
    }

    /**
     * Decode Job XML into a list of Jobs
     * @param reader input XML
     * @return List of jobs
     * @throws JobDefinitionException
     */
    List<ScheduledExecution> decodeXml(Reader reader) throws JobDefinitionException {
        decodeFormat('xml', reader)
    }

    /**
     * Decode YAML job defintion from a file
     * @param file yaml content file
     * @return list of jobs
     */
    List<ScheduledExecution> decodeYaml(File file) throws JobDefinitionException {
        decodeFormat('yaml', file)
    }

    /**
     * Decode YAML from a reader
     * @param reader
     * @return list of jobs
     */
    List<ScheduledExecution> decodeYaml(Reader reader) throws JobDefinitionException {
        decodeFormat('yaml', reader)
    }

    /**
     * Decode job defintion from a file
     * @param format job format
     * @param file yaml content file
     * @return list of jobs
     */
    List<ScheduledExecution> decodeFormat(String format, File file) throws JobDefinitionException {
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
    List<ScheduledExecution> decodeFormat(String format, InputStream inputStream) throws JobDefinitionException {
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
    List<ScheduledExecution> decodeFormat(String format, String content) throws JobDefinitionException {
        decodeFormat(format, new StringReader(content))
    }

    /**
     * Decode Job format into a list of Jobs
     * @param reader input XML
     * @return List of jobs
     * @throws JobDefinitionException
     */
    List<ScheduledExecution> decodeFormat(String format, Reader reader) throws JobDefinitionException {
        def jobMaps = getFormat(format).decode(reader)
        createJobs(jobMaps)
    }
}


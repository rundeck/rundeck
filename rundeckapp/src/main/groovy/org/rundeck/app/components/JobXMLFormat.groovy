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

import com.dtolabs.rundeck.app.support.BuilderUtil
import com.dtolabs.rundeck.util.XmlParserUtil
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.MarkupBuilder
import org.rundeck.app.components.jobs.JobDefinitionException
import org.rundeck.app.components.jobs.JobFormat
import org.rundeck.app.components.jobs.JobDefinitionComponent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.codecs.JobsXMLCodec

/**
 * Definition for jobs XML format
 */
@CompileStatic
class JobXMLFormat implements JobFormat, ApplicationContextAware {
    final String format = 'xml'
    ApplicationContext applicationContext

    /**
     * Convert Xmap Map to Canonical Job Map
     * @param inputXmap Xmap data
     * @return canonical job Map
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Map convertXmapToJobMap(Map inputXmap) throws JobDefinitionException {
        def oMap = JobsXMLCodec.convertXMapToJobMap(inputXmap)
        applicationContext?.getBeansOfType(JobDefinitionComponent)?.each { String bean, JobDefinitionComponent jobImport ->
            def vMap = jobImport.importXMap(inputXmap, oMap)
            if (vMap) {
                oMap = vMap
            }
        }
        oMap
    }

    /**
     * Convert groovy Node structure to Xmap
     * @param data parsed groovy Node
     * @return Map
     */
    Map convertNodeToXMap(Node data) throws JobDefinitionException {
        final Object object = XmlParserUtil.toObject(data, false)
        if (!(object instanceof Map)) {
            throw new JobDefinitionException("Expected map data")
        }
        (Map) object
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    List<Map> decode(final Reader reader) throws JobDefinitionException {
        Node doc
        XmlParser parser = new XmlParser()
        try {
            doc = parser.parse(reader)
        } catch (Exception e) {
            throw new JobDefinitionException("Unable to parse xml: ${e}")
        }

        if (!doc) {
            throw new JobDefinitionException("XML Document could not be parsed.")
        }
        if (doc.name() != 'joblist') {
            throw new JobDefinitionException("Document root tag was not 'joblist': '${doc.name()}'")
        }
        if (!doc.job || doc.job.size() < 1) {
            throw new JobDefinitionException("No 'job' element was found")
        }
        return doc.job.collect { convertXmapToJobMap(convertNodeToXMap(it)) }
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
        Map oMap = JobsXMLCodec.convertJobMap(map, preserveUuid, replaceId, stripJobRef)
        applicationContext?.getBeansOfType(JobDefinitionComponent)?.each { String bean, JobDefinitionComponent export ->
            def vMap = export.exportXMap(oMap)
            if (vMap) {
                oMap = vMap
            }
        }
        return oMap
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void encode(final List<Map> list, Options options, final Writer writer) {
        def xml = new MarkupBuilder(writer)
        BuilderUtil bu = new BuilderUtil()
        bu.forceLineEndings = true
        bu.lineEndingChars = '\n'
        //todo: set line ending from config?
        bu.canonical = true
        xml.joblist() {
            list.each { Map jobMap ->
                job {
                    bu.mapToDom(
                            jobMapToXMap(
                                    jobMap,
                                    options.preserveUuid,
                                    options.replaceIds.get(jobMap.id),
                                    options.stripJobRef?.name()
                            ),
                            delegate
                    )
                }
            }
        }
    }
}

/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.server.plugins.logging

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.workflow.OutputContext
import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.storage.files.FileStorageTree
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.data.DataUtil

import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * @author greg
 * @since 5/17/17
 */
@Plugin(name = SaveFileFilterPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Save File Filter',
        description = '''...''')

class SaveFileFilterPlugin implements LogFilterPlugin {
    public static final String PROVIDER_NAME = 'save-file-data'
    public static final String PATTERN = '^(.+?)\\s*>>\\s*(.+)$'

    @PluginProperty(
            title = "Pattern",
            description = '''Regular Expression for matching a writting operation.''',
            defaultValue = SaveFileFilterPlugin.PATTERN,
            required = true
    )
    String pattern

    @PluginProperty(
            title = 'Use Regex',
            description = '''If true, use Regular Expression to capture data''',
            defaultValue = 'true'
    )
    Boolean useRegex

    @PluginProperty(
            title = 'Log Data',
            description = '''If true, log the captured data''',
            defaultValue = 'false'
    )
    Boolean logData

    Pattern dataPattern;
    OutputContext outputContext
    FileStorageTree fileStorageTree
    List<String> allData
    private ObjectMapper mapper
    String project
    String jobName
    String execid

    @Override
    void init(final PluginLoggingContext context) {
        outputContext = context.getOutputContext()
        fileStorageTree = context.getFileStorateTree()
        mapper = new ObjectMapper()
        allData = []
        project = context.dataContext.job.project
        jobName = context.dataContext.job.name
        execid = context.dataContext.job.execid
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if (event.eventType == 'log'
                && (event.loglevel == LogLevel.NORMAL || event.loglevel == LogLevel.VERBOSE)
                && event.message?.length() > 0) {

            if(!useRegex){
                allData = [this.pattern]
            } else {
                dataPattern = Pattern.compile(pattern)
                Matcher match = dataPattern.matcher(event.message)
                while (match.find()) {
                    if(match.groupCount()==1){
                        allData.add(match.group(1))
                    }else {
                        allData.add(match.group(2))
                    }
                }
            }
        }
    }

    @Override
    void complete(final PluginLoggingContext context) {
        if (allData.size() > 0) {
            allData.unique { a, b -> a <=> b }.each {String pathMatched ->
                Path pathToSave = PathUtil.asPath("/${PathUtil.asPath(pathMatched).getName()}")
                byte[] fileContent = fileContent(pathMatched, context)
                boolean hasFile = fileStorageTree.hasFileOnExecWorkpacePath(pathToSave, project, jobName, execid)

                Map<String, String> map = [:]
//                    if(contentType){
//                        map[StorageUtil.RES_META_RUNDECK_CONTENT_TYPE] = contentType
//                    }

                InputStream stream = new ByteArrayInputStream(fileContent)

                if(hasFile){
                    fileStorageTree.updateResource(
                            pathToSave,
                            DataUtil.withStream(stream, map, StorageUtil.factory()),
                            project,
                            jobName,
                            execid)
                } else {
                    fileStorageTree.createResource(
                            pathToSave,
                            DataUtil.withStream(stream, map, StorageUtil.factory()),
                            project,
                            jobName,
                            execid)
                }
            }
            if (logData) {
                writeLog(context)
            }
        }
    }

    private byte[] fileContent(String path, PluginLoggingContext context){
        return new File(DataContextUtils.replaceDataReferencesInString(path, context.dataContext)).getBytes()
    }

    private void writeLog(PluginLoggingContext context){
        Map<String, String> logPath = [:]
        allData.unique { a, b -> a <=> b }.each { String pathMatched ->
            logPath.put("Path source", pathMatched)
        }
        ObjectMapper objectMapper = new ObjectMapper()
        StringWriter stringWriter = new StringWriter()
        objectMapper.writeValue(stringWriter, logPath)


        context.log(
                2,
                stringWriter.toString(),
                [
                        'content-data-type'       : 'application/json',
                        'content-meta:table-title': 'File saved on storage'
                ]
        )
    }
}

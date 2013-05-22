package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.logging.StreamingLogReader

/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
 * FSExecutionLogProvider.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/22/13 7:03 PM
 * 
 */
class FSExecutionLogProvider implements ExecutionLogProvider{
    File basedir
    LogLevel defaultLogLevel
    OutputLogFormat logFormat
    RundeckLogFormat rundeckLogFormat
    public FSExecutionLogProvider(File basedir, LogLevel defaultLogLevel){
        this.basedir=basedir
        this.defaultLogLevel=defaultLogLevel
        rundeckLogFormat= new RundeckLogFormat()
        this.logFormat= rundeckLogFormat
    }

    @Override
    StreamingLogWriter getLogWriter(String id, String key, LogLevel logThreshold, Map<String, String> defaultMeta) {
        File file = getFileForKey(key)
        if (!file.getParentFile().isDirectory()) {
            if (!file.getParentFile().mkdirs()) {
                throw new IllegalStateException("Unable to create directories for storage: " + file)
            }
        }
        return new FSStreamingLogWriter(new FileOutputStream(file), logThreshold, defaultMeta, logFormat)
    }

    private File getFileForKey(String key) {
        new File(basedir, key + ".log")
    }

    @Override
    StreamingLogReader getLogReader(String id, String key) {
        File file = getFileForKey(key)
        return getLogReaderForFile(file)
    }
    StreamingLogReader getLogReaderForFile(File file) {
        if (!file.exists()){
            throw new IllegalArgumentException("File does not exist: "+file)
        }
        return new FSStreamingLogReader(file,"UTF-8", logFormat);
    }

    @Override
    URL getURLForKey(String key) {
        return getFileForKey(key).toURI().toURL()
    }
}

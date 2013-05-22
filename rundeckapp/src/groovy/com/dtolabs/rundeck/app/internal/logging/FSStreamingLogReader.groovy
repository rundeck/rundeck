package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogEntryIterator
import com.dtolabs.rundeck.core.logging.ReverseSeekingStreamingLogReader

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
 * FSStreamingLogReader.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/23/13 7:40 PM
 * 
 */
class FSStreamingLogReader implements ReverseSeekingStreamingLogReader {
    private File file
    private String encoding
    RundeckLogFormat rundeckLogFormat
    /**
     * Optional date for resolving legacy unspecific timestamps
     */
    Date referenceDate
    public FSStreamingLogReader(File file, String encoding, RundeckLogFormat rundeckLogFormat) {
        this.file = file
        this.encoding = encoding
        this.rundeckLogFormat=rundeckLogFormat
    }
    private LogEntryIterator detectedIterator(){

    }

    public long getTotalSize() {
        return file.length()
    }

    public Date getLastModified() {
        return new Date(file.lastModified())
    }

    @Override
    LogEntryIterator beginFromOffset(long offset) {
        def raf = new FileInputStream(file)
        raf.channel.position(offset)
        //XXX
        def LegacyLogEntryLineIterator iterator = new LegacyLogEntryLineIterator(new FSFileLineIterator(raf, encoding))
        iterator.referenceDate= referenceDate?:new Date(file.lastModified())
        return iterator
    }

    Iterator<LogEvent> iteratorFromOffset(long offset) {
        return beginFromOffset(offset)
    }

    @Override
    LogEntryIterator logEntryIterator() {
        return beginFromOffset(0)
    }

    @Override
    Iterator<LogEvent> iterator() {
        return logEntryIterator()
    }

    /**
     * Returns the iterator starting at offset entries from the end.
     * @param offset
     * @return
     */
    LogEntryIterator logEntryIteratorFromReverseOffset(long offset) {
        return beginFromOffset(LegacyLogEntryLineIterator.seekBackwards(file, (int) offset))
    }

    /**
     * Offset indicates number of entries from the end in this case, not byte index.
     * @param offset
     * @return
     */
    @Override
    Iterator<LogEvent> iteratorFromReverseOffset(long offset) {
        return logEntryIteratorFromReverseOffset(offset)
    }
}

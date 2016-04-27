package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogEventIterator
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
    File file
    private String encoding
    RundeckLogFormat rundeckLogFormat
    private boolean detectedFormat
    private boolean detected
    private LogEventIterator iterator
    /**
     * Optional date for resolving legacy unspecific timestamps
     */
    Date referenceDate
    public FSStreamingLogReader(File file, String encoding, RundeckLogFormat rundeckLogFormat) {
        this.file = file
        this.encoding = encoding
        this.rundeckLogFormat=rundeckLogFormat
    }
    private void detectLegacyLogFile(){
        file.withReader('UTF-8') {reader->
            detectedFormat=RundeckLogFormat.detectFormat(reader.readLine())
            detected=true
        }
    }
    private LogEventIterator detectedIterator(FSFileLineIterator fsiter){
        if(!detected){
            detectLegacyLogFile()
        }
        if (detectedFormat) {
            return new LogEventLineIterator(fsiter, rundeckLogFormat)
        } else {
            def iterator = new LegacyLogEventLineIterator(fsiter)
            iterator.referenceDate = referenceDate ?: new Date(file.lastModified())
            return iterator
        }
    }
    private long detectedSeekBackwards(int offset){
        if (!detected) {
            detectLegacyLogFile()
        }

        if (detectedFormat) {
            return LogEventLineIterator.seekBackwards(file, (int) offset, rundeckLogFormat)
        } else {
            return LegacyLogEventLineIterator.seekBackwards(file, (int) offset)
        }
    }

    public long getTotalSize() {
        return file.length()
    }

    public Date getLastModified() {
        return new Date(file.lastModified())
    }

    @Override
    void openStream(Long offset) {
        if(null!=iterator){
            throw new IllegalStateException("Already open")
        }
        this.iterator=beginFromOffset(offset)
    }

    @Override
    void openStreamFromReverseOffset(Long offset) {
        if (null != iterator) {
            throw new IllegalStateException("Already open")
        }
        this.iterator= beginFromOffset(detectedSeekBackwards((int) offset))
    }

    private LogEventIterator beginFromOffset(long offset) {
        def raf = new FileInputStream(file)
        raf.channel.position(offset)
        def LogEventIterator iterator = detectedIterator(new FSFileLineIterator(raf, encoding))
        return iterator
    }

    @Override
    boolean hasNext() {
        if (null == iterator) {
            throw new IllegalStateException("Not open")
        }
        return iterator.hasNext()
    }

    @Override
    LogEvent next() {
        if (null == iterator) {
            throw new IllegalStateException("Not open")
        }
        return iterator.next()
    }

    @Override
    void remove() {
        if (null == iterator) {
            throw new IllegalStateException("Not open")
        }
        iterator.remove()
    }

    @Override
    void close() throws IOException {
        if (null == iterator) {
            throw new IllegalStateException("Not open")
        }
        iterator.close()
    }

    @Override
    boolean isComplete() {
        if (null == iterator) {
            throw new IllegalStateException("Not open")
        }
        return iterator.isComplete()
    }

    @Override
    long getOffset() {
        if (null == iterator) {
            throw new IllegalStateException("Not open")
        }
        return iterator.getOffset()
    }
}

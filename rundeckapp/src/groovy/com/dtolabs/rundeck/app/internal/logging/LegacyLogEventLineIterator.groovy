package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEventIterator
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.OffsetIterator
import com.dtolabs.rundeck.core.utils.Utility
import rundeck.services.ExecutionService

import java.text.ParseException
import java.text.SimpleDateFormat

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

/**
 * LegacyLogEventLineIterator.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/23/13 7:50 PM
 *
 * @deprecated
 */
class LegacyLogEventLineIterator implements LogEventIterator{
    OffsetIterator<String> iter
    private boolean complete
    private long offset
    private Deque<LogEvent> latest
    private Deque<Long> poslist
    private StringBuilder buf
    private Map<String,String> msgbuf
    private wasStarted=false
    private SimpleDateFormat fallbackFormat
    private SimpleDateFormat w3cDateFormat
    Date referenceDate
    private String referenceDateString
    public LegacyLogEventLineIterator(OffsetIterator<String> iter){
        this.iter=iter
        buf=new StringBuilder()
        offset=iter.offset
        complete=!iter.hasNext()
        latest = new ArrayDeque<LogEvent>()
        poslist = new ArrayDeque<Long>()
        poslist << iter.offset

        w3cDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        fallbackFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        w3cDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        fallbackFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private void checkStarted(){
        if (!wasStarted){
            readNextEntry()
            wasStarted = true
        }
    }
    private void readNextEntry(){
        while (!complete && iter.hasNext() && !latest) {
            parseLine(iter.next())
        }
    }
    public void setReferenceDate(Date value){
        referenceDate=value
        def format=new SimpleDateFormat("yyyy-MM-dd'T'", Locale.US);
        format.timeZone= TimeZone.getTimeZone("GMT")
        referenceDateString=format.format(referenceDate)+"%sZ"
    }
    private Date parseTime(String time){
        try {
            return w3cDateFormat.parse(time);
        } catch (ParseException e) {
        }
        //attempt legacy conversion
        if(referenceDateString){
            return w3cDateFormat.parse(String.format(referenceDateString,time));
        }
        return fallbackFormat.parse(time);
    }

    @Override
    boolean isComplete() {
        return complete
    }

    @Override
    boolean hasNext() {
        checkStarted()
        if (complete){
            return false
        }else{
            return latest.size() > 0
        }
    }

    @Override
    LogEvent next() {
        checkStarted()
        LogEvent next=latest.removeFirst()
        poslist.removeFirst()
        offset=poslist.peekFirst()
        if (!complete && latest.size()<1){
            readNextEntry()
        }
        return next
    }

    @Override
    void remove() {
        iter.remove()
    }

    @Override
    long getOffset() {
        return offset
    }

    @Override
    void close() throws IOException {
        iter.close()
    }

    /**
     * drain buffers into item, save offset
     */
    private void finishMessage(){
        def data = msgbuf
        buf = new StringBuilder()
        msgbuf = [:]
        poslist << iter.offset
        latest << new DefaultLogEvent(eventType: LogUtil.EVENT_TYPE_LOG, datetime: parseTime(data.time), message:
                data.mesg, metadata: data, loglevel: LogLevel.looseValueOf(data.level, LogLevel.NORMAL))
    }

    /**
     * Parse the log line, and if aany log entries are completed add them to the buffer
     * @param line
     */
    private void parseLine(String line){
        if (line =~ /^\^\^\^END\^\^\^/) {
            complete = true;
            if (msgbuf) {
                msgbuf.mesg += buf.toString()
                finishMessage()
            }
        }else if (line =~ /^\^\^\^/) {
            if (msgbuf) {
                msgbuf.mesg += buf.toString()
                finishMessage()
            }
            def temp = line.substring(3, line.length())
            def boolean full = false;
            if (temp =~ /\^\^\^$/ || temp == "") {
                if (temp.length() >= 3) {
                    temp = temp.substring(0, temp.length() - 3)
                }
                full = true
            }
            def arr = temp.split("\\|", ExecutionService.EXEC_FORMAT_SEQUENCE.size() + 1)
            if (arr.size() >= 3) {
                def time = arr[0].trim()
                def level = arr[1].trim()
                def mesg;
                def List<String> list = []
                list.addAll(Arrays.asList(arr))
                if (list.size() >= ExecutionService.EXEC_FORMAT_SEQUENCE.size()) {
                    if (list.size() > ExecutionService.EXEC_FORMAT_SEQUENCE.size()) {
                        //join last sections into one message.
                        def sb = new StringBuffer()
                        sb.append(list.get(ExecutionService.EXEC_FORMAT_SEQUENCE.size()))
                        for (def i = ExecutionService.EXEC_FORMAT_SEQUENCE.size() + 1; i < list.size(); i++) {
                            sb.append('|')
                            sb.append(list.get(i).trim())
                        }
                        mesg = sb.toString()
                    } else {
                        mesg = ''
                        list << mesg
                    }
                } else if (list.size() > 3 && list.size() < ExecutionService.EXEC_FORMAT_SEQUENCE.size()) {
                    def sb = new StringBuffer()
                    sb.append(list.get(2).trim())
                    for (def i = 3; i < list.size(); i++) {
                        sb.append('|')
                        sb.append(list.get(i).trim())
                    }
                    mesg = sb.toString()
                } else {
                    mesg = list[list.size() - 1].trim()
                }
                msgbuf = [time: time, level: level, mesg: mesg ]
                if (list.size() >= LegacyLogOutFormatter.EXEC_FORMAT_SEQUENCE.size()) {
                    for (int i = 2; i < list.size() - 1; i++) {
                        msgbuf[LegacyLogOutFormatter.EXEC_FORMAT_SEQUENCE[i]] = list[i].trim()
                    }
                }
                if (full) {
                    finishMessage()
                }
            }
        } else if (line =~ /\^\^\^$/ && msgbuf) {
            def temp = line.substring(0, line.length() - 3)
            buf << "\n" + temp
            msgbuf.mesg += buf.toString()
            finishMessage()
        } else {
            buf << "\n" + line
        }
    }

    private static final String lSep=System.getProperty("line.separator")
    /**
     * Seek backwards within the file to the specified entry index from the end.
     * @param file
     * @param count
     * @return
     */
    public static long seekBackwards(File file, int count){
        //NB: we search for log entry ending indicators, so we have to skip 2 of them
        //1: the final sigil, 2: the end of the final entry, before we can seek back the number of entries
        //this might skip over a single entry if the log is not complete at the end of the file
        long seek=Utility.seekBack(file, count + 2, "^^^${lSep}")
        if (seek>0){
            seek += "^^^${lSep}".getBytes("UTF-8").length
        }
        seek
    }
}

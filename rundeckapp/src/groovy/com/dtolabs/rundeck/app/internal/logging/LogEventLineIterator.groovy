package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogEventIterator
import com.dtolabs.rundeck.core.logging.OffsetIterator

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/21/13
 * Time: 6:04 PM
 */
class LogEventLineIterator implements LogEventIterator{
    private static final String lSep = System.getProperty("line.separator")
    OffsetIterator<String> iter
    private boolean complete
    private long offset
    private Deque<LogEvent> latest
    private Deque<Long> poslist
    private DefaultLogEvent eventBuf
    private wasStarted = false
    private LineLogFormat lineLogFormat
    boolean closed=false

    public LogEventLineIterator(OffsetIterator<String> iter, LineLogFormat format) {
        this.iter = iter
        offset = iter.offset
        complete = !iter.hasNext()
        latest = new ArrayDeque<LogEvent>()
        poslist = new ArrayDeque<Long>()
        poslist << iter.offset
        this.lineLogFormat=format
    }

    private void checkStarted() {
        if (!wasStarted) {
            readNextEntry()
            wasStarted = true
        }
    }

    private void readNextEntry() {
        if(closed) {
            throw new IllegalStateException("Closed")
        }
        while (!complete && iter.hasNext() && !latest) {
            parseLine(iter.next())
        }
    }

    @Override
    boolean isComplete() {
        return complete
    }

    @Override
    boolean hasNext() {
        checkStarted()
        if (complete) {
            return false
        } else {
            return latest.size() > 0
        }
    }

    @Override
    LogEvent next() {
        checkStarted()
        LogEvent next = latest.removeFirst()
        poslist.removeFirst()
        offset = poslist.peekFirst()
        if (!complete && latest.size() < 1) {
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

    /**
     * drain buffers into item, save offset
     */
    private void finishMessage(LogEvent event) {
        poslist << iter.offset
        latest << event
        eventBuf=null
    }
    /**
     * Parse the log line, and if aany log entries are completed add them to the buffer
     * @param line
     */
    private void parseLine(String line) {
        def LineLogFormat.FormatItem item = lineLogFormat.parseLine(line)
        if(item.lineComplete){
            if (!eventBuf && item.entry){
                eventBuf=new DefaultLogEvent(item.entry)
            }else if(!eventBuf){
                //no entry, skip it
                return
            }else if(item.partial){
                //merge any partial
                eventBuf.message+=item.partial
            }
            finishMessage(eventBuf)
        }else if(item.entry){
            if(eventBuf){
                finishMessage(eventBuf)
            }
            eventBuf= new DefaultLogEvent(item.entry)
        } else if (item.partial && eventBuf) {
            //merge any partial
            eventBuf.message += item.partial
        }else if(item.partial){
            //partial but no event
        }
        if(item.fileEnd){
            complete=true
        }
        if(item.fileStart){

        }
        if(item.invalid){
            //invalid
        }
    }

    /**
     * Seek backwards within the file to the specified entry index from the end.
     * @param file
     * @param count
     * @return
     */
    public static long seekBackwards(File file, int count, LineLogFormat format) {
        format.seekBackwards(file,count)
    }

    @Override
    void close() throws IOException {
        iter.close()
        closed=true
    }
}

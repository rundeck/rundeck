package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel

import java.text.SimpleDateFormat

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/21/13
 * Time: 2:03 PM
 */
class RundeckLogFormat implements OutputLogFormat, LineLogFormat{
    public static final String FORMAT_MIME= "text/x-rundeck-log-v2.0"
    SimpleDateFormat w3cDateFormat
    public RundeckLogFormat() {
        w3cDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        w3cDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public String getHead() {
        return "^^^${FORMAT_MIME}^^^\n";
    }

    public String getTail() {
        return '^^^END^^^';
    }

    @Override
    String outputBegin() {
        return getHead()
    }

    @Override
    String outputFinish() {
        return getTail()
    }

    /**
     * @param context data
     * @param message message
     *
     * @return
     */
    @Override
    String outputEvent(LogEvent entry) {
        def String dDate = w3cDateFormat.format(entry.datetime)
        String dMesg = entry.message ?: '';
        while (dMesg.endsWith('\r')) {
            dMesg = dMesg.substring(0, dMesg.length() - 1)
        }
        StringBuffer sb = new StringBuffer()
        sb.append('^^^')
        //TODO event type
        //date
        sb.append(dDate).append('|')
        //level
        sb.append(entry.logLevel)
        sb.append("|")

        //metadata
        def metadata = entry.metadata
        if(metadata){
            sb.append('{')
            def sort = metadata.keySet().sort()
            for (int i = 0; i < sort.size(); i++) {
                def key = sort[i]
                //todo allow encoded special chars
                if (i > 0) {
                    sb.append('|')
                }
                sb.append(key.replaceAll('[=|}]', ''))
                sb.append('=')
                sb.append(metadata[key].replaceAll('[|}]', ''))
            }

            sb.append('}')
        }

        sb.append("|")
        //mesg
        sb.append(dMesg.replaceAll(/([^\])/,'\\\\$1'))
        //end
        sb.append('^^^')

        return sb.toString()
    }

    static class RDFormatItem implements LineLogFormat.FormatItem {
        boolean fileEnd
        boolean lineComplete
        boolean fileStart
        boolean invalid
        LogEvent entry
        String partial
        String errorMessage
        @Override
        String toString() {
            if(invalid) {
                return "Invalid Line: " + errorMessage
            }else{
                return genToString()
            }
        }

        @Override
        public java.lang.String genToString() {
            return "RDFormatItem{" +
                    "fileEnd=" + fileEnd +
                    ", lineComplete=" + lineComplete +
                    ", fileStart=" + fileStart +
                    ", entry=" + entry +
                    ", partial='" + partial + '\'' +
                    '}';
        }
        static LineLogFormat.FormatItem error(String message){
            def item = new RDFormatItem()
            item.invalid=true
            item.errorMessage=message
            return item
        }
    }
    /**
     * Parse the log line, and if aany log entries are completed add them to the buffer
     * @param line
     */
    LineLogFormat.FormatItem parseLine(String line) {
        if (line == getTail()) {
            return new RDFormatItem(fileEnd: true)
        }else if (line == getHead()) {
            return new RDFormatItem(fileStart: true)
        } else if (line =~ /^\^\^\^/) {
            def temp = line.substring(3, line.length())
            def item= new RDFormatItem()
            def arr = temp.split("\\|", 2)

            if (arr.length != 3) {
                return RDFormatItem.error( "Expected 3 sections: "+arr.length)
            }
            Date time = w3cDateFormat.parse(arr[0])
            LogLevel level = LogLevel.valueOf(arr[1])
            def rest=arr[2]
            def meta=[:]
            if(rest.startsWith('{')){
                //parse meta
                arr=rest.substring(0).split('}|',1)
                if (arr.length != 2) {
                    return RDFormatItem.error("Metadata end section not seen")
                }
                def metastring = arr[0]
                rest=arr[1]
                metastring.split('\\|').each {str->
                    def metarr=str.split('=',1)
                    if(metarr.length==2 && metarr[0]){
                        meta.put(metarr[0],metarr[1])
                    }else{
                        return RDFormatItem.error("Metadata section invalid: "+str)
                    }
                }
            }else{
                arr = rest.substring(0).split('|', 1)
                if (arr.length != 2) {
                    return RDFormatItem.error("Expected message section")
                }
                rest = arr[1]
            }

            if(rest =~ /^\^\^$/){
                rest = rest.substring(0, temp.length() - 3)
                item.lineComplete = true
            }
            def message= decodeLog(rest)
            item.entry = new DefaultLogEvent(logLevel: level, datetime: time, message: message, metadata: meta)
            return item
        } else if (line =~ /\^\^\^$/ ) {
            def temp = line.substring(0, line.length() - 3)
            temp= decodeLog(temp)
            return new RDFormatItem(partial: temp + '\n', lineComplete: true)
        } else {
            return new RDFormatItem(partial: decodeLog(line) + '\n', lineComplete: false)
        }
    }

    private String decodeLog(String rest) {
        rest.replaceAll(/\([^\])/, '$1')
    }

}

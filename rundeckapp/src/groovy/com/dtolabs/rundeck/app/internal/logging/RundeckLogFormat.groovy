package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.utils.Utility

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/21/13
 * Time: 2:03 PM
 */
class RundeckLogFormat implements OutputLogFormat, LineLogFormat {
    public static final String FORMAT_MIME = "text/x-rundeck-log-v2.0"
    public static final String DELIM = '^'
    public static final String FILE_START = DELIM + FORMAT_MIME + DELIM
    public static final String FILE_END = DELIM + "END" + DELIM
    public static final String DEFAULT_EVENT_TYPE= LogUtil.EVENT_TYPE_LOG
    public static final LogLevel DEFAULT_LOG_LEVEL= LogLevel.NORMAL

    static final char BACKSLASH = '\\' as char
    private static final ThreadLocal<DateFormat> w3cDateFormat = new ThreadLocal<DateFormat>() {
        protected DateFormat initialValue() {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            return fmt;
        }
    };

    public RundeckLogFormat() {

    }

    public String getHead() {
        return FILE_START ;
    }

    public String getTail() {
        return FILE_END ;
    }

    @Override
    String outputBegin() {
        return getHead()
    }

    @Override
    String outputFinish() {
        return getTail()
    }

    static boolean detectFormat(String firstLine){
        return firstLine?.startsWith(FILE_START)
    }
    /**
     * @param context data
     * @param message message
     *
     * @return
     */
    @Override
    String outputEvent(LogEvent entry) {
        def String date = w3cDateFormat.get().format(entry.datetime)
        String dMesg = entry.message ?: '';
        while (dMesg.endsWith('\r')) {
            dMesg = dMesg.substring(0, dMesg.length() - 1)
        }
        StringBuffer sb = new StringBuffer()
        sb.append(DELIM)
        //date
        sb.append(date).append('|')
        sb.append(entry.eventType&&entry.eventType!=DEFAULT_EVENT_TYPE?entry.eventType.replaceAll('\\|',''):'').append('|')
        //level
        sb.append(entry.loglevel==DEFAULT_LOG_LEVEL?'':entry.loglevel).append("|")

        //metadata
        def metadata = entry.metadata
        if (metadata) {
            sb.append('{')
            def sort = metadata.keySet().sort()
            for (int i = 0; i < sort.size(); i++) {
                def key = sort[i]
                if(null==metadata[key]){
                    continue
                }
                if (i > 0) {
                    sb.append('|')
                }
                sb.append(backslashEscape(key,'=|}'))
                sb.append('=')
                sb.append(backslashEscape(metadata[key],'=|}'))
            }

            sb.append('}')
        }

        sb.append("|")
        //mesg
        sb.append(backslashEscape(dMesg, DELIM))
        //end
        sb.append(DELIM)

        return sb.toString()
    }

    static String backslashEscape(String dMesg, String chars) {
        dMesg ? dMesg.replaceAll('([\\\\' + chars + '])', '\\\\$1') : ''
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
            if (invalid) {
                return "Invalid Line: " + errorMessage
            } else {
                return genToString()
            }
        }

        public java.lang.String genToString() {
            return "RDFormatItem{" +
                    "fileEnd=" + fileEnd +
                    ", lineComplete=" + lineComplete +
                    ", fileStart=" + fileStart +
                    ", entry=" + entry +
                    ", partial='" + partial + '\'' +
                    '}';
        }

        static LineLogFormat.FormatItem error(String message) {
            def item = new RDFormatItem()
            item.invalid = true
            item.errorMessage = message
            return item
        }
    }
    /**
     * Parse the log line, and if aany log entries are completed add them to the buffer
     * @param line
     */
    LineLogFormat.FormatItem parseLine(String line) {
        if (line == FILE_END) {
            return new RDFormatItem(fileEnd: true)
        } else if (line == FILE_START) {
            return new RDFormatItem(fileStart: true)
        } else if (line.startsWith(DELIM)) {
            def temp = line.substring(DELIM.length())
            def item = new RDFormatItem()
            def arr = temp.split("\\|", 4)

            if (arr.length != 4) {
                return RDFormatItem.error("Expected 4 sections: " + arr.length)
            }
            Date time = w3cDateFormat.get().parse(arr[0])
            String eventType = arr[1] ?: DEFAULT_EVENT_TYPE
            LogLevel level = arr[2]?LogLevel.valueOf(arr[2]):DEFAULT_LOG_LEVEL
            def rest = arr[3]
            def meta = [:]
            if (rest.startsWith('{')) {
                //parse meta
                rest = rest.substring(1)
                def done = false
                while (!done) {
                    def (key, delim, newrest) = decodeMetaKey(rest)
                    if (!delim) {
                        return RDFormatItem.error("Meta section invalid: " + rest)
                    }
                    rest = newrest
                    def (val, delim2, newrest2) = decodeMetaValue(rest)
                    if (!delim2) {
                        return RDFormatItem.error("Meta section invalid: " + rest)
                    }
                    if (delim2 == '}') {
                        done = true
                    }
                    meta.put(key, val)
                    rest = newrest2
                }
                if (rest.startsWith('|')) {
                    rest = rest.substring(1)
                } else {
                    return RDFormatItem.error("Expected message section: " + rest)
                }
            } else {
                arr = rest.substring(0).split('|', 2)
                if (arr.length != 2) {
                    return RDFormatItem.error("Expected message section: " + rest)
                }
                rest = arr[1]
            }

            def (message, done) = decodeLog(rest)
            item.lineComplete = done?true:false
            item.entry = new DefaultLogEvent(loglevel: level, datetime: time, message: message, metadata: meta,eventType: eventType)
            return item
        } else {
            def (temp, done) = decodeLog(line)
            return new RDFormatItem(partial: temp + '\n', lineComplete: done ? true : false)
        }
    }

    long seekBackwards(File file, int count) {
        //NB: we search for log entry ending indicators, so we have to skip 2 of them
        //1: the final sigil, 2: the end of the final entry, before we can seek back the number of entries
        //this might skip over a single entry if the log is not complete at the end of the file
        String lSep = System.getProperty("line.separator")
        def seek = Utility.seekBack(file, count + 2, DELIM + lSep)
        if (seek > 0) {
            seek += "^${lSep}".getBytes("UTF-8").length
        }
        seek
    }

    static List decodeMetaKey(String input) {
        def (text, done, rest) = unescape(input, BACKSLASH, '=|}\\', '=')
        return [text, done, rest]
    }

    static List decodeMetaValue(String input) {
        def (text, done, rest) = unescape(input, BACKSLASH, '=|}\\', ['|', '}'] as String[])
        return [text, done, rest]
    }

    static List decodeLog(String input) {
        def (text, done, rest) = unescape(input, BACKSLASH, '^\\', DELIM)
        return [text, done]
    }
    /**
     * unescape a string, returning the result, whether the delimiter was seen, and any remaining string
     * @param input input stream
     * @param escapeChar the escape char
     * @param delimiter String delimiter where parsing should stop
     * @return List of: String unescaped, Boolean isDone, String remaining
     */
    static List unescape(String input, char escapeChar, String delimiter) {
        return unescape(input, escapeChar, delimiter + escapeChar, delimiter)
    }
    /**
     * unescape a string, returning the result, whether the delimiter was seen, and any remaining string
     * @param input input stream
     * @param escapeChar the escape char
     * @param validEscaped String containing chars allowed to be escaped
     * @param delimiter String delimiter where parsing should stop
     * @return List of: String unescaped, Boolean isDone, String remaining
     */
    static List unescape(String input, char escapeChar, String validEscaped, String delimiter) {
        return unescape(input, escapeChar, validEscaped, [delimiter] as String[])
    }
    /**
     * unescape a string, returning the result, whether the delimiter was seen, and any remaining string
     * @param input input stream
     * @param escapeChar the escape char
     * @param validEscaped String containing chars allowed to be escaped
     * @param delimiter array of delimiters indicating where parsing should stop
     * @return List of: String unescaped, Boolean delimiterReached, String remaining
     */
    static List unescape(String input, char escapeChar, String validEscaped, String[] delimiter) {
        def newline = new StringBuilder()
        def done = false
        def doneDelimiter = null
        def escaped = false
        int[] delimX = new int[delimiter.length]
        char[][] delimChars = new char[delimiter.length][]
        for (int z = 0; z < delimiter.length; z++) {
            delimX[z] = 0
            delimChars[z] = delimiter[z].toCharArray()
        }
        char[] allowEscaped = validEscaped.toCharArray()
        char[] array = input.toCharArray()
        int i;
        for (i = 0; i < array.length && !done; i++) {
            char c = array[i]
            switch (c) {
                case escapeChar:
                    if (escaped) {
                        newline.append(escapeChar)
                        escaped = false
                    } else {
                        escaped = true
                    }
                    for (int z = 0; z < delimiter.length; z++) {
                        delimX[z] = 0
                    }
                    break
                case allowEscaped:
                    if (escaped) {
                        newline.append(c)
                        escaped = false
                        for (int z = 0; z < delimiter.length; z++) {
                            delimX[z] = 0
                        }
                    } else {
                        boolean delimFound = false
                        for (int z = 0; z < delimX.length; z++) {
                            if (c == delimChars[z][delimX[z]]) {
                                delimX[z]++
                                delimFound = true
                                if (delimX[z] == delimChars[z].length) {
                                    doneDelimiter = delimiter[z]
                                    done = true
                                    break
                                }
                            }
                        }
                        if (!delimFound) {
                            //append
                            newline.append(c)
                        }
                    }
                    break
                default:
                    if (escaped) {
                        newline << escapeChar
                        escaped = false
                    }
                    boolean partialSeen = false
                    for (int z = 0; z < delimiter.length; z++) {
                        if (delimX[z] > 0 && !partialSeen) {
                            //partial delimiter seen before unescaped char
//                            throw new IllegalStateException("Unexpected partial delimiter seen: "+(new String(delimiter[z])))
                            newline.append(delimChars[z], 0, delimX[z])
                            partialSeen = true
                        }
                        delimX[z] = 0
                    }
                    newline.append(c)
            }
        }
        if (!done) {
            for (int z = 0; z < delimiter.length; z++) {
                if (delimX[z] > 0) {
                    //partial delimiter seen before unescaped char
//                            throw new IllegalStateException("Unexpected partial delimiter seen: "+(new String(delimiter[z])))
                    newline.append(delimChars[z], 0, delimX[z])
                    break
                }
            }
        }
        String rest = i <= array.length - 1 ? new String(array, i, array.length - i) : null

        return [newline.toString(), doneDelimiter, rest]
    }

}

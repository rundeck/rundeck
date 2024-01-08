/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.core.logging.internal;

import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;
import com.dtolabs.rundeck.core.logging.LogUtil;
import com.dtolabs.rundeck.core.utils.Utility;
import com.google.common.base.Predicate;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/21/13
 * Time: 2:03 PM
 */
public class RundeckLogFormat implements OutputLogFormat, LineLogFormat {
    private static final Logger log = LoggerFactory.getLogger(RundeckLogFormat.class);

    public static final String FORMAT_MIME = "text/x-rundeck-log-v2.0";
    public static final String DELIM = "^";

    public static final String PIPE = String.valueOf('|');
    public static final String FILE_START = DELIM + FORMAT_MIME + DELIM;
    public static final String FILE_END = DELIM + "END" + DELIM;
    public static final String DEFAULT_EVENT_TYPE= LogUtil.EVENT_TYPE_LOG;
    public static final LogLevel DEFAULT_LOG_LEVEL= LogLevel.NORMAL;

    static final char BACKSLASH = '\\';
    private static final ThreadLocal<DateFormat> w3cDateFormat = new ThreadLocal<DateFormat>() {
        protected DateFormat initialValue() {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
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
    public String outputBegin() {
        return getHead();
    }

    @Override
    public String outputFinish() {
        return getTail();
    }

    static boolean detectFormat(String firstLine){
        return firstLine != null && firstLine.startsWith(FILE_START);
    }

    @Override
    public String outputEvent(LogEvent entry) {
        String date = w3cDateFormat.get().format(entry.getDatetime());
        String dMesg = entry.getMessage() != null ? entry.getMessage() : "";
        while (dMesg.endsWith("\\r")) {
            dMesg = dMesg.substring(0, dMesg.length() - 1);
        }
        StringBuffer sb = new StringBuffer();
        sb.append(DELIM);
        //date
        sb.append(date).append('|');
        String et = entry.getEventType();
        sb.append(et !=null&&!et.equals(DEFAULT_EVENT_TYPE)?et.replaceAll("\\|",""):"").append(PIPE);
        //level
        sb.append(entry.getLoglevel().equals(DEFAULT_LOG_LEVEL)?"": entry.getLoglevel()).append(PIPE);

        //metadata
        if (entry.getMetadata() != null && !entry.getMetadata().isEmpty()) {
            sb.append('{');
            List<String> sort = new ArrayList<>(entry.getMetadata().keySet());
            Collections.sort(sort);
            for (int i = 0; i < sort.size(); i++) {
                String key = sort.get(i);
                if(null==entry.getMetadata().get(key)){
                    continue;
                }
                if (i > 0) {
                    sb.append('|');
                }
                sb.append(backslashEscape(key,"=|}"));
                sb.append('=');
                sb.append(backslashEscape(entry.getMetadata().get(key),"=|}"));
            }

            sb.append('}');
        }

        sb.append("|");
        //mesg
        sb.append(backslashEscape(dMesg, DELIM));
        //end
        sb.append(DELIM);

        return sb.toString();
    }

    static String backslashEscape(String dMesg, String chars) {
        return dMesg != null ? dMesg.replaceAll("([\\\\" + chars + "])", "\\\\$1") : "";
    }

    public static class RDFormatItem implements LineLogFormat.FormatItem {
        boolean fileEnd;
        boolean lineComplete;
        boolean fileStart;
        boolean invalid;
        LogEvent entry;
        String partial;
        String errorMessage;

        RDFormatItem() {}

        RDFormatItem(String partial, boolean lineComplete) {
            this.lineComplete = lineComplete;
            this.partial = partial;
        }

        public static FormatItem asFileEnd() {
            RDFormatItem i  = new RDFormatItem();
            i.fileEnd = true;
            return i;
        }

        public static FormatItem asFileStart() {
            RDFormatItem i  = new RDFormatItem();
            i.fileStart = true;
            return i;
        }

        @Override
        public String toString() {
            if (invalid) {
                return "Invalid Line: " + errorMessage;
            } else {
                return genToString();
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
            RDFormatItem item = new RDFormatItem();
            item.invalid = true;
            item.errorMessage = message;
            return item;
        }

        public boolean getFileEnd() {
            return fileEnd;
        }

        public boolean getLineComplete() {
            return lineComplete;
        }

        public boolean getFileStart() {
            return fileStart;
        }

        @Override
        public boolean isInvalid() {
            return invalid;
        }

        @Override
        public LogEvent getEntry() {
            return entry;
        }

        @Override
        public String getPartial() {
            return partial;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
    /**
     * Parse the log line, and if aany log entries are completed add them to the buffer
     * @param line
     */
    public LineLogFormat.FormatItem parseLine(String line) {
        if (FILE_END.equals(line)) {
            return RDFormatItem.asFileEnd();
        } else if (FILE_START.equals(line)) {
            return RDFormatItem.asFileStart();
        } else if (line.startsWith(DELIM)) {
            String temp = line.substring(DELIM.length());
            if (StringUtils.isBlank(temp)) {
                //delim alone
                return new RDFormatItem("", true);
            }
            RDFormatItem item = new RDFormatItem();
            String[] arr = temp.split("\\|", 4);

            if (arr.length != 4) {
                return RDFormatItem.error("Expected 4 sections: " + arr.length);
            }
            Date time = null;
            try {
                time = w3cDateFormat.get().parse(arr[0]);
            } catch(ParseException pe) {
                throw new RuntimeException("Unabled to parse date",pe);
            }
            String eventType = StringUtils.isNotBlank(arr[1]) ? arr[1] : DEFAULT_EVENT_TYPE;
            LogLevel level = StringUtils.isNotBlank(arr[2])?LogLevel.valueOf(arr[2]):DEFAULT_LOG_LEVEL;
            String rest = arr[3];
            Map<String, String> meta = new HashMap<>();
            if (rest.startsWith("{")) {
                //parse meta
                rest = rest.substring(1);
                boolean done = false;
                while (!done) {
                    UnescapedData decodedKey = decodeMetaKey(rest);
                    if (!decodedKey.hasReachedDelimiter()) {
                        return RDFormatItem.error("Meta section invalid: " + rest);
                    }
                    rest = decodedKey.remaining;
                    UnescapedData decodedVal = decodeMetaValue(rest);
                    //def (String val, delim2, newrest2) = [decodedVal[0] as String, decodedVal[1], decodedVal[2] as String]
                    if (!decodedVal.hasReachedDelimiter()) {
                        return RDFormatItem.error("Meta section invalid: " + rest);
                    }
                    if (decodedVal.delimiterReached.equals("}")) {
                        done = true;
                    }
                    meta.put(decodedKey.unescaped, decodedVal.unescaped);
                    rest = decodedVal.remaining;
                }
                if (rest.startsWith(PIPE)) {
                    rest = rest.substring(1);
                } else {
                    return RDFormatItem.error("Expected message section: " + rest);
                }
            } else {
                arr = rest.split(PIPE, 2);
                if (arr.length != 2) {
                    return RDFormatItem.error("Expected message section: " + rest);
                }
                rest = arr[1];
            }

            UnescapedData decoded = decodeLog(rest);
            //def (message, done) = [decoded[0] as String, decoded[1]]
            item.lineComplete = decoded.hasReachedDelimiter();
            item.entry = new DefaultLogEvent(level,
                    time,
                    decoded.unescaped + (decoded.hasReachedDelimiter() ? "" : '\n'),
                    eventType,
                    meta
            );
            return item;
        } else {
            UnescapedData decoded = decodeLog(line);
            //def (String temp, done) = [decoded[0] as String, decoded[1]];
            return new RDFormatItem(decoded.unescaped + '\n', decoded.hasReachedDelimiter());
        }
    }

    public long seekBackwards(File file, int count) {
        //seek backwards to log entry ending strings, using the LogMessagePositionTester to
        //verify that the following line is a log message, not some other entry type
        String lSep = System.getProperty("line.separator");
        long seek = -1;
        try {
            int markerLen = ("^"+lSep).getBytes("UTF-8").length;
            seek = Utility.seekBack(file, count, DELIM + lSep, new LogMessageBegin());
            if (seek > 0) {
                seek += markerLen;
            }
        } catch(IOException iex) {
            log.error("Unable to seek back",iex);
        }
        return seek;
    }
    /**
     * verifies that the input stream is positioned at the start of a log message entry
     */
    static class LogMessageBegin implements Predicate<InputStream> {
        boolean firstTime = true;
        /**
         * Return true if the stream position is at a log message entry
         * @param stream
         * @return
         */
        public boolean apply(final InputStream stream) {
            if (firstTime) {
                //first line ending found will never have a valid
                //log entry following it, even if the prefix check is correct
                //because it means the log entry isn't complete
                firstTime = false;
                return false;
            }
            String prefix = "^DDDD-DD-DDTDD:DD:DDZ|";
            String sample = prefix + DEFAULT_EVENT_TYPE + PIPE;
            byte[] buff = new byte[sample.length()];
            int len = -1;
            try {
                len = stream.read(buff);
            } catch (IOException ex) {
                throw new RuntimeException("Stream could not be read",ex);
            }
            if (len != buff.length) {
                return false;
            }

            //simple verification that date prefix matches
            String s = new String(buff);
            for (int i = 0; i < prefix.length(); i++) {
                if (prefix.charAt(i) != 'D' && prefix.charAt(i) != s.charAt(i)) {
                    return false;
                }
            }

            //match log message with explicit or elided type string
            String sub = s.substring(prefix.length());
            return sub.startsWith(PIPE) || sub.startsWith(DEFAULT_EVENT_TYPE + PIPE);
        }
    }

    static UnescapedData decodeMetaKey(String input) {
        return unescape(input, BACKSLASH, "=|}\\", "=");
    }

    static UnescapedData decodeMetaValue(String input) {
        return unescape(input, BACKSLASH, "=|}\\", PIPE, "}");
    }


    static UnescapedData decodeLog(String input) {
        return unescape(input, BACKSLASH, "^\\", DELIM);
    }

    /**
     * unescape a string, returning the result, whether the delimiter was seen, and any remaining string
     * @param input input stream
     * @param escapeChar the escape char
     * @param delimiter String delimiter where parsing should stop
     * @return List of: String unescaped, Boolean isDone, String remaining
     */
    static UnescapedData unescape(String input, char escapeChar, String delimiter) {
        return unescape(input, escapeChar, delimiter + escapeChar, delimiter);
    }

    /**
     * unescape a string, returning the result, whether the delimiter was seen, and any remaining string
     * @param input input stream
     * @param escapeChar the escape char
     * @param validEscaped String containing chars allowed to be escaped
     * @param delimiter array of delimiters indicating where parsing should stop
     * @return List of: String unescaped, Boolean delimiterReached, String remaining
     */
    static UnescapedData unescape(String input, char escapeChar, String validEscaped, String... delimiter) {
        StringBuilder newline = new StringBuilder();
        boolean done = false;
        String doneDelimiter = null;
        boolean escaped = false;
        int[] delimX = new int[delimiter.length];
        char[][] delimChars = new char[delimiter.length][];
        for (int z = 0; z < delimiter.length; z++) {
            delimX[z] = 0;
            delimChars[z] = delimiter[z].toCharArray();
        }

        List<Character> allowEscaped = validEscaped.chars().mapToObj(c->(char)c).collect(Collectors.toList());
        char[] array = input.toCharArray();
        int i;
        for (i = 0; i < array.length && !done; i++) {
            char c = array[i];
            if(c == escapeChar) {
                if (escaped) {
                    newline.append(escapeChar);
                    escaped = false;
                }
                else {
                    escaped = true;
                }
                for (int z = 0; z < delimiter.length; z++) {
                    delimX[z] = 0;
                }
            } else if(allowEscaped.contains(c)) {
                if (escaped) {
                    newline.append(c);
                    escaped = false;
                    for (int z = 0; z < delimiter.length; z++) {
                        delimX[z] = 0;
                    }
                }
                else {
                    boolean delimFound = false;
                    for (int z = 0; z < delimX.length; z++) {
                        if (c == delimChars[z][delimX[z]]) {
                            delimX[z]++;
                            delimFound = true;
                            if (delimX[z] == delimChars[z].length) {
                                doneDelimiter = delimiter[z];
                                done = true;
                                break;
                            }
                        }
                    }
                    if (!delimFound) {
                        //append
                        newline.append(c);
                    }
                }
            }
            else {
                if (escaped) {
                    newline.append(escapeChar);
                    escaped = false;
                }
                boolean partialSeen = false;
                for (int z = 0; z < delimiter.length; z++) {
                    if (delimX[z] > 0 && !partialSeen) {
                        //partial delimiter seen before unescaped char
//                            throw new IllegalStateException("Unexpected partial delimiter seen: "+(new String(delimiter[z])))
                        newline.append(delimChars[z], 0, delimX[z]);
                        partialSeen = true;
                    }
                    delimX[z] = 0;
                }
                newline.append(c);
            }
        }
        if (!done) {
            for (int z = 0; z < delimiter.length; z++) {
                if (delimX[z] > 0) {
                    //partial delimiter seen before unescaped char
//                            throw new IllegalStateException("Unexpected partial delimiter seen: "+(new String(delimiter[z])))
                    newline.append(delimChars[z], 0, delimX[z]);
                    break;
                }
            }
        }
        String rest = i <= array.length - 1 ? new String(array, i, array.length - i) : null;

        return new UnescapedData(newline.toString(), doneDelimiter, rest);
    }

    static class UnescapedData {
        public final String unescaped;
        public final String delimiterReached;
        public final String remaining;

        public UnescapedData(final String unescaped, final String delimiterReached, final String remaining) {
            this.unescaped = unescaped;
            this.delimiterReached = delimiterReached;
            this.remaining = remaining;
        }

        public boolean hasReachedDelimiter() {
            return delimiterReached != null;
        }

    }

}

/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* PartialLineBuffer.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/21/11 4:27 PM
* 
*/
package com.dtolabs.rundeck.core.utils;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * A Line oriented String buffer which can also return the last incomplete line read
 */
public class PartialLineBuffer {
    private ArrayList<String> lines = new ArrayList<String>();
    String partialLine = null;
    /**
     * new partial line data exists since last read
     */
    boolean newdata = false;

    private void appendString(final String str, final boolean linending) {
        if (linending) {
            if (null != partialLine) {
                if (partialLine.endsWith("\r") && str.equals("")) {
                    lines.add(partialLine.substring(0, partialLine.length() - 1));
                } else if (partialLine.endsWith("\r")) {
                    lines.add(partialLine.substring(0, partialLine.length() - 1));
                    lines.add(str);
                } else {
                    lines.add(partialLine + str);
                }
                partialLine = null;
                newdata = false;
            } else {
                lines.add(str);
            }
        } else {
            if (null != partialLine) {
                if (partialLine.endsWith("\r")) {
                    lines.add(partialLine.substring(0, partialLine.length() - 1));
                    if ("".equals(str)) {
                        partialLine = null;
                        newdata = false;
                    } else {
                        partialLine = str;
                        newdata = true;
                    }
                } else {
                    partialLine += str;
                    newdata = true;
                }
            } else {
                partialLine = str;
                newdata = true;
            }

        }
    }

    char[] cbuf = new char[1024];

    /**
     * Read some chars from a reader, and return the number of characters read
     *
     * @param reader input reader
     *
     * @return characters read
     *
     * @throws IOException if thrown by underlying read action
     */
    public int read(final Reader reader) throws IOException {
        final int c = reader.read(cbuf);
        if (c > 0) {
            addData(cbuf, 0, c);
        }
        return c;
    }

    /**
     * Add character data to the buffer
     * @param data data
     * @param off offset
     * @param size length
     */
    public void addData(final char[] data, final int off, final int size) {
        if(size<1){
            return;
        }
        final String str = new String(data, off, size);
        if (str.contains("\n")) {
            final String[] lines = str.split("\\r?\\n", -1);
            for (int i = 0 ; i < lines.length - 1 ; i++) {
                appendString(lines[i], true);
            }
            if (lines.length > 0) {
                final String last = lines[lines.length - 1];
                if ("".equals(last)) {
                    //end of previous line
                } else {
                    appendString(last, false);
                }
            }
        } else if (str.contains("\r")) {
            final String[] lines = str.split("\\r", -1);
            for (int i = 0 ; i < lines.length - 2 ; i++) {
                appendString(lines[i], true);
            }
            //last two split strings
            //a: 1,1: yes,no
            //b: 0,1: yes,no
            //c: 0,0: yes(pass),no
            //d: 1,0: yes(pass),
            if (lines.length >= 2) {
                final String last2 = lines[lines.length - 2];

                final String last = lines[lines.length - 1];
                if (!"".equals(last)) {
                    appendString(last2, true);
                    appendString(last, false);
                } else {
                    //pass \r for later \r\n resolution
                    appendString(last2 + "\r", false);
                }
            }
        } else {
            appendString(str, false);
        }
    }

    /**
     * @return the last partial line read and mark it
     */
    public String getPartialLine() {
        return getPartialLine(true);
    }

    /**
     * Unmark any partial line so it can be read again
     */
    public void unmarkPartial(){
        if(null!=partialLine) {
            newdata = true;
        }
    }
    /**
     * Clear the partial fragment string
     */
    public void clearPartial(){
        partialLine=null;
        newdata = false;
    }

    /**
     * @return the last partial line read, optionally marking it as already read. Subsequent calls with mark set to true
     * will return null if there as been no new data.
     * @param mark true to mark it
     */
    public String getPartialLine(final boolean mark) {
        if (mark && newdata) {
            newdata = false;
            if (partialLine.endsWith("\r")) {
                return partialLine.substring(0, partialLine.length() - 1);
            }
            return partialLine;
        } else if (mark && !newdata) {
            return null;
        } else {
            if (null != partialLine && partialLine.endsWith("\r")) {
                return partialLine.substring(0, partialLine.length() - 1);
            }
            return partialLine;
        }
    }

    /**
     * @return Read the next line if any, and remove it from the buffer.
     */
    public String readLine() {
        if (lines.size() > 0) {
            return lines.remove(0);
        } else {
            return null;
        }
    }

    /**
     * @return  the line buffer
     */
    public List<String> getLines() {
        return lines;
    }

}

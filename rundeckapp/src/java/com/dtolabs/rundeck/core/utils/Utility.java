/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* Utility.java
* 
* User: greg
* Created: Jan 24, 2008 10:06:00 AM
* $Id$
*/
package com.dtolabs.rundeck.core.utils;

import com.google.common.base.Predicate;

import java.io.*;


/**
 * Utility java code
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class Utility {

    /**
     * seekBack searches backwards for certain markers in a file, and returns position of the final marker found.
     * count specifies how many markers to search for.  if the search reaches the beginning of the file without finding
     * all of the markers, then 0 is returned.
     * @param f the file to search
     * @param count number of markers to find
     * @param marker text string marker
     * @return location of marker number <i>count</i> found from the end of the file, or 0
     * @throws IOException
     */
    public static long seekBack(File f, int count, String marker) throws IOException {
        return seekBack(f, count, marker, null);
    }
    /**
     * seekBack searches backwards for certain markers in a file, and returns position of the final marker found.
     * count specifies how many markers to search for.  if the search reaches the beginning of the file without finding
     * all of the markers, then 0 is returned.
     * @param f the file to search
     * @param count number of markers to find
     * @param marker text string marker
     * @param validity predicate to test whether the stream is at a valid position, or null
     * @return location of marker number <i>count</i> found from the end of the file, or 0
     * @throws IOException
     */
    public static long seekBack(File f, int count, String marker, Predicate<InputStream> validity) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        long size = fis.getChannel().size();
        long pos = size;
        long foundpos = -1;
        //seek backwards for *count occurrences of the marker
        pos = pos - marker.length();
        boolean done = false;
        char[] comp = marker.toCharArray();
        byte[] buf = new byte[marker.length()];
        int matchcount = 0;
        while (matchcount < count && !done && pos >= 0) {
            fis.getChannel().position(pos);
            int r = fis.read(buf);
            if (r == marker.length()) {
                //compare contents of buf with comparison, and cycle forward if marker > 1 char length
                int cycle = 0;
                boolean bmatch = false;
                int j = 0;
                for (cycle = 0; cycle < marker.length(); cycle++) {
                    boolean cmatch = true;
                    for (j = 0; j < (buf.length - cycle); j++) {
                        if (buf[j] != (byte) comp[j + cycle]) {
                            cmatch = false;
                            break;
                        }
                    }
                    if (cmatch) {
                        bmatch = true;
                        break;
                    }
                }
                if (bmatch) {
                    //matched the buffer with the comparison at point 'cycle'.
                    //if cycle is 0, then  found one count. otherwise, backup
                    if (0 == cycle) {
                        if (null == validity || validity.apply(fis)) {
                            matchcount++;
                            foundpos = pos;
                        }
                        pos -= marker.length();
                    } else {
                        pos -= cycle;
                    }
                } else {
                    pos -= marker.length();
                }
            } else {
//                System.err.println("unable to read " + marker.length() + " bytes...");
                done = true;
            }
        }
//        System.err.println("matchcount > count? "+(matchcount > count)+", done: "+done+", pos: "+pos);
        if(pos<=0){
//            System.err.println("setting foundpos to 0");
            foundpos=0;
        }
        fis.close();
        return foundpos;
    }

}

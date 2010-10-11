/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* Streams.java
* 
* User: greg
* Created: Feb 4, 2008 4:06:55 PM
* $Id$
*/
package com.dtolabs.utils;

import org.apache.tools.ant.types.FilterSet;

import java.io.*;


/**
 * Streams utility class for reading and writing streams
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class Streams {
    /**
     * Read the data from the input stream and copy to the outputstream.
     *
     * @param in  inputstream
     * @param out outpustream
     *
     * @throws java.io.IOException if thrown by underlying io operations
     */
    public static void copyStream(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[10240];
        int c;
        c = in.read(buffer);
        while (c >= 0) {
            if (c > 0) {
                out.write(buffer, 0, c);
            }
            c = in.read(buffer);
        }
    }

    /**
     * Read the data from the input stream and write to the outputstream, filtering with an Ant FilterSet.
     *
     * @param in  inputstream
     * @param out outputstream
     * @param set FilterSet to use
     *
     * @throws java.io.IOException if thrown by underlying io operations
     */
    public static void copyStreamWithFilterSet(final InputStream in, final OutputStream out, final FilterSet set)
        throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        String lSep = System.getProperty("line.separator");
        String line = reader.readLine();
        while (null != line) {
            writer.write(set.replaceTokens(line));
            writer.write(lSep);
            line = reader.readLine();
        }
        writer.flush();
    }
}

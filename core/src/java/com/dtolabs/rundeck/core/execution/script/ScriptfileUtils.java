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

package com.dtolabs.rundeck.core.execution.script;

import com.dtolabs.rundeck.core.common.Framework;

import java.io.*;

/**
 * Utility methods for writing temp files for scripts and setting file permissions.
 */
public class ScriptfileUtils {

    /**
     * Write an inputstream to a FileWriter
     * @param input input stream
     * @param writer writer
     * @throws IOException if an error occurs
     */
    private static void writeStream(final InputStream input, final FileWriter writer) throws IOException {
        final InputStreamReader inStream = new InputStreamReader(input);
        final BufferedReader inbuff = new BufferedReader(inStream);
        String inData;
        final String linesep = System.getProperty("line.separator");
        while ((inData = inbuff.readLine()) != null) {
            writer.write(inData);
            writer.write(linesep);
        }
        inbuff.close();
    }
    
    /**
     * Copy from a Reader to a FileWriter
     * @param reader reader
     * @param writer writer
     * @throws IOException if an error occurs
     */
    private static void writeReader(final Reader reader, final FileWriter writer) throws IOException {
        final BufferedReader inbuff = new BufferedReader(reader);
        String inData;
        final String linesep = System.getProperty("line.separator");
        while ((inData = inbuff.readLine()) != null) {
            writer.write(inData);
            writer.write(linesep);
        }
        inbuff.close();
    }

    /**
     * Copy a source file to a tempfile for script execution
     *
     * @param framework  framework
     * @param sourcefile source file
     *
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    public static File writeScriptTempfile(final Framework framework, final File sourcefile) throws IOException {
        return writeScriptTempfile(framework, new FileInputStream(sourcefile), null, null);
    }

    /**
     * Copy a source stream to a tempfile for script execution
     *
     * @param framework framework
     * @param stream    source
     *
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    public static File writeScriptTempfile(final Framework framework, final InputStream stream) throws IOException {
        return writeScriptTempfile(framework, stream, null, null);
    }

    /**
     * Copy string content to a tempfile for script execution
     *
     * @param framework framework
     * @param source    string content
     *
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    public static File writeScriptTempfile(final Framework framework, final String source) throws IOException {
        return writeScriptTempfile(framework, null, source, null);

    }
    /**
     * Copy reader content to a tempfile for script execution
     *
     * @param framework framework
     * @param source    string content
     *
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    public static File writeScriptTempfile(final Framework framework, final Reader source) throws IOException {
        return writeScriptTempfile(framework, null, null, source);
    }

    /**
     * Copy a source stream or string content to a tempfile for script execution
     *
     * @param framework framework
     * @param stream    source stream
     * @param source    content
     *
     * @param reader
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    private static File writeScriptTempfile(final Framework framework, final InputStream stream,
                                            final String source, final Reader reader) throws
        IOException {
        /**
         * Prepare a file to save the content
         */
        final File scriptfile;
        scriptfile = File.createTempFile("dispatch", ".tmp", new File(framework.getProperty("framework.var.dir")));

        final FileWriter writer = new FileWriter(scriptfile);

        if (null != source) {
            //write script content to temp file
            writer.write(source);
        }else if (null != reader) {
            ScriptfileUtils.writeReader(reader, writer);
        } else if (null != stream) {
            ScriptfileUtils.writeStream(stream, writer);
        }

        writer.close();
        scriptfile.deleteOnExit();

        return scriptfile;
    }

    /**
     * Set the executable flag on a file if supported by the OS
     *
     * @param scriptfile target file
     *
     * @throws IOException if an error occurs
     */
    public static void setExecutePermissions(final File scriptfile) throws IOException {
        ////////////////////
        // XXX: the following commented block is available in java 6 only
        /*if (!scriptfile.setExecutable(true, true)) {
            warn("Unable to set executable bit on temp script file, execution may fail: " + scriptfile
                .getAbsolutePath());
        }*/
        ///////////////////

        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            final Process process = Runtime.getRuntime().exec(
                new String[]{"chmod", "+x", scriptfile.getAbsolutePath()});
            int result=-1;
            try {
                result=process.waitFor();
            } catch (InterruptedException e) {

            }
            if(result>0) {
                throw new IOException("exec returned: " + result);
            }
        }
    }


}
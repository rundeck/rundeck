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

package com.dtolabs.rundeck.core.execution.script;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.common.INodeEntry;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for writing temp files for scripts and setting file permissions.
 */
public class ScriptfileUtils {


    public static final String SHOULD_ADD_BOM = "file-copier-add-utf8-bom";

    /**
     * @return the line ending style for the node, based on the osFamily, or use LOCAL if
     * undetermined
     *
     * @param node node
     *
     */
    public static LineEndingStyle lineEndingStyleForNode(final INodeEntry node) {
        return LineEndingStyle.forOsFamily(node.getOsFamily());
    }

    public static boolean shouldAddBomForNode(final INodeEntry node){
        if (null != node.getAttributes() && null != node.getAttributes().get(SHOULD_ADD_BOM)) {
            String s = node.getAttributes().get(SHOULD_ADD_BOM);
            return "true".equals(s);
        }
        return false;
    }

    /**
     * Line ending style
     */
    public static enum LineEndingStyle {
        /**
         * Unix line endings, "\n"
         */
        UNIX,
        /**
         * Windows line endings, "\r\n"
         */
        WINDOWS,
        /**
         * Local line endings of the jvm host
         */
        LOCAL;

        /**
         * @return line ending style given an OS Family string, or returns the Local line ending
         * style if the string does not map to a known style
         *
         * @param family family
         *
         */
        public static LineEndingStyle forOsFamily(final String family) {
            if (null != family) {
                try {
                    return valueOf(family.toUpperCase());
                } catch (IllegalArgumentException e) {
                }
            }
            return LOCAL;
        }

        /**
         * @return the line separator string for this style
         *
         */
        public String getLineSeparator() {
            switch (this) {
                case UNIX:
                    return "\n";
                case WINDOWS:
                    return "\r\n";
                case LOCAL:
                default:
                    return System.getProperty("line.separator");
            }
        }
    }

    /**
     * Write an inputstream to a FileWriter
     * @param input input stream
     * @param writer writer
     * @throws IOException if an error occurs
     */
    private static void writeStream(final InputStream input, final FileWriter writer)
            throws IOException
    {
        writeStream(input, writer, LineEndingStyle.LOCAL, false);
    }

    /**
     * Write an inputstream to a FileWriter
     * @param input input stream
     * @param writer writer
     * @throws IOException if an error occurs
     */
    private static void writeStream(
            final InputStream input,
            final FileWriter writer,
            final LineEndingStyle style,
            final boolean addBom
    ) throws IOException
    {
        try (InputStreamReader inStream = new InputStreamReader(input)) {
            writeReader(inStream, writer, style, addBom);
        }
    }

    /**
     * Copy from a Reader to a FileWriter
     * @param reader reader
     * @param writer writer
     * @throws IOException if an error occurs
     */
    private static void writeReader(final Reader reader, final FileWriter writer) throws IOException {
        writeReader(reader, writer, LineEndingStyle.LOCAL, false);
    }

    /**
     * Copy from a Reader to a FileWriter
     *
     * @param reader reader
     * @param writer writer
     *
     * @throws IOException if an error occurs
     */
    private static void writeReader(
            final Reader reader,
            final FileWriter writer,
            final LineEndingStyle style,
            final boolean addBom
    ) throws IOException
    {
        final BufferedReader inbuff = new BufferedReader(reader);
        String inData;
        final String linesep =
                null == style ?
                        LineEndingStyle.LOCAL.getLineSeparator() :
                        style.getLineSeparator();

        if(addBom && StandardCharsets.UTF_8.aliases().contains(writer.getEncoding())
                && style == LineEndingStyle.WINDOWS) {
            writer.write('\ufeff');
        }
        while ((inData = inbuff.readLine()) != null) {
            writer.write(inData);
            writer.write(linesep);
        }
        inbuff.close();
    }

    /**
     * Copy reader content to a tempfile for script execution
     *
     * @param framework framework
     * @param source    string content
     * @param style  style
     *
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    public static File writeScriptTempfile(
            final Framework framework,
            final Reader source,
            final LineEndingStyle style
    )
            throws IOException
    {
        return writeScriptTempfile(framework, null, null, source, style);
    }


    /**
     * Copy a source stream or string content to a tempfile for script execution
     *
     * @param framework framework
     * @param stream    source stream
     * @param source    content
     * @param style     file line ending style to use
     * @param reader reader
     *
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    public static File writeScriptTempfile(
            final Framework framework,
            final InputStream stream,
            final String source,
            final Reader reader,
            final LineEndingStyle style
    ) throws IOException
    {
        /*
         * Prepare a file to save the content
         */
        final File scriptfile = createTempFile(framework);

        writeScriptFile(stream, source, reader, style, scriptfile);

        return scriptfile;
    }

    /**
     * Write script content to a destination file from one of the sources
     *
     * @param stream       stream source
     * @param scriptString script content
     * @param reader       reader source
     * @param style        line ending style
     * @param scriptfile   destination file
     *
     * @throws IOException on io error
     */
    public static void writeScriptFile(
            InputStream stream,
            String scriptString,
            Reader reader,
            LineEndingStyle style,
            File scriptfile,
            boolean addBom
    ) throws IOException
    {
        try (FileWriter writer = new FileWriter(scriptfile)) {
            if (null != scriptString) {
                ScriptfileUtils.writeReader(new StringReader(scriptString), writer, style, addBom);
            } else if (null != reader) {
                ScriptfileUtils.writeReader(reader, writer, style, addBom);
            } else if (null != stream) {
                ScriptfileUtils.writeStream(stream, writer, style, addBom);
            } else {
                throw new IllegalArgumentException("no script source argument");
            }
        }
    }

    /**
     * Write script content to a destination file from one of the sources
     *
     * @param stream       stream source
     * @param scriptString script content
     * @param reader       reader source
     * @param style        line ending style
     * @param scriptfile   destination file
     *
     * @throws IOException on io error
     */
    public static void writeScriptFile(
            InputStream stream,
            String scriptString,
            Reader reader,
            LineEndingStyle style,
            File scriptfile
    ) throws IOException
    {
        writeScriptFile(stream, scriptString, reader, style, scriptfile, false);
    }

    /**
     * Creates a temp file and marks it for deleteOnExit, to clean up proactively call {@link #releaseTempFile(java.io.File)} with the result when complete
     *
     * @return Create a temp file in the framework
     * @param framework  fwk
     * @throws IOException on io error
     * @deprecated
     */
    @Deprecated
    public static File createTempFile(final Framework framework) throws IOException {
        return createTempFile((IFramework) framework);
    }

    /**
     * Creates a temp file and marks it for deleteOnExit, to clean up proactively call
     * {@link #releaseTempFile(java.io.File)} with the result when complete
     *
     * @param framework fwk
     * @return Create a temp file in the framework
     * @throws IOException on io error
     */
    public static File createTempFile(final IFramework framework) throws IOException {
        String fileExt = ".tmp";
        if ("windows".equalsIgnoreCase(framework.createFrameworkNode().getOsFamily())) {
            fileExt = ".tmp.bat";
        }
        final File dispatch = File.createTempFile(
                "dispatch",
                fileExt,
                new File(framework.getPropertyLookup().getProperty("framework.tmp.dir"))
        );
        registerTempFile(dispatch);
        return dispatch;
    }

    private static Set<File> tempFilesToDelete = Collections.synchronizedSet(new HashSet<File>());
    private static void registerTempFile(File file){
        file.deleteOnExit();
        tempFilesToDelete.add(file);
    }

    /**
     * Remove a file that may have been created by {@link #createTempFile(com.dtolabs.rundeck.core.common.Framework)}.
     * If the file was not created that way, it will not be deleted.
     * @param file a temp file created with {@link #createTempFile(com.dtolabs.rundeck.core.common.Framework)}
     * @return true if the temp file was known and was deleted
     */
    public static boolean releaseTempFile(File file) {
        return tempFilesToDelete.remove(file) && file.delete();
    }

    /**
     * Set the executable flag on a file if supported by the OS
     *
     * @param scriptfile target file
     *
     * @throws IOException if an error occurs
     */
    public static void setExecutePermissions(final File scriptfile) throws IOException {
        if (!scriptfile.setExecutable(true, true)) {
            System.err.println("Unable to set executable bit on temp script file, execution may fail: " + scriptfile
                .getAbsolutePath());
        }
    }


}

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
* JobDefinitionSerializer.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 17, 2010 3:10:23 PM
* $Id$
*/
package com.dtolabs.client.services;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.execution.ExecutionUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;
import com.dtolabs.rundeck.core.utils.OptsUtil;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;

/**
 * JobDefinitionSerializer utility converts execution contexts to jobs.xml formatted XML.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class JobDefinitionSerializer {
    /**
     * Serialize a script dispatch into jobs.xml format
     *
     * @param dispatchdef the script dispatch descriptor
     *
     * @return the XML Document
     * @throws java.io.IOException if the input IDispatchedScript throws it when accessing script stream input.
     */
    public static Document serialize(final IDispatchedScript dispatchdef) throws IOException {
        if(null==dispatchdef) {
            throw new IllegalArgumentException("cannot be null");
        }
        final Document doc = DocumentFactory.getInstance().createDocument();
        final Element root = doc.addElement("joblist");
        final String loglevelstr = ExecutionUtils.getMessageLogLevel(dispatchdef.getLoglevel(), Constants.MSG_INFO)
            .toUpperCase();

        final Element job = addJobBasic(root, "dispatch commandline job", loglevelstr);

        addScriptDispatch(dispatchdef, job);

        addNodefilters(job, dispatchdef.getNodeThreadcount(), dispatchdef.isKeepgoing(), dispatchdef.getNodeExcludePrecedence(), dispatchdef.getNodeFilter());

        return doc;
    }

    private static Element addJobBasic(final Element root, final String jobName, final String loglevel) {
        final Element job = root.addElement("job");
        job.addElement("name").addText(jobName);
        job.addElement("description").addText(jobName);
        job.addElement("additional");
        job.addElement("loglevel").addText(loglevel);
        return job;
    }

    /**
     * Add script dispatch content to the job element
     *
     * @param dispatchdef dispatch definition
     * @param job         job element
     * @throws java.io.IOException if the input IDispatchedScript throws it when accessing script stream input.
     */
    private static void addScriptDispatch(final IDispatchedScript dispatchdef, final Element job) throws IOException {
        if(null== dispatchdef.getFrameworkProject()) {
            throw new IllegalArgumentException("No project is specified");
        }
        final Element ctx = job.addElement("context");
        ctx.addElement("project").addText(dispatchdef.getFrameworkProject());
        final InputStream stream = dispatchdef.getScriptAsStream();
        final Element seq = job.addElement("sequence");
        final Element cmd = seq.addElement("command");
        if (null != dispatchdef.getScript() || null != stream) {

            //full script
            final Element script = cmd.addElement("script");
            if(null!= dispatchdef.getScript()){
                script.addCDATA(dispatchdef.getScript());
            }else{
                //serialize script inputstream and add string to dom
                final StringWriter sw = new StringWriter();
                copyReader(new InputStreamReader(stream), sw, 10240);
                sw.flush();
                script.addCDATA(sw.toString());
            }
            if (null != dispatchdef.getArgs() && dispatchdef.getArgs().length > 0) {
                final Element argstring = cmd.addElement("scriptargs");
                argstring.addText(OptsUtil.join(dispatchdef.getArgs()));
            }
        } else if (null != dispatchdef.getServerScriptFilePath()) {
            //server-local script filepath
            final Element filepath = cmd.addElement("scriptfile");
            filepath.addText(dispatchdef.getServerScriptFilePath());
            if (null != dispatchdef.getArgs() && dispatchdef.getArgs().length>0) {
                final Element argstring = cmd.addElement("scriptargs");
                argstring.addText(OptsUtil.join(dispatchdef.getArgs()));
            }
        } else if (null != dispatchdef.getArgs() && dispatchdef.getArgs().length > 0) {
            //shell command
            final Element exec = cmd.addElement("exec");
            exec.addText(OptsUtil.join(dispatchdef.getArgs()));
        } else {
            throw new IllegalArgumentException("Dispatched script did not specify a command, script or filepath");
        }


    }


    /**
     * Add nodefilter and dispatch element to job element for the nodeset, if not null
     *
     * @param job     job element
     * @param threadCount thread count
     * @param excludePrecedence exclude precedence set
     * @param nodeFilter node filter string
     */
    private static void addNodefilters(final Element job, int threadCount, boolean keepgoing,
            boolean excludePrecedence, String nodeFilter) {
        final int threadcount = (threadCount > 1) ? threadCount : 1;
        if (null != nodeFilter) {
            final Element filters = job.addElement("nodefilters");;
            filters.addElement("filter").addText(nodeFilter);
            filters.addAttribute("excludeprecedence", Boolean.toString(excludePrecedence));
        }
        final Element dispatch = job.addElement("dispatch");
        dispatch.addElement("threadcount").addText(Integer.toString(threadcount));
        dispatch.addElement("keepgoing").addText(Boolean.toString(keepgoing));

    }


    /**
     * Convert the input script context to a Jobs.xml document and serialize it to the given file.
     *
     * @param script the input dispatch script
     * @param file    destination file
     *
     * @throws IOException if an error occurs
     */
    public static void serializeToFile(final IDispatchedScript script, final File file) throws IOException {
        final Document doc = serialize(script);
        serializeDocToFile(file, doc);
    }


    /**
     * Read the data from the Reader and writer it as text to the Writer
     *
     * @param in      inputstream
     * @param out     outpustream
     * @param bufSize size of the buffer
     *
     * @throws java.io.IOException if thrown by underlying io operations
     */
    private static void copyReader(final Reader in, final Writer out, final int bufSize) throws IOException {
        final char[] buffer = new char[bufSize];
        int c;
        c = in.read(buffer);
        while (c >= 0) {
            if (c > 0) {
                out.write(buffer, 0, c);
            }
            c = in.read(buffer);
        }
    }

    private static void serializeDocToFile(final File file, final Document doc) throws IOException {
        final OutputFormat format = OutputFormat.createPrettyPrint();
        final XMLWriter writer = new XMLWriter(new FileOutputStream(file), format);
        writer.write(doc);
        writer.flush();
    }
}

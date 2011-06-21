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
* RundeckAPICentralDispatcher.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 18, 2010 11:59:34 AM
* $Id$
*/
package com.dtolabs.client.services;

import com.dtolabs.client.utils.WebserviceResponse;
import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.utils.NodeSet;
import com.dtolabs.utils.Streams;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RundeckAPICentralDispatcher serializes uses server API v1 to submit requests and receive responses.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class RundeckAPICentralDispatcher implements CentralDispatcher {
    /**
     * Webservice link prefix for a stored job.
     */
    public static final String RUNDECK_JOB_LINK_PREFIX = "/job/show/";
    /**
     * Webservice link prefix for an execution
     */
    public static final String RUNDECK_EXEC_LINK_PREFIX = "/execution/show/";

    /*******************
     * API v1 endpoints
     *******************/

    /**
     * RUNDECK API Version
     */
    public static final String RUNDECK_API_VERSION = "1";
    /**
     * RUNDECK API base path
     */
    public static final String RUNDECK_API_BASE = "/api/" + RUNDECK_API_VERSION;
    /**
     * API endpoint for execution report
     */
    public static final String RUNDECK_API_EXECUTION_REPORT = RUNDECK_API_BASE + "/report/create";


    /**
     * Webservice endpoint for running scripts
     */
    public static final String RUNDECK_API_RUN_SCRIPT = RUNDECK_API_BASE + "/run/script";
    /**
     * Webservice endpoint for running commands
     */
    public static final String RUNDECK_API_RUN_COMMAND = RUNDECK_API_BASE + "/run/command";
    /**
     * Webservice endpoint for queue list requests
     */
    public static final String RUNDECK_API_LIST_EXECUTIONS_PATH = RUNDECK_API_BASE + "/executions/running";
    /**
     * Webservice endpoint for killing job executions
     */
    public static final String RUNDECK_API_KILL_JOB_PATH = RUNDECK_API_BASE + "/execution/$id/abort";
    /**
     * Webservice endpoint for exporting stored jobs.
     */
    public static final String RUNDECK_API_JOBS_EXPORT_PATH = RUNDECK_API_BASE + "/jobs/export";

    /**
     * Webservice endpoint for listing stored jobs.
     */
    public static final String RUNDECK_API_JOBS_LIST_PATH = RUNDECK_API_BASE + "/jobs";

    /**
     * upload path
     */
    public static final String RUNDECK_API_JOBS_UPLOAD = RUNDECK_API_BASE + "/jobs/import";
    /**
     * Webservice endpoint for running job by name or id
     */
    public static final String RUNDECK_API_JOBS_RUN = RUNDECK_API_BASE + "/job/$id/run";

    /**
     * logger
     */
    public static final Logger logger = Logger.getLogger(RundeckAPICentralDispatcher.class);
    private ServerService serverService;

    /**
     * Create a RundeckCentralDispatcher
     *
     * @param framework the framework
     */
    public RundeckAPICentralDispatcher(final Framework framework) {
        serverService = new ServerService(framework);
    }

    /**
     * Report execution status
     *
     * @param project          project
     * @param title            execution title
     * @param status           result status, either 'succeed','cancel','fail'
     * @param failedNodeCount  total node count
     * @param successNodeCount count of successful nodes
     * @param tags
     * @param script           script content (can be null if summary specified)
     * @param summary          summary of execution (can be null if script specified)
     * @param start            start date (can be null)
     * @param end              end date (can be null)
     *
     * @throws com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException
     *
     */
    public void reportExecutionStatus(final String project, final String title, final String status,
                                      final int failedNodeCount, final int successNodeCount,
                                      final String tags, final String script, final String summary, final Date start,
                                      final Date end) throws
        CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("project", project);
        params.put("title", title);
        params.put("status", status);
        params.put("nodesuccesscount", Integer.toString(successNodeCount));
        params.put("nodefailcount", Integer.toString(failedNodeCount));
        if (null != tags) {
            params.put("tags", tags);
        }
        if (null != script) {
            params.put("script", script);
        }
        params.put("summary", summary);
        if (null != start) {
            params.put("start", Long.toString(start.getTime()));
        }
        if (null != end) {
            params.put("end", Long.toString(end.getTime()));
        }

        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_API_EXECUTION_REPORT, params, null, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }

        validateResponse(response);

    }


    public QueuedItemResult queueDispatcherScript(final IDispatchedScript iDispatchedScript) throws
        CentralDispatcherException {
        final String argString;
        final String scriptString;
        final boolean isExec;

        try {

            //write script to file
            final InputStream stream = iDispatchedScript.getScriptAsStream();
            if (null != iDispatchedScript.getScript() || null != stream) {
                //full script
                if (null != iDispatchedScript.getScript()) {
                    scriptString = iDispatchedScript.getScript();
                } else {
                    //read stream to string
                    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    Streams.copyStream(stream, byteArrayOutputStream);
                    scriptString = new String(byteArrayOutputStream.toByteArray());
                }
                if (null != iDispatchedScript.getArgs() && iDispatchedScript.getArgs().length > 0) {
                    argString = CLIUtils.generateArgline(null, iDispatchedScript.getArgs());
                } else {
                    argString = null;
                }

                isExec = false;
            } else if (null != iDispatchedScript.getServerScriptFilePath()) {
                //server-local script filepath

                //read stream to string
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Streams.copyStream(new FileInputStream(new File(iDispatchedScript.getServerScriptFilePath())),
                    byteArrayOutputStream);
                scriptString = new String(byteArrayOutputStream.toByteArray());

                if (null != iDispatchedScript.getArgs() && iDispatchedScript.getArgs().length > 0) {
                    argString = CLIUtils.generateArgline(null, iDispatchedScript.getArgs());
                } else {
                    argString = null;
                }
                isExec = false;
            } else if (null != iDispatchedScript.getArgs() && iDispatchedScript.getArgs().length > 0) {
                //shell command
                scriptString = null;
                argString = CLIUtils.generateArgline(null, iDispatchedScript.getArgs());
                isExec = true;
            } else {
                throw new IllegalArgumentException("Dispatched script did not specify a command, script or filepath");
            }

        } catch (IOException e) {
            throw new CentralDispatcherServerRequestException("Unable to queue command: " + e.getMessage(), e);
        }

        //request parameters
        final HashMap<String, String> params = new HashMap<String, String>();

        params.put("project", iDispatchedScript.getFrameworkProject());
        if (isExec) {
            params.put("exec", argString);
        } else {
            params.put("scriptFile", scriptString);
        }
        addLoglevelParams(params, iDispatchedScript.getLoglevel());
        addAPINodeSetParams(params, iDispatchedScript.getNodeSet(), iDispatchedScript.getNodeSet().isKeepgoing());

        return submitRunRequest(null, params,
            isExec ? RUNDECK_API_RUN_COMMAND : RUNDECK_API_RUN_SCRIPT);
    }

    /**
     * Submit a request to the server which expects a list of execution items in the response, and return a single
     * QueuedItemResult parsed from the response.
     *
     * @param tempxml     xml temp file (or null)
     * @param otherparams parameters for the request
     * @param requestPath
     *
     * @return a single QueuedItemResult
     *
     * @throws com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException
     *          if an error occurs
     */
    private QueuedItemResult submitExecutionRequest(final File tempxml, final HashMap<String, String> otherparams,
                                                    final String requestPath) throws CentralDispatcherException {


        final HashMap<String, String> params = new HashMap<String, String>();
        if (null != otherparams) {
            params.putAll(otherparams);
        }

        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(requestPath, params, tempxml, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }
        validateResponse(response);

        final ArrayList<QueuedItem> list = parseExecutionListResult(response);
        if (null == list || list.size() < 1) {

            return QueuedItemResultImpl.failed("Server response contained no success information.");
        } else {

            final QueuedItem next = list.iterator().next();
            return QueuedItemResultImpl.successful("Succeeded queueing " + next.getName(), next.getId(), next.getUrl(),
                next.getName());
        }
    }


    /**
     * Submit a request to the server which expects an execution id in response, and return a single
     * QueuedItemResult parsed from the response.
     *
     * @param tempxml     xml temp file (or null)
     * @param otherparams parameters for the request
     * @param requestPath
     *
     * @return a single QueuedItemResult
     *
     * @throws com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException
     *          if an error occurs
     */
    private QueuedItemResult submitRunRequest(final File tempxml, final HashMap<String, String> otherparams,
                                              final String requestPath) throws CentralDispatcherException {


        final HashMap<String, String> params = new HashMap<String, String>();
        if (null != otherparams) {
            params.putAll(otherparams);
        }

        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(requestPath, params, tempxml, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }
        validateResponse(response);

        final Document resultDoc = response.getResultDoc();

        if (null != resultDoc.selectSingleNode("/result/execution") && null != resultDoc.selectSingleNode(
            "/result/execution/@id")) {
            final Node node = resultDoc.selectSingleNode("/result/execution/@id");
            final String succeededId = node.getStringValue();
            final String name = "adhoc";
            String url = createExecutionURL(succeededId);
            url = makeAbsoluteURL(url);
            logger.info("\t[" + succeededId + "] <" + url + ">");
            return QueuedItemResultImpl.successful("Succeeded queueing " + name, succeededId, url, name);
        }
        return QueuedItemResultImpl.failed("Server response contained no success information.");
    }

    public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();

        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_API_LIST_EXECUTIONS_PATH, params, null, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }

        validateResponse(response);

        ////////////////////
        //parse result list of queued items, return the collection of QueuedItems
        ///////////////////

        return parseExecutionListResult(response);

    }

    private ArrayList<QueuedItem> parseExecutionListResult(final WebserviceResponse response) {
        final Document resultDoc = response.getResultDoc();

        final Node node = resultDoc.selectSingleNode("/result/executions");
        final List items = node.selectNodes("execution");
        final ArrayList<QueuedItem> list = new ArrayList<QueuedItem>();
        if (null != items && items.size() > 0) {
            for (final Object o : items) {
                final Node node1 = (Node) o;
                final String id = node1.selectSingleNode("@id").getStringValue();
                final Node jobname = node1.selectSingleNode("job/name");
                final Node desc = node1.selectSingleNode("description");
                final String name;
                if (null != jobname) {
                    name = jobname.getStringValue();
                } else {
                    name = desc.getStringValue();
                }
                String url = node1.selectSingleNode("@href").getStringValue();
                url = makeAbsoluteURL(url);
                logger.info("\t" + ": " + name + " [" + id + "] <" + url + ">");
                list.add(QueuedItemResultImpl.createQueuedItem(id, url, name));
            }
        }
        return list;
    }

    /**
     * If the url appears relative to an authority, i.e. it starts with "/", then convert it to be absolute using the
     * server URL base provided by the ServerService.
     *
     * @param url a relative url path to convert into absolute url.
     *
     * @return absolute URL if input was a url path, otherwise returns the input string.
     */
    String makeAbsoluteURL(String url) {
        if (null != url && url.startsWith("/")) {
            //change relative URL into absolute using the base server URL
            try {
                final URL serverUrl = new URL(serverService.getConnParams().getServerUrl());
                final URL newUrl = new URL(serverUrl, url);
                url = newUrl.toExternalForm();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    /**
     * If the url appears relative to an authority, i.e. it starts with "/", then convert it to be absolute using the
     * server URL (including context path) provided by the ServerService.
     *
     * @param url a context-relative url path to convert into absolute url.
     *
     * @return absolute URL if input was a url path, otherwise returns the input string.
     */
    String makeContextAbsoluteURL(String url) {
        if (null != url && url.startsWith("/")) {
            //change relative URL into absolute using the base server URL
            try {
                final URL newUrl = new URL(serverService.getConnParams().getServerUrl() + url);
                url = newUrl.toExternalForm();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    /**
     * Validate the response is in expected envlope form with &lt;result&gt; content.
     *
     * @param response response
     *
     * @return Envelope if format is correct and there is no error
     *
     * @throws com.dtolabs.client.services.CentralDispatcherServerRequestException
     *          if the format is incorrect, or the Envelope indicates an error response.
     */
    private void validateJobsResponse(final WebserviceResponse response) throws
        CentralDispatcherServerRequestException {
        if (null == response) {
            throw new CentralDispatcherServerRequestException("Response was null");
        }

        if (null != response.getResponseMessage()) {
            logger.info("Response: " + response.getResponseMessage());
        }
        final Document resultDoc = response.getResultDoc();
        if (null == resultDoc) {
            throw new CentralDispatcherServerRequestException("Response content unexpectedly empty");
        }
        try {
            logger.debug(serialize(resultDoc));
        } catch (IOException e) {
            logger.debug("ioexception serializing result doc", e);
        }

        if (!"joblist".equals(resultDoc.getRootElement().getName())) {
            throw new CentralDispatcherServerRequestException("Response had unexpected content: " + resultDoc);
        }

    }

    /**
     * Validate the response is in expected envlope form with &lt;result&gt; content.
     *
     * @param response response
     *
     * @return Envelope if format is correct and there is no error
     *
     * @throws com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException
     *          if the format is incorrect, or the Envelope indicates an error response.
     */
    private Envelope validateResponse(final WebserviceResponse response) throws
        CentralDispatcherException {
        if (null == response) {
            throw new CentralDispatcherServerRequestException("Response was null");
        }

        if (null != response.getResponseMessage()) {
            logger.info("Response: " + response.getResponseMessage());
        }
        final Document resultDoc = response.getResultDoc();
        if (null == resultDoc) {
            throw new CentralDispatcherServerRequestException("Response content unexpectedly empty. " + (response
                                                                                                             .getResponseMessage()
                                                                                                         != null
                                                                                                         ? response
                .getResponseMessage() : ""));
        }
        try {
            logger.debug(serialize(resultDoc));
        } catch (IOException e) {
            logger.debug("ioexception serializing result doc", e);
        }

        if (!"result".equals(resultDoc.getRootElement().getName())) {
            throw new CentralDispatcherServerRequestException("Response had unexpected content: " + resultDoc);
        }
        final Envelope envelope = new Envelope(response.getResultDoc());
        if (envelope.isErrorResult()) {
            final StringBuffer sb = new StringBuffer();
            final StringBuffer buffer = envelope.errorMessages();
            if (buffer.length() > 0) {
                sb.append(buffer);
            } else {
                sb.append("Server reported an error");
                if (null != response.getResponseMessage()) {
                    sb.append(": ").append(response.getResponseMessage());
                }
            }
            if (null != response.getResponseMessage()) {
                logger.error("Server reported an error: " + response.getResponseMessage());
            } else {
                logger.error("Server reported an error.");
            }
            throw new CentralDispatcherFailureResponseException(sb.toString());
        }
        return envelope;
    }


    /**
     * Check if the response is an API error response, if so throw an exception, otherwise return;
     *
     * @param response response
     *
     * @throws com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException
     *          if the format is incorrect, or the Envelope indicates an error response.
     */
    private void checkErrorResponse(final WebserviceResponse response) throws
        CentralDispatcherException {
        if (null == response) {
            throw new CentralDispatcherServerRequestException("Response was null");
        }

        if (null != response.getResponseMessage()) {
            logger.info("Response: " + response.getResponseMessage());
        }
        final Document resultDoc = response.getResultDoc();
        if (null == resultDoc) {
            return;
        }
        try {
            logger.debug(serialize(resultDoc));
        } catch (IOException e) {
            logger.debug("ioexception serializing result doc", e);
        }

        if (!"result".equals(resultDoc.getRootElement().getName())) {
            return;
        }
        final Envelope envelope = new Envelope(response.getResultDoc());
        if (envelope.isErrorResult()) {
            final StringBuffer sb = new StringBuffer();
            final StringBuffer buffer = envelope.errorMessages();
            if (buffer.length() > 0) {
                sb.append(buffer);
            } else {
                sb.append("Server reported an error");
                if (null != response.getResponseMessage()) {
                    sb.append(": ").append(response.getResponseMessage());
                }
            }
            if (null != response.getResponseMessage()) {
                logger.error("Server reported an error: " + response.getResponseMessage());
            } else {
                logger.error("Server reported an error.");
            }
            throw new CentralDispatcherFailureResponseException(sb.toString());
        }
    }


    public DispatcherResult killDispatcherExecution(final String execId) throws CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("id", execId);

        final String rundeckApiKillJobPath = RUNDECK_API_KILL_JOB_PATH.replace("$id", execId);
        //2. send request via ServerService
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(rundeckApiKillJobPath, params, null, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }

        final Envelope envelope = validateResponse(response);

        final Node result1 = envelope.doc.selectSingleNode("result");
        final String abortStatus = result1.selectSingleNode("abort/@status").getStringValue();
        final boolean result = !"failed".equals(abortStatus);
        final StringBuffer sb = envelope.successMessages();
        return new DispatcherResult() {
            public boolean isSuccessful() {
                return result;
            }

            public String getMessage() {
                return sb.toString();
            }
        };
    }

    /**
     * Return the URL for a job based on its ID
     *
     * @param id job ID
     *
     * @return absolute URL for the job's link
     */
    private String createJobURL(final String id) {
        return makeContextAbsoluteURL(RUNDECK_JOB_LINK_PREFIX + id);
    }
    /**
     * Return the URL for a job based on its ID
     *
     * @param id job ID
     *
     * @return absolute URL for the job's link
     */
    private String createExecutionURL(final String id) {
        return makeContextAbsoluteURL(RUNDECK_EXEC_LINK_PREFIX + id);
    }

    public Collection<IStoredJob> listStoredJobs(final IStoredJobsQuery iStoredJobsQuery, final OutputStream output,
                                                 final JobDefinitionFileFormat fformat) throws
        CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();
        final String nameMatch = iStoredJobsQuery.getNameMatch();
        String groupMatch = iStoredJobsQuery.getGroupMatch();
        final String projectFilter = iStoredJobsQuery.getProjectFilter();
        final String idlistFilter = iStoredJobsQuery.getIdlist();

        if (null != output && null != fformat) {
            params.put("format", fformat.getName());
        } else {
            params.put("format", JobDefinitionFileFormat.xml.getName());
        }
        if (null != nameMatch) {
            params.put("jobFilter", nameMatch);
        }
        if (null != groupMatch) {
            final Matcher matcher = Pattern.compile("^/*(.+?)/*$").matcher(groupMatch);
            if (matcher.matches()) {
                //strip leading and trailing slashes
                groupMatch = matcher.group(1);
            }
            params.put("groupPath", groupMatch);
        }
        if (null != projectFilter) {
            params.put("project", projectFilter);
        }
        if (null != idlistFilter) {
            params.put("idlist", idlistFilter);
        }

        //2. send request via ServerService
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_API_JOBS_EXPORT_PATH, params, null, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }
        checkErrorResponse(response);
        //if xml, do local validation and listing
        if (null == fformat || fformat == JobDefinitionFileFormat.xml) {
            validateJobsResponse(response);

            ////////////////////
            //parse result list of queued items, return the collection of QueuedItems
            ///////////////////

            final Document resultDoc = response.getResultDoc();

            final Node node = resultDoc.selectSingleNode("/joblist");
            final ArrayList<IStoredJob> list = new ArrayList<IStoredJob>();
            if (null == node) {
                return list;
            }
            final List items = node.selectNodes("job");
            if (null != items && items.size() > 0) {
                for (final Object o : items) {
                    final Node node1 = (Node) o;
                    final String id = node1.selectSingleNode("id").getStringValue();
                    final String name = node1.selectSingleNode("name").getStringValue();
                    final String url = createJobURL(id);

                    final Node gnode = node1.selectSingleNode("group");
                    final String group = null != gnode ? gnode.getStringValue() : null;
                    final String description = node1.selectSingleNode("description").getStringValue();
                    list.add(StoredJobImpl.create(id, name, url, group, description, projectFilter));
                }
            }

            if (null != output) {
                //write output doc to the outputstream
                final OutputFormat format = OutputFormat.createPrettyPrint();
                try {
                    final XMLWriter writer = new XMLWriter(output, format);
                    writer.write(resultDoc);
                    writer.flush();
                } catch (IOException e) {
                    throw new CentralDispatcherServerRequestException(e);
                }
            }
            return list;
        } else if (fformat == JobDefinitionFileFormat.yaml) {
            //do rought yaml parse
            final Collection<Map> mapCollection = validateJobsResponseYAML(response);
            final ArrayList<IStoredJob> list = new ArrayList<IStoredJob>();

            if (null == mapCollection || mapCollection.size() < 1) {
                return list;
            }
            for (final Map map : mapCollection) {
                final String id = map.get("id").toString();
                final String name = (String) map.get("name");
                final String group = map.containsKey("group") ? (String) map.get("group") : null;
                final String desc = map.containsKey("description") ? (String) map.get("description") : "";
                final String url = createJobURL(id);
                list.add(StoredJobImpl.create(id, name, url, group, desc, projectFilter));
            }

            if (null != output) {
                //write output doc to the outputstream
                try {
                    final Writer writer = new OutputStreamWriter(output);
                    writer.write(response.getResults());
                    writer.flush();
                } catch (IOException e) {
                    throw new CentralDispatcherServerRequestException(e);
                }
            }
            return list;
        }
        return null;
    }

    public Collection<IStoredJob> reallistStoredJobs(final IStoredJobsQuery iStoredJobsQuery) throws
        CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();
        final String nameMatch = iStoredJobsQuery.getNameMatch();
        String groupMatch = iStoredJobsQuery.getGroupMatch();
        final String projectFilter = iStoredJobsQuery.getProjectFilter();
        final String idlistFilter = iStoredJobsQuery.getIdlist();

        if (null != nameMatch) {
            params.put("jobFilter", nameMatch);
        }
        if (null != groupMatch) {
            final Matcher matcher = Pattern.compile("^/*(.+?)/*$").matcher(groupMatch);
            if (matcher.matches()) {
                //strip leading and trailing slashes
                groupMatch = matcher.group(1);
            }
            params.put("groupPath", groupMatch);
        }
        if (null != projectFilter) {
            params.put("project", projectFilter);
        }
        if (null != idlistFilter) {
            params.put("idlist", idlistFilter);
        }

        //2. send request via ServerService
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_API_JOBS_LIST_PATH, params, null, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }
        validateResponse(response);
        //extract job list
        final Document resultDoc = response.getResultDoc();
        final ArrayList<IStoredJob> list = new ArrayList<IStoredJob>();

        final Node jobs = resultDoc.selectSingleNode("/result/jobs");
        for (final Object job1 : jobs.selectNodes("job")) {
            final Node job = (Node) job1;
            final String id = job.selectSingleNode("@id").getStringValue();
            final String name = job.selectSingleNode("name").getStringValue();
            final String group = job.selectSingleNode("group").getStringValue();
            final String desc = job.selectSingleNode("desc").getStringValue();
            final String url = createJobURL(id);
            list.add(StoredJobImpl.create(id, name, url, group, desc, projectFilter));
        }

        return list;
    }

    /**
     * Validate the response is in expected yaml format
     *
     * @param response response
     *
     * @return Collection of job data maps if format is correct and there is no error
     *
     * @throws com.dtolabs.client.services.CentralDispatcherServerRequestException
     *          if the format is incorrect, or the response indicates an error response.
     */
    private Collection<Map> validateJobsResponseYAML(final WebserviceResponse response) throws
        CentralDispatcherServerRequestException {
        if (null == response) {
            throw new CentralDispatcherServerRequestException("Response was null");
        }

        if (null != response.getResponseMessage()) {
            logger.info("Response: " + response.getResponseMessage());
        }
        String resultContentType = response.getResultContentType();
        if (resultContentType.contains(";")) {
            resultContentType = resultContentType.substring(0, resultContentType.indexOf(";"));
        }
        if (!resultContentType.endsWith("/yaml")) {
            throw new CentralDispatcherServerRequestException(
                "Expected YAML response, unexpected content type: " + response.getResultContentType());
        }
        final ArrayList<Map> dataset = new ArrayList<Map>();
        final Object resobj;
        try {
            final Yaml yaml = new Yaml(new SafeConstructor());
            resobj = yaml.load(response.getResults());
        } catch (YAMLException e) {
            throw new CentralDispatcherServerRequestException("Failed to parse YAML: " + e.getMessage(), e);
        }
        if (resobj instanceof Collection) {
            dataset.addAll((Collection) resobj);
        } else {
            throw new CentralDispatcherServerRequestException("Response had unexpected content type: " + resobj);
        }

        for (final Map map : dataset) {
            if (!map.containsKey("name") || !map.containsKey("id")) {
                throw new CentralDispatcherServerRequestException("Response had unexpected dataset: " + resobj);
            }
        }
        return dataset;
    }


    public QueuedItemResult queueDispatcherJob(final IDispatchedJob dispatchedJob) throws CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();
        if (null == dispatchedJob.getJobRef()) {
            throw new IllegalArgumentException("JobRef was null");
        }
        final String jobid;
        if (null != dispatchedJob.getJobRef().getJobId()) {
            jobid = dispatchedJob.getJobRef().getJobId();
        } else {
            final String name;
            final String group;
            final String project = dispatchedJob.getJobRef().getProject();

            if (null != dispatchedJob.getJobRef().getName()) {
                name = dispatchedJob.getJobRef().getName();
            } else {
                name = null;
            }
            if (null != dispatchedJob.getJobRef().getGroup()) {
                group = dispatchedJob.getJobRef().getGroup();
            } else {
                group = null;
            }
            //Query to find matching job
            final Collection<IStoredJob> iStoredJobs = reallistStoredJobs(new IStoredJobsQueryImpl(name, group, null,
                project));
            if (null == iStoredJobs) {
                throw new CentralDispatcherException("Unable to query jobs");
            }
            if (iStoredJobs.size() < 1) {
                throw new CentralDispatcherException(
                    "Job not found matching query: " + (null != group ? group : "") + "/" + name);
            }
            if (iStoredJobs.size() > 1) {
                throw new CentralDispatcherException(
                    "Job not was not unique: " + (null != group ? group : "") + "/" + name + ": " + iStoredJobs.size()
                    + " jobs found");
            }
            //use found id
            final IStoredJob next = iStoredJobs.iterator().next();
            jobid = next.getJobId();

        }

        addAPINodeSetParams(params, dispatchedJob.getNodeSet(), dispatchedJob.isKeepgoing());
        addLoglevelParams(params, dispatchedJob.getLoglevel());
        if (null != dispatchedJob.getArgs() && dispatchedJob.getArgs().length > 0) {
            params.put("argString", CLIUtils.generateArgline(null, dispatchedJob.getArgs()));
        }
        
        final String apipath = RUNDECK_API_JOBS_RUN.replace("$id", jobid);
        return submitExecutionRequest(null, params, apipath);
    }


    private void addLoglevelParams(final Map<String, String> params, final int loglevel) {

        String loglevelstr = RundeckAppConstants.MSG_INFO.toUpperCase();
        switch (loglevel) {
            case Constants.DEBUG_LEVEL:
                loglevelstr = RundeckAppConstants.MSG_DEBUG.toUpperCase();
                break;
            case Constants.VERBOSE_LEVEL:
                loglevelstr = RundeckAppConstants.MSG_VERBOSE.toUpperCase();
                break;
            case Constants.INFO_LEVEL:
                loglevelstr = RundeckAppConstants.MSG_INFO.toUpperCase();
                break;
            case Constants.WARN_LEVEL:
                loglevelstr = RundeckAppConstants.MSG_WARN.toUpperCase();
                break;
            case Constants.ERR_LEVEL:
                loglevelstr = RundeckAppConstants.MSG_ERR.toUpperCase();
                break;
        }

        params.put("loglevel", loglevelstr);
    }

    /**
     * Add entries to the Map for node filter parameters from the nodeset
     */
    public static void addAPINodeSetParams(final HashMap<String, String> params, final NodeSet nodeSet,
                                           final Boolean isKeepgoing) {

        if (null == nodeSet) {
            return;
        }
        if (nodeSet.getThreadCount() > 0) {
            params.put("nodeThreadcount", Integer.toString(nodeSet.getThreadCount()));
        }
        if (null != isKeepgoing) {
            params.put("nodeKeepgoing", Boolean.toString(isKeepgoing));
        }
        if (nodeSet.isBlank()) {
            return;
        }
        boolean excludeprecedence = true;
        if (null != nodeSet.getExclude() && nodeSet.getExclude().isDominant()) {
            excludeprecedence = true;
        } else if (null != nodeSet.getInclude() && nodeSet.getInclude().isDominant()) {
            excludeprecedence = false;
        }
        params.put("exclude-precedence", Boolean.toString(excludeprecedence));

        final NodeSet.Include include = nodeSet.getInclude();

        for (final NodeSet.FILTER_ENUM filter : NodeSet.FILTER_ENUM.values()) {
            String value = null;
            if (null != include && !include.isBlank()) {
                value = filter.value(include);
            }

            final String key = filter.getName();
            if (null != value && !"".equals(value)) {
                params.put(key, value);
            } else {
                params.put(key, "");
            }
        }
        final NodeSet.Exclude exclude = nodeSet.getExclude();

        for (final NodeSet.FILTER_ENUM filter : NodeSet.FILTER_ENUM.values()) {
            String value = null;
            if (null != exclude && !exclude.isBlank()) {
                value = filter.value(exclude);
            }
            final String key = filter.getName();
            if (null != value && !"".equals(value)) {
                params.put("exclude-" + key, value);
            } else {
                params.put("exclude-" + key, "");
            }
        }


    }

    public Collection<IStoredJobLoadResult> loadJobs(final ILoadJobsRequest iLoadJobsRequest, final File input,
                                                     final JobDefinitionFileFormat format) throws

        CentralDispatcherException {
        final HashMap<String,String> params = new HashMap<String, String>();
        params.put("dupeOption", iLoadJobsRequest.getDuplicateOption().toString());

        if (null != format) {
            params.put("format", format.getName());
        }

        /*
         * Send the request bean and the file as a multipart request.
         */

        //2. send request via ServerService
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_API_JOBS_UPLOAD, params, input, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }
        validateResponse(response);

        ////////////////////
        //parse result list of queued items, return the collection of QueuedItems
        ///////////////////

        final Document result = response.getResultDoc();

        final int succeeded;
        final int failed;
        final int skipped;
        Node node = result.selectSingleNode("/result/succeeded/@count");
        if (null != node) {
            succeeded = Integer.parseInt(node.getStringValue());
        } else {
            succeeded = -1;
        }
        node = result.selectSingleNode("/result/failed/@count");
        if (null != node) {
            failed = Integer.parseInt(node.getStringValue());
        } else {
            failed = -1;
        }
        node = result.selectSingleNode("/result/skipped/@count");
        if (null != node) {
            skipped = Integer.parseInt(node.getStringValue());
        } else {
            skipped = -1;
        }
        final ArrayList<IStoredJobLoadResult> resultList = new ArrayList<IStoredJobLoadResult>();
        if (succeeded > 0) {
            logger.debug("Succeeded creating/updating " + succeeded + " Jobs:");
            final List nodes = result.selectNodes("/result/succeeded/job");
            for (final Object node2 : nodes) {
                final Node node1 = (Node) node2;
                final IStoredJobLoadResult storedJobLoadResult = parseAPIJobResult(node1, true, false,
                    "Succeeded");
                resultList.add(storedJobLoadResult);

            }
        }
        if (failed > 0) {
            logger.debug("Failed to add " + failed + " Jobs:");
            final List nodes = result.selectNodes("/result/failed/job");
            for (final Object node2 : nodes) {
                final Node node1 = (Node) node2;
                final String error = null != node1.selectSingleNode("error") ? node1.selectSingleNode("error")
                    .getStringValue() : "Failed";
                final IStoredJobLoadResult storedJobLoadResult = parseAPIJobResult(node1, false, false,
                    error);

                resultList.add(storedJobLoadResult);
            }
        }
        if (skipped > 0) {
            logger.debug("Skipped " + skipped + " Jobs:");
            final List nodes = result.selectNodes("/result/skipped/job");
            for (final Object node2 : nodes) {
                final Node node1 = (Node) node2;

                final String error = null != node1.selectSingleNode("error") ? node1.selectSingleNode("error")
                    .getStringValue() : "Skipped";
                final IStoredJobLoadResult storedJobLoadResult = parseAPIJobResult(node1, true, true,
                    error);
                resultList.add(storedJobLoadResult);

            }
        }
        return resultList;
    }

    private IStoredJobLoadResult parseAPIJobResult(final Node node1, final boolean successful, final boolean skippedJob,
                                                   final String message) {
        final Node idNode = node1.selectSingleNode("id");
        final String id = null != idNode ? idNode.getStringValue() : null;
        final String name = node1.selectSingleNode("name").getStringValue();
        final String url = null != id ? createJobURL(id) : null;
        final String group = null != node1.selectSingleNode("group") ? node1.selectSingleNode("group")
            .getStringValue() : null;
        final String description = null != node1.selectSingleNode("description") ? node1.selectSingleNode("description")
            .getStringValue() : null;
        logger.debug("\t" + name + " [" + id + "] <" + url + ">");
        return StoredJobLoadResultImpl.createLoadResult(id, name, url,
            group, description, successful, skippedJob,
            message);
    }

    /**
     * Utility to serialize Document as a String for debugging
     *
     * @param document document
     *
     * @return xml string
     *
     * @throws java.io.IOException if error occurs
     */
    private static String serialize(final Document document) throws IOException {
        final OutputFormat format = OutputFormat.createPrettyPrint();
        final StringWriter sw = new StringWriter();
        final XMLWriter writer = new XMLWriter(sw, format);
        writer.write(document);
        writer.flush();
        sw.flush();
        return sw.toString();
    }

    /**
     * Defines a wrapper around a DOM object, providing error/success boolean results, as well as utility methods for
     * returning embedded success or error messages.
     */
    private class Envelope {
        private Document doc;

        public Envelope(final Document doc) {
            this.doc = doc;
        }

        public boolean isErrorResult() {
            final Node node = doc.selectSingleNode("/result/@error");
            return null != node && "true".equals(node.getText());
        }

        public boolean isSuccessResult() {
            final Node node = doc.selectSingleNode("/result/@success");
            return null == node || "true".equals(node.getText());
        }

        public StringBuffer successMessages() {
            return readMessages("/result/success/message");
        }

        public StringBuffer errorMessages() {
            return readMessages("/result/error/message");
        }

        private StringBuffer readMessages(final String xpath) {
            final StringBuffer sb = new StringBuffer();
            final List errs = doc.selectNodes(xpath);
            if (null != errs) {
                for (final Object err : errs) {
                    final Node node = (Node) err;
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(node.getStringValue());
                    logger.error("\t" + node.getStringValue());
                }
            }
            return sb;
        }
    }
}

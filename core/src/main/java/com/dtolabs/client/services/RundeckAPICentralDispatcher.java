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
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.utils.OptsUtil;
import com.dtolabs.utils.Streams;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    public static final String RUNDECK_API_VERSION = "2";
    public static final String RUNDECK_API_VERSION_4 = "4";
    public static final String RUNDECK_API_VERSION_5 = "5";
    public static final String RUNDECK_API_VERSION_8 = "8";
    public static final String RUNDECK_API_VERSION_9 = "9";
    /**
     * RUNDECK API base path
     */
    public static final String RUNDECK_API_BASE = "/api/" + RUNDECK_API_VERSION;

    /**
     * RUNDECK API Base for v4
     */
    public static final String RUNDECK_API_BASE_v4 = "/api/" + RUNDECK_API_VERSION_4;
    /**
     * RUNDECK API Base for v5
     */
    public static final String RUNDECK_API_BASE_v5 = "/api/" + RUNDECK_API_VERSION_5;
    /**
     * RUNDECK API Base for v8
     */
    public static final String RUNDECK_API_BASE_v8 = "/api/" + RUNDECK_API_VERSION_8;
    /**
     * RUNDECK API Base for v8
     */
    public static final String RUNDECK_API_BASE_v9 = "/api/" + RUNDECK_API_VERSION_9;

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
     * Webservice endpoint for running a script from a URL
     */
    public static final String RUNDECK_API_RUN_URL = RUNDECK_API_BASE_v4 + "/run/url";
    /**
     * Webservice endpoint for queue list requests
     */
    public static final String RUNDECK_API_LIST_EXECUTIONS_PATH = RUNDECK_API_BASE + "/executions/running";
    /**
     * Webservice endpoint for getting execution information
     */
    public static final String RUNDECK_API_EXECUTION_PATH = RUNDECK_API_BASE + "/execution/$id";
    /**
     * Webservice endpoint for killing job executions
     */
    public static final String RUNDECK_API_KILL_JOB_PATH = RUNDECK_API_BASE + "/execution/$id/abort";
    /**
     * Webservice endpoint for execution output
     */
    public static final String RUNDECK_API_EXEC_OUTPUT_PATH = RUNDECK_API_BASE_v5 + "/execution/$id/output";
    /**
     * Webservice endpoint for exporting stored jobs.
     */
    public static final String RUNDECK_API_JOBS_EXPORT_PATH = RUNDECK_API_BASE + "/jobs/export";
    /**
     * Webservice endpoint for exporting stored jobs.
     */
    public static final String RUNDECK_API_JOBS_BULK_DELETE_PATH = RUNDECK_API_BASE_v5 + "/jobs/delete";

    /**
     * Webservice endpoint for listing stored jobs.
     */
    public static final String RUNDECK_API_JOBS_LIST_PATH = RUNDECK_API_BASE + "/jobs";

    /**
     * upload path
     */
    public static final String RUNDECK_API_JOBS_UPLOAD = RUNDECK_API_BASE_v9 + "/jobs/import";
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
     * @param tags tags
     * @param script           script content (can be null if summary specified)
     * @param summary          summary of execution (can be null if script specified)
     * @param start            start date (can be null)
     * @param end              end date (can be null)
     *
     * @throws com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException on error
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
            response = serverService.makeRundeckRequest(RUNDECK_API_EXECUTION_REPORT, null, params);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }

        validateResponse(response);

    }


    public QueuedItemResult queueDispatcherScript(final IDispatchedScript iDispatchedScript) throws
        CentralDispatcherException {
        final List<String> args = new ArrayList<String>();
        final String scriptString;
        String scriptURL=null;
        final boolean isExec;
        final boolean isUrl;
        File uploadFile=null;

        //write script to file
        final InputStream stream = iDispatchedScript.getScriptAsStream();
        if (null != iDispatchedScript.getScript() || null != stream) {
            //full script
            if (null != iDispatchedScript.getScript()) {
                scriptString = iDispatchedScript.getScript();
            } else {
                //read stream to string
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    Streams.copyStream(stream, byteArrayOutputStream);
                } catch (IOException e) {
                    throw new CentralDispatcherServerRequestException("Unable to queue command: " + e.getMessage(), e);
                }
                scriptString = new String(byteArrayOutputStream.toByteArray());
            }
            isExec = false;
            isUrl = false;
        } else if (null != iDispatchedScript.getServerScriptFilePath()) {
            //server-local script filepath
            uploadFile = new File(iDispatchedScript.getServerScriptFilePath());
            isExec = false;
            isUrl = false;
        }else if(null!=iDispatchedScript.getScriptURLString()) {
            //read stream to string
            scriptURL = iDispatchedScript.getScriptURLString();
            scriptString=null;
            isExec = false;
            isUrl = true;
        } else if (null != iDispatchedScript.getArgs() && iDispatchedScript.getArgs().length > 0) {
            //shell command
            scriptString = null;
            isExec = true;
            isUrl = false;
        } else {
            throw new IllegalArgumentException("Dispatched script did not specify a command, script or filepath");
        }
        if (null != iDispatchedScript.getArgs() && iDispatchedScript.getArgs().length > 0) {
            args.addAll(Arrays.asList(iDispatchedScript.getArgs()));
        }

        //request parameters
        final HashMap<String, String> params = new HashMap<String, String>();
        final HashMap<String, String> data = new HashMap<String, String>();

        params.put("project", iDispatchedScript.getFrameworkProject());
        if (isExec) {
            data.put("exec", OptsUtil.join(args));
        }else if (null != scriptURL) {
            data.put("scriptURL", scriptURL);
        }
        if (!isExec && args.size() > 0) {
            params.put("argString", OptsUtil.join(args));
        }
        addLoglevelParams(params, iDispatchedScript.getLoglevel());
        addAPINodeSetParams(params, iDispatchedScript.isKeepgoing(), iDispatchedScript.getNodeFilter(),
                iDispatchedScript.getNodeThreadcount(), iDispatchedScript.getNodeExcludePrecedence());

        return submitRunRequest(uploadFile,
                                params,
                                data,
                                isExec ? RUNDECK_API_RUN_COMMAND : isUrl ? RUNDECK_API_RUN_URL : RUNDECK_API_RUN_SCRIPT,
                                "scriptFile");
    }

    /**
     * Submit a request to the server which expects a list of execution items in the response, and return a single
     * QueuedItemResult parsed from the response.
     *
     * @param tempxml     xml temp file (or null)
     * @param otherparams parameters for the request
     * @param requestPath path
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
        final HashMap<String, String> formdata = new HashMap<String, String>();
        formdata.put("a", "a");
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(requestPath, params, tempxml, "POST", null,formdata,"xmlBatch");
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
     * @param uploadFileParam name of file upload parameter
     * @param tempxml     xml temp file (or null)
     * @param otherparams parameters for the request
     * @param requestPath path
     *
     * @return a single QueuedItemResult
     *
     * @throws com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException
     *          if an error occurs
     */
    private QueuedItemResult submitRunRequest(final File tempxml,
                                              final HashMap<String, String> otherparams,
                                              final HashMap<String, String> dataValues,
                                              final String requestPath,
                                              final String uploadFileParam) throws CentralDispatcherException {


        final HashMap<String, String> params = new HashMap<String, String>();
        if (null != otherparams) {
            params.putAll(otherparams);
        }
        final HashMap<String, String> data = new HashMap<String, String>();
        if (null != dataValues) {
            data.putAll(dataValues);
        }

        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(requestPath, params, tempxml, null, null, data, uploadFileParam);
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

    /**
     * List the items on the dispatcher queue for a project
     *
     * @return Collection of Strings listing the active dispatcher queue items
     *
     * @throws CentralDispatcherException if an error occurs
     */
    public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
        throw new CentralDispatcherException(
            "Unsupported operation: project is required by the RunDeck API");
    }
    /**
     * List the items on the dispatcher queue for a project
     *
     * @param project Project name
     *
     * @return Collection of Strings listing the active dispatcher queue items
     *
     * @throws CentralDispatcherException if an error occurs
     */
    public Collection<QueuedItem> listDispatcherQueue(final String project) throws CentralDispatcherException {
        if(null==project){
            throw new CentralDispatcherException(
                "Unsupported operation: project is required by the RunDeck API");
        }
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("project", project);

        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_API_LIST_EXECUTIONS_PATH, params, null, null,
                                                        null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }

        validateResponse(response);

        ////////////////////
        //parse result list of queued items, return the collection of QueuedItems
        ///////////////////

        return parseExecutionListResult(response);

    }

    private List<ExecutionDetail> parseExecutionsResult(final WebserviceResponse response) {
        final Document resultDoc = response.getResultDoc();

        final Node node = resultDoc.selectSingleNode("/result/executions");
        final List items = node.selectNodes("execution");
        final ArrayList<ExecutionDetail> list = new ArrayList<ExecutionDetail>();
        if (null != items && items.size() > 0) {
            for (final Object o : items) {
                final Node node1 = (Node) o;
                ExecutionDetailImpl detail = new ExecutionDetailImpl();
                String url = node1.selectSingleNode("@href").getStringValue();
                url = makeAbsoluteURL(url);
                detail.setId(stringNodeValue(node1, "@id", null));
                try {
                    detail.setStatus(ExecutionState.valueOf(stringNodeValue(node1, "@status", null)));
                } catch (IllegalArgumentException e) {
                }
                detail.setUrl(url);
                detail.setUser(stringNodeValue(node1, "user", null));
                detail.setAbortedBy(stringNodeValue(node1, "abortedBy", null));
                detail.setDescription(stringNodeValue(node1, "description", null));
                detail.setArgString(stringNodeValue(node1, "argString", null));
                detail.setDateStarted(w3cDateNodeValue(node1, "date-started", null));
                detail.setDateCompleted(w3cDateNodeValue(node1, "date-started", null));
                final Node jobNode = node1.selectSingleNode("job");
                if(null!=jobNode){
                    final String jobId = stringNodeValue(jobNode, "@id", null);
                    StoredJobExecutionImpl job = new StoredJobExecutionImpl(
                        jobId,
                        stringNodeValue(jobNode,"name",null),
                        createJobURL(jobId),
                        stringNodeValue(jobNode,"group",null),
                        stringNodeValue(jobNode,"description",null),
                        stringNodeValue(jobNode,"project",null),
                        longNodeValue(jobNode, "@averageDuration", -1 )
                    );
                    detail.setExecutionJob(job);
                }
                list.add(detail);
            }
        }
        return list;
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
            logger.debug("Response: " + response.getResponseMessage());
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
        final HashMap<String, String> data = new HashMap<String, String>();
        //:( trigger POST correctly
        data.put("a", "a");

        final String rundeckApiKillJobPath = substitutePathVariable(RUNDECK_API_KILL_JOB_PATH, "id", execId);
        //2. send request via ServerService
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(rundeckApiKillJobPath, params, null, "POST", null,data,null);
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
     * Return execution detail for a particular execution.
     *
     * @param execId ID of the execution
     *
     * @return Execution detail
     */
    public ExecutionDetail getExecution(final String execId) throws CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();

        final String rundeckApiKillJobPath = substitutePathVariable(RUNDECK_API_EXECUTION_PATH, "id", execId);
        //2. send request via ServerService
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(rundeckApiKillJobPath, params, null, null, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }

        final Envelope envelope = validateResponse(response);

//        try {
//            System.err.println(serialize(response.getResultDoc()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        ////////////////////
        //parse result list of queued items, return the collection of QueuedItems
        ///////////////////

        final List<ExecutionDetail> details = parseExecutionsResult(response);
        if (details.size() != 1) {
            throw new CentralDispatcherException("The results were unexpected: did not contain 1 execution definition");
        }
        final ExecutionDetail detail = details.get(0);
        return detail;
    }

    /**
     * Follow execution output for an Execution by synchronously emitting output to a receiver
     * @param execId execution ID
     * @param request request
     * @param receiver output receiver
     * @return result
     * @throws CentralDispatcherException on error
     */
    public ExecutionFollowResult followDispatcherExecution(final String execId, final ExecutionFollowRequest request,
                                                           final ExecutionFollowReceiver receiver) throws
        CentralDispatcherException {


        final String rundeckApiExecOutputJobPath = substitutePathVariable(RUNDECK_API_EXEC_OUTPUT_PATH, "id", execId)
                                                   + ".xml";
        //output complete
        boolean complete = false;
        boolean interrupt = false;
        boolean receiverfinished = false;
        boolean jobsuccess = false;
        boolean jobcomplete = false;
        boolean jobcancel = false;
        //byte offset
        Long offset = 0L;
        Long rlastmod = 0L;
        boolean resume = null != request && request.isResume();

        //percent complete
        double percentage = 0.0;
        //delay between requests
        final int BASE_DELAY = 1000;
        final int MAX_DELAY = 5000;
        long delay = BASE_DELAY;
        float backoff = 1f;
        String jobstatus=null;

        while (!complete && !interrupt && !receiverfinished) {
            //follow output until complete

            final HashMap<String, String> params = new HashMap<String, String>();

            if(resume){
                params.put("lastlines", "0");
                resume=false;
            }else{
                params.put("offset", offset.toString());
                params.put("lastmod", rlastmod.toString());
            }
            params.put("maxlines", "500");

            logger.debug("request" + rundeckApiExecOutputJobPath + " params: " + params);
            //2. send request via ServerService
            final WebserviceResponse response;
            try {
                response = serverService.makeRundeckRequest(rundeckApiExecOutputJobPath, params, null, null, null);
            } catch (MalformedURLException e) {
                throw new CentralDispatcherServerRequestException("Failed to make request", e);
            }

            final Envelope envelope = validateResponse(response);

            final Node result1 = envelope.doc.selectSingleNode("result/output");
            if(null==result1){
                throw new CentralDispatcherServerRequestException("Response output was unexpected");
            }
            final String errorStr = stringNodeValue(result1, "error", null);
            final String messageStr = stringNodeValue(result1, "message", null);
            final Boolean unmodified = boolNodeValue(result1, "unmodified", null);
            final Boolean empty = boolNodeValue(result1, "empty", null);

            final Boolean iscompleted = boolNodeValue(result1, "completed", null);
            final Boolean jobcompleted = boolNodeValue(result1, "execCompleted", null);
            jobstatus = stringNodeValue(result1, "execState", null);

            final Long lastmod = longNodeValue(result1, "lastModified", 0);
            final Long duration = longNodeValue(result1, "execDuration", 0);
            final Long totalsize = longNodeValue(result1, "totalSize", 0);

            final Double percentLoaded = floatNodeValue(result1, "percentLoaded", 0.0);
            final Long dataoffset = longNodeValue(result1, "offset", -1L);

            if (dataoffset > 0 && dataoffset > offset) {
                offset = dataoffset;
            }
            if (lastmod > 0 && lastmod > rlastmod) {
                rlastmod = lastmod;
            }
            if (percentLoaded > 0.0 && percentLoaded > percentage) {
                percentage = percentLoaded;
            }

            //update delay
            if (null != unmodified && unmodified && delay < MAX_DELAY) {
                delay = delay + (Math.round(backoff * BASE_DELAY));
            } else if (null != unmodified && !unmodified) {
                delay = BASE_DELAY;
            }

            if(null!=iscompleted){
                complete=iscompleted;
            }

            if (null != receiver && !receiver.receiveFollowStatus(offset, totalsize, duration)) {
                //end
                receiverfinished = true;
                break;
            }

            final List list = result1.selectNodes("entries/entry");
            for (final Object obj : list) {
                Node node = (Node) obj;
                final String timeStr = stringNodeValue(node, "@time", null);
                final String levelStr = stringNodeValue(node, "@level", null);
                final String user = stringNodeValue(node, "@user", null);
                final String command = stringNodeValue(node, "@command", null);
                final String nodeName = stringNodeValue(node, "@node", null);
                final String logMessage = node.getStringValue();
                if (null != receiver && !receiver.receiveLogEntry(timeStr, levelStr, user, command, nodeName,
                    logMessage)) {
                    receiverfinished = true;
                    break;
                }
            }
            //sleep delay
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                interrupt = true;
            }
        }
        final boolean finalComplete = complete;
        final boolean finalReceiverfinished = receiverfinished;
        ExecutionState state=null;
        if(null!=jobstatus){
            try {
                state = ExecutionState.valueOf(jobstatus);
            } catch (IllegalArgumentException e){
            }
        }
        final ExecutionState finalState = state;
        return new ExecutionFollowResult() {
            public boolean isLogComplete() {
                return finalComplete;
            }

            public ExecutionState getState() {
                return finalState;
            }

            public boolean isReceiverFinished() {
                return finalReceiverfinished;
            }
        };
    }

    private String stringNodeValue(Node result1, final String path, final String defValue) {
        return null != result1.selectSingleNode(path) ? result1.selectSingleNode(path)
            .getStringValue() : defValue;
    }

    private Boolean boolNodeValue(final Node result1, final String path, final Boolean defValue) {
        if(null != result1.selectSingleNode(path)) {
            return Boolean.parseBoolean(result1.selectSingleNode(path).getStringValue());
        }
        return defValue;
    }

    private double floatNodeValue(final Node result1, final String path, final double defValue) {
        if (null != result1.selectSingleNode(path)) {
            try {
                Float.parseFloat(result1.selectSingleNode(path).getStringValue());
            } catch (NumberFormatException e) {

            }
        }
        return defValue;
    }

    private long longNodeValue(final Node result1, final String path, final long defValue) {
        if (null != result1.selectSingleNode(path)) {
            try {
                return Long.parseLong(
                    result1.selectSingleNode(path).getStringValue());
            } catch (NumberFormatException e) {

            }
        }
        return defValue;
    }

    private static final SimpleDateFormat w3cDateFormat = new SimpleDateFormat(""){{
        setTimeZone(TimeZone.getTimeZone("GMT"));
    }};
    private Date w3cDateNodeValue(final Node result1, final String path, final Date defValue) {
        if (null != result1.selectSingleNode(path)) {
            final String stringValue = result1.selectSingleNode(path).getStringValue();
            try {
                return w3cDateFormat.parse(stringValue);
            } catch (ParseException e) {

            }
        }
        return defValue;
    }

    /**
     * @return Replace a "$var" within a path string with a value, properly encoding it.
     * @param path the URL path to substitute the var within
     * @param var the name of the var in the string
     * @param value the value to substitute
     * @throws com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException  on URIException
     */
    public static String substitutePathVariable(final String path, final String var, final String value) throws
        CentralDispatcherException {
        final String encoded;
        try {
            encoded = URIUtil.encodeWithinPath(value);
        } catch (URIException e) {
            throw new CentralDispatcherException(e);
        }
        return path.replace("$" + var, encoded);
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

        final String expectedContentType;
        if (null != fformat) {
            params.put("format", fformat.getName());
            expectedContentType = fformat == JobDefinitionFileFormat.xml ? "text/xml" : "text/yaml";
        } else {
            params.put("format", JobDefinitionFileFormat.xml.getName());
            expectedContentType = "text/xml";
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
            response = serverService.makeRundeckRequest(RUNDECK_API_JOBS_EXPORT_PATH, params, null, null,
                expectedContentType, null);
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
                    final Node uuid = node1.selectSingleNode("uuid");
                    final Node id1 = node1.selectSingleNode("id");
                    final String id = null!=uuid?uuid.getStringValue():id1.getStringValue();
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
            //do rough yaml parse

            final Collection<Map> mapCollection;
            //write to temp file

            File temp;
            InputStream tempIn;
            try {
                temp= File.createTempFile("listStoredJobs", ".yaml");
                temp.deleteOnExit();
                OutputStream os = new FileOutputStream(temp);
                try{
                    Streams.copyStream(response.getResultStream(), os);
                }finally {
                    os.close();
                }
                tempIn = new FileInputStream(temp);
                try {
                    mapCollection = validateJobsResponseYAML(response, tempIn);
                } finally {
                    tempIn.close();
                }
            } catch (IOException e) {
                throw new CentralDispatcherServerRequestException(e);
            }
            final ArrayList<IStoredJob> list = new ArrayList<IStoredJob>();

            if (null == mapCollection || mapCollection.size() < 1) {
                return list;
            }
            for (final Map map : mapCollection) {
                final Object uuidobj = map.get("uuid");
                final Object idobj = map.get("id");
                final String id = null != uuidobj ? uuidobj.toString() : idobj.toString();
                final String name = (String) map.get("name");
                final String group = map.containsKey("group") ? (String) map.get("group") : null;
                final String desc = map.containsKey("description") ? (String) map.get("description") : "";
                final String url = createJobURL(id);
                list.add(StoredJobImpl.create(id, name, url, group, desc, projectFilter));
            }

            if (null != output) {
                //write output doc to the outputstream
                try {
                    tempIn = new FileInputStream(temp);
                    try {
                        Streams.copyStream(tempIn, output);
                    }finally {
                        tempIn.close();
                    }
                    temp.delete();
                } catch (IOException e) {
                    throw new CentralDispatcherServerRequestException(e);
                }
            }
            return list;
        }
        return null;
    }

    public Collection<DeleteJobResult> deleteStoredJobs(Collection<String> jobIds) throws CentralDispatcherException {
        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("ids", jobIds);
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_API_JOBS_BULK_DELETE_PATH, null, params );
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }
        checkErrorResponse(response);

        ////////////////////
        //parse result list of delete result items, return the collection of DeleteJobResult
        ///////////////////

        final Document result = response.getResultDoc();

        final int succeeded;
        final int failed;
        Node node = result.selectSingleNode("/result/deleteJobs/succeeded/@count");
        if (null != node) {
            succeeded = Integer.parseInt(node.getStringValue());
        } else {
            succeeded = -1;
        }
        node = result.selectSingleNode("/result/deleteJobs/failed/@count");
        if (null != node) {
            failed = Integer.parseInt(node.getStringValue());
        } else {
            failed = -1;
        }
        final ArrayList<DeleteJobResult> resultList = new ArrayList<DeleteJobResult>();
        if (succeeded > 0) {
            logger.debug("Succeeded deleting " + succeeded + " Jobs:");
            final List nodes = result.selectNodes("/result/deleteJobs/succeeded/deleteJobResult");
            for (final Object node2 : nodes) {
                final Node node1 = (Node) node2;
                final String message = null != node1.selectSingleNode("message") ? node1.selectSingleNode("message")
                    .getStringValue() : "Succeeded";
                final DeleteJobResult storedJobLoadResult = parseAPIJobDeleteResult(node1, true, message);
                resultList.add(storedJobLoadResult);

            }
        }
        if (failed > 0) {
            logger.debug("Failed to delete " + failed + " Jobs:");
            final List nodes = result.selectNodes("/result/deleteJobs/failed/deleteJobResult");
            for (final Object node2 : nodes) {
                final Node node1 = (Node) node2;
                final String error = null != node1.selectSingleNode("error") ? node1.selectSingleNode("error")
                    .getStringValue() : "Failed";
                final DeleteJobResult storedJobLoadResult = parseAPIJobDeleteResult(node1, false, error);

                resultList.add(storedJobLoadResult);
            }
        }

        return resultList;
    }

    public Collection<IStoredJob> reallistStoredJobs(final IStoredJobsQuery iStoredJobsQuery) throws
        CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();
        final String nameMatch = iStoredJobsQuery.getNameMatch();
        String groupMatch = iStoredJobsQuery.getGroupMatch();
        final String projectFilter = iStoredJobsQuery.getProjectFilter();
        final String idlistFilter = iStoredJobsQuery.getIdlist();

        if (null != nameMatch) {
            params.put("jobExactFilter", nameMatch);
        }
        if (null != groupMatch) {
            final Matcher matcher = Pattern.compile("^/*(.+?)/*$").matcher(groupMatch);
            if (matcher.matches()) {
                //strip leading and trailing slashes
                groupMatch = matcher.group(1);
            }
            params.put("groupPathExact", groupMatch);
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
            response = serverService.makeRundeckRequest(RUNDECK_API_JOBS_LIST_PATH, params, null, null, null);
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
            final String desc = job.selectSingleNode("description").getStringValue();
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
     * @param resultStream input stream
     * @return Collection of job data maps if format is correct and there is no error
     *
     * @throws com.dtolabs.client.services.CentralDispatcherServerRequestException
     *          if the format is incorrect, or the response indicates an error response.
     */
    private Collection<Map> validateJobsResponseYAML(final WebserviceResponse response, final InputStream resultStream) throws
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
            resobj = yaml.load(resultStream);
        } catch (YAMLException e) {
            throw new CentralDispatcherServerRequestException("Failed to parse YAML: " + e.getMessage(), e);
        }
        if (resobj instanceof Collection) {
            dataset.addAll((Collection) resobj);
        } else {
            throw new CentralDispatcherServerRequestException("Response had unexpected content type: " + resobj);
        }

        for (final Map map : dataset) {
            if (!map.containsKey("name") || !map.containsKey("id") && !map.containsKey("uuid")) {
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
            if(null==dispatchedJob.getJobRef().getName() || "".equals(dispatchedJob.getJobRef().getName())) {
                throw new CentralDispatcherException("job name input is required");
            }
            final String project = dispatchedJob.getJobRef().getProject();
            final String name = dispatchedJob.getJobRef().getName();
            final String group;
            if (null != dispatchedJob.getJobRef().getGroup() && !"".equals(dispatchedJob.getJobRef().getGroup())) {
                group = dispatchedJob.getJobRef().getGroup();
            } else {
                //indicates a top level job
                group = "-";
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
                ArrayList<String> ids = new ArrayList<String>();
                for (final IStoredJob iStoredJob : iStoredJobs) {
                    ids.add(iStoredJob.getJobId());
                }
                throw new CentralDispatcherException(
                    "Job was not unique: " + (null != group ? group : "") + "/" + name + ": " + iStoredJobs.size()
                    + " jobs found: " + ids);
            }
            //use found id
            final IStoredJob next = iStoredJobs.iterator().next();
            jobid = next.getJobId();

        }

        addAPINodeSetParams(params, dispatchedJob.isKeepgoing(), dispatchedJob
                .getNodeFilter(), dispatchedJob.getNodeThreadcount(), dispatchedJob.getNodeExcludePrecedence());
        addLoglevelParams(params, dispatchedJob.getLoglevel());
        if (null != dispatchedJob.getArgs() && dispatchedJob.getArgs().length > 0) {
            params.put("argString", OptsUtil.join(dispatchedJob.getArgs()));
        }

        final String apipath = substitutePathVariable(RUNDECK_API_JOBS_RUN, "id", jobid);
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
     * @param params request params
     * @param isKeepgoing true keepgoing
     * @param nodeFilter node filter string
     * @param threadcount thread count
     * @param excludePrecedence precedence
     */
    public static void addAPINodeSetParams(final HashMap<String, String> params,
            final Boolean isKeepgoing, String nodeFilter, int threadcount, Boolean excludePrecedence) {
        if (null != nodeFilter && !"".equals(nodeFilter.trim())) {
            params.put("filter", nodeFilter);
            if (null != excludePrecedence) {
                params.put("exclude-precedence", Boolean.toString(excludePrecedence));
            }
        }
        if (threadcount > 0) {
            params.put("nodeThreadcount", Integer.toString(threadcount));
        }
        if (null != isKeepgoing) {
            params.put("nodeKeepgoing", Boolean.toString(isKeepgoing));
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

        if (null != iLoadJobsRequest.getProject()) {
            params.put("project", iLoadJobsRequest.getProject());
        }
        if(null!= iLoadJobsRequest.getUUIDOption()) {
            params.put("uuidOption", iLoadJobsRequest.getUUIDOption().toString());
        }

        /*
         * Send the request bean and the file as a multipart request.
         */

        //2. send request via ServerService
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_API_JOBS_UPLOAD, params, input, null, "xmlBatch");
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

    private DeleteJobResult parseAPIJobDeleteResult(final Node node1, final boolean successful, final String message) {
        final Node idNode = node1.selectSingleNode("@id");
        final String id = null != idNode ? idNode.getStringValue() : null;
        final Node codeNode = node1.selectSingleNode("errorCode");
        final String errorCode = null != codeNode ? codeNode.getStringValue() : null;
        logger.debug("\t[" + id + "] " + message);
        return DeleteJobResultImpl.createDeleteJobResultImpl(successful, message, id, errorCode);
    }
    private IStoredJobLoadResult parseAPIJobResult(final Node node1, final boolean successful, final boolean skippedJob,
                                                   final String message) {
        final Node uuidNode = node1.selectSingleNode("uuid");
        final Node idNode = node1.selectSingleNode("id");
        final String id = null != uuidNode ? uuidNode.getStringValue() :null != idNode ? idNode.getStringValue() : null;
        final String name = node1.selectSingleNode("name").getStringValue();
        final String url = null != id ? createJobURL(id) : null;
        final String group = null != node1.selectSingleNode("group") ? node1.selectSingleNode("group")
            .getStringValue() : null;
        final String description = null != node1.selectSingleNode("description") ? node1.selectSingleNode("description")
            .getStringValue() : null;
        final String project = null != node1.selectSingleNode("project") ? node1.selectSingleNode("project").getStringValue() : null;
        logger.debug("\t" + name + " [" + id + "] <" + url + "> (" + project + ")");
        return StoredJobLoadResultImpl.createLoadResult(id, name, url, group, description, project, successful,
            skippedJob, message);
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
    private static class Envelope {
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

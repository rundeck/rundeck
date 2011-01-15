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
* RundeckCentralDispatcher.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 18, 2010 11:59:34 AM
* $Id$
*/
package com.dtolabs.client.services;

import com.dtolabs.client.utils.WebserviceResponse;
import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.utils.NodeSet;
import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RundeckCentralDispatcher serializes central dispatcher requests and submits them to the central dispatcher in the
 * run-center application, via a remote web service call.  The responses are parsed and returned.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class RundeckCentralDispatcher implements CentralDispatcher {
    /**
     * Webservice endpoint for queuing job executions
     */
    public static final String RUNDECK_EXEC_JOB_PATH = "/scheduledExecution/uploadAndExecute.xml";
    /**
     * Webservice endpoint for queue list requests
     */
    public static final String RUNDECK_LIST_EXECUTIONS_PATH = "/menu/queueList.xml";
    /**
     * Webservice endpoint for killing job executions
     */
    public static final String RUNDECK_KILL_JOB_PATH = "/execution/cancelExecution.xml";
    /**
     * Webservice endpoint for listing stored jobs.
     */
    public static final String RUNDECK_LIST_STORED_JOBS_PATH = "/menu/workflows.xml";
    /**
     * Webservice link prefix for a stored job.
     */
    public static final String RUNDECK_JOB_LINK_PREFIX = "/scheduledExecution/show/";
    /**
     * upload path
     */
    public static final String RUNDECK_JOBS_UPLOAD = "/scheduledExecution/upload";
    /**
     * Webservice endpoint for running job by name or id
     */
    public static final String RUNDECK_JOBS_RUN = "/scheduledExecution/runJobByName.xml";
    /**
     * logger
     */
    public static final Logger logger = Logger.getLogger(RundeckCentralDispatcher.class);
    private ServerService serverService;

    /**
     * Create a RundeckCentralDispatcher
     *
     * @param framework the framework
     */
    public RundeckCentralDispatcher(final Framework framework) {
        serverService = new ServerService(framework);
    }



    public QueuedItemResult queueDispatcherScript(final IDispatchedScript iDispatchedScript) throws
        CentralDispatcherException {
        final File tempxml;
        try {
            tempxml = File.createTempFile("dispatch", "xml");
            JobDefinitionSerializer.serializeToFile(iDispatchedScript, tempxml);
        } catch (IOException e) {
            throw new CentralDispatcherServerRequestException("Unable to queue command: " + e.getMessage(), e);
        }
        final QueuedItemResult queuedItemResult = submitQueueRequest(tempxml, new HashMap<String, String>(),
            RUNDECK_EXEC_JOB_PATH);
        if (!tempxml.delete()) {
            logger.warn("Failed to delete temp file: " + tempxml.getAbsolutePath());
        }
        return queuedItemResult;
    }

    /**
     * Submit a request to the server which expects a list of queued item results in the response, and return a single QueuedItemResult parsed from the response.
     *
     * @param tempxml xml temp file (or null)
     * @param otherparams parameters for the request
     *
     * @param requestPath
     * @return a single QueuedItemResult
     *
     * @throws CentralDispatcherException if an error occurs
     */
    private QueuedItemResult submitQueueRequest(final File tempxml, final HashMap<String, String> otherparams,
                                                final String requestPath) throws CentralDispatcherException {


        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("xmlreq", "true");
        if(null!=otherparams){
            params.putAll(otherparams);
        }

        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(requestPath, params, tempxml, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }
        validateResponse(response);

        ////////////////////
        //parse result list of execution responses.  this implementation handles multiple responses, but only
        //returns a single QueuedItem (the first one found).
        //TODO: update to return multiple QueuedItems when multiple job queue requests are supported
        ///////////////////

        final Document resultDoc = response.getResultDoc();

        final int succeeded;
        final int failed;

        Node node = resultDoc.selectSingleNode("/result/succeeded/@count");
        if (null != node) {
            succeeded = Integer.parseInt(node.getStringValue());
        } else {
            succeeded = -1;
        }
        node = resultDoc.selectSingleNode("/result/failed/@count");
        if (null != node) {
            failed = Integer.parseInt(node.getStringValue());
        } else {
            failed = -1;
        }
        final String succeededId;
        if (succeeded > 0) {
            logger.info("Succeeded queueing " + succeeded + " Job" + (succeeded > 1 ? "s" : "") + ":");
            final List nodes = resultDoc.selectNodes("/result/succeeded/execution");
            final Node node1 = (Node) nodes.iterator().next();
            final String index = node1.selectSingleNode("@index").getStringValue();
            final String id = node1.selectSingleNode("id").getStringValue();
            succeededId = id;
            final String name = node1.selectSingleNode("name").getStringValue();
            String url = node1.selectSingleNode("url").getStringValue();
            url = makeAbsoluteURL(url);
            logger.info("\t" + index + ": " + name + " [" + id + "] <" + url + ">");
            return QueuedItemResultImpl.successful("Succeeded queueing " + name, succeededId, url, name);
        }
        if (failed > 0) {
            final String s1 = "Failed to queue " + failed + " Job" + (failed > 1 ? "s" : "") + ":";
            logger.error(s1);
            final List nodes = resultDoc.selectNodes("/result/failed/execution");
            final Node node1 = (Node) nodes.iterator().next();
            final String index = node1.selectSingleNode("@index").getStringValue();
            final String id = null != node1.selectSingleNode("id") ? node1.selectSingleNode("id")
                .getStringValue()
                                                                   : null;
            String url = null != node1.selectSingleNode("url") ? node1.selectSingleNode("url")
                .getStringValue()
                                                               : null;
            url = makeAbsoluteURL(url);
            final String error = null != node1.selectSingleNode("error") ? node1.selectSingleNode("error")
                .getStringValue() : null;
            final String message = null != node1.selectSingleNode("message") ? node1.selectSingleNode("message")
                .getStringValue() : null;
            final String errmsg = error + (null != id ? " [" + id + "] <" + url + ">" : "")
                                  + (null != message ? " : " + message : "");
            final String s2 = index + ": " + errmsg;
            logger.error(s2);
            return QueuedItemResultImpl.failed(errmsg);
        }
        return QueuedItemResultImpl.failed("Server response contained no success information.");
    }

    public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("xmlreq", "true");

        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_LIST_EXECUTIONS_PATH, params, null, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }

        validateResponse(response);

        ////////////////////
        //parse result list of queued items, return the collection of QueuedItems
        ///////////////////

        final Document resultDoc = response.getResultDoc();

        final Node node = resultDoc.selectSingleNode("/result/items");
        final List items = node.selectNodes("item");
        final ArrayList<QueuedItem> list = new ArrayList<QueuedItem>();
        if (null != items && items.size() > 0) {
            for (final Object o : items) {
                final Node node1 = (Node) o;
                final String id = node1.selectSingleNode("id").getStringValue();
                final String name = node1.selectSingleNode("name").getStringValue();
                String url = node1.selectSingleNode("url").getStringValue();
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
                final URL newUrl = new URL(serverService.getConnParams().getServerUrl()+ url);
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
     * @throws CentralDispatcherServerRequestException if the format is incorrect, or the Envelope indicates an error response.
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
     * @throws CentralDispatcherException if the format is incorrect, or the Envelope indicates an error response.
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
                .getResponseMessage() != null ? response.getResponseMessage() : ""));
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
            if(buffer.length()>0){
                sb.append(buffer);
            }else{
                sb.append("Server reported an error");
                if(null!=response.getResponseMessage()){
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


    public DispatcherResult killDispatcherExecution(final String execId) throws CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("id", execId);
        params.put("xmlreq", "true");

        //2. send request via ServerService
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_KILL_JOB_PATH, params, null, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }

        final Envelope envelope = validateResponse(response);

        final boolean result = envelope.isSuccessResult();
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

    public Collection<IStoredJob> listStoredJobs(final IStoredJobsQuery iStoredJobsQuery,
                                                 final OutputStream output) throws CentralDispatcherException {
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("xmlreq", "true");
        final String nameMatch = iStoredJobsQuery.getNameMatch();
         String groupMatch = iStoredJobsQuery.getGroupMatch();
        final String projectFilter = iStoredJobsQuery.getProjectFilter();
        final String commandFilter = iStoredJobsQuery.getCommand();
        final String idlistFilter = iStoredJobsQuery.getIdlist();
        final String typeFilter = iStoredJobsQuery.getType();
        final String resourceFilter = iStoredJobsQuery.getResource();

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
            params.put("projFilter", projectFilter);
        }
        if (null != resourceFilter) {
            params.put("objFilter", resourceFilter);
        }
        if (null != typeFilter) {
            params.put("typeFilter", typeFilter);
        }
        if (null != commandFilter   ) {
            params.put("cmdFilter", commandFilter);
        }
        if (null != idlistFilter   ) {
            params.put("idlist", idlistFilter);
        }

        params.put("xmlreq", "true");

        //2. send request via ServerService
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_LIST_STORED_JOBS_PATH, params, null, null);
        } catch (MalformedURLException e) {
            throw new CentralDispatcherServerRequestException("Failed to make request", e);
        }

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
                list.add(StoredJobImpl.create(id, name, url, group, description));
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
    }

    public QueuedItemResult queueDispatcherJob(final IDispatchedJob dispatchedJob) throws CentralDispatcherException {
        final HashMap<String,String> params = new HashMap<String, String>();
        if(null==dispatchedJob.getJobRef()) {
            throw new IllegalArgumentException("JobRef was null");
        }
        if(null!=dispatchedJob.getJobRef().getJobId()) {
            params.put("id", dispatchedJob.getJobRef().getJobId());
        }else {
            if (null != dispatchedJob.getJobRef().getName()) {
                params.put("jobName", dispatchedJob.getJobRef().getName());
            }
            if (null != dispatchedJob.getJobRef().getGroup()) {
                params.put("groupPath", dispatchedJob.getJobRef().getGroup());
            }
        }

        addNodeSetParams(params, dispatchedJob.getNodeSet(), dispatchedJob.isKeepgoing(), "extra.");
        addLoglevelParams(params, dispatchedJob.getLoglevel(), "extra.");
        if(null!=dispatchedJob.getArgs() && dispatchedJob.getArgs().length>0) {
            params.put("extra.argString", CLIUtils.generateArgline(null, dispatchedJob.getArgs()));
        }
        /*
         * submit request to the URL path, expecting standard "queued item list" result 
         */
        return submitQueueRequest(null, params, RUNDECK_JOBS_RUN);
    }

    
    private void addLoglevelParams(Map<String,String> params, int loglevel, final String prefix) {

        String loglevelstr= RundeckAppConstants.MSG_INFO.toUpperCase();
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

        params.put(prefix + "loglevel", loglevelstr);
    }

    /**
     * Maps nodeset keys to the parameter suffix used in the service request
     */
    private static Map<String, String> nodeFilterParams = new HashMap<String, String>();
    static{
        nodeFilterParams.put(NodeSet.HOSTNAME,"");
        nodeFilterParams.put(NodeSet.TYPE,"Type");
        nodeFilterParams.put(NodeSet.TAGS,"Tags");
        nodeFilterParams.put(NodeSet.OS_NAME,"OsName");
        nodeFilterParams.put(NodeSet.OS_FAMILY,"OsFamily");
        nodeFilterParams.put(NodeSet.OS_ARCH,"OsArch");
        nodeFilterParams.put(NodeSet.OS_VERSION,"OsVersion");
        nodeFilterParams.put(NodeSet.NAME,"Name");
    }


    public static void addNodeSetParams(final HashMap<String, String> params, final NodeSet nodeSet,
                                        final Boolean isKeepgoing, final String prefix) {

        if (null == nodeSet) {
            return;
        }
        if (nodeSet.getThreadCount() > 0) {
            params.put(prefix + "nodeThreadcount", Integer.toString(nodeSet.getThreadCount()));
        }
        if(null!=isKeepgoing){
            params.put(prefix + "nodeKeepgoing", Boolean.toString(isKeepgoing));
        }
        if (nodeSet.isBlank()) {
            return;
        }
        params.put(prefix + "doNodedispatch", "true");
        boolean excludeprecedence = true;
        if (null != nodeSet.getExclude() && nodeSet.getExclude().isDominant()) {
            excludeprecedence = true;
        } else if (null != nodeSet.getInclude() && nodeSet.getInclude().isDominant()) {
            excludeprecedence = false;
        }
        params.put(prefix + "nodeExcludePrecedence", Boolean.toString(excludeprecedence));

        final NodeSet.Include include = nodeSet.getInclude();

        for (NodeSet.FILTER_ENUM filter : NodeSet.FILTER_ENUM.values()) {
            String value = null;
            if (null != include && !include.isBlank()) {
                value = filter.value(include);
            }

            String key = nodeFilterParams.get(filter.getName());
            if (null != value && !"".equals(value)) {
                params.put(prefix + "nodeInclude" + key, value);
            } else {
                params.put(prefix + "nodeInclude" + key, "");
            }
        }
        final NodeSet.Exclude exclude = nodeSet.getExclude();

        for (NodeSet.FILTER_ENUM filter : NodeSet.FILTER_ENUM.values()) {
            String value = null;
            if (null != exclude && !exclude.isBlank()) {
                value = filter.value(exclude);
            }
            String key = nodeFilterParams.get(filter.getName());
            if (null != value && !"".equals(value)) {
                params.put(prefix + "nodeExclude" + key, value);
            } else {
                params.put(prefix + "nodeExclude" + key, "");
            }
        }


    }

    public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest iLoadJobsRequest, File input) throws
        CentralDispatcherException {
        final HashMap params = new HashMap();
        params.put("dupeOption", iLoadJobsRequest.getDuplicateOption().toString());
        params.put("xmlreq", "true");

        /*
         * Send the request bean and the file as a multipart request.
         */

        //2. send request via ServerService
        final WebserviceResponse response;
        try {
            response = serverService.makeRundeckRequest(RUNDECK_JOBS_UPLOAD, params, input, null);
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
        ArrayList<IStoredJobLoadResult> resultList = new ArrayList<IStoredJobLoadResult>();
        if (succeeded > 0) {
            logger.debug("Succeeded creating/updating " + succeeded + " Jobs:");
            final List nodes = result.selectNodes("/result/succeeded/job");
            for (final Object node2 : nodes) {
                final Node node1 = (Node) node2;
                final IStoredJobLoadResult storedJobLoadResult = parseStoredJobResult(node1, true, false,
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
                final IStoredJobLoadResult storedJobLoadResult = parseStoredJobResult(node1, false, false,
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
                final IStoredJobLoadResult storedJobLoadResult = parseStoredJobResult(node1, true, true,
                    error);
                resultList.add(storedJobLoadResult);

            }
        }
        return resultList;
    }

    private IStoredJobLoadResult parseStoredJobResult(final Node node1, final boolean successful, final boolean skippedJob,
                                                      final String message) {
        final String index = node1.selectSingleNode("@index").getStringValue();
        final Node idNode = node1.selectSingleNode("id");
        final String id = null!= idNode? idNode.getStringValue():null;
        final String name = node1.selectSingleNode("name").getStringValue();
        final Node urlNode = node1.selectSingleNode("url");
        final String url = null != urlNode ? makeAbsoluteURL(urlNode.getStringValue()) : null;
        final String group = null != node1.selectSingleNode("group") ? node1.selectSingleNode("group")
            .getStringValue() : null;
        final String description = null != node1.selectSingleNode("description") ? node1.selectSingleNode("description")
            .getStringValue() : null;
        logger.debug("\t" + index + ": " + name + " [" + id + "] <" + url + ">");
        int ndx = -1;
        try {
            ndx = Integer.parseInt(index);
        } catch (NumberFormatException e) {

        }
        return StoredJobLoadResultImpl.createLoadResult(id, name, url,
            group, description, successful, skippedJob,
            message, ndx);
    }

    /**
     * Utility to serialize Document as a String for debugging
     *
     * @param document document
     *
     * @return xml string
     *
     * @throws IOException if error occurs
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

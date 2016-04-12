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
* JschNodeExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:46 PM
* 
*/
package com.dtolabs.rundeck.core.execution.impl.jsch;

import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.impl.common.AntSupport;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.utils.BasicSource;
import com.dtolabs.rundeck.core.execution.utils.LeadPipeOutputStream;
import com.dtolabs.rundeck.core.execution.utils.PasswordSource;
import com.dtolabs.rundeck.core.execution.utils.ResponderTask;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.tasks.net.ExtSSHExec;
import com.dtolabs.rundeck.core.tasks.net.SSHTaskBuilder;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;
import com.jcraft.jsch.JSchException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.*;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;


/**
 * JschNodeExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class JschNodeExecutor implements NodeExecutor, Describable {
    public static final Logger logger = Logger.getLogger(JschNodeExecutor.class.getName());
    public static final String SERVICE_PROVIDER_TYPE = "jsch-ssh";
    public static final String FWK_PROP_AUTH_CANCEL_MSG = "framework.messages.error.ssh.authcancel";
    public static final String FWK_PROP_AUTH_CANCEL_MSG_DEFAULT =
        "Authentication failure connecting to node: \"{0}\". Make sure your resource definitions and credentials are up to date.";
    public static final String FWK_PROP_AUTH_FAIL_MSG = "framework.messages.error.ssh.authfail";
    public static final String FWK_PROP_AUTH_FAIL_MSG_DEFAULT =
        "Authentication failure connecting to node: \"{0}\". Password incorrect.";
    public static final String NODE_ATTR_SSH_KEYPATH = "ssh-keypath";
    public static final String NODE_ATTR_SSH_KEY_RESOURCE = "ssh-key-storage-path";
    public static final String NODE_ATTR_SSH_PASSWORD_STORAGE_PATH= "ssh-password-storage-path";
    public static final String NODE_ATTR_LOCAL_SSH_AGENT = "local-ssh-agent";
    public static final String NODE_ATTR_LOCAL_TTL_SSH_AGENT = "local-ttl-ssh-agent";

    public static final String PROJ_PROP_PREFIX = "project.";
    public static final String FWK_PROP_PREFIX = "framework.";

    public static final String FWK_PROP_SSH_KEYPATH = FWK_PROP_PREFIX + NODE_ATTR_SSH_KEYPATH;
    public static final String PROJ_PROP_SSH_KEYPATH = PROJ_PROP_PREFIX + NODE_ATTR_SSH_KEYPATH;
    public static final String FWK_PROP_SSH_KEY_RESOURCE = FWK_PROP_PREFIX + NODE_ATTR_SSH_KEY_RESOURCE;
    public static final String FWK_PROP_SSH_PASSWORD_STORAGE_PATH= FWK_PROP_PREFIX + NODE_ATTR_SSH_PASSWORD_STORAGE_PATH;
    public static final String PROJ_PROP_SSH_KEY_RESOURCE = PROJ_PROP_PREFIX + NODE_ATTR_SSH_KEY_RESOURCE;
    public static final String PROJ_PROP_SSH_PASSWORD_STORAGE_PATH = PROJ_PROP_PREFIX + NODE_ATTR_SSH_PASSWORD_STORAGE_PATH;

    public static final String NODE_ATTR_SSH_AUTHENTICATION = "ssh-authentication";
    public static final String NODE_ATTR_SSH_PASSWORD_OPTION = "ssh-password-option";
    public static final String DEFAULT_SSH_PASSWORD_OPTION = "option.sshPassword";
    public static final String SUDO_OPT_PREFIX = "sudo-";
    public static final String SUDO2_OPT_PREFIX = "sudo2-";
    public static final String NODE_ATTR_SUDO_PASSWORD_OPTION = "password-option";
    public static final String DEFAULT_SUDO_PASSWORD_OPTION = "option.sudoPassword";
    public static final String DEFAULT_SUDO2_PASSWORD_OPTION = "option.sudo2Password";
    public static final String NODE_ATTR_SSH_KEY_PASSPHRASE_OPTION = "ssh-key-passphrase-option";
    public static final String NODE_ATTR_SSH_KEY_PASSPHRASE_STORAGE_PATH = "ssh-key-passphrase-storage-path";
    public static final String DEFAULT_SSH_KEY_PASSPHRASE_OPTION = "option.sshKeyPassphrase";


    public static final String FWK_PROP_SSH_AUTHENTICATION = FWK_PROP_PREFIX + NODE_ATTR_SSH_AUTHENTICATION;
    public static final String PROJ_PROP_SSH_AUTHENTICATION = PROJ_PROP_PREFIX + NODE_ATTR_SSH_AUTHENTICATION;

    public static final String NODE_ATTR_SUDO_COMMAND_ENABLED = "command-enabled";
    public static final String NODE_ATTR_SUDO_PROMPT_PATTERN = "prompt-pattern";
    public static final String DEFAULT_SUDO_PROMPT_PATTERN = "^\\[sudo\\] password for .+: .*";
    public static final String NODE_ATTR_SUDO_FAILURE_PATTERN = "failure-pattern";
    public static final String DEFAULT_SUDO_FAILURE_PATTERN = "^.*try again.*";
    public static final String NODE_ATTR_SUDO_COMMAND_PATTERN = "command-pattern";
    public static final String DEFAULT_SUDO_COMMAND_PATTERN = "^sudo$";
    public static final String DEFAULT_SUDO2_COMMAND_PATTERN = "^sudo .+? sudo .*$";
    public static final String NODE_ATTR_SUDO_PROMPT_MAX_LINES = "prompt-max-lines";
    public static final int DEFAULT_SUDO_PROMPT_MAX_LINES = 12;
    public static final String NODE_ATTR_SUDO_RESPONSE_MAX_LINES = "response-max-lines";
    public static final int DEFAULT_SUDO_RESPONSE_MAX_LINES = 2;
    public static final String NODE_ATTR_SUDO_PROMPT_MAX_TIMEOUT = "prompt-max-timeout";
    public static final long DEFAULT_SUDO_PROMPT_MAX_TIMEOUT = 5000;
    public static final String NODE_ATTR_SUDO_RESPONSE_MAX_TIMEOUT = "response-max-timeout";
    public static final long DEFAULT_SUDO_RESPONSE_MAX_TIMEOUT = 5000;
    public static final String NODE_ATTR_SUDO_FAIL_ON_PROMPT_MAX_LINES = "fail-on-prompt-max-lines";
    public static final boolean DEFAULT_SUDO_FAIL_ON_PROMPT_MAX_LINES = false;
    public static final String NODE_ATTR_SUDO_FAIL_ON_PROMPT_TIMEOUT = "fail-on-prompt-timeout";
    public static final boolean DEFAULT_SUDO_FAIL_ON_PROMPT_TIMEOUT = true;
    public static final String NODE_ATTR_SUDO_FAIL_ON_RESPONSE_TIMEOUT = "fail-on-response-timeout";
    public static final boolean DEFAULT_SUDO_FAIL_ON_RESPONSE_TIMEOUT = false;
    public static final String NODE_ATTR_SUDO_SUCCESS_ON_PROMPT_THRESHOLD = "success-on-prompt-threshold";
    public static final String NODE_ATTR_SUDO_PASSWORD_STORAGE_PATH= "password-storage-path";
    public static final boolean DEFAULT_SUDO_SUCCESS_ON_PROMPT_THRESHOLD = true;
    public static final String PROJECT_SSH_USER = PROJ_PROP_PREFIX + "ssh.user";

    public static final String SSH_CONFIG_PREFIX = "ssh-config-";
    public static final String FWK_SSH_CONFIG_PREFIX = FWK_PROP_PREFIX + SSH_CONFIG_PREFIX;
    public static final String PROJ_SSH_CONFIG_PREFIX = PROJ_PROP_PREFIX + SSH_CONFIG_PREFIX;
    private Framework framework;

    public JschNodeExecutor(final Framework framework) {
        this.framework = framework;
    }

    public static final String CONFIG_KEYPATH = "keypath";
    public static final String CONFIG_KEYSTORE_PATH = "keystoragepath";
    public static final String CONFIG_PASSSTORE_PATH = "passwordstoragepath";
    public static final String CONFIG_SUDO_PASSSTORE_PATH = "sudopasswordstoragepath";
    public static final String CONFIG_AUTHENTICATION = "authentication";

    static final Description DESC ;

    static final Property SSH_KEY_FILE_PROP = PropertyUtil.string(CONFIG_KEYPATH, "SSH Key File path",
            "File Path to the SSH Key to use",
            false, null);

    static final Property SSH_KEY_STORAGE_PROP = PropertyBuilder.builder()
            .string(CONFIG_KEYSTORE_PATH)
            .required(false)
            .title("SSH Key Storage Path")
            .description("Path to the SSH Key to use within Rundeck Storage. E.g. \"keys/path/key1.pem\"")
            .renderingOption(StringRenderingConstants.SELECTION_ACCESSOR_KEY,
                    StringRenderingConstants.SelectionAccessor.STORAGE_PATH)
            .renderingOption(StringRenderingConstants.STORAGE_PATH_ROOT_KEY, "keys")
            .renderingOption(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-key-type=private")
            .build();
    static final Property SSH_PASSWORD_STORAGE_PROP = PropertyBuilder.builder()
            .string(CONFIG_PASSSTORE_PATH)
            .required(false)
            .title("SSH Password Storage Path")
            .description("Path to the Password to use within Rundeck Storage. E.g. \"keys/path/my.password\". Can be overridden by a Node attribute named 'ssh-password-storage-path'.")
            .renderingOption(StringRenderingConstants.SELECTION_ACCESSOR_KEY,
                    StringRenderingConstants.SelectionAccessor.STORAGE_PATH)
            .renderingOption(StringRenderingConstants.STORAGE_PATH_ROOT_KEY, "keys")
            .renderingOption(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-data-type=password")
            .build();

    public static final Property SSH_AUTH_TYPE_PROP = PropertyUtil.select(CONFIG_AUTHENTICATION, "SSH Authentication",
            "Type of SSH Authentication to use",
            true, SSHTaskBuilder.AuthenticationType.privateKey.toString(), Arrays.asList(SSHTaskBuilder
            .AuthenticationType.values()), null, null);

    static {
        DescriptionBuilder builder = DescriptionBuilder.builder();
        builder.name(SERVICE_PROVIDER_TYPE)
                .title("SSH")
                .description("Executes a command on a remote node via SSH.")
                ;

        builder.property(SSH_KEY_FILE_PROP);
        builder.property(SSH_KEY_STORAGE_PROP);
        builder.property(SSH_PASSWORD_STORAGE_PROP);
        builder.property(SSH_AUTH_TYPE_PROP);

        builder.mapping(CONFIG_KEYPATH, PROJ_PROP_SSH_KEYPATH);
        builder.frameworkMapping(CONFIG_KEYPATH, FWK_PROP_SSH_KEYPATH);
        builder.mapping(CONFIG_KEYSTORE_PATH, PROJ_PROP_SSH_KEY_RESOURCE);
        builder.frameworkMapping(CONFIG_KEYSTORE_PATH, FWK_PROP_SSH_KEY_RESOURCE);
        builder.mapping(CONFIG_PASSSTORE_PATH, PROJ_PROP_SSH_PASSWORD_STORAGE_PATH);
        builder.frameworkMapping(CONFIG_PASSSTORE_PATH, FWK_PROP_SSH_PASSWORD_STORAGE_PATH);
        builder.mapping(CONFIG_AUTHENTICATION, PROJ_PROP_SSH_AUTHENTICATION);
        builder.frameworkMapping(CONFIG_AUTHENTICATION, FWK_PROP_SSH_AUTHENTICATION);

        DESC=builder.build();
    }


    public Description getDescription() {
        return DESC;
    }

    public NodeExecutorResult executeCommand(final ExecutionContext context, final String[] command,
                                             final INodeEntry node)  {
        if (null == node.getHostname() || null == node.extractHostname() || StringUtils.isBlank(node.extractHostname())) {
            return NodeExecutorResultImpl.createFailure(
                StepFailureReason.ConfigurationFailure,
                "Hostname must be set to connect to remote node '" + node.getNodename() + "'",
                node
            );
        }

        final ExecutionListener listener = context.getExecutionListener();
        final Project project = new Project();
        AntSupport.addAntBuildListener(listener, project);

        boolean success = false;
        final ExtSSHExec sshexec;
        //perform jsch sssh command
        final NodeSSHConnectionInfo nodeAuthentication = new NodeSSHConnectionInfo(node, framework,
                                                                                   context);
        final int timeout = nodeAuthentication.getSSHTimeout();
        try {

            sshexec = SSHTaskBuilder.build(node, command, project, context.getDataContext(),
                                           nodeAuthentication, context.getLoglevel(),listener);
        } catch (SSHTaskBuilder.BuilderException e) {
            return NodeExecutorResultImpl.createFailure(StepFailureReason.ConfigurationFailure,
                                                        e.getMessage(), node);
        }

        //Sudo support

        final ExecutorService executor = Executors.newSingleThreadExecutor(
                new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        return new Thread(null, r, "SudoResponder " + node.getNodename() + ": "
                                + System.currentTimeMillis());
                    }
                }
        );

        final Future<ResponderTask.ResponderResult> responderFuture;

        final SudoResponder sudoResponder;
        try {
            sudoResponder = SudoResponder.create(
                    node,
                    framework,
                    context,
                    SUDO_OPT_PREFIX,
                    passwordSourceWithPrefix(nodeAuthentication, SUDO_OPT_PREFIX),
                    DEFAULT_SUDO_COMMAND_PATTERN
            );
        } catch (IOException e) {
            return NodeExecutorResultImpl.createFailure(StepFailureReason.ConfigurationFailure,
                                                        e.getMessage(), node);
        }
        Runnable responderCleanup=null;
        if (sudoResponder.isSudoEnabled() && sudoResponder.matchesCommandPattern(command[0])) {
            final DisconnectResultHandler resultHandler = new DisconnectResultHandler();

            //configure two piped i/o stream pairs, to connect to the input/output of the SSH connection
            final PipedInputStream responderInput = new PipedInputStream();
            final PipedOutputStream responderOutput = new PipedOutputStream();
            final PipedInputStream jschInput = new PipedInputStream();
            //lead pipe allows connected inputstream to close and not hang the writer to this stream
            final PipedOutputStream jschOutput = new LeadPipeOutputStream();
            try {
                responderInput.connect(jschOutput);
                jschInput.connect(responderOutput);
            } catch (IOException e) {
                return NodeExecutorResultImpl.createFailure(StepFailureReason.IOFailure, e.getMessage(), node);
            }

            //first sudo prompt responder
            ResponderTask responder = new ResponderTask(sudoResponder, responderInput, responderOutput, resultHandler);

            /**
             * Callable will be executed by the ExecutorService
             */
            final Callable<ResponderTask.ResponderResult> responderResultCallable;


            //if 2nd responder

            final SudoResponder sudoResponder2;
            try {
                sudoResponder2 = SudoResponder.create(
                        node,
                        framework,
                        context,
                        SUDO2_OPT_PREFIX,
                        passwordSourceWithPrefix(nodeAuthentication, SUDO2_OPT_PREFIX),
                        DEFAULT_SUDO2_COMMAND_PATTERN
                );
            } catch (IOException e) {
                return NodeExecutorResultImpl.createFailure(StepFailureReason.ConfigurationFailure,
                                                            e.getMessage(), node);
            }
            if (sudoResponder2.isSudoEnabled()
                && sudoResponder2.matchesCommandPattern(CLIUtils.generateArgline(null, command, false))) {
                logger.debug("Enable second sudo responder");

                sudoResponder2.setDescription("Second " + SudoResponder.DEFAULT_DESCRIPTION);
                sudoResponder.setDescription("First " + SudoResponder.DEFAULT_DESCRIPTION);

                //sequence of the first then the second sudo responder
                responderResultCallable = responder.createSequence(sudoResponder2);
            } else {
                responderResultCallable = responder;
            }


            //set up SSH execution
            sshexec.setAllocatePty(true);
            sshexec.setInputStream(jschInput);
            sshexec.setSecondaryStream(jschOutput);
            sshexec.setDisconnectHolder(resultHandler);


            responderFuture = executor.submit(responderResultCallable);
            //close streams after responder is finished
            responderCleanup = new Runnable() {
                public void run() {
                    logger.debug("SudoResponder shutting down...");
                    try {
                        responderInput.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        responderOutput.flush();
                        responderOutput.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //executor pool shutdown
                    executor.shutdownNow();
                }
            };
            executor.submit(responderCleanup);
        }else {
            responderFuture = null;
        }
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().log(3,
                                               "Starting SSH Connection: " + nodeAuthentication.getUsername() + "@"
                                               + node.getHostname() + " ("
                                               + node.getNodename() + ")");
        }
        String errormsg = null;
        FailureReason failureReason=null;
        try {
            sshexec.execute();
            success = true;
        } catch (BuildException e) {
            final ExtractFailure extractJschFailure = extractFailure(e,node, timeout, framework);
            errormsg = extractJschFailure.getErrormsg();
            failureReason = extractJschFailure.getReason();
            context.getExecutionListener().log(0, errormsg);
        }
        if (null != responderCleanup) {
            responderCleanup.run();
        }
        shutdownAndAwaitTermination(executor);
        if (null != responderFuture) {
            try {
                logger.debug("Waiting 5 seconds for responder future result");
                final ResponderTask.ResponderResult result = responderFuture.get(5, TimeUnit.SECONDS);
                logger.debug("Responder result: " + result);
                if (!result.isSuccess() && !result.isInterrupted()) {
                    context.getExecutionListener().log(0,
                                                       result.getResponder().toString() + " failed: "
                                                       + result.getFailureReason());
                }
            } catch (InterruptedException e) {
                //ignore
            } catch (java.util.concurrent.ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                //ignore
            }
        }
        final int resultCode = sshexec.getExitStatus();

        if (success) {
            return NodeExecutorResultImpl.createSuccess(node);
        } else {
            return NodeExecutorResultImpl.createFailure(failureReason, errormsg, node, resultCode);
        }
    }

    /**
     * create password source
     * @param nodeAuthentication auth
     * @param prefix prefix
     * @return source
     * @throws IOException if password loading from storage fails
     */
    private PasswordSource passwordSourceWithPrefix(
            final NodeSSHConnectionInfo nodeAuthentication,
            final String prefix
    ) throws IOException
    {
        if (null != nodeAuthentication.getSudoPasswordStoragePath(prefix)) {
            return new BasicSource(nodeAuthentication.getSudoPasswordStorageData(prefix));
        } else {
            return new BasicSource(nodeAuthentication.getSudoPassword(prefix));
        }
    }

    static enum JschFailureReason implements FailureReason {
        /**
         * A problem with the SSH connection
         */
        SSHProtocolFailure
    }

    static ExtractFailure extractFailure(BuildException e, INodeEntry node, int timeout, Framework framework) {
        String errormsg;
        FailureReason failureReason;

        if (e.getMessage().contains("Timeout period exceeded, connection dropped")) {
            errormsg =
                "Failed execution for node: " + node.getNodename() + ": Execution Timeout period exceeded (after "
                + timeout + "ms), connection dropped";
            failureReason = NodeStepFailureReason.ConnectionTimeout;
        } else if (null != e.getCause() && e.getCause() instanceof JSchException) {
            JSchException jSchException = (JSchException) e.getCause();
            return extractJschFailure(node, timeout, jSchException, framework);
        } else if (e.getMessage().contains("Remote command failed with exit status")) {
            errormsg = e.getMessage();
            failureReason = NodeStepFailureReason.NonZeroResultCode;
        } else {
            failureReason = StepFailureReason.Unknown;
            errormsg = e.getMessage();
        }
        return new ExtractFailure(errormsg, failureReason);
    }

    static ExtractFailure extractJschFailure(final INodeEntry node,
                                             final int timeout,
                                             final JSchException jSchException, final Framework framework) {
        String errormsg;
        FailureReason reason;

        if (null == jSchException.getCause()) {
            if (jSchException.getMessage().contains("Auth cancel")) {

                String msgformat = FWK_PROP_AUTH_CANCEL_MSG_DEFAULT;
                if (framework.getPropertyLookup().hasProperty(FWK_PROP_AUTH_CANCEL_MSG)) {
                    msgformat = framework.getProperty(FWK_PROP_AUTH_CANCEL_MSG);
                }
                errormsg = MessageFormat.format(msgformat, node.getNodename(), jSchException.getMessage());
                reason = NodeStepFailureReason.AuthenticationFailure;
            } else if (jSchException.getMessage().contains("Auth fail")) {
                String msgformat = FWK_PROP_AUTH_FAIL_MSG_DEFAULT;
                if (framework.getPropertyLookup().hasProperty(FWK_PROP_AUTH_FAIL_MSG)) {
                    msgformat = framework.getProperty(FWK_PROP_AUTH_FAIL_MSG);
                }
                errormsg = MessageFormat.format(msgformat, node.getNodename(), jSchException.getMessage());
                reason = NodeStepFailureReason.AuthenticationFailure;
            } else {
                reason = JschFailureReason.SSHProtocolFailure;
                errormsg = jSchException.getMessage();
            }
        } else {
            Throwable cause = ExceptionUtils.getRootCause(jSchException);
            errormsg = cause.getMessage();
            if (cause instanceof NoRouteToHostException) {
                reason = NodeStepFailureReason.ConnectionFailure;
            } else if (cause instanceof UnknownHostException) {
                reason = NodeStepFailureReason.HostNotFound;
            } else if (cause instanceof SocketTimeoutException) {
                errormsg = "Connection Timeout (after " + timeout + "ms): " + cause.getMessage();
                reason = NodeStepFailureReason.ConnectionTimeout;
            } else if (cause instanceof SocketException) {
                reason = NodeStepFailureReason.ConnectionFailure;
            } else {
                reason = StepFailureReason.Unknown;
            }
        }
        return new ExtractFailure(errormsg, reason);
    }

    /**
     * Shutdown the ExecutorService
     */
    void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdownNow(); // Disable new tasks from being submitted
        try {
            logger.debug("Waiting up to 30 seconds for ExecutorService to shut down");
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.debug("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Disconnects the SSH connection if the result was not successful.
     */
    private static class DisconnectResultHandler implements ResponderTask.ResultHandler,
                                                            ExtSSHExec.DisconnectHolder {
        private volatile ExtSSHExec.Disconnectable disconnectable;
        private volatile boolean needsDisconnect=false;

        public void setDisconnectable(final ExtSSHExec.Disconnectable disconnectable) {
            this.disconnectable = disconnectable;
            checkAndDisconnect();
        }

        public void handleResult(final boolean success, final String reason) {
            needsDisconnect=!success;
            checkAndDisconnect();
        }

        /**
         * synchronize to prevent race condition on setDisconnectable and handleResult
         */
        private synchronized void checkAndDisconnect() {
            if (null != disconnectable && needsDisconnect) {
                disconnectable.disconnect();
                needsDisconnect = false;
            }
        }
    }


    static class ExtractFailure {

        private String errormsg;
        private FailureReason reason;

        private ExtractFailure(String errormsg, FailureReason reason) {
            this.errormsg = errormsg;
            this.reason = reason;
        }

        public String getErrormsg() {
            return errormsg;
        }

        public FailureReason getReason() {
            return reason;
        }


    }
}

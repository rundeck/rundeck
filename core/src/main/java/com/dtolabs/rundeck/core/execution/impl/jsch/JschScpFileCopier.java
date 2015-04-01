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
* JschScpFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:47 PM
* 
*/
package com.dtolabs.rundeck.core.execution.impl.jsch;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.service.DestinationFileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.tasks.net.SSHTaskBuilder;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Echo;

import java.io.File;
import java.io.InputStream;


/**
 * JschScpFileCopier is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class JschScpFileCopier extends BaseFileCopier implements FileCopier, Describable, DestinationFileCopier {
    public static final String SERVICE_PROVIDER_TYPE = "jsch-scp";


    static final Description DESC = DescriptionBuilder.builder()
        .name(SERVICE_PROVIDER_TYPE)
        .title("SCP")
        .description("Copies a script file to a remote node via SCP.")
        .property(JschNodeExecutor.SSH_KEY_FILE_PROP)
        .property(JschNodeExecutor.SSH_KEY_STORAGE_PROP)
        .property(JschNodeExecutor.SSH_PASSWORD_STORAGE_PROP)
        .property(JschNodeExecutor.SSH_AUTH_TYPE_PROP)
        .mapping(JschNodeExecutor.CONFIG_KEYPATH, JschNodeExecutor.PROJ_PROP_SSH_KEYPATH)
        .mapping(JschNodeExecutor.CONFIG_AUTHENTICATION, JschNodeExecutor.PROJ_PROP_SSH_AUTHENTICATION)
        .mapping(JschNodeExecutor.CONFIG_KEYSTORE_PATH, JschNodeExecutor.PROJ_PROP_SSH_KEY_RESOURCE)
        .mapping(JschNodeExecutor.CONFIG_PASSSTORE_PATH, JschNodeExecutor.PROJ_PROP_SSH_PASSWORD_STORAGE_PATH)
        .frameworkMapping(JschNodeExecutor.CONFIG_KEYSTORE_PATH, JschNodeExecutor.FWK_PROP_SSH_KEY_RESOURCE)
        .frameworkMapping(JschNodeExecutor.CONFIG_PASSSTORE_PATH, JschNodeExecutor.FWK_PROP_SSH_PASSWORD_STORAGE_PATH)
        .frameworkMapping(JschNodeExecutor.CONFIG_KEYPATH, JschNodeExecutor.FWK_PROP_SSH_KEYPATH)
        .frameworkMapping(JschNodeExecutor.CONFIG_AUTHENTICATION, JschNodeExecutor.FWK_PROP_SSH_AUTHENTICATION)
        .build();


    public Description getDescription() {
        return DESC;
    }

    private Framework framework;

    public JschScpFileCopier(Framework framework) {
        this.framework = framework;
    }

    public String copyFileStream(final ExecutionContext context, InputStream input, INodeEntry node) throws
                                                                                                     FileCopierException {

        return copyFile(context, null, input, null, node);
    }

    public String copyFile(final ExecutionContext context, File scriptfile, INodeEntry node) throws
                                                                                             FileCopierException {
        return copyFile(context, scriptfile, null, null, node);
    }

    public String copyScriptContent(ExecutionContext context, String script, INodeEntry node) throws
                                                                                              FileCopierException {

        return copyFile(context, null, null, script, node);
    }


    private String copyFile(final ExecutionContext context, final File scriptfile, final InputStream input,
                            final String script, final INodeEntry node) throws FileCopierException {
        return copyFile(context, scriptfile, input, script, node, null);

    }

    private String copyFile(
            final ExecutionContext context,
            final File scriptfile,
            final InputStream input,
            final String script,
            final INodeEntry node,
            final String destinationPath
    ) throws FileCopierException {

        Project project = new Project();

        final String remotefile;
        if(null==destinationPath) {
            String identity = null!=context.getDataContext() && null!=context.getDataContext().get("job")?
                              context.getDataContext().get("job").get("execid"):null;
            remotefile = generateRemoteFilepathForNode(
                    node,
                    context.getFramework().getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()),
                    context.getFramework(),
                    (null != scriptfile ? scriptfile.getName() : "dispatch-script"),
                    null,
                    identity
            );
        }else {
            remotefile = destinationPath;
        }
        //write to a local temp file or use the input file
        final File localTempfile =
                null != scriptfile ?
                        scriptfile :
                        writeTempFile(
                                context,
                                scriptfile,
                                input,
                                script
                        );

//        logger.debug("temp file for node " + node.getNodename() + ": " + temp.getAbsolutePath() + ",
// datacontext: " + dataContext);
        final Task scp;
        final NodeSSHConnectionInfo nodeAuthentication = new NodeSSHConnectionInfo(
                node,
                framework,
                context);
        try {

            scp = SSHTaskBuilder.buildScp(node, project, remotefile, localTempfile, nodeAuthentication,
                    context.getLoglevel(),context.getExecutionListener());
        } catch (SSHTaskBuilder.BuilderException e) {
            throw new FileCopierException("Configuration error: " + e.getMessage(),
                    StepFailureReason.ConfigurationFailure, e);
        }

        /**
         * Copy the file over
         */
        context.getExecutionListener().log(3,"copying file: '" + localTempfile.getAbsolutePath()
                + "' to: '" + node.getNodename() + ":" + remotefile + "'");

        String errormsg = null;
        try {
            scp.execute();
        } catch (BuildException e) {
            JschNodeExecutor.ExtractFailure failure = JschNodeExecutor.extractFailure(e,
                    node,
                    nodeAuthentication.getSSHTimeout(),
                    framework);
            errormsg = failure.getErrormsg();
            FailureReason failureReason = failure.getReason();
            context.getExecutionListener().log(0, errormsg);
            throw new FileCopierException("[jsch-scp] Failed copying the file: " + errormsg, failureReason, e);
        }finally {
            if(null == scriptfile) {
                if (!ScriptfileUtils.releaseTempFile(localTempfile)) {
                    context.getExecutionListener().log(
                            Constants.WARN_LEVEL,
                            "Unable to remove local temp file: " + localTempfile.getAbsolutePath()
                    );
                }
            }
        }
        return remotefile;
    }

    private Echo createEcho(final String message, final Project project, final String logLevel) {
        final Echo echo = new Echo();
        echo.setProject(project);
        final Echo.EchoLevel level = new Echo.EchoLevel();
        level.setValue(logLevel);
        echo.setLevel(level);
        echo.setMessage(message);
        return echo;
    }

    private Echo createEchoVerbose(final String message, final Project project) {
        return createEcho(message, project, "debug");
    }

    public String copyFileStream(ExecutionContext context, InputStream input, INodeEntry node,
            String destination) throws FileCopierException {
        return copyFile(context, null, input, null, node, destination);
    }

    public String copyFile(ExecutionContext context, File file, INodeEntry node,
            String destination) throws FileCopierException {
        return copyFile(context, file, null, null, node, destination);
    }

    public String copyScriptContent(ExecutionContext context, String script, INodeEntry node,
            String destination) throws FileCopierException {
        return copyFile(context, null, null, script, node, destination);
    }
}

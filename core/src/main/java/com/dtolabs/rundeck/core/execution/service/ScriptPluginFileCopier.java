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
* ScriptPluginFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/5/11 10:11 AM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.plugins.BaseScriptPlugin;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


/**
 * ScriptPluginFileCopier wraps the execution of the script and supplies the FileCopier interface.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptPluginFileCopier extends BaseScriptPlugin implements FileCopier {
    @Override
    public boolean isAllowCustomProperties() {
        return false;
    }

    ScriptPluginFileCopier(final ScriptPluginProvider provider, final Framework framework) {
        super(provider, framework);
    }

    static void validateScriptPlugin(final ScriptPluginProvider plugin) throws PluginException {
        if (null == plugin.getScriptArgs()) {
            throw new PluginException(
                "no script-args defined for provider: " + plugin);
        }
    }

    /**
     * Copy inputstream
     */
    public String copyFileStream(final ExecutionContext executionContext, final InputStream inputStream,
                                 final INodeEntry node) throws FileCopierException {

        return copyFile(executionContext, null, inputStream, null, node);
    }

    /**
     * Copy existing file
     */
    public String copyFile(final ExecutionContext executionContext, final File file, final INodeEntry node) throws
                                                                                                            FileCopierException {
        return copyFile(executionContext, file, null, null, node);
    }

    /**
     * Copy string content
     */
    public String copyScriptContent(final ExecutionContext executionContext, final String s,
                                    final INodeEntry node) throws
                                                           FileCopierException {
        return copyFile(executionContext, null, null, s, node);
    }

    static enum ScriptPluginFailureReason implements FailureReason {
        ScriptPluginFileCopierExpectedOutputMissing
    }

    /**
     * Internal copy method accepting file, inputstream or string
     */
    String copyFile(final ExecutionContext executionContext, final File file, final InputStream input,
                    final String content, final INodeEntry node) throws
                                                                 FileCopierException {
        final String pluginname = getProvider().getName();
        final Map<String, Map<String, String>> localDataContext = createScriptDataContext(
            executionContext.getFramework(),
            executionContext.getFrameworkProject(),
            executionContext.getDataContext());

        //add node context data
        localDataContext.put("node", DataContextUtils.nodeData(node));

        //write the temp file and replace tokens in the script with values from the dataContext
        final File tempfile = BaseFileCopier.writeScriptTempFile(executionContext, file, input, content, node);


        //add some more data context values to allow templatized script-copy attribute
        final HashMap<String, String> scptexec = new HashMap<String, String>();
        //set up the data context to include the local temp file
        scptexec.put("file", tempfile.getAbsolutePath());
        localDataContext.put("file-copy", scptexec);

        final String[] finalargs = createScriptArgs(localDataContext);
        executionContext.getExecutionListener().log(3, "[" + getProvider().getName() + "] executing: " + Arrays.asList(
            finalargs));

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int result = -1;
        try {
            result = ScriptExecUtil.runLocalCommand(finalargs,
                                                    DataContextUtils.generateEnvVarsFromContext(localDataContext),
                                                    null,
                                                    byteArrayOutputStream,
                                                    System.err
            );
        } catch (IOException e) {
            executionContext.getExecutionListener().log(0, e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            executionContext.getExecutionListener().log(0, e.getMessage());
            e.printStackTrace();
        }
        final boolean success = result == 0;
        executionContext.getExecutionListener().log(3,
                                                    "[" + pluginname + "]: result code: " + result + ", success: "
                                                    + success);

        if (!success) {
            throw new FileCopierException("[" + pluginname + "]: external script failed with exit code: " + result,
                                          NodeExecutorResult.Reason.NonZeroResultCode);
        }

        //load string of output from outputstream
        final String output = byteArrayOutputStream.toString();
        if (null == output || output.length() < 1) {
            throw new FileCopierException("[" + pluginname + "]: No output from external script",
                                          ScriptPluginFailureReason.ScriptPluginFileCopierExpectedOutputMissing
            );
        }
        //TODO: require any specific format for the data?
        //look for first line of output
        final String[] split1 = output.split("(\\r?\\n)");
        if (split1.length < 1) {
            throw new FileCopierException("[" + pluginname + "]: No output from external script",
                                          ScriptPluginFailureReason.ScriptPluginFileCopierExpectedOutputMissing);
        }
        final String remotefilepath = split1[0];

        executionContext.getExecutionListener().log(3, "[" + pluginname + "]: result filepath: " + remotefilepath);

        return remotefilepath;
    }
}

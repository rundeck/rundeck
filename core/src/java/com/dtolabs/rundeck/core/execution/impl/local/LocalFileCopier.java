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
* LocalFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 5:43 PM
* 
*/
package com.dtolabs.rundeck.core.execution.impl.local;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;

import java.io.File;
import java.io.InputStream;

/**
 * LocalFileCopier is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class LocalFileCopier extends BaseFileCopier implements FileCopier {
    public static final String SERVICE_PROVIDER_TYPE = "local";

    public LocalFileCopier(Framework framework) {
        this.framework = framework;
    }

    public String copyScriptContent(ExecutionContext context, String script, INodeEntry node) throws
        FileCopierException {

        return copyFile(context, null, null, script, node);
    }

    private Framework framework;

    public String copyFileStream(final ExecutionContext context, InputStream input, INodeEntry node) throws
        FileCopierException {

        return copyFile(context, null, input, null, node);
    }

    public String copyFile(final ExecutionContext context, File scriptfile, INodeEntry node) throws
        FileCopierException {
        return copyFile(context, scriptfile, null, null, node);
    }

    private String copyFile(final ExecutionContext context, File scriptfile, InputStream input, String script,
                            INodeEntry node) throws FileCopierException {
        //write the temp file and replace tokens in the script with values from the dataContext
        final File localTempfile = writeScriptTempFile(context, scriptfile, input, script, node);
        return localTempfile.getAbsolutePath();
    }
}

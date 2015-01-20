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
* RemoteFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:47 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;

import java.io.File;
import java.io.InputStream;

/**
 * FileCopier copies a file or its contents to a local or remote node.  The destination on the node is not
 * predetermined, but some utility methods of {@link com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier} can
 * be used to generate a destination file path.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface FileCopier {
    /**
     * Copy the contents of an input stream to the node
     *
     * @param context context
     * @param input   the input stream
     * @param node node
     *
     * @return File path of the file after copying to the node
     *
     * @throws FileCopierException if an error occurs
     */
    public String copyFileStream(final ExecutionContext context, InputStream input, INodeEntry node) throws
        FileCopierException;

    /**
     * Copy the contents of an input stream to the node
     *
     * @param context context
     * @param file    local file tocopy
     * @param node node
     *
     * @return File path of the file after copying to the node
     *
     * @throws FileCopierException if an error occurs
     */
    public String copyFile(final ExecutionContext context, File file, INodeEntry node) throws FileCopierException;

    /**
     * Copy the contents of an input stream to the node
     *
     * @param context context
     * @param script  file content string
     * @param node node
     *
     * @return File path of the file after copying to the node
     *
     * @throws FileCopierException if an error occurs
     */
    public String copyScriptContent(final ExecutionContext context, String script, INodeEntry node) throws
        FileCopierException;
}

/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An optional extension of {@link FileCopier} that provides a way to copy multiple files at once,
 * the method {@link #copyFiles(ExecutionContext, File, List, String, INodeEntry)} can be overridden
 * to perform a more efficient multi file copy.  Otherwise the
 * {@link FileCopier#copyFile(ExecutionContext, File, INodeEntry)}
 * is repeated.
 *
 * @author greg
 * @since 3/30/17
 */
public interface MultiFileCopier extends FileCopier {
    /**
     * Copy the contents of an input stream to the node
     *
     * @param context    context
     * @param basedir    base directory for relative paths of copied files
     * @param files      list of local files to copy, using relative paths from the basedir as subpaths appended
     *                   to the remote path
     * @param remotePath remote directory path to copy file(s) to
     * @param node       node
     *
     * @return File path of the file after copying to the node
     *
     * @throws FileCopierException if an error occurs
     */
    default String[] copyFiles(
            ExecutionContext context,
            final File basedir,
            List<File> files,
            String remotePath,
            INodeEntry node
    ) throws FileCopierException
    {
        return MultiFileCopierUtil.copyMultipleFiles(
                this,
                context,
                basedir,
                files,
                remotePath,
                node
        );
    }
}

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
 * Utility
 *
 * @author greg
 * @since 4/3/17
 */
public class MultiFileCopierUtil {
    /**
     * Perform a multiple file copy using a FileCopier
     *
     * @param copier
     * @param context
     * @param basedir
     * @param files
     * @param remotePath
     * @param node
     *
     * @return
     *
     * @throws FileCopierException
     */
    public static String[] copyMultipleFiles(
            FileCopier copier,
            ExecutionContext context,
            final File basedir,
            List<File> files,
            String remotePath,
            INodeEntry node
    ) throws FileCopierException
    {
        List<String> copied = new ArrayList<>();
        for (File file : files) {
            String relpath = FileUtils.relativePath(basedir, file);
            String destination = remotePath + relpath;
            copied.add(copier.copyFile(context, file, node, destination));
        }
        return copied.toArray(new String[copied.size()]);
    }
}

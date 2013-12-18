/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.resources.format.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A Resource Model source that caches results in a file, in case an error is thrown.
 */
public class FileResourceModelSourceCache implements ResourceModelSourceCache {
    private File cacheFile;
    private ResourceFormatGenerator generator;
    private ResourceModelSource fileResourceModelSource;

    public FileResourceModelSourceCache(File cacheFile, ResourceFormatGenerator generator,
            ResourceModelSource fileResourceModelSource) {
        this.cacheFile = cacheFile;
        this.generator = generator;
        this.fileResourceModelSource = fileResourceModelSource;
    }

    @Override
    public void storeNodesInCache(INodeSet nodes) throws ResourceModelSourceException {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
            try {
                generator.generateDocument(nodes, fileOutputStream);
            } finally {
                fileOutputStream.close();
            }
        } catch (ResourceFormatGeneratorException e) {
            throw new ResourceModelSourceException("Failed to generate cache file: " + e.getLocalizedMessage(), e);
        } catch (IOException e) {
            throw new ResourceModelSourceException("Failed to generate cache file: " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public INodeSet loadCachedNodes() throws ResourceModelSourceException {
        return fileResourceModelSource.getNodes();
    }
}

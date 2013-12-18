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
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorException;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserException;
import com.dtolabs.rundeck.core.resources.format.ResourceXMLFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.ResourceXMLFormatParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A Resource Model source that caches results in a file, in case an error is thrown.
 */
public class FileCachedResourceModelSource extends CachingResourceModelSource {
    private File cacheFile;
    private ResourceXMLFormatGenerator generator;
    private ResourceXMLFormatParser parser;

    public FileCachedResourceModelSource(ResourceModelSource delegate, File cacheFile) {
        super(delegate);
        this.cacheFile = cacheFile;
        this.generator = new ResourceXMLFormatGenerator();
        this.parser = new ResourceXMLFormatParser();
    }

    @Override
    void storeNodesInCache(INodeSet nodes) throws ResourceModelSourceException {
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
    INodeSet loadCachedNodes() throws ResourceModelSourceException{
        if(!cacheFile.exists()){
            return null;
        }
        try {
            return parser.parseDocument(cacheFile);
        } catch (ResourceFormatParserException e) {
            throw new ResourceModelSourceException("Failed to load cached file: " + e.getLocalizedMessage(), e);
        }
    }
}

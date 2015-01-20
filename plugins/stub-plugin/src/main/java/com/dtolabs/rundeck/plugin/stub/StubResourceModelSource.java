/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.plugin.stub;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

/**
 * StubResourceModelSource is ...
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-09-03
 */
public class StubResourceModelSource implements ResourceModelSource {
    Properties configuration;
    int count;
    String prefix;
    String suffix;
    String tags;

    public StubResourceModelSource(Properties configuration) {
        this.configuration = configuration;
        count = Integer.parseInt(configuration.getProperty("count"));
        prefix = configuration.getProperty("prefix");
        suffix = configuration.getProperty("suffix");
        tags = configuration.getProperty("tags");
    }

    @Override
    public INodeSet getNodes() throws ResourceModelSourceException {
        NodeSetImpl iNodeEntries = new NodeSetImpl();
        for(int i=0;i<count;i++){
            NodeEntryImpl nodeEntry = new NodeEntryImpl();
            nodeEntry.setNodename((prefix != null ? prefix : "node") + "-" + i  + (suffix != null ? suffix : ""));
            nodeEntry.setHostname((prefix != null ? prefix : "") + "host" + (suffix != null ? suffix : ""));
            nodeEntry.setUsername((prefix != null ? prefix : "") + "user" + (suffix != null ? suffix : ""));
            nodeEntry.setAttribute("node-executor", "stub");
            nodeEntry.setAttribute("file-copier", "stub");
            if(null!=tags){
                nodeEntry.setTags(new HashSet(Arrays.asList(tags.split("\\s*,\\s*"))));
            }

            iNodeEntries.putNode(nodeEntry);
        }
        return iNodeEntries;
    }
}

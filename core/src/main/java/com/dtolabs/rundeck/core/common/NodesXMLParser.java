/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* NodesXmlParser.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Apr 23, 2010 2:35:19 PM
* $Id$
*/
package com.dtolabs.rundeck.core.common;

import static com.dtolabs.shared.resources.ResourceXMLConstants.*;

import com.dtolabs.shared.resources.ResourceXMLConstants;
import com.dtolabs.shared.resources.ResourceXMLParser;
import com.dtolabs.shared.resources.ResourceXMLParserException;
import com.dtolabs.shared.resources.ResourceXMLReceiver;
import com.dtolabs.utils.Mapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;

/**
 * NodesXmlParser invokes the ResourceXmlParser to collate the Node entries, and sends the parsed nodes to the {@link
 * NodeReceiver} object with the parsed node entities.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodesXMLParser implements NodeFileParser, ResourceXMLReceiver {
    final File file;
    final InputStream input;
    final NodeReceiver nodeReceiver;

    /**
     * Create NodesXmlParser
     *
     * @param nodeReceiver Nodes object
     */
    public NodesXMLParser(final NodeReceiver nodeReceiver) {
        this.file = null;
        this.input=null;
        this.nodeReceiver = nodeReceiver;
    }
    /**
     * Create NodesXmlParser
     *
     * @param file         nodes file
     * @param nodeReceiver Nodes object
     */
    public NodesXMLParser(final File file, final NodeReceiver nodeReceiver) {
        this.file = file;
        this.input=null;
        this.nodeReceiver = nodeReceiver;
    }
    /**
     * Create NodesXmlParser
     *
     * @param input         nodes file
     * @param nodeReceiver Nodes object
     */
    public NodesXMLParser(final InputStream input, final NodeReceiver nodeReceiver) {
        this.input = input;
        this.file=null;
        this.nodeReceiver = nodeReceiver;
    }

    /**
     * Parse the project.xml formatted file and fill in the nodes found
     */
    public void parse() throws NodeFileParserException {
        final ResourceXMLParser resourceXMLParser;
        if(null!=file){
            resourceXMLParser=new ResourceXMLParser(file);
        }else{
            resourceXMLParser = new ResourceXMLParser(input);
        }
        //parse both node and settings
        resourceXMLParser.setReceiver(this);
//        long start = System.currentTimeMillis();
        try {
            resourceXMLParser.parse();
        } catch (ResourceXMLParserException e) {
            throw new NodeFileParserException(e);
        } catch (IOException e) {
            throw new NodeFileParserException(e);
        }
//        System.err.println("parse: " + (System.currentTimeMillis() - start));
    }

    public boolean resourceParsed(final ResourceXMLParser.Entity entity) {
        //continue parsing entities until the end
        return true;
    }

    /**
     * Fill the NodeEntryImpl based on the Entity's parsed attributes
     *
     * @param entity entity
     * @param node node
     */
    private void fillNode(final ResourceXMLParser.Entity entity, final NodeEntryImpl node) {
        node.setUsername(entity.getProperty(NODE_USERNAME));
        node.setHostname(entity.getProperty(NODE_HOSTNAME));
        node.setOsArch(entity.getProperty(NODE_OS_ARCH));
        node.setOsFamily(entity.getProperty(NODE_OS_FAMILY));
        node.setOsName(entity.getProperty(NODE_OS_NAME));
        node.setOsVersion(entity.getProperty(NODE_OS_VERSION));
        node.setDescription(entity.getProperty(COMMON_DESCRIPTION));
        final String tags = entity.getProperty(COMMON_TAGS);
        final HashSet<String> tags1;
        if (null != tags && !"".equals(tags)) {
            tags1 = new HashSet<String>();
            for (final String s : tags.split(",")) {
                tags1.add(s.trim());
            }
        } else {
            tags1 = new HashSet<String>();
        }
        node.setTags(tags1);

        if (null == node.getAttributes()) {
            node.setAttributes(new HashMap<String, String>());
        }
        if (null != entity.getProperties()) {
            for (String key : entity.getProperties().stringPropertyNames()) {
                if (!ResourceXMLConstants.allPropSet.contains(key)) {
                    node.getAttributes().put(key, entity.getProperty(key));
                }
            }

        }
        //parse embedded attribute elements
    }

    public void resourcesParsed(final ResourceXMLParser.EntitySet entities) {
        //all entities are parsed, now process the nodes
        for (final ResourceXMLParser.Entity entity : entities.getEntities()) {
            if (!NODE_ENTITY_TAG.equals(entity.getResourceType())) {
                continue;
            }

            /*
            * Create a INodeEntry from the parsed entity and put it into the Nodes object
            */
            final NodeEntryImpl node = new NodeEntryImpl(entity.getProperty("hostname"), entity.getName());
            fillNode(entity, node);
            if (null != nodeReceiver) {
                nodeReceiver.putNode(node);
            }
        }
    }
}

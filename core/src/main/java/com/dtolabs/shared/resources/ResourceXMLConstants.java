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
* ResourceXMLConstants.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Apr 26, 2010 11:44:37 AM
* $Id$
*/
package com.dtolabs.shared.resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ResourceXMLConstants contains constants for parsing Resource XML format.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ResourceXMLConstants {
    /**
     * Public identifier for Project document 1.0 dtd
     */
    public static final String DTD_PROJECT_DOCUMENT_1_0_EN =
        "-//DTO Labs Inc.//DTD Resources Document 1.0//EN";
    /**
     * resources path to project.dtd file
     */
    public static final String PROJECT_DTD_RESOURCE_PATH = "com/dtolabs/shared/resources/project.dtd";
    public static final String COMMON_DESCRIPTION = "description";
    public static final String COMMON_NAME = "name";
    public static final String COMMON_TAGS = "tags";
    public static final String BEAN_NAME = "nodename";
    /**
     * Common attributes of all entity xml nodes
     */
    static final String[] commonProps = {
        COMMON_DESCRIPTION,
        COMMON_TAGS
    };
    public static final String NODE_HOSTNAME = "hostname";
    public static final String NODE_OS_ARCH = "osArch";
    public static final String NODE_OS_FAMILY = "osFamily";
    public static final String NODE_OS_NAME = "osName";
    public static final String NODE_OS_VERSION = "osVersion";
    public static final String NODE_USERNAME = "username";
    public static final String NODE_EDIT_URL = "editUrl";
    public static final String NODE_REMOTE_URL = "remoteUrl";
    /**
     * attributes of "node" nodes
     */
    static final String[] nodeProps = {
        NODE_HOSTNAME,
        NODE_OS_ARCH,
        NODE_OS_FAMILY,
        NODE_OS_NAME,
        NODE_OS_VERSION,
        NODE_USERNAME,
//        NODE_EDIT_URL,
//        NODE_REMOTE_URL
    };
    private static final HashSet<String> _nodePropSet= new HashSet<String>();
    static {
        _nodePropSet.addAll(Arrays.asList(nodeProps));
    }


    /**
     * Set of all node specific properties
     */
    public static final Set<String> nodePropSet =  Collections.unmodifiableSet(_nodePropSet);
    private static final HashSet<String> _allPropSet = new HashSet<String>();
    static {
        _allPropSet.addAll(nodePropSet);
        _allPropSet.addAll(Arrays.asList(commonProps));
        _allPropSet.add(COMMON_NAME);
        _allPropSet.add(BEAN_NAME);
    }

    /**
     * Set of all resource properties
     */
    public static final Set<String> allPropSet =  Collections.unmodifiableSet(_allPropSet);

    public static final String NODE_ENTITY_TAG = "node";
    public static final String ATTRIBUTE_TAG = "attribute";
    public static final String ATTRIBUTE_NAME_ATTR = "name";
    public static final String ATTRIBUTE_VALUE_ATTR = "value";

}

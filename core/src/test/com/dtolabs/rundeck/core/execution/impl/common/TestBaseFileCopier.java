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
* TestBaseFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 4:20 PM
* 
*/
package com.dtolabs.rundeck.core.execution.impl.common;

import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import junit.framework.TestCase;

import java.util.*;

/**
 * TestBaseFileCopier is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestBaseFileCopier extends TestCase {
    public void setUp() throws Exception {

    }

    public void tearDown() throws Exception {

    }

    public void testRemoteDirForNode() throws Exception {
        final BaseFileCopier baseFileCopier = new BaseFileCopier();
        NodeEntryImpl node = new NodeEntryImpl();
        node.setOsFamily("unix");

        assertEquals("/tmp/", baseFileCopier.getRemoteDirForNode(node));

        node.setOsFamily("windows");

        assertEquals("C:/WINDOWS/TEMP/", baseFileCopier.getRemoteDirForNode(node));
    }

    public void testAppendFilepath() throws Exception {
        final BaseFileCopier baseFileCopier = new BaseFileCopier();
        NodeEntryImpl node = new NodeEntryImpl();
        node.setOsFamily("unix");

        assertEquals("test.sh", baseFileCopier.appendRemoteFileExtensionForNode(node, "test"));
        assertEquals("test.sh", baseFileCopier.appendRemoteFileExtensionForNode(node, "test.sh"));
        node.setOsFamily("windows");
        assertEquals("test.bat", baseFileCopier.appendRemoteFileExtensionForNode(node, "test"));
        assertEquals("test.bat", baseFileCopier.appendRemoteFileExtensionForNode(node, "test.bat"));
    }
}

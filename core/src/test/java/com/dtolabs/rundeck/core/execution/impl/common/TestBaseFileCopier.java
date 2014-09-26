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

import java.util.regex.Pattern;

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

        assertEquals("C:\\WINDOWS\\TEMP\\", baseFileCopier.getRemoteDirForNode(node));
    }

    public void testGenerateFilepathUnix() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        assertMatches("/tmp/\\d+-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
    }
    public void testGenerateFilepathFileExtension() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        assertMatches("/tmp/\\d+-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("/tmp/\\d+-node1-blah.sh.ext", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh","ext"));
        assertMatches("/tmp/\\d+-node1-blah.sh.ext", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh",".ext"));
        assertMatches("/tmp/\\d+-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh",null));
    }
    public void testGenerateFilepathFileExtensionWindows() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("windows");
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-node1-blah.sh.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-node1-blah.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-node1-blah.sh.ext", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh","ext"));
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-node1-blah.bat.ext", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat","ext"));
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-node1-blah.sh.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh",null));
    }

    public void testGenerateFilepathWindows() throws Exception {
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("windows");
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-node1-blah.sh.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-node1-blah.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));
    }
    public void testGenerateFilepathTargetDir() throws Exception {
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("windows");
        node.setAttribute(BaseFileCopier.FILE_COPY_DESTINATION_DIR, "c:\\my\\tmp");
        assertMatches("c:\\\\my\\\\tmp\\\\\\d+-node1-blah.sh.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("c:\\\\my\\\\tmp\\\\\\d+-node1-blah.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));
        node.setAttribute(BaseFileCopier.FILE_COPY_DESTINATION_DIR, "c:\\my\\tmp\\");
        assertMatches("c:\\\\my\\\\tmp\\\\\\d+-node1-blah.sh.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("c:\\\\my\\\\tmp\\\\\\d+-node1-blah.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));

        node.setOsFamily("unix");
        node.setAttribute(BaseFileCopier.FILE_COPY_DESTINATION_DIR, "/my/tmp");
        assertMatches("/my/tmp/\\d+-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("/my/tmp/\\d+-node1-blah.bat.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));
        node.setAttribute(BaseFileCopier.FILE_COPY_DESTINATION_DIR, "/my/tmp/");
        assertMatches("/my/tmp/\\d+-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("/my/tmp/\\d+-node1-blah.bat.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));
    }
    public void testGenerateFilepathBadChars() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node name1");
        node.setOsFamily("unix");
        assertMatches("/tmp/\\d+-node_name1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("/tmp/\\d+-node_name1-blah_flah.sh", BaseFileCopier.generateRemoteFilepathForNode(node,
                "blah flah.sh"));
        assertMatches("/tmp/\\d+-node_name1-blah___flah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah///flah.sh"));
    }

    private void assertMatches(String pattern, String string) {
        assertTrue(pattern + " did not match: " + string, matches(pattern, string));
    }

    private static boolean matches(String s, String s1) {
        return Pattern.compile(s).matcher(s1).matches();
    }
}

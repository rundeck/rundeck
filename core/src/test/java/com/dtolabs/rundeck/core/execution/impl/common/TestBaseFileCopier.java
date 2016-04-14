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

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.authorization.Authorization;
import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
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
        assertMatches("/tmp/\\d+-.{10}-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
    }
    public void testGenerateFilepathFileExtension() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        assertMatches("/tmp/\\d+-.{10}-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("/tmp/\\d+-.{10}-node1-blah.sh.ext", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh","ext"));
        assertMatches(
                "/tmp/\\d+-.{10}-node1-blah.sh.ext", BaseFileCopier.generateRemoteFilepathForNode(
                        node,
                        "blah.sh",
                        ".ext"
                )
        );
        assertMatches("/tmp/\\d+-.{10}-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh",null));
    }
    public void testGenerateFilepathFileExtensionIdent() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        assertMatches("/tmp/\\d+-abc-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh",null,"abc"));
        assertMatches("/tmp/\\d+-def-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh",null,"def"));
    }
    class testProject implements IRundeckProject{
        Map<String, String> properties;
        @Override
        public String getName() {
            return null;
        }

        @Override
        public IProjectInfo getInfo() {
            return null;
        }

        @Override
        public List<Map<String, Object>> listResourceModelConfigurations() {
            return null;
        }

        @Override
        public INodeSet getNodeSet() throws NodeFileParserException {
            return null;
        }

        @Override
        public Authorization getProjectAuthorization() {
            return null;
        }

        @Override
        public boolean updateNodesResourceFile() throws UpdateUtils.UpdateException {
            return false;
        }

        @Override
        public void updateNodesResourceFileFromUrl(
                final String providerURL,
                final String username,
                final String password
        )
                throws UpdateUtils.UpdateException
        {

        }

        @Override
        public void updateNodesResourceFile(final INodeSet nodeset) throws UpdateUtils.UpdateException {

        }

        @Override
        public String getProperty(final String name) {
            return properties.get(name);
        }

        @Override
        public boolean hasProperty(final String key) {
            return properties.containsKey(key);
        }

        @Override
        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public Map<String, String> getProjectProperties() {
            return properties;
        }

        @Override
        public void mergeProjectProperties(final Properties properties, final Set<String> removePrefixes) {

        }

        @Override
        public void setProjectProperties(final Properties properties) {

        }

        @Override
        public Date getConfigLastModifiedTime() {
            return null;
        }

        @Override
        public IProjectNodes getProjectNodes() {
            return null;
        }

        @Override
        public boolean existsFileResource(final String path) {
            return false;
        }

        @Override
        public boolean existsDirResource(final String path) {
            return false;
        }

        @Override
        public List<String> listDirPaths(final String path) {
            return null;
        }

        @Override
        public boolean deleteFileResource(final String path) {
            return false;
        }

        @Override
        public long storeFileResource(final String path, final InputStream input) throws IOException {
            return 0;
        }

        @Override
        public long loadFileResource(final String path, final OutputStream output) throws IOException {
            return 0;
        }
    }
    public void testGenerateFilepathFileExtensionProject() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        testProject project=new testProject();
        project.properties = new HashMap<>();
        project.properties.put(BaseFileCopier.PROJECT_FILE_COPY_DESTINATION_DIR, "/tmp2");
        assertMatches(
                "/tmp2/\\d+-abc-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(
                        node,
                        project,
                        null,
                        "blah.sh",
                        null,
                        "abc"
                )
        );
    }
    public void testGenerateFilepathFileExtensionProjectOsFamily() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        testProject project=new testProject();
        project.properties = new HashMap<>();
        project.properties.put(BaseFileCopier.PROJECT_FILE_COPY_DESTINATION_DIR, "/tmp2");
        project.properties.put(BaseFileCopier.PROJECT_FILE_COPY_DESTINATION_DIR+".unix", "/tmp3");
        assertMatches(
                "/tmp3/\\d+-abc-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(
                        node,
                        project,
                        null,
                        "blah.sh",
                        null,
                        "abc"
                )
        );
    }
    public void testGenerateFilepathFileExtensionProjectOsFamily2() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("windows");
        testProject project=new testProject();
        project.properties = new HashMap<>();
        project.properties.put(BaseFileCopier.PROJECT_FILE_COPY_DESTINATION_DIR, "/tmp2");
        project.properties.put(BaseFileCopier.PROJECT_FILE_COPY_DESTINATION_DIR+".unix", "/tmp3");
        project.properties.put(BaseFileCopier.PROJECT_FILE_COPY_DESTINATION_DIR+".windows", "c:\\my\\tmp");
        assertMatches(
                "c:\\\\my\\\\tmp\\\\\\d+-abc-node1-blah.sh.bat",
                BaseFileCopier.generateRemoteFilepathForNode(
                        node,
                        project,
                        null,
                        "blah.sh",
                        null,
                        "abc"
                )
        );
    }
    class testFramework implements IFramework{
        Map<String, String> properties;
        @Override
        public ProjectManager getFrameworkProjectMgr() {
            return null;
        }

        @Override
        public IPropertyLookup getPropertyLookup() {
            Properties props = new Properties();
            props.putAll(properties);

            return PropertyLookup.create(props);
        }

        @Override
        public String getFrameworkNodeHostname() {
            return null;
        }

        @Override
        public String getFrameworkNodeName() {
            return null;
        }

        @Override
        public NodeEntryImpl createFrameworkNode() {
            return null;
        }

        @Override
        public INodeSet filterAuthorizedNodes(
                final String project,
                final Set<String> actions,
                final INodeSet unfiltered,
                final AuthContext authContext
        )
        {
            return null;
        }

        @Override
        public INodeDesc getNodeDesc() {
            return null;
        }

        @Override
        public boolean isLocalNode(final INodeDesc node) {
            return false;
        }

        @Override
        public FrameworkSupportService getService(final String name) {
            return null;
        }

        @Override
        public void setService(final String name, final FrameworkSupportService service) {

        }

        @Override
        public OrchestratorService getOrchestratorService() {
            return null;
        }

        @Override
        public ExecutionService getExecutionService() {
            return null;
        }

        @Override
        public WorkflowExecutionService getWorkflowExecutionService() {
            return null;
        }

        @Override
        public StepExecutionService getStepExecutionService() {
            return null;
        }

        @Override
        public FileCopier getFileCopierForNodeAndProject(
                final INodeEntry node, final String project
        ) throws ExecutionServiceException
        {
            return null;
        }

        @Override
        public FileCopierService getFileCopierService() {
            return null;
        }

        @Override
        public NodeExecutor getNodeExecutorForNodeAndProject(
                final INodeEntry node, final String project
        ) throws ExecutionServiceException
        {
            return null;
        }

        @Override
        public NodeExecutorService getNodeExecutorService() throws ExecutionServiceException {
            return null;
        }

        @Override
        public NodeStepExecutionService getNodeStepExecutorService() throws ExecutionServiceException {
            return null;
        }

        @Override
        public NodeStepExecutor getNodeStepExecutorForItem(final NodeStepExecutionItem item)
                throws ExecutionServiceException
        {
            return null;
        }

        @Override
        public NodeDispatcher getNodeDispatcherForContext(final ExecutionContext context)
                throws ExecutionServiceException
        {
            return null;
        }

        @Override
        public ResourceModelSourceService getResourceModelSourceService() {
            return null;
        }

        @Override
        public ResourceFormatParserService getResourceFormatParserService() {
            return null;
        }

        @Override
        public ResourceFormatGeneratorService getResourceFormatGeneratorService() {
            return null;
        }

        @Override
        public ServiceProviderLoader getPluginManager() {
            return null;
        }
    }
    public void testGenerateFilepathFileExtensionFramework() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        testFramework framework = new testFramework();
        framework.properties = new HashMap<>();
        framework.properties.put(BaseFileCopier.FRAMEWORK_FILE_COPY_DESTINATION_DIR, "/tmp3");
        assertMatches(
                "/tmp3/\\d+-abc-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(
                        node,
                        null,
                        framework,
                        "blah.sh",
                        null,
                        "abc"
                )
        );
    }
    public void testGenerateFilepathFileExtensionFrameworkOsfamily() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        testFramework framework = new testFramework();
        framework.properties = new HashMap<>();
        framework.properties.put(BaseFileCopier.FRAMEWORK_FILE_COPY_DESTINATION_DIR, "/tmp3");
        framework.properties.put(BaseFileCopier.FRAMEWORK_FILE_COPY_DESTINATION_DIR+".unix", "/tmp4");
        assertMatches(
                "/tmp4/\\d+-abc-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(
                        node,
                        null,
                        framework,
                        "blah.sh",
                        null,
                        "abc"
                )
        );
    }
    public void testGenerateFilepathFileExtensionFrameworkOsfamilyWindows() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("windows");
        testFramework framework = new testFramework();
        framework.properties = new HashMap<>();
        framework.properties.put(BaseFileCopier.FRAMEWORK_FILE_COPY_DESTINATION_DIR, "/tmp3");
        framework.properties.put(BaseFileCopier.FRAMEWORK_FILE_COPY_DESTINATION_DIR+".unix", "/tmp4");
        framework.properties.put(BaseFileCopier.FRAMEWORK_FILE_COPY_DESTINATION_DIR+".windows", "c:\\tmp\\blah");
        assertMatches(
                "c:\\\\tmp\\\\blah\\\\\\d+-abc-node1-blah.sh.bat",
                BaseFileCopier.generateRemoteFilepathForNode(
                        node,
                        null,
                        framework,
                        "blah.sh",
                        null,
                        "abc"
                )
        );
    }
    public void testGenerateFilepathFileExtensionNodeVsFramework() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        node.getAttributes().put(BaseFileCopier.FILE_COPY_DESTINATION_DIR, "/tmp4");
        testFramework framework = new testFramework();
        framework.properties = new HashMap<>();
        framework.properties.put(BaseFileCopier.FRAMEWORK_FILE_COPY_DESTINATION_DIR, "/tmp3");
        assertMatches(
                "/tmp4/\\d+-abc-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(
                        node,
                        null,
                        framework,
                        "blah.sh",
                        null,
                        "abc"
                )
        );
    }
    public void testGenerateFilepathFileExtensionNodeVsProject() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        node.getAttributes().put(BaseFileCopier.FILE_COPY_DESTINATION_DIR, "/tmp4");
        testProject project = new testProject();
        project.properties = new HashMap<>();
        project.properties.put(BaseFileCopier.PROJECT_FILE_COPY_DESTINATION_DIR, "/tmp3");
        assertMatches(
                "/tmp4/\\d+-abc-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(
                        node,
                        project,
                        null,
                        "blah.sh",
                        null,
                        "abc"
                )
        );
    }
    public void testGenerateFilepathFileExtensionFrameworkVsProject() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("unix");
        testProject project = new testProject();
        project.properties = new HashMap<>();
        project.properties.put(BaseFileCopier.PROJECT_FILE_COPY_DESTINATION_DIR, "/tmp3");
        testFramework framework = new testFramework();
        framework.properties = new HashMap<>();
        framework.properties.put(BaseFileCopier.FRAMEWORK_FILE_COPY_DESTINATION_DIR, "/tmp4");
        assertMatches(
                "/tmp3/\\d+-abc-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(
                        node,
                        project,
                        framework,
                        "blah.sh",
                        null,
                        "abc"
                )
        );
    }
    public void testGenerateFilepathFileExtensionWindows() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("windows");
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-.{10}-node1-blah.sh.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-.{10}-node1-blah.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-.{10}-node1-blah.sh.ext", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh","ext"));
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-.{10}-node1-blah.bat.ext", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat","ext"));
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-.{10}-node1-blah.sh.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh",null));
    }

    public void testGenerateFilepathWindows() throws Exception {
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("windows");
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-.{10}-node1-blah.sh.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("C:\\\\WINDOWS\\\\TEMP\\\\\\d+-.{10}-node1-blah.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));
    }
    public void testGenerateFilepathTargetDir() throws Exception {
        NodeEntryImpl node = new NodeEntryImpl("node1");
        node.setOsFamily("windows");
        node.setAttribute(BaseFileCopier.FILE_COPY_DESTINATION_DIR, "c:\\my\\tmp");
        assertMatches("c:\\\\my\\\\tmp\\\\\\d+-.{10}-node1-blah.sh.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("c:\\\\my\\\\tmp\\\\\\d+-.{10}-node1-blah.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));
        node.setAttribute(BaseFileCopier.FILE_COPY_DESTINATION_DIR, "c:\\my\\tmp\\");
        assertMatches("c:\\\\my\\\\tmp\\\\\\d+-.{10}-node1-blah.sh.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("c:\\\\my\\\\tmp\\\\\\d+-.{10}-node1-blah.bat", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));

        node.setOsFamily("unix");
        node.setAttribute(BaseFileCopier.FILE_COPY_DESTINATION_DIR, "/my/tmp");
        assertMatches("/my/tmp/\\d+-.{10}-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("/my/tmp/\\d+-.{10}-node1-blah.bat.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));
        node.setAttribute(BaseFileCopier.FILE_COPY_DESTINATION_DIR, "/my/tmp/");
        assertMatches("/my/tmp/\\d+-.{10}-node1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("/my/tmp/\\d+-.{10}-node1-blah.bat.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.bat"));
    }
    public void testGenerateFilepathBadChars() throws Exception{
        NodeEntryImpl node = new NodeEntryImpl("node name1");
        node.setOsFamily("unix");
        assertMatches("/tmp/\\d+-.{10}-node_name1-blah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah.sh"));
        assertMatches("/tmp/\\d+-.{10}-node_name1-blah_flah.sh", BaseFileCopier.generateRemoteFilepathForNode(node,
                "blah flah.sh"));
        assertMatches("/tmp/\\d+-.{10}-node_name1-blah___flah.sh", BaseFileCopier.generateRemoteFilepathForNode(node, "blah///flah.sh"));
    }

    private void assertMatches(String pattern, String string) {
        assertTrue(pattern + " did not match: " + string, matches(pattern, string));
    }

    private static boolean matches(String s, String s1) {
        return Pattern.compile(s).matcher(s1).matches();
    }
}

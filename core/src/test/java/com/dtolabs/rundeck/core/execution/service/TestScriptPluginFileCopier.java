package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta;
import com.dtolabs.rundeck.core.utils.ScriptExecHelper;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TestScriptPluginFileCopier {
    //
    // junit exported java properties (e.g. from maven's project.properties)
    //
    public static String RDECK_BASE = System.getProperty("rdeck.base", "build/rdeck_base");

    //
    // derived modules and projects base
    //
    private static String PROJECTS_BASE = RDECK_BASE + "/" + "projects";

    private Framework getFramework() {
        return Framework.getInstance(RDECK_BASE, PROJECTS_BASE);
    }

    @Before
    public void setup() {
        getFramework().getFrameworkProjectMgr().createFrameworkProject(TestScriptPluginFileCopier.class.getName());
    }

    @After
    public void teardown() {
        getFramework().getFrameworkProjectMgr().removeFrameworkProject(TestScriptPluginFileCopier.class.getName());
    }

    static class TestProvider implements ScriptPluginProvider {
        private String name;
        private String service;
        private String scriptArgs;
        private String scriptInterpreter;
        private File archiveFile;
        private File contentsBasedir;
        private File scriptFile;
        private boolean interpreterArgsQuoted;
        private Map<String, Object> metadata;

        @Override public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        @Override public String getScriptArgs() {
            return scriptArgs;
        }

        public void setScriptArgs(String scriptArgs) {
            this.scriptArgs = scriptArgs;
        }

        @Override public String getScriptInterpreter() {
            return scriptInterpreter;
        }

        public void setScriptInterpreter(String scriptInterpreter) {
            this.scriptInterpreter = scriptInterpreter;
        }

        @Override public File getArchiveFile() {
            return archiveFile;
        }

        public void setArchiveFile(File archiveFile) {
            this.archiveFile = archiveFile;
        }

        @Override public File getContentsBasedir() {
            return contentsBasedir;
        }

        public void setContentsBasedir(File contentsBasedir) {
            this.contentsBasedir = contentsBasedir;
        }

        @Override public File getScriptFile() {
            return scriptFile;
        }

        public void setScriptFile(File scriptFile) {
            this.scriptFile = scriptFile;
        }

        public boolean getInterpreterArgsQuoted() {
            return interpreterArgsQuoted;
        }

        public void setInterpreterArgsQuoted(boolean interpreterArgsQuoted) {
            this.interpreterArgsQuoted = interpreterArgsQuoted;
        }

        @Override public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        @Override
        public PluginMeta getPluginMeta() {
            return null;
        }

        @Override
        public boolean getDefaultMergeEnvVars() {
            return false;
        }
    }

    public static class TestHelper implements ScriptExecHelper {
        @Override public int runLocalCommand(
                String osFamily,
                ExecArgList execArgList,
                Map<String, Map<String, String>> dataContext,
                File workingdir,
                OutputStream outputStream,
                OutputStream errorStream
        ) throws IOException, InterruptedException {
            Assert.fail("Unexpected");
            return 0;
        }

        @Override public int runLocalCommand(
                String[] command,
                Map<String, String> envMap,
                File workingdir,
                OutputStream outputStream,
                OutputStream errorStream
        ) throws IOException, InterruptedException {
            Assert.fail("Unexpected");
            return 0;
        }

        @Override public String[] createScriptArgs(
                Map<String, Map<String, String>> localDataContext,
                String scriptargs,
                String[] scriptargsarr,
                String scriptinterpreter,
                boolean interpreterargsquoted,
                String filepath
        ) {
            Assert.fail("Unexpected");
            return new String[0];
        }

        @Override public String[] createScriptArgs(
                Map<String, Map<String, String>> localDataContext,
                INodeEntry node,
                String scriptargs,
                String[] scriptargsarr,
                String scriptinterpreter,
                boolean interpreterargsquoted,
                String filepath
        ) {
            Assert.fail("Unexpected");
            return new String[0];
        }

        @Override public ExecArgList createScriptArgList(
                String filepath,
                String scriptargs,
                String[] scriptargsarr,
                String scriptinterpreter,
                boolean interpreterargsquoted
        ) {
            Assert.fail("Unexpected");
            return null;
        }

        @Override
        public Map<String, String> loadLocalEnvironment() {
            Assert.fail("Unexpected");
            return null;
        }
    }
    private class testListener implements ExecutionListener {
        @Override public boolean isTerse() {
            return false;
        }

        @Override public String getLogFormat() {
            return null;
        }

        @Override public void log(int level, String message) {

        }

        @Override public void event(String eventType, String message, Map eventMeta) {

        }

        @Override public FailedNodesListener getFailedNodesListener() {
            return null;
        }

        @Override public void beginNodeExecution(
                ExecutionContext context, String[] command, INodeEntry node
        ) {

        }

        @Override public void finishNodeExecution(
                NodeExecutorResult result, ExecutionContext context, String[] command, INodeEntry node
        ) {

        }

        @Override public void beginNodeDispatch(
                ExecutionContext context, StepExecutionItem item
        ) {

        }

        @Override public void beginNodeDispatch(
                ExecutionContext context, Dispatchable item
        ) {

        }

        @Override public void finishNodeDispatch(
                DispatcherResult result, ExecutionContext context, StepExecutionItem item
        ) {

        }

        @Override public void finishNodeDispatch(
                DispatcherResult result, ExecutionContext context, Dispatchable item
        ) {

        }

        @Override public void beginFileCopyFileStream(
                ExecutionContext context, InputStream input, INodeEntry node
        ) {

        }

        @Override public void beginFileCopyFile(
                ExecutionContext context, File input, INodeEntry node
        ) {

        }

        @Override public void beginFileCopyScriptContent(
                ExecutionContext context, String input, INodeEntry node
        ) {

        }

        @Override public void finishFileCopy(
                String result, ExecutionContext context, INodeEntry node
        ) {

        }

        @Override public ExecutionListenerOverride createOverride() {
            return null;
        }
    }

    @Test
    public void base_copyfile() throws FileCopierException, IOException {
        Framework framework = getFramework();
        TestProvider testProvider = new TestProvider();
        File archiveFile = File.createTempFile("test-archive", "tmp");
        archiveFile.deleteOnExit();
        testProvider.setArchiveFile(archiveFile);
        final File scriptFile = File.createTempFile("test-scriptfile", "tmp");
        scriptFile.deleteOnExit();
        testProvider.setScriptFile(scriptFile);
        testProvider.setContentsBasedir(File.createTempFile("test-basedir", "tmp"));
        testProvider.setMetadata(new HashMap<String, Object>());
        testProvider.setName("test-plugin");
        testProvider.setScriptArgs("");

        ScriptPluginFileCopier scriptPluginFileCopier = new ScriptPluginFileCopier(testProvider, framework);
        scriptPluginFileCopier.setScriptExecHelper(
                new TestHelper() {
                    public ExecArgList createScriptArgList(
                            final String filepath,
                            final String scriptargs,
                            final String[] scriptargsarr,
                            final String scriptinterpreter,
                            final boolean interpreterargsquoted
                    ) {
                        Assert.assertEquals(scriptFile.getAbsolutePath(), filepath);
                        Assert.assertEquals("", scriptargs);
                        Assert.assertEquals(null, scriptargsarr);
                        Assert.assertEquals(null, scriptinterpreter);
                        Assert.assertEquals(false, interpreterargsquoted);
                        return ExecArgList.builder().arg(filepath, false).build();
                    }
                    public int runLocalCommand(
                            final String osFamily,
                            final ExecArgList execArgList,
                            final Map<String, Map<String, String>> dataContext,
                            final File workingdir,
                            final OutputStream outputStream,
                            final OutputStream errorStream
                    )
                            throws IOException, InterruptedException {
                        Assert.assertNotNull(osFamily);
                        Assert.assertEquals(
                                new ArrayList<String>(Arrays.asList(scriptFile.getAbsolutePath())),
                                execArgList.asFlatStringList()
                        );
                        //temp file containing input script
                        Assert.assertNotNull(dataContext.get("file-copy").get("file"));
                        Assert.assertEquals(null, workingdir);
                        Assert.assertNotNull(outputStream);
                        Assert.assertNotNull(errorStream);
                        return 0;
                    }

                }
        );
        Map<String, String> stringStringMap = new HashMap<String,String>();
        Map<String, Map<String, String>> testdc = DataContextUtils.addContext("test", stringStringMap, null);
        ExecutionContextImpl context = ExecutionContextImpl.builder()
                                                           .framework(framework)
                                                           .frameworkProject(TestScriptPluginFileCopier.class.getName())
                                                           .dataContext(testdc)
                                                           .executionListener(new testListener())
                                                           .build();
        NodeEntryImpl nodeEntry = new NodeEntryImpl("node1");
        String result = scriptPluginFileCopier.copyFile(context, null, nodeEntry);
        Assert.assertNotNull(result);
    }

    /**
     * The destination is passed in, and expected on return
     * @throws FileCopierException
     * @throws IOException
     */
    @Test
    public void destination_copyfile() throws FileCopierException, IOException {
        Framework framework = getFramework();
        TestProvider testProvider = new TestProvider();
        File archiveFile = File.createTempFile("test-archive", "tmp");
        archiveFile.deleteOnExit();
        testProvider.setArchiveFile(archiveFile);
        final File scriptFile = File.createTempFile("test-scriptfile", "tmp");
        scriptFile.deleteOnExit();
        testProvider.setScriptFile(scriptFile);
        testProvider.setContentsBasedir(File.createTempFile("test-basedir", "tmp"));
        testProvider.setMetadata(new HashMap<String, Object>());
        testProvider.setName("test-plugin");
        testProvider.setScriptArgs("");

        ScriptPluginFileCopier scriptPluginFileCopier = new ScriptPluginFileCopier(testProvider, framework);
        scriptPluginFileCopier.setScriptExecHelper(
                new TestHelper() {
                    public ExecArgList createScriptArgList(
                            final String filepath,
                            final String scriptargs,
                            final String[] scriptargsarr,
                            final String scriptinterpreter,
                            final boolean interpreterargsquoted
                    ) {
                        Assert.assertEquals(scriptFile.getAbsolutePath(), filepath);
                        Assert.assertEquals("", scriptargs);
                        Assert.assertEquals(null, scriptargsarr);
                        Assert.assertEquals(null, scriptinterpreter);
                        Assert.assertEquals(false, interpreterargsquoted);
                        return ExecArgList.builder().arg(filepath, false).build();
                    }
                    public int runLocalCommand(
                            final String osFamily,
                            final ExecArgList execArgList,
                            final Map<String, Map<String, String>> dataContext,
                            final File workingdir,
                            final OutputStream outputStream,
                            final OutputStream errorStream
                    )
                            throws IOException, InterruptedException {
                        Assert.assertNotNull(osFamily);
                        Assert.assertEquals(
                                new ArrayList<String>(Arrays.asList(scriptFile.getAbsolutePath())),
                                execArgList.asFlatStringList()
                        );
                        //temp file containing input script
                        Assert.assertNotNull(dataContext.get("file-copy").get("file"));
                        Assert.assertEquals(null, workingdir);
                        Assert.assertNotNull(outputStream);
                        Assert.assertNotNull(errorStream);
                        return 0;
                    }

                }
        );
        Map<String, String> stringStringMap = new HashMap<String,String>();
        Map<String, Map<String, String>> testdc = DataContextUtils.addContext("test", stringStringMap, null);
        ExecutionContextImpl context = ExecutionContextImpl.builder()
                                                           .framework(framework)
                                                           .frameworkProject(TestScriptPluginFileCopier.class.getName())
                                                           .dataContext(testdc)
                                                           .executionListener(new testListener())
                                                           .build();
        NodeEntryImpl nodeEntry = new NodeEntryImpl("node1");
        String result = scriptPluginFileCopier.copyFile(context, null, nodeEntry,"/my/path/test");
        Assert.assertEquals("/my/path/test",result);
    }


    /**
     * The destination is passed in, but the filecopier echoes a filepath
     * @throws FileCopierException
     * @throws IOException
     */
    @Test
    public void echo_filepath_copyfile() throws FileCopierException, IOException {
        Framework framework = getFramework();
        TestProvider testProvider = new TestProvider();
        File archiveFile = File.createTempFile("test-archive", "tmp");
        archiveFile.deleteOnExit();
        testProvider.setArchiveFile(archiveFile);
        final File scriptFile = File.createTempFile("test-scriptfile", "tmp");
        scriptFile.deleteOnExit();
        testProvider.setScriptFile(scriptFile);
        testProvider.setContentsBasedir(File.createTempFile("test-basedir", "tmp"));
        testProvider.setMetadata(new HashMap<String, Object>());
        testProvider.setName("test-plugin");
        testProvider.setScriptArgs("");

        final String testOutputFilepath = "/another/test/path";

        ScriptPluginFileCopier scriptPluginFileCopier = new ScriptPluginFileCopier(testProvider, framework);
        scriptPluginFileCopier.setScriptExecHelper(
                new TestHelper() {
                    public ExecArgList createScriptArgList(
                            final String filepath,
                            final String scriptargs,
                            final String[] scriptargsarr,
                            final String scriptinterpreter,
                            final boolean interpreterargsquoted
                    ) {
                        Assert.assertEquals(scriptFile.getAbsolutePath(), filepath);
                        Assert.assertEquals("", scriptargs);
                        Assert.assertEquals(null, scriptargsarr);
                        Assert.assertEquals(null, scriptinterpreter);
                        Assert.assertEquals(false, interpreterargsquoted);
                        return ExecArgList.builder().arg(filepath, false).build();
                    }
                    public int runLocalCommand(
                            final String osFamily,
                            final ExecArgList execArgList,
                            final Map<String, Map<String, String>> dataContext,
                            final File workingdir,
                            final OutputStream outputStream,
                            final OutputStream errorStream
                    )
                            throws IOException, InterruptedException {
                        Assert.assertNotNull(osFamily);
                        Assert.assertEquals(
                                new ArrayList<String>(Arrays.asList(scriptFile.getAbsolutePath())),
                                execArgList.asFlatStringList()
                        );
                        //temp file containing input script
                        Assert.assertNotNull(dataContext.get("file-copy").get("file"));
                        Assert.assertEquals(null, workingdir);
                        Assert.assertNotNull(outputStream);
                        Assert.assertNotNull(errorStream);

                        //write the test filepath to the output stream
                        outputStream.write((testOutputFilepath+"\n").getBytes());
                        return 0;
                    }

                }
        );
        Map<String, String> stringStringMap = new HashMap<String,String>();
        Map<String, Map<String, String>> testdc = DataContextUtils.addContext("test", stringStringMap, null);
        ExecutionContextImpl context = ExecutionContextImpl.builder()
                                                           .framework(framework)
                                                           .frameworkProject(TestScriptPluginFileCopier.class.getName())
                                                           .dataContext(testdc)
                                                           .executionListener(new testListener())
                                                           .build();
        NodeEntryImpl nodeEntry = new NodeEntryImpl("node1");
        String result = scriptPluginFileCopier.copyFile(context, null, nodeEntry,"/my/path/test");
        Assert.assertEquals(testOutputFilepath,result);
    }

}
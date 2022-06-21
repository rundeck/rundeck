package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl

import com.dtolabs.rundeck.core.common.*
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.data.DataContext
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.utils.Streams
import spock.lang.Specification;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse
import static org.junit.Assert.fail

public class ScriptFileNodeStepExecutorSpec extends Specification {
    private static final String PROJ_NAME = "TestScriptFileNodeStepExecutor";
    public static final String UNIX_FILE_EXT = "sh";
    public static final String WINDOWS_FILE_EXT = "bat";

    def setup() {

        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        final IRundeckProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
                PROJ_NAME);
        AbstractBaseTest.generateProjectResourcesFile(
                new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                frameworkProject
        );


    }

    def teardown() throws Exception {
        File projectdir = new File(getFrameworkProjectsBase(), PROJ_NAME);
        FileUtils.deleteDir(projectdir);
    }
    enum ReasonMock implements FailureReason{
        Test
    }

    static class FileCopierMock implements FileCopier {
        String testResult;
        ExecutionContext testContext;
        InputStream testInput;
        INodeEntry testNode;
        boolean throwException;

        @Override
        public String copyFileStream(
                final ExecutionContext context, final InputStream input, final INodeEntry node, final String destination
        ) throws FileCopierException
        {
            testContext = context;
            testNode = node;
            testInput = input;
            if(throwException) {
                throw new FileCopierException("copyFileStream test", ReasonMock.Test);
            }
            return destination;
        }

        File testFile;
        String testFileContents;

        @Override
        public String copyFile(
                final ExecutionContext context,
                final File file,
                final INodeEntry node,
                final String destination
        )
                throws FileCopierException
        {
            testContext = context;
            testNode = node;
            testFile = file;
            try {
                testFileContents=getContentString(testFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (throwException) {
                throw new FileCopierException("copyFile test", ReasonMock.Test);
            }
            return destination;
        }

        String testScript;

        @Override
        public String copyScriptContent(
                final ExecutionContext context, final String script, final INodeEntry node, final String destination
        ) throws FileCopierException
        {
            testContext = context;
            testNode = node;
            testScript = script;

            if (throwException) {
                throw new FileCopierException("copyScriptContent test", ReasonMock.Test);
            }
            return destination;
        }


    }

    public static class MultiTestNodeExecutorMock implements NodeExecutor {
        List<ExecutionContext> testContext = new ArrayList<ExecutionContext>();
        List<String[]> testCommand = new ArrayList<String[]>();
        List<INodeEntry> testNode = new ArrayList<INodeEntry>();
        List<NodeExecutorResult> testResult = new ArrayList<NodeExecutorResult>();
        int index = 0;

        public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node)  {
            this.testContext.add(context);
            this.testCommand.add(command);
            this.testNode.add(node);
            return testResult.get(index++);
        }

    }

    /**
     * Unix target node will copy using file copier, then exec "chmod +x [destfile]", then execute the
     * filepath, then 'rm [destfile]'
     */
    public void testInterpretCommandScriptContentLocalUnix() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .threadCount(1)
                .build();
        final String testScript = "a script\n";
        final String fileExt= UNIX_FILE_EXT;

        ScriptFileCommand command = new ScriptFileCommandBase()  {
            public String getScript() {
                return testScript;
            }
        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult=nodeExecutorResults;
            testcopier.testResult="/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

        then:
            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNotNull(testScript, testcopier.testFile);
            assertEquals(testScript, testcopier.testFileContents);
            assertFalse(testcopier.testFile.exists());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue(filepath.endsWith("." + UNIX_FILE_EXT));
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(filepath, strings2[0]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));

            //second call is to exec the filepath
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(2));

    }
    /**
     * Inline script content is token-expanded
     */
    public void testInterpretCommandScriptContent_expandTokens() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        test1.getAttributes().put("bingo","fifty");
        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .threadCount(1)
                .dataContext(DataContextUtils.addContext("option",new HashMap<String, String>(){{
                    put("doodad","nifty");
                }},null))
                .build();
        final String testScript = "a script\na line @node.bingo@\nanother @option.doodad@\nagain @option.dne@\n";
        final String expectScript = "a script\na line fifty\nanother nifty\nagain \n";

        ScriptFileCommand command = new ScriptFileCommandBase()  {
            public String getScript() {
                return testScript;
            }
        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));

            testexec.testResult=nodeExecutorResults;
            testcopier.testResult="/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
        then:
            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNotNull(testScript, testcopier.testFile);
            assertEquals(expectScript, testcopier.testFileContents);
            assertFalse(testcopier.testFile.exists());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue(filepath.endsWith("." + UNIX_FILE_EXT));
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));


            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(filepath, strings2[0]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));

            //second call is to exec the filepath
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(2));

    }

    /**
     * Copy to unix target with DOS source line endings, expect unix line endings
     */
    public void testInterpretCommandScriptContentLocalUnix_sourceDosLineendings() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .threadCount(1)
                .build();
        final String testScript = "a script\r\n2 a script\r\n";
        final String expectScript = "a script\n2 a script\n";

        ScriptFileCommand command = new ScriptFileCommandBase()  {
            public String getScript() {
                return testScript;
            }
        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));

            testexec.testResult=nodeExecutorResults;
            testcopier.testResult="/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
        then:
            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNotNull(testcopier.testFile);
            assertEquals(expectScript, testcopier.testFileContents);
            assertFalse(testcopier.testFile.exists());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue(filepath.endsWith("." + UNIX_FILE_EXT));
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(filepath, strings2[0]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));

            //second call is to exec the filepath
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(2));

    }

    private static String getContentString(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fileInputStream = new FileInputStream(file);
        Streams.copyStream(fileInputStream, baos);
        return new String(baos.toByteArray());
    }

    /**
     * Unix target node will copy using file copier, then exec "chmod +x [destfile]", then execute the filepath
     */
    public void testInterpretCommandScriptContentWithArgs() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .build();
        final String testScript = "a script\n";

        ScriptFileCommand command = new ScriptFileCommandBase() {
            public String getScript() {
                return testScript;
            }

            public String[] getArgs() {
                return ["some","args"];
            }
        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
        then:
            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNotNull(testScript, testcopier.testFile);
            assertEquals(testScript, testcopier.testFileContents);
            assertFalse(testcopier.testFile.exists());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue(filepath.endsWith("." + UNIX_FILE_EXT));
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(3, strings2.length);
            assertEquals(filepath, strings2[0]);
            assertEquals("some", strings2[1]);
            assertEquals("args", strings2[2]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));

            //second call is to exec the filepath
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(2));

    }

    /**
     * Unix target node will copy using file copier, then exec "chmod +x [destfile]", then execute the
     * filepath.
     *
     * test result if chmod fails.
     */
    public void testInterpretCommandScriptContentLocalUnixChmodFailure() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .build();
        final String testScript = "a script\n";

        ScriptFileCommand command = new ScriptFileCommandBase() {
            public String getScript() {
                return testScript;
            }

        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createFailure(NodeStepFailureReason.NonZeroResultCode,
                    "failed",
                    null));
            testexec.testResult=nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
        then:
            assertNotNull(interpreterResult);
            assertFalse(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(0));

            assertEquals(context, testcopier.testContext);
            assertNotNull(testScript, testcopier.testFile);
            assertEquals(testScript, testcopier.testFileContents);
            assertFalse(testcopier.testFile.exists());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called once
            assertEquals(1, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue( filepath.endsWith("."+UNIX_FILE_EXT));
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

    }

    /**
     * Windows target node will copy using file copier, then execute the
     * filepath
     */
    public void testInterpretCommandScriptContentLocalWindows() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("windows");

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .build();
        final String testScript = "a script\n";
        final String expectScript = "a script\r\n";

        ScriptFileCommand command = new ScriptFileCommandBase() {
            public String getScript() {
                return testScript;
            }

        };

        when:
        final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
        testexec.testResult=nodeExecutorResults;
        testcopier.testResult = "/test/file/path";
        final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

        then:
        assertNotNull(interpreterResult);
        assertTrue(interpreterResult.isSuccess());
        assertEquals(interpreterResult, nodeExecutorResults.get(0));

        assertEquals(context, testcopier.testContext);
        assertNotNull(testcopier.testFile);
        assertEquals(expectScript, testcopier.testFileContents);
        assertFalse(testcopier.testFile.exists());
        assertEquals(test1, testcopier.testNode);

        //test nodeexecutor was called twice
        assertEquals(2, testexec.index);

        //second call is to exec the filepath
        final String[] strings2 = testexec.testCommand.get(0);
        assertEquals(1, strings2.length);
        String filepath = strings2[0];
        assertNotNull(filepath);
        assertTrue(filepath.endsWith("." + WINDOWS_FILE_EXT));
//            assertEquals(context, testexec.testContext.get(0));
        assertEquals(test1, testexec.testNode.get(0));

        //second call is to exec the filepath
        final String[] strings3 = testexec.testCommand.get(1);
        assertEquals(2, strings3.length);
        assertEquals("del", strings3[0]);
        assertEquals(filepath, strings3[1]);
        assertEquals(test1, testexec.testNode.get(1));
    }

    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileLocal() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .build();
        final File testScriptFile = new File("Testfile");


        ScriptFileCommand command = new ScriptFileCommandBase() {

            public String getServerScriptFilePath() {
                return testScriptFile.getAbsolutePath();
            }

        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
        then:
            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertNull(testcopier.testInput);
            assertEquals(testScriptFile.getAbsolutePath(), testcopier.testFile.getAbsolutePath());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue(filepath.endsWith("." + UNIX_FILE_EXT));
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(strings2[0], filepath);
            assertEquals(filepath, strings2[0]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));
            //second call is to exec the filepath
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
            assertEquals(test1, testexec.testNode.get(2));

    }
    /**
     * Use script file specifier in execution item
     */
    public void testFile_notTokenExpanded() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        test1.getAttributes().put("bingo", "fifty");

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .dataContext(
                        DataContextUtils.addContext(
                                "option", new HashMap<String, String>() {{
                            put("doodad", "nifty");
                        }}, null
                        )
                )
                .build();
        final File testScriptFile = File.createTempFile("Testfile", "tmp");
        testScriptFile.deleteOnExit();

        final String testScript =
                "a script\n" +
                        "a line @node.bingo@\n" +
                        "another @option.doodad@\n" +
                        "again @option.dne@\n";
        final String expectScript = "a script\n" +
                "a line @node.bingo@\n" +
                "another @option.doodad@\n" +
                "again @option.dne@\n";
        //write tofile
        FileOutputStream fileOutputStream = new FileOutputStream(testScriptFile);
        fileOutputStream.write(testScript.getBytes());
        fileOutputStream.close();

        ScriptFileCommand command = new ScriptFileCommandBase() {

            public String getServerScriptFilePath() {
                return testScriptFile.getAbsolutePath();
            }

        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

        then:
            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertNull(testcopier.testInput);
            assertEquals(testScriptFile.getAbsolutePath(), testcopier.testFile.getAbsolutePath());
            assertEquals(expectScript, testcopier.testFileContents);
            assertTrue(testcopier.testFile.exists());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue(filepath.endsWith("." + UNIX_FILE_EXT));
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(filepath, strings2[0]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));
            //second call is to exec the filepath
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
            assertEquals(test1, testexec.testNode.get(2));

    }

    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileInterpreterLocal() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final File testScriptFile = new File("Testfile");
        String[] expectedCommand = ["sudo", "-u", "bob", '${scriptfile}']
        
        then:
        testExecute(expectedCommand, testScriptFile, interpreter, null, false, null,
                null
        );
    }

    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileInterpreterWithArgs() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final String[] args = ["arg1", "arg2"];
        final File testScriptFile = new File("Testfile");
        String[] expected = ["sudo", "-u", "bob", '${scriptfile}', "arg1", "arg2"];
        
        then:
        testExecute(expected, testScriptFile, interpreter, args, false, null, null);
    }

    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFile_withExtension() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final String[] args = ["arg1", "arg2"];
        final File testScriptFile = new File("Testfile");
        String[] expected = ["sudo", "-u", "bob", '${scriptfile}', "arg1", "arg2"]
        
        then:
        testExecute(expected, testScriptFile, interpreter, args, false, null, "myext");
    }
    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileInterpreterWithArgsExpandBasicOption() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final String[] args = ["arg1", "arg2", '${option.opt1}'];
        final File testScriptFile = new File("Testfile");
        String[] expected = ["sudo", "-u", "bob", '${scriptfile}', "arg1", "arg2", "somevalue"];
        HashMap<String, String> options = new HashMap<String, String>() {{
            put("opt1","somevalue");
            put("opt2","other value");
        }};
        Map<String, Map<String, String>> dataContext = DataContextUtils.addContext("option", options, null);
        
        then:
        testExecute(expected, testScriptFile, interpreter, args, false, dataContext, null);
    }
    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileInterpreterWithArgsExpandQuotedOption() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final String[] args = ["arg1", "arg2", '${option.opt2}'];
        final File testScriptFile = new File("Testfile");
        HashMap<String, String> options = new HashMap<String, String>() {{
            put("opt1","somevalue");
            put("opt2","other value");
        }};
        Map<String, Map<String, String>> dataContext = DataContextUtils.addContext("option", options, null);
        String[] expected = ["sudo", "-u", "bob", '${scriptfile}', "arg1", "arg2", "'other value'"]
        
        then:
        testExecute(expected, testScriptFile, interpreter, args, false, dataContext, null);
    }
    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileInterpreterWithArgsSpace() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final String[] args = ["arg1 arg2"]
        final File testScriptFile = new File("Testfile");
        String[] expected = ["sudo", "-u", "bob", '${scriptfile}', "'arg1 arg2'"];
        
        then:
        testExecute(expected, testScriptFile, interpreter, args, false, null, null);
    }

    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileInterpreterQuotedLocal() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final File testScriptFile = new File("Testfile");
        String[] expected = ["sudo", "-u", "bob", '${scriptfile}']
        
        then:
        testExecute(expected, testScriptFile, interpreter, null, true, null,
                null
        );
    }
    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileInterpreterQuotedWithArgs() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final File testScriptFile = new File("Testfile");
        String[] args = ["arg1", "arg2"]
        String[] expected = ["sudo", "-u", "bob", "'\${scriptfile} arg1 arg2'"]
        then:
        testExecute(expected, testScriptFile, interpreter, args, true, null,
                null
        );
    }

    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileInterpreterQuotedWithArgsWithSpace() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final File testScriptFile = new File("Testfile");
        String[] args = ["arg1 arg2"];
        String[] expected = ["sudo", "-u", "bob", "'\${scriptfile} '\"'\"'arg1 arg2'\"'\"''"]
        then:
        testExecute(expected, testScriptFile,
                interpreter, args, true, null, null
        );
    }

    public void testInterpretCommandScriptFileInterpreterQuotedOptionArgsWithoutSpace() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final File testScriptFile = new File("Testfile");
        String[] args = ["arg1 arg2", '${option.opt1}']
        HashMap<String, String> options = new HashMap<String, String>() {{
            put("opt1", "somevalue");
            put("opt2", "other value");
        }};
        Map<String, Map<String, String>> dataContext = DataContextUtils.addContext("option", options, null)
        String[] expected = ["sudo", "-u", "bob", "'\${scriptfile} '\"'\"'arg1 arg2'\"'\"' somevalue'"]
        then:
        testExecute(expected, testScriptFile,
                interpreter, args, true, dataContext,
                null
        );
    }


    public void testInterpretCommandScriptFileInterpreterQuotedOptionArgsWithSpace() throws Exception {
        when:
        final String interpreter = "sudo -u bob";
        final File testScriptFile = new File("Testfile");
        String[] args = ["arg1 arg2", '${option.opt2}'];
        HashMap<String, String> options = new HashMap<String, String>() {{
            put("opt1", "somevalue");
            put("opt2", "other value");
        }};
        Map<String, Map<String, String>> dataContext = DataContextUtils.addContext("option", options, null);
        String[] expected = ["sudo", "-u", "bob", "'\${scriptfile} '\"'\"'arg1 arg2'\"'\"' '\"'\"'other " +
                "value'\"'\"''"]
        
        then:
        testExecute(expected, testScriptFile,
                interpreter, args, true, dataContext, null
        );
    }


    private void testExecute(
            String[] expectedCommand,
            final File testScriptFile,
            final String scriptInterpreter,
            final String[] args,
            final boolean argsQuoted,
            Map<String, Map<String, String>> dataContext,
            final String fileExtension
    ) throws NodeStepException {
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");

        ExecutionContextImpl.Builder builder = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah");
        if(dataContext) {
            builder.dataContext(dataContext);
        }
        final StepExecutionContext context = builder
                .build();

        ScriptFileCommand command = new ScriptFileCommandBase() {

            public String getServerScriptFilePath() {
                return testScriptFile.getAbsolutePath();
            }

            @Override
            public String getScriptInterpreter() {
                return scriptInterpreter;
            }

            @Override
            public String[] getArgs() {
                return args;
            }

            @Override
            public String getFileExtension() {
                return fileExtension;
            }

            @Override
            public boolean getInterpreterArgsQuoted() {
                return argsQuoted;
            }
        };
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertNull(testcopier.testInput);
            assertEquals(testScriptFile.getAbsolutePath(), testcopier.testFile.getAbsolutePath());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            final String filepath=strings[2];
            if(null!=fileExtension) {
                assertTrue(filepath.endsWith("." + fileExtension));
            }else {
                assertTrue(filepath.endsWith(
                        "." +
                                (test1.getOsFamily().equals("windows")
                                        ? WINDOWS_FILE_EXT
                                        : UNIX_FILE_EXT)
                )
                );
            }
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);


//            assertEquals(context, testexec.testContext.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            //replace ${scriptfile} with actual filepath
            for (int i = 0; i < expectedCommand.length; i++) {
                String s = expectedCommand[i];
                if(s.contains('${scriptfile}')) {
                    expectedCommand[i]=s.replaceAll(Pattern.quote('${scriptfile}'), Matcher.quoteReplacement(filepath));
                }
            }
            assertArrayEquals(expectedCommand, strings2);
            assertEquals(test1, testexec.testNode.get(1));

            //third call to remove script
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(2));


    }

    private void assertArrayEquals(String[] expected2, String[] strings2) {
        assertEquals(expected2.length, strings2.length);
        assertEquals(Arrays.asList(expected2), Arrays.asList(strings2));
    }

    /**
     * Test inputstream
     */
    public void testInterpretCommandScriptInputLocal() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .build();
        final InputStream inputStream = new ByteArrayInputStream("a test script\n".getBytes());


        ScriptFileCommand command = new ScriptFileCommandBase() {

            public InputStream getScriptAsStream() {
                return inputStream;
            }

        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

        then:
            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertNotNull(testcopier.testFile);
            assertEquals("a test script\n", testcopier.testFileContents);
            assertFalse(testcopier.testFile.exists());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue(filepath.endsWith("."+UNIX_FILE_EXT));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(strings2[0], filepath);
            assertEquals(filepath, strings2[0]);
            assertEquals(test1, testexec.testNode.get(1));
            //first call is chmod +x filepath
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
            assertEquals(test1, testexec.testNode.get(2));

    }
    /**
     * Test inputstream
     */
    public void testInputStream_expandTokens() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        test1.getAttributes().put("bingo", "fifty");

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .dataContext(
                        DataContextUtils.addContext(
                                "option",
                                new HashMap<String, String>() {{
                                    put("doodad", "nifty");
                                }},
                                null
                        )
                )
                .build();

        final String testScript =
                "a script\na line @node.bingo@\nanother @option.doodad@\nagain @option.dne@\n";
        final String expectScript = "a script\na line fifty\nanother nifty\nagain \n";
        final InputStream inputStream = new ByteArrayInputStream(testScript.getBytes());


        ScriptFileCommand command = new ScriptFileCommandBase() {

            public InputStream getScriptAsStream() {
                return inputStream;
            }

        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

        then:
            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertNotNull(testcopier.testFile);
            assertEquals(expectScript, testcopier.testFileContents);
            assertFalse(testcopier.testFile.exists());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue(filepath.endsWith("." + UNIX_FILE_EXT));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(filepath, strings2[0]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
            assertEquals(test1, testexec.testNode.get(2));

    }

    public void testInterpretCommandRmFileFails() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .build();
        final InputStream inputStream = new ByteArrayInputStream("a test script\n".getBytes());


        ScriptFileCommand command = new ScriptFileCommandBase() {

            public InputStream getScriptAsStream() {
                return inputStream;
            }

        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createFailure(null, "failed", test1));
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

        then:
            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertNotNull(testcopier.testFile);
            assertEquals("a test script\n",testcopier.testFileContents);
            assertFalse( testcopier.testFile.exists());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue(filepath.endsWith("."+UNIX_FILE_EXT));
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(filepath, strings2[0]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));
            //first call is chmod +x filepath
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(2));

    }


    public void testInterpretCommandCopyFailure() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .build();
        final InputStream inputStream = new ByteArrayInputStream("".bytes);


        ScriptFileCommand command = new ScriptFileCommandBase() {

            public InputStream getScriptAsStream() {
                return inputStream;
            }

        };
        when:
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult = nodeExecutorResults;

            //set filecopier to throw exception
            testcopier.throwException=true;
            testcopier.testResult = "/test/file/path";
            final NodeStepResult interpreterResult;
        then:
            try {
                interpreterResult = interpret.executeNodeStep(context, command, test1);
                fail("interpreter should have thrown exception");
            } catch (NodeStepException e) {
                assertTrue(e.getCause() instanceof FileCopierException);
                assertEquals("copyFile test", e.getCause().getMessage());
            }

    }

    /**
     * Windows target node will copy using file copier and read the addBom property  from node, then execute the
     * filepath
     */
    public void testInterpretCommandScriptContentLocalWindowsUsingBom() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("windows");
        test1.getAttributes().put(ScriptfileUtils.SHOULD_ADD_BOM, "true");

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .build();
        final String testScript = "a script\n";
        String expectScript = "a script\r\n";
        if("UTF-8".equals(Charset.defaultCharset().name())){
            expectScript = "\ufeffa script\r\n";
        }

        ScriptFileCommand command = new ScriptFileCommandBase() {
            public String getScript() {
                return testScript;
            }

        };

        when:
        final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
        testexec.testResult=nodeExecutorResults;
        testcopier.testResult = "/test/file/path";
        final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

        then:
        assertNotNull(interpreterResult);
        assertTrue(interpreterResult.isSuccess());
        assertEquals(interpreterResult, nodeExecutorResults.get(0));

        assertEquals(context, testcopier.testContext);
        assertNotNull(testcopier.testFile);
        assertEquals(expectScript, testcopier.testFileContents);
        assertFalse(testcopier.testFile.exists());
        assertEquals(test1, testcopier.testNode);

        //test nodeexecutor was called twice
        assertEquals(2, testexec.index);

        //second call is to exec the filepath
        final String[] strings2 = testexec.testCommand.get(0);
        assertEquals(1, strings2.length);
        String filepath = strings2[0];
        assertNotNull(filepath);
        assertTrue(filepath.endsWith("." + WINDOWS_FILE_EXT));
//            assertEquals(context, testexec.testContext.get(0));
        assertEquals(test1, testexec.testNode.get(0));

        //second call is to exec the filepath
        final String[] strings3 = testexec.testCommand.get(1);
        assertEquals(2, strings3.length);
        assertEquals("del", strings3[0]);
        assertEquals(filepath, strings3[1]);
        assertEquals(test1, testexec.testNode.get(1));
    }

    /**
     * Use script file specifier in execution item and replaces tokens in file path
     */
    def 'testInterpretCommandScriptFileLocal with replace token in file path'() throws Exception {
        given:
        final Framework frameworkInstance = AbstractBaseTest.createTestFramework();
        ScriptFileNodeStepExecutor interpret = new ScriptFileNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        MultiTestNodeExecutorMock testexec = new MultiTestNodeExecutorMock();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(frameworkInstance);
        service.registerInstance("local", testexec);

        FileCopierMock testcopier = new FileCopierMock();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(frameworkInstance);
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");

        def sharedctx = SharedDataContextUtils.sharedContext()
        sharedctx.merge(ContextView.global(), DataContextUtils.context('data', [fileName: 'Testfile']))

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .sharedDataContext(sharedctx)
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .threadCount(1)
                .build()
        final File testScriptFile = new File("Testfile");


        ScriptFileCommand command = new ScriptFileCommandBase() {

            public String getServerScriptFilePath() {
                return testScriptFile.getAbsolutePath().replace('Testfile', '${data.fileName}')
            }

        };
        when:
        final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
        testexec.testResult = nodeExecutorResults;
        testcopier.testResult = "/test/file/path";
        final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
        then:
        assertNotNull(interpreterResult);
        assertTrue(interpreterResult.isSuccess());
        assertEquals(interpreterResult, nodeExecutorResults.get(1));

        assertEquals(context, testcopier.testContext);
        assertNull(testcopier.testScript);
        assertNull(testcopier.testInput);
        assertEquals(testScriptFile.getAbsolutePath(), testcopier.testFile.getAbsolutePath());
        assertEquals(test1, testcopier.testNode);

        //test nodeexecutor was called twice
        assertEquals(3, testexec.index);
        //first call is chmod +x filepath
        final String[] strings = testexec.testCommand.get(0);
        assertEquals(3, strings.length);
        assertEquals("chmod", strings[0]);
        assertEquals("+x", strings[1]);
        String filepath = strings[2];
        assertTrue(filepath.endsWith("." + UNIX_FILE_EXT));
        assertEquals(test1, testexec.testNode.get(0));

        //second call is to exec the filepath
        final String[] strings2 = testexec.testCommand.get(1);
        assertEquals(1, strings2.length);
        assertEquals(strings2[0], filepath);
        assertEquals(filepath, strings2[0]);
        assertEquals(test1, testexec.testNode.get(1));
        //second call is to exec the filepath
        final String[] strings3 = testexec.testCommand.get(2);
        assertEquals(3, strings3.length);
        assertEquals("rm", strings3[0]);
        assertEquals("-f", strings3[1]);
        assertEquals(filepath, strings3[2]);
        assertEquals(test1, testexec.testNode.get(2));

    }

}


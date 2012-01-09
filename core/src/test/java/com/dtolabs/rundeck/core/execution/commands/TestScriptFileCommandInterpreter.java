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
* TestScriptFileCommandInterpreter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 3:37 PM
* 
*/
package com.dtolabs.rundeck.core.execution.commands;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * TestScriptFileCommandInterpreter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestScriptFileCommandInterpreter extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestScriptFileCommandInterpreter";

    public TestScriptFileCommandInterpreter(String name) {
        super(name);
    }

    public void setUp() {

        final Framework frameworkInstance = getFrameworkInstance();
        final FrameworkProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
            PROJ_NAME);
        File resourcesfile = new File(frameworkProject.getNodesResourceFilePath());
        //copy test nodes to resources file
        try {
            FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                resourcesfile);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }

    }

    public void tearDown() throws Exception {
        super.tearDown();
        File projectdir = new File(getFrameworkProjectsBase(), PROJ_NAME);
        FileUtils.deleteDir(projectdir);
    }

    public static class testFileCopier implements FileCopier {
        String testResult;
        ExecutionContext testContext;
        InputStream testInput;
        INodeEntry testNode;
        boolean throwException;

        public String copyFileStream(ExecutionContext context, InputStream input, INodeEntry node) throws
            FileCopierException {
            testContext = context;
            testNode = node;
            testInput = input;
            if(throwException) {
                throw new FileCopierException("copyFileStream test");
            }
            return testResult;
        }

        File testFile;

        public String copyFile(ExecutionContext context, File file, INodeEntry node) throws FileCopierException {
            testContext = context;
            testNode = node;
            testFile = file;
            if (throwException) {
                throw new FileCopierException("copyFile test");
            }
            return testResult;
        }

        String testScript;

        public String copyScriptContent(ExecutionContext context, String script, INodeEntry node) throws
            FileCopierException {
            testContext = context;
            testNode = node;
            testScript = script;

            if (throwException) {
                throw new FileCopierException("copyScriptContent test");
            }
            return testResult;
        }
    }

    public static class multiTestNodeExecutor implements NodeExecutor {
        List<ExecutionContext> testContext = new ArrayList<ExecutionContext>();
        List<String[]> testCommand = new ArrayList<String[]>();
        List<INodeEntry> testNode = new ArrayList<INodeEntry>();
        List<NodeExecutorResult> testResult = new ArrayList<NodeExecutorResult>();
        int index = 0;

        public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) throws
            ExecutionException {
            this.testContext.add(context);
            this.testCommand.add(command);
            this.testNode.add(node);
            return testResult.get(index++);
        }

    }

    /**
     * Unix target node will copy using file copier, then exec "chmod +x [destfile]", then execute the
     * filepath
     */
    public void testInterpretCommandScriptContentLocalUnix() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ScriptFileCommandInterpreter interpret = new ScriptFileCommandInterpreter(getFrameworkInstance());

        //setup nodeexecutor for local node
        multiTestNodeExecutor testexec = new multiTestNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        testFileCopier testcopier = new testFileCopier();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(getFrameworkInstance());
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        final ExecutionContext context = new ExecutionContext() {
            public String getFrameworkProject() {
                return PROJ_NAME;
            }

            public Framework getFramework() {
                return frameworkInstance;
            }

            public String getUser() {
                return "blah";
            }

            public NodeSet getNodeSelector() {

                return null;
            }

            public int getThreadCount() {
                return 1;
            }

            public String getNodeRankAttribute() {
                return null;
            }

            public boolean isNodeRankOrderAscending() {
                return false;
            }

            public boolean isKeepgoing() {
                return false;
            }
            public String[] getArgs() {
                return new String[0];
            }

            public int getLoglevel() {
                return 0;
            }

            public Map<String, Map<String, String>> getDataContext() {
                return null;
            }

            public Map<String, Map<String, String>> getPrivateDataContext() {
                return null;
            }

            public ExecutionListener getExecutionListener() {
                return null;
            }

            public File getNodesFile() {
                return null;
            }
        };
        final String testScript = "a script";

        ScriptFileCommand command = new ScriptFileCommand() {
            public String getScript() {
                return testScript;
            }

            public InputStream getScriptAsStream() {
                return null;
            }

            public String getServerScriptFilePath() {
                return null;
            }

            public String[] getArgs() {
                return new String[0];
            }
        };
        {
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 1;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 2;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            testexec.testResult=nodeExecutorResults;
            testcopier.testResult="/test/file/path";
            final InterpreterResult interpreterResult = interpret.interpretCommand(context, command, test1);

            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertEquals(testScript, testcopier.testScript);
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(2, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            assertEquals("/test/file/path", strings[2]);
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(strings2[0], strings[2]);
            assertEquals("/test/file/path", strings2[0]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));
        }
    }

    /**
     * Unix target node will copy using file copier, then exec "chmod +x [destfile]", then execute the filepath
     */
    public void testInterpretCommandScriptContentWithArgs() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ScriptFileCommandInterpreter interpret = new ScriptFileCommandInterpreter(getFrameworkInstance());

        //setup nodeexecutor for local node
        multiTestNodeExecutor testexec = new multiTestNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        testFileCopier testcopier = new testFileCopier();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(getFrameworkInstance());
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        final ExecutionContext context = new ExecutionContext() {
            public String getFrameworkProject() {
                return PROJ_NAME;
            }

            public Framework getFramework() {
                return frameworkInstance;
            }

            public String getUser() {
                return "blah";
            }

            public NodeSet getNodeSelector() {

                return null;
            }

            public int getThreadCount() {
                return 1;
            }

            public String getNodeRankAttribute() {
                return null;
            }

            public boolean isNodeRankOrderAscending() {
                return false;
            }

            public boolean isKeepgoing() {
                return false;
            }
            public String[] getArgs() {
                return new String[0];
            }

            public int getLoglevel() {
                return 0;
            }

            public Map<String, Map<String, String>> getDataContext() {
                return null;
            }

            public Map<String, Map<String, String>> getPrivateDataContext() {
                return null;
            }

            public ExecutionListener getExecutionListener() {
                return null;
            }

            public File getNodesFile() {
                return null;
            }
        };
        final String testScript = "a script";

        ScriptFileCommand command = new ScriptFileCommand() {
            public String getScript() {
                return testScript;
            }

            public InputStream getScriptAsStream() {
                return null;
            }

            public String getServerScriptFilePath() {
                return null;
            }

            public String[] getArgs() {
                return new String[]{"some","args"};
            }
        };
        {
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 1;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 2;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final InterpreterResult interpreterResult = interpret.interpretCommand(context, command, test1);

            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertEquals(testScript, testcopier.testScript);
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(2, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            assertEquals("/test/file/path", strings[2]);
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(3, strings2.length);
            assertEquals(strings2[0], strings[2]);
            assertEquals("/test/file/path", strings2[0]);
            assertEquals("some", strings2[1]);
            assertEquals("args", strings2[2]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));
        }
    }

    /**
     * Unix target node will copy using file copier, then exec "chmod +x [destfile]", then execute the
     * filepath.
     *
     * test result if chmod fails.
     */
    public void testInterpretCommandScriptContentLocalUnixChmodFailure() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ScriptFileCommandInterpreter interpret = new ScriptFileCommandInterpreter(frameworkInstance);

        //setup nodeexecutor for local node
        multiTestNodeExecutor testexec = new multiTestNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        testFileCopier testcopier = new testFileCopier();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(getFrameworkInstance());
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        final ExecutionContext context = new ExecutionContext() {
            public String getFrameworkProject() {
                return PROJ_NAME;
            }

            public Framework getFramework() {
                return frameworkInstance;
            }


            public String getUser() {
                return "blah";
            }

            public NodeSet getNodeSelector() {

                return null;
            }

            public int getThreadCount() {
                return 1;
            }

            public String getNodeRankAttribute() {
                return null;
            }

            public boolean isNodeRankOrderAscending() {
                return false;
            }

            public boolean isKeepgoing() {
                return false;
            }

            public String[] getArgs() {
                return new String[0];
            }

            public int getLoglevel() {
                return 0;
            }

            public Map<String, Map<String, String>> getDataContext() {
                return null;
            }

            public Map<String, Map<String, String>> getPrivateDataContext() {
                return null;
            }

            public ExecutionListener getExecutionListener() {
                return null;
            }

            public File getNodesFile() {
                return null;
            }
        };
        final String testScript = "a script";

        ScriptFileCommand command = new ScriptFileCommand() {
            public String getScript() {
                return testScript;
            }

            public InputStream getScriptAsStream() {
                return null;
            }

            public String getServerScriptFilePath() {
                return null;
            }

            public String[] getArgs() {
                return new String[0];
            }
        };
        {
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 1;
                }

                public boolean isSuccess() {
                    return false;
                }
            });
            testexec.testResult=nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final InterpreterResult interpreterResult = interpret.interpretCommand(context, command, test1);

            assertNotNull(interpreterResult);
            assertFalse(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(0));

            assertEquals(context, testcopier.testContext);
            assertEquals(testScript, testcopier.testScript);
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called once
            assertEquals(1, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            assertEquals("/test/file/path", strings[2]);
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));


        }
    }

    /**
     * Windows target node will copy using file copier, then execute the
     * filepath
     */
    public void testInterpretCommandScriptContentLocalWindows() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ScriptFileCommandInterpreter interpret = new ScriptFileCommandInterpreter(frameworkInstance);

        //setup nodeexecutor for local node
        multiTestNodeExecutor testexec = new multiTestNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        testFileCopier testcopier = new testFileCopier();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(getFrameworkInstance());
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("windows");
        final ExecutionContext context = new ExecutionContext() {
            public String getFrameworkProject() {
                return PROJ_NAME;
            }

            public Framework getFramework() {
                return frameworkInstance;
            }

            public String getUser() {
                return "blah";
            }

            public NodeSet getNodeSelector() {

                return null;
            }

            public int getThreadCount() {
                return 1;
            }

            public String getNodeRankAttribute() {
                return null;
            }

            public boolean isNodeRankOrderAscending() {
                return false;
            }

            public boolean isKeepgoing() {
                return false;
            }
            public String[] getArgs() {
                return new String[0];
            }

            public int getLoglevel() {
                return 0;
            }

            public Map<String, Map<String, String>> getDataContext() {
                return null;
            }

            public Map<String, Map<String, String>> getPrivateDataContext() {
                return null;
            }

            public ExecutionListener getExecutionListener() {
                return null;
            }


            public File getNodesFile() {
                return null;
            }
        };
        final String testScript = "a script";

        ScriptFileCommand command = new ScriptFileCommand() {
            public String getScript() {
                return testScript;
            }

            public InputStream getScriptAsStream() {
                return null;
            }

            public String getServerScriptFilePath() {
                return null;
            }

            public String[] getArgs() {
                return new String[0];
            }
        };
        {
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 1;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            testexec.testResult=nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final InterpreterResult interpreterResult = interpret.interpretCommand(context, command, test1);

            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(0));

            assertEquals(context, testcopier.testContext);
            assertEquals(testScript, testcopier.testScript);
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(1, testexec.index);

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(0);
            assertEquals(1, strings2.length);
            assertNotNull(strings2[0]);
            assertEquals("/test/file/path", strings2[0]);
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));
        }
    }

    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileLocal() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ScriptFileCommandInterpreter interpret = new ScriptFileCommandInterpreter(frameworkInstance);

        //setup nodeexecutor for local node
        multiTestNodeExecutor testexec = new multiTestNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        testFileCopier testcopier = new testFileCopier();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(getFrameworkInstance());
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        final ExecutionContext context = new ExecutionContext() {
            public String getFrameworkProject() {
                return PROJ_NAME;
            }

            public Framework getFramework() {
                return frameworkInstance;
            }


            public String getUser() {
                return "blah";
            }

            public NodeSet getNodeSelector() {

                return null;
            }

            public int getThreadCount() {
                return 1;
            }

            public String getNodeRankAttribute() {
                return null;
            }

            public boolean isNodeRankOrderAscending() {
                return false;
            }

            public boolean isKeepgoing() {
                return false;
            }

            public String[] getArgs() {
                return new String[0];
            }

            public int getLoglevel() {
                return 0;
            }

            public Map<String, Map<String, String>> getDataContext() {
                return null;
            }

            public Map<String, Map<String, String>> getPrivateDataContext() {
                return null;
            }

            public ExecutionListener getExecutionListener() {
                return null;
            }

            public File getNodesFile() {
                return null;
            }
        };
        final File testScriptFile = new File("Testfile");


        ScriptFileCommand command = new ScriptFileCommand() {
            public String getScript() {
                return null;
            }

            public InputStream getScriptAsStream() {
                return null;
            }

            public String getServerScriptFilePath() {
                return testScriptFile.getAbsolutePath();
            }

            public String[] getArgs() {
                return new String[0];
            }
        };
        {
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 1;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 2;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final InterpreterResult interpreterResult = interpret.interpretCommand(context, command, test1);

            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertNull(testcopier.testInput);
            assertEquals(testScriptFile.getAbsolutePath(), testcopier.testFile.getAbsolutePath());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(2, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            assertEquals("/test/file/path", strings[2]);
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(strings2[0], strings[2]);
            assertEquals("/test/file/path", strings2[0]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));
        }
    }

    /**
     * Test inputstream
     */
    public void testInterpretCommandScriptInputLocal() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ScriptFileCommandInterpreter interpret = new ScriptFileCommandInterpreter(frameworkInstance);

        //setup nodeexecutor for local node
        multiTestNodeExecutor testexec = new multiTestNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        testFileCopier testcopier = new testFileCopier();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(getFrameworkInstance());
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        final ExecutionContext context = new ExecutionContext() {
            public String getFrameworkProject() {
                return PROJ_NAME;
            }

            public Framework getFramework() {
                return frameworkInstance;
            }


            public String getUser() {
                return "blah";
            }

            public NodeSet getNodeSelector() {

                return null;
            }

            public int getThreadCount() {
                return 1;
            }

            public String getNodeRankAttribute() {
                return null;
            }

            public boolean isNodeRankOrderAscending() {
                return false;
            }

            public boolean isKeepgoing() {
                return false;
            }

            public String[] getArgs() {
                return new String[0];
            }

            public int getLoglevel() {
                return 0;
            }

            public Map<String, Map<String, String>> getDataContext() {
                return null;
            }

            public Map<String, Map<String, String>> getPrivateDataContext() {
                return null;
            }

            public ExecutionListener getExecutionListener() {
                return null;
            }

            public File getNodesFile() {
                return null;
            }
        };
        final InputStream inputStream = new ByteArrayInputStream(new byte[]{0});


        ScriptFileCommand command = new ScriptFileCommand() {
            public String getScript() {
                return null;
            }

            public InputStream getScriptAsStream() {
                return inputStream;
            }

            public String getServerScriptFilePath() {
                return null;
            }

            public String[] getArgs() {
                return new String[0];
            }
        };
        {
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 1;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 2;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final InterpreterResult interpreterResult = interpret.interpretCommand(context, command, test1);

            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertEquals(inputStream, testcopier.testInput);
            assertNull(testcopier.testFile);
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(2, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            assertEquals("/test/file/path", strings[2]);
//            assertEquals(context, testexec.testContext.get(0));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(strings2[0], strings[2]);
            assertEquals("/test/file/path", strings2[0]);
//            assertEquals(context, testexec.testContext.get(1));
            assertEquals(test1, testexec.testNode.get(1));
        }
    }


    public void testInterpretCommandCopyFailure() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ScriptFileCommandInterpreter interpret = new ScriptFileCommandInterpreter(frameworkInstance);

        //setup nodeexecutor for local node
        multiTestNodeExecutor testexec = new multiTestNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        testFileCopier testcopier = new testFileCopier();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(getFrameworkInstance());
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");
        final ExecutionContext context = new ExecutionContext() {
            public String getFrameworkProject() {
                return PROJ_NAME;
            }

            public Framework getFramework() {
                return frameworkInstance;
            }


            public String getUser() {
                return "blah";
            }

            public NodeSet getNodeSelector() {

                return null;
            }

            public int getThreadCount() {
                return 1;
            }

            public String getNodeRankAttribute() {
                return null;
            }

            public boolean isNodeRankOrderAscending() {
                return false;
            }

            public boolean isKeepgoing() {
                return false;
            }

            public String[] getArgs() {
                return new String[0];
            }

            public int getLoglevel() {
                return 0;
            }

            public Map<String, Map<String, String>> getDataContext() {
                return null;
            }

            public Map<String, Map<String, String>> getPrivateDataContext() {
                return null;
            }

            public ExecutionListener getExecutionListener() {
                return null;
            }

            public File getNodesFile() {
                return null;
            }
        };
        final InputStream inputStream = new ByteArrayInputStream(new byte[]{0});


        ScriptFileCommand command = new ScriptFileCommand() {
            public String getScript() {
                return null;
            }

            public InputStream getScriptAsStream() {
                return inputStream;
            }

            public String getServerScriptFilePath() {
                return null;
            }

            public String[] getArgs() {
                return new String[0];
            }
        };
        {
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 1;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            nodeExecutorResults.add(new NodeExecutorResult() {
                public int getResultCode() {
                    return 2;
                }

                public boolean isSuccess() {
                    return true;
                }
            });
            testexec.testResult = nodeExecutorResults;

            //set filecopier to throw exception
            testcopier.throwException=true;
            testcopier.testResult = "/test/file/path";
            final InterpreterResult interpreterResult;
            try {
                interpreterResult = interpret.interpretCommand(context, command, test1);
                fail("interpreter should have thrown exception");
            } catch (InterpreterException e) {
                assertTrue(e.getCause() instanceof FileCopierException);
                assertEquals("copyFileStream test", e.getCause().getMessage());
            }
        }
    }

}

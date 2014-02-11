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

package com.dtolabs.client.services;
/*
* JobDefinitionSerializerTest.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 18, 2010 4:24:37 PM
* $Id$
*/

import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.utils.NodeSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class JobDefinitionSerializerTest extends TestCase {
    JobDefinitionSerializer jobDefinitionSerializer;
    private static final String TEST_PROJECT = "Depot";
    private static final String TEST_OBJ = "name";
    private static final String TEST_TYPE = "Type";
    private static final String TEST_COMMAND = "command";
    private static final String TEST_USERNAME = "username";

    public JobDefinitionSerializerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(JobDefinitionSerializerTest.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
    class testScript implements IDispatchedScript{
        public Map<String, String> getOptions() {
            return null;
        }

        @Override
        public Boolean getNodeExcludePrecedence() {
            return true;
        }

        @Override
        public int getNodeThreadcount() {
            return -1;
        }

        @Override
        public Boolean isKeepgoing() {
            return false;
        }

        @Override
        public String getNodeFilter() {
            return null;
        }

        public String getFrameworkProject() {
            return null;
        }

        public boolean hasScript() {
            return false;
        }

        public String getScript() {
            return null;
        }

        public InputStream getScriptAsStream() {
            return null;
        }

        public String getServerScriptFilePath() {
            return null;
        }

        public String getScriptURLString() {
            return null;
        }

        public String[] getArgs() {
            return null;
        }

        public int getLoglevel() {
            return 0;
        }

        public Map<String, Map<String, String>> getDataContext() {
            return null;
        }

    }
    public void testSerializeScript() throws Exception{
        /**
         * project property is null
         */
        final IDispatchedScript simpleCommand_invalid1 = new testScript() {
            public String[] getArgs() {
                return new String[]{"some","shell","command"};
            }
        };

        {
            //test invalid input: no project
            try {
                final Document document = JobDefinitionSerializer.serialize(simpleCommand_invalid1);
                fail("should not have succeeded");
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
                assertEquals("No project is specified", e.getMessage());
            }

        }
        /**
         * Has no script/command/scriptpath
         */
        final IDispatchedScript simpleCommand_invalid2 = new testScript() {
            public String getFrameworkProject() {
                return TEST_PROJECT;
            }

            public String[] getArgs() {
                return new String[0];
            }
        };

        {
            //test invalid input: no command/script/scriptpath
            try {
                final Document document = JobDefinitionSerializer.serialize(simpleCommand_invalid2);
                fail("should not have succeeded");
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
                assertEquals("Dispatched script did not specify a command, script or filepath", e.getMessage());
            }

        }
        /**
         * Basic shell command 
         */
        final IDispatchedScript simpleCommand1 = new testScript() {

            public String getFrameworkProject() {
                return TEST_PROJECT;
            }

            public String[] getArgs() {
                return new String[]{"some","shell","command"};
            }

        };

        {
            //test basic components

            final Document document = JobDefinitionSerializer.serialize(simpleCommand1);
//            p(document);

            assertNotNull(document);
            assertEquals("incorrect root element", "joblist", document.getRootElement().getName());
            final List jobs = document.selectNodes("/joblist/job");
            assertNotNull("missing /joblist/job element", jobs);
            assertEquals("wrong size for /joblist/job", 1, jobs.size());

            assertNotNull("expected /joblist/job/name", document.selectSingleNode("/joblist/job/name"));
            assertEquals("dispatch commandline job", document.selectSingleNode("/joblist/job/name").getStringValue());
            assertNotNull("expected /joblist/job/description", document.selectSingleNode("/joblist/job/description"));
            assertEquals("dispatch commandline job", document.selectSingleNode("/joblist/job/description").getStringValue());
            assertNotNull("expected /joblist/job/loglevel", document.selectSingleNode("/joblist/job/loglevel"));
            assertEquals("ERROR", document.selectSingleNode("/joblist/job/loglevel").getStringValue());

            assertEquals(1, document.selectNodes("/joblist/job/context").size());
            assertNotNull(document.selectSingleNode("/joblist/job/context"));
            assertNotNull(document.selectSingleNode("/joblist/job/context/project"));
            assertEquals(TEST_PROJECT, document.selectSingleNode("/joblist/job/context/project").getStringValue());
            assertNull(document.selectSingleNode("/joblist/job/context/type"));
            assertNull(document.selectSingleNode("/joblist/job/context/object"));
            assertNull(document.selectSingleNode("/joblist/job/context/command"));
            assertNotNull(document.selectSingleNode("/joblist/job/dispatch"));
            assertNotNull(document.selectSingleNode("/joblist/job/dispatch/threadcount"));
            assertEquals("1",document.selectSingleNode("/joblist/job/dispatch/threadcount").getText());
            assertNotNull(document.selectSingleNode("/joblist/job/dispatch/keepgoing"));
            assertEquals("false", document.selectSingleNode("/joblist/job/dispatch/keepgoing").getText());
        }
        /**
         * Basic shell command, set threadcount=3, keepgoing=true
         */
        final IDispatchedScript simpleCommand2 = new testScript() {
            private NodeSet nset;

            @Override
            public int getNodeThreadcount() {
                return 3;
            }

            @Override
            public Boolean isKeepgoing() {
                return true;
            }

            public String getFrameworkProject() {
                return TEST_PROJECT;
            }
            public String[] getArgs() {
                return new String[]{"some","shell","command"};
            }
        };

        {
            //test threadcount/keepgoing change

            final Document document = JobDefinitionSerializer.serialize(simpleCommand2);
            assertNotNull(document.selectSingleNode("/joblist/job/dispatch"));
            assertNotNull(document.selectSingleNode("/joblist/job/dispatch/threadcount"));
            assertEquals("3", document.selectSingleNode("/joblist/job/dispatch/threadcount").getText());
            assertNotNull(document.selectSingleNode("/joblist/job/dispatch/keepgoing"));
            assertEquals("true", document.selectSingleNode("/joblist/job/dispatch/keepgoing").getText());
        }

        
        {
            //test simple shell command

            final Document document = JobDefinitionSerializer.serialize(simpleCommand1);
//            p(document);

            assertNotNull(document);
            assertEquals("incorrect root element", "joblist", document.getRootElement().getName());
            final List jobs = document.selectNodes("/joblist/job");
            assertNotNull("missing /joblist/job element", jobs);
            assertEquals("wrong size for /joblist/job", 1, jobs.size());

            assertNotNull(document.selectSingleNode("/joblist/job/sequence/command/exec"));
            assertEquals("some shell command", document.selectSingleNode("/joblist/job/sequence/command/exec").getText());
            assertNull(document.selectSingleNode("/joblist/job/sequence/command/script"));
            assertNull(document.selectSingleNode("/joblist/job/sequence/command/scriptfile"));
            assertNull(document.selectSingleNode("/joblist/job/sequence/command/scriptargs"));
        }

        {
            IDispatchedScript script = new testScript() {

                public String getFrameworkProject() {
                    return TEST_PROJECT;
                }

                public String getScript() {
                    return "#!/bin/bash\n"
                           + "\n"
                           + "echo this is a test\n"
                           + "uptime\n";
                }

                public String[] getArgs() {
                    return new String[0];
                }
            };

            //test simple script with no args

            final Document document = JobDefinitionSerializer.serialize(script);
//            p(document);

            assertNotNull(document);
            assertEquals("incorrect root element", "joblist", document.getRootElement().getName());
            final List jobs = document.selectNodes("/joblist/job");
            assertNotNull("missing /joblist/job element", jobs);
            assertEquals("wrong size for /joblist/job", 1, jobs.size());

            assertNull(document.selectSingleNode("/joblist/job/sequence/command/exec"));
            assertNotNull(document.selectSingleNode("/joblist/job/sequence/command/script"));
            assertEquals("#!/bin/bash\n"
                         + "\n"
                         + "echo this is a test\n"
                         + "uptime\n", document.selectSingleNode("/joblist/job/sequence/command/script").getText());
            assertNull(document.selectSingleNode("/joblist/job/sequence/command/scriptfile"));
            assertNull(document.selectSingleNode("/joblist/job/sequence/command/scriptargs"));
        }
        {
            IDispatchedScript script = new testScript() {
                public String getFrameworkProject() {
                    return TEST_PROJECT;
                }

                public String getScript() {
                    return "#!/bin/bash\n"
                           + "\n"
                           + "echo this is a test\n"
                           + "uptime\n";
                }

                public String[] getArgs() {
                    return new String[]{"this","is","args"};
                }
            };

            //test simple script with args

            final Document document = JobDefinitionSerializer.serialize(script);
//            p(document);

            assertNotNull(document);
            assertEquals("incorrect root element", "joblist", document.getRootElement().getName());
            final List jobs = document.selectNodes("/joblist/job");
            assertNotNull("missing /joblist/job element", jobs);
            assertEquals("wrong size for /joblist/job", 1, jobs.size());

            assertNull(document.selectSingleNode("/joblist/job/sequence/command/exec"));
            assertNotNull(document.selectSingleNode("/joblist/job/sequence/command/script"));
            assertEquals("#!/bin/bash\n"
                         + "\n"
                         + "echo this is a test\n"
                         + "uptime\n", document.selectSingleNode("/joblist/job/sequence/command/script").getText());
            assertNull(document.selectSingleNode("/joblist/job/sequence/command/scriptfile"));
            assertEquals("this is args", document.selectSingleNode("/joblist/job/sequence/command/scriptargs").getText());
        }
        {
            IDispatchedScript script = new testScript() {

                public String getFrameworkProject() {
                    return TEST_PROJECT;
                }

                public String getServerScriptFilePath() {
                    return "/usr/local/scripts/test1.sh";
                }

                public String[] getArgs() {
                    return new String[0];
                }
            };
            //test simple script file with no Args

            final Document document = JobDefinitionSerializer.serialize(script);
//            p(document);

            assertNotNull(document);
            assertEquals("incorrect root element", "joblist", document.getRootElement().getName());
            final List jobs = document.selectNodes("/joblist/job");
            assertNotNull("missing /joblist/job element", jobs);
            assertEquals("wrong size for /joblist/job", 1, jobs.size());

            assertNull(document.selectSingleNode("/joblist/job/sequence/command/exec"));
            assertNull(document.selectSingleNode("/joblist/job/sequence/command/script"));
            assertNotNull(document.selectSingleNode("/joblist/job/sequence/command/scriptfile"));
            assertEquals("/usr/local/scripts/test1.sh", document.selectSingleNode("/joblist/job/sequence/command/scriptfile").getText());
            assertNull(document.selectSingleNode("/joblist/job/sequence/command/scriptargs"));
        }
        {
            IDispatchedScript script = new testScript() {

                public String getFrameworkProject() {
                    return TEST_PROJECT;
                }

                public String getServerScriptFilePath() {
                    return "/usr/local/scripts/test1.sh";
                }

                public String[] getArgs() {
                    return new String[]{"-this","arg","-is","here"};
                }

            };
            //test \ script file with Args

            final Document document = JobDefinitionSerializer.serialize(script);
//            p(document);

            assertNotNull(document);
            assertEquals("incorrect root element", "joblist", document.getRootElement().getName());
            final List jobs = document.selectNodes("/joblist/job");
            assertNotNull("missing /joblist/job element", jobs);
            assertEquals("wrong size for /joblist/job", 1, jobs.size());

            assertNull(document.selectSingleNode("/joblist/job/sequence/command/exec"));
            assertNull(document.selectSingleNode("/joblist/job/sequence/command/script"));
            assertNotNull(document.selectSingleNode("/joblist/job/sequence/command/scriptfile"));
            assertEquals("/usr/local/scripts/test1.sh", document.selectSingleNode("/joblist/job/sequence/command/scriptfile").getText());
            assertNotNull("expected not null scriptargs",document.selectSingleNode("/joblist/job/sequence/command/scriptargs"));
            assertEquals("-this arg -is here", document.selectSingleNode("/joblist/job/sequence/command/scriptargs").getText());
        }
    }


    private static void p(Document document) throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(System.err, format);
        writer.write(document);
        writer.flush();
    }
}

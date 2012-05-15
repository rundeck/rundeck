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
* TestScriptResourceUtil.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/17/11 11:42 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * TestScriptResourceUtil is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestScriptResourceUtil extends TestCase {
    File workingdir;
    Map<String, String> testEnv;

    public void setUp() throws Exception {
        workingdir = new File("build/testdir");
        testEnv = new ProcessBuilder("test").environment();
    }

    public void tearDown() throws Exception {

    }

    public void testBuildProcessArgs() throws Exception {
        //
        File scriptfile = new File(workingdir, "test1.sh");
        {
            final ProcessBuilder processBuilder = ScriptResourceUtil.buildProcess(workingdir, scriptfile, null, null,
                null,
                null, true);
            assertEquals(Arrays.asList(scriptfile.getAbsolutePath()), processBuilder.command());
            assertEquals(workingdir, processBuilder.directory());
            assertEquals(testEnv.size(), processBuilder.environment().size());
        }
        {
            final ProcessBuilder processBuilder = ScriptResourceUtil.buildProcess(workingdir, scriptfile, "a b", null,
                null,
                null, true);
            assertEquals(Arrays.asList(scriptfile.getAbsolutePath() + " a b"), processBuilder.command());
            assertEquals(workingdir, processBuilder.directory());
            assertEquals(testEnv.size(), processBuilder.environment().size());
        }
        {
            //interpreter args quoted
            final ProcessBuilder processBuilder = ScriptResourceUtil.buildProcess(workingdir, scriptfile, "a b", null,
                null,
                "test -c", true);
            assertEquals(Arrays.asList("test", "-c", scriptfile.getAbsolutePath() + " a b"), processBuilder.command());
            assertEquals(workingdir, processBuilder.directory());
            assertEquals(testEnv.size(), processBuilder.environment().size());
        }
        {
            //interpreter args not quoted
            final ProcessBuilder processBuilder = ScriptResourceUtil.buildProcess(workingdir, scriptfile, "a b", null,
                null,
                "test -c", false);
            assertEquals(Arrays.asList("test", "-c", scriptfile.getAbsolutePath() , "a","b"), processBuilder.command());
            assertEquals(workingdir, processBuilder.directory());
            assertEquals(testEnv.size(), processBuilder.environment().size());
        }
    }

    public void testBuildProcessEnv() throws Exception {
        //
        File scriptfile = new File(workingdir, "test1.sh");
        {
            final Map<String, Map<String, String>> envContext = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testctx = new HashMap<String, String>();
            testctx.put("test1", "value1");
            testctx.put("test2", "value2");
            envContext.put("test", testctx);

            final ProcessBuilder processBuilder = ScriptResourceUtil.buildProcess(workingdir, scriptfile, null,
                envContext, null,
                null, true);
            assertEquals(Arrays.asList(scriptfile.getAbsolutePath()), processBuilder.command());
            assertEquals(workingdir, processBuilder.directory());
            final Map<String, String> environment = processBuilder.environment();
            assertEquals(testEnv.size() + 2, environment.size());
            assertTrue("env: " + environment, environment.containsKey("RD_TEST_TEST1"));
            assertEquals("value1", environment.get("RD_TEST_TEST1"));
            assertTrue(environment.containsKey("RD_TEST_TEST2"));
            assertEquals("value2", environment.get("RD_TEST_TEST2"));
        }
    }

    public void testBuildProcessDataReferences() throws Exception {
        //
        File scriptfile = new File(workingdir, "test1.sh");
        {
            //test null data context
            final Map<String, Map<String, String>> dataCtx = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testctx = new HashMap<String, String>();
            testctx.put("test1", "value1");
            dataCtx.put("test", testctx);

            final ProcessBuilder processBuilder = ScriptResourceUtil.buildProcess(workingdir, scriptfile,
                "noarg ${test.test1} ${test.test2} ${test.test3}", null, null, null, true);
            assertEquals(Arrays.asList(
                scriptfile.getAbsolutePath() + " noarg ${test.test1} ${test.test2} ${test.test3}"),
                processBuilder.command());
            assertEquals(workingdir, processBuilder.directory());

        }{
            //test null data context, not quoted args
            final Map<String, Map<String, String>> dataCtx = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testctx = new HashMap<String, String>();
            testctx.put("test1", "value1");
            dataCtx.put("test", testctx);

            final ProcessBuilder processBuilder = ScriptResourceUtil.buildProcess(workingdir, scriptfile,
                "noarg ${test.test1} ${test.test2} ${test.test3}", null, null, null, false);
            assertEquals(Arrays.asList(
                scriptfile.getAbsolutePath() , "noarg","${test.test1}","${test.test2}","${test.test3}"),
                processBuilder.command());
            assertEquals(workingdir, processBuilder.directory());

        }
        {
            //test partial data context
            final Map<String, Map<String, String>> dataCtx = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testctx = new HashMap<String, String>();
            testctx.put("test1", "value1");
            dataCtx.put("test", testctx);

            final ProcessBuilder processBuilder = ScriptResourceUtil.buildProcess(workingdir, scriptfile,
                "noarg ${test.test1} ${test.test2} ${test.test3}",
                null, dataCtx,
                null, true);
            assertEquals(Arrays.asList(scriptfile.getAbsolutePath() + " noarg value1 ${test.test2} ${test.test3}"),
                processBuilder.command());
            assertEquals(workingdir, processBuilder.directory());

        }
        {
            //test partial data context, not quoted args
            final Map<String, Map<String, String>> dataCtx = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testctx = new HashMap<String, String>();
            testctx.put("test1", "value1");
            dataCtx.put("test", testctx);

            final ProcessBuilder processBuilder = ScriptResourceUtil.buildProcess(workingdir, scriptfile,
                "noarg ${test.test1} ${test.test2} ${test.test3}",
                null, dataCtx,
                null, false);
            assertEquals(Arrays.asList(scriptfile.getAbsolutePath() , "noarg","value1","${test.test2}","${test.test3}"),
                processBuilder.command());
            assertEquals(workingdir, processBuilder.directory());

        }
        {
            //test partial data context, not quoted args, space in args
            final Map<String, Map<String, String>> dataCtx = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testctx = new HashMap<String, String>();
            testctx.put("test1", "value1");
            testctx.put("test2", "value is2");
            dataCtx.put("test", testctx);

            final ProcessBuilder processBuilder = ScriptResourceUtil.buildProcess(workingdir, scriptfile,
                "noarg ${test.test1} ${test.test2} ${test.test3}",
                null, dataCtx,
                null, false);
            assertEquals(Arrays.asList(scriptfile.getAbsolutePath() , "noarg","value1","value is2","${test.test3}"),
                processBuilder.command());
            assertEquals(workingdir, processBuilder.directory());

        }
    }


    public void testBuildExecParamsArgs() throws Exception {
        //
        File scriptfile = new File(workingdir, "test1.sh");
        {
            final ScriptResourceUtil.ExecParams params = ScriptResourceUtil.buildExecParams(scriptfile, null,
                null, null);

            assertEquals(Arrays.asList(scriptfile.getAbsolutePath()), Arrays.asList(params.getArgs()));
            assertEquals(0, params.getEnvarr().length);
        }
        {
            final ScriptResourceUtil.ExecParams params = ScriptResourceUtil.buildExecParams(scriptfile, "a b",
                null, null);
            assertEquals(Arrays.asList(scriptfile.getAbsolutePath(),"a","b"), Arrays.asList(params.getArgs()));
            assertEquals(0, params.getEnvarr().length);
        }
    }

    public void testBuildExecParamsEnv() throws Exception {
        //
        File scriptfile = new File(workingdir, "test1.sh");
        {
            final Map<String, Map<String, String>> envContext = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testctx = new HashMap<String, String>();
            testctx.put("test1", "value1");
            testctx.put("test2", "value2");
            envContext.put("test", testctx);

            final ScriptResourceUtil.ExecParams params = ScriptResourceUtil.buildExecParams(scriptfile, null,
                envContext,
                null);
            assertEquals(Arrays.asList(scriptfile.getAbsolutePath()), Arrays.asList(
                params.getArgs()));
            final String[] environment = params.getEnvarr();
            assertEquals(2, environment.length);
            assertEquals("RD_TEST_TEST1=value1", environment[0]);
            assertEquals("RD_TEST_TEST2=value2", environment[1]);
        }
    }

    public void testBuildExecParamsDataReferences() throws Exception {
        //
        File scriptfile = new File(workingdir, "test1.sh");
        {
            //test null data context
            final Map<String, Map<String, String>> dataCtx = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testctx = new HashMap<String, String>();
            testctx.put("test1", "value1");
            dataCtx.put("test", testctx);

            final ScriptResourceUtil.ExecParams params = ScriptResourceUtil.buildExecParams(scriptfile,
                "noarg ${test.test1} ${test.test2} ${test.test3}", null, null);
            assertEquals(Arrays.asList(
                scriptfile.getAbsolutePath() ,"noarg","${test.test1}","${test.test2}","${test.test3}"),
                Arrays.asList(params.getArgs()));

        }
        {
            //test partial data context
            final Map<String, Map<String, String>> dataCtx = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testctx = new HashMap<String, String>();
            testctx.put("test1", "value1");
            dataCtx.put("test", testctx);

            final ScriptResourceUtil.ExecParams params = ScriptResourceUtil.buildExecParams(scriptfile,
                "noarg ${test.test1} ${test.test2} ${test.test3}", null, dataCtx);
            assertEquals(Arrays.asList(
                scriptfile.getAbsolutePath(), "noarg", "value1", "${test.test2}", "${test.test3}"),
                Arrays.asList(params.getArgs()));

        }
    }
}

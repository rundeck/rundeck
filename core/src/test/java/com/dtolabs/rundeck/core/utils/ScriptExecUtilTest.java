package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecArgList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 6/18/13 Time: 3:47 PM
 */
@RunWith(JUnit4.class)
public class ScriptExecUtilTest {

    @Test
    public void noOsFamilyQuotedDefaultUnix() {
        testCreateScriptArgs(
                null,
                null,
                new String[]{"arg1", "arg2"},
                "bash -c",
                "/path/to/file",
                true,
                new String[]{"bash", "-c", "'/path/to/file arg1 arg2'"}
        );
    }
    @Test
    public void noOsFamilyUnquoted() {
        testCreateScriptArgs(
                null,
                null,
                new String[]{"arg1", "arg2"},
                "bash -c",
                "/path/to/file",
                false,
                new String[]{"bash", "-c", "/path/to/file", "arg1", "arg2"}
        );
    }
    @Test
    public void withSpacesQuoted() {
        testCreateScriptArgs("unix", null,
                new String[]{"arg1 arg2", "arg3"},
                "bash -c",
                "/path/to/file",
                true,
                new String[]{"bash", "-c", "'/path/to/file '\"'\"'arg1 arg2'\"'\"' arg3'"}
        );
    }
    @Test
    public void withQuotesQuoted() {
        testCreateScriptArgs("unix", null,
                new String[]{"arg1'arg2", "arg3"},
                "bash -c",
                "/path/to/file",
                true,
                new String[]{"bash", "-c", "'/path/to/file '\"'\"'arg1'\"'\"'\"'\"'\"'\"'\"'\"'arg2'\"'\"' arg3'"}
        );
    }
    @Test
    public void withSpacesUnquoted() {
        testCreateScriptArgs("unix", null,
                new String[]{"arg1 arg2", "arg3"},
                "bash -c",
                "/path/to/file",
                false,
                new String[]{"bash", "-c", "/path/to/file", "'arg1 arg2'", "arg3"}
        );
    }
    @Test
    public void withQuotesUnquoted() {
        testCreateScriptArgs("unix", null,
                new String[]{"arg1'arg2", "arg3"},
                "bash -c",
                "/path/to/file",
                false,
                new String[]{"bash", "-c", "/path/to/file", "'arg1'\"'\"'arg2'", "arg3"}
        );
    }

    @Test
    public void noSpacesQuoted() {
        testCreateScriptArgs("unix", null,
                new String[]{"arg1", "arg2"},
                "bash -c",
                "/path/to/file",
                true,
                new String[]{"bash", "-c", "'/path/to/file arg1 arg2'"}
        );
    }
    @Test
    public void noSpacesUnquoted() {
        testCreateScriptArgs("unix", null,
                new String[]{"arg1", "arg2"},
                "bash -c",
                "/path/to/file",
                false,
                new String[]{"bash", "-c", "/path/to/file", "arg1", "arg2"}
        );
    }


    @Test
    public void dataContextQuoted() {
        testCreateScriptArgs("unix", DataContextUtils.addContext("option", new HashMap<String,
                String>() {{
            put("test", "test1");
            put("test2", "test 2");
        }}, null),
                new String[]{"arg1 arg2", "${option.test}"},
                "bash -c",
                "/path/to/file",
                true,
                new String[]{"bash", "-c", "'/path/to/file '\"'\"'arg1 arg2'\"'\"' test1'"}
        );
    }
    @Test
    public void dataContextUnquoted() {
        testCreateScriptArgs("unix", DataContextUtils.addContext("option", new HashMap<String,
                String>() {{
            put("test", "test1");
            put("test2", "test 2");
        }}, null),
                new String[]{"arg1 arg2", "${option.test}"},
                "bash -c",
                "/path/to/file",
                false,
                new String[]{"bash", "-c", "/path/to/file", "'arg1 arg2'", "test1"}
        );
    }
    @Test
    public void customInterpreterFilePathlocation() {
        testCreateScriptArgs("unix", DataContextUtils.addContext("option", new HashMap<String,
                String>() {{
            put("test", "test1");
            put("test2", "test 2");
        }}, null),
                new String[]{"arg1 arg2", "${option.test}"},
                "Powershell -Noprofile -Command 'Invoke-Command -ScriptBlock " +
                        "([ScriptBlock]::Create((Get-Content ${scriptfile})))'",
                "\\path\\to\\file",
                false,
                new String[]{"Powershell", "-Noprofile", "-Command", "Invoke-Command -ScriptBlock ([ScriptBlock]::Create((Get-Content \\path\\to\\file)))","'arg1 arg2'", "test1"}
        );
    }
    @Test
    public void dataContextOptionWithSpacesQuoted() {
        testCreateScriptArgs("unix", DataContextUtils.addContext("option", new HashMap<String,
                String>() {{
            put("test", "test1");
            put("test2", "test 2");
        }}, null),
                new String[]{"arg1 arg2", "${option.test2}"},
                "bash -c",
                "/path/to/file",
                true,
                new String[]{"bash", "-c", "'/path/to/file '\"'\"'arg1 arg2'\"'\"' '\"'\"'test 2'\"'\"''"}
        );
    }
    @Test
    public void dataContextOptionWithSpacesUnquoted() {
        testCreateScriptArgs("unix", DataContextUtils.addContext("option", new HashMap<String,
                String>() {{
            put("test", "test1");
            put("test2", "test 2");
        }}, null),
                new String[]{"arg1 arg2", "${option.test2}"},
                "bash -c",
                "/path/to/file",
                false,
                new String[]{"bash", "-c", "/path/to/file", "'arg1 arg2'", "'test 2'"}
        );
    }

    private void testCreateScriptArgs(String osFamily, Map<String, Map<String, String>> dataContext, String[] args, String
            scriptinterpreter, String filepath, boolean interpreterargsquoted, String[] expected) {
        testCreateScriptArgList(osFamily, dataContext, args, scriptinterpreter, filepath, interpreterargsquoted,
                expected);
    }
    private void testCreateScriptArgsArray(String osFamily, Map<String, Map<String, String>> dataContext, String[] args, String
            scriptinterpreter, String filepath, boolean interpreterargsquoted, String[] expected) {
        NodeEntryImpl node = null;
        if (null != osFamily) {
            node = new NodeEntryImpl("blah");
            node.setOsFamily(osFamily);
        }

        Map<String, Map<String, String>> localDataContext = dataContext;

        String[] scriptArgs = ScriptExecUtil.createScriptArgs(
                localDataContext,
                node,
                null,
                args,
                scriptinterpreter, interpreterargsquoted,
                filepath
        );
        Assert.assertArrayEquals(Arrays.asList(scriptArgs).toString(), expected, scriptArgs);
    }
    private void testCreateScriptArgList(String osFamily, Map<String, Map<String, String>> dataContext, String[] args, String
            scriptinterpreter, String filepath, boolean interpreterargsquoted, String[] expected) {
        NodeEntryImpl node = null;
        if (null != osFamily) {
            node = new NodeEntryImpl("blah");
            node.setOsFamily(osFamily);
        }

        ExecArgList arglist = ScriptExecUtil.createScriptArgList(
                filepath, null,
                args,
                scriptinterpreter,
                interpreterargsquoted
        );
        ArrayList<String> scriptArgs = arglist.buildCommandForNode(dataContext, osFamily);
        Assert.assertArrayEquals(scriptArgs.toString(), expected, scriptArgs.toArray(new String[scriptArgs.size()]));
    }
}

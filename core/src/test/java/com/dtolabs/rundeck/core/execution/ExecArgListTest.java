package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

/**
 * $INTERFACE is ... User: greg Date: 6/19/13 Time: 2:23 PM
 */
@RunWith(JUnit4.class)
public class ExecArgListTest {

    public void testBuildCommandForNode(ExecArgList list, List<String> expected, Map<String, Map<String,
            String>> dataContext, String osFamily) {
        ArrayList<String> strings = list.buildCommandForNode(dataContext, osFamily);
        Assert.assertEquals(expected, strings);
    }

    private List<String> list(String... strs) {
        return Arrays.asList(strs);
    }

    private Map<String, String> map(String... strs) {
        HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        for(int i=0;i+1<strs.length;i+=2) {
            stringStringHashMap.put(strs[i], strs[i + 1]);
        }
        return stringStringHashMap;
    }

    @Test
    public void buildSimpleCommandNoOSFamily() {
        testBuildCommandForNode(ExecArgList.fromStrings(true, "a", "command"), list("a", "command"), null, null);
    }

    @Test
    public void buildSsimpleSpaceNoOSFamilyUnixQuotes() {
        testBuildCommandForNode(ExecArgList.fromStrings(true, "a", "test command"), list("a", "'test command'"), null,
                null);
    }

    @Test
    public void buildUnixSpace() {
        testBuildCommandForNode(ExecArgList.fromStrings(true, "a", "test command"), list("a", "'test command'"), null,
                "unix");
    }

    @Test
    public void buildUnixSpaceUnquoted() {
        testBuildCommandForNode(ExecArgList.fromStrings(false, "a", "test command"), list("a", "test command"), null,
                "unix");
    }

    @Test
    public void buildQuoteUnquote() {
        ExecArgList a = ExecArgList.builder().arg("a test", false).arg("b test", true).build();
        testBuildCommandForNode(a, list("a test", "'b test'"), null,
                "unix");
    }
    @Test
    public void buildExpandOptionRef() {
        testBuildCommandForNode(
                ExecArgList.fromStrings(true, "a", "command", "${option.test}"),
                list("a", "command", "test1"),
                DataContextUtils.addContext("option", map("test", "test1"), null),
                null);
    }
    @Test
    public void buildExpandOptionRefMissingIsBlank() {
        testBuildCommandForNode(
                ExecArgList.fromStrings(true, "a", "command", "${option.test2}"),
                list("a", "command", ""),
                DataContextUtils.addContext("option", map("test", "test1"), null),
                null);
    }
    @Test
    public void buildExpandNodeRef() {
        testBuildCommandForNode(
                ExecArgList.fromStrings(true, "a", "command", "${node.name}"),
                list("a", "command", "node1"),
                DataContextUtils.addContext("node", map("name", "node1"), null),
                null);
    }
    @Test
    public void buildExpandNodeRefMissing() {
        testBuildCommandForNode(
                ExecArgList.fromStrings(true, "a", "command", "${node.blah}"),
                list("a", "command", "'${node.blah}'"),
                DataContextUtils.addContext("node", map("name", "node1"), null),
                null);
    }


    @Test
    public void buildSubListUnquoted() {
        ExecArgList.Builder builder =
                ExecArgList.builder()
                        .arg("a test", false)
                        .arg("b test", true)
                        .subList(false)
                        .arg("alpha beta", true)
                        .arg("delta gamma", false)
                        .parent();
        ExecArgList a = builder.build();
        testBuildCommandForNode(a, list("a test", "'b test'", "'alpha beta' delta gamma"), null,
                "unix");
    }

    @Test
    public void buildSubListQuoted() {
        ExecArgList.Builder builder =
                ExecArgList.builder()
                        .arg("a test", false)
                        .arg("b test", true)
                        .subList(true)
                        .arg("alpha beta", true)
                        .arg("delta gamma", false)
                        .parent();
        ExecArgList a = builder.build();
        testBuildCommandForNode(a, list("a test", "'b test'", "''\"'\"'alpha beta'\"'\"' delta gamma'"), null,
                "unix");
    }

    @Test
    public void joinAndQuoteNoQuote() {
        Assert.assertEquals("a b c", ExecArgList.joinAndQuote(list("a", "b", "c"), null));
    }
    @Test
    public void joinAndQuoteUnix() {
        Assert.assertEquals("'a b c'", ExecArgList.joinAndQuote(list("a", "b", "c"),
                CLIUtils.argumentQuoteForOperatingSystem("unix")));
    }
}

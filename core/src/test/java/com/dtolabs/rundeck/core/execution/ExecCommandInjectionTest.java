package com.dtolabs.rundeck.core.execution;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for command injection protection in exec commands with ${option.name} syntax
 */
@RunWith(JUnit4.class)
public class ExecCommandInjectionTest {

    @Test
    public void testCommandInjectionPipeBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("Scanning port: 80 | whoami", true, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'Scanning port: 80 | whoami'", result.get(1));
    }

    @Test
    public void testCommandInjectionRedirectionBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("data > /tmp/poc_file", true, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'data > /tmp/poc_file'", result.get(1));
    }

    @Test
    public void testCommandInjectionCommandSubstitutionBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("Current user: $(whoami)", true, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'Current user: $(whoami)'", result.get(1));
    }

    @Test
    public void testCommandInjectionBackticksBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("Current user: `whoami`", true, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'Current user: `whoami`'", result.get(1));
    }

    @Test
    public void testCommandInjectionAndOperatorBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("test && rm -rf /", true, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'test && rm -rf /'", result.get(1));
    }

    @Test
    public void testCommandInjectionSemicolonBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("test; whoami", true, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'test; whoami'", result.get(1));
    }

    @Test
    public void testSimpleValueNotQuoted() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("80", true, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("80", result.get(1));
    }

    @Test
    public void testPathWithSpacesQuoted() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("cat", false, false);
        builder.arg("/my files/log.txt", true, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("cat", result.get(0));
        Assert.assertEquals("'/my files/log.txt'", result.get(1));
    }

    @Test
    public void testQuotingDisabledAllowsUnquoted() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("data > /tmp/test", false, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("data > /tmp/test", result.get(1));
    }

    @Test
    public void testGarrettTestCaseBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("Scanning port: 80 6c 1d > /tmp/poc_amd", true, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'Scanning port: 80 6c 1d > /tmp/poc_amd'", result.get(1));
    }
}

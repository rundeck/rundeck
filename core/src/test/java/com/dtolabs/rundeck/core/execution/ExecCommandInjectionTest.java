package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Collections;
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

    @Test
    public void testFeatureQuotingBackwardCompatibleQuotesModifiedStrings() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, true);
        builder.arg("Scanning port: 80 | whoami", true, true);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'Scanning port: 80 | whoami'", result.get(1));
    }

    @Test
    public void testQuotingAppliedWhenArgumentContainsPropertyReference() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        // Simulate an argument that originally contained ${option.port} and was replaced
        builder.arg("80 | whoami", true, false);
        ExecArgList execArgList = builder.build();
        
        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'80 | whoami'", result.get(1));
    }

    @Test
    public void testQuotingSkippedWhenArgumentDoesNotContainPropertyReference() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        // Argument that never contained a property reference
        builder.arg("literal text", false, false);
        ExecArgList execArgList = builder.build();

        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "unix");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("literal text", result.get(1));
    }

    // -------------------------------------------------------------------------
    // RUN-4179 Problem 1 — Per-value quoting / semicolon regression
    // -------------------------------------------------------------------------

    /**
     * FAILS before fix: ${unquotedoption.*} refs must be exempted from the converter
     * in SharedDataContextUtils.replaceDataReferences.
     *
     * Before fix: converter is applied to ALL resolved values, including ${unquoted.*},
     * quoting values that users deliberately chose not to quote — a breaking change.
     *
     * After fix: the unquoted prefix is detected before calling converter.convert(),
     * so the value passes through unmodified.
     */
    @Test
    public void testUnquotedRefNotQuotedByConverter() {
        WFSharedContext ctx = new WFSharedContext();
        ctx.merge(ContextView.global(), DataContextUtils.context("option",
                Collections.singletonMap("port", "80 && whoami")));

        String result = SharedDataContextUtils.replaceDataReferences(
                "${unquotedoption.port}",
                ctx,
                ContextView.global(),
                ContextView::nodeStep,
                CLIUtils.argumentQuoteForOperatingSystem("unix"),
                false,
                true
        );

        Assert.assertEquals(
                "${unquotedoption.*} values must NOT be quoted by the converter (breaking change otherwise)",
                "80 && whoami",
                result
        );
    }

    /**
     * Regression guard: per-value quoting leaves template-level semicolons outside
     * the quoted values so the shell can interpret them as command separators.
     *
     * Simulates per-value quoting of safe values: quoteUnixShellArg passes /workspace
     * and exec123 through unchanged, so the template semicolon is never wrapped.
     * Passes both before and after the fix (documents the correct approach).
     */
    @Test
    public void testPerValueQuotingPreservesSemicolonForSafeValues() {
        WFSharedContext ctx = new WFSharedContext();
        ctx.merge(ContextView.global(), DataContextUtils.context("data",
                Collections.singletonMap("home_dir", "/workspace")));
        ctx.merge(ContextView.global(), DataContextUtils.context("job",
                Collections.singletonMap("execid", "exec123")));

        // Simulate what the fixed plugin does: expand with OS-aware converter
        String expanded = SharedDataContextUtils.replaceDataReferences(
                "${data.home_dir}/${job.execid};",
                ctx,
                ContextView.global(),
                ContextView::nodeStep,
                CLIUtils.argumentQuoteForOperatingSystem("unix"),
                false,
                true
        );

        // Safe values contain no shell-special chars; quoteUnixShellArg passes through
        Assert.assertEquals("/workspace/exec123;", expanded);

        // Feed into ExecArgList with shouldQuote=false (quoting already done)
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("cd", false, false);
        builder.arg(expanded, false, false);
        ArrayList<String> cmd = builder.build().buildCommandForNode(new HashMap<>(), "unix");

        Assert.assertEquals("cd", cmd.get(0));
        Assert.assertEquals(
                "Semicolon must remain a shell separator after per-value quoting of safe values",
                "/workspace/exec123;",
                cmd.get(1)
        );
    }

    /**
     * Security guard: per-value quoting blocks injection via data.* context even
     * when the template-level semicolon stays free.
     *
     * If data.home_dir contains an injection payload, the converter wraps that value
     * in single quotes, trapping the injected operator inside the quoted value.
     * The template-level trailing semicolon is outside the quoted value and remains free.
     */
    @Test
    public void testPerValueQuotingBlocksDataContextInjection() {
        WFSharedContext ctx = new WFSharedContext();
        ctx.merge(ContextView.global(), DataContextUtils.context("data",
                Collections.singletonMap("home_dir", "/workspace; rm -rf /")));
        ctx.merge(ContextView.global(), DataContextUtils.context("job",
                Collections.singletonMap("execid", "exec123")));

        String expanded = SharedDataContextUtils.replaceDataReferences(
                "${data.home_dir}/${job.execid};",
                ctx,
                ContextView.global(),
                ContextView::nodeStep,
                CLIUtils.argumentQuoteForOperatingSystem("unix"),
                false,
                true
        );

        // Injected value is quoted; exec123 has no special chars so it stays unquoted;
        // template ';' at the end stays outside any quoted value → free as separator
        Assert.assertEquals("'/workspace; rm -rf /'/exec123;", expanded);

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("cd", false, false);
        builder.arg(expanded, false, false);
        ArrayList<String> cmd = builder.build().buildCommandForNode(new HashMap<>(), "unix");

        Assert.assertEquals(
                "Injected value must be quoted; template semicolon must remain free",
                "'/workspace; rm -rf /'/exec123;",
                cmd.get(1)
        );
    }
}

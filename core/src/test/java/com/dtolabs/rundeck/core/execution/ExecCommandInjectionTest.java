package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.data.BaseDataContext;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext;
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

    private static Map<String, String> singleOption(String key, String value) {
        Map<String, String> m = new HashMap<>();
        m.put(key, value);
        return m;
    }

    private static WFSharedContext sharedContextWithOption(String nodeName, String key, String value) {
        WFSharedContext sharedContext = new WFSharedContext();
        sharedContext.merge(ContextView.node(nodeName), new BaseDataContext("option", singleOption(key, value)));
        return sharedContext;
    }

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

    @Test
    public void testWindowsCommandInjectionPipeBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("Scanning port: 80 | whoami", true, false);
        ExecArgList execArgList = builder.build();

        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "windows");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("\"Scanning port: 80 | whoami\"", result.get(1));
    }

    @Test
    public void testWindowsCommandInjectionAndOperatorBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("test && del /q C:\\", true, false);
        ExecArgList execArgList = builder.build();

        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "windows");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("\"test && del /q C:\\\\\"", result.get(1));
    }

    @Test
    public void testWindowsCommandInjectionRedirectionBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("data > C:\\tmp\\file", true, false);
        ExecArgList execArgList = builder.build();

        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "windows");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("\"data > C:\\tmp\\file\"", result.get(1));
    }

    @Test
    public void testWindowsPathWithSpacesQuoted() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("pwsh", false, false);
        builder.arg("-File", false, false);
        builder.arg("\\\\server\\share\\path with spaces\\script.ps1", true, false);
        ExecArgList execArgList = builder.build();

        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "windows");

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("pwsh", result.get(0));
        Assert.assertEquals("-File", result.get(1));
        Assert.assertEquals("\"\\\\server\\share\\path with spaces\\script.ps1\"", result.get(2));
    }

    @Test
    public void testWindowsEnvVarExpansionBlocked() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("%PATH%", true, false);
        ExecArgList execArgList = builder.build();

        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "windows");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("\"%%PATH%%\"", result.get(1));
    }

    @Test
    public void testWindowsInternalDoubleQuotesEscaped() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("say \"hello\"", true, false);
        ExecArgList execArgList = builder.build();

        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "windows");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("\"say \\\"hello\\\"\"", result.get(1));
    }

    @Test
    public void testWindowsSimpleValueNotQuoted() {
        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("80", true, false);
        ExecArgList execArgList = builder.build();

        Map<String, Map<String, String>> dataContext = new HashMap<>();
        ArrayList<String> result = execArgList.buildCommandForNode(dataContext, "windows");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("80", result.get(1));
    }

    /*
     * Regression tests for the shared-context buildCommandForNode(sharedContext, nodeName, osFamily,
     * commandInterpreter) overload used by ExecutionServiceImpl for every real node command execution.
     * That path resolves ${option.*} references and (if the argument is flagged for quoting) must
     * quote each substituted reference's *value* in place, rather than quoting the whole expanded
     * argument string -- fixing regressions reported in
     * https://github.com/rundeck/rundeck/issues/10293 and
     * https://github.com/rundeck/rundeck/issues/10027, both caused by the whole-argument quoting
     * this class's RUN-4175 fix originally introduced.
     */

    @Test
    public void testWholeArgumentReferenceStillQuotedSafely() {
        // Original CVE case (RUN-4175): a bare reference as its own argument must still be fully
        // quoted when its value contains shell metacharacters.
        WFSharedContext sharedContext = sharedContextWithOption("anode", "port", "80 | whoami");

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("${option.port}", true, false);
        ExecArgList execArgList = builder.build();

        ArrayList<String> result = execArgList.buildCommandForNode(sharedContext, "anode", "unix", null);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'80 | whoami'", result.get(1));
    }

    @Test
    public void testReferenceEmbeddedInLargerArgumentQuotedInPlace() {
        // #10293's exact reported scenario: a whole multi-word argument grouped by the job
        // author's own quotes, with an option reference embedded inside it, must stay one
        // argument. "userval1" has no shell-special characters, so it needs no quoting of its
        // own -- matching the reporter's own confirmation that the *unquoted* form
        // ("sudo sh script.sh userval1") is what actually worked.
        WFSharedContext sharedContext = sharedContextWithOption("anode", "parm1", "userval1");

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("sudo", false, false);
        builder.arg("sh /u01/scripts/sre/myscript.sh ${option.parm1}", true, false);
        ExecArgList execArgList = builder.build();

        ArrayList<String> result = execArgList.buildCommandForNode(sharedContext, "anode", "unix", null);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("sudo", result.get(0));
        Assert.assertEquals("sh /u01/scripts/sre/myscript.sh userval1", result.get(1));
    }

    @Test
    public void testReferenceEmbeddedInLargerArgumentIsQuotedInPlaceWhenNeeded() {
        // Same shape as above, but with a value that does need quoting (contains a space): only
        // the value itself is wrapped in quotes, in place, not the whole surrounding argument.
        WFSharedContext sharedContext = sharedContextWithOption("anode", "parm1", "user val");

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("sudo", false, false);
        builder.arg("sh /u01/scripts/sre/myscript.sh ${option.parm1}", true, false);
        ExecArgList execArgList = builder.build();

        ArrayList<String> result = execArgList.buildCommandForNode(sharedContext, "anode", "unix", null);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("sudo", result.get(0));
        Assert.assertEquals("sh /u01/scripts/sre/myscript.sh 'user val'", result.get(1));
    }

    @Test
    public void testAlreadyQuotedReferenceIsNotDoubleQuoted() {
        // #10027-style: the job author already wrapped the reference in their own single quotes;
        // the fix must not double-quote the already-quoted value.
        WFSharedContext sharedContext = sharedContextWithOption("anode", "TenantPath", "CLIENT_Name");

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("-TenantPath '${option.TenantPath}'", true, false);
        ExecArgList execArgList = builder.build();

        ArrayList<String> result = execArgList.buildCommandForNode(sharedContext, "anode", "unix", null);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("-TenantPath 'CLIENT_Name'", result.get(0));
    }

    @Test
    public void testInjectionEmbeddedInLargerArgumentStillBlocked() {
        // A malicious option value embedded within a larger job-authored argument must still be
        // safely contained as a single quoted token, not allowed to break out and inject a second
        // command.
        WFSharedContext sharedContext = sharedContextWithOption("anode", "parm1", "x; rm -rf /");

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("sudo", false, false);
        builder.arg("sh script.sh ${option.parm1}", true, false);
        ExecArgList execArgList = builder.build();

        ArrayList<String> result = execArgList.buildCommandForNode(sharedContext, "anode", "unix", null);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("sudo", result.get(0));
        Assert.assertEquals("sh script.sh 'x; rm -rf /'", result.get(1));
    }

    @Test
    public void testQuotingDisabledLeavesReferenceUnquoted() {
        WFSharedContext sharedContext = sharedContextWithOption("anode", "port", "80 | whoami");

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        // arg not flagged for quoting, e.g. rundeck.feature.exec.quoting.enabled=false
        builder.arg("${option.port}", false, false);
        ExecArgList execArgList = builder.build();

        ArrayList<String> result = execArgList.buildCommandForNode(sharedContext, "anode", "unix", null);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("80 | whoami", result.get(1));
    }

    @Test
    public void testWindowsReferenceEmbeddedInLargerArgumentQuotedInPlace() {
        WFSharedContext sharedContext = sharedContextWithOption("anode", "name", "test file");

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("pwsh", false, false);
        builder.arg("-File", false, false);
        builder.arg("C:\\scripts\\${option.name}.ps1", true, false);
        ExecArgList execArgList = builder.build();

        ArrayList<String> result = execArgList.buildCommandForNode(sharedContext, "anode", "windows", null);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("pwsh", result.get(0));
        Assert.assertEquals("-File", result.get(1));
        Assert.assertEquals("C:\\scripts\\\"test file\".ps1", result.get(2));
    }

    @Test
    public void testMissingOptionReferenceIsBlankedNotLiteral() {
        // A reference to an option with no value should still resolve to blank (existing
        // behavior), not leave the literal ${option.x} text in the command. An empty value has
        // nothing to escape, so it is left unquoted, same as any other plain value with no
        // shell-special characters.
        WFSharedContext sharedContext = new WFSharedContext();

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        builder.arg("${option.missing}", true, false);
        ExecArgList execArgList = builder.build();

        ArrayList<String> result = execArgList.buildCommandForNode(sharedContext, "anode", "unix", null);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("", result.get(1));
    }

    @Test
    public void testAlreadySubstitutedArgumentIsStillQuotedAsWhole() {
        // Some node step plugins (e.g. ScriptBasedRemoteScriptNodeStepPlugin, backing bundled
        // script-type plugins) resolve ${...} references themselves before building the
        // ExecArgList, so the string reaching buildCommandForNode() here never contains ${...} at
        // all -- it's already the final, materialized value. There is nothing to quote "in place"
        // in that case, so the whole (already-substituted) value must still be quoted as a unit,
        // exactly as it always was, or injection protection would be silently lost for that caller.
        WFSharedContext sharedContext = new WFSharedContext();

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, false);
        // No ${...} present -- this is what an already-substituted, dangerous value looks like.
        builder.arg("80 | whoami", true, false);
        ExecArgList execArgList = builder.build();

        ArrayList<String> result = execArgList.buildCommandForNode(sharedContext, "anode", "unix", null);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'80 | whoami'", result.get(1));
    }

    @Test
    public void testFeatureQuotingBackwardCompatibleQuotesModifiedStringsOnSharedContextPath() {
        // Same legacy semantics as testFeatureQuotingBackwardCompatibleQuotesModifiedStrings, but on
        // the shared-context buildCommandForNode(sharedContext, ...) overload: an argument NOT
        // flagged "quoted" must still be quoted as a whole if featureQuotingBackwardCompatible is
        // enabled and substitution changed its value -- this must keep working even though quoting
        // for quoted=true arguments is now applied per-reference during expansion, not afterward.
        WFSharedContext sharedContext = sharedContextWithOption("anode", "port", "80 | whoami");

        ExecArgList.Builder builder = ExecArgList.builder();
        builder.arg("echo", false, true);
        builder.arg("Scanning port: ${option.port}", false, true);
        ExecArgList execArgList = builder.build();

        ArrayList<String> result = execArgList.buildCommandForNode(sharedContext, "anode", "unix", null);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("echo", result.get(0));
        Assert.assertEquals("'Scanning port: 80 | whoami'", result.get(1));
    }
}

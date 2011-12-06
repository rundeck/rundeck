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
* TestSSHTaskBuilder.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/15/11 3:43 PM
* 
*/
package com.dtolabs.rundeck.core.tasks.net;

import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import junit.framework.TestCase;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.util.*;

/**
 * TestSSHTaskBuilder is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestSSHTaskBuilder extends TestCase {


    static class testSSHBaseInterface implements SSHTaskBuilder.SSHBaseInterface {
        boolean failonerror;
        boolean trust;
        Project project;
        boolean verbose;
        String host;
        int port;
        String username;
        String keyfile;
        String passphrase;
        String password;
        String knownhosts;


        public void setFailonerror(boolean failonerror) {
            this.failonerror = failonerror;
        }

        public void setTrust(boolean trust) {
            this.trust = trust;
        }

        public void setProject(Project project) {
            this.project = project;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setKeyfile(String keyfile) {
            this.keyfile = keyfile;
        }

        public void setPassphrase(String passphrase) {
            this.passphrase = passphrase;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setKnownhosts(String knownhosts) {
            this.knownhosts = knownhosts;
        }

    }

    static class testSCPInterface extends testSSHBaseInterface implements SSHTaskBuilder.SCPInterface {
        String localFile;
        String remoteTofile;

        public void setLocalFile(String localFile) {
            this.localFile = localFile;
        }

        public void setRemoteTofile(String remoteTofile) {
            this.remoteTofile = remoteTofile;
        }
    }

    static class testSSHExecInterface extends testSSHBaseInterface implements SSHTaskBuilder.SSHExecInterface {
        String command;
        long timeout;
        String outputproperty;
        boolean failonerror;

        ArrayList<Environment.Variable> environment = new ArrayList<Environment.Variable>();


        public void setCommand(String command) {
            this.command = command;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public void setOutputproperty(String outputproperty) {
            this.outputproperty = outputproperty;
        }

        public void addEnv(Environment.Variable env) {
            environment.add(env);
        }
    }

    private static class testSSHConnectionInfo implements SSHTaskBuilder.SSHConnectionInfo {
        SSHTaskBuilder.AuthenticationType authenticationType;
        String privateKeyfilePath;
        String password;
        int SSHTimeout;
        String username;

        public SSHTaskBuilder.AuthenticationType getAuthenticationType() {
            return authenticationType;
        }

        public String getPrivateKeyfilePath() {
            return privateKeyfilePath;
        }

        public String getPassword() {
            return password;
        }

        public int getSSHTimeout() {
            return SSHTimeout;
        }

        public String getUsername() {
            return username;
        }
    }

    File testKeyfile;
    File testSourcefile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testKeyfile = File.createTempFile("test", "keyfile");
        testSourcefile = File.createTempFile("test", "sourcefile");

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testKeyfile.delete();
        testSourcefile.delete();
    }

    class testState {
        final NodeEntryImpl node = new NodeEntryImpl("hostname", "nodename");
        String[] strings = {"a", "b", "c"};
        final Project project = new Project();
        final Map<String, Map<String, String>> stringMapMap = new HashMap<String, Map<String, String>>();
        final testSSHConnectionInfo sshConnectionInfo = new testSSHConnectionInfo();
        int loglevel = 0;
        String remotePath;
        File sourceFile;

        testState() {
            sshConnectionInfo.authenticationType = SSHTaskBuilder.AuthenticationType.privateKey;
            sshConnectionInfo.privateKeyfilePath = testKeyfile.getAbsolutePath();
            sshConnectionInfo.username = "testusername";
        }
    }

    private void assertInvariable(testState state, testSSHExecInterface test) {
        //never changes
        assertEquals("sshexec.output", test.outputproperty);
        assertInvariableBase(state, test);
    }

    private void assertInvariableBase(testState state, testSSHBaseInterface test) {
        assertNull(test.knownhosts);
        assertEquals(true, test.failonerror);
        assertEquals(state.project, test.project);
        assertEquals(true, test.trust);
    }

    private void runBuildSSH(final testState state, final testSSHExecInterface test) throws
        SSHTaskBuilder.BuilderException {
        SSHTaskBuilder.build(test, state.node, state.strings, state.project, state.stringMapMap,
            state.sshConnectionInfo, state.loglevel);
    }

    private void runBuildSCP(final testState state, final testSCPInterface test) throws
        SSHTaskBuilder.BuilderException {
        SSHTaskBuilder.buildScp(test, state.node, state.project, state.remotePath,
            state.sourceFile, state.sshConnectionInfo, state.loglevel);
    }

    public void testBuildSSHDefault() throws Exception {
        final testState state = new testState();
        final testSSHExecInterface test = new testSSHExecInterface();

        runBuildSSH(state, test);


        assertEquals(CLIUtils.generateArgline(null, state.strings), test.command);
        assertEquals("hostname", test.host);
        assertEquals(0, test.port);
        assertEquals(testKeyfile.getAbsolutePath(), test.keyfile);
        assertEquals("", test.passphrase);
        assertEquals(null, test.password);
        assertEquals(0, test.timeout);
        assertEquals("testusername", test.username);
        assertEquals(false, test.verbose);
        assertInvariable(state, test);


    }

    public void testBuildSSHVariables() throws Exception {
        final testState state = new testState();
        final testSSHExecInterface test = new testSSHExecInterface();

        state.node.setHostname("hostname:33");
        state.sshConnectionInfo.SSHTimeout = 600;
        state.sshConnectionInfo.username = "usernameValue";

        runBuildSSH(state, test);

        assertEquals(CLIUtils.generateArgline(null, state.strings), test.command);
        assertEquals("hostname", test.host);
        assertEquals(33, test.port);
        assertEquals(testKeyfile.getAbsolutePath(), test.keyfile);
        assertEquals("", test.passphrase);
        assertEquals(null, test.password);
        assertEquals(600, test.timeout);
        assertEquals("usernameValue", test.username);
        assertEquals(false, test.verbose);

        //never changes
        assertInvariable(state, test);
    }

    public void testBuildSSHKeyNoUsername() throws Exception {
        final testState state = new testState();
        final testSSHExecInterface test = new testSSHExecInterface();

        state.sshConnectionInfo.authenticationType = SSHTaskBuilder.AuthenticationType.privateKey;
        state.sshConnectionInfo.username = null;

        //null node hostname
        try {
            runBuildSSH(state, test);
            fail("Shouldn't succeed");
        } catch (SSHTaskBuilder.BuilderException e) {
            assertEquals("username was not set", e.getMessage());
        }

    }

    public void testBuildSSHPassword() throws Exception {
        final testState state = new testState();
        final testSSHExecInterface test = new testSSHExecInterface();

        state.sshConnectionInfo.authenticationType = SSHTaskBuilder.AuthenticationType.password;
        state.sshConnectionInfo.password = "passwordValue";
        state.sshConnectionInfo.username = "usernameValue";

        runBuildSSH(state, test);

        assertEquals(CLIUtils.generateArgline(null, state.strings), test.command);
        assertEquals("hostname", test.host);
        assertEquals(0, test.port);
        assertEquals(null, test.keyfile);
        assertEquals(null, test.passphrase);
        assertEquals("passwordValue", test.password);
        assertEquals(0, test.timeout);
        assertEquals("usernameValue", test.username);
        assertEquals(false, test.verbose);

        //never changes
        assertInvariable(state, test);
    }

    public void testBuildSSHPasswordNoUsername() throws Exception {
        final testState state = new testState();
        final testSSHExecInterface test = new testSSHExecInterface();

        state.sshConnectionInfo.authenticationType = SSHTaskBuilder.AuthenticationType.password;
        state.sshConnectionInfo.password = "passwordValue";
        state.sshConnectionInfo.username = null;

        //null node hostname
        try {
            runBuildSSH(state, test);
            fail("Shouldn't succeed");
        } catch (SSHTaskBuilder.BuilderException e) {
            assertEquals("username was not set", e.getMessage());
        }

    }

    public void testBuildSSHVerbose() throws Exception {
        final testState state = new testState();
        final testSSHExecInterface test = new testSSHExecInterface();


        state.loglevel = 3;

        runBuildSSH(state, test);

        assertEquals(true, test.verbose);

        assertEquals(CLIUtils.generateArgline(null, state.strings), test.command);
        assertEquals("hostname", test.host);
        assertEquals(0, test.port);
        assertEquals(testKeyfile.getAbsolutePath(), test.keyfile);
        assertEquals("", test.passphrase);
        assertEquals(null, test.password);
        assertEquals(0, test.timeout);
        assertEquals("testusername", test.username);

        //never changes
        assertInvariable(state, test);
    }

    public void testBuildSSHNoHostname() throws Exception {
        final testState state = new testState();
        final testSSHExecInterface test = new testSSHExecInterface();

        /**
         * Set hostname=null
         */
        state.node.setHostname(null);

        //null node hostname
        try {
            runBuildSSH(state, test);
            fail("Shouldn't succeed");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }


    public void testBuildSSHNoAuthtype() throws Exception {
        final testState state = new testState();
        final testSSHExecInterface test = new testSSHExecInterface();

        /**
         * Set authtype=null
         */
        state.sshConnectionInfo.authenticationType = null;

        //no auth type
        try {
            runBuildSSH(state, test);
            fail("Shouldn't succeed");
        } catch (SSHTaskBuilder.BuilderException e) {
            assertNotNull(e);
        }
    }

    public void testBuildSCPDefault() throws Exception {

        final testState state = new testState();
        final testSCPInterface test = new testSCPInterface();

        state.sourceFile = testSourcefile;
        state.remotePath = "/test/path";

        runBuildSCP(state, test);

        assertEquals(testSourcefile.getAbsolutePath(), test.localFile);
        assertEquals("testusername@hostname:/test/path", test.remoteTofile);

        assertEquals("hostname", test.host);
        assertEquals(0, test.port);
        assertEquals(testKeyfile.getAbsolutePath(), test.keyfile);
        assertEquals("", test.passphrase);
        assertEquals(null, test.password);
        assertEquals("testusername", test.username);
        assertEquals(false, test.verbose);
        assertInvariableBase(state, test);
    }

    public void testBuildSCPPassword() throws Exception {

        final testState state = new testState();
        final testSCPInterface test = new testSCPInterface();

        state.sshConnectionInfo.authenticationType = SSHTaskBuilder.AuthenticationType.password;
        state.sshConnectionInfo.password = "passwordValue";
        state.sourceFile = testSourcefile;
        state.remotePath = "/test/path";

        runBuildSCP(state, test);

        assertEquals(testSourcefile.getAbsolutePath(), test.localFile);
        assertEquals("testusername@hostname:/test/path", test.remoteTofile);

        assertEquals("hostname", test.host);
        assertEquals(0, test.port);
        assertEquals(null, test.keyfile);
        assertEquals(null, test.passphrase);
        assertEquals("passwordValue", test.password);
        assertEquals("testusername", test.username);
        assertEquals(false, test.verbose);
        assertInvariableBase(state, test);
    }

    public void testBuildSCPKeyNoUsername() throws Exception {

        final testState state = new testState();
        final testSCPInterface test = new testSCPInterface();

        state.sourceFile = testSourcefile;
        state.remotePath = "/test/path";

        state.sshConnectionInfo.username = null;

        //null username
        try {
            runBuildSCP(state, test);
            fail("Shouldn't succeed");
        } catch (SSHTaskBuilder.BuilderException e) {
            assertEquals("username was not set", e.getMessage());
        }
    }

    public void testBuildSCPKeyPasswordUsername() throws Exception {

        final testState state = new testState();
        final testSCPInterface test = new testSCPInterface();


        state.sshConnectionInfo.authenticationType = SSHTaskBuilder.AuthenticationType.password;
        state.sshConnectionInfo.password = "passwordValue";
        state.sourceFile = testSourcefile;
        state.remotePath = "/test/path";

        state.sshConnectionInfo.username = null;

        //null username
        try {
            runBuildSCP(state, test);
            fail("Shouldn't succeed");
        } catch (SSHTaskBuilder.BuilderException e) {
            assertEquals("username was not set", e.getMessage());
        }
    }

    public void testBuildSCPNoSourcefile() throws Exception {

        final testState state = new testState();
        final testSCPInterface test = new testSCPInterface();

        state.sourceFile = null;
        state.remotePath = "/test/path";

        //null sourceFile
        try {
            runBuildSCP(state, test);
            fail("Shouldn't succeed");
        } catch (SSHTaskBuilder.BuilderException e) {
            assertEquals("sourceFile was not set", e.getMessage());
        }

    }

    public void testBuildSCPNoRemotePath() throws Exception {

        final testState state = new testState();
        final testSCPInterface test = new testSCPInterface();

        state.sourceFile = testSourcefile;
        state.remotePath = null;

        //null remotePath
        try {
            runBuildSCP(state, test);
            fail("Shouldn't succeed");
        } catch (SSHTaskBuilder.BuilderException e) {
            assertEquals("remotePath was not set", e.getMessage());
        }

    }
}

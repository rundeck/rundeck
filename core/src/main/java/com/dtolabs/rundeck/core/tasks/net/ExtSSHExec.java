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

/**
 * This class is a copy of SSHExec from Ant 1.8.1 sources to support RUNDECK specific
 * requirements (e.g., log verbosity).
 *
 *
 * @author Alex Honor <a href="mailto:alex@dtosolutions.com">alex@dtosolutions.com</a>
 * @version $Revision$
 */

package com.dtolabs.rundeck.core.tasks.net;

import com.dtolabs.rundeck.plugins.PluginLogger;
import com.jcraft.jsch.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHBase;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHUserInfo;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.KeepAliveOutputStream;
import org.apache.tools.ant.util.TeeOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executes a command on a remote machine via ssh.
 * @since     Ant 1.6 (created February 2, 2003)
 */
public class ExtSSHExec extends SSHBase implements SSHTaskBuilder.SSHExecInterface {

    private static final int BUFFER_SIZE = 8192;
    private static final int RETRY_INTERVAL = 500;

    /** the command to execute via ssh */
    private String command = null;

    /** units are milliseconds, default is 0=infinite */
    private long maxwait = 0;

    /** for waiting for the command to finish */
    private Thread thread = null;

    private String outputProperty = null;   // like <exec>
    private File outputFile = null;   // like <exec>
    private String inputProperty = null;   // like <exec>
    private File inputFile = null;   // like <exec>
    private boolean append = false;   // like <exec>
    private InputStream inputStream=null;
    private OutputStream secondaryStream=null;
    private DisconnectHolder disconnectHolder=null;
    private PluginLogger logger;

    private Resource commandResource = null;
    private List<Environment.Variable> envVars=null;

    private static final String TIMEOUT_MESSAGE =
        "Timeout period exceeded, connection dropped.";

    /**
     * Constructor for SSHExecTask.
     */
    public ExtSSHExec() {
        super();
    }

    /**
     * Sets the command to execute on the remote host.
     *
     * @param command  The new command value
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * Sets a commandResource from a file
     * @param f the value to use.
     * @since Ant 1.7.1
     */
    public void setCommandResource(String f) {
        this.commandResource = new FileResource(new File(f));
    }

    /**
     * The connection can be dropped after a specified number of
     * milliseconds. This is sometimes useful when a connection may be
     * flaky. Default is 0, which means &quot;wait forever&quot;.
     *
     * @param timeout  The new timeout value in seconds
     */
    public void setTimeout(long timeout) {
        maxwait = timeout;
    }
    public long getTimeout(){
        return maxwait;
    }

    /**
     * If used, stores the output of the command to the given file.
     *
     * @param output  The file to write to.
     */
    public void setOutput(File output) {
        outputFile = output;
    }

    /**
     * If used, the content of the file is piped to the remote command
     *
     * @param input  The file which provides the input data for the remote command
     */
    public void setInput(File input) {
        inputFile = input;
    }

    /**
     * If used, the content of the property is piped to the remote command
     *
     * @param inputProperty  The property which contains the input data for the remote command.
     */
    public void setInputProperty(String inputProperty) {
    	this.inputProperty = inputProperty;
    }

    /**
     * Determines if the output is appended to the file given in
     * <code>setOutput</code>. Default is false, that is, overwrite
     * the file.
     *
     * @param append  True to append to an existing file, false to overwrite.
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * If set, the output of the command will be stored in the given property.
     *
     * @param property  The name of the property in which the command output
     *      will be stored.
     */
    public void setOutputproperty(String property) {
        outputProperty = property;
    }

    private boolean allocatePty = false;
    
    /**
     * Allocate a Pseudo-Terminal.
     * If set true, the SSH connection will be setup to run over an allocated pty.
     * @param b if true, allocate the pty. (default false
     */
    public void setAllocatePty(boolean b) {
        allocatePty = b;
    }

    private int exitStatus =-1;
    /**
     * Return exitStatus of the remote execution, after it has finished or failed.
     * The return value prior to retrieving the result will be -1. If that value is returned
     * after the task has executed, it indicates that an exception was thrown prior to retrieval
     * of the value.
     */
    public int getExitStatus(){
        return exitStatus;
    }
    /**
     * Add an Env element
     * @param env element
     */
    public void addEnv(final Environment.Variable env){
        if(null==envVars) {
            envVars = new ArrayList<Environment.Variable>();
        }
        envVars.add(env);
    }

    /**
     * Return the disconnectHolder
     */
    public DisconnectHolder getDisconnectHolder() {
        return disconnectHolder;
    }

    /**
     * Set a disconnectHolder
     */
    public void setDisconnectHolder(final DisconnectHolder disconnectHolder) {
        this.disconnectHolder = disconnectHolder;
    }

    public PluginLogger getPluginLogger() {
        return logger;
    }

    public void setPluginLogger(PluginLogger logger) {
        this.logger = logger;
    }

    public int getAntLogLevel() {
        return antLogLevel;
    }

    public void setAntLogLevel(int antLogLevel) {
        this.antLogLevel = antLogLevel;
    }

    public Map<String, String> getSshConfig() {
        return sshConfig;
    }

    public InputStream getSshKeyData() {
        return sshKeyData;
    }

    /**
     * Allows disconnecting the ssh connection
     */
    public static interface Disconnectable{
        /**
         * Disconnect
         */
        public void disconnect();
    }

    /**
     * Interface for receiving access to Disconnectable
     */
    public static interface DisconnectHolder{
        /**
         * Set disconnectable
         */
        public void setDisconnectable(Disconnectable disconnectable);
    }

    /**
     * Execute the command on the remote host.
     *
     * @exception BuildException  Most likely a network error or bad parameter.
     */
    public void execute() throws BuildException {
        if (getHost() == null) {
            throw new BuildException("Host is required.");
        }
        if (getUserInfo().getName() == null) {
            throw new BuildException("Username is required.");
        }
        if (getUserInfo().getKeyfile() == null
            && getUserInfo().getPassword() == null
            && getSshKeyData() == null) {
            throw new BuildException("Password or Keyfile is required.");
        }
        if (command == null && commandResource == null) {
            throw new BuildException("Command or commandResource is required.");
        }

        if (inputFile != null && inputProperty != null) {
            throw new BuildException("You can't specify both inputFile and"
                                     + " inputProperty.");
        }
        if (inputFile != null && !inputFile.exists()) {
            throw new BuildException("The input file "
                                     + inputFile.getAbsolutePath()
                                     + " does not exist.");
        }

        Session session = null;
        StringBuffer output = new StringBuffer();
        try {
            session = openSession();

            if(null!=getDisconnectHolder()){
                final Session sub=session;
                getDisconnectHolder().setDisconnectable(new Disconnectable() {
                    public void disconnect() {
                        sub.disconnect();
                    }
                });
            }

            /* called once */
            if (command != null) {
                executeCommand(session, command, output);
            } else { // read command resource and execute for each command
                try {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(commandResource.getInputStream()));
                    String cmd;
                    while ((cmd = br.readLine()) != null) {
                        executeCommand(session, cmd, output);
                        output.append("\n");
                    }
                    FileUtils.close(br);
                } catch (IOException e) {
                    if (getFailonerror()) {
                        throw new BuildException(e);
                    } else {
                        log("Caught exception: " + e.getMessage(),
                            Project.MSG_ERR);
                    }
                }
            }
        } catch (JSchException e) {
            if (getFailonerror()) {
                throw new BuildException(e);
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }
        } finally {
            if (outputProperty != null) {
                getProject().setNewProperty(outputProperty, output.toString());
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void executeCommand(Session session, String cmd, StringBuffer sb)
        throws BuildException {
        final ByteArrayOutputStream out ;
        final OutputStream tee;
        final OutputStream teeout;
        if(null!=outputFile || null!=outputProperty){
            out = new ByteArrayOutputStream();
        }else{
            out=null;
        }
        if(null!=getSecondaryStream() && null!=out) {
            teeout= new TeeOutputStream(out, getSecondaryStream());
        } else if(null!= getSecondaryStream()){
            teeout= getSecondaryStream();
        }else if(null!=out){
            teeout=out;
        }else{
            teeout=null;
        }
        if(null!=teeout){
            tee = new TeeOutputStream(teeout, new KeepAliveOutputStream(System.out));
        }else{
            tee= new KeepAliveOutputStream(System.out);
        }

        InputStream istream = null ;
        if (inputFile != null) {
            try {
                istream = new FileInputStream(inputFile) ;
            } catch (IOException e) {
                // because we checked the existence before, this one
                // shouldn't happen What if the file exists, but there
                // are no read permissions?
                log("Failed to read " + inputFile + " because of: "
                    + e.getMessage(), Project.MSG_WARN);
            }
        }
        if (inputProperty != null) {
            String inputData = getProject().getProperty(inputProperty) ;
            if (inputData != null) {
                istream = new ByteArrayInputStream(inputData.getBytes()) ;
            }        	
        }

        if(getInputStream()!=null){
            istream=getInputStream();
        }

        try {
            final ChannelExec channel;
            session.setTimeout((int) maxwait);
            /* execute the command */
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            // Enable AgentForwarding
            channel.setAgentForwarding(true);
            channel.setOutputStream(tee);
            channel.setExtOutputStream(new KeepAliveOutputStream(System.err), true);
            if (istream != null) {
                channel.setInputStream(istream);
            }
            channel.setPty(allocatePty);

            /* set env vars if any are embedded */
            if(null!=envVars && envVars.size()>0){
                for(final Environment.Variable env:envVars) {
                    channel.setEnv(env.getKey(), env.getValue());
                }
            }
            
            channel.connect();
            // wait for it to finish
            thread =
                new Thread() {
                    public void run() {
                        while (!channel.isClosed()) {
                            if (thread == null) {
                                return;
                            }
                            try {
                                sleep(RETRY_INTERVAL);
                            } catch (Exception e) {
                                // ignored
                            }
                        }
                    }
                };

            thread.start();
            thread.join(maxwait);

            if (thread.isAlive()) {
                // ran out of time
                thread = null;
                if (getFailonerror()) {
                    throw new BuildException(TIMEOUT_MESSAGE);
                } else {
                    log(TIMEOUT_MESSAGE, Project.MSG_ERR);
                }
            } else {
                //success
                if (outputFile != null && null != out) {
                    writeToFile(out.toString(), append, outputFile);
                }

                // this is the wrong test if the remote OS is OpenVMS,
                // but there doesn't seem to be a way to detect it.
                exitStatus = channel.getExitStatus();
                int ec = channel.getExitStatus();
                if (ec != 0) {
                    String msg = "Remote command failed with exit status " + ec;
                    if (getFailonerror()) {
                        throw new BuildException(msg);
                    } else {
                        log(msg, Project.MSG_ERR);
                    }
                }
            }
        } catch (BuildException e) {
            throw e;
        } catch (JSchException e) {
            if (e.getMessage().indexOf("session is down") >= 0) {
                if (getFailonerror()) {
                    throw new BuildException(TIMEOUT_MESSAGE, e);
                } else {
                    log(TIMEOUT_MESSAGE, Project.MSG_ERR);
                }
            } else {
                if (getFailonerror()) {
                    throw new BuildException(e);
                } else {
                    log("Caught exception: " + e.getMessage(),
                        Project.MSG_ERR);
                }
            }
        } catch (Exception e) {
            if (getFailonerror()) {
                throw new BuildException(e);
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }
        } finally {
            if(null!=out){
                sb.append(out.toString());
            }
            FileUtils.close(istream);
        }
    }

    /**
     * Writes a string to a file. If destination file exists, it may be
     * overwritten depending on the "append" value.
     *
     * @param from           string to write
     * @param to             file to write to
     * @param append         if true, append to existing file, else overwrite
     * @exception Exception  most likely an IOException
     */
    private void writeToFile(String from, boolean append, File to)
        throws IOException {
        FileWriter out = null;
        try {
            out = new FileWriter(to.getAbsolutePath(), append);
            StringReader in = new StringReader(from);
            char[] buffer = new char[BUFFER_SIZE];
            int bytesRead;
            while (true) {
                bytesRead = in.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }


    private String knownHosts;

    private InputStream sshKeyData;
    @Override
    public void setSshKeyData(InputStream sshKeyData) {
        this.sshKeyData=sshKeyData;
    }

    /**
     * Sets the path to the file that has the identities of
     * all known hosts.  This is used by SSH protocol to validate
     * the identity of the host.  The default is
     * <i>${user.home}/.ssh/known_hosts</i>.
     *
     * @param knownHosts a path to the known hosts file.
     */
    public void setKnownhosts(String knownHosts) {
        this.knownHosts = knownHosts;
        super.setKnownhosts(knownHosts);
    }

    private Map<String,String> sshConfig;
    @Override
    public void setSshConfig(Map<String, String> config) {
        this.sshConfig=config;
    }


    /**
     * Open an ssh seession.
     *
     * Copied from SSHBase 1.8.1
     * @return the opened session
     * @throws JSchException on error
     */
    protected Session openSession() throws JSchException {
        return SSHTaskBuilder.openSession(this);
    }

    private int antLogLevel=Project.MSG_INFO;

    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Set an inputstream for pty input to the session
     */
    public void setInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public OutputStream getSecondaryStream() {
        return secondaryStream;
    }

    /**
     * Set a secondary outputstream to read from the connection
     */
    public void setSecondaryStream(final OutputStream secondaryStream) {
        this.secondaryStream = secondaryStream;
    }

    @Override
    public String getKeyfile() {
        return getUserInfo().getKeyfile();
    }

    @Override
    public String getKnownhosts() {
        return knownHosts;
    }

    public SSHUserInfo getUserInfo(){
        return super.getUserInfo();
    }
}

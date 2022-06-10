package org.rundeck.plugin.scm.git.ssh

import groovy.transform.CompileStatic
import net.schmizz.keepalive.KeepAliveProvider
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.io.IsolatedOutputStream

@CompileStatic
class SshjSession implements RemoteSession {

    Session session
    SSHClient sshClient
    URIish uri
    private String privateKeyFile
    Map<String, String> sshConfig
    private OpenSshConfig config

    SshjSession(URIish uri, Map<String, String> sshConfig, OpenSshConfig config, String privateKeyFile) {
        this.sshConfig = sshConfig
        this.config = config
        this.privateKeyFile = privateKeyFile
        this.uri = uri

        this.sshClient = createConnection()

    }

    @Override
    Process exec(String commandName, int timeout) throws IOException {
        this.session = sshClient.startSession()
        return new SshjProcess(commandName, timeout)
    }

    @Override
    void disconnect() {
        session.close()
        sshClient.close()
    }

    private SSHClient createConnection(){

        String user = uri.getUser()
        String host = uri.getHost()
        int port = uri.getPort()

        if(config){
            OpenSshConfig.Host hc = config.lookup(host);
            if (port <= 0)
                port = hc.getPort();
            if (user == null)
                user = hc.getUser()
        }

        DefaultConfig defaultConfig = new DefaultConfig()
        defaultConfig.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE)
        SSHClient ssh = new SSHClient(defaultConfig)

        if(sshConfig.get("StrictHostKeyChecking") == "yes"){
            ssh.loadKnownHosts()
        }else{
            ssh.addHostKeyVerifier(new PromiscuousVerifier())
        }

        try{
            if (port != null) {
                ssh.connect(host, port)
            } else {
                ssh.connect(host);
            }

            if(privateKeyFile){
                KeyProvider key = ssh.loadKeys(privateKeyFile)
                ssh.authPublickey(user, key)
            }

            return ssh
        }catch(IOException e){
            throw new TransportException(uri, e.getMessage(), e)
        }
    }


    private class SshjProcess extends Process {

        int timeout
        Session.Command cmd
        InputStream inputStream
        OutputStream outputStream
        InputStream errorStream

        SshjProcess(String commandName, int tms) throws TransportException, IOException {
            try {
                timeout = tms
                cmd = session.exec(commandName)
                setupStreams()
            } catch (Exception e) {
                closeOutputStream()
                throw new TransportException(uri, e.getMessage(), e)
            }
        }

        private boolean isRunning() {
            return cmd.getExitStatus() < 0 && cmd.isOpen()
        }

        @Override
        int waitFor() throws InterruptedException {
            while (isRunning()){
                Thread.sleep(100)
            }
            return exitValue()
        }

        @Override
        int exitValue() {
            if (isRunning()){
                throw new IllegalStateException()
            }
            return cmd.getExitStatus()
        }

        @Override
        void destroy() {
            cmd.close()
            closeOutputStream()
            session.close()
        }

        void setupStreams() throws IOException {
            inputStream = cmd.getInputStream()
            OutputStream out = cmd.getOutputStream()
            if (timeout <= 0) {
                outputStream = out
            } else {
                IsolatedOutputStream i = new IsolatedOutputStream(out)
                outputStream = new BufferedOutputStream(i, 16 * 1024)
            }
            errorStream = cmd.getErrorStream()
        }

        void closeOutputStream() {
            if (outputStream != null) {
                outputStream.close()
            }
        }
    }


}

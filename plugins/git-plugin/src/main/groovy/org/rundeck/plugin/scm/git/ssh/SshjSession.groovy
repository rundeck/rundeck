package org.rundeck.plugin.scm.git.ssh

import groovy.transform.CompileStatic
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.io.IsolatedOutputStream

@CompileStatic
class SshjSession implements RemoteSession {

    Session session
    SSHClient sshClient
    URIish uri

    SshjSession(SSHClient sshClient, URIish uri) {
        this.sshClient = sshClient
        this.uri = uri
    }

    @Override
    Process exec(String commandName, int timeout) throws IOException {
        this.session = sshClient.startSession()
        return new SshjProcess(commandName, timeout)
    }

    @Override
    void disconnect() {
        session.close()
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

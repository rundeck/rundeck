package org.rundeck.plugin.scm.git.ssh

import groovy.transform.CompileStatic
import net.schmizz.keepalive.KeepAliveProvider
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider
import net.schmizz.sshj.userauth.keyprovider.KeyFormat
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil
import net.schmizz.sshj.userauth.password.PasswordFinder
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.io.IsolatedOutputStream

@CompileStatic
class SshjSession implements RemoteSession {

    Session session
    SSHClient sshClient
    URIish uri
    private String privateKey
    Map<String, String> sshConfig
    private OpenSshConfig config

    SshjSession(URIish uri, Map<String, String> sshConfig, OpenSshConfig config, String privateKey) {
        this.sshConfig = sshConfig
        this.config = config
        this.privateKey = privateKey
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
        try {
            session?.close()
        } finally {
            sshClient.close()
        }
    }

    private SSHClient createConnection() {

        String user = uri.getUser()
        String host = uri.getHost()
        int port = uri.getPort()

        if (config) {
            OpenSshConfig.Host hc = config.lookup(host)
            if (port <= 0)
                port = hc.getPort()
            if (user == null)
                user = hc.getUser()
        }

        DefaultConfig defaultConfig = new DefaultConfig()
        defaultConfig.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE)
        SSHClient ssh = new SSHClient(defaultConfig)

        if (sshConfig.get("StrictHostKeyChecking") == "no") {
            ssh.addHostKeyVerifier(new PromiscuousVerifier())
        } else {
            //fail secure: default to strict host key checking unless explicitly disabled
            ssh.loadKnownHosts()
        }

        try {
            if (port != null) {
                ssh.connect(host, port)
            } else {
                ssh.connect(host)
            }

            if (privateKey) {
                KeyFormat format = KeyProviderUtil.detectKeyFileFormat(privateKey, true)
                if (format == null || format == KeyFormat.Unknown) {
                    throw new IOException("Unrecognized or invalid SSH private key format")
                }
                FileKeyProvider keys = Factory.Named.Util.create(ssh.getTransport().getConfig().getFileKeyProviderFactories(), format.toString())
                if (keys == null) {
                    throw new IOException("No key provider available for SSH private key format: ${format}")
                }
                keys.init(new StringReader(privateKey), (PasswordFinder) null)
                ssh.authPublickey(user, keys)
            }

            return ssh
        } catch (IOException e) {
            ssh.close()
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
            while (isRunning()) {
                Thread.sleep(100)
            }
            return exitValue()
        }

        @Override
        int exitValue() {
            if (isRunning()) {
                throw new IllegalStateException()
            }
            return cmd.getExitStatus()
        }

        @Override
        void destroy() {
            if (cmd != null) cmd.close()
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
                try {
                    outputStream.close()
                } catch (IOException ignored) {
                    // Closing/flushing the process output stream after the underlying SSH
                    // channel has already been torn down (see destroy(), which closes the
                    // command before this) can throw - e.g. SSHJ's ChannelOutputStream throws
                    // ConnectionException("Stream closed") if any buffered bytes remain
                    // in the timeout>0 BufferedOutputStream/IsolatedOutputStream wrapper
                    // (see setupStreams()) when the channel is no longer open. At this point
                    // the process/channel is already being abandoned, so a failure to flush
                    // leftover output is not an actionable error. JGit's own reference
                    // RemoteSession implementation (JschSession.JSchProcess) swallows this
                    // exact case the same way; do the same here.
                }
            }
        }
    }


}

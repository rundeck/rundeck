package org.rundeck.plugin.scm.git

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.util.FS

/**
 * Factory using direct private key, which also handles callback via TransportConfigCallback
 */
class PluginSshSessionFactory extends JschConfigSessionFactory implements TransportConfigCallback {
    private byte[] privateKey
    Map<String, String> sshConfig

    PluginSshSessionFactory(final byte[] privateKey) {
        this.privateKey = privateKey
    }

    @Override
    protected void configure(final OpenSshConfig.Host hc, final Session session) {
        if (sshConfig) {
            sshConfig.each { k, v ->
                session.setConfig(k, v)
            }
        }
    }

    @Override
    protected JSch createDefaultJSch(final FS fs) throws JSchException {
        JSch jsch = super.createDefaultJSch(fs)
        jsch.removeAllIdentity()
        jsch.addIdentity("private", privateKey, null, null)
        //todo: explicitly set known host keys?
        return jsch
    }

    @Override
    protected Session createSession(
            final OpenSshConfig.Host hc,
            final String user,
            final String host,
            final int port,
            final FS fs
    ) throws JSchException
    {
        return super.createSession(hc, user, host, port, fs)
    }

    @Override
    void configure(final Transport transport) {
        if (transport instanceof SshTransport) {
            SshTransport sshTransport = (SshTransport) transport
            sshTransport.setSshSessionFactory(this)
        }
    }
}

package org.rundeck.plugin.scm.git.ssh

import groovy.transform.CompileStatic
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS

import java.nio.charset.StandardCharsets


@CompileStatic
class SshjSessionFactory extends SshSessionFactory {
    private byte[] privateKey
    Map<String, String> sshConfig
    private OpenSshConfig config
    //private String privateKeyFile

    private final InetAddress pinnedAddress

    SshjSessionFactory(byte[] privateKey, Map<String, String> sshConfig) {
        this(privateKey, sshConfig, null)
    }

    /**
     * @param privateKey SSH private key material
     * @param sshConfig session options
     * @param pinnedAddress address to open, or {@code null} to let the session resolve the host
     */
    SshjSessionFactory(byte[] privateKey, Map<String, String> sshConfig, InetAddress pinnedAddress) {
        this.privateKey = privateKey
        this.sshConfig = sshConfig
        this.pinnedAddress = pinnedAddress
    }

    @Override
    RemoteSession getSession(URIish uri, CredentialsProvider credentialsProvider, FS fs, int tms) throws TransportException {
        if (config == null)
            config = OpenSshConfig.get(fs)

        String keyContent = new String(privateKey, StandardCharsets.UTF_8)
        return new SshjSession(uri, sshConfig, config, keyContent, pinnedAddress)
    }

    @Override
    String getType() {
        return null
    }
}

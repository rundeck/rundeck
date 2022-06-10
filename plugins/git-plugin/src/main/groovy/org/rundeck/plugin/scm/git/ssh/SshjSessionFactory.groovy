package org.rundeck.plugin.scm.git.ssh

import groovy.transform.CompileStatic
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS


@CompileStatic
class SshjSessionFactory extends SshSessionFactory {
    private byte[] privateKey
    Map<String, String> sshConfig
    private OpenSshConfig config
    private String privateKeyFile

    SshjSessionFactory(byte[] privateKey, Map<String, String> sshConfig) {
        this.privateKey = privateKey
        this.sshConfig = sshConfig

        if(privateKey){
            File tempFile = File.createTempFile("tmp", "key")
            tempFile.deleteOnExit()
            FileOutputStream fos = new FileOutputStream(tempFile)
            fos.write(privateKey)
            privateKeyFile = tempFile.getAbsolutePath()
        }
    }

    @Override
    RemoteSession getSession(URIish uri, CredentialsProvider credentialsProvider, FS fs, int tms) throws TransportException {
        if (config == null)
            config = OpenSshConfig.get(fs)

        return new SshjSession(uri, sshConfig, config, privateKeyFile )
    }


}

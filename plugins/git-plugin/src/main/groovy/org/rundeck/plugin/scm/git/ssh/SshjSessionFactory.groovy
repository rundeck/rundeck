package org.rundeck.plugin.scm.git.ssh

import groovy.transform.CompileStatic
import net.schmizz.keepalive.KeepAliveProvider
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
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
    private SSHClient sshClient

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

        String user = uri.getUser()
        String host = uri.getHost()
        int port = uri.getPort()

        if (config == null)
            config = OpenSshConfig.get(fs)

        final OpenSshConfig.Host hc = config.lookup(host)
        if (port <= 0)
            port = hc.getPort()
        if (user == null)
            user = hc.getUser()

        sshClient = createConnection(user, host,port)
        return new SshjSession(sshClient, uri)
    }

    @Override
    void releaseSession(RemoteSession session) {
        super.releaseSession(session)
        sshClient.close()

    }

    SSHClient createConnection(String user, String host, int port){

        DefaultConfig defaultConfig = new DefaultConfig();
        defaultConfig.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE)
        SSHClient ssh = new SSHClient(defaultConfig)

        if(sshConfig.get("StrictHostKeyChecking") == "yes"){
            ssh.loadKnownHosts()
        }else{
            ssh.addHostKeyVerifier(new PromiscuousVerifier())
        }

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
    }

}

package rundeckapp.init.servlet;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;

public class JettyServletHstsCustomizer implements JettyServerCustomizer {

    Boolean sniHostCheck = false
    long stsMaxAgeSeconds
    Boolean stsIncludeSubdomains


    /**
     * Customizes the given server by adding a secure request customizer for SSL configurations
     *
     * @param server The server to customize.
     */
    @Override
    public void customize(Server server) {
        server.getConnectors().each { connector ->
            HttpConfiguration config = connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration()
            if(checkSSL(config)){
                config.addCustomizer(new SecureRequestCustomizer(sniHostCheck, stsMaxAgeSeconds, stsIncludeSubdomains))
            }
        }
    }

    /**
     * Checks if SSL is enabled in the HTTP configuration.
     *
     * @param httpConfig the HTTP configuration to check
     * @return true if SSL is enabled, false otherwise
     */
    static boolean checkSSL(HttpConfiguration httpConfig) {
        int securePort = httpConfig.getSecurePort()
        boolean isSslEnabled = (securePort > 0)
        return isSslEnabled
    }
}

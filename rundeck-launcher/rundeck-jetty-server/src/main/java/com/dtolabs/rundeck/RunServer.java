/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.plus.jaas.JAASUserRealm;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.*;
import java.util.Properties;

/**
 * Run the jetty server using system properties and commandline input for configuration
 */
public class RunServer {

    public static final String SERVER_HTTP_HOST = "server.http.host";
    int port = Integer.getInteger("server.http.port", 4440);
    int httpsPort = Integer.getInteger("server.https.port", 4443);
    File basedir;
    File serverdir;
    private static final String REALM_NAME = "rundeckrealm";
    private static final String SYS_PROP_LOGIN_MODULE = "loginmodule.name";
    public static final String SYS_PROP_WEB_CONTEXT = "server.web.context";
    File configdir;
    String loginmodulename;
    private boolean useJaas;
    private static final String RUNDECK_JAASLOGIN = "rundeck.jaaslogin";
    public static final String RUNDECK_SSL_CONFIG = "rundeck.ssl.config";
    public static final String RUNDECK_KEYSTORE = "keystore";
    public static final String RUNDECK_KEYSTORE_PASSWORD = "keystore.password";
    public static final String RUNDECK_KEY_PASSWORD = "key.password";
    public static final String RUNDECK_TRUSTSTORE = "truststore";
    public static final String RUNDECK_TRUSTSTORE_PASSWORD = "truststore.password";
    private String keystore;
    private String keystorePassword;
    private String keyPassword;
    private String truststore;
    private String truststorePassword;
    private String appContext;
    private static final String RUNDECK_SERVER_SERVER_DIR = "rundeck.server.serverDir";
    private static final String RUNDECK_SERVER_CONFIG_DIR = "rundeck.server.configDir";

    public static void main(final String[] args) throws Exception {
        new RunServer().run(args);
    }

    public RunServer() {
        useJaas = null == System.getProperty(RUNDECK_JAASLOGIN) || Boolean.getBoolean(RUNDECK_JAASLOGIN);
        loginmodulename = System.getProperty(SYS_PROP_LOGIN_MODULE, "rundecklogin");
        appContext = System.getProperty(SYS_PROP_WEB_CONTEXT, "/");
    }

    /**
     * Run with arguments
     *
     * @param args
     *
     * @throws Exception
     */
    public void run(final String[] args) throws Exception {
        parseArgs(args);
        init();
        if (null != basedir) {
            System.setProperty("rdeck.base", basedir.getAbsolutePath());
        }
        final Server server = new Server();
        if(isSSLEnabled()){
            configureSSLConnector(server);
        }else{
            warnNoSSLConfig();
            configureHTTPConnector(server);
        }

        server.setStopAtShutdown(true);
        final WebAppContext context = createWebAppContext(new File(serverdir, "exp/webapp"));
        server.addHandler(context);
        configureRealms(server);
        try {
            
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(100);
        }
    }

    private void warnNoSSLConfig() {
        System.err.println("WARNING: HTTPS is not enabled, specify -Drundeck.ssl.config="+(basedir.getAbsolutePath().replaceAll("\\\\","/"))+"/server/config/ssl.properties to enable.");
    }

    private boolean isSSLEnabled() {
        if (null == System.getProperty(RUNDECK_SSL_CONFIG)) {
            return false;
        }
        if (null != keystore) {
            if (!new File(keystore).exists()) {
                System.err.println("ERROR: keystore file does not exist, you must create it: " + keystore);
                return false;
            }
        } else {
            System.err.println("ERROR: keystore property not specified: " + System.getProperty(RUNDECK_SSL_CONFIG));
            return false;
        }
        return true;
    }

    private void configureHTTPConnector(final Server server) {
        final SocketConnector connector = new SocketConnector();
        connector.setPort(port);
        connector.setHost(System.getProperty(SERVER_HTTP_HOST, null));
        server.addConnector(connector);
    }

    private void configureSSLConnector(final Server server) {
        //configure ssl
        final SslSocketConnector connector = new SslSocketConnector();
        connector.setPort(httpsPort);
        connector.setMaxIdleTime(30000);
        connector.setKeystore(keystore);
        connector.setPassword(keystorePassword);
        connector.setKeyPassword(keyPassword);
        connector.setTruststore(truststore);
        connector.setTrustPassword(truststorePassword);
        connector.setHost(System.getProperty(SERVER_HTTP_HOST, null));
        server.addConnector(connector);
    }

    /**
     * Configure jetty realm.  if system property "rundeck.jaaslogin" is false, then use a simple HashRealm, otherwise
     * use a JAAS realm.
     *
     * @param server
     *
     * @throws IOException
     */
    private void configureRealms(final Server server) throws IOException {
        if (useJaas) {
            configureJAASRealms(server);
        } else {
            configureHashRealms(server);
        }
    }

    /**
     * Configure HashUserRealm for realm.properties in the config dir
     *
     * @param server
     *
     * @throws IOException
     */
    private void configureHashRealms(final Server server) throws IOException {
        final HashUserRealm realm = new HashUserRealm();
        realm.setName(REALM_NAME);
        final File conffile = new File(configdir, "realm.properties");
        realm.setConfig(conffile.getAbsolutePath());
        server.addUserRealm(realm);
    }

    /**
     * Configure JAAS realm using login module name "rundecklogin"
     *
     * @param server
     */
    private void configureJAASRealms(final Server server) {
        final JAASUserRealm realm = new JAASUserRealm();
        realm.setCallbackHandlerClass("org.mortbay.jetty.plus.jaas.callback.DefaultCallbackHandler");
        realm.setName(REALM_NAME);
        realm.setLoginModuleName(loginmodulename);
        server.addUserRealm(realm);
    }

    /**
     * Create the webapp context for the given path
     *
     * @param webapp webapp path
     *
     * @return
     *
     * @throws IOException
     */
    private WebAppContext createWebAppContext(final File webapp) throws IOException {
        if (!webapp.isDirectory() || !new File(webapp, "WEB-INF").isDirectory()) {
            throw new RuntimeException("expected expanded webapp at location: " + webapp.getAbsolutePath());
        }
        final WebAppContext context = new WebAppContext(webapp.getAbsolutePath(), appContext);
        context.setTempDirectory(new File(serverdir, "work"));
        return context;
    }

    /**
     * init values based on provided system properties or arguments. System property "rundeck.server.serverDir" sets the
     * server location, defaults to basedir/server. "rundeck.server.configDir" sets the config dir location, defaults to
     * serverdir/config
     */
    private void init() {
        if (null != System.getProperty(RUNDECK_SERVER_SERVER_DIR)) {
            serverdir = new File(System.getProperty(RUNDECK_SERVER_SERVER_DIR));
        } else {
            serverdir = new File(basedir, "server");
        }
        if (null != System.getProperty(RUNDECK_SERVER_CONFIG_DIR)) {
            configdir = new File(System.getProperty(RUNDECK_SERVER_CONFIG_DIR));
        } else {
            configdir = new File(serverdir, "config");
        }
        if(null!=System.getProperty(RUNDECK_SSL_CONFIG)){
            final Properties sslProperties = new Properties();
            try{
                sslProperties.load(new FileInputStream(System.getProperty(RUNDECK_SSL_CONFIG)));
            } catch (IOException e) {
                System.err.println("Could not load specified rundeck.ssl.config file: " + System.getProperty(
                    RUNDECK_SSL_CONFIG) + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
            keystore = sslProperties.getProperty(RUNDECK_KEYSTORE);
            keystorePassword = sslProperties.getProperty(RUNDECK_KEYSTORE_PASSWORD);
            keyPassword = sslProperties.getProperty(RUNDECK_KEY_PASSWORD);
            truststore = sslProperties.getProperty(RUNDECK_TRUSTSTORE);
            truststorePassword = sslProperties.getProperty(RUNDECK_TRUSTSTORE_PASSWORD);
        }
    }

    /**
     * Parse CLI arguments as input options.  current usage: &lt;basedir&gt; [port]
     *
     * @param args
     */
    private void parseArgs(final String[] args) {
        if (args.length > 0) {
            basedir = new File(args[0]);
        } else {
            throw new RuntimeException("Basedir argument required");
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            httpsPort = Integer.parseInt(args[2]);
        }
    }
}
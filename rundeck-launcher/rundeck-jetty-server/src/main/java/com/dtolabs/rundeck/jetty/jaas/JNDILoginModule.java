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

/*
* JNDILoginModule.java
* 
* User: greg
* Created: Apr 2, 2008 3:54:50 PM
* $Id$
*/
package com.dtolabs.rundeck.jetty.jaas;

import org.apache.log4j.Logger;
import org.eclipse.jetty.plus.jaas.callback.ObjectCallback;
import org.eclipse.jetty.plus.jaas.spi.AbstractLoginModule;
import org.eclipse.jetty.plus.jaas.spi.UserInfo;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.security.Credential;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;


/**
 * JNDILoginModule provides jndi based login 
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class JNDILoginModule extends AbstractLoginModule {
    private static final org.eclipse.jetty.util.log.Logger log = Log.getLogger(JNDILoginModule.class);

    /**
     * Get the UserInfo for a specified username
     * @param username username
     * @return the UserInfo
     * @throws Exception
     */
    public UserInfo getUserInfo(String username) throws Exception {
        DirContext dir = context();
        ArrayList roleList = new ArrayList(getUserRoles(username));
        String credentials = getUserCredentials(username);
        return new UserInfo(username, Credential.getCredential(credentials), roleList);

    }


    public static final String DEFAULT_FILENAME = "rundeck-jndi.properties";


    private HashMap fileMap = new HashMap();
    private String propertyFileName;

    public static final String CONNECTION_NAME_PROP = "rundeck.auth.jndi.connectionName";
    public static final String CONNECTION_PASS_PROP = "rundeck.auth.jndi.connectionPassword";
    public static final String CONNECTION_URL_PROP = "rundeck.auth.jndi.connectionUrl";
    public static final String CONNECTION_AUTH_TYPE = "rundeck.auth.jndi.authType";
    public static final String ROLEBASE_PROP = "rundeck.auth.jndi.roleBase";
    public static final String ROLENAMERDN_PROP = "rundeck.auth.jndi.roleNameRDN";
    public static final String ROLEMEMBERRDN_PROP = "rundeck.auth.jndi.roleMemberRDN";
    public static final String USERBASE_PROP = "rundeck.auth.jndi.userBase";
    public static final String USERNAMERDN_PROP = "rundeck.auth.jndi.userNameRDN";
    /**
    //from itnav
    /**
     * role name prefix for project roles: {@value}
     */
    public static final String PROJECT_ROLE_NAME_PREFIX = "projectRole-";

    private String connectionUrl;
    private String connectionName;
    private String connectionPassword;
    private String connectionAuth;
    private String roleBase;
    private String roleNameRDN;
    private String roleMemberRDN;
    private String userBase;
    private String userNameRDN;

    private DirContext initialDirContext;

    public static final String RESTRICTED_PROJECTS_ROLE = "restrictedProjects";


    /**
     * Read contents of the configured property file.
     *
     * @param subject
     * @param callbackHandler
     * @param sharedState
     * @param options
     *
     * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject,
     *      javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map sharedState, Map options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        Properties props = loadProperties((String) options.get("file"));
        initWithProps(props);
    }

    private void initWithProps(Properties props) {
        connectionUrl=props.getProperty(CONNECTION_URL_PROP);
        connectionName = props.getProperty(CONNECTION_NAME_PROP);
        connectionPassword = props.getProperty(CONNECTION_PASS_PROP);
        connectionAuth = props.containsKey(CONNECTION_AUTH_TYPE) ? props.getProperty(CONNECTION_AUTH_TYPE) : "simple";
        roleBase = props.getProperty(ROLEBASE_PROP);
        roleNameRDN = props.getProperty(ROLENAMERDN_PROP);
        roleMemberRDN = props.getProperty(ROLEMEMBERRDN_PROP);
        userBase = props.getProperty(USERBASE_PROP);
        userNameRDN = props.getProperty(USERNAMERDN_PROP);
    }

    private synchronized DirContext context() throws NamingException {
        if (null == initialDirContext) {

            Hashtable env = new Hashtable(11);

            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, connectionUrl);
            env.put(Context.SECURITY_PRINCIPAL, connectionName);
            env.put(Context.SECURITY_CREDENTIALS, connectionPassword);

            this.initialDirContext = new InitialDirContext(env);
        }
        return initialDirContext;
    }

    private synchronized boolean authbindContext(String userName, String pass) throws NamingException {


        String userDN = userNameRDN + "=" + userName + "," + userBase;

        Hashtable env = new Hashtable(11);

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, connectionUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userDN);
        env.put(Context.SECURITY_CREDENTIALS, pass);
        log.debug("attempting bind with userDN: " + userDN);
        DirContext ctx = new InitialDirContext(env);
        try {

            ctx.lookup(userDN);
            ctx.close();
            log.debug("bind succeeded");
            return true;
        } catch (NamingException e) {
            log.debug("bind failed", e);
            e.printStackTrace();
        }
        return false;
    }

    public boolean login() throws LoginException {
        if ("simple".equals(connectionAuth)) {
            return super.login();
        }else if ("bind".equals(connectionAuth)) {
            log.debug("login using bind");
            try {
                if (getCallbackHandler() == null) {
                    throw new LoginException("No callback handler");
                }

                Callback[] callbacks = configureCallbacks();
                getCallbackHandler().handle(callbacks);

                String webUserName = ((NameCallback) callbacks[0]).getName();
                Object webCredential = ((ObjectCallback) callbacks[1]).getObject();

                if ((webUserName == null) || (webCredential == null)) {
                    setAuthenticated(false);
                    return isAuthenticated();
                }
                ArrayList roleList = new ArrayList(getUserRoles(webUserName));
                UserInfo userInfo= new UserInfo(webUserName, null, roleList);

                log.debug("userRoles: " + roleList);
                if (userInfo == null) {
                    setAuthenticated(false);
                    return isAuthenticated();
                }

                setCurrentUser(new JAASUserInfo(userInfo));
                //bind with user info
                setAuthenticated(authbindContext(webUserName, (String) webCredential));
                log.debug("login returning: isAuthenticated? " + isAuthenticated());

                return isAuthenticated();
            }
            catch (IOException e) {
                log.warn(e);
                throw new LoginException(e.toString());
            }
            catch (UnsupportedCallbackException e) {
                log.warn(e);
                throw new LoginException(e.toString());
            }
            catch (Exception e) {
                log.warn(e);
                throw new LoginException(e.toString());
            }
        } else {
            throw new IllegalStateException(CONNECTION_AUTH_TYPE + " was an unrecognized value: " + connectionAuth);
        }
    }

    private Properties loadProperties(String filename) {
        File propsFile;

        if (filename == null) {
            propsFile = new File(System.getProperty("jetty.home"), DEFAULT_FILENAME);
        } else {
            propsFile = new File(filename);
            if (!propsFile.exists()) {
                propsFile = new File(System.getProperty("jetty.home"), filename);
            }
        }

        //give up, can't find a property file to load
        if (!propsFile.exists()) {
            log.warn("No property file found: " + propsFile.getAbsolutePath());
            throw new IllegalStateException("No property file specified in login module configuration file, or it does not exist");
        }


        try {
            this.propertyFileName = propsFile.getCanonicalPath();
            if (fileMap.get(propertyFileName) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Properties file " + propertyFileName + " already in cache, skipping load");
                }
                return (Properties) fileMap.get(propertyFileName);
            }

            Properties props = new Properties();
            props.load(new FileInputStream(propsFile));

            fileMap.put(propertyFileName, props);
            return props;
        }
        catch (Exception e) {
            log.warn("Error loading properties from file", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * Get the collection of role names for the user
     *
     * @param userName name of the user
     *
     * @return Collection of String
     *
     * @throws javax.naming.NamingException
     */
    private Collection getUserRoles(String userName) throws NamingException {
        log.debug("Obtaining roles for userName: " + userName);

        // filter expression: all roles with uniqueMember matching user DN
        // (uniqueMember=cn=userName,dc=company,dc=com)
        String filter = "(" + roleMemberRDN + "=" + userNameRDN + "=" + userName + "," + userBase + ")";

        //search directory
        NamingEnumeration roleResults = this.search(roleBase,
                                                    filter,
                                                    new String[]{
                                                        roleMemberRDN
                                                    });
        ArrayList results = new ArrayList();
        while (roleResults != null && roleResults.hasMore()) {

            SearchResult roleResult = (SearchResult) roleResults.next();

            String roleResultName = roleResult.getName();
            if ("".equals(roleResultName)) {
                //logger.debug("ignoring empty result");
                continue;
            }
            String roleResultValue = (roleResultName.split("="))[1];
            results.add(roleResultValue);
        }
        return results;
    }


    private String getUserCredentials(String userName) throws NamingException{
        log.debug("Obtaining credentials for userName: " + userName);
        DirContext dirContext = context();
        String userDN = userNameRDN + "=" + userName + "," + userBase;
        Attributes roleAttrs = dirContext.getAttributes(userDN);
        Attribute unique = roleAttrs.get("userPassword");
        Object o = unique.get();
        if(o instanceof byte[]) {
            String p = new String((byte[]) o);
            return p;
        }else if(o instanceof String){
            return (String) o;
        }else {
            throw new NamingException("unexpected datatype for password field");
        }
    }
    /**
     * Return a Map of usernames to role sets.
     *
     * @return Mapping of usernames (String) to set of roles (Set of String)
     *
     * @throws javax.naming.NamingException
     */
    private Map getUsers() throws NamingException {

        String filter = "(" + roleNameRDN + "=*)";

        HashMap users = new HashMap();

        NamingEnumeration results = this.search(roleBase, filter, new String[]{roleMemberRDN});
        while (results != null && results.hasMore()) {

            SearchResult roleResult = (SearchResult) results.next();

            String roleResultName = roleResult.getName();
            if ("".equals(roleResultName)) {
                continue;
            }

            String roleResultValue = (roleResultName.split("="))[1];

            Attributes roleAttrs = roleResult.getAttributes();

            Attribute roleAttr = roleAttrs.get(roleMemberRDN);
            NamingEnumeration valueEnum = roleAttr.getAll();
            while (valueEnum != null && valueEnum.hasMore()) {
                String value = (String) valueEnum.next();

                String name;
                if (value.endsWith("," + userBase)) {
                    name = value.substring(0, value.length() - userBase.length() - 1);
                    name = name.split("=")[1];
                } else {
                    log.debug("found unrecognized DN: " + value);
                    continue;
                }

                if (users.containsKey(name)) {
                    HashSet roles = (HashSet) users.get(name);
                    roles.add(roleResultValue);
                } else {
                    HashSet roles = new HashSet();
                    roles.add(roleResultValue);
                    users.put(name, roles);
                }
            }
        }
        return users;

    }


    // search using search base, filter, and declared return attributes
    private NamingEnumeration search(String base, String filter,
                                     String[] returnAttrs)
        throws NamingException {

        SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        constraints.setReturningAttributes(returnAttrs);

        // search the directory based on base, filter, and contraints
        //logger.debug("searching, base: " + base + " using filter: " + filter);
        DirContext dirContext = context();
        // get established jndi connection
        return dirContext.search(base, filter, constraints);
    }
}

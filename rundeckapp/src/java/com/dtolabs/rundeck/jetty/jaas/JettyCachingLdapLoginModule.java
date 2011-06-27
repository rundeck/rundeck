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

package com.dtolabs.rundeck.jetty.jaas;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.mortbay.jetty.plus.jaas.callback.ObjectCallback;
import org.mortbay.jetty.plus.jaas.spi.AbstractLoginModule;
import org.mortbay.jetty.plus.jaas.spi.UserInfo;
import org.mortbay.jetty.security.Credential;
import org.mortbay.log.Log;

/**
 * 
 * A LdapLoginModule for use with JAAS setups
 * 
 * The jvm should be started with the following parameter: <br>
 * <br>
 * <code>
* -Djava.security.auth.login.config=etc/ldap-loginModule.conf
* </code> <br>
 * <br>
 * and an example of the ldap-loginModule.conf would be: <br>
 * <br>
 * 
 * <pre>
 * ldaploginmodule {
 *    com.dtolabs.rundeck.jetty.jaas.JettyCachingLdapLoginModule required
 *    debug="true"
 *    contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
 *    hostname="ldap.example.com"
 *    port="389"
 *    bindDn="cn=Directory Manager"
 *    bindPassword="directory"
 *    authenticationMethod="simple"
 *    forceBindingLogin="false"
 *    forceBindingLoginUseRootContextForRoles="false"
 *    userBaseDn="ou=people,dc=alcatel"
 *    userRdnAttribute="uid"
 *    userIdAttribute="uid"
 *    userPasswordAttribute="userPassword"
 *    userObjectClass="inetOrgPerson"
 *    roleBaseDn="ou=groups,dc=example,dc=com"
 *    roleNameAttribute="cn"
 *    roleMemberAttribute="uniqueMember"
 *    roleObjectClass="groupOfUniqueNames"
 *    rolePrefix="rundeck"
 *    cacheDurationMillis="500"
 *    reportStatistics="true";
 *    };
 * </pre>
 * 
 * @author Jesse McConnell <jesse@codehaus.org>
 * @author Frederic Nizery <frederic.nizery@alcatel-lucent.fr>
 * @author Trygve Laugstol <trygvis@codehaus.org>
 * @author Noah Campbell <noahcampbell@gmail.com>
 */
public class JettyCachingLdapLoginModule extends AbstractLoginModule {

    /**
     * Provider URL
     */
    private String _providerUrl;
    
    /**
     * Role prefix to remove from ldap group name.
     */
    private String _rolePrefix = "";

    /**
     * Duration of storing the user in memory.
     */
    private int _cacheDuration = 0;

    /**
     * hostname of the ldap server
     */
    private String _hostname;

    /**
     * port of the ldap server
     */
    private int _port = 389;

    /**
     * Context.SECURITY_AUTHENTICATION
     */
    private String _authenticationMethod;

    /**
     * Context.INITIAL_CONTEXT_FACTORY
     */
    private String _contextFactory;

    /**
     * root DN used to connect to
     */
    private String _bindDn;

    /**
     * password used to connect to the root ldap context
     */
    private String _bindPassword;

    /**
     * object class of a user
     */
    private String _userObjectClass = "inetOrgPerson";

    /**
     * attribute that the principal is located
     */
    private String _userRdnAttribute = "uid";

    /**
     * attribute that the principal is located
     */
    private String _userIdAttribute = "cn";

    /**
     * name of the attribute that a users password is stored under
     * <p/>
     * NOTE: not always accessible, see force binding login
     */
    private String _userPasswordAttribute = "userPassword";

    /**
     * base DN where users are to be searched from
     */
    private String _userBaseDn;

    /**
     * base DN where role membership is to be searched from
     */
    private String _roleBaseDn;

    /**
     * object class of roles
     */
    private String _roleObjectClass = "groupOfUniqueNames";

    /**
     * name of the attribute that a username would be under a role class
     */
    private String _roleMemberAttribute = "uniqueMember";

    /**
     * the name of the attribute that a role would be stored under
     */
    private String _roleNameAttribute = "roleName";

    private boolean _debug;

    /**
     * if the getUserInfo can pull a password off of the user then password
     * comparison is an option for authn, to force binding login checks, set
     * this to true
     */
    private boolean _forceBindingLogin = false;

    /**
     * if _forceFindingLogin is true, and _forceBindingLoginUseRootContextForRoles
     * is true, then role memberships are obtained using _rootContext
     */
    private boolean _forceBindingLoginUseRootContextForRoles = false;

    private DirContext _rootContext;

    private boolean _reportStatistics;

    private static final ConcurrentHashMap<String, CachedUserInfo> USERINFOCACHE = 
        new ConcurrentHashMap<String, CachedUserInfo>();
    
    /**
     * The number of cache hits for UserInfo objects.
     */
    private static long userInfoCacheHits;
    
    /**
     * The number of login attempts for this particular module.
     */
    private static long loginAttempts;

    /**
     * get the available information about the user
     * <p/>
     * for this LoginModule, the credential can be null which will result in a
     * binding ldap authentication scenario
     * <p/>
     * roles are also an optional concept if required
     * 
     * @param username
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public UserInfo getUserInfo(String username) throws Exception {
        
        String pwdCredential = getUserCredentials(username);

        if (pwdCredential == null) {
            return null;
        }

        pwdCredential = convertCredentialLdapToJetty(pwdCredential);

        Credential credential = Credential.getCredential(pwdCredential);
        List roles = getUserRoles(_rootContext, username);

        return new UserInfo(username, credential, roles);
    }

    protected String doRFC2254Encoding(String inputString) {
        StringBuffer buf = new StringBuffer(inputString.length());
        for (int i = 0; i < inputString.length(); i++) {
            char c = inputString.charAt(i);
            switch (c) {
            case '\\':
                buf.append("\\5c");
                break;
            case '*':
                buf.append("\\2a");
                break;
            case '(':
                buf.append("\\28");
                break;
            case ')':
                buf.append("\\29");
                break;
            case '\0':
                buf.append("\\00");
                break;
            default:
                buf.append(c);
                break;
            }
        }
        return buf.toString();
    }

    /**
     * attempts to get the users credentials from the users context
     * <p/>
     * NOTE: this is not an user authenticated operation
     * 
     * @param username
     * @return
     * @throws LoginException
     */
    @SuppressWarnings("unchecked")
    private String getUserCredentials(String username) throws LoginException {
        String ldapCredential = null;

        SearchControls ctls = new SearchControls();
        ctls.setCountLimit(1);
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = "(&(objectClass={0})({1}={2}))";

//        Log.debug("Searching for users with filter: \'" + filter + "\'" + " from base dn: " + _userBaseDn);

        try {
            Object[] filterArguments = { _userObjectClass, _userIdAttribute, username };
            NamingEnumeration results = _rootContext.search(_userBaseDn, filter, filterArguments,
                    ctls);

            Log.debug("Found user?: " + results.hasMoreElements());

            if (!results.hasMoreElements()) {
                throw new LoginException("User not found.");
            }

            SearchResult result = findUser(username);

            Attributes attributes = result.getAttributes();

            Attribute attribute = attributes.get(_userPasswordAttribute);
            if (attribute != null) {
                try {
                    byte[] value = (byte[]) attribute.get();

                    ldapCredential = new String(value);
                } catch (NamingException e) {
                    Log.debug("no password available under attribute: " + _userPasswordAttribute);
                }
            }
        } catch (NamingException e) {
            throw new LoginException("Root context binding failure.");
        }

        Log.debug("user cred is: " + ldapCredential);

        return ldapCredential;
    }

    /**
     * attempts to get the users roles from the root context
     * <p/>
     * NOTE: this is not an user authenticated operation
     * 
     * @param dirContext
     * @param username
     * @return
     * @throws LoginException
     */
    @SuppressWarnings("unchecked")
    private List getUserRoles(DirContext dirContext, String username) throws LoginException,
            NamingException {
        String userDn = _userRdnAttribute + "=" + username + "," + _userBaseDn;

        return getUserRolesByDn(dirContext, userDn);
    }

    @SuppressWarnings("unchecked")
    private List getUserRolesByDn(DirContext dirContext, String userDn) throws LoginException,
            NamingException {
        ArrayList roleList = new ArrayList();

        if (dirContext == null || _roleBaseDn == null || _roleMemberAttribute == null
                || _roleObjectClass == null) {
            return roleList;
        }

        SearchControls ctls = new SearchControls();
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = "(&(objectClass={0})({1}={2}))";
        Object[] filterArguments = { _roleObjectClass, _roleMemberAttribute, userDn };
        NamingEnumeration results = dirContext.search(_roleBaseDn, filter, filterArguments, ctls);

        Log.debug("Found user roles?: " + results.hasMoreElements());

        while (results.hasMoreElements()) {
            SearchResult result = (SearchResult) results.nextElement();

            Attributes attributes = result.getAttributes();

            if (attributes == null) {
                continue;
            }

            Attribute roleAttribute = attributes.get(_roleNameAttribute);

            if (roleAttribute == null) {
                continue;
            }

            NamingEnumeration roles = roleAttribute.getAll();
            while (roles.hasMore()) {
                if (_rolePrefix != null && !"".equalsIgnoreCase(_rolePrefix)) {
                    String role = (String) roles.next();
                    // Log.info("Role for user " + userDn + ": " + role); 
                    roleList.add(role.replace(_rolePrefix, ""));
                } else {
                    roleList.add(roles.next());
                }
            }
        }

        return roleList;
    }

    /**
     * since ldap uses a context bind for valid authentication checking, we
     * override login()
     * <p/>
     * if credentials are not available from the users context or if we are
     * forcing the binding check then we try a binding authentication check,
     * otherwise if we have the users encoded password then we can try
     * authentication via that mechanic
     * 
     * @return
     * @throws LoginException
     */
    public boolean login() throws LoginException {
        try {
            if (getCallbackHandler() == null) {
                throw new LoginException("No callback handler");
            }

            Callback[] callbacks = configureCallbacks();
            getCallbackHandler().handle(callbacks);

            String webUserName = ((NameCallback) callbacks[0]).getName();
            Object webCredential = ((ObjectCallback) callbacks[1]).getObject();

            if (webUserName == null || webCredential == null) {
                setAuthenticated(false);
                return isAuthenticated();
            }

            loginAttempts++;
            
            if(_reportStatistics)
            {
                DecimalFormat percentHit = new DecimalFormat("#.##");
                Log.info("Login attempts: " + loginAttempts + ", Hits: " + userInfoCacheHits + 
                        ", Ratio: " + percentHit.format((double)userInfoCacheHits / loginAttempts * 100f) + "%.");
            }
            
            if (_forceBindingLogin) {
                return bindingLogin(webUserName, webCredential);
            }

            // This sets read and the credential
            UserInfo userInfo = getUserInfo(webUserName);

            if (userInfo == null) {
                setAuthenticated(false);
                return false;
            }

            setCurrentUser(new JAASUserInfo(userInfo));

            if (webCredential instanceof String) {
                return credentialLogin(Credential.getCredential((String) webCredential));
            }

            return credentialLogin(webCredential);
        } catch (UnsupportedCallbackException e) {
            throw new LoginException("Error obtaining callback information.");
        } catch (IOException e) {
            if (_debug) {
                e.printStackTrace();
            }
            throw new LoginException("IO Error performing login.");
        } catch (Exception e) {
            if (_debug) {
                e.printStackTrace();
            }
            throw new LoginException("Error obtaining user info.");
        }
    }

    /**
     * password supplied authentication check
     * 
     * @param webCredential
     * @return
     * @throws LoginException
     */
    protected boolean credentialLogin(Object webCredential) throws LoginException {
        setAuthenticated(getCurrentUser().checkCredential(webCredential));
        return isAuthenticated();
    }

    /**
     * binding authentication check This methode of authentication works only if
     * the user branch of the DIT (ldap tree) has an ACI (acces control
     * instruction) that allow the access to any user or at least for the user
     * that logs in.
     * 
     * @param username
     * @param password
     * @return
     * @throws LoginException
     */
    @SuppressWarnings("unchecked")
    protected boolean bindingLogin(String username, Object password) throws LoginException,
            NamingException {
        
        if(_cacheDuration > 0) { // only worry about caching if there is a cacheDuration set.
            CachedUserInfo cached = USERINFOCACHE.get(username);
            if (cached != null) {       
                if(System.currentTimeMillis() < cached.expires) {
                    Log.debug("Cache Hit for " + username + ".");
                    userInfoCacheHits++;
                    
                    setCurrentUser(new JAASUserInfo(cached.userInfo));
                    setAuthenticated(true);
                    return true;   
                } else {
                    Log.info("Cache Eviction for " + username + ".");
                    USERINFOCACHE.remove(username);
                }
            } else {
                Log.debug("Cache Miss for " + username + ".");
            }
        }
        
        SearchResult searchResult = findUser(username);

        String userDn = searchResult.getNameInNamespace();

        Log.info("Attempting authentication: " + userDn);

        Hashtable environment = getEnvironment();
        environment.put(Context.SECURITY_PRINCIPAL, userDn);
        environment.put(Context.SECURITY_CREDENTIALS, password);

        DirContext dirContext = new InitialDirContext(environment);

	// use _rootContext to find roles, if configured to doso
	if ( _forceBindingLoginUseRootContextForRoles ) {
	    dirContext = _rootContext;
	    Log.debug("Using _rootContext for role lookup.");
	}
        List roles = getUserRolesByDn(dirContext, userDn);

        UserInfo userInfo = new UserInfo(username, null, roles);
        if(_cacheDuration > 0) {
            USERINFOCACHE.put(username, new CachedUserInfo(userInfo, System.currentTimeMillis() + _cacheDuration));
            Log.debug("Adding " + username + " set to expire: " + System.currentTimeMillis() + _cacheDuration);
        }
        setCurrentUser(new JAASUserInfo(userInfo));
        setAuthenticated(true);
        return true;
    }

    @SuppressWarnings("unchecked")
    private SearchResult findUser(String username) throws NamingException, LoginException {
        SearchControls ctls = new SearchControls();
        ctls.setCountLimit(1);
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = "(&(objectClass={0})({1}={2}))";

        Log.debug("Searching for users with filter: \'" + filter + "\'" + " from base dn: "
                + _userBaseDn);

        Object[] filterArguments = new Object[] { _userObjectClass, _userIdAttribute, username };
        NamingEnumeration results = _rootContext.search(_userBaseDn, filter, filterArguments, ctls);

        Log.debug("Found user?: " + results.hasMoreElements());

        if (!results.hasMoreElements()) {
            throw new LoginException("User not found.");
        }

        return (SearchResult) results.nextElement();
    }

    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState,
            Map options) {
        super.initialize(subject, callbackHandler, sharedState, options);

        _hostname = (String) options.get("hostname");
        if(options.containsKey("port")) {
            _port = Integer.parseInt((String) options.get("port"));   
        }
        _providerUrl = (String) options.get("providerUrl");
        _contextFactory = (String) options.get("contextFactory");
        _bindDn = (String) options.get("bindDn");
        _bindPassword = (String) options.get("bindPassword");
        _authenticationMethod = (String) options.get("authenticationMethod");

        _userBaseDn = (String) options.get("userBaseDn");

        _roleBaseDn = (String) options.get("roleBaseDn");

        if (options.containsKey("forceBindingLogin")) {
            _forceBindingLogin = Boolean.parseBoolean((String) options.get("forceBindingLogin"));
        }

        if (options.containsKey("forceBindingLoginUseRootContextForRoles")) {
            _forceBindingLoginUseRootContextForRoles = Boolean.parseBoolean((String) options.get("forceBindingLoginUseRootContextForRoles"));
        }

        _userObjectClass = getOption(options, "userObjectClass", _userObjectClass);
        _userRdnAttribute = getOption(options, "userRdnAttribute", _userRdnAttribute);
        _userIdAttribute = getOption(options, "userIdAttribute", _userIdAttribute);
        _userPasswordAttribute = getOption(options, "userPasswordAttribute", _userPasswordAttribute);
        _roleObjectClass = getOption(options, "roleObjectClass", _roleObjectClass);
        _roleMemberAttribute = getOption(options, "roleMemberAttribute", _roleMemberAttribute);
        _roleNameAttribute = getOption(options, "roleNameAttribute", _roleNameAttribute);
        _debug = Boolean.parseBoolean(String.valueOf(getOption(options, "debug", Boolean
                .toString(_debug))));

        _rolePrefix = (String) options.get("rolePrefix");
        
        _reportStatistics = Boolean.parseBoolean(String.valueOf(getOption(options, "reportStatistics", Boolean
                .toString(_reportStatistics))));

        String cacheDurationSetting = (String) options.get("cacheDurationMillis");
        if (cacheDurationSetting != null) {
            try {
                _cacheDuration = Integer.parseInt(cacheDurationSetting);
            } catch (NumberFormatException e) {
                Log.warn("Unable to parse cacheDurationMillis to a number: " + cacheDurationSetting,
                        ". Using default: " + _cacheDuration, e);
            }
        }

        try {
            _rootContext = new InitialDirContext(getEnvironment());
        } catch (NamingException ex) {
            throw new IllegalStateException("Unable to establish root context", ex);
        }
    }

    public boolean commit() throws LoginException {
        try {
            _rootContext.close();
        } catch (NamingException e) {
            throw new LoginException("error closing root context: " + e.getMessage());
        }

        return super.commit();
    }

    public boolean abort() throws LoginException {
        try {
            _rootContext.close();
        } catch (NamingException e) {
            throw new LoginException("error closing root context: " + e.getMessage());
        }

        return super.abort();
    }

    @SuppressWarnings("unchecked")
    private String getOption(Map options, String key, String defaultValue) {
        Object value = options.get(key);

        if (value == null) {
            return defaultValue;
        }

        return (String) value;
    }

    /**
     * get the context for connection
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public Hashtable getEnvironment() {
        Properties env = new Properties();

        env.put(Context.INITIAL_CONTEXT_FACTORY, _contextFactory);
        if(_providerUrl != null) {
            env.put(Context.PROVIDER_URL, _providerUrl);
        } else {
            if (_hostname != null) {
                String url = "ldap://" + _hostname + "/";
                if (_port != 0) {
                    url += ":" + _port + "/";
                } 
            
                Log.warn("Using hostname and port.  Use providerUrl instead: " + url);
                env.put(Context.PROVIDER_URL, url);
            }
        }
        
        if (_authenticationMethod != null) {
            env.put(Context.SECURITY_AUTHENTICATION, _authenticationMethod);
        }

        if (_bindDn != null) {
            env.put(Context.SECURITY_PRINCIPAL, _bindDn);
        }

        if (_bindPassword != null) {
            env.put(Context.SECURITY_CREDENTIALS, _bindPassword);
        }

        return env;
    }


    private static String convertCredentialLdapToJetty(String encryptedPassword) {
        if (encryptedPassword == null) {
            return encryptedPassword;
        }

        if ("{MD5}".startsWith(encryptedPassword.toUpperCase())) {
            return "MD5:"
                    + encryptedPassword.substring("{MD5}".length(), encryptedPassword.length());
        }

        if ("{CRYPT}".startsWith(encryptedPassword.toUpperCase())) {
            return "CRYPT:"
                    + encryptedPassword.substring("{CRYPT}".length(), encryptedPassword.length());
        }

        return encryptedPassword;
    }

    private static final class CachedUserInfo {
        public final long expires;
        public final UserInfo userInfo;

        public CachedUserInfo(UserInfo userInfo, long expires) {
            this.userInfo = userInfo;
            this.expires = expires;
        }
    }
}

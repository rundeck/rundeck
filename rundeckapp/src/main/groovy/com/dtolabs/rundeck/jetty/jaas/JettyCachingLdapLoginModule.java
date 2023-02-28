/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.jetty.jaas;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.*;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

//import ch.qos.logback.classic.Level;
import grails.util.Holders;
import org.eclipse.jetty.jaas.callback.ObjectCallback;
import org.eclipse.jetty.jaas.spi.AbstractLoginModule;
import org.eclipse.jetty.jaas.spi.UserInfo;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import rundeck.services.ConfigurationService;

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
 *    timeoutRead="5000"
 *    timeoutConnect="30000"
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
 *    roleUsernameMemberAttribute="memberUid"
 *    roleObjectClass="groupOfUniqueNames"
 *    rolePrefix="rundeck"
 *    cacheDurationMillis="500"
 *    reportStatistics="true"
 *    nestedGroups="false";
 *    };
 * </pre>
 *
 * @author Jesse McConnell <a href="mailto:jesse@codehaus.org">jesse@codehaus.org</a>
 * @author Frederic Nizery <a href="mailto:frederic.nizery@alcatel-lucent.fr">frederic.nizery@alcatel-lucent.fr</a>
 * @author Trygve Laugstol <a href="mailto:trygvis@codehaus.org">trygvis@codehaus.org</a>
 * @author Noah Campbell <a href="mailto:noahcampbell@gmail.com">noahcampbell@gmail.com</a>
 */
public class JettyCachingLdapLoginModule extends AbstractLoginModule {

    private static final Logger LOG                 = LoggerFactory.getLogger(JettyCachingLdapLoginModule.class);
    public static final  String OBJECT_CLASS_FILTER = "(&(objectClass={0})({1}={2}))";

    static {
        String logLevelSysProp = System.getProperty("com.dtolabs.rundeck.jetty.jaas.LEVEL");
        if(logLevelSysProp != null) {
            //TODO: FIX
           // Level level = Level.toLevel(logLevelSysProp);
           // ((ch.qos.logback.classic.Logger) LOG).setLevel(level);
        }
    }

    private static final Pattern rolePattern = Pattern.compile("^cn=([^,]+)", Pattern.CASE_INSENSITIVE);
    private static final String CRYPT_TYPE   = "CRYPT:";
    private static final String MD5_TYPE     = "MD5:";

    protected final String _roleMemberFilter = "member=*";
    /**
     * Provider URL
     */
    protected String _providerUrl;

    /**
     * Role prefix to remove from ldap group name.
     */
    protected String _rolePrefix = "";

    /**
     * Duration of storing the user in memory.
     */
    protected int _cacheDuration = 0;

    /**
     * hostname of the ldap server
     */
    protected String _hostname;

    /**
     * port of the ldap server
     */
    protected int _port = 389;

    /**
     * Context.SECURITY_AUTHENTICATION
     */
    protected String _authenticationMethod;

    /**
     * Context.INITIAL_CONTEXT_FACTORY
     */
    protected String _contextFactory;

    /**
     * root DN used to connect to
     */
    protected String _bindDn;

    /**
     * password used to connect to the root ldap context
     */
    protected String _bindPassword;

    /**
     * object class of a user
     */
    protected String _userObjectClass = "inetOrgPerson";

    /**
     * attribute that the principal is located
     */
    protected String _userRdnAttribute = "uid";

    /**
     * attribute that the principal is located
     */
    protected String _userIdAttribute = "cn";

    /**
     * name of the attribute that a users password is stored under
     * <br>
     * NOTE: not always accessible, see force binding login
     */
    protected String _userPasswordAttribute = "userPassword";

    /**
     * base DN where users are to be searched from
     */
    protected String _userBaseDn;

    /**
     * attribute of user first name
     */
    protected String _userFirstNameAttribute = "givenName";

    /**
      * attribute of user last name
      */
    protected String _userLastNameAttribute = "sn";
    /**
      * attribute of user email
      */
    protected String _userEmailAttribute = "mail";

    /**
     * base DN where role membership is to be searched from
     */
    protected String _roleBaseDn;

    /**
     * object class of roles
     */
    protected String _roleObjectClass = "groupOfUniqueNames";

    /**
     * name of the attribute that a user DN would be under a role class
     */
    protected String _roleMemberAttribute = "uniqueMember";

    /**
     * name of the attribute that a username would be under a role class
     */
    protected String _roleUsernameMemberAttribute=null;

    /**
     * the name of the attribute that a role would be stored under
     */
    protected String _roleNameAttribute = "roleName";

    protected boolean _debug;

    protected boolean _ldapsVerifyHostname=true;

    /**
     * if the getUserInfo can pull a password off of the user then password
     * comparison is an option for authn, to force binding login checks, set
     * this to true
     */
    protected boolean _forceBindingLogin = false;

    /**
     * if _forceFindingLogin is true, and _forceBindingLoginUseRootContextForRoles
     * is true, then role memberships are obtained using _rootContext
     */
    protected boolean _forceBindingLoginUseRootContextForRoles = false;

    protected DirContext _rootContext;

    protected LdapContext ldapContext;

    protected boolean _reportStatistics;

    /**
     * List of supplemental roles provided in config file that get added to
     * all users.
     */
    protected List<String> _supplementalRoles;

    protected boolean _nestedGroups;

    /**
     * timeout for LDAP read
     */
    protected long _timeoutRead =0;

    /**
     * timeout for LDAP connection
     */
    protected long _timeoutConnect =0;

    protected static final ConcurrentHashMap<String, CachedUserInfo> USERINFOCACHE =
        new ConcurrentHashMap<String, CachedUserInfo>();

    private String _userLastName;
    private String _userFirstName;
    private String _userEmail;
    /**
     * The number of cache hits for UserInfo objects.
     */
    protected static long userInfoCacheHits;

    /**
     * The number of login attempts for this particular module.
     */
    protected static long loginAttempts;
    private static ConcurrentHashMap<String, List<String>> roleMemberOfMap;
    private static long roleMemberOfMapExpires = 0;

    /**
     * This is to allow AD roles to be paginated
     */
    protected boolean rolePagination = true;

    /**
     * Maximun roles per page when pagination is enabled
     */
    protected int rolesPerPage = 100;

    /**
     * get the available information about the user
     * <br>
     * for this LoginModule, the credential can be null which will result in a
     * binding ldap authentication scenario
     * <br>
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
        pwdCredential = decodeBase64EncodedPwd(pwdCredential);
        Credential credential = Credential.getCredential(pwdCredential);
        List roles = getUserRoles(_rootContext, username);

        return new UserInfo(username, credential, roles);
    }

    String decodeBase64EncodedPwd(String encoded) {
        String chkString = null;
        String prefix = "";
        if(encoded.startsWith(CRYPT_TYPE)) {
            chkString = encoded.substring(CRYPT_TYPE.length(),encoded.length());
            prefix = CRYPT_TYPE;
        } else if(encoded.startsWith(MD5_TYPE)) {
            chkString = encoded.substring(MD5_TYPE.length(), encoded.length());
            prefix = MD5_TYPE;
        } else {
            return encoded; //make no attempt to further decode because we don't know what value this might be
        }

        return prefix+(isBase64(chkString) ? org.apache.commons.codec.binary.Hex.encodeHexString(org.apache.commons.codec.binary.Base64.decodeBase64(chkString)) : chkString);
    }

    boolean isBase64(String chkBase64) {
        if(isHex(chkBase64)) return false;
        try {
            Base64.getDecoder().decode(chkBase64);
            return chkBase64.replace(" ","").length() % 4 == 0;
        } catch(IllegalArgumentException iaex) {}
        return false;
    }

    boolean isHex(String chkHex) {
        if(chkHex.length() != 32 || chkHex.endsWith("=")) return false;
        return chkHex.matches("^[0-9a-fA-F]+$");
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

        try {
            Object[] filterArguments = { _userObjectClass, _userIdAttribute, username };
            NamingEnumeration results = _rootContext.search(_userBaseDn, OBJECT_CLASS_FILTER, filterArguments, ctls);

            debug("Found user?: " + results.hasMoreElements());

            if (!results.hasMoreElements()) {
                throw new LoginException("User not found.");
            }

            SearchResult result = findUser(username);

            Attributes attributes = result.getAttributes();

            setDemographicAttributes(attributes);
            Attribute attribute = attributes.get(_userPasswordAttribute);
            if (attribute != null) {
                try {
                    byte[] value = (byte[]) attribute.get();

                    ldapCredential = new String(value);
                } catch (NamingException e) {
                    LOG.info("no password available under attribute: " + _userPasswordAttribute);
                }
            }
        } catch (NamingException e) {
            throw new LoginException("Root context binding failure.");
        }

        debug("user cred is present: " + (ldapCredential != null));

        return ldapCredential;
    }

    /**
     * attempts to get the users roles from the root context
     *
     * NOTE: this is not an user authenticated operation
     *
     * @param dirContext
     * @param username
     * @return
     * @throws LoginException
     */
    @SuppressWarnings("unchecked")
    protected List getUserRoles(DirContext dirContext, String username) throws LoginException,
            NamingException, IOException {
        String userDn = _userRdnAttribute + "=" + username + "," + _userBaseDn;
        return getUserRolesByDn(dirContext, userDn, username);
    }

    @SuppressWarnings("unchecked")
    private List getUserRolesByDn(DirContext dirContext, String userDn, String username) throws LoginException,
            NamingException, IOException {
        List<String> roleList = new ArrayList<String>();

        if (dirContext == null || _roleBaseDn == null || (_roleMemberAttribute == null
                                                          && _roleUsernameMemberAttribute == null)
                || _roleObjectClass == null) {
            LOG.warn("JettyCachingLdapLoginModule: No user roles found: roleBaseDn, roleObjectClass and roleMemberAttribute or roleUsernameMemberAttribute must be specified.");
            addSupplementalRoles(roleList);
            return roleList;
        }

        List<SearchResult> results;
        if(rolePagination){
            results = getPaginatedRoles(userDn, username);
        }else{
            results = getNonPaginatedRoles(dirContext, userDn, username);
        }
        roleList = getRoleList(results);
        addSupplementalRoles(roleList);
        if(_nestedGroups) {
            roleList = getNestedRoles(dirContext, roleList);
        }

        if (roleList.size() < 1) {
            LOG.warn("JettyCachingLdapLoginModule: User '" + username + "' has no role membership; role query configuration may be incorrect");
        }else{
            debug("JettyCachingLdapLoginModule: User '" + username + "' has roles: " + roleList);
        }

        return roleList;
    }


    /**
     * It searches for roles without pagination
     * @param dirContext dirContext
     * @param userDn userDn
     * @param username username
     *
     * @return List<SearchResult>
     * @throws NamingException
     */
    private List<SearchResult> getNonPaginatedRoles(DirContext dirContext, String userDn, String username) throws NamingException {
        List<SearchResult> searchResults = new ArrayList<>();
        String[] attrIDs = { _roleNameAttribute };
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(attrIDs);
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String filter = OBJECT_CLASS_FILTER;
        NamingEnumeration<SearchResult> results = null;
        if(null !=_roleUsernameMemberAttribute){
            Object[] filterArguments = { _roleObjectClass, _roleUsernameMemberAttribute, username };
            results = dirContext.search(_roleBaseDn, filter, filterArguments, ctls);
        }else{
            Object[] filterArguments = { _roleObjectClass, _roleMemberAttribute, userDn };
            results = dirContext.search(_roleBaseDn, filter, filterArguments, ctls);
        }
        while (results != null && results.hasMoreElements()) {
            searchResults.add(results.nextElement());
        }
        return searchResults;
    }

    /**
     * It searches for roles with pagination
     * @param userDn userDn
     * @param username username
     *
     * @return List<SearchResult>
     * @throws NamingException
     */
    private List<SearchResult> getPaginatedRoles(String userDn, String username) throws IOException, NamingException {
        List<SearchResult> searchResults = new ArrayList<>();

        int pageSize = rolesPerPage;
        byte[] cookie = null;
        ldapContext.setRequestControls(new Control[]{
                new PagedResultsControl(pageSize, Control.CRITICAL) });
        do {
            String filter = OBJECT_CLASS_FILTER;

            Object[] filterArguments = null;
            if(null !=_roleUsernameMemberAttribute){
                filterArguments = new Object[]{_roleObjectClass, _roleUsernameMemberAttribute, username};
            }else{
                filterArguments = new Object[]{_roleObjectClass, _roleMemberAttribute, userDn};
            }

            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration results = ldapContext.search(
                    _roleBaseDn,
                    filter,
                    filterArguments,
                    searchControls);

            // Iterate over a batch of search results
            while (results != null && results.hasMoreElements()) {
                searchResults.add((SearchResult)results.nextElement());
            }
            // Examine the paged results control response
            Control[] controls = ldapContext.getResponseControls();
            if (controls != null) {
                for (int i = 0; i < controls.length; i++) {
                    if (controls[i] instanceof PagedResultsResponseControl) {
                        PagedResultsResponseControl prrc =
                                (PagedResultsResponseControl)controls[i];
                        cookie = prrc.getCookie();
                    }
                }
            }
            ldapContext.setRequestControls(new Control[]{
                    new PagedResultsControl(pageSize, cookie, Control.CRITICAL) });
        } while (cookie != null);

        return searchResults;
    }

    private List<String> getRoleList(List<SearchResult> results) throws NamingException {
        List<String> roleList = new ArrayList<String>();
        for (SearchResult searchResult : results) {
            Attributes attributes = searchResult.getAttributes();
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
                    roleList.add(role.replace(_rolePrefix, ""));
                } else {
                    roleList.add((String) roles.next());
                }
            }
        }
        return roleList;
    }

    protected void addSupplementalRoles(final List<String> roleList) {
        if (null != _supplementalRoles) {
            for (String supplementalRole : _supplementalRoles) {
                if(null!=supplementalRole&& !"".equals(supplementalRole.trim())){
                    roleList.add(supplementalRole.trim());
                }
            }
        }
    }

    private List<String> getNestedRoles(DirContext dirContext, List<String> roleList) {

        HashMap<String, List<String>> roleMemberOfMap = new HashMap<String, List<String>>();
        roleMemberOfMap.putAll(getRoleMemberOfMap(dirContext));

        List<String> mergedList = mergeRoles(roleList, roleMemberOfMap);

        return mergedList;
    }

    private List<String> mergeRoles(List<String> roles, HashMap<String, List<String>> roleMemberOfMap) {
        List<String> newRoles = new ArrayList<String>();
        List<String> mergedRoles = new ArrayList<String>();
        mergedRoles.addAll(roles);

        for(String role : roles) {
            if(roleMemberOfMap.containsKey(role)) {
                for(String newRole : roleMemberOfMap.get(role)) {
                    if (!roles.contains(newRole)) {
                        newRoles.add(newRole);
                    }
                }
                roleMemberOfMap.remove(role);
            }

        }
        if(!newRoles.isEmpty()) {
            mergedRoles.addAll(mergeRoles(newRoles, roleMemberOfMap));
        }
        return mergedRoles;
    }

    private ConcurrentHashMap<String, List<String>> getRoleMemberOfMap(DirContext dirContext) {
        if (_cacheDuration == 0 || System.currentTimeMillis() > roleMemberOfMapExpires) { // only worry about caching if there is a cacheDuration set.
            roleMemberOfMap = buildRoleMemberOfMap(dirContext);
            roleMemberOfMapExpires = System.currentTimeMillis() + _cacheDuration;
        }

        return roleMemberOfMap;
    }

    private ConcurrentHashMap<String, List<String>> buildRoleMemberOfMap(DirContext dirContext) {
        Object[] filterArguments = { _roleObjectClass };
        SearchControls ctls = new SearchControls();
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        ConcurrentHashMap<String, List<String>> roleMemberOfMap = new ConcurrentHashMap<String, List<String>>();

        try {
            NamingEnumeration<SearchResult> results = dirContext.search(_roleBaseDn, _roleMemberFilter, ctls);
            while (results.hasMoreElements()) {
                SearchResult result = results.nextElement();
                Attributes attributes = result.getAttributes();

                if (attributes == null) {
                    continue;
                }

                Attribute roleAttribute = attributes.get(_roleNameAttribute);
                Attribute memberAttribute = attributes.get(_roleMemberAttribute);

                if (roleAttribute == null || memberAttribute == null) {
                    continue;
                }

                NamingEnumeration role = roleAttribute.getAll();
                NamingEnumeration members = memberAttribute.getAll();

                if(!role.hasMore() || !members.hasMore()) {
                    continue;
                }

                String roleName = (String) role.next();
                if (_rolePrefix != null && !"".equalsIgnoreCase(_rolePrefix)) {
                    roleName = roleName.replace(_rolePrefix, "");
                }

                while(members.hasMore()) {
                    String member = (String) members.next();
                    Matcher roleMatcher = rolePattern.matcher(member);
                    if(!roleMatcher.find()) {
                        continue;
                    }
                    String roleMember = roleMatcher.group(1);
                    List<String> memberOf;
                    if(roleMemberOfMap.containsKey(roleMember)) {
                        memberOf = roleMemberOfMap.get(roleMember);
                    } else {
                        memberOf = new ArrayList<String>();
                    }

                    memberOf.add(roleName);

                    roleMemberOfMap.put(roleMember, memberOf);
                }

            }
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return roleMemberOfMap;
    }
    protected boolean isDebug(){
        return _debug;
    }
    /**
     * Default behavior to emit to System.err
     * @param message message
     */
    protected void debug(String message) {
        if (isDebug()) {
            LOG.debug(message);
        }
    }


    /**
     * Gets credentials by calling {@link #getCallBackAuth()}, then performs {@link #authenticate(String, Object)}
     *
     * @return true if authenticated
     * @throws LoginException
     */
    @Override
    public boolean login() throws LoginException {
        try{
            Object[] userPass= getCallBackAuth();
            if (null == userPass || userPass.length < 2) {
                setAuthenticated(false);
                throw new FailedLoginException();
            }
            String name = (String) userPass[0];
            Object pass = userPass[1];
            boolean authenticated = authenticate(name, pass);
            setAuthenticated(authenticated);

            if (!isAuthenticated()) {
                throw new FailedLoginException();
            }
            return isAuthenticated();
        } catch (UnsupportedCallbackException e) {
            throw new LoginException("Error obtaining callback information.");
        } catch (IOException e) {
            if (_debug) {
                e.printStackTrace();
            }
            throw new LoginException("IO Error performing login.");
        }
    }

    /**
     *
     * @return Return the object[] containing username and password, by using the callback mechanism
     *
     * @throws IOException
     * @throws UnsupportedCallbackException
     * @throws LoginException
     */
    protected Object[] getCallBackAuth() throws IOException, UnsupportedCallbackException, LoginException {
        if (getCallbackHandler() == null) {
            throw new LoginException("No callback handler");
        }

        Callback[] callbacks = configureCallbacks();
        getCallbackHandler().handle(callbacks);

        String webUserName = ((NameCallback) callbacks[0]).getName();
        Object webCredential = ((ObjectCallback) callbacks[1]).getObject();
        if (webCredential == null) {
            webCredential = ((PasswordCallback)callbacks[2]).getPassword();
        }
        return new Object[]{webUserName,webCredential};
    }


    /**
     * since ldap uses a context bind for valid authentication checking, we
     * override login()
     * <br>
     * if credentials are not available from the users context or if we are
     * forcing the binding check then we try a binding authentication check,
     * otherwise if we have the users encoded password then we can try
     * authentication via that mechanic
     * @param webUserName user
     * @param webCredential password
     *
     *
     * @return true if authenticated
     * @throws LoginException
     */
    protected boolean authenticate(final String webUserName, final Object webCredential) throws LoginException {
        try {
            if (isEmptyOrNull(webUserName) || isEmptyOrNull(webCredential) || (webCredential instanceof char[] && ((char[]) webCredential).length == 0)) {
                LOG.info("empty username or password not allowed");
                setAuthenticated(false);
                return isAuthenticated();
            }

            loginAttempts++;

            if(_reportStatistics)
            {
                DecimalFormat percentHit = new DecimalFormat("#.##");
                LOG.info("Login attempts: " + loginAttempts + ", Hits: " + userInfoCacheHits +
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

            JAASUserInfo jaasUserInfo = new JAASUserInfo(userInfo);
            jaasUserInfo.fetchRoles(); //must run this otherwise will throw NPE later
            setCurrentUser(jaasUserInfo);

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
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            if (_debug) {
                e.printStackTrace();
            }
            throw new LoginException("Error obtaining user info.");
        }
    }

    private boolean isEmptyOrNull(final Object value) {
        return null==value || "".equals(value);
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
        final String cacheToken = Credential.MD5.digest(username + ":" + password.toString());
        if (_cacheDuration > 0) { // only worry about caching if there is a cacheDuration set.
            CachedUserInfo cached = USERINFOCACHE.get(cacheToken);
            if (cached != null) {
                if (System.currentTimeMillis() < cached.expires) {
                    debug("Cache Hit for " + username + ".");
                    userInfoCacheHits++;
                    JAASUserInfo jaasUserInfo = new JAASUserInfo(cached.userInfo);
                    try {
                        jaasUserInfo.fetchRoles();
                    } catch(Exception ex) {
                        if(_debug) {
                            LOG.debug("Failed to fetch roles",ex);
                        }
                        throw new LoginException("Error obtaining user info.");
                    }
                    setCurrentUser(jaasUserInfo);
                    setAuthenticated(true);
                    return true;
                } else {
                    LOG.info("Cache Eviction for " + username + ".");
                    USERINFOCACHE.remove(cacheToken);
                }
            } else {
                debug("Cache Miss for " + username + ".");
            }
        }

        SearchResult searchResult = findUser(username);

        String userDn = searchResult.getNameInNamespace();

        LOG.info("Attempting authentication: " + userDn);
        DirContext dirContext = createBindUserDirContext(userDn, password);
        setDemographicAttributes(searchResult.getAttributes());

        // use _rootContext to find roles, if configured to doso
        if ( _forceBindingLoginUseRootContextForRoles ) {
            dirContext = _rootContext;
            debug("Using _rootContext for role lookup.");
        }
        List roles = null;
        try {
            roles = getUserRolesByDn(dirContext, userDn, username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        UserInfo userInfo = new UserInfo(username, new Password(password.toString()), roles);
        if (_cacheDuration > 0) {
            USERINFOCACHE.put(cacheToken,
                new CachedUserInfo(userInfo,
                    System.currentTimeMillis() + _cacheDuration));
            debug("Adding " + username + " set to expire: " + System.currentTimeMillis() + _cacheDuration);
        }
        JAASUserInfo jaasUserInfo = new JAASUserInfo(userInfo);
        try {
            jaasUserInfo.fetchRoles();
        } catch(Exception ex) {
            if(_debug) {
                LOG.debug("Failed to fetch roles",ex);
            }
            throw new LoginException("Error obtaining user info.");
        }
        setCurrentUser(jaasUserInfo);
        setAuthenticated(true);
        return true;
    }

    BindDirContextCreator userBindDirContextCreator;
    static interface BindDirContextCreator{
        DirContext createBindUserDirContext(final String userDn, final Object password) throws NamingException;
    }

    private DirContext createBindUserDirContext(final String userDn, final Object password) throws NamingException {
        if (null != userBindDirContextCreator) {
            return userBindDirContextCreator.createBindUserDirContext(userDn, password);
        }
        Hashtable environment = getEnvironment();
        environment.put(Context.SECURITY_PRINCIPAL, userDn);
        environment.put(Context.SECURITY_CREDENTIALS, password);
        return new InitialDirContext(environment);
    }

    @SuppressWarnings("unchecked")
    private SearchResult findUser(String username) throws NamingException, LoginException {
        SearchControls ctls = new SearchControls();
        ctls.setCountLimit(1);
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = OBJECT_CLASS_FILTER;

        debug("Searching for users with filter: \'" + filter + "\'" + " from base dn: "
                + _userBaseDn);

        Object[] filterArguments = new Object[] { _userObjectClass, _userIdAttribute, username };
        NamingEnumeration results = _rootContext.search(_userBaseDn, filter, filterArguments, ctls);

        debug("Found user?: " + results.hasMoreElements());

        if (!results.hasMoreElements()) {
            throw new LoginException("User not found.");
        }

        return (SearchResult) results.nextElement();
    }

    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
                           Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);

        initializeOptions(options);

        try {
            _rootContext = new InitialDirContext(getEnvironment());
            if(rolePagination){
                ldapContext = new InitialLdapContext(_rootContext.getEnvironment(), null);
            }
        } catch (NamingException ex) {
            LOG.error("Naming error",ex);
            throw new IllegalStateException("Unable to establish root context: "+ex.getMessage());
        }
    }

    private void setDemographicAttributes(final Attributes attributes) {
        _userFirstName = safeGetAttributeAsString(attributes.get(_userFirstNameAttribute));
        _userLastName = safeGetAttributeAsString(attributes.get(_userLastNameAttribute));
        _userEmail = safeGetAttributeAsString(attributes.get(_userEmailAttribute));
    }

    private String safeGetAttributeAsString(final Attribute attribute) {
        try {
            if (attribute == null || attribute.get() == null) return null;
            return attribute.get().toString();
        } catch(NamingException nex) {
            LOG.warn("Unable to retrieve rundeck sync attributes. User will not be synced.", nex);
        }
        return null;
    }

    public void initializeOptions(final Map options) {
        _hostname = (String) options.get("hostname");
        if(options.containsKey("port")) {
            _port = Integer.parseInt((String) options.get("port"));
        }
        _providerUrl = (String) options.get("providerUrl");
        _contextFactory = (String) options.get("contextFactory");
        _bindDn = (String) options.get("bindDn");
        String bindPassword = attemptBindPwdFromRdkConfig();
        if(bindPassword != null && !"null".equals(bindPassword)) {
            _bindPassword = bindPassword;
        } else {
            _bindPassword = (String) options.get("bindPassword");
        }
        _authenticationMethod = (String) options.get("authenticationMethod");

        _userBaseDn = (String) options.get("userBaseDn");

        _roleBaseDn = (String) options.get("roleBaseDn");

        if (options.containsKey("forceBindingLogin")) {
            _forceBindingLogin = Boolean.parseBoolean((String) options.get("forceBindingLogin"));
        }

        if (options.containsKey("nestedGroups")) {
            _nestedGroups = Boolean.parseBoolean((String) options.get("nestedGroups"));
        }

        if (options.containsKey("forceBindingLoginUseRootContextForRoles")) {
            _forceBindingLoginUseRootContextForRoles = Boolean.parseBoolean((String) options.get("forceBindingLoginUseRootContextForRoles"));
        }

        _userObjectClass = getOption(options, "userObjectClass", _userObjectClass);
        _userRdnAttribute = getOption(options, "userRdnAttribute", _userRdnAttribute);
        _userIdAttribute = getOption(options, "userIdAttribute", _userIdAttribute);
        _userPasswordAttribute = getOption(options, "userPasswordAttribute", _userPasswordAttribute);
        _userLastNameAttribute = getOption(options, "userLastNameAttribute", _userLastNameAttribute);
        _userFirstNameAttribute = getOption(options, "userFirstNameAttribute", _userFirstNameAttribute);
        _userEmailAttribute = getOption(options, "userEmailAttribute", _userEmailAttribute);
        _roleObjectClass = getOption(options, "roleObjectClass", _roleObjectClass);
        _roleMemberAttribute = getOption(options, "roleMemberAttribute", _roleMemberAttribute);
        _roleUsernameMemberAttribute = getOption(options, "roleUsernameMemberAttribute", _roleUsernameMemberAttribute);
        _roleNameAttribute = getOption(options, "roleNameAttribute", _roleNameAttribute);
        _debug = Boolean.parseBoolean(String.valueOf(getOption(options, "debug", Boolean
                .toString(_debug))));
        _ldapsVerifyHostname = Boolean.parseBoolean(String.valueOf(getOption(options, "ldapsVerifyHostname", Boolean
                .toString(_ldapsVerifyHostname))));

        _rolePrefix = (String) options.get("rolePrefix");

        _reportStatistics = Boolean.parseBoolean(String.valueOf(getOption(options, "reportStatistics", Boolean
                .toString(_reportStatistics))));

        rolePagination = Boolean.parseBoolean(String.valueOf(getOption(options, "rolePagination", Boolean
                .toString(rolePagination))));

        rolesPerPage = Integer.valueOf(getOption(options, "rolesPerPage", String.valueOf(rolesPerPage)));

        Object supplementalRoles = options.get("supplementalRoles");
        if (null != supplementalRoles) {
            this._supplementalRoles = new ArrayList<String>();
            this._supplementalRoles.addAll(Arrays.asList(supplementalRoles.toString().split(", *")));
        }

        String cacheDurationSetting = (String) options.get("cacheDurationMillis");
        if (cacheDurationSetting != null) {
            try {
                _cacheDuration = Integer.parseInt(cacheDurationSetting);
            } catch (NumberFormatException e) {
                LOG.warn("Unable to parse cacheDurationMillis to a number: " + cacheDurationSetting,
                        ". Using default: " + _cacheDuration, e);
            }
        }
        if (options.containsKey("timeoutRead")) {
            _timeoutRead = Long.parseLong((String) options.get("timeoutRead"));
        }
        if (options.containsKey("timeoutConnect")) {
            _timeoutConnect = Long.parseLong((String) options.get("timeoutConnect"));
        }
    }

    String attemptBindPwdFromRdkConfig() {
        ConfigurationService cfgSvc = getConfigurationService();
        return cfgSvc != null
        ? cfgSvc.getString("security.ldap.bindPassword")
        : null;
    }

    ConfigurationService getConfigurationService() {
        ApplicationContext ctx = Holders.findApplicationContext();
        if(ctx == null) return null;
        return ctx.containsBeanDefinition("configurationService") ?
               ((ConfigurationService)ctx.getBean("configurationService"))
               : null ;
    }

    public boolean commit() throws LoginException {
        try {
            _rootContext.close();
        } catch (NamingException e) {
            throw new LoginException("error closing root context: " + e.getMessage());
        }
        if(_userFirstName != null) getSubject().getPrincipals().add(new LdapFirstNamePrincipal(_userFirstName));
        if(_userLastName != null) getSubject().getPrincipals().add(new LdapLastNamePrincipal(_userLastName));
        if(_userEmail != null) getSubject().getPrincipals().add(new LdapEmailPrincipal(_userEmail));

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
    protected String getOption(Map options, String key, String defaultValue) {
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
        String url = null;
        if(_providerUrl != null) {
            url =  _providerUrl;
        } else {
            if (_hostname != null) {
                url = "ldap://" + _hostname + "/";
                if (_port != 0) {
                    url += ":" + _port + "/";
                }

                LOG.warn("Using hostname and port.  Use providerUrl instead: " + url);
            }
        }
        env.put(Context.PROVIDER_URL, url);

        if (_authenticationMethod != null) {
            env.put(Context.SECURITY_AUTHENTICATION, _authenticationMethod);
        }

        if (_bindDn != null) {
            env.put(Context.SECURITY_PRINCIPAL, _bindDn);
        }

        if (_bindPassword != null) {
            env.put(Context.SECURITY_CREDENTIALS, _bindPassword);
        }
        env.put("com.sun.jndi.ldap.read.timeout", Long.toString(_timeoutRead));
        env.put("com.sun.jndi.ldap.connect.timeout", Long.toString(_timeoutConnect));

        // Set the SSLContextFactory to implementation that validates cert subject
        if (url != null && url.startsWith("ldaps") && _ldapsVerifyHostname) {
            try {
                URI uri = new URI(url);
                HostnameVerifyingSSLSocketFactory.setTargetHost(uri.getHost());
                env.put("java.naming.ldap.factory.socket",
                        "com.dtolabs.rundeck.jetty.jaas.HostnameVerifyingSSLSocketFactory");
            }
            catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        return env;
    }


    private static String convertCredentialLdapToJetty(String encryptedPassword) {
        if (encryptedPassword == null) {
            return encryptedPassword;
        }

        if (encryptedPassword.toUpperCase().startsWith("{MD5}")) {
            return "MD5:"
                   + encryptedPassword.substring("{MD5}".length(), encryptedPassword.length());
        }

        if (encryptedPassword.toUpperCase().startsWith("{CRYPT}")) {
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

/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.jndi;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Category;


/**
 * Jndi class wrapper.
 * provides methods to connect to and obtain info from jndi server (such as ldap)
 *
 * @author Chuck Scott <a href="mailto:chuck@dtosolutions.com">chuck@dtosolutions.com</a>
 */

public class Jndi {

    static Category logger = Category.getInstance(Jndi.class.getName());

    // Directory Context datamember from initial jndi connection
    private DirContext initialDirContext;

    /**
     * setter for the jndi initialDirContext
     *
     * @param initialDirContext
     */
    public void setInitialDirContext(DirContext initialDirContext) {
        this.initialDirContext = initialDirContext;
    }

    /**
     * getter for the jndi initialDirContext
     *
     * @return DirContext
     */
    public DirContext getInitialDirContext() {
        return this.initialDirContext;
    }

    // jndi connectionName datamember such as "cn=Manager,dc=foo,dc=com"
    private String connectionName;

    /**
     * setter for the jndi connection name
     *
     * @param connectionName
     */
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    /**
     * getter for the jndi connection name
     *
     * @return String
     */
    public String getConnectionName() {
        return this.connectionName;
    }

    // jndi connectionPassword datamember such as "secret"
    private String connectionPassword;

    /**
     * setter for the jndi connection password
     *
     * @param connectionPassword
     */
    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    /**
     * getter for the jndi connection password
     *
     * @return String
     */
    public String getConnectionPassword() {
        return this.connectionPassword;
    }

    // jndi connectionUrl datamember such as "ldap://ldap1.foo.com:389/"
    private String connectionUrl;

    /**
     * setter for the jndi connection url
     *
     * @param connectionUrl
     */
    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    /**
     * getter for the jndi connection url
     *
     * @return String
     */
    public String getConnectionUrl() {
        return this.connectionUrl;
    }

    // jndi roleBase datamember such as "dc=roles,dc=foo,dc=com"
    private String roleBase;

    /**
     * setter for the jndi role base
     *
     * @param roleBase
     */
    public void setRoleBase(String roleBase) {
        this.roleBase = roleBase;
    }

    /**
     * getter for the jndi role base
     *
     * @return String
     */
    public String getRoleBase() {
        return this.roleBase;
    }

    // jndi roleNameRDN datamember such as "cn"
    private String roleNameRDN;

    /**
     * setter for the jndi role name rdn
     *
     * @param roleNameRDN
     */
    public void setRoleNameRDN(String roleNameRDN) {
        this.roleNameRDN = roleNameRDN;
    }

    /**
     * getter for the jndi role name rdn
     *
     * @return String
     */
    public String getRoleNameRDN() {
        return this.roleNameRDN;
    }

    // jndi roleMemberRDN datamember such as "uniqueMember"
    private String roleMemberRDN;

    /**
     * setter for the jndi role member rdn
     *
     * @param roleMemberRDN
     */
    public void setRoleMemberRDN(String roleMemberRDN) {
        this.roleMemberRDN = roleMemberRDN;
    }

    /**
     * setter for the jndi role member rdn
     *
     * @return String
     */
    public String getRoleMemberRDN() {
        return this.roleMemberRDN;
    }

    // jndi userBase datamember such as "dc=foo,dc=com"
    private String userBase;

    /**
     * setter for the jndi user base
     *
     * @param userBase
     */
    public void setUserBase(String userBase) {
        this.userBase = userBase;
    }

    /**
     * getter for the jndi user base
     *
     * @return String
     */
    public String getUserBase() {
        return this.userBase;
    }

    // jndi userNameRDN datamember such as "cn"
    private String userNameRDN;

    /**
     * setter for the jndi username rdn
     *
     * @param userNameRDN
     */
    public void setUserNameRDN(String userNameRDN) {
        this.userNameRDN = userNameRDN;
    }

    /**
     * getter for the jndi username rdn
     *
     * @return String
     */
    public String getUserNameRDN() {
        return this.userNameRDN;
    }

    /**
     * sets parameters needed for initial connection to the jndi server
     *
     * @param connectionName     distinquished name (dn) to connect to jndi server with
     * @param connectionPassword password for the dn
     * @param connectionUrl      url to connect to the jndi server with
     * @param roleBase           the suffix for all roles
     * @param roleNameRDN        the key name (such as "cn") to reference
     * @param roleMemberRDN      the key name to reference members of roles
     * @param userBase           the suffix for users
     * @param userNameRDN        the key name to reference a username
     * @throws NamingException
     */
    public Jndi(String connectionName, String connectionPassword,
                String connectionUrl, String roleBase, String roleNameRDN,
                String roleMemberRDN, String userBase, String userNameRDN)
            throws NamingException {

        Hashtable env = new Hashtable(11);

        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, connectionUrl);
        env.put(Context.SECURITY_PRINCIPAL, connectionName);
        env.put(Context.SECURITY_CREDENTIALS, connectionPassword);

        DirContext initialCtx = new InitialDirContext(env);
        setInitialDirContext(initialCtx);

        this.setConnectionName(connectionName);
        this.setConnectionPassword(connectionPassword);
        this.setConnectionUrl(connectionUrl);
        this.setRoleBase(roleBase);
        this.setRoleNameRDN(roleNameRDN);
        this.setRoleMemberRDN(roleMemberRDN);
        this.setUserBase(userBase);
        this.setUserNameRDN(userNameRDN);
    }

    public Jndi(JndiConfig cfg) throws NamingException {
        this(cfg.getConnectionName(), cfg.getConnectionPassword(), cfg.getConnectionUrl(),
                cfg.getRoleBase(), cfg.getRoleNameRDN(), cfg.getRoleMemberRDN(),
                cfg.getUserBase(), cfg.getUserNameRDN());
    }


    /**
     * return list of all users for a given role as a String array
     *
     * @param roleName
     * @return String[]
     * @throws NamingException
     */
    public String[] getUsers(String roleName) throws NamingException {
        logger.debug("Obtaining users for roleName: " + roleName);
        String roleBase = this.getRoleBase();
        String filter = "(" + this.getRoleNameRDN() + "=" + roleName + ")";

        logger.debug("returningAttributes set to: " + this.getRoleMemberRDN());
        //search directory
        NamingEnumeration roleResults = this.search(roleBase,
                filter,
                new String[]{
                    this.getRoleMemberRDN()
                });

        String[] users = this.getUsers(roleName, roleResults);
        return users;
    }

    /**
     * return list of roles associated with given userName
     *
     * @param userName
     * @return String[]
     * @throws NamingException
     */
    public String[] getRoles(String userName) throws NamingException {

        logger.debug("Obtaining roles for userName: " + userName);

        // create filter expression something like: (cn=*)
        //    for roleBase
        String roleBase = this.getRoleBase();
        String filter = "(" + this.getRoleNameRDN() + "=*)";

        // set the sole returning attribute identifyied by roleMember rdn
        // and apply constraints
        logger.debug("returningAttributes set to: " + this.getRoleMemberRDN());

        //search directory
        NamingEnumeration roleResults = this.search(roleBase,
                filter,
                new String[]{
                    this.getRoleMemberRDN()
                });

        String[] roles = this.getRoles(userName, roleResults);
        return roles;
    }

    // get Roles for userName from a enumeration of SearchResult's
    private String[] getUsers(String roleName, NamingEnumeration roleResults)
            throws NamingException {

        ArrayList userList = new ArrayList();

        while (roleResults != null && roleResults.hasMore()) {

            SearchResult roleResult = (SearchResult) roleResults.next();

            String roleResultName = roleResult.getName();
            if ("".equals(roleResultName)) {
                logger.debug("ignoring empty result");
                continue;
            }
            logger.debug("retrieved roleResultName: " + roleResultName + ", setting return list");

            String roleResultValue = (roleResultName.split("="))[1];
            Attributes roleAttrs = roleResult.getAttributes();

            Attribute roleAttr = roleAttrs.get(this.getRoleMemberRDN());
            NamingEnumeration valueEnum = roleAttr.getAll();
            while (valueEnum != null && valueEnum.hasMore()) {
                String value = (String) valueEnum.next();
                logger.debug("belongs to: " + roleResultValue);
                userList.add(value);
            }
        }
        String[] users = null;
        if (null != userList) {
            users = this.toStringArray(userList);
        } else {
            users = new String[]{};
        }
        return users;

    }

    // get Roles for userName from a enumeration of SearchResult's
    private String[] getRoles(String userName, NamingEnumeration roleResults)
            throws NamingException {

        String userDNRegex = "^"+this.getUserNameRDN() + "=" + userName + ",.*,{0,1}" + this.getUserBase()+"$";
        logger.debug(userName + " has regex dn: " + userDNRegex);


        ArrayList roleList = new ArrayList();

        logger.debug("processing results");
        while (roleResults != null && roleResults.hasMore()) {

            SearchResult roleResult = (SearchResult) roleResults.next();

            String roleResultName = roleResult.getName();
            if ("".equals(roleResultName)) {
                logger.debug("ignoring empty result");
                continue;
            }
            logger.debug("retrieved roleResultName: " + roleResultName + ", setting return list");

            String roleResultValue = (roleResultName.split("="))[1];

            logger.debug("roleResultValue: " + roleResultValue);

            Attributes roleAttrs = roleResult.getAttributes();

            if (roleAttrs.size() == 0) { 
               logger.debug("no attributes defined for role: " + roleResultName + " continuing");
               continue;
            }

            Attribute roleAttr = roleAttrs.get(this.getRoleMemberRDN());
            if (null != roleAttr) {
               NamingEnumeration valueEnum = roleAttr.getAll();
               while (valueEnum != null && valueEnum.hasMore()) {
                  String value = (String) valueEnum.next();
                  logger.debug("checking value: " + value);
                  if (value.matches(userDNRegex)) {
                     logger.debug("belongs to: " + roleResultValue);
                     roleList.add(roleResultValue);
                     break;
                  }
               }
            } else {
               logger.debug("roleMemberRdn: " + this.getRoleMemberRDN() + " not found, continuing");
            }
        }

        String[] roles = null;
        if (null != roleList) {
            roles = this.toStringArray(roleList);
        } else {
            roles = new String[]{};
        }
        return roles;
    }

    // convert a List containing Strings to a String[], it would be nice
    // to learn how to use (Object[])List.toArray()
    private String[] toStringArray(List list) {

        return (String[]) list.toArray(new String[list.size()]);
    }

    // search using search base, filter, and declared return attributes
    private NamingEnumeration search(String base, String filter,
                                     String[] returnAttrs)
            throws NamingException {

        SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        constraints.setReturningAttributes(returnAttrs);

        // search the directory based on base, filter, and contraints
        logger.debug("searching, base: " + base + " using filter: " + filter);
        // get established jndi connection
        DirContext initialDirContext = this.getInitialDirContext();
        NamingEnumeration results = initialDirContext.search(base,
                filter,
                constraints);
        return results;
    }

}

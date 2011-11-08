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

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.PoliciesParseException;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.jndi.Jndi;
import com.dtolabs.rundeck.core.jndi.JndiConfig;
import com.dtolabs.rundeck.core.utils.PropertyLookup;
import org.apache.log4j.Logger;

import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;

/**
 * JndiAuthorization, Provides basic authentication using JNDI to lookup user roles.
 *
 * @author Chuck Scott <a href="mailto:chuck@dtosolutions.com">chuck@dtosolutions.com</a>
 */
public class JndiAuthorization extends BaseAclsAuthorization implements Authorization, LegacyAuthorization {
    static final String JNDI_PROPFILE = "jndi.properties";



    /**
     * reference to Jndi
     */
    private final Jndi jndi;


    /**
     * Default constructor
     *
     * @param framework
     * @param aclBaseDir
     * @throws IOException
     * @throws PoliciesParseException 
     */
    public JndiAuthorization(final Framework framework, final File aclBaseDir) throws IOException, PoliciesParseException {
        this(framework,new File(framework.getConfigDir(), JNDI_PROPFILE), aclBaseDir);
    }

    /**
     * Default constructor
     *
     * @param configFile
     * @param aclBaseDir
     * @throws IOException
     * @throws PoliciesParseException 
     */
    public JndiAuthorization(final Framework framework,final File configFile, final File aclBaseDir) throws IOException, PoliciesParseException {
        super(framework, aclBaseDir);

        final PropertyLookup lookup = PropertyLookup.create(configFile);
        final JndiConfigParser cp = new JndiConfigParser(lookup);
        final JndiConfig cfg = cp.parse(); //read JNDI config props

        // connect to jndi server and set appropiate settings related to framework.properties config
        try {
            logger.debug("Connecting to JNDI Server: " + cfg.getConnectionUrl());
            jndi = new Jndi(cfg);
        } catch (NamingException e) {
            throw new AuthorizationException("Caught NameNotFoundException, error: " + e.getMessage() +
                                             ", Unable to connect to JNDI Server: " + cfg.getConnectionUrl()
                                             + " with connectionName: " +
                                             cfg.getConnectionName());
        }
        logger.debug(toString());
    }

    /**
     * reference to logger object
     */
    static Logger logger = Logger.getLogger(JndiAuthorization.class.getName());


    public String[] determineUserRoles(String user) {
        String[] roles;
        try {
            logger.debug("obtaining list of roles for user: " + user);
            roles = jndi.getRoles(user);
        } catch (NamingException e) {
            logger.error("Unable to obtain role memberships for user: " + user);
            throw new AuthorizationException("Caught NamingException, error: " + e.getMessage() +
                    ", Unable to obtain role memberships for user: " + user);
        }
        return roles;
    }

    /**
     * return a string representation of this object
     *
     * @return String
     */
    public String toString() {
        return "JndiAuthorization{" +
               "aclBasedir=" + getAclBasedir() +
               "}";
    }


}

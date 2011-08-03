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

/*
* BaseAclsAuthorization.java
* 
* User: greg
* Created: Jun 27, 2007 4:53:56 PM
* $Id$
*/
package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.providers.PoliciesParseException;
import com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization;
import com.dtolabs.rundeck.core.common.Framework;
import org.apache.log4j.Logger;

import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;


/**
 * BaseAclsAuthorization is a legacy class and is being preserved until it can be depreciated.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @author noahcampbell
 * @version $Revision$
 */
public abstract class BaseAclsAuthorization implements Authorization, LegacyAuthorization {

    /**
     * reference to logger object
     */
    static Logger logger = Logger.getLogger(BaseAclsAuthorization.class);
    private final File aclBasedir;
    
    private final SAREAuthorization authorization;
    private String[] matchedRoles = new String[]{};
    
    

    /**
     * Default constructor
     *
     * @param aclBasedir
     *
     * @throws IOException
     * @throws PoliciesParseException 
     */
    public BaseAclsAuthorization(final Framework framework, final File aclBasedir) throws IOException, PoliciesParseException {
        this.aclBasedir = aclBasedir;
        
        authorization = new SAREAuthorization(aclBasedir);
    }
    
    /**
     * 
     * Return a list of roles declare in the undelrying policy files.
     * 
     * Note: I understand why this is here, but it is ugly, I don't like it and it needs to be 
     * removed. (NSC).
     * 
     * @param aclBasedir Typically $RDECK_BASE/etc
     * @return roles A list of roles present in the *.aclpolicy files.
     * @throws IOException
     */
    public static List<String> listRoles(final File aclBasedir) throws IOException {
        try {
            SAREAuthorization authorization = new SAREAuthorization(aclBasedir);
            return authorization.hackMeSomeRoles();
            
        } catch (PoliciesParseException ppe) {
            return Collections.emptyList();
        }
        
    }

    void setMatchedRoles(final String[] matchedRoles) {
        this.matchedRoles  = matchedRoles;
    }

    /**
     * getMatchedRoles, returns list of matched roles as a String array
     *
     * @return String[]
     */
    public String[] getMatchedRoles() {
        //return copy of mutable array
        return null != matchedRoles ? matchedRoles.clone() : null;
    }

    /**
     * listMatchedRoles, returns list of matched roles as a whitespace seperated String
     *
     * @return String
     */
    public String listMatchedRoles() {
        final String[] matches = getMatchedRoles();
        final StringBuffer mrBuffer = new StringBuffer();
        for (int i = 0; i < matches.length; i++) {
            final String matchedRole = matches[i];
            mrBuffer.append(matchedRole);
            if (i != (matches.length - 1)) {
                mrBuffer.append(" ");
            }
        }
        return mrBuffer.toString();
    }

    /**
     * type/name/module/command authorization
     *
     * @param user
     * @param project
     * @param resourceType
     * @param resourceName
     * @param module
     * @param commandName
     *
     * @return
     *
     * @throws AuthorizationException
     */
    boolean authorize(final String user, final String project, final String resourceType, final String resourceName,
                              final String module, final String commandName)
        throws AuthorizationException {

        logger.debug("authorize(), user: " + user + ", " +
                     " project: " + project + ", " +
                     " deploymentType: " + resourceType + ", " +
                     " deploymentName: " + resourceName + ", " +
                     " module: " + module + ", " + " command: " + commandName);

        // Allow access to base modules.
        // TODO: can this be refactored? 
        if(null == project){
            return true;
        }
        
        // get current time in day/hour/minute int format and create a conforming timeandday string
        final Calendar rightNow = Calendar.getInstance();
        // Subtract 1 before we create the TimeanddayExp object which requires
        // day to be between 0 and 6, not 1 and 7.
        final int day = rightNow.get(Calendar.DAY_OF_WEEK) - 1;
        final int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        final int minute = rightNow.get(Calendar.MINUTE);
        final String timeandday = new TimeanddayExp(new Integer(day).toString(),
                                                    new Integer(hour).toString(),
                                                    new Integer(minute).toString()).toString();


        // Consult with an subclass to get a list of role memberships for user
        final String[] roles = determineUserRoles(user);
        setMatchedRoles(roles);

        // shouldn't happen
        if (null == roles) {
            logger.error("Unable to obtain role memberships for user: " + user);
            throw new AuthorizationException("Unable to obtain role memberships for user: " + user + " unknown error");
        }

        // if user has no roles, return false
        if (roles.length == 0) {
            logger.debug("no roles defined for user: " + user + " , returning false");
            return false;
        }
        Map<String, String> moduleResource = new HashMap<String, String>();
        moduleResource.put("module", module);
        moduleResource.put("name", commandName);
        
        Subject subject = new Subject();
        subject.getPrincipals().add(new Username(user));
        for(String role : roles) {
            subject.getPrincipals().add(new Group(role));
        }
        
        Set<Attribute> environment = new HashSet<Attribute>();
        environment.add(new Attribute(URI.create("http://dtolabs.com/rundeck/env/resource-type"), resourceType));
        environment.add(new Attribute(URI.create("http://dtolabs.com/rundeck/env/resource-name"), resourceName));
        environment.add(new Attribute(URI.create("http://dtolabs.com/rundeck/env/project"), project));
        environment.add(new Attribute(URI.create("http://dtolabs.com/rundeck/env/now.cron"), timeandday));
        Decision decision = this.authorization.evaluate(moduleResource, subject, "EXECUTE", environment);

        return decision.isAuthorized();

    }


    /**
     * script authorization
     *
     * @param user user name
     * @param project project name
     * @param adhocScript script to execute
     *
     * @return
     *
     * @throws AuthorizationException
     */
    public boolean authorizeScript(final String user, final String project, final String adhocScript)
        throws AuthorizationException {

        if(logger.isDebugEnabled()) {
            logger.debug("authorize(), user: " + user + ", " +
                     " project: " + project + ", " +
                     " adhocScript: " + adhocScript );
        }

        // Allow access to base modules
        if (null == project) {
            return true;
        }
        
        // get current time in day/hour/minute int format and create a conforming timeandday string
        final Calendar rightNow = Calendar.getInstance();
        // substract 1 before we create the Timeandday object which requires
        // day to be between 0 and 6, not 1 and 7.
        final int day = rightNow.get(Calendar.DAY_OF_WEEK) - 1;
        final int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        final int minute = rightNow.get(Calendar.MINUTE);
        final String timeandday = new TimeanddayExp(new Integer(day).toString(),
                                                    new Integer(hour).toString(),
                                                    new Integer(minute).toString()).toString();

        // consult with Jndi to get a list of role memberships for user
        final String roles[];
        roles = determineUserRoles(user);
        setMatchedRoles(roles);

        // shouldn't happen
        if (null == roles) {
            logger.error("Unable to obtain role memberships for user: " + user);
            throw new AuthorizationException("Unable to obtain role memberships for user: " + user + " unknown error");
        }

        // if user has no roles, return false
        if (roles.length == 0) {
            logger.debug("no roles defined for user: " + user + " , returning false");
            return false;
        }
        
        Map<String,String> resource = new HashMap<String,String>();
        resource.put("job", adhocScript);
        
        Subject subject = new Subject();
        subject.getPrincipals().add(new Username(user));
        for(String role : roles) {
            subject.getPrincipals().add(new Group(role));
        }
        
        Set<Attribute> environment = new HashSet<Attribute>();
        environment.add(new Attribute(URI.create("http://dtolabs.com/rundeck/env/project"), project));
        environment.add(new Attribute(URI.create("http://dtolabs.com/rundeck/env/now.cron"), timeandday));
        
        Decision decision = this.authorization.evaluate(resource, subject, "EXECUTE", environment);

        if(logger.isDebugEnabled()) {
            logger.debug(user + " authorized: " + decision);
        }

        return decision.isAuthorized();

    }

    public abstract String[] determineUserRoles(String user);

    /**
     * return a string representation of this object
     *
     * @return String
     */
    public String toString() {
        return "BaseAclsAuthorization{" +
               "aclBasedir=" + aclBasedir +
               "}";
    }

    public File getAclBasedir() {
        return aclBasedir;
    }
    
    public Decision evaluate(Map<String, String> resource, Subject subject, String action,
            Set<Attribute> environment) {
        return this.authorization.evaluate(resource, subject, action, environment);
    }

    public Set<Decision> evaluate(Set<Map<String, String>> resources, Subject subject,
            Set<String> action, Set<Attribute> environment) {
        return this.authorization.evaluate(resources, subject, action, environment);
    }
}

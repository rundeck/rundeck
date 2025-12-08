/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.jaas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base class for JAAS LoginModules.
 * Replaces org.eclipse.jetty.jaas.spi.AbstractLoginModule
 * 
 * Provides:
 * - Standard JAAS lifecycle (initialize, login, commit, abort, logout)
 * - Authentication state management
 * - Principal management
 * - Callback handling helpers
 * - Debug logging support
 */
public abstract class AbstractLoginModule implements LoginModule {
    private static final Logger log = LoggerFactory.getLogger(AbstractLoginModule.class);
    
    private Subject subject;
    private CallbackHandler callbackHandler;
    private boolean debug;
    
    // Authentication state
    private boolean authenticated;
    private boolean committed;
    
    // Principals to be added to Subject
    private Principal userPrincipal;
    private List<Principal> rolePrincipals;
    
    // Credentials from callbacks
    private String username;
    private Object credentials;
    
    /**
     * Initialize this LoginModule.
     * 
     * @param subject The Subject to be authenticated
     * @param callbackHandler A CallbackHandler for communicating with the end user
     * @param sharedState State shared with other configured LoginModules
     * @param options Options specified in the login Configuration
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, 
                          Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        
        // Check for debug option
        if (options.containsKey("debug")) {
            this.debug = Boolean.parseBoolean(options.get("debug").toString());
        }
        
        debug("AbstractLoginModule.initialize() called");
    }
    
    /**
     * Authenticate the user by prompting for a username and password.
     * 
     * @return true if authentication succeeded, false otherwise
     * @throws LoginException if this LoginModule is unable to perform the authentication
     */
    @Override
    public boolean login() throws LoginException {
        try {
            debug("AbstractLoginModule.login() called");
            
            // Get username and credentials via callbacks
            if (callbackHandler == null) {
                throw new LoginException("No CallbackHandler available");
            }
            
            Object[] credentials = getCallBackAuth();
            this.username = (String) credentials[0];
            this.credentials = credentials[1];
            
            if (username == null) {
                debug("No username provided");
                setAuthenticated(false);
                return false;
            }
            
            // Get user info from subclass
            UserInfo userInfo = getUserInfo(username);
            if (userInfo == null) {
                debug("No UserInfo found for: " + username);
                setAuthenticated(false);
                return false;
            }
            
            // Check credentials
            if (userInfo.getCredential() != null && !userInfo.getCredential().check(this.credentials)) {
                debug("Credential check failed for: " + username);
                setAuthenticated(false);
                return false;
            }
            
            // Authentication succeeded - prepare principals
            this.userPrincipal = new RundeckPrincipal(username);
            this.rolePrincipals = new ArrayList<>();
            for (String roleName : userInfo.getRoleNames()) {
                this.rolePrincipals.add(new RundeckRole(roleName));
            }
            
            setAuthenticated(true);
            debug("Authentication succeeded for: " + username);
            return true;
            
        } catch (IOException | UnsupportedCallbackException e) {
            throw new LoginException("Callback failed: " + e.getMessage());
        } catch (Exception e) {
            debug("Exception during login: " + e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
            throw new LoginException("Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Commit the authentication (add Principals to Subject).
     * 
     * @return true if commit succeeded
     * @throws LoginException if commit fails
     */
    @Override
    public boolean commit() throws LoginException {
        debug("AbstractLoginModule.commit() called, authenticated=" + authenticated);
        
        if (!authenticated) {
            setCommitted(false);
            clearPrincipals();
            return false;
        }
        
        // Add principals to subject
        if (userPrincipal != null) {
            subject.getPrincipals().add(userPrincipal);
        }
        if (rolePrincipals != null) {
            subject.getPrincipals().addAll(rolePrincipals);
        }
        
        setCommitted(true);
        debug("Commit succeeded, added " + (rolePrincipals != null ? rolePrincipals.size() : 0) + " role(s)");
        return true;
    }
    
    /**
     * Abort the authentication attempt.
     * 
     * @return true if abort succeeded
     * @throws LoginException if abort fails
     */
    @Override
    public boolean abort() throws LoginException {
        debug("AbstractLoginModule.abort() called");
        clearPrincipals();
        setAuthenticated(false);
        setCommitted(false);
        return true;
    }
    
    /**
     * Logout the user (remove Principals from Subject).
     * 
     * @return true if logout succeeded
     * @throws LoginException if logout fails
     */
    @Override
    public boolean logout() throws LoginException {
        debug("AbstractLoginModule.logout() called");
        
        // Remove principals from subject
        if (userPrincipal != null) {
            subject.getPrincipals().remove(userPrincipal);
        }
        if (rolePrincipals != null) {
            subject.getPrincipals().removeAll(rolePrincipals);
        }
        
        clearPrincipals();
        setAuthenticated(false);
        setCommitted(false);
        return true;
    }
    
    /**
     * Get user information for the specified username.
     * Subclasses must implement this to provide user authentication data.
     * 
     * @param username The username to look up
     * @return UserInfo containing credentials and roles, or null if user not found
     * @throws Exception if lookup fails
     */
    public abstract UserInfo getUserInfo(String username) throws Exception;
    
    /**
     * Helper method to perform callbacks for username and password.
     * Mimics Jetty's callback behavior.
     * 
     * @return Array of [username, credentials]
     * @throws IOException if callback I/O fails
     * @throws UnsupportedCallbackException if callback not supported
     */
    protected Object[] getCallBackAuth() throws IOException, UnsupportedCallbackException {
        NameCallback nameCallback = new NameCallback("Username: ");
        org.rundeck.jaas.callback.ObjectCallback objectCallback = new org.rundeck.jaas.callback.ObjectCallback();
        PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);
        
        Callback[] callbacks = new Callback[] { nameCallback, objectCallback, passwordCallback };
        callbackHandler.handle(callbacks);
        
        String username = nameCallback.getName();
        Object credentials = objectCallback.getObject();
        
        // If no object credential, use password
        if (credentials == null) {
            char[] password = passwordCallback.getPassword();
            credentials = password;
        }
        
        return new Object[] { username, credentials };
    }
    
    /**
     * Debug logging helper.
     * 
     * @param message The message to log
     */
    protected void debug(String message) {
        if (debug) {
            log.debug(message);
        }
    }
    
    /**
     * Clear stored principals.
     */
    private void clearPrincipals() {
        userPrincipal = null;
        rolePrincipals = null;
        username = null;
        credentials = null;
    }
    
    // State management methods
    
    protected boolean isAuthenticated() {
        return authenticated;
    }
    
    protected void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    protected boolean isCommitted() {
        return committed;
    }
    
    protected void setCommitted(boolean committed) {
        this.committed = committed;
    }
    
    protected CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }
    
    protected Subject getSubject() {
        return subject;
    }
    
    protected boolean isDebug() {
        return debug;
    }
    
    protected String getUsername() {
        return username;
    }
    
    protected Object getCredentials() {
        return credentials;
    }
}


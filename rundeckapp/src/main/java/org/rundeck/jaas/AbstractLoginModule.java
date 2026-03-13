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

import com.dtolabs.rundeck.core.config.Features;
import grails.util.Holders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

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
            String rawUsername = (String) credentials[0];
            this.credentials = credentials[1];
            
            if (rawUsername == null) {
                debug("No username provided");
                setAuthenticated(false);
                return false;
            }
            
            // Normalize username if feature is enabled
            this.username = normalizeUsername(rawUsername);
            
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
                RundeckRole role = new RundeckRole(roleName);
                this.rolePrincipals.add(role);
                debug("Prepared role principal: " + role + " for user: " + username);
            }
            
            setAuthenticated(true);
            debug("Authentication succeeded for: " + username + " with " + rolePrincipals.size() + " role(s)");
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
            boolean addedUser = subject.getPrincipals().add(userPrincipal);
            debug("Added user principal: " + userPrincipal + " (success: " + addedUser + ")");
        }
        if (rolePrincipals != null) {
            debug("About to add " + rolePrincipals.size() + " role principal(s) to Subject");
            for (Principal rolePrincipal : rolePrincipals) {
                boolean added = subject.getPrincipals().add(rolePrincipal);
                debug("  Adding role principal: " + rolePrincipal + " (success: " + added + ")");
                if (!added) {
                    // Check if it already exists in the set
                    boolean exists = subject.getPrincipals().contains(rolePrincipal);
                    debug("    Role principal NOT added. Already exists: " + exists);
                    // Check what's actually in the Subject
                    for (Principal p : subject.getPrincipals()) {
                        if (p.getName().equals(rolePrincipal.getName())) {
                            debug("    Found principal with same name: " + p + " (class: " + p.getClass().getName() + ")");
                            debug("    Equals check: " + p.equals(rolePrincipal) + ", Hash: " + p.hashCode() + " vs " + rolePrincipal.hashCode());
                        }
                    }
                }
            }
        }
        
        setCommitted(true);
        debug("Commit succeeded. Subject now has " + subject.getPrincipals().size() + " total principal(s)");
        // List all principals in the Subject
        for (Principal p : subject.getPrincipals()) {
            debug("  Subject principal: " + p);
        }
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
     * @throws LoginException if login fails
     */
    protected Object[] getCallBackAuth() throws IOException, UnsupportedCallbackException, LoginException {
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
    
    /**
     * Create standard callbacks for username/password/object.
     * Helper method for subclasses that override login().
     */
    protected Callback[] configureCallbacks() {
        return new Callback[] {
            new NameCallback("Username: "),
            new org.rundeck.jaas.callback.ObjectCallback(),
            new PasswordCallback("Password: ", false)
        };
    }
    
    // Case-insensitive username support
    
    /**
     * Get the application context. Can be overridden for testing.
     * @return ApplicationContext or null if not available
     */
    protected ApplicationContext getApplicationContext() {
        return Holders.findApplicationContext();
    }
    
    /**
     * Check if case-insensitive username feature is enabled.
     * Uses reflection to avoid compile-time dependency on Groovy FeatureService.
     * 
     * @return true if feature is enabled, false otherwise
     */
    protected boolean isCaseInsensitiveUsernameEnabled() {
        try {
            ApplicationContext ctx = getApplicationContext();
            if (ctx == null || !ctx.containsBeanDefinition("featureService")) {
                return false;
            }
            
            Object featureService = ctx.getBean("featureService");
            if (featureService == null) return false;
            
            // Grails 7: Use reflection to call: featureService.featurePresent(Features.CASE_INSENSITIVE_USERNAME)
            // Features implements FeaturesDefinition, so use that class for the method signature
            java.lang.reflect.Method method = featureService.getClass().getMethod("featurePresent", com.dtolabs.rundeck.core.config.FeaturesDefinition.class);
            Boolean result = (Boolean) method.invoke(featureService, Features.CASE_INSENSITIVE_USERNAME);
            return result != null && result;
            
        } catch (Exception e) {
            if (debug) {
                log.debug("Unable to check case-insensitive username feature", e);
            }
            return false;
        }
    }
    
    /**
     * Normalize username to lowercase if case-insensitive feature is enabled.
     * This ensures consistent username handling across ACLs, Key Storage, and audit logs.
     * 
     * @param username the username to normalize
     * @return normalized username (lowercase) if feature enabled, original username otherwise
     */
    protected String normalizeUsername(String username) {
        if (username == null) return null;
        if (isCaseInsensitiveUsernameEnabled()) {
            String normalized = username.toLowerCase();
            if (debug && !username.equals(normalized)) {
                log.debug("Normalized username from '{}' to '{}'", username, normalized);
            }
            return normalized;
        }
        return username;
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
    
    public Subject getSubject() {
        return subject;
    }
    
    /**
     * Set the Subject for testing purposes.
     * @param subject The Subject to use
     */
    public void setSubject(Subject subject) {
        this.subject = subject;
    }
    
    /**
     * Get the CallbackHandler.
     * @return The CallbackHandler
     */
    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }
    
    /**
     * Set the CallbackHandler for testing purposes.
     * Allows tests to inject mock callback handlers instead of using direct field access.
     * @param callbackHandler The CallbackHandler to use
     */
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }
    
    protected boolean isDebug() {
        return debug;
    }
    
    /**
     * Set debug mode for testing purposes.
     * @param debug Whether to enable debug logging
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    protected String getUsername() {
        return username;
    }
    
    protected Object getCredentials() {
        return credentials;
    }
    
    /**
     * Wrapper for current user (used by LDAP modules for caching/state).
     * For subclasses that manage user state directly.
     */
    private JAASUserInfo currentUser;
    
    protected void setCurrentUser(JAASUserInfo user) {
        this.currentUser = user;
    }
    
    protected JAASUserInfo getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Simple wrapper around UserInfo to provide compatibility
     * with existing LDAP module code.
     */
    public static class JAASUserInfo {
        private final UserInfo userInfo;
        
        public JAASUserInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
        }
        
        public UserInfo getUserInfo() {
            return userInfo;
        }
        
        public boolean checkCredential(Object credential) {
            if (userInfo.getCredential() == null) {
                return credential == null;
            }
            return userInfo.getCredential().check(credential);
        }
        
        public void fetchRoles() {
            // Placeholder method for compatibility
            // Roles are already fetched in UserInfo
        }
        
        /**
         * Add principals to a Subject based on the UserInfo.
         * Used by tests to manually populate a Subject.
         * @param subject The Subject to add principals to
         */
        public void setJAASInfo(Subject subject) {
            if (userInfo == null) {
                return;
            }
            
            // Add user principal
            RundeckPrincipal userPrincipal = new RundeckPrincipal(userInfo.getUserName());
            subject.getPrincipals().add(userPrincipal);
            
            // Add role principals
            if (userInfo.getRoleNames() != null) {
                for (String roleName : userInfo.getRoleNames()) {
                    RundeckRole rolePrincipal = new RundeckRole(roleName);
                    subject.getPrincipals().add(rolePrincipal);
                }
            }
        }
    }
}


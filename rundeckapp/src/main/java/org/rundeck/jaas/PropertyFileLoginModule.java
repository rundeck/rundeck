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
import javax.security.auth.callback.CallbackHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Base class for property file-based authentication.
 * 
 * DESIGN CHANGE from Jetty JAAS implementation:
 * - Previously extended org.eclipse.jetty.jaas.spi.PropertyFileLoginModule (removed in Jetty 12)
 * - Now extends org.rundeck.jaas.AbstractLoginModule (our standard Java JAAS implementation)
 * - Reads property files directly instead of using Jetty's PropertyUserStore
 * - Maintains same property file format for backward compatibility
 * 
 * Property File Format:
 * username: password [,rolename ...]
 * 
 * Example:
 * admin: MD5:5f4dcc3b5aa765d61d8327deb882cf99,user,admin
 * user1: password,user
 */
public class PropertyFileLoginModule extends AbstractLoginModule {
    private static final Logger log = LoggerFactory.getLogger(PropertyFileLoginModule.class);
    
    protected String propertyFileName;
    protected Properties userProperties;
    protected long lastModified = 0;
    protected boolean hotReload = true;
    
    /**
     * Load or reload the property file if it has been modified.
     * 
     * @throws IOException if file cannot be read
     */
    protected void loadPropertyFile() throws IOException {
        if (propertyFileName == null) {
            throw new IOException("Property file name not configured");
        }
        
        File propFile = new File(propertyFileName);
        if (!propFile.exists()) {
            throw new IOException("Property file not found: " + propertyFileName);
        }
        
        // Check if reload is needed
        long currentModified = propFile.lastModified();
        if (userProperties != null && !hotReload) {
            // Hot reload disabled, don't reload
            return;
        }
        
        if (userProperties != null && currentModified == lastModified) {
            // File hasn't changed
            return;
        }
        
        // Load or reload the file
        debug("Loading property file: " + propertyFileName);
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
        }
        
        // Don't normalize keys - case-insensitive lookup will handle it
        userProperties = props;
        lastModified = currentModified;
        debug("Loaded " + props.size() + " users from " + propertyFileName);
    }
    
    /**
     * Get user info from the property file.
     * 
     * @param username The username to look up
     * @return UserInfo with credentials and roles, or null if user not found
     */
    @Override
    public UserInfo getUserInfo(String username) throws Exception {
        loadPropertyFile();
        
        // When called from AbstractLoginModule.login(), username is already normalized
        // When called directly (e.g., from tests), we need to normalize it
        String lookupUsername = normalizeUsername(username);
        
        if (userProperties == null || !userProperties.containsKey(lookupUsername)) {
            debug("User not found in property file: " + lookupUsername);
            return null;
        }
        
        String value = userProperties.getProperty(lookupUsername);
        if (value == null || value.trim().isEmpty()) {
            debug("Empty value for user: " + username);
            return null;
        }
        
        // Parse format: password [,rolename ...]
        String[] parts = value.split(",");
        String password = parts[0].trim();
        
        List<String> roles = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String role = parts[i].trim();
            if (!role.isEmpty()) {
                roles.add(role);
            }
        }
        
        PasswordCredential credential = PasswordCredential.getCredential(password);
        debug("Found user: " + username + " with " + roles.size() + " role(s)");
        
        return new UserInfo(username, credential, roles);
    }
    
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                          Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        
        // Get the file option and set the property file name
        if (options.containsKey("file")) {
            String fileName = (String) options.get("file");
            setPropertyFileName(fileName);
        }
    }
    
    /**
     * Set the property file name from options.
     */
    public void setPropertyFileName(String fileName) {
        this.propertyFileName = fileName;
    }
    
    /**
     * Set whether to hot-reload the property file when it changes.
     */
    public void setHotReload(boolean hotReload) {
        this.hotReload = hotReload;
    }
    
    /**
     * Check if hot-reload is enabled.
     * @return true if hot-reload is enabled
     */
    public boolean isReloadEnabled() {
        return this.hotReload;
    }
}


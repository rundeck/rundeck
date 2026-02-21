/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.jaas.jetty;

import org.rundeck.jaas.PropertyFileLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.util.Map;

/**
 * JAAS LoginModule that reads username/password/roles from a property file
 * with support for hot-reload when the file changes.
 * 
 * DESIGN CHANGE from Jetty JAAS implementation:
 * - Previously extended org.eclipse.jetty.jaas.spi.AbstractLoginModule
 * - Previously used org.eclipse.jetty.security.PropertyUserStore for file management
 * - Now extends org.rundeck.jaas.PropertyFileLoginModule (our implementation)
 * - Directly manages property file loading and hot-reload without Jetty dependencies
 * 
 * This simplification removes dependency on Jetty's PropertyUserStore while
 * maintaining the same functionality and property file format.
 * 
 * Configuration options:
 * - file: Path to the property file (required)
 * - hotReload: Enable/disable hot reload (default: true)
 * - debug: Enable debug logging (default: false)
 * 
 * Property file format:
 * username: password [,rolename ...]
 * 
 * Example JAAS config:
 * RDpropertyfilelogin {
 *   org.rundeck.jaas.jetty.ReloadablePropertyFileLoginModule required
 *   debug="true"
 *   file="/path/to/realm.properties";
 * };
 */
public class ReloadablePropertyFileLoginModule extends PropertyFileLoginModule {
    private static final Logger log = LoggerFactory.getLogger(ReloadablePropertyFileLoginModule.class);

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                          Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        
        // Get configuration options
        String fileName = (String) options.get("file");
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Property file path must be specified with 'file' option");
        }
        
        setPropertyFileName(fileName);
        
        // Check for hotReload option (default true)
        if (options.containsKey("hotReload")) {
            boolean hotReload = Boolean.parseBoolean((String) options.get("hotReload"));
            setHotReload(hotReload);
        }
        
        debug("ReloadablePropertyFileLoginModule initialized with file: " + fileName);
    }
}

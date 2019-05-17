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
package com.dtolabs.rundeck.core.plugins;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class PluginValidator {
    private static final Logger log = Logger.getLogger(PluginValidator.class.getName());
    public static boolean validate(File pluginFile) {
        if(pluginFile.getName().endsWith(".jar")) {
            return JarPluginProviderLoader.isValidJarPlugin(pluginFile);
        } else if(pluginFile.getName().endsWith(".zip")) {
            try {
                return ScriptPluginProviderLoader.validatePluginMeta(
                        ScriptPluginProviderLoader.loadMeta(pluginFile),
                        pluginFile
                ).getState() == PluginValidation.State.VALID;
            } catch (Exception iex) {
                log.error("Error loading plugin.", iex);
            }
        } else if(pluginFile.getName().endsWith(".groovy")) {
            return true;
        } else {
            log.error("File: ${pluginFile.getName()} is not a valid Rundeck plugin.");
        }
        return false;
    }
}

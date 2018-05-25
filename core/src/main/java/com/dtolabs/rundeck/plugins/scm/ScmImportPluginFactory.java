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

package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Factory for {@link ScmImportPlugin}, interface for SCMImport plugins.
 */
public interface ScmImportPluginFactory {
    /**
     * Create the plugin
     *
     * @param context      context
     * @param input        setup config
     * @param trackedItems tracked items list
     *
     * @return plugin instance
     *
     * @throws ConfigurationException if an error occurs
     */
    ScmImportPlugin createPlugin(ScmOperationContext context, Map<String, String> input, List<String> trackedItems)
            throws ConfigurationException;

    /**
     * Setup properties for the base directory
     *
     * @param basedir project base directory
     *
     * @return setup properties
     */
    List<Property> getSetupPropertiesForBasedir(File basedir);
}

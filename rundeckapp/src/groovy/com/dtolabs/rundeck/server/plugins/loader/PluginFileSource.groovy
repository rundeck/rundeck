/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.server.plugins.loader

/**
 * PluginFileSource provides plugin file manifests and can retrieve the plugin file contents
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-07-18
 */
public interface PluginFileSource {
    /**
     * List manifests
     * @return
     */
    List<PluginFileManifest> listManifests()
    /**
     * Return a {@link PluginFileContents} for the given manifest, or null if it is not available
     * @param manifest
     * @return
     */
    PluginFileContents getContentsForPlugin(PluginFileManifest manifest)
}

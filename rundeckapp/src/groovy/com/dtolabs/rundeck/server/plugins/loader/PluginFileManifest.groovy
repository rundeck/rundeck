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
 * PluginFileManifest describes a plugin file
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-07-18
 */
interface PluginFileManifest {
    /**
     * Descriptive name
     * @return
     */
    String getName()
    /**
     * Textual description
     * @return
     */
    String getDescription()
    /**
     * Filename when stored
     * @return
     */
    String getFileName()
    /**
     * Source url
     * @return
     */
    String getUrl()
    /**
     * Author
     * @return
     */
    String getAuthor()
    /**
     * Version string
     * @return
     */
    String getVersion()
}

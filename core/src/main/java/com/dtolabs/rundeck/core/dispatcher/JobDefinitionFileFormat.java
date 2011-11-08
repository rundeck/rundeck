/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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

package com.dtolabs.rundeck.core.dispatcher;

/*
* DispatcherFileFormat.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 25, 2011 1:42:05 PM
*
*/

/**
 * Supported file formats for jobs
 */
public enum JobDefinitionFileFormat {

    /**
     * xml format
     */
    xml(CentralDispatcher.FORMAT_XML),
    /**
     * yaml format
     */
    yaml(CentralDispatcher.FORMAT_YAML);

    private String name;

    JobDefinitionFileFormat(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

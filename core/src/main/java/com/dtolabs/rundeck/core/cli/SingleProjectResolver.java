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

/*
* SingleProjectResolver.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/12/11 3:48 PM
* 
*/
package com.dtolabs.rundeck.core.cli;

import java.util.*;

/**
 * SingleProjectResolver determines if only a single project exists, and can return the name of it.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface SingleProjectResolver {

    /**
     * @return true if only one project exists
     */
    public boolean hasSingleProject();

    /**
     * @return the name of the single project
     */
    public String getSingleProjectName();
}

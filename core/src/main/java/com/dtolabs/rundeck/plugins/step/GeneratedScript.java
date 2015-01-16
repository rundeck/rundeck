/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* GeneratedScript.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/20/12 11:46 AM
* 
*/
package com.dtolabs.rundeck.plugins.step;

/**
 * GeneratedScript represents either a script and arguments, or a single command to execute on a remote system
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface GeneratedScript {
    /**
     * Return the script to execute
     * @return the script
     */
    public String getScript();

    /**
     * Return arguments to the script
     * @return the args
     */
    public String[] getArgs();

    /**
     * Return the command to execute
     * @return the command
     */
    public String[] getCommand();
}

/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
* NodesGeneratorException.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 19, 2011 12:38:48 PM
*
*/
package com.dtolabs.rundeck.core.common;

/**
 * NodesGeneratorException indicates an error when generating output from nodes data.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodesGeneratorException extends Exception {
    public NodesGeneratorException() {
        super();
    }

    public NodesGeneratorException(String msg) {
        super(msg);
    }

    public NodesGeneratorException(Exception cause) {
        super(cause);
    }

    public NodesGeneratorException(String msg, Exception cause) {
        super(msg, cause);
    }
}

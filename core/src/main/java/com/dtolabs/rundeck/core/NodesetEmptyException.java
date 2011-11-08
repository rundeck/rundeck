/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* NodesetEmptyException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jun 2, 2010 3:06:47 PM
* $Id$
*/
package com.dtolabs.rundeck.core;

import com.dtolabs.rundeck.core.common.NodesSelector;

/**
 * NodesetEmptyException is thrown when execution is not performed because no nodes matched the
 * filter parameters.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodesetEmptyException extends CoreException {

    private NodesSelector nodeset;
    public NodesetEmptyException(final NodesSelector nodeset) {
        super("No matched nodes: " + nodeset);
        this.nodeset = nodeset;
    }


    public NodesSelector getNodeset() {
        return nodeset;
    }
}

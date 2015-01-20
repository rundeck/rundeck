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
* NodesFileGenerator.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 17, 2011 4:48:23 PM
*
*/
package com.dtolabs.rundeck.core.common;

import java.io.IOException;
import java.util.Collection;

/**
 * NodesFileGenerator interface for a file serializer for nodes data
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface NodesFileGenerator {

    /**
     * Add Node object
     *
     * @param node node
     */
    public void addNode(final INodeEntry node) ;

    /**
     * Add all Node objects
     *
     * @param nodes the nodes
     */
    public void addNodes(final Collection<INodeEntry> nodes) ;

    /**
     * Generate output from the provided ndoes.
     * @throws IOException on io error
     * @throws NodesGeneratorException on other error
     */
    public void generate() throws IOException, NodesGeneratorException ;
}

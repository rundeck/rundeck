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
* ResourceXMLReceiver.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Apr 23, 2010 3:38:16 PM
* $Id$
*/
package com.dtolabs.shared.resources;

/**
 * ResourceXMLReceiver is used to receive parsing results from ResourceXMLParser, either as they are parsed, or
 * in entirety as a set of all parsed Entities.  If the {@link #resourceParsed(com.dtolabs.shared.resources.ResourceXMLParser.Entity)} method
 * returns "false", then parsing will cease at this point.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface ResourceXMLReceiver {
    /**
     * Called after an individual resource is parsed, returns false if parsing should stop, or true to continue.
     * @param entity entity
     * @return true if parsing should continue
     */
    public boolean resourceParsed(ResourceXMLParser.Entity entity);

    /**
     * Called after all resources are parsed with the set of parsed entities
     * @param entities entity set
     */
    public void resourcesParsed(ResourceXMLParser.EntitySet entities);
}

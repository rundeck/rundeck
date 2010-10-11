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
* Analyzer.java
* 
* User: greg
* Created: Oct 8, 2009 9:43:35 AM
* $Id$
*/
package com.dtolabs.launcher.check;

import java.io.File;
import java.util.Properties;
import java.util.Collection;

/**
 * Checker performs validity checks on files/dirs and properties
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface Checker {

    /**
     * Check that an expected file or dir exists
     *
     * @param dir file or directory path
     * @param directory if true, the path is a directory
     * @return true if the file/dir exists and is the correct type
     */
    boolean checkFileExists(File dir, boolean directory);

    /**
     * Check that a required property exists or has a certain value
     * @param key property name
     * @param value value, or null
     * @param props properties
     * @return true if the property value matches expectations
     */
    boolean checkPropertyValue(String key, String value, Properties props);


    /**
     * Check required property values
     * @param expectedProps default values for all properties to expect
     * @param props actual property values to check
     * @return count of successfully checked properties
     */
    int checkPropertyValues(Properties expectedProps, Properties props);

    /**
     * Check that the property keys exist
     * @param keys keys
     * @param props properties
     * @return count of existing properties
     */
    int checkPropertiesExist(Collection<String> keys, Properties props);

}

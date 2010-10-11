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
* PolicyAnalyzer.java
* 
* User: greg
* Created: Oct 8, 2009 10:20:52 AM
* $Id$
*/
package com.dtolabs.launcher.check;

import java.io.File;
import java.util.Properties;
import java.util.Collection;

/**
 * PolicyAnalyzer performs checks according to policy of the check being required or expected.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface PolicyAnalyzer {

    /**
     * Check that an expected file or dir exists
     *
     * @param dir the directory or file
     * @param directory if true, it is a directory
     * @return true if the file/dir exists as expected
     */
    boolean requireFileExists(File dir, boolean directory);
    /**
     * Check that an expected file or dir exists
     *
     * @param dir the directory or file
     * @param directory if true, it is a directory
     * @return true if the file/dir exists as expected
     */
    boolean expectFileExists(File dir, boolean directory);

    /**
     * Check that a required property exists or has a certain value
     *
     * @param key             property name
     * @param value           value, or null
     * @param props           properties
     * @return true if the property value matches expectations
     */
    boolean expectPropertyValue(String key, String value, Properties props);

    /**
     * Check that a required property exists or has a certain value
     *
     * @param key             property name
     * @param value           value, or null
     * @param props           properties
     * @return true if the property value matches expectations
     */
    boolean requirePropertyValue(String key, String value, Properties props);

    /**
     * Check required property values
     *
     * @param expectedProps   default values for all properties to expect
     * @param props           actual property values to check
     * @return count of successfully checked properties
     */
    int expectPropertyValues(Properties expectedProps, Properties props);

    /**
     * Check required property values
     *
     * @param expectedProps   default values for all properties to expect
     * @param props           actual property values to check
     * @return count of successfully checked properties
     */
    int requirePropertyValues(Properties expectedProps, Properties props);

    /**
     * Check expected properties exist
     *
     * @param keys   key values expected to exist
     * @param props           actual property values to check
     *
     * @return count of successfully checked properties
     */
    int expectPropertiesExist(Collection<String> keys, Properties props);

    /**
     * Check required properties exist
     *
     * @param keys   key values required to exist
     * @param props           actual property values to check
     *
     * @return count of successfully checked properties
     */
    int requirePropertiesExist(Collection<String> keys, Properties props);
}

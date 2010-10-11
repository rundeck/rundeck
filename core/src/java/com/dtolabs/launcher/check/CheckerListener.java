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
* CheckerListener.java
* 
* User: greg
* Created: Oct 8, 2009 10:25:05 AM
* $Id$
*/
package com.dtolabs.launcher.check;

import java.io.File;

/**
 * CheckerListener listens for check details.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface CheckerListener {

    /**
     * Check has started for the file
     * @param file the file
     */
    public void beginCheckOnFile(File file);

    /**
     * Check has started on a directory
     * @param dir the dir
     */
    public void beginCheckOnDirectory(File dir);


    /**
     * Check has started on a properties file
     * @param file the properties file
     */
    public void beginCheckOnProperties(File file);


    /**
     * The file was seen as expected
     * @param file file
     */
    public void expectedFile(File file);

    /**
     * The directory was seen as expected
     * @param dir the dir
     */
    public void expectedDirectory(File dir);

    /**
     * The property was seen as expected
     * @param key property name
     * @param value property value
     */
    public void expectedPropertyValue(String key, String value);

    /**
     * File was missing
     * @param file the file
     */
    public void missingFile(File file);

    /**
     * Path was a directory, not a file
     * @param file the file
     */
    public void notAFile(File file);

    /**
     * Directory was missing
     * @param dir directory
     */
    public void missingDirectory(File dir);

    /**
     * Path was a file, not a directory
     * @param dir directory
     */
    public void notADirectory(File dir);

    /**
     * Property value was incorrect
     * @param key property name
     * @param value seen value
     * @param expected expected value
     */
    public void incorrectPropertyValue(String key, String value, String expected);

    /**
     * Property was not found in the file
     * @param key expected property
     * @param expected expected value
     */
    public void missingPropertyValue(String key, String expected);

}

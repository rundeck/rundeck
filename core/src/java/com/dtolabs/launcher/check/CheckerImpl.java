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
* AnalyzerImpl.java
* 
* User: greg
* Created: Oct 8, 2009 9:52:51 AM
* $Id$
*/
package com.dtolabs.launcher.check;

import java.util.*;
import java.io.File;

/**
 * CheckerImpl implements {@link Checker}, performs appropriate checks.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class CheckerImpl implements Checker {
    private CheckerListener listener;

    /**
     * Create CheckerImpl, using a listener to report check results to.
     * @param listener listener to receive check reports.
     */
    public CheckerImpl(final CheckerListener listener) {
        this.listener = listener;
    }

    /**
     * Check required property values
     *
     * @param expectedProps default values for all properties to expect
     * @param props         actual property values to check
     *
     * @return count of successfully checked properties
     */
    public int checkPropertyValues(final Properties expectedProps, final Properties props) {
        int count = 0;
        for (final Object o : expectedProps.keySet()) {
            final String key = (String) o;
            if (checkPropertyValue(key, expectedProps.getProperty(key), props)) {
                count++;
            }
        }
        return count;
    }


    public int checkPropertiesExist(final Collection<String> keys, final Properties props) {
        int count = 0;
        for (final String key : keys) {
            if (checkPropertyValue(key, null, props)) {
                count++;
            }
        }
        return count;
    }


    /**
     * Check that a property exists or has a certain value. If expected is null, check that the property exists.
     *
     * @param key      property name
     * @param expected value, or null to check for existence
     * @param props    properties
     *
     * @return true if the property value matches expectations
     */
    public boolean checkPropertyValue(final String key, final String expected, final Properties props) {
        if (!props.containsKey(key)) {
            listener.missingPropertyValue(key, expected);
            return false;
        } else if (null != expected && !expected.equals(props.getProperty(key))) {
            final String seen = props.getProperty(key);
            listener.incorrectPropertyValue(key, seen, expected);
            return false;
        } else if (null == expected && props.containsKey(key)) {
            listener.expectedPropertyValue(key, props.getProperty(key));
            return true;
        }
        listener.expectedPropertyValue(key, expected);
        return true;
    }

    /**
     * Check that a directory or file exists
     *
     * @param dir directory/file to check
     * @param directory if true, expect a directory
     */
    public boolean checkFileExists(final File dir, final boolean directory) {
        if (!dir.exists()) {
            if (directory) {
                listener.missingDirectory(dir);
            } else {
                listener.missingFile(dir);
            }
            return false;
        }
        if (directory && !dir.isDirectory()) {
            listener.notADirectory(dir);
            return false;
        } else if (!directory && !dir.isFile()) {
            listener.notAFile(dir);
            return false;
        } else {
            if (directory) {
                listener.expectedDirectory(dir);
            } else {
                listener.expectedFile(dir);
            }
        }
        return true;
    }

}

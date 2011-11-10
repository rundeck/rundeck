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
* VersionConstants.java
*
* User: greg
* Created: Nov 26, 2008 10:39:12 AM
* $Id$
*/
package com.dtolabs.rundeck.core;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * VersionConstants defines the version and build numbers for the distribution.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public final class VersionConstants {
    /**
     * Version
     */
    public final static String VERSION;
    /**
     * Build number.
     */
    public final static String BUILD;
    /**
     * Buid ident string.
     */
    public final static String VERSION_IDENT;

    static {
        //load rundeck version from properties
        final Properties versionProperties = new Properties();
        try {
            final InputStream resourceAsStream = VersionConstants.class.getClassLoader().getResourceAsStream(
                "META-INF/com/dtolabs/rundeck/core/application.properties");
            if (null != resourceAsStream) {
                versionProperties.load(resourceAsStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        VERSION = versionProperties.getProperty("version.number", "UNKNOWN");
        BUILD = versionProperties.getProperty("version.build", "UNKNOWN");
        VERSION_IDENT = versionProperties.getProperty("version.ident", "UNKNOWN");
    }
}

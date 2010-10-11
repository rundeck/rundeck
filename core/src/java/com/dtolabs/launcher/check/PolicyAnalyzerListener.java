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
* PolicyAnalyzerListener.java
* 
* User: greg
* Created: Oct 8, 2009 10:27:06 AM
* $Id$
*/
package com.dtolabs.launcher.check;

import java.io.File;

/**
 * PolicyAnalyzerListener is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface PolicyAnalyzerListener {

    public void beginCheckOnFile(File file);

    public void beginCheckOnDirectory(File file);

    public void beginCheckOnProperties(File file);

    public void passedFile(File file);

    public void failedFile(File file, boolean missing, boolean incorrectType, boolean invalidated);

    public void passedDirectory(File file);

    public void failedDirectory(File file, boolean missing, boolean incorrectType, boolean invalidated);

    public void failedPropertyValue(String key, String value, String expected, boolean invalidated);

    public void passedPropertyValue(String key, String value);
}

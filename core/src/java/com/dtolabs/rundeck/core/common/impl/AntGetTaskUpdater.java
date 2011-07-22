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
* AntGetTaskUpdater.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 5:07 PM
* 
*/
package com.dtolabs.rundeck.core.common.impl;

import com.dtolabs.rundeck.core.common.FileUpdater;
import com.dtolabs.rundeck.core.common.FileUpdaterException;
import com.dtolabs.rundeck.core.common.URLFileUpdaterFactory;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Get;

import java.io.File;
import java.net.URL;

/**
 * AntGetTaskUpdater is a FileUpdater using the Ant Get task to get a URL.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @deprecated legacy implementation, see {@link URLFileUpdater}
 */
public class AntGetTaskUpdater implements FileUpdater {
    final static Logger logger = Logger.getLogger(AntGetTaskUpdater.class.getName());
    static final boolean USETIMESTAMP = true;

    private URL url;
    private String username;
    private String password;

    public AntGetTaskUpdater(URL url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public static final Factory FACTORY = new Factory();

    public static URLFileUpdaterFactory factory() {
        return FACTORY;
    }

    public static class Factory implements URLFileUpdaterFactory {
        public FileUpdater fileUpdaterFromURL(final URL url, final String username, final String password) {
            return new AntGetTaskUpdater(url, username, password);
        }
    }

    public void updateFile(final File destinationFile) throws FileUpdaterException {

        final Project p = new Project();
        final Task getTask = createTask(url, destinationFile, username, password);
        getTask.setProject(p);
        try {
            getTask.execute();
        } catch (BuildException e) {
            logger.error(
                "Error getting URL <" + url + ">" + (null != username ? "(user: " + username + ", pass: ****) " : "")
                + e
                    .getMessage(), e);
            throw new FileUpdaterException(e);
        }
    }

    private static Task createTask(final URL fileUrl, final File destFile,
                                   final String username, final String password) {

        final Get getTask = new Get();
        getTask.setDest(destFile);
        if (null != username) {
            getTask.setUsername(username);
        }
        if (null != password) {
            getTask.setPassword(password);
        }
        getTask.setSrc(fileUrl);
        getTask.setHttpUseCaches(false);
        getTask.setMaxTime(60);
        /**
         * If it is an empty file, then ignore timestamp check and get it
         */
        if (destFile.length() == 0) {
            getTask.setUseTimestamp(false);
        } else {
            getTask.setUseTimestamp(USETIMESTAMP);
        }


        return getTask;
    }
}

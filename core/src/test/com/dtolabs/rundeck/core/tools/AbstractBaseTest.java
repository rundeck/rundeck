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

package com.dtolabs.rundeck.core.tools;


import junit.framework.TestCase;
import com.dtolabs.launcher.Setup;
import com.dtolabs.rundeck.core.common.Framework;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * This is the base test class for the rdeck framework.
 * configureFramework() must be called prior to any framework based testing
 */
public abstract class AbstractBaseTest extends TestCase {

    //
    // junit exported java properties (e.g. from maven's project.properties)
    //
    public static String ANT_HOME = System.getProperty("ant.home");
    public static String RDECK_HOME = System.getProperty("rdeck.home");
    public static String RDECK_BASE = System.getProperty("rdeck.base");

    //
    // derived modules and projects base
    //
    private static String MODULES_BASE = RDECK_BASE + "/" +"modules";
    private static String PROJECTS_BASE = RDECK_BASE + "/" + "projects";

    private static String EXTENSIONS_BASE = RDECK_HOME + "/" + "lib" + "/" + "extensions";

    //
    // rdeck's derived taskdef and typedef properties
    //
    private static String TASKDEF_PROPERTIES = RDECK_HOME + "/classes/com/dtolabs/rundeck/core/tasks/taskdef.properties";

    private static String TYPEDEF_PROPERTIES = RDECK_HOME + "/classes/com/dtolabs/rundeck/core/types/typedef.properties";

    /** hostname used for local node in test environment */
    public static final String localNodeHostname = "test1";
    // rdeck setup arguments
    public static String[] SETUP_ARGS = {
        "-n", localNodeHostname
    };


    private static String antHome;

    public String getAntHome() {
        return antHome;
    }

    private static String homeDir;

    public String getHomeDir() {
        return homeDir;
    }

    private static String baseDir;

    public String getBaseDir() {
        return baseDir;
    }

    private static String modulesBase;

    public String getModulesBase() {
        return modulesBase;
    }

    private static String projectsBase;

    public String getFrameworkProjectsBase() {
        return projectsBase;
    }

    private static String taskdefProperties;

    public String getTaskdefProperties() {
        return taskdefProperties;
    }

    private static String typedefProperties;

    public String getTypedefProperties() {
        return typedefProperties;
    }

    private static File ctlDavBase;

    public static File getCtlDavBase() {
        return ctlDavBase;
    }

    public static void setCtlDavBase(File ctlDavBase) {
        AbstractBaseTest.ctlDavBase = ctlDavBase;
    }

    public String getExtensionsBase() {
        return EXTENSIONS_BASE;
    }

    public AbstractBaseTest(String name) {
        super(name);
    }

    protected String getExistingFilePath(String filename, String type)
            throws BuildException {

        File file = new File(filename);
        String filenamePath = file.getAbsolutePath();

        if (type.equals("file")) {
            if (!file.exists() || !file.isFile()) {
                throw new BuildException("file: " + filenamePath + " does not exist or is not a regular file");
            }
        } else if (type.equals("dir")) {
            if (!file.exists() || !file.isDirectory()) {
                throw new BuildException("file: " + filenamePath + " does not exist or is not a directory");
            }
        } else {
            throw new BuildException("file type: " + type + " not supported");
        }

        return filenamePath;
    }

    protected Framework getFrameworkInstance() {
        if(null==instance){
            instance = Framework.getInstance(RDECK_BASE, MODULES_BASE, PROJECTS_BASE);
        }
        return instance;
    }

    protected void configureFramework()
            throws BuildException {

        antHome = ANT_HOME;
        homeDir = RDECK_HOME;
        baseDir = RDECK_BASE;
        ctlDavBase = new File(RDECK_BASE,"dav-test");
        modulesBase = MODULES_BASE;
        projectsBase = PROJECTS_BASE;
        taskdefProperties = TASKDEF_PROPERTIES;
        typedefProperties = TYPEDEF_PROPERTIES;
        new File(modulesBase).mkdirs();
        new File(baseDir,"etc").mkdirs();
        ctlDavBase.mkdirs();
        File dummykey = new File(baseDir, "etc/dummy_ssh_key.pub");
        try {
            dummykey.createNewFile();
        } catch (IOException e) {
            throw new BuildException("failed to create dummy keyfile: " + e.getMessage(), e);
        }

        // check to see if Setup was run, if so, just return.
        if (new File(baseDir, "etc" + "/" + "framework.properties").exists()) {
            //System.out.println("Setup already run");
            return;
        }

        final String davUrl;
        try {
            davUrl = ctlDavBase.toURI().toURL().toExternalForm();
        } catch (MalformedURLException e) {
            throw new BuildException("unable to create webdav uri: " + e.getMessage(), e);
        }
        
        final ArrayList argsList = new ArrayList(Arrays.asList(SETUP_ARGS));
        argsList.add("--framework.webdav.uri=" + davUrl);
        argsList.add("--framework.ssh.keypath=" + dummykey.getAbsolutePath());

        try {
            System.out.println("Running Setup");
            Setup setup = new Setup();
            setup.execute((String[]) argsList.toArray(new String[argsList.size()]));
        } catch (Exception e) {
            throw new BuildException("Caught Setup exception: " + e.getMessage(), e);
        }

    }
    Framework instance;

    protected void setUp() {
        configureFramework();
    }

}

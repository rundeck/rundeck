/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.tools;


import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.utils.FileUtils;
import junit.framework.TestCase;
import com.dtolabs.launcher.Setup;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * This is the base test class for the rdeck framework.
 * configureFramework() must be called prior to any framework based testing
 */
public abstract class AbstractBaseTest extends TestCase {

    //
    // junit exported java properties (e.g. from maven's project.properties)
    //
    public static String RDECK_BASE = System.getProperty("rdeck.base","build/rdeck_base");

    //
    // derived modules and projects base
    //
    private static String PROJECTS_BASE = RDECK_BASE + "/" + "projects";


    /** hostname used for local node in test environment */
    public static final String localNodeHostname = "test1";
    // rdeck setup arguments
    public static String[] SETUP_ARGS = {
        "-n", localNodeHostname
    };


    private static String baseDir;

    public String getBaseDir() {
        return baseDir;
    }

    private String projectsBase = PROJECTS_BASE;

    public String getFrameworkProjectsBase() {
        return projectsBase;
    }

    public AbstractBaseTest(String name) {
        super(name);
    }

    public static void generateProjectResourcesFile(final File source, final IRundeckProject frameworkProject){
        generateProjectResourcesFile(source, frameworkProject, false);
    }
    public static void generateProjectResourcesFile(final File source, final IRundeckProject frameworkProject, boolean localExecutor){
        //copy test nodes to resources file
        File resourcesfile = null;
        try {
            resourcesfile = File.createTempFile("resources", ".xml");
            resourcesfile.deleteOnExit();
            FileUtils.copyFileStreams(source, resourcesfile);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }
        Properties properties = new Properties();
        properties.put("resources.source.1.type", "file");
        properties.put("resources.source.1.config.file", resourcesfile.getAbsolutePath());
        properties.put("resources.source.1.config.generateFileAutomatically", "false");
        properties.put("resources.source.1.config.includeServerNode", "true");
        if(localExecutor) {
            properties.put("resources.source.2.type", "local");
            properties.put("service.NodeExecutor.default.provider", "local");
        }

        Set<String> prefixes=new HashSet<String>();
        prefixes.add("resources.source");
        frameworkProject.mergeProjectProperties(properties,prefixes);
    }
    public static Properties generateProjectResourcesFile(final File source){
        //copy test nodes to resources file
        File resourcesfile = null;
        try {
            resourcesfile = File.createTempFile("resources", ".xml");
            resourcesfile.deleteOnExit();
            FileUtils.copyFileStreams(source, resourcesfile);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }
        Properties properties = new Properties();
        properties.put("resources.source.1.type", "file");
        properties.put("resources.source.1.config.file", resourcesfile.getAbsolutePath());
        properties.put("resources.source.1.config.generateFileAutomatically", "false");
        properties.put("resources.source.1.config.includeServerNode", "true");
        properties.put("resources.source.2.type", "local");
        properties.put("service.NodeExecutor.default.provider", "local");
        return properties;
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
            instance = createTestFramework();
        }
        return instance;
    }

    public static Framework createTestFramework() {
        ServiceSupport serviceSupport = new ServiceSupport();
        BaseFrameworkExecutionServices services = new BaseFrameworkExecutionServices();
        serviceSupport.setExecutionServices(services);
        Framework framework = AbstractBaseTest.createTestFramework(serviceSupport);
        services.setFramework(framework);
        return framework;
    }
    public static Framework createTestFramework(IFrameworkServices services) {
        if(!new File(RDECK_BASE).exists()) {
            configureFramework();
        }
        return FrameworkFactory.createForFilesystem(RDECK_BASE, services);
    }

    protected static void configureFramework()
            throws BuildException {

        baseDir = RDECK_BASE;
        if(new File(baseDir).exists()){
            FileUtils.deleteDir(new File(baseDir));
        }
        File projectsDir = new File(PROJECTS_BASE);
        FileUtils.deleteDir(projectsDir);
        projectsDir.mkdirs();
        new File(baseDir,"etc").mkdirs();
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

        final ArrayList argsList = new ArrayList(Arrays.asList(SETUP_ARGS));
        argsList.add("--framework.ssh.keypath=" + dummykey.getAbsolutePath());
        argsList.add("-d");
        argsList.add(new File(baseDir).getAbsolutePath());


        try {
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

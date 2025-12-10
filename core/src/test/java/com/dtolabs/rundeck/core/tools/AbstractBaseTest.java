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
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
        final File etcDir = new File(baseDir, "etc");
        etcDir.mkdirs();
        File dummykey = new File(baseDir, "etc/dummy_ssh_key.pub");
        try {
            dummykey.createNewFile();
        } catch (IOException e) {
            throw new BuildException("failed to create dummy keyfile: " + e.getMessage(), e);
        }

        // check to see if Setup was run, if so, just return.
        final File fwkPropsFile = new File(etcDir, "framework.properties");
        if (fwkPropsFile.exists()) {
            //System.out.println("Setup already run");
            return;
        }

        //
        //set up minimal framework.properties and project.properties files and dir structure
        //

        final File varDir = new File(baseDir, "var");
        final File tmpDir = new File(varDir, "tmp");
        tmpDir.mkdirs();
        try(OutputStream out = new FileOutputStream(fwkPropsFile)) {
            Properties props = new Properties();
            props.setProperty("framework.ssh.keypath", dummykey.getAbsolutePath());

            /**
             * framework.server.name = test1
             * framework.server.hostname = test1
             * framework.server.port = 4440
             * framework.server.url = http://test1:4440/rundeck
             *
             * # ----------------------------------------------------------------
             * # Installation locations
             * # ----------------------------------------------------------------
             *
             * rdeck.base=/Users/gschueler/devel/runner-test/rundeck/core/build/rdeck_base
             *
             * framework.projects.dir=/Users/gschueler/devel/runner-test/rundeck/core/build/rdeck_base/projects
             * framework.etc.dir=/Users/gschueler/devel/runner-test/rundeck/core/build/rdeck_base/etc
             * framework.var.dir=/Users/gschueler/devel/runner-test/rundeck/core/build/rdeck_base/var
             * framework.tmp.dir=/Users/gschueler/devel/runner-test/rundeck/core/build/rdeck_base/var/tmp
             * framework.logs.dir=/Users/gschueler/devel/runner-test/rundeck/core/build/rdeck_base/var/logs
             * framework.libext.dir=/Users/gschueler/devel/runner-test/rundeck/core/build/rdeck_base/libext
             * framework.ssh.user = username
             *
             * # ssh connection timeout after a specified number of milliseconds.
             * # "0" value means wait forever.
             * framework.ssh.timeout = 0
             *
             * # ----------------------------------------------------------------
             * # Auto generated server UUID: @rundeck.server.uuid@
             * # ----------------------------------------------------------------
             * rundeck.server.uuid = @rundeck.server.uuid@
             */
            props.setProperty("framework.server.name", localNodeHostname);
            props.setProperty("framework.server.hostname", localNodeHostname);
            props.setProperty("framework.server.port", "4440");
            props.setProperty("framework.server.url", "http://" + localNodeHostname + ":4440/rundeck");
            props.setProperty("rdeck.base", new File(baseDir).getAbsolutePath());
            props.setProperty("framework.projects.dir", new File(PROJECTS_BASE).getAbsolutePath());
            props.setProperty("framework.etc.dir", etcDir.getAbsolutePath());
            props.setProperty("framework.var.dir", varDir.getAbsolutePath());

            props.setProperty("framework.tmp.dir", tmpDir.getAbsolutePath());
            props.setProperty("framework.logs.dir", new File(varDir,"logs").getAbsolutePath());
            props.setProperty("framework.libext.dir", new File(baseDir, "libext").getAbsolutePath());
            props.setProperty("framework.ssh.user", "username");
            props.setProperty("framework.ssh.timeout", "0");
            props.setProperty("rundeck.server.uuid", "@rundeck.server.uuid@");
            props.store(out, "framework properties");
        } catch (IOException e) {
            throw new BuildException("failed to create framework properties: " + e.getMessage(), e);
        }

        File projectsProps = new File(etcDir, "projects.properties");
        try (OutputStream out = new FileOutputStream(projectsProps)) {
            Properties props = new Properties();
            /**
             * project.dir = /Users/gschueler/devel/runner-test/rundeck/core/build/rdeck_base/projects/${project.name}
             * #
             * # The base directory of project specific configuration files
             * #
             * project.etc.dir = /Users/gschueler/devel/runner-test/rundeck/core/build/rdeck_base/projects/${project.name}/etc
             *
             * #
             * # The default resources file
             * #
             * resources.source.1.type=file
             * resources.source.1.config.file=/Users/gschueler/devel/runner-test/rundeck/core/build/rdeck_base/projects/${project.name}/etc/resources.xml
             * resources.source.1.config.format=resourcexml
             * resources.source.1.config.includeServerNode=true
             * resources.source.1.config.generateFileAutomatically=true
             */
            props.setProperty("project.dir", new File(PROJECTS_BASE, "${project.name}").getAbsolutePath());
            props.setProperty("project.etc.dir", new File(PROJECTS_BASE, "${project.name}/etc").getAbsolutePath());
            props.setProperty("resources.source.1.type", "file");
            props.setProperty("resources.source.1.config.file",
                new File(PROJECTS_BASE, "${project.name}/etc/resources.xml").getAbsolutePath());
            props.setProperty("resources.source.1.config.format", "resourcexml");
            props.setProperty("resources.source.1.config.includeServerNode", "true");
            props.setProperty("resources.source.1.config.generateFileAutomatically", "true");
            props.store(out, "projects properties");
        } catch (IOException e) {
            throw new BuildException("failed to create projects properties: " + e.getMessage(), e);
        }

    }
    Framework instance;

    protected void setUp() {
        configureFramework();
    }

}

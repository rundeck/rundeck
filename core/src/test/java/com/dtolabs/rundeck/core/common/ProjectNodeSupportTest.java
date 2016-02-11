package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ProjectNodeSupportTest extends AbstractBaseTest {
    private final String PROJECT_NAME = "ProjectNodeSupportTest";

    File projectBasedir;
    File nodesfile;
    File projectPropsFile;
    public ProjectNodeSupportTest() {
        super("ProjectNodeSupportTest");
    }

    @Before
    public void setUp() {
        projectBasedir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        nodesfile = new File(projectBasedir, "/etc/resources.xml");
        projectPropsFile = new File(projectBasedir, "/etc/project.properties");
    }

    @After
    public void tearDown() throws Exception {
        File projectdir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        FileUtils.deleteDir(projectdir);
        getFrameworkInstance().getFilesystemFrameworkProjectManager().removeFrameworkProject(PROJECT_NAME);
    }

    @Test
    public void testValidateResourceProviderURL() throws Exception{
        FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                                           new File(getFrameworkProjectsBase()),
                                                           getFrameworkInstance().getFilesystemFramework(), getFrameworkInstance().getFilesystemFrameworkProjectManager());
        ProjectNodeSupport projectNodeSupport = new ProjectNodeSupport(project,
                                                                       getFrameworkInstance()
                                                                                          .getResourceFormatGeneratorService(),
                                                                       getFrameworkInstance()
                                                                                          .getResourceModelSourceService()
        );

        //use invalid protocol
        try {
            projectNodeSupport.validateResourceProviderURL("ftp://test.com/test");
            fail("Should fail");
        } catch (UpdateUtils.UpdateException e) {
            assertEquals("URL protocol not allowed: ftp", e.getMessage());
        }
        //use valid protocol
        try {
            projectNodeSupport.validateResourceProviderURL("http://test.com/test");
        } catch (UpdateUtils.UpdateException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        //use valid protocol
        try {
            projectNodeSupport.validateResourceProviderURL("https://test.com/test");
        } catch (UpdateUtils.UpdateException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        //use valid protocol
        try {
            projectNodeSupport.validateResourceProviderURL("file:///tmp/test");
        } catch (UpdateUtils.UpdateException e) {
            fail("Unexpected exception: " + e.getMessage());
        }

    }public void writeProps(final Properties props, final File outputFile) throws IOException {

        FileOutputStream fos = new FileOutputStream(outputFile);
        try{
            props.store(fos,null);
        }finally{
            fos.close();
        }
    }
    public void loadProps(final Properties props, final File file) throws IOException{

        FileInputStream fis = new FileInputStream(file);
        try{
            props.load(fis);
        }finally{
            fis.close();
        }
    }

    @Test
    public void testIsAllowedProviderURL() throws Exception{
        FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                                           new File(getFrameworkProjectsBase()),
                                                           getFrameworkInstance().getFilesystemFramework(),
                                                           getFrameworkInstance().getFilesystemFrameworkProjectManager(),
                                                           FrameworkFactory.createNodesFactory(getFrameworkInstance().getFilesystemFramework()));

        //set project providerURL and allowed URL regexes
        Properties orig = new Properties();
        loadProps(orig,projectPropsFile);

        Properties newProps = new Properties();
        loadProps(newProps,projectPropsFile);
        final String providerURL = new File(
                "src/test/resources/com/dtolabs/rundeck/core/common/test-nodes2.xml")
                .toURI().toURL().toExternalForm();
        newProps.setProperty("project.resources.url", providerURL);
        newProps.setProperty(FrameworkProject.PROJECT_RESOURCES_ALLOWED_URL_PREFIX + "0", "^http://example.com/test1$");
        newProps.setProperty(FrameworkProject.PROJECT_RESOURCES_ALLOWED_URL_PREFIX + "1",
                             "^http://example.com/test2/.*$");
        newProps.setProperty(FrameworkProject.PROJECT_RESOURCES_ALLOWED_URL_PREFIX + "2",
                             "^https://example.com/.*?/monkey$");

        project.setProjectProperties(newProps);

        project = FrameworkProject.create(PROJECT_NAME,
                                          new File(getFrameworkProjectsBase()),
                                          getFrameworkInstance().getFilesystemFramework(),getFrameworkInstance().getFilesystemFrameworkProjectManager());
        ProjectNodeSupport projectNodeSupport = new ProjectNodeSupport(project,
                                                                       getFrameworkInstance()
                                                                               .getResourceFormatGeneratorService(),
                                                                       getFrameworkInstance()
                                                                               .getResourceModelSourceService()
        );

        //provider URL for the project should work
        assertTrue(projectNodeSupport.isAllowedProviderURL(providerURL));

        //valid URL match 0
        assertTrue(projectNodeSupport.isAllowedProviderURL("http://example.com/test1"));
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2"));

        //valid URL match 1
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/elephant"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/bologna/something"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/elf"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/bologna"));

        //valid URL match 2
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/blah/monkey"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/blah/blee/monkey"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/xylophone/monkey"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/a/lonely/xylophone/monkey"));

        //invalid file URL
        assertFalse(projectNodeSupport.isAllowedProviderURL("file:///tmp/test"));

        //invalid https URL
        assertFalse(projectNodeSupport.isAllowedProviderURL("https://example.com/test1"));
        //invalid http URL
        assertFalse(projectNodeSupport.isAllowedProviderURL("http://example.com/blah/monkey"));

        //set some framework properties to intersect the regexes


        final File frameworkProps = new File(getFrameworkInstance().getBaseDir(),
                                             "/etc/framework.properties");
        Properties origFProps = new Properties();
        loadProps(origFProps,frameworkProps);

        Properties newFProps = new Properties();
        loadProps(newFProps,frameworkProps);
        newFProps.setProperty(FrameworkProject.FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX+"0", "^https?://example.com/test[\\d]$");
        newFProps.setProperty(FrameworkProject.FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX + "1",
                              "^http://example.com/test2/(elf|bologna)$");
        newFProps.setProperty(FrameworkProject.FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX+"2",
                              "^https://example.com/.*?xylophone/monkey$");
        newFProps.setProperty(FrameworkProject.FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX+"3",
                              "^file:///tmp/test.*$");

        writeProps(newFProps,frameworkProps);

        //load framework instance
        Framework framework = Framework.getInstance(getFrameworkInstance().getBaseDir().getAbsolutePath(),
                                                    getFrameworkProjectsBase());
        project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()),
                                          framework.getFilesystemFramework(), framework.getFilesystemFrameworkProjectManager());
        projectNodeSupport = new ProjectNodeSupport(project,
                                                                       getFrameworkInstance()
                                                                               .getResourceFormatGeneratorService(),
                                                                       getFrameworkInstance()
                                                                               .getResourceModelSourceService()
        );
        //provider URL for the project should work
        assertTrue(projectNodeSupport.isAllowedProviderURL(providerURL));

        //valid URL match 0
        assertTrue(projectNodeSupport.isAllowedProviderURL("http://example.com/test1"));
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2"));

        //valid URL match 1
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/"));
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/elephant"));
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/bologna/something"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/elf"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/bologna"));

        //valid URL match 2
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/blah/monkey"));
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/blah/blee/monkey"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/xylophone/monkey"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/a/lonely/xylophone/monkey"));

        //invalid file URL
        assertFalse(projectNodeSupport.isAllowedProviderURL("file:///tmp/test"));

        //invalid https URL
        assertFalse(projectNodeSupport.isAllowedProviderURL("https://example.com/test1"));
        //invalid http URL
        assertFalse(projectNodeSupport.isAllowedProviderURL("http://example.com/blah/monkey"));


        //remove project specific props
        writeProps(orig,projectPropsFile);
        project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()),
                                          framework.getFilesystemFramework(), framework.getFilesystemFrameworkProjectManager());
        projectNodeSupport = new ProjectNodeSupport(project,
                                                                       getFrameworkInstance()
                                                                               .getResourceFormatGeneratorService(),
                                                                       getFrameworkInstance()
                                                                               .getResourceModelSourceService()
        );
        //provider URL for the project should now fail
        assertFalse(projectNodeSupport.isAllowedProviderURL(providerURL));

        //valid URL match 0
        assertTrue(projectNodeSupport.isAllowedProviderURL("http://example.com/test1"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2"));

        //valid URL match 1
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/"));
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/elephant"));
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/bologna/something"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/elf"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTP://example.com/test2/bologna"));

        //valid URL match 2
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/blah/monkey"));
        assertFalse(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/blah/blee/monkey"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/xylophone/monkey"));
        assertTrue(projectNodeSupport.isAllowedProviderURL("HTTPs://example.com/a/lonely/xylophone/monkey"));

        //invalid file URL
        assertTrue(projectNodeSupport.isAllowedProviderURL("file:///tmp/test"));

        //invalid https URL
        assertTrue(projectNodeSupport.isAllowedProviderURL("https://example.com/test1"));
        //invalid http URL
        assertFalse(projectNodeSupport.isAllowedProviderURL("http://example.com/blah/monkey"));

        //restore fprops
        writeProps(origFProps,frameworkProps);
    }

}
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

package com.dtolabs.shared.resources;
/*
* TestResourceXMLParser.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Apr 26, 2010 3:58:04 PM
* $Id$
*/

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.HashSet;

public class TestResourceXMLParser extends TestCase {
    File dnefile1;
    File testfile1;
    File testfile2;
    File testfile3;
    File testfile4;
    File testfile5;
    File invalidfile1;
    File invalidfile2;

    public TestResourceXMLParser(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestResourceXMLParser.class);
    }

    protected void setUp() throws Exception {
        dnefile1 = new File("test-does-not-exist.xml");
        testfile1 = new File("src/test/com/dtolabs/shared/resources/test-resources1.xml");
        testfile2 = new File("src/test/com/dtolabs/shared/resources/test-resources2.xml");
        testfile3 = new File("src/test/com/dtolabs/shared/resources/test-resources3.xml");
        testfile4 = new File("src/test/com/dtolabs/shared/resources/test-resources4.xml");
        testfile5 = new File("src/test/com/dtolabs/shared/resources/test-resources5.xml");
        invalidfile1 = new File("src/test/com/dtolabs/shared/resources/test-resources-invalid1.xml");
        invalidfile2 = new File("src/test/com/dtolabs/shared/resources/test-resources-invalid2.xml");
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testParse() throws Exception {
        {//test file that does not exist
            try {
                ResourceXMLParser resourceXMLParser= new ResourceXMLParser(true,dnefile1);
                resourceXMLParser.parse();
                fail("Should have thrown an Exception");
            } catch (FileNotFoundException e) {
                assertNotNull(e);
            }
        }
        { //test basic file without receiver
            ResourceXMLParser resourceXMLParser= new ResourceXMLParser(true,testfile1);
            resourceXMLParser.parse();
        }
        { //test basic file with receiver
            ResourceXMLParser resourceXMLParser= new ResourceXMLParser(true,testfile1);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver(){
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] =true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] =true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 4, items.size());
        }
        { //test basic file with receiver, return false from resourceParsed
            ResourceXMLParser resourceXMLParser = new ResourceXMLParser(true, testfile1);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver() {
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] = true;
                    return false;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] = true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
        }
        { //test basic file with receiver, selective xpath="node"
            final String XPATH = "node";
            ResourceXMLParser resourceXMLParser= new ResourceXMLParser(true,testfile1);
            resourceXMLParser.setEntityXpath(XPATH);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver(){
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] =true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] =true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
            final ResourceXMLParser.Entity entity = items.get(0);
            assertEquals("wrong node type", "node", entity.getResourceType());
            assertEquals("wrong name", "node1", entity.getName());
            assertEquals("wrong type", "Node1", entity.getType());
            assertEquals("wrong properties size", 0, entity.getProperties().size());
            assertEquals("wrong resources size", 0, entity.getResources().size());
            assertEquals("wrong referrers size", 0, entity.getReferrers().size());
            assertNull("wrong transforms ", entity.getTransforms());
        }
        { //test basic file with receiver, selective xpath="setting"
            final String XPATH = "setting";
            ResourceXMLParser resourceXMLParser= new ResourceXMLParser(true,testfile1);
            resourceXMLParser.setEntityXpath(XPATH);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver(){
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] =true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] =true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
            final ResourceXMLParser.Entity entity = items.get(0);
            assertEquals("wrong node type", "setting", entity.getResourceType());
            assertEquals("wrong name", "setting1", entity.getName());
            assertEquals("wrong type", "Setting1", entity.getType());
            assertEquals("wrong properties size", 0, entity.getProperties().size());
            assertEquals("wrong resources size", 0, entity.getResources().size());
            assertEquals("wrong referrers size", 0, entity.getReferrers().size());
            assertNull("wrong transforms ", entity.getTransforms());
        }
        { //test basic file with receiver, selective xpath="package"
            final String XPATH = "package";
            ResourceXMLParser resourceXMLParser= new ResourceXMLParser(true,testfile1);
            resourceXMLParser.setEntityXpath(XPATH);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver(){
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] =true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] =true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
            final ResourceXMLParser.Entity entity = items.get(0);
            assertEquals("wrong node type", "package", entity.getResourceType());
            assertEquals("wrong name", "package1", entity.getName());
            assertEquals("wrong type", "Package1", entity.getType());
            assertEquals("wrong properties size", 0, entity.getProperties().size());
            assertEquals("wrong resources size", 0, entity.getResources().size());
            assertEquals("wrong referrers size", 0, entity.getReferrers().size());
            assertNull("wrong transforms ", entity.getTransforms());
        }
        { //test basic file with receiver, selective xpath="deployment"
            final String XPATH = "deployment";
            ResourceXMLParser resourceXMLParser= new ResourceXMLParser(true,testfile1);
            resourceXMLParser.setEntityXpath(XPATH);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver(){
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] =true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] =true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
            final ResourceXMLParser.Entity entity = items.get(0);
            assertEquals("wrong node type", "deployment", entity.getResourceType());
            assertEquals("wrong name", "deployment1", entity.getName());
            assertEquals("wrong type", "Deployment1", entity.getType());
            assertEquals("wrong properties size", 0, entity.getProperties().size());
            assertEquals("wrong resources size", 0, entity.getResources().size());
            assertEquals("wrong referrers size", 0, entity.getReferrers().size());
            assertNull("wrong transforms ", entity.getTransforms());
        }

        /**************
         *  test attributes
         ***************/

        { //test attributes of xpath="node"
            final String XPATH = "node";
            ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, testfile2);
            resourceXMLParser.setEntityXpath(XPATH);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver() {
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] = true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] = true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
            final ResourceXMLParser.Entity entity = items.get(0);
            assertEquals("wrong node type", "node", entity.getResourceType());
            assertEquals("wrong name", "node1", entity.getName());
            assertEquals("wrong type", "Node1", entity.getType());
            assertEquals("wrong properties size", 8, entity.getProperties().size());
            assertEquals("wrong resources size", 0, entity.getResources().size());
            assertEquals("wrong referrers size", 0, entity.getReferrers().size());
            assertNull("wrong transforms ", entity.getTransforms());
            assertEquals("wrong value", "description1", entity.getProperty(ResourceXMLConstants.COMMON_DESCRIPTION));
            assertEquals("wrong value", "tag1,tag2", entity.getProperty(ResourceXMLConstants.COMMON_TAGS));
            assertEquals("wrong value", "hostname1", entity.getProperty(ResourceXMLConstants.NODE_HOSTNAME));
            assertEquals("wrong value", "username1", entity.getProperty(ResourceXMLConstants.NODE_USERNAME));
            assertEquals("wrong value", "osArch1", entity.getProperty(ResourceXMLConstants.NODE_OS_ARCH));
            assertEquals("wrong value", "osFamily1", entity.getProperty(ResourceXMLConstants.NODE_OS_FAMILY));
            assertEquals("wrong value", "osName1", entity.getProperty(ResourceXMLConstants.NODE_OS_NAME));
            assertEquals("wrong value", "osVersion1", entity.getProperty(ResourceXMLConstants.NODE_OS_VERSION));
        }

        { //test attributes of xpath="setting"
            final String XPATH = "setting";
            ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, testfile2);
            resourceXMLParser.setEntityXpath(XPATH);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver() {
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] = true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] = true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
            final ResourceXMLParser.Entity entity = items.get(0);
            assertEquals("wrong node type", "setting", entity.getResourceType());
            assertEquals("wrong name", "setting1", entity.getName());
            assertEquals("wrong type", "Setting1", entity.getType());
            assertEquals("wrong properties size", 4, entity.getProperties().size());
            assertEquals("wrong resources size", 0, entity.getResources().size());
            assertEquals("wrong referrers size", 0, entity.getReferrers().size());
            assertNull("wrong transforms ", entity.getTransforms());
            assertEquals("wrong value", "description2", entity.getProperty(ResourceXMLConstants.COMMON_DESCRIPTION));
            assertEquals("wrong value", "tag1,tag3", entity.getProperty(ResourceXMLConstants.COMMON_TAGS));
            assertEquals("wrong value", "settingType1", entity.getProperty(ResourceXMLConstants.SETTING_TYPE));
            assertEquals("wrong value", "settingValue1", entity.getProperty(ResourceXMLConstants.SETTING_VALUE));
        }
        { //test attributes of xpath="package"
            final String XPATH = "package";
            ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, testfile2);
            resourceXMLParser.setEntityXpath(XPATH);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver() {
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] = true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] = true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
            final ResourceXMLParser.Entity entity = items.get(0);
            assertEquals("wrong node type", "package", entity.getResourceType());
            assertEquals("wrong name", "package1", entity.getName());
            assertEquals("wrong type", "Package1", entity.getType());
            assertEquals("wrong properties size", 15, entity.getProperties().size());
            assertEquals("wrong resources size", 0, entity.getResources().size());
            assertEquals("wrong referrers size", 0, entity.getReferrers().size());
            assertNull("wrong transforms ", entity.getTransforms());
            assertEquals("wrong value", "description3", entity.getProperty(ResourceXMLConstants.COMMON_DESCRIPTION));
            assertEquals("wrong value", "tag3,tag4", entity.getProperty(ResourceXMLConstants.COMMON_TAGS));
            assertEquals("wrong value", "arch1", entity.getProperty(ResourceXMLConstants.PKG_ARCH));
            assertEquals("wrong value", "base1", entity.getProperty(ResourceXMLConstants.PKG_BASE));
            assertEquals("wrong value", "buildtime1", entity.getProperty(ResourceXMLConstants.PKG_BUILDTIME));
            assertEquals("wrong value", "filename1", entity.getProperty(ResourceXMLConstants.PKG_FILENAME));
            assertEquals("wrong value", "filetype1", entity.getProperty(ResourceXMLConstants.PKG_FILETYPE));
            assertEquals("wrong value", "installrank1", entity.getProperty(ResourceXMLConstants.PKG_INSTALLRANK));
            assertEquals("wrong value", "installroot1", entity.getProperty(ResourceXMLConstants.PKG_INSTALLROOT));
            assertEquals("wrong value", "release1", entity.getProperty(ResourceXMLConstants.PKG_RELEASE));
            assertEquals("wrong value", "releasetag1", entity.getProperty(ResourceXMLConstants.PKG_RELEASETAG));
            assertEquals("wrong value", "repoUrl1", entity.getProperty(ResourceXMLConstants.PKG_REPO_URL));
            assertEquals("wrong value", "restart1", entity.getProperty(ResourceXMLConstants.PKG_RESTART));
            assertEquals("wrong value", "vendor1", entity.getProperty(ResourceXMLConstants.PKG_VENDOR));
            assertEquals("wrong value", "version1", entity.getProperty(ResourceXMLConstants.PKG_VERSION));
        }
        { //test attributes of xpath="deployment"
            final String XPATH = "deployment";
            ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, testfile2);
            resourceXMLParser.setEntityXpath(XPATH);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver() {
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] = true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] = true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
            final ResourceXMLParser.Entity entity = items.get(0);
            assertEquals("wrong node type", "deployment", entity.getResourceType());
            assertEquals("wrong name", "deployment1", entity.getName());
            assertEquals("wrong type", "Deployment1", entity.getType());
            assertEquals("wrong properties size", 5, entity.getProperties().size());
            assertEquals("wrong resources size", 0, entity.getResources().size());
            assertEquals("wrong referrers size", 0, entity.getReferrers().size());
            assertNull("wrong transforms ", entity.getTransforms());
            assertEquals("wrong value", "description4", entity.getProperty(ResourceXMLConstants.COMMON_DESCRIPTION));
            assertEquals("wrong value", "tag5,tag6", entity.getProperty(ResourceXMLConstants.COMMON_TAGS));
            assertEquals("wrong value", "basedir1", entity.getProperty(ResourceXMLConstants.DEPLOYMENT_BASEDIR));
            assertEquals("wrong value", "installRoot1", entity.getProperty(ResourceXMLConstants.DEPLOYMENT_INSTALL_ROOT));
            assertEquals("wrong value", "startuprank1", entity.getProperty(ResourceXMLConstants.DEPLOYMENT_STARTUPRANK));
        }
        /************
         * test resources/referrers
         ************/

        { //test parsing of resource/referrers
            final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, testfile3);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver() {
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] = true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] = true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 4, items.size());
            final ResourceXMLParser.Entity nodeEntity = items.get(0);
            assertEquals("wrong resource type", "node", nodeEntity.getResourceType());
            assertEquals("wrong resources count", 1, nodeEntity.getResources().size());
            assertEquals("wrong resources.replace", "true", nodeEntity.getProperty(
                ResourceXMLConstants.RESOURCES_REPLACE_PROP));

            final ResourceXMLParser.Entity dep1 = nodeEntity.getResources().iterator().next();
            assertNotNull(dep1);
            assertEquals("wrong resource type", "deployment", dep1.getResourceType());
            assertEquals("wrong  name", "deployment1", dep1.getName());
            assertEquals("wrong  type", "Deployment1", dep1.getType());

            final ResourceXMLParser.Entity settingEntity = items.get(1);
            assertEquals("wrong resource type", "setting", settingEntity.getResourceType());
            assertEquals("wrong resources count", 0, settingEntity.getResources().size());
            assertEquals("wrong referrers count", 1, settingEntity.getReferrers().size());
            assertNull("wrong referrers.replace", settingEntity.getProperty(ResourceXMLConstants.REFERRERS_REPLACE_PROP));
            final ResourceXMLParser.Entity testDep1 = settingEntity.getReferrers().iterator().next();
            assertEquals("wrong entity", dep1, testDep1);

            final ResourceXMLParser.Entity packageEntity = items.get(2);
            assertEquals("wrong resource type", "package", packageEntity.getResourceType());
            assertEquals("wrong resources count", 0, packageEntity.getResources().size());
            assertEquals("wrong referrers count", 1, packageEntity.getReferrers().size());
            assertEquals("wrong referrers.replace", "true", packageEntity.getProperty(ResourceXMLConstants.REFERRERS_REPLACE_PROP));
            final ResourceXMLParser.Entity testDep2 = packageEntity.getReferrers().iterator().next();
            assertEquals("wrong entity", dep1, testDep2);

            final ResourceXMLParser.Entity deploymentEntity = items.get(3);
            assertEquals("wrong resource type", "deployment", deploymentEntity.getResourceType());
            assertEquals("wrong resources count", 3, deploymentEntity.getResources().size());
            assertEquals("wrong referrers count", 1, deploymentEntity.getReferrers().size());
            assertEquals("wrong resources.replace", "false", deploymentEntity.getProperty(
                ResourceXMLConstants.RESOURCES_REPLACE_PROP));
            final HashSet<ResourceXMLParser.Entity> resources = deploymentEntity.getResources();
            boolean seenpackage=false;
            boolean seenreference=false;
            boolean seensetting=false;
            for (final ResourceXMLParser.Entity resource : resources) {
                if(null==resource.getResourceType() && !seenreference){
                    assertEquals("wrong resource ref name", "apackage2", resource.getName());
                    assertEquals("wrong resource ref type", "Package2", resource.getType());
                    seenreference=true;
                }else if(ResourceXMLConstants.PACKAGE_ENTITY_TAG.equals(resource.getResourceType()) && !seenpackage){
                    assertEquals("wrong resource  name", "package1", resource.getName());
                    assertEquals("wrong resource  type", "Package1", resource.getType());
                    seenpackage=true;
                }else if(ResourceXMLConstants.SETTING_ENTITY_TAG.equals(resource.getResourceType()) && !seensetting){
                    assertEquals("wrong resource  name", "setting1", resource.getName());
                    assertEquals("wrong resource  type", "Setting1", resource.getType());
                    seensetting=true;
                }else {
                    fail("unexpected resource: " + resource.getResourceType());
                }
            }
            assertTrue("expected resource reference", seenreference);
            assertTrue("expected package resource", seenpackage);
            assertTrue("expected setting resource", seensetting);

        }
        /***************
         * Test transforms
         ******************/

        { //test parsing of transforms
            final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, testfile4);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver() {
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] = true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] = true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 2, items.size());
            final ResourceXMLParser.Entity nodeEntity = items.get(0);
            assertEquals("wrong resource type", "deployment", nodeEntity.getResourceType());
            assertEquals("wrong resources count", 0, nodeEntity.getResources().size());
            assertEquals("wrong referrers count", 0, nodeEntity.getReferrers().size());
            assertNotNull("missing transforms", nodeEntity.getTransforms());
            assertEquals("wrong transforms count", 1, nodeEntity.getTransforms().size());
            Properties xform1 = nodeEntity.getTransforms().get(0);
            assertEquals("wrong value", "name1.file", xform1.getProperty(ResourceXMLConstants.TRANFORM_NAME));
            assertEquals("wrong value", "xformdesc1", xform1.getProperty(ResourceXMLConstants.TRANFORM_DESCRIPTION));
            assertEquals("wrong value", "direction1", xform1.getProperty(ResourceXMLConstants.TRANFORM_DIRECTION));
            assertEquals("wrong value", "filetype1", xform1.getProperty(ResourceXMLConstants.TRANFORM_FILETYPE));
            assertEquals("wrong value", "outputdir1", xform1.getProperty(ResourceXMLConstants.TRANFORM_OUTPUTDIR));
            assertEquals("wrong value", "proximity1", xform1.getProperty(ResourceXMLConstants.TRANFORM_PROXIMITY));
            assertEquals("wrong value", "template1", xform1.getProperty(ResourceXMLConstants.TRANFORM_TEMPLATE));
            assertEquals("wrong value", "templatedir1", xform1.getProperty(ResourceXMLConstants.TRANFORM_TEMPLATEDIR));
            assertEquals("wrong value", "templatetype1", xform1.getProperty(ResourceXMLConstants.TRANFORM_TEMPLATETYPE));


            final ResourceXMLParser.Entity deploymentEntity2 = items.get(1);
            assertEquals("wrong resource type", "deployment", deploymentEntity2.getResourceType());
            assertEquals("wrong resources count", 0, deploymentEntity2.getResources().size());
            assertEquals("wrong referrers count", 0, deploymentEntity2.getReferrers().size());
            assertNotNull("missing transforms", deploymentEntity2.getTransforms());
            assertEquals("wrong transforms count",2, deploymentEntity2.getTransforms().size());
            Properties xform2 = deploymentEntity2.getTransforms().get(0);
            assertEquals("wrong value", "name2.file", xform2.getProperty(ResourceXMLConstants.TRANFORM_NAME));
            assertEquals("wrong value", "xformdesc2", xform2.getProperty(ResourceXMLConstants.TRANFORM_DESCRIPTION));
            assertEquals("wrong value", "direction2", xform2.getProperty(ResourceXMLConstants.TRANFORM_DIRECTION));
            assertEquals("wrong value", "filetype2", xform2.getProperty(ResourceXMLConstants.TRANFORM_FILETYPE));
            assertEquals("wrong value", "outputdir2", xform2.getProperty(ResourceXMLConstants.TRANFORM_OUTPUTDIR));
            assertEquals("wrong value", "proximity2", xform2.getProperty(ResourceXMLConstants.TRANFORM_PROXIMITY));
            assertEquals("wrong value", "template2", xform2.getProperty(ResourceXMLConstants.TRANFORM_TEMPLATE));
            assertEquals("wrong value", "templatedir2", xform2.getProperty(ResourceXMLConstants.TRANFORM_TEMPLATEDIR));
            assertEquals("wrong value", "templatetype2", xform2.getProperty(ResourceXMLConstants.TRANFORM_TEMPLATETYPE));
            Properties xform3 = deploymentEntity2.getTransforms().get(1);
            assertEquals("wrong value", "name3.file", xform3.getProperty(ResourceXMLConstants.TRANFORM_NAME));
            assertEquals("wrong value", "xformdesc3", xform3.getProperty(ResourceXMLConstants.TRANFORM_DESCRIPTION));
            assertEquals("wrong value", "direction3", xform3.getProperty(ResourceXMLConstants.TRANFORM_DIRECTION));
            assertEquals("wrong value", "filetype3", xform3.getProperty(ResourceXMLConstants.TRANFORM_FILETYPE));
            assertEquals("wrong value", "outputdir3", xform3.getProperty(ResourceXMLConstants.TRANFORM_OUTPUTDIR));
            assertEquals("wrong value", "proximity3", xform3.getProperty(ResourceXMLConstants.TRANFORM_PROXIMITY));
            assertEquals("wrong value", "template3", xform3.getProperty(ResourceXMLConstants.TRANFORM_TEMPLATE));
            assertEquals("wrong value", "templatedir3", xform3.getProperty(ResourceXMLConstants.TRANFORM_TEMPLATEDIR));
            assertEquals("wrong value", "templatetype3", xform3.getProperty(ResourceXMLConstants.TRANFORM_TEMPLATETYPE));


        }
        /*************
         * Test validation
         *************/
        { //testfile4 has no doctype declaration
            //do not validate, should parse fine
            final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, testfile4);
            resourceXMLParser.parse();
        }
        { //testfile4 has no doctype declaration
            //do validate, should fail with no doctype
            final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(true, testfile4);
            try {
                resourceXMLParser.parse();
                fail("Should not succeed");
            } catch (ResourceXMLParserException e) {
                assertNotNull(e);
            }
        }
        {//testfile5 has valid doctype declaration
            //do not validate, should parse fine
            final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, testfile5);
            resourceXMLParser.parse();
        }
        {//testfile5 has valid doctype declaration
            //do validate, should parse fine
            final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(true, testfile5);
            resourceXMLParser.parse();
        }
        {//invalidfile1 has incorrect root element
            //do not validate, will not fail with extra XML content
            final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, invalidfile1);
            resourceXMLParser.parse();
        }
        {//invalidfile1 has incorrect root element
            //do validate, should fail validation
            final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(true, invalidfile1);
            try {
                resourceXMLParser.parse();
                fail("should not succeed validation");
            } catch (ResourceXMLParserException e) {
                assertNotNull(e);
            }
        }
        {//invalidfile12 has incorrect child element
            //do not validate, will not fail with extra XML content
            final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, invalidfile2);
            resourceXMLParser.parse();
        }
        {//invalidfile2 has incorrect child element
            //do validate, should fail validation
            final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(true, invalidfile2);
            try {
                resourceXMLParser.parse();
                fail("should not succeed validation");
            } catch (ResourceXMLParserException e) {
                assertNotNull(e);
            }
        }
    }
}
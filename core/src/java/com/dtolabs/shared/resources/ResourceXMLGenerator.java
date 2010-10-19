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
* ResourceXMLGenerator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Oct 7, 2010 11:40:20 AM
* 
*/
package com.dtolabs.shared.resources;

import com.dtolabs.rundeck.core.common.INodeEntry;
import static com.dtolabs.shared.resources.ResourceXMLConstants.*;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * ResourceXMLGenerator can generate a resources.xml file given a set of entities or INodeEntry objects.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ResourceXMLGenerator {
    static Logger log4j = Logger.getLogger(ResourceXMLGenerator.class.getName());

    private File file;
    private OutputStream output;
    private List<ResourceXMLParser.Entity> entities;

    /**
     * Constructor for the ResourceXMLGenerator
     *
     * @param file destination output file
     */
    public ResourceXMLGenerator(final File file) {
        this.file = file;
        this.entities = new ArrayList<ResourceXMLParser.Entity>();
    }

    /**
     * Constructor for the ResourceXMLGenerator
     *
     * @param file destination output file
     */
    public ResourceXMLGenerator(final OutputStream output) {
        this.output = output;
        this.entities = new ArrayList<ResourceXMLParser.Entity>();
    }

    /**
     * Return entities list
     *
     * @return
     */
    public List<ResourceXMLParser.Entity> getEntities() {
        return entities;
    }

    /**
     * Set entities list
     *
     * @param entities
     */
    public void setEntities(final List<ResourceXMLParser.Entity> entities) {
        this.entities = entities;
    }

    /**
     * Add entity object
     *
     * @param entity
     */
    public void addEntity(final ResourceXMLParser.Entity entity) {
        entities.add(entity);
    }

    /**
     * Add Node object
     *
     * @param node
     */
    public void addNode(final INodeEntry node) {
        //convert to entity
        final ResourceXMLParser.Entity entity = createEntity(node);
        addEntity(entity);
        if(null!=entity.getResources()){
            for (final ResourceXMLParser.Entity entity1 : entity.getResources()) {
                addEntity(entity1);
            }
        }
    }

    /**
     * Create entity from Node
     *
     * @param node
     *
     * @return
     */
    private ResourceXMLParser.Entity createEntity(final INodeEntry node) {
        final ResourceXMLParser.Entity ent = new ResourceXMLParser.Entity();
        ent.setName(node.getNodename());
        ent.setType(null != node.getType() ? node.getType() : "Node");
        ent.setResourceType(NODE_ENTITY_TAG);
        ent.setProperty(COMMON_DESCRIPTION, notNull(node.getDescription()));
        if (null != node.getTags() && node.getTags().size() > 0) {
            ent.setProperty(COMMON_TAGS, joinStrings(new TreeSet(node.getTags()), ","));
        }
        ent.setProperty(NODE_HOSTNAME, notNull(node.getHostname()));
        ent.setProperty(NODE_OS_ARCH, notNull(node.getOsArch()));
        ent.setProperty(NODE_OS_FAMILY, notNull(node.getOsFamily()));
        ent.setProperty(NODE_OS_NAME, notNull(node.getOsName()));
        ent.setProperty(NODE_OS_VERSION, notNull(node.getOsVersion()));
        ent.setProperty(NODE_USERNAME, notNull(node.getUsername()));
        if(null!=node.getAttributes() && null!=node.getAttributes().get(NODE_EDIT_URL)) {
            ent.setProperty(NODE_EDIT_URL, notNull(node.getAttributes().get(NODE_EDIT_URL)));
        }
        if(null!=node.getAttributes() && null!=node.getAttributes().get(NODE_REMOTE_URL)) {
            ent.setProperty(NODE_REMOTE_URL, notNull(node.getAttributes().get(NODE_REMOTE_URL)));
        }
        //iterate settings
        if(null!=node.getSettings()){
            for (final String setName : node.getSettings().keySet()) {
                String value = node.getSettings().get(setName);
                final ResourceXMLParser.Entity setent = new ResourceXMLParser.Entity();
                setent.setName(setName);
                setent.setResourceType(SETTING_ENTITY_TAG);
                setent.setProperty(SETTING_VALUE, value);
                setent.setType("Setting");
                ent.addResource(setent);
            }
        }

        return ent;
    }

    /**
     * utility to join tags into string
     *
     * @param tags
     * @param delim
     *
     * @return
     */
    private static String joinStrings(final Set tags, final String delim) {
        final StringBuffer sb = new StringBuffer();
        for (final Object tag : tags) {
            if (sb.length() > 0) {
                sb.append(delim);
            }
            sb.append(tag);
        }

        return sb.toString();
    }

    /**
     * Generate and store the XML file
     *
     * @throws IOException
     */
    public void generate() throws IOException {
        final Document doc = DocumentFactory.getInstance().createDocument().addDocType("project",
            DTD_PROJECT_DOCUMENT_1_0_EN, "project.dtd");
        final Element root = doc.addElement("project");
        //iterate through entities in correct order
        for (final ResourceXMLParser.Entity entity : entities) {
            if (NODE_ENTITY_TAG.equals(entity.getResourceType())) {
                final Element ent = genEntityCommon(root, entity);
                genNode(ent, entity);
            }
        }
        for (final ResourceXMLParser.Entity entity : entities) {
            if (SETTING_ENTITY_TAG.equals(entity.getResourceType())) {
                final Element ent = genEntityCommon(root, entity);
                genSetting(ent, entity);
            }
        }
        for (final ResourceXMLParser.Entity entity : entities) {
            if (PACKAGE_ENTITY_TAG.equals(entity.getResourceType())) {
                final Element ent = genEntityCommon(root, entity);
                genPackage(ent, entity);
            }
        }
        for (final ResourceXMLParser.Entity entity : entities) {
            if (DEPLOYMENT_ENTITY_TAG.equals(entity.getResourceType())) {
                final Element ent = genEntityCommon(root, entity);
                genDeployment(ent, entity);
            }
        }

        if (null != file) {
            serializeDocToStream(new FileOutputStream(file), doc);
        } else if (null != output) {
            serializeDocToStream(output, doc);
        }
    }

    /**
     * Generate resources section and resource references
     *
     * @param ent
     * @param entity
     */
    private void genResources(final Element ent, final ResourceXMLParser.Entity entity) {
        if (null == entity.getResources() || entity.getResources().size() < 1) {
            return;
        }
        final Element resources = ent.addElement(RESOURCES_GROUP_TAG);
        if (Boolean.toString(true).equals(entity.getProperty(RESOURCES_REPLACE_PROP))) {
            resources.addAttribute(COMMON_REPLACE, Boolean.toString(true));
        }
        for (final ResourceXMLParser.Entity entity1 : entity.getResources()) {
            final Element res = resources.addElement(RESOURCE_REF_TAG);
            res.addAttribute(COMMON_NAME, entity1.getName());
            res.addAttribute(COMMON_TYPE, entity1.getType());
        }

    }

    /**
     * Gen "node" tag contents
     *
     * @param ent
     * @param entity
     */
    private void genNode(final Element ent, final ResourceXMLParser.Entity entity) {
        for (final String nodeProp : nodeProps) {
            ent.addAttribute(nodeProp, notNull(entity.getProperty(nodeProp)));
        }
        genResources(ent, entity);
    }


    /**
     * Gen "setting" tag contents
     *
     * @param ent
     * @param entity
     */
    private void genSetting(final Element ent, final ResourceXMLParser.Entity entity) {
        for (final String prop : settingProps) {
            ent.addAttribute(prop, notNull(entity.getProperty(prop)));
        }
    }

    /**
     * Gen "package" tag contents
     *
     * @param ent
     * @param entity
     */
    private void genPackage(final Element ent, final ResourceXMLParser.Entity entity) {
        for (final String prop : packageProps) {
            ent.addAttribute(prop, notNull(entity.getProperty(prop)));
        }
        genResources(ent, entity);
    }

    /**
     * Gen "deployment" tag contents
     *
     * @param ent
     * @param entity
     */
    private void genDeployment(final Element ent, final ResourceXMLParser.Entity entity) {
        for (final String prop : deploymentProps) {
            ent.addAttribute(prop, notNull(entity.getProperty(prop)));
        }
        genResources(ent, entity);
    }

    /**
     * Create entity tag based on resourceType of entity, and add common attributes
     *
     * @param root
     * @param entity
     *
     * @return
     */
    private Element genEntityCommon(final Element root, final ResourceXMLParser.Entity entity) {
        final Element tag = root.addElement(entity.getResourceType());
        tag.addAttribute(COMMON_NAME, entity.getName());
        tag.addAttribute(COMMON_TYPE, entity.getType());
        tag.addAttribute(COMMON_DESCRIPTION, notNull(entity.getProperty(COMMON_DESCRIPTION)));
        if (null != entity.getProperty(COMMON_TAGS)) {
            tag.addAttribute(COMMON_TAGS, notNull(entity.getProperty(COMMON_TAGS)));
        }
        return tag;
    }

    /**
     * Return "" if input is null, otherwise return input
     *
     * @param s input string
     *
     * @return
     */
    private String notNull(final String s) {
        if (null == s) {
            return "";
        }
        return s;
    }

    /**
     * Write Document to a file
     *
     * @param file
     * @param doc
     *
     * @throws IOException
     */
    private static void serializeDocToStream(final OutputStream output, final Document doc) throws IOException {
        final OutputFormat format = OutputFormat.createPrettyPrint();
        final XMLWriter writer = new XMLWriter(output, format);
        writer.write(doc);
        writer.flush();
    }
}

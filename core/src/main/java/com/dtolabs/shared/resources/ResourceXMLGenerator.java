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
import com.dtolabs.rundeck.core.common.NodesFileGenerator;
import static com.dtolabs.shared.resources.ResourceXMLConstants.*;

import com.dtolabs.utils.XMLChar;
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
import java.util.*;

/**
 * ResourceXMLGenerator can generate a resources.xml file given a set of entities or INodeEntry objects.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ResourceXMLGenerator implements NodesFileGenerator {
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
    }

    public void addNodes(final Collection<INodeEntry> iNodeEntries) {
        for (final INodeEntry iNodeEntry : iNodeEntries) {
            addNode(iNodeEntry);
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
        ent.setResourceType(NODE_ENTITY_TAG);
        

        if(null!=node.getAttributes()){
            for (final String setName : node.getAttributes().keySet()) {
                String value = node.getAttributes().get(setName);
                ent.setProperty(setName, value);
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
        final Document doc = DocumentFactory.getInstance().createDocument();
        final Element root = doc.addElement("project");
        //iterate through entities in correct order
        for (final ResourceXMLParser.Entity entity : entities) {
            if (NODE_ENTITY_TAG.equals(entity.getResourceType())) {
                final Element ent = genEntityCommon(root, entity);
                genNode(ent, entity);
            }
        }

        if (null != file) {
            FileOutputStream out=new FileOutputStream(file);
            try{
                serializeDocToStream(out, doc);
            }finally{
                out.close();
            }
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
    private void genAttributes(final Element ent, final ResourceXMLParser.Entity entity) {
        if (null == entity.getProperties() ) {
            return;
        }
        for (final String key:entity.getProperties().stringPropertyNames()){
            if (!ResourceXMLConstants.allPropSet.contains(key)) {
                //test attribute name is a valid XML attribute name
                if(XMLChar.isValidName(key)){
                    ent.addAttribute(key, entity.getProperties().getProperty(key));
                }else {
                    //add sub element
                    final Element atelm = ent.addElement(ATTRIBUTE_TAG);
                    atelm.addAttribute(ATTRIBUTE_NAME_ATTR, key);
                    atelm.addAttribute(ATTRIBUTE_VALUE_ATTR, entity.getProperties().getProperty(key));
                }
            }
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
        genAttributes(ent, entity);
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
        tag.addAttribute(COMMON_DESCRIPTION, notNull(entity.getProperty(COMMON_DESCRIPTION)));
        tag.addAttribute(COMMON_TAGS, notNull(entity.getProperty(COMMON_TAGS)));
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

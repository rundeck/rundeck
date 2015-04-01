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
* ResourceXMLParser.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Apr 23, 2010 3:35:55 PM
* $Id$
*/
package com.dtolabs.shared.resources;

import static com.dtolabs.shared.resources.ResourceXMLConstants.*;

import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * ResourceXMLParser parses a resources.xml formatted file, and provides several interfaces for using the result data.
 * <br>
 * The {@link #parse()} method parses the configured File as a sequence of {@link com.dtolabs.shared.resources.ResourceXMLParser.Entity}
 * objects, one for each entry in the file.  It passes these objects to any configured {@link com.dtolabs.shared.resources.ResourceXMLReceiver}
 * object. One should be set using {@link
 * #setReceiver(ResourceXMLReceiver)} to receive parsed entities or the entire entity set.
 * <br>
 * The default entityXpath property value is set to match all entity types in the resource xml
 * <code>(node|setting|package|deployment)</code>, but this can be set to any Xpath to limit the entities that are
 * parsed from the document. (e.g. "<code>node|package</code>" or "<code>node[@name='mynode']</code>").
 * <br>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ResourceXMLParser {
    static Logger log4j = Logger.getLogger(ResourceXMLParser.class.getName());

    private File file;
    private InputStream input;
    private Document doc;
    private ResourceXMLReceiver receiver;
    public static final String DEFAULT_ENTITY_XPATH = NODE_ENTITY_TAG ;
    private String entityXpath = DEFAULT_ENTITY_XPATH;

    /**
     * Constructor for the ResourceXMLParser
     *
     * @param file     source file
     */
    public ResourceXMLParser(final File file) {
        this.file = file;
    }
    /**
     * Constructor for the ResourceXMLParser
     *
     * @param input     source file
     */
    public ResourceXMLParser(final InputStream input) {
        this.input = input;
    }


    /**
     * Constructor for the ResourceXMLParser
     *
     * @param doc     source document
     */
    public ResourceXMLParser(final Document doc) {
        this.doc = doc;
    }


    /**
     * Parse the document, applying the configured Receiver to the parsed entities
     *
     * @throws ResourceXMLParserException parse error
     * @throws java.io.IOException io error
     */
    public void parse() throws ResourceXMLParserException, IOException {
        final EntityResolver resolver = createEntityResolver();
        final SAXReader reader = new SAXReader(false);
        reader.setEntityResolver(resolver);

        try {

            final Document doc;
            if(null==this.doc){
                final InputStream in;
                if(null!=file){
                    in = new FileInputStream(file);
                }else{
                    in = input;
                }
                try{
                    doc=reader.read(in);
                }finally{
                    if(null!=file){
                        in.close();
                    }
                }
            }else{
                doc=this.doc;
            }

            final EntitySet set = new EntitySet();
            final Element root = doc.getRootElement();

            final List list = root.selectNodes(entityXpath);
            for (final Object n : list) {
                final Node node = (Node) n;
                final Entity ent = parseEnt(node, set);
                if (null != receiver) {
                    if (!receiver.resourceParsed(ent)) {
                        break;
                    }
                }
            }
            if (null != receiver) {
                receiver.resourcesParsed(set);
            }

        } catch (DocumentException e) {
            throw new ResourceXMLParserException(e);
        }
    }

    public static EntityResolver createEntityResolver() {
        return new EntityResolver() {
            public InputSource resolveEntity(final String publicId, final String systemId) {
                if (publicId.equals(DTD_PROJECT_DOCUMENT_1_0_EN)) {
                    final InputStream in = ResourceXMLParser.class.getClassLoader().getResourceAsStream(
                        PROJECT_DTD_RESOURCE_PATH);
                    if (null != in) {
                        return new InputSource(in);
                    } else {
                        System.err.println(
                            "couldn't load resource " + PROJECT_DTD_RESOURCE_PATH + ":" + ResourceXMLParser.class
                                .getClassLoader().getResource(PROJECT_DTD_RESOURCE_PATH));
                        final File file1 = new File("src/java/" + PROJECT_DTD_RESOURCE_PATH);
                        if (file1.exists()) {
                            try {
                                return new InputSource(new FileInputStream(file1));
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
                return null;
            }
        };
    }

    /**
     * Given xml Node and EntitySet, parse the entity defined in the Node
     *
     * @param node DOM node
     * @param set  entity set holder
     *
     * @return parsed Entity object
     *
     * @throws ResourceXMLParserException if entity definition was previously found, or another error occurs
     */
    private Entity parseEnt(final Node node, final EntitySet set) throws ResourceXMLParserException {
        final Entity ent = parseResourceRef(set, node);
        ent.setResourceType(node.getName());
        parseEntProperties(ent, node);
        parseEntSubAttributes(ent, node);
        return ent;
    }


    /**
     * Parse a simple resource/entity node for the type/name attributes, returning a new or existing Entity
     *
     * @param set entity set
     * @param n   entity DOM node
     *
     * @return new or existing Entity
     *
     * @throws ResourceXMLParserException if the ndoe is missing the required attributes
     */
    private Entity parseResourceRef(final EntitySet set, final Node n) throws ResourceXMLParserException {
        final Node node2 = n.selectSingleNode("@" + COMMON_NAME);
        if (null == node2) {
            throw new ResourceXMLParserException("@" + COMMON_NAME + " required: " + reportNodeErrorLocation(n));
        }
        final String rname = node2.getStringValue();
        return set.getOrCreateEntity( rname);
    }

    private static final HashMap<String, String[]> entityProperties = new HashMap<String, String[]>();

    static {
        entityProperties.put(NODE_ENTITY_TAG, nodeProps);
    }

    /**
     * Parse the DOM attributes as properties for the particular entity node type
     *
     * @param ent  Entity object
     * @param node entity DOM node
     *
     * @throws ResourceXMLParserException if the DOM node is an unexpected tag name
     */
    private void parseEntProperties(final Entity ent, final Node node) throws ResourceXMLParserException {
        if (null == entityProperties.get(node.getName())) {
            throw new ResourceXMLParserException(
                "Unexpected entity declaration: " + node.getName() + ": " + reportNodeErrorLocation(node));
        }
        final Element node1 = (Element) node;
        //load all element attributes as properties
        for (final Object o : node1.attributes()) {
            final Attribute attr = (Attribute) o;
            ent.properties.setProperty(attr.getName(), attr.getStringValue());
        }
    }

    /**
     * Parse the DOM attributes as properties for the particular entity node type
     *
     * @param ent  Entity object
     * @param node entity DOM node
     *
     * @throws ResourceXMLParserException if the DOM node is an unexpected tag name
     */
    private void parseEntSubAttributes(final Entity ent, final Node node) throws ResourceXMLParserException {
        final Element node1 = (Element) node;
        //load all sub elements called "attribute" as properties
        for (final Object attribute : node1.selectNodes(ATTRIBUTE_TAG)) {
            Element attr=(Element) attribute;
            if(null==attr.selectSingleNode("@" + ATTRIBUTE_NAME_ATTR)) {
                throw new ResourceXMLParserException(
                    ATTRIBUTE_TAG + " element has no '" + ATTRIBUTE_NAME_ATTR + "' attribute: "
                    + reportNodeErrorLocation(attr));
            }
            String attrname = attr.selectSingleNode("@" + ATTRIBUTE_NAME_ATTR).getStringValue();

            String attrvalue;
            //look for "value" attribute
            if(null!=attr.selectSingleNode("@"+ATTRIBUTE_VALUE_ATTR)) {
                attrvalue = attr.selectSingleNode("@" + ATTRIBUTE_VALUE_ATTR).getStringValue();
            }else if(null!= attr.getText()) {
                //look for text content
                attrvalue = attr.getText();
            }else {
                throw new ResourceXMLParserException(
                    ATTRIBUTE_TAG + " element has no '" + ATTRIBUTE_VALUE_ATTR + "' attribute or text content: "
                    + reportNodeErrorLocation(attr));
            }

            ent.properties.setProperty(attrname, attrvalue);
        }
    }

    /**
     * Return a String describing the DOM node's location and parent type name
     *
     * @param e the node
     *
     * @return string describing xpath location and parent "type" element name
     */
    protected static String reportNodeErrorLocation(final Node e) {
        return "at xpath " + e.getUniquePath();
    }


    /**
     * Return the ResourceXMLReceiver
     *
     * @return the ResourceXMLReceiver
     */
    public ResourceXMLReceiver getReceiver() {
        return receiver;
    }

    /**
     * Set the ResourceXMLReceiver to use.  It will be invoked to receive the {@link
     * com.dtolabs.shared.resources.ResourceXMLParser.Entity} objects created during the {@link #parse()} method,
     * and will also receive the complete {@link com.dtolabs.shared.resources.ResourceXMLParser.EntitySet} at the
     * end of the sequence.  It can govern whether parsing should continue or not, see {@link
     * com.dtolabs.shared.resources.ResourceXMLReceiver}
     *
     * @param receiver the new ResourceXMLReceiver
     */
    public void setReceiver(final ResourceXMLReceiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Contains the set of parsed entities from the document.
     */
    public static class EntitySet {

        private HashMap<String, Entity> entcache = new HashMap<String, Entity>();

        void addEntity(final Entity ent) {
            entcache.put(ent.getId(), ent);
        }

        boolean containsEntity(final String name) {
            return entcache.containsKey(name);
        }

        Entity createEntity( final String name) {
            final Entity ent = new Entity();
            ent.setName(name);
            ent.set = this;
            addEntity(ent);
            return ent;
        }

        Entity getOrCreateEntity(final String name) {
            if (containsEntity(name)) {
                return entcache.get(name);
            }
            return createEntity(name);
        }

        /**
         * Return the collection of entities.
         *
         * @return the entities.
         */
        public Collection<Entity> getEntities() {
            return entcache.values();
        }
    }

    /**
     * Represents a parsed resource entity in the xml, which consists of a name property, a type property, and a set of
     * name/value properties.  These property names correspond to the attribute names of the type of entity being
     * parsed.  See {@link com.dtolabs.shared.resources.ResourceXMLConstants} for property names.
     * <br>
     * The specific entity declaration type (node,setting,package,deployment) can be found with the {@link
     * #getResourceType()} method. This method will return null if the entity is a resource-reference with no
     * corresponding entity definition in the XML.
     * <br>
     * Two special properties, "resources.replace" and "referrers.replace" correspond to the values of the "replace"
     * attribute on any embedded resource/referrer references for the entity.
     */
    public static class Entity {
        private EntitySet set;


        Entity() {
            this.properties = new Properties();
        }

        private Properties properties;

        private String name;
        private String resourceType;

        public String getId() {
            return name;
        }


        public String getName() {
            return name;
        }

        void setName(final String name) {
            this.name = name;
        }


        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Entity entity = (Entity) o;

            if (name != null ? !name.equals(entity.name) : entity.name != null) {
                return false;
            }
            return true;

        }

        @Override
        public int hashCode() {
            int result = set != null ? set.hashCode() : 0;
            result = 31 * result + (properties != null ? properties.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        public Properties getProperties() {
            return properties;
        }

        public String getProperty(final String prop) {
            return properties.getProperty(prop);
        }
        public void setProperty(final String prop, final String value) {
            properties.setProperty(prop, value);
        }

        /**
         * Return the name of the resource xml "tag" defining this entity, or null if it is a resource reference
         *
         * @return the name of the resource xml "tag" defining this entity, or null if it is a resource reference
         */
        public String getResourceType() {
            return resourceType;
        }

        void setResourceType(final String resourceType) {
            this.resourceType = resourceType;
        }
    }
}

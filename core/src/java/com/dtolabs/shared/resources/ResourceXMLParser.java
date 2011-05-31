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
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

/**
 * ResourceXMLParser parses a resources.xml formatted file, and provides several interfaces for using the result data.
 * <p/>
 * The {@link #parse()} method parses the configured File as a sequence of {@link com.dtolabs.shared.resources.ResourceXMLParser.Entity}
 * objects, one for each entry in the file.  It passes these objects to any configured {@link com.dtolabs.shared.resources.ResourceXMLReceiver}
 * object. One should be set using {@link
 * #setReceiver(ResourceXMLReceiver)} to receive parsed entities or the entire entity set.
 * <p/>
 * The default entityXpath property value is set to match all entity types in the resource xml
 * <code>(node|setting|package|deployment)</code>, but this can be set to any Xpath to limit the entities that are
 * parsed from the document. (e.g. "<code>node|package</code>" or "<code>node[@name='mynode']</code>").
 * See {@link #setEntityXpath(String)}.
 * <p/>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ResourceXMLParser {
    static Logger log4j = Logger.getLogger(ResourceXMLParser.class.getName());

    private boolean validate;
    private File file;
    private ResourceXMLReceiver receiver;
    public static final String DEFAULT_ENTITY_XPATH =
        NODE_ENTITY_TAG + "|" + SETTING_ENTITY_TAG + "|" + PACKAGE_ENTITY_TAG + "|" + DEPLOYMENT_ENTITY_TAG;
    private String entityXpath = DEFAULT_ENTITY_XPATH;

    /**
     * Constructor for the ResourceXMLParser
     *
     * @param validate if true, perform DTD validation. If false, do not.
     * @param file     source file
     */
    public ResourceXMLParser(final boolean validate, final File file) {
        this.validate = validate;
        this.file = file;
    }


    /**
     * Parse the document, applying the configured Receiver to the parsed entities
     *
     * @throws ResourceXMLParserException
     * @throws FileNotFoundException
     */
    public void parse() throws ResourceXMLParserException, FileNotFoundException {
        final EntityResolver resolver = createEntityResolver();
        final SAXReader reader = new SAXReader(false);
        reader.setEntityResolver(resolver);

        try {

            final Document doc = reader.read(new FileInputStream(file));
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
        if (ent.wasParsed) {
            log4j.warn("Parsed duplicate resource definition (" + ent.getId() + "): " + reportNodeErrorLocation(node)
                    +". file: "+file.getAbsolutePath());
        }
        ent.setResourceType(node.getName());
        parseEntProperties(ent, node);
        ent.wasParsed = true;
        parseEntResources(ent, set, node);
        if (DEPLOYMENT_ENTITY_TAG.equals(node.getName())) {
            parseTransforms(ent, node);
        }
        return ent;
    }

    /**
     * Parse the transforms content for a deployment entity
     *
     * @param ent  the Entity object
     * @param node the entity DOM node
     *
     * @throws ResourceXMLParserException if an error occurs
     */
    private void parseTransforms(final Entity ent, final Node node) throws ResourceXMLParserException {
        final Node transforms = node.selectSingleNode(TRANSFORMS_GROUP_TAG);
        if (null != transforms) {
            final Node replnode = transforms.selectSingleNode("@" + COMMON_REPLACE);
            if (null != replnode) {
                ent.properties.put("transforms.replace", Boolean.valueOf(replnode.getStringValue()));
            }
            for (final Object o : transforms.selectNodes(TRANSFORM_TAG)) {
                final Node n = (Node) o;
                final Properties props = new Properties();
                parseProperties(props, n, TRANSFORM_ATTRIBUTES);
                ent.addTransform(props);

            }
        }
    }

    /**
     * Go through the resources/referrers elements, parsing references and adding them to the entity
     *
     * @param ent  current entity
     * @param set  entity set
     * @param node current entity DOM node
     *
     * @throws ResourceXMLParserException if an error occurs
     */
    private void parseEntResources(final Entity ent, final EntitySet set, final Node node) throws
        ResourceXMLParserException {
        final Node resourcesnode = node.selectSingleNode(RESOURCES_GROUP_TAG);
        if (null != resourcesnode && !SETTING_ENTITY_TAG.equals(node.getName())) {
            final Node replnode = resourcesnode.selectSingleNode("@" + COMMON_REPLACE);
            if (null != replnode) {
                ent.properties.put(RESOURCES_REPLACE_PROP, replnode.getStringValue());
            }
            for (final Object o : resourcesnode.selectNodes(RESOURCE_REF_TAG)) {
                final Node n = (Node) o;

                final Entity rent = parseResourceRef(set, n);
                ent.addResource(rent);
                rent.addReferrer(ent);

            }
        }
        final Node refsnode = node.selectSingleNode(REFERRERS_GROUP_TAG);
        if (null != refsnode) {
            final Node replnode = refsnode.selectSingleNode("@" + COMMON_REPLACE);
            if (null != replnode) {
                ent.properties.put(REFERRERS_REPLACE_PROP, replnode.getStringValue());
            }
            for (final Object o : refsnode.selectNodes(RESOURCE_REF_TAG)) {
                final Node n = (Node) o;

                final Entity rent = parseResourceRef(set, n);
                ent.addReferrer(rent);
                rent.addResource(ent);
            }
        }
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
        final Node node1 = n.selectSingleNode("@" + COMMON_TYPE);
        final Node node2 = n.selectSingleNode("@" + COMMON_NAME);
        if (null == node1) {
            throw new ResourceXMLParserException("@" + COMMON_TYPE + " required: " + reportNodeErrorLocation(n));
        }
        if (null == node2) {
            throw new ResourceXMLParserException("@" + COMMON_NAME + " required: " + reportNodeErrorLocation(n));
        }
        final String rtype = node1.getStringValue();
        final String rname = node2.getStringValue();
        return set.getOrCreateEntity(rtype, rname);
    }

    private static final HashMap<String, String[]> entityProperties = new HashMap<String, String[]>();

    static {
        entityProperties.put(NODE_ENTITY_TAG, nodeProps);
        entityProperties.put(SETTING_ENTITY_TAG, settingProps);
        entityProperties.put(PACKAGE_ENTITY_TAG, packageProps);
        entityProperties.put(DEPLOYMENT_ENTITY_TAG, deploymentProps);
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
        parseProperties(ent.properties, node, commonProps);
        parseProperties(ent.properties, node, entityProperties.get(node.getName()));

    }

    /**
     * Parse attributes of a Node into a Properties object, using a set of predetermined keys.
     *
     * @param properties the Properties object to write to
     * @param node       a node
     * @param props      the set of attribute names to parse into properties
     */
    private void parseProperties(final Properties properties, final Node node, final String[] props) {
        for (final String prop : props) {
            final Node node1 = node.selectSingleNode("@" + prop);
            if (null != node1) {
                properties.setProperty(prop, node1.getStringValue());
            }
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
     * Return the entityXpath property
     *
     * @return entityXpath property
     */
    public String getEntityXpath() {
        return entityXpath;
    }

    /**
     * Set the entityXpath property, the XPATH string used for selecting entity definitions in the target document
     * during the {@link #parse()} method.  The default value is {@link #DEFAULT_ENTITY_XPATH} and selects all entity
     * definitions.
     *
     * @param entityXpath new XPATH string
     */
    public void setEntityXpath(final String entityXpath) {
        this.entityXpath = entityXpath;
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

        boolean containsEntity(final String type, final String name) {
            return entcache.containsKey(Entity.entityId(type, name));
        }

        Entity createEntity(final String type, final String name) {
            final Entity ent = new Entity();
            ent.setType(type);
            ent.setName(name);
            ent.set = this;
            addEntity(ent);
            return ent;
        }

        Entity getOrCreateEntity(final String type, final String name) {
            if (containsEntity(type, name)) {
                return entcache.get(Entity.entityId(type, name));
            }
            return createEntity(type, name);
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
     * <p/>
     * Entities also may have other entity "referrers" ({@link #getReferrers()}), and most entities may have "resources"
     * ({@link #getResources()}).
     * <p/>
     * The specific entity declaration type (node,setting,package,deployment) can be found with the {@link
     * #getResourceType()} method. This method will return null if the entity is a resource-reference with no
     * corresponding entity definition in the XML.
     * <p/>
     * Two special properties, "resources.replace" and "referrers.replace" correspond to the values of the "replace"
     * attribute on any embedded resource/referrer references for the entity.
     */
    public static class Entity {
        private EntitySet set;
        private boolean wasParsed = false;

        private HashSet<Entity> resources;
        private HashSet<Entity> referrers;
        private ArrayList<Properties> transforms;

        Entity() {
            this.properties = new Properties();
            this.resources = new HashSet<Entity>();
            this.referrers = new HashSet<Entity>();
        }

        private Properties properties;

        private String name;
        private String type;
        private String resourceType;

        public String getId() {
            return entityId(type, name);
        }

        static String entityId(final String type, final String name) {
            return type + ":" + name;
        }

        void addResource(final Entity ent) {
            resources.add(ent);
        }

        void addReferrer(final Entity ent) {
            referrers.add(ent);
        }

        public String getName() {
            return name;
        }

        void setName(final String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        void setType(final String type) {
            this.type = type;
        }

        /**
         * Return resources
         *
         * @return
         */
        public HashSet<Entity> getResources() {
            return resources;
        }

        /**
         * Return referrers
         *
         * @return
         */
        public HashSet<Entity> getReferrers() {
            return referrers;
        }

        /**
         * Return transforms
         *
         * @return
         */
        public ArrayList<Properties> getTransforms() {
            return transforms;
        }

        void addTransform(final Properties props) {
            if (null == transforms) {
                this.transforms = new ArrayList<Properties>();
            }
            transforms.add(props);
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
            return !(type != null ? !type.equals(entity.type) : entity.type != null);

        }

        @Override
        public int hashCode() {
            int result = set != null ? set.hashCode() : 0;
            result = 31 * result + (properties != null ? properties.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
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

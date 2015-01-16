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
* NodeEntryImpl.java
* 
* User: greg
* Created: Mar 31, 2008 4:58:29 PM
* $Id$
*/
package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.utils.StringArrayUtil;

import java.util.*;


/**
 * NodeEntryImpl provides a bean representation of INodesEntry
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodeEntryImpl extends NodeBaseImpl implements INodeEntry, INodeDesc {

    protected static final String USER_AT_HOSTNAME_REGEX = "([^@])+@([^@:])+";
    protected static final String PORT_REGEX = "([^:]+):([0-9]+)";
    public static final String OS_NAME = "osName";
    public static final String OS_FAMILY = "osFamily";
    public static final String OS_VERSION = "osVersion";
    public static final String HOSTNAME = "hostname";
    public static final String OS_ARCH = "osArch";
    public static final String USERNAME = "username";
    public static final String DESCRIPTION = "description";
    public static final String NAME = "nodename";
    public static final String TAGS = "tags";

    /**
     * All attribute names for predefined properties.
     */
    static final String[] PROPERTY_ATTRIBUTE_NAMES = {
        OS_NAME,
        OS_FAMILY,
        OS_VERSION,
        HOSTNAME,
        OS_ARCH,
        USERNAME,
        DESCRIPTION,
        NAME,
        TAGS
    };
    private Set tags;

    private Map<String, String> attributes;

    /**
     * Base constructor
     */
    public NodeEntryImpl() {
        super();
        this.tags = new HashSet();
        this.attributes = new HashMap<String, String>();
    }

    /**
     * Create an instance with a nodename value
     *
     * @param nodename the node name
     */
    public NodeEntryImpl(final String nodename) {
        this();
        setNodename(nodename);
    }

    /**
     * Create a NodeEntryImpl with hostname and nodename
     *
     * @param hostname hostname
     * @param nodename node name
     */
    public NodeEntryImpl(final String hostname, final String nodename) {
        this(nodename);
        setHostname(hostname);
    }

    /**
     * Factory method
     * @return create an instance with a hostname and node name.
     *
     * @param hostname hostname value
     * @param nodename node name
     */
    public static INodeEntry create(final String hostname, final String nodename) {
        return new NodeEntryImpl(hostname, nodename);
    }

    @Override
    public void setNodename(final String nodename) {
        super.setNodename(nodename);
        setAttribute(NAME, nodename);
    }

    @Override
    public String getNodename() {
        return getAttribute(NAME);
    }

    public Set getTags() {
        return tags;
    }

    public void setTags(final Set tags) {
        this.tags = tags;
        final Object[] objects = tags.toArray();
        Arrays.sort(objects);
        setAttribute(TAGS, StringArrayUtil.asString(objects, ", "));
    }

    public String getOsName() {
        return getAttribute(OS_NAME);
    }

    public void setOsName(final String osName) {
        setAttribute(OS_NAME, osName);
    }

    public String getOsFamily() {
        return getAttribute(OS_FAMILY);
    }

    public void setOsFamily(final String osFamily) {
        setAttribute(OS_FAMILY, osFamily);
    }

    public String getOsVersion() {
        return getAttribute(OS_VERSION);
    }

    public void setOsVersion(final String osVersion) {
        setAttribute(OS_VERSION, osVersion);
    }

    public String getHostname() {
        return getAttribute(HOSTNAME);
    }

    public void setHostname(final String hostname) {
        setAttribute(HOSTNAME, hostname);
    }

    public String getOsArch() {
        return getAttribute(OS_ARCH);
    }

    public void setOsArch(final String osArch) {
        setAttribute(OS_ARCH, osArch);
    }


    public String getUsername() {
        return getAttribute(USERNAME);
    }

    public void setUsername(final String username) {
        setAttribute(USERNAME, username);
    }


    public boolean equals(final INodeDesc node) {
        return getNodename().equals(node.getNodename());
    }

    @Override
    public String toString() {
        return "NodeEntryImpl{" +
               "tags=" + tags +
               ", attributes=" + attributes +
               ", project='" + project + '\'' +
               '}';
    }

    /**
     * Checks if nodename contains a user name
     *
     * @param host hostname value
     *
     * @return true if matches a "user@host" pattern
     */
    public static boolean containsUserName(final String host) {
        if (null == host) {
            throw new IllegalArgumentException("Null hostname value");
        }
        return host.matches(USER_AT_HOSTNAME_REGEX);
    }

    public boolean containsUserName() {
        return containsUserName(getHostname());
    }

    /**
     * Extract a username from a "username@hostname" pattern.
     *
     * @param hostname value
     *
     * @return username extracted, or null
     */
    public static String extractUserName(final String hostname) {
        if (containsUserName(hostname)) {
            return hostname.substring(0, hostname.indexOf("@"));
        } else {
            return null;
        }
    }

    /**
     * Gets the username for remote connections. The following logic is used: If the username property is set then the
     * username value is returned. If the username is not set and the hostname contains a "user@" prefix then the "user"
     * substring is returned If the hostname contains a substring that looks like a port (eg, :22) the port is stripped
     * off
     *
     * @return Gets the username for remote connections
     */
    public String extractUserName() {
        final String username = getUsername();
        if (null != username && !"".equals(username)) {
            return username;
        }
        return extractUserName(getHostname());
    }

    /**
     * Extract hostname from a string of the pattern "username@hostname:port"
     *
     * @param host the hostname
     *
     * @return the extracted hostname, or the original string if the pattern is not matched
     */
    public static String extractHostname(final String host) {
        String extracted = host;
        if (containsUserName(host)) {
            extracted = host.substring(host.indexOf("@") + 1, host.length());
        }

        if (containsPort(extracted)) {
            extracted = host.substring(0, host.indexOf(":"));
        }

        return extracted;
    }

    public String extractHostname() {
        return extractHostname(getHostname());
    }

    public String extractPort() {
        return extractPort(getHostname());
    }

    /**
     * Extract the port string from a string in the pattern "hostname:port"
     *
     * @param host the hostname
     *
     * @return the extracted port string, or null if the pattern is not matched.
     */
    public static String extractPort(final String host) {
        if (containsPort(host)) {
            return host.substring(host.indexOf(":") + 1, host.length());
        } else {
            return null;
        }
    }

    public boolean containsPort() {
        return containsPort(getHostname());
    }

    /**
     * Return true if the hostname contains a port value in the form "hostname:port".
     *
     * @param host hostname
     *
     * @return true if it matches the "host:port" pattern
     */
    public static boolean containsPort(final String host) {
        if (null == host) {
            throw new IllegalArgumentException("Null hostname value");
        }
        return host.matches(PORT_REGEX);
    }

    private String project;

    public void setFrameworkProject(final String project) {
        this.project = project;
    }

    public String getFrameworkProject() {
        return project;
    }


    /**
     * Get the map of attributes for the node, including all predefined attribtes available via acessors.
     *
     * @return attributes
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(final Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void setDescription(final String description) {
        setAttribute(DESCRIPTION, description);
    }

    public String getDescription() {
        return getAttribute(DESCRIPTION);
    }

    /**
     * @return the attributes for the node without any of the predefined attributes.
     */
    public Map<String, String> getExtendedAttributes() {
        return nodeExtendedAttributes(this);
    }

    /**
     * @return the attributes for the node without any of the predefined attributes.
     * @param node node
     */
    public static Map<String, String> nodeExtendedAttributes(final INodeEntry node) {
        final HashMap<String, String> attrs = new HashMap<String, String>();
        if (null != node.getAttributes()) {
            attrs.putAll(node.getAttributes());
        }
        for (final String attr : PROPERTY_ATTRIBUTE_NAMES) {
            attrs.remove(attr);
        }
        return attrs;
    }

    /**
     * @return the node attributes broken into namespaces, the result map will be contructed as:
     * "namespace" : { "key": ["attr","value"] }  where "attr" is the source full attribute name
     * @param node node
     */
    public static Map<String, Map<String, List<String>>> nodeNamespacedAttributes(final INodeEntry node) {
        final Map<String, String> attrs = nodeExtendedAttributes(node);
        final Map<String, Map<String, List<String>>> nsAttrs = new HashMap<String, Map<String, List<String>>>();
        for (String s : attrs.keySet()) {
            String[] parts=null;
            String ns="";
            String key=null;
            if (s.contains(":")) {
                parts = s.split(":", 2);
            }
            if (null != parts && parts.length > 1) {
                ns = notBlank(parts[0], "");
                key = parts[1];
            } else {
                ns = "";
                key = s;
            }
            if (null == nsAttrs.get(ns)) {
                nsAttrs.put(ns, new HashMap<String, List<String>>());
            }
            nsAttrs.get(ns).put(key, Arrays.asList(s, attrs.get(s)));
        }
        return nsAttrs;
    }

    private static String notBlank(String part, String defaultValue) {
        return null != part && !"".equals(part.trim()) ? part : defaultValue;
    }

    /**
     * Get the value for a specific attribute
     *
     * @param name attribute name
     *
     * @return attribute value, or null if it is not set
     */
    public String getAttribute(final String name) {
        return getAttributes().get(name);
    }

    /**
     * Set the value for a specific attribute
     *
     * @param name  attribute name
     * @param value attribute value
     *  @return value
     */
    public String setAttribute(final String name, final String value) {
        if(null!=value){
            return getAttributes().put(name, value);
        }else{
            getAttributes().remove(name);
            return value;
        }
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}

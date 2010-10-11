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

import java.util.*;


/**
 * NodeEntryImpl provides a bean representation of a nodes.properties entry
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodeEntryImpl extends NodeBaseImpl implements INodeEntry, INodeDesc {

    protected static final String USER_AT_HOSTNAME_REGEX = "([^@])+@([^@:])+";
    protected static final String PORT_REGEX = "([^:]+):([0-9]+)";

    private Set tags;
    private String osName;
    private String osArch;
    private String osFamily;
    private String osVersion;
    private String hostname;
    private String type;
    private String description;
    private Map<String, String> attributes;
    private Map<String, String> settings;

    private NodeEntryImpl(String nodename) {
        super(nodename);
        this.tags = new HashSet();
    }


    /**
     * Create a NodeEntryImpl with hostname and nodename
     *
     * @param hostname hostname
     * @param nodename node name
     */
    public NodeEntryImpl(final String hostname, final String nodename) {
        this(nodename);
        this.hostname = hostname;
    }

    public static INodeEntry create(final String hostname, final String nodename) {
        return new NodeEntryImpl(hostname, nodename);
    }

    public static INodeEntry create(final INodeDesc node) {
        return new NodeEntryImpl(node.getHostname(), node.getNodename());
    }

    public Set getTags() {
        return tags;
    }

    public void setTags(final Set tags) {
        this.tags = tags;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(final String osName) {
        this.osName = osName;
    }

    public String getOsFamily() {
        return osFamily;
    }

    public void setOsFamily(final String osFamily) {
        this.osFamily = osFamily;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(final String osVersion) {
        this.osVersion = osVersion;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getOsArch() {
        return osArch;
    }

    public void setOsArch(final String osArch) {
        this.osArch = osArch;
    }

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public boolean equals(final INodeDesc node) {
        return nodename.equals(node.getNodename());
    }


    public String toString() {
        return "NodeEntryImpl{" +
               "nodename=" + nodename +
               ",hostname=" + hostname +
               ",type=" + type +
               ",osName=" + osName +
               ",osArch=" + osArch +
               ",osFamily=" + osFamily +
               ",osVersion=" + osVersion +
               ",username=" + username +
               ",tags=" + tags +
               ",attributes=" + attributes +
               "}";
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

    public String extractUserName(final String hostname) {
        if (containsUserName(hostname)) {
            return hostname.substring(0, hostname.indexOf("@"));
        } else {
            return null;
        }
    }

    /**
     * Gets the username for remote connections. The following logic is used: If the username property is set then the
     * username value is returned. If the username is not set and the hostname contains a "user@" prefix then the
     * "user" substring is returned If the hostname contains a substring that looks like a port (eg, :22) the port is
     * stripped off
     *
     * @return Gets the username for remote connections
     */
    public String extractUserName() {
        if (null != username && !"".equals(username)) {
            return username;
        }
        return extractUserName(hostname);
    }

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
        return extractHostname(hostname);
    }

    public String extractPort() {
        return extractPort(hostname);
    }

    public static String extractPort(final String host) {
        if (containsPort(host)) {
            return host.substring(host.indexOf(":") + 1, host.length());
        } else {
            return host;
        }
    }

    public boolean containsPort() {
        return containsPort(hostname);
    }

    /**
     * Checks if nodename contains a port value
     *
     * @param host port value
     *
     * @return true if matches a "host:port" pattern
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
     * Get the map of attributes for the node.
     *
     * @return attributes
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }
}

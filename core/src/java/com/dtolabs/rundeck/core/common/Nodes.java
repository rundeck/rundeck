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

package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.utils.NodeSet;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;


/**
 * Parses the $RDECK_BASE/etc/nodes.properties file and provides various lookup methods.
 */
public class Nodes implements NodeReceiver {
    static Logger logger = Logger.getLogger(Nodes.class.getName());

    /**
     * Reference to mapfile
     */
    private final File nodesFile;

    private final HashMap<String,INodeEntry> nodes;
    private final HashMap<String, INodeEntry> nodesByHostname;

    private boolean valid;
    private NodeFileParserException parserException;

    /**
     * Return true if file was parsed correctly
     * @return
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Return any exception thrown by parser if it is not valid, otherwise return false
     * @return
     */
    public NodeFileParserException getParserException() {
        return parserException;
    }


    /**
     * Parsing format options
     */
    public static enum Format{
        resourcexml,
        resourceyaml
    }
    final private Format format;


    /**
     * Base constructor
     *
     * @param project project
     * @param nodesDataFile The file to parse
     * @param format
     * @throws NodeFileParserException if a parsing error occurs
     */
    protected Nodes(final File nodesDataFile, final Format format) throws
        NodeFileParserException {
        this.format= format;
        nodesFile = nodesDataFile;
        nodes=new HashMap<String, INodeEntry>();
        nodesByHostname=new HashMap<String, INodeEntry>();
        valid=false;
        if(!nodesFile.exists()){
            logger.warn("nodes resource file doesn't exist: " + nodesFile.getAbsolutePath());
            valid=true;
        }else{
            final NodeFileParser parser = createParser(nodesFile);
            try {
                parser.parse();
                valid=true;
            } catch (NodeFileParserException e) {
                this.parserException=e;
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void addFrameworkNode(final FrameworkProject project){

        //add local node if it does not exist
        final String fwkNode = project.getFrameworkProjectMgr().getFramework().getFrameworkNodeName();
        if (null == nodes.get(fwkNode)) {
            nodes.put(fwkNode, project.getFrameworkProjectMgr().getFramework().createFrameworkNode());
        }
    }

    /**
     * Factory method to create Nodes for a project, using specified nodes resources file
     *
     * @param project   framework project
     * @param nodesFile nodes resource file
     * @param format    file format
     *
     * @return Nodes object
     *
     * @throws NodeFileParserException if a parsing error occurs
     */
    public static Nodes create(final File nodesFile, final Format format) throws
        NodeFileParserException {
        return new Nodes(nodesFile, format);
    }

    /**
     * Gets the file containing the deployment information
     *
     * @return file
     */
    public File getFile() {
        return nodesFile;
    }


    /**
     * Return the list of {@link com.dtolabs.rundeck.core.common.INodeEntry} objects.
     * @return Collection of INodeEntry objects
     */
    public Collection<INodeEntry> listNodes() {
        final List<INodeEntry> l = new ArrayList<INodeEntry>(nodes.values());
        Collections.sort(l, new NodeEntryComparator());
        return l;
    }
    /**
     * Return the size of the set of nodes
     * @return size of unfiltered nodeset
     */
    public int countNodes() {
        return nodes.size();
    }

    /**
     * Comparator for INodeBase objects
     */
    public static class NodeEntryComparator implements Comparator<INodeBase> {
        /**
         * Creates a new instance of NodeEntryComparator
         */
        public NodeEntryComparator() {
        }

        public int compare(final INodeBase nodeA, final INodeBase nodeB) {
            return nodeA.getNodename().compareTo(nodeB.getNodename());
        }
    }


    /**
     * Return true if the node with the given name exists
     * @param name node name
     * @return true if node exists
     */
    public boolean hasNode(final String name) {
        return nodes.containsKey(name);
    }

    /**
     * Return true if the node with the given hostname exists
     *
     * @param hostname hostname
     *
     * @deprecated hostname is not unique, should not be used for identity
     */
    public boolean hasNodeByHostname(final String hostname) {
        return nodesByHostname.containsKey(hostname);
    }

    /**
     * Return the node
     * @param name name of the node object
     * @return INodeEntry object for the given node name
     */
    public INodeEntry getNode(final String name) {
        return nodes.get(name);
    }
    /**
     * Return the node
     * @param hostname name of the node object
     * @return INodeEntry object for the given node name
     * @deprecated hostname is not unique, should not be used for identity
     */
    public INodeEntry getNodeByHostname(final String hostname) {
        return nodesByHostname.get(hostname);
    }

    /**
     * Put a filled node entry into the dataset
     * @param iNodeEntry
     */
    public void putNode(final INodeEntry iNodeEntry) {
        nodes.put(iNodeEntry.getNodename(), iNodeEntry);
        nodesByHostname.put(iNodeEntry.getHostname(), iNodeEntry);
    }

    /**
     * Create a NodeFileParser given the project and the source file, using the predetermined format
     *
     * @param propfile the nodes resource file
     * @return a new parser based on the determined format
     */
    protected NodeFileParser createParser(final File propfile) {
        switch(format){
            case resourcexml:
                return new NodesXMLParser(propfile, this);
            case resourceyaml:
                return new NodesYamlParser(propfile,this);
            default:
                throw new IllegalArgumentException("Nodes resource file format not valid: " + format);
        }
    }


    /**
     * Return a collection of {@link INodeEntry} objects filtered by the nodeset, using the parameters
     * @param nodeEntries collection of source nodes references
     * @param nodeSet NodeSet object to apply filters from. a null NodeSet includes all nodes
     *
     * @return collection of matching {@link INodeEntry} objects
     */
    public Collection<INodeEntry> filterNodes(final Collection<? extends INodeBase> nodeEntries, final NodeSet nodeSet) {
        final ArrayList<INodeEntry> list = new ArrayList<INodeEntry>();
        for (final INodeBase nodeEntry: nodeEntries) {
            final INodeEntry entry = getNode(nodeEntry.getNodename());
            if (null != entry && (null == nodeSet || !nodeSet.shouldExclude(entry))) {
                list.add(entry); // should not be excluded so include it
            }
        }
        return list;
    }

   /**
     * Return a collection of {@link INodeEntry} objects for the collection of node names
     * @param nodenames collection of node identifiers
     *
     * @return collection of {@link INodeEntry} objects for the given node names
     */
    public Collection<INodeEntry> getNodeEntries(final Collection<? extends INodeBase> nodenames) {
        final ArrayList<INodeEntry> list = new ArrayList<INodeEntry>();
        for (final INodeBase nodename:nodenames) {
            final INodeEntry entry = getNode(nodename.getNodename());
            if (null!=entry) {
                list.add(entry); 
            }
        }
        return list;
    }
    /**
     * Return a collection of {@link INodeEntry} objects filtered by the nodeset,
     * using the values from {@link #listNodes()} and the nodeset parameter
     *
     * @param nodeset NodeSet object to apply filters from
     * @return collection of matching {@link INodeEntry} objects
     */
    public Collection<INodeEntry> filterNodes(final NodeSet nodeset) {
        return filterNodes(listNodes(), nodeset);
    }

    /**
     * Prints values of node entries. Useful for debug messages.
     * @return String containing formated node entries
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Nodes{");
        for (Iterator iter = nodes.values().iterator(); iter.hasNext();) {
            INodeEntry node = (INodeEntry) iter.next();
            sb.append(node.toString());
            sb.append(" ");
        }
        sb.append("}");

        return sb.toString();
    }


}

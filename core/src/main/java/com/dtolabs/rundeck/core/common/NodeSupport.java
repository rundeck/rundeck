package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.authorization.Decision;
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.resources.FileResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;

import java.io.File;
import java.net.URI;
import java.util.*;

/**
 * Created by greg on 2/20/15.
 */
public class NodeSupport implements IFrameworkNodes{
    private IPropertyLookup lookup;

    /**
     * Gets the value of "framework.server.hostname" property
     *
     * @return Returns value of framework.server.hostname property
     */
    @Override
    public String getFrameworkNodeHostname() {
        String hostname = getLookup().getProperty("framework.server.hostname");
        if (null!=hostname) {
            return hostname.trim();
        } else {
            return hostname;
        }
    }

    /**
     * Gets the value of "framework.server.name" property
     *
     * @return Returns value of framework.server.name property
     */
    @Override
    public String getFrameworkNodeName() {
        String name = getLookup().getProperty("framework.server.name");
        if (null!=name) {
            return name.trim();
        } else {
            return name;
        }
    }

    /**
     * @return Generate a node entry for the framework with default values
     */
    @Override
    public NodeEntryImpl createFrameworkNode() {
        NodeEntryImpl node = new NodeEntryImpl(getFrameworkNodeHostname(), getFrameworkNodeName());
        node.setUsername(getLookup().getProperty("framework.ssh.user"));
        node.setDescription("Rundeck server node");
        node.setOsArch(System.getProperty("os.arch"));
        node.setOsName(System.getProperty("os.name"));
        node.setOsVersion(System.getProperty("os.version"));
        //family has to be guessed at
        //TODO: determine cygwin somehow
        final String s = System.getProperty("file.separator");
        node.setOsFamily("/".equals(s) ? "unix" : "\\".equals(s) ? "windows" : "");
        return node;
    }



    /**
     * @return the nodeset consisting only of the input nodes where the specified actions are all authorized
     * @param project project name
     * @param actions action set
     * @param unfiltered nodes
     * @param authContext authoriziation
     */
    @Override
    public INodeSet filterAuthorizedNodes(
            final String project, final Set<String> actions, final INodeSet unfiltered,
            AuthContext authContext
    ) {
        if (null == actions || actions.size() <= 0) {
            return unfiltered;
        }
        final HashSet<Map<String, String>> resources = new HashSet<Map<String, String>>();
        for (final INodeEntry iNodeEntry : unfiltered.getNodes()) {
            HashMap<String, String> resdef = new HashMap<String, String>(iNodeEntry.getAttributes());
            resdef.put("type", "node");
            resdef.put("rundeck_server", Boolean.toString(isLocalNode(iNodeEntry)));
            resources.add(resdef);
        }
        final Set<Decision> decisions = authContext.evaluate(resources,
                                                             actions,
                                                             Collections.singleton(
                                                                     new Attribute(
                                                                             URI.create(EnvironmentalContext.URI_BASE + "project"),
                                                                             project
                                                                     )
                                                             ));
        final NodeSetImpl authorized = new NodeSetImpl();
        HashMap<String, Set<String>> authorizations = new HashMap<String, Set<String>>();
        for (final Decision decision : decisions) {
            if (decision.isAuthorized() && actions.contains(decision.getAction())) {
                final String nodename = decision.getResource().get("nodename");
                if(null==authorizations.get(nodename)) {
                    authorizations.put(nodename, new HashSet<String>());
                }
                authorizations.get(nodename).add(decision.getAction());
            }
        }
        for (final Map.Entry<String, Set<String>> entry : authorizations.entrySet()) {
            if(entry.getValue().size()==actions.size()) {
                authorized.putNode(unfiltered.getNode(entry.getKey()));
            }
        }
        return authorized;
    }




    /**
     * References the {@link INodeDesc} instance representing the framework node.
     */
    private INodeDesc nodedesc;
    /**
     * Gets the {@link INodeDesc} value describing the framework node
     * @return the singleton {@link INodeDesc} object for this framework instance
     */
    @Override
    public INodeDesc getNodeDesc() {
        if (null==nodedesc) {
            nodedesc = NodeEntryImpl.create(getFrameworkNodeHostname(), getFrameworkNodeName());
        }
        return nodedesc;
    }


    /**
     * Return true if the node is the local framework node.  Compares the (logical) node names
     * of the nodes after eliding any embedded 'user@' parts.
     * @param node the node
     * @return true if the node's name is the same as the framework's node name
     */
    @Override
    public boolean isLocalNode(INodeDesc node) {
        final String fwkNodeName = getFrameworkNodeName();
        return fwkNodeName.equals(node.getNodename());
    }

    public IPropertyLookup getLookup() {
        return lookup;
    }

    public void setLookup(final IPropertyLookup lookup) {
        this.lookup = lookup;
    }
}

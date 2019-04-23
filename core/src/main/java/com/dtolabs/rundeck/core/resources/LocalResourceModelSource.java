package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.CODE_SYNTAX_MODE;
import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.DISPLAY_TYPE_KEY;
import static com.dtolabs.rundeck.plugins.util.DescriptionBuilder.buildDescriptionWith;

public class LocalResourceModelSource implements ResourceModelSource {
    final Framework framework;
    String     description;
    String     hostname;
    String     osArch;
    String     osName;
    String     osVersion;
    String     osFamily;
    Properties attributes;

    public LocalResourceModelSource(final Framework framework) {
        this.framework = framework;
    }

    @Override
    public INodeSet getNodes() throws ResourceModelSourceException {
        NodeSetImpl newNodes = new NodeSetImpl();
        final NodeEntryImpl node = createFrameworkNodeNew();
        newNodes.putNode(node);
        return newNodes;
    }

    private NodeEntryImpl createFrameworkNodeNew() {
        NodeEntryImpl node = new NodeEntryImpl(
            null != hostname ? hostname : framework.getFrameworkNodeHostname(),
            framework.getFrameworkNodeName()
        );

        node.setDescription(description != null ? description : "Rundeck server node");
        node.setOsArch(osArch != null ? osArch : System.getProperty("os.arch"));
        node.setOsName(osName != null ? osName : System.getProperty("os.name"));
        node.setOsVersion(osVersion != null ? osVersion : System.getProperty("os.version"));
        //family has to be guessed at
        if (osFamily != null) {
            node.setOsFamily(osFamily);
        } else {
            final String s = System.getProperty("file.separator");
            node.setOsFamily("/".equals(s) ? "unix" : "\\".equals(s) ? "windows" : "");
        }
        if (null != attributes) {
            for (Object o : attributes.keySet()) {
                if (o.toString().equals("tags")) {
                    String[] split = attributes.getProperty(o.toString()).split("\\s*,\\s*");
                    node.setTags(new HashSet<String>(Arrays.asList(split)));

                } else {
                    node.setAttribute(o.toString(), attributes.getProperty(o.toString()));
                }
            }
        }
        return node;
    }

    private NodeEntryImpl createFrameworkNodeOrig() {
        return framework.createFrameworkNode();
    }

    static Description createDescription() {
        return buildDescriptionWith(d -> d
            .name(LocalResourceModelSourceFactory.SERVICE_PROVIDER_TYPE)
            .title("Local")
            .description("Provides the local node as the single resource")
            .property(
                p -> p
                    .string("description")
                    .title("Description")
                    .description("Description of the local server node")
                    .defaultValue("Rundeck server node")
            )
            .property(
                p -> p
                    .string("hostname")
                    .title("Hostname")
                    .description("Server hostname (default: via host OS)")
            )
            .property(
                p -> p
                    .string("osFamily")
                    .title("OS Family")
                    .description("OS Family: unix, windows, ... (default: via host OS)")
            )
            .property(
                p -> p
                    .string("osName")
                    .title("OS Name")
                    .description("(default: via host OS)")
            )
            .property(
                p -> p
                    .string("osArch")
                    .title("OS Architecture")
                    .description("(default: via host OS)")
            )
            .property(
                p -> p
                    .string("osVersion")
                    .title("OS Version")
                    .description("(default: via host OS)")
            )
            .property(
                p -> p
                    .string("attributes")
                    .renderingOption(DISPLAY_TYPE_KEY, "CODE")
                    .renderingOption(CODE_SYNTAX_MODE, "properties")
                    .title("Attributes")
                    .description("Custom attributes, in Java properties format")
            )
            .metadata("faicon", "hdd")
        );
    }

    public void configure(final Properties configuration) throws ConfigurationException {
        description = configuration.getProperty("description");
        hostname = configuration.getProperty("hostname");
        osFamily = configuration.getProperty("osFamily");
        osVersion = configuration.getProperty("osVersion");
        osName = configuration.getProperty("osName");
        osArch = configuration.getProperty("osArch");
        String attributes = configuration.getProperty("attributes");
        if (null != attributes && !("".equals(attributes.trim()))) {
            Properties props = new Properties();
            try {
                props.load(new StringReader(attributes));
            } catch (IOException e) {
                throw new ConfigurationException("Cannot parse attributes text as Java Properties format: "
                                                 + e.getMessage(), e);
            }
            this.attributes = props;
        }
    }
}

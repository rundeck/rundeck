package com.dtolabs.rundeck.app.api.node

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.core.common.INodeEntry
import groovy.transform.EqualsAndHashCode
import io.swagger.v3.oas.annotations.media.Schema

@ApiResource
@Schema(description = "Node")
@EqualsAndHashCode
class Node {

    @Schema(description = "Name of the node, this must be a unique Identifier across nodes within a project", example = "infra-node-1", requiredMode = Schema.RequiredMode.REQUIRED)
    String nodename

    @Schema(description = "Hostname of the node. This can be any IP Address or hostname to address the node.", example = "10.10.10.10:4001", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String hostname

    @Schema(description = "User name to connect to the node via SSH", example = "bob", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String username

    @Schema(description = "A description of the Node", example = "Infra node", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String description

    @Schema(description = "A list of tags associated with the node", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    List<String> tags

    @Schema(description = "An OS Family of the node", example = "Unix", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String osFamily

    @Schema(description = "OS Architecture", example = "amd64", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String osArch

    @Schema(description = "OS Name", example = "Linux", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String osName

    @Schema(description = "OS Version", example = "5.15.49-linuxkit", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String osVersion

    @Schema(description = "URL to an external resource model service", example = "http://example.com/resource", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String editUrl

    @Schema(description = "URL to an external resource model editor service", example = "http://example.com/editor", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String remoteUrl

    @Schema(description = "A map of additional attributes for the node", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    Map<String, String> attributes

    /**
     * Create a Node from an INodeEntry
     */
    static Node from(INodeEntry iNodeEntry) {
        new Node(
                nodename: iNodeEntry.getNodename(),
                hostname: iNodeEntry.getHostname(),
                username: iNodeEntry.getUsername(),
                description: iNodeEntry.getDescription(),
                tags: iNodeEntry.getTags().collect { it.toString() },
                osFamily: iNodeEntry.getOsFamily(),
                osArch: iNodeEntry.getOsArch(),
                osName: iNodeEntry.getOsName(),
                osVersion: iNodeEntry.getOsVersion(),
                attributes: iNodeEntry.getAttributes()
        )
    }
}

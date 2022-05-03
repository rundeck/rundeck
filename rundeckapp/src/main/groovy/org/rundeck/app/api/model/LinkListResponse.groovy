package org.rundeck.app.api.model

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema(description = "Links")
class LinkListResponse {

    @Schema(name = "_links", description = "Available named links")
    Map<String, Link> _links = [:]

    @Schema(name = "link", description = "Link definition")
    static class Link {

        @Schema(name = "href", description = "Link URL")
        String href
    }

    void addLink(String name, String href) {
        _links[name] = new Link(href: href)
    }
}

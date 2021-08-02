package org.rundeck.core.executions.provenance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExternalLink
        implements Provenance<ExternalLink.LinkData>
{
    private final LinkData data;

    @Getter
    @RequiredArgsConstructor
    public static class LinkData {
        private final String name;
        private final String url;
    }

    public static ExternalLink from(String name, String url) {
        return new ExternalLink(new LinkData(name, url));
    }
}

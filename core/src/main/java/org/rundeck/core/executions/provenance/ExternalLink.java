package org.rundeck.core.executions.provenance;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExternalLink
        implements Provenance<ExternalLink.LinkData>
{
    private LinkData data;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class LinkData {
        private String name;
        private String url;
    }

    public static ExternalLink from(String name, String url) {
        return new ExternalLink(new LinkData(name, url));
    }
}

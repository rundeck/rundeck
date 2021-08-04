package org.rundeck.core.executions.provenance;

import lombok.*;

@Getter

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WebRequestProvenance
        implements Provenance<WebRequestProvenance.RequestInfo>
{
    private RequestInfo data;

    @Override
    public String toString() {
        return "Web Request: "+data.requestUri;
    }

    @Getter

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    static class RequestInfo {
        private String requestUri;
    }

    public static WebRequestProvenance from(String requestUri) {
        return new WebRequestProvenance(new RequestInfo(requestUri));
    }
}

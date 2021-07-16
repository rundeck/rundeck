package org.rundeck.core.executions.provenance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class WebRequestProvenance
        implements Provenance<WebRequestProvenance.RequestInfo>
{
    private final RequestInfo data;

    public WebRequestProvenance(final RequestInfo data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Web Request: "+data.requestUri;
    }

    @Getter
    @RequiredArgsConstructor
    static class RequestInfo {
        private final String requestUri;
    }

    public static WebRequestProvenance from(String requestUri) {
        return new WebRequestProvenance(new RequestInfo(requestUri));
    }
}

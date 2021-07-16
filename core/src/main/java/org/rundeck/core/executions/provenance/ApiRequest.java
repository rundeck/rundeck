package org.rundeck.core.executions.provenance;

import lombok.Getter;

@Getter
public class ApiRequest implements Provenance<WebRequestProvenance.RequestInfo>{
    private final WebRequestProvenance.RequestInfo data;

    public ApiRequest(final WebRequestProvenance.RequestInfo data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "API Request : "+data.getRequestUri();
    }

}

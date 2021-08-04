package org.rundeck.core.executions.provenance;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ApiRequest implements Provenance<WebRequestProvenance.RequestInfo>{
    private WebRequestProvenance.RequestInfo data;

    @Override
    public String toString() {
        return "API Request : "+data.getRequestUri();
    }

}

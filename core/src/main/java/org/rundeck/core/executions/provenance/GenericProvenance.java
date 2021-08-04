package org.rundeck.core.executions.provenance;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

import java.util.Map;

@Getter

@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("generic")
@EqualsAndHashCode
public class GenericProvenance
        implements Provenance<GenericProvenance.GenericData>
{
    private GenericData data;

    @Getter

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    static class GenericData {
        private String description;
        private Map data;

    }

    @Override
    public String toString() {
        return data.description + ": " + data;
    }

    public static GenericProvenance from(String description, Map data) {
        return new GenericProvenance(new GenericData(description, data));
    }
}

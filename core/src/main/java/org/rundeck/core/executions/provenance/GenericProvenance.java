package org.rundeck.core.executions.provenance;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
@JsonTypeName("generic")
public class GenericProvenance
        implements Provenance<GenericProvenance.GenericData>
{
    private final GenericData data;

    @Getter
    @RequiredArgsConstructor
    static class GenericData {
        private final String description;
        private final Map data;

    }

    @Override
    public String toString() {
        return data.description + ": " + data;
    }

    public static GenericProvenance from(String description, Map data) {
        return new GenericProvenance(new GenericData(description, data));
    }
}

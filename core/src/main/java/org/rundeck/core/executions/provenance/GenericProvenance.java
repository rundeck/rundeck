package org.rundeck.core.executions.provenance;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
@JsonTypeName("generic")
public class GenericProvenance
        implements Provenance<Map>
{
    private final Map data;

    @Override
    public String toString() {
        return "Other(" + data + ")";
    }
}

package org.rundeck.core.executions.provenance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvenanceList {
    private List<Provenance<?>> provenances = new ArrayList<>();
}

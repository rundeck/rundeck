package org.rundeck.core.executions.provenance;

import java.util.Map;

/**
 * Defines a type of Provenance data for executions
 */
public interface ProvenanceComponent {

    Map<String,Class<? extends Provenance<?>>> getProvenanceTypes();

    <T> boolean handlesProvenance(Class<T> type);

    <T> String describeProvenance(T provenance);

}

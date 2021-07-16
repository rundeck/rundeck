package org.rundeck.core.executions.provenance;


/**
 * A basic provenance component
 */
public abstract class BaseProvenanceComponent
        implements ProvenanceComponent
{

    @Override
    public <T> boolean handlesProvenance(final Class<T> type) {
        return getProvenanceTypes().values().stream().anyMatch(t -> t.isAssignableFrom(type));
    }

    @Override
    public <T> String describeProvenance(final T provenance) {
        return provenance.toString();
    }
}

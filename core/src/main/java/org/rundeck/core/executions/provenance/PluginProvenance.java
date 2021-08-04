package org.rundeck.core.executions.provenance;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PluginProvenance
        implements Provenance<PluginProvenance.PluginData>
{
    private PluginData data;

    @Override
    public String toString() {
        return data.service + " Plugin: " + data.provider;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PluginData {
        private String provider;
        private String service;
    }
}

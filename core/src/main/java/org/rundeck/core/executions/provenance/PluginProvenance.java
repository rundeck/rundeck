package org.rundeck.core.executions.provenance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PluginProvenance
        implements Provenance<PluginProvenance.PluginData>
{
    private final PluginData data;

    @Override
    public String toString() {
        return data.service + " Plugin: " + data.provider;
    }

    @Getter
    public static class PluginData {

        private final String provider;
        private final String service;

        public PluginData(final String provider, final String service) {
            this.provider = provider;
            this.service = service;
        }
    }
}

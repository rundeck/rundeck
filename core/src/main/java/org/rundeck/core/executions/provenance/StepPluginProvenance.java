package org.rundeck.core.executions.provenance;

import lombok.*;

@Getter

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StepPluginProvenance
        implements Provenance<StepPluginProvenance.StepPluginData>
{
    private StepPluginData data;

    @Override
    public String toString() {
        return data.getService() + " Plugin: " + data.getProvider() + " - Step " + data.stepCtx;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class StepPluginData
            extends PluginProvenance.PluginData
    {

        private String stepCtx;

        public StepPluginData(final String provider, final String service, final String stepCtx) {
            super(provider, service);
            this.stepCtx = stepCtx;
        }
    }

}

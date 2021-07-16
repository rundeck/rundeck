package org.rundeck.core.executions.provenance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StepPluginProvenance
        implements Provenance<StepPluginProvenance.StepPluginData>
{
    private final StepPluginData data;

    @Override
    public String toString() {
        return data.getService() + " Plugin: " + data.getProvider() + " - Step " + data.stepCtx;
    }

    @Getter
    public static class StepPluginData
            extends PluginProvenance.PluginData
    {

        private final String stepCtx;

        public StepPluginData(final String provider, final String service, final String stepCtx) {
            super(provider, service);
            this.stepCtx = stepCtx;
        }
    }

}

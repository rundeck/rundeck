package org.rundeck.core.executions.provenance;

import java.util.Map;

public class ProvenanceUtil {
    public static GenericProvenance generic(Map data) {
        return new GenericProvenance(data);
    }

    public static ExecutionFollowupProvenance executionFollowup(String executionId) {
        return new ExecutionFollowupProvenance(new ExecutionFollowupProvenance.ExecutionData(executionId));
    }

    public static PluginProvenance plugin(final String provider, final String service) {
        return new PluginProvenance(new PluginProvenance.PluginData(provider, service));
    }

    public static StepPluginProvenance stepPlugin(final String provider, final String service, final String stepCtx) {
        return new StepPluginProvenance(new StepPluginProvenance.StepPluginData(provider, service, stepCtx));
    }

    public static WebRequestProvenance webRequest(final String requestUri) {
        return new WebRequestProvenance(new WebRequestProvenance.RequestInfo(requestUri));
    }

    public static ApiRequest apiRequest(final String requestUri) {
        return new ApiRequest(new WebRequestProvenance.RequestInfo(requestUri));
    }

    public static RetryProvenance retry(final String executionId, final String reason) {
        return new RetryProvenance(new RetryProvenance.RetryData(executionId, reason));
    }

    public static SchedulerProvenance scheduler(final String name, final String id, final String crontab) {
        return SchedulerProvenance.from(name, id, crontab);
    }

}

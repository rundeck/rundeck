package org.rundeck.core.executions.provenance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class RetryProvenance
        implements Provenance<RetryProvenance.RetryData>
{

    private final RetryData data;

    public RetryProvenance(final RetryData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Retry Execution " + data.executionId +
               (data.reason != null ? ": " + data.reason : "");
    }

    @Getter
    @RequiredArgsConstructor
    static class RetryData {
        private final String executionId;
        private final String reason;
    }

    public static RetryProvenance from(String executionId, String reason) {
        return new RetryProvenance(new RetryData(executionId,  reason));
    }
}

package org.rundeck.core.executions.provenance;

import lombok.*;

@Getter

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RetryProvenance
        implements Provenance<RetryProvenance.RetryData>
{

    private RetryData data;

    @Override
    public String toString() {
        return "Retry Execution " + data.executionId +
               (data.reason != null ? ": " + data.reason : "");
    }

    @Getter

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    static class RetryData {
        private String executionId;
        private String reason;
    }

    public static RetryProvenance from(String executionId, String reason) {
        return new RetryProvenance(new RetryData(executionId,  reason));
    }
}

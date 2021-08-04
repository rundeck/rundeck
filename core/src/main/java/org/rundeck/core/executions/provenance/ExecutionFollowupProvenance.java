package org.rundeck.core.executions.provenance;


import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExecutionFollowupProvenance
        implements Provenance<ExecutionFollowupProvenance.ExecutionData>
{
    private ExecutionData data;

    @Getter

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    static class ExecutionData {
        private String executionId;

    }

    @Override
    public String toString() {
        return "Execution " + data.getExecutionId();
    }
}

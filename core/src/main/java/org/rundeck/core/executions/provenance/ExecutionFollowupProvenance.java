package org.rundeck.core.executions.provenance;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExecutionFollowupProvenance
        implements Provenance<ExecutionFollowupProvenance.ExecutionData>
{
    private final ExecutionData data;

    @Getter
    @RequiredArgsConstructor
    static class ExecutionData {
        private final String executionId;

    }

    @Override
    public String toString() {
        return "Execution " + data.getExecutionId();
    }
}

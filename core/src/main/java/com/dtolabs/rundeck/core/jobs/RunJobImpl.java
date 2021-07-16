package com.dtolabs.rundeck.core.jobs;

import lombok.Builder;
import lombok.Getter;
import org.rundeck.core.executions.provenance.Provenance;

import java.util.List;
import java.util.Map;

@Builder
@Getter
public class RunJobImpl
        implements JobService.RunJob
{
    private final JobReference jobReference;

    private final List<Provenance<?>> provenance;
    private final String executionType;

    private final String argString;
    private final Map<String, ?> optionData;

    private final String jobFilter;

    private final String asUser;

    private final Map<String, ?> extraMeta;
}

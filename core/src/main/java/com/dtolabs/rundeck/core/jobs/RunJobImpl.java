package com.dtolabs.rundeck.core.jobs;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.rundeck.core.executions.Provenance;

import java.util.Map;

@Builder
@Getter
public class RunJobImpl
        implements JobService.RunJob
{
    private final JobReference jobReference;

    private final Provenance provenance;

    private final String argString;
    private final Map<String, ?> optionData;

    private final String jobFilter;

    private final String asUser;

    private final Map<String, ?> extraMeta;
}

package com.dtolabs.rundeck.core.jobs.options;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface JobOptionConfigEntry {
    Map toMap();

}

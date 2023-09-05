package com.dtolabs.rundeck.core.common;

import lombok.Data;

import java.util.Properties;

@Data
class SourceDefinitionImpl implements SourceDefinition {
    private final String type;
    private final Properties properties;
    private final Properties extraProperties;
    private final String ident;
    private final int index;
}

package org.rundeck.app.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemConfig
        implements SysConfigProp
{
    private final String key;
    private final String visibility;
    private final String category;
    private final String group;
    private final String strata;
    private final boolean required;
    private final boolean restart;
    private final String label;
    private final String labelCode;
    private final String datatype;
    private final boolean encrypted;
    private final String defaultValue;
    private final String link;
    private final String description;
    private final String descriptionCode;
    private final String authRequired;
}

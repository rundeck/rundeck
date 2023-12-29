package org.rundeck.app.config;

import java.util.List;

/**
 * Provides configuration definitions at System level
 */
public interface SystemConfigurable {
    List<SysConfigProp> getSystemConfigProps();
}

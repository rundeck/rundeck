package org.rundeck.app.components.project;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Names of builtin pseudo components used for project export
 */
public enum BuiltinExportComponents {
    jobs,
    executions,
    config;

    public static List<String> names() {
        return Arrays.stream(BuiltinExportComponents.values()).map(Enum::name).collect(Collectors.toList());
    }
}

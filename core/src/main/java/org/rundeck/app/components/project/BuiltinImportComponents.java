package org.rundeck.app.components.project;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Names of builtin pseudo components used for project import
 */
public enum BuiltinImportComponents {
    jobs,
    executions,
    config,
    readme,
    acl,
    scm;

    public static List<String> names() {
        return Arrays.stream(BuiltinImportComponents.values()).map(Enum::name).collect(Collectors.toList());
    }
}

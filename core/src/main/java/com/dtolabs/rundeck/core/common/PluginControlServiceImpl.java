package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.plugins.configuration.Description;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by rodrigo on 30-01-18.
 */
public class PluginControlServiceImpl implements PluginControlService {

    private final Framework framework;
    private final String    projectName;

    private PluginControlServiceImpl(Framework framework, String project) {
        this.framework = framework;
        this.projectName = project;
    }

    public static PluginControlService forProject(Framework framework, String project) {
        return new PluginControlServiceImpl(framework, project);
    }

    /**
     * @return list of disabled plugins for the project, in Service:provider format
     */
    @Override
    public List<String> listDisabledPlugins() {
        String disabledPlugins = framework.getProjectProperty(projectName, "disabled.plugins");
        if (disabledPlugins != null && !disabledPlugins.trim().isEmpty()) {
            return Arrays.asList(disabledPlugins.split("\\s*,\\s*"));
        }
        return new ArrayList<>();
    }

    /**
     * @param plugins     descriptions list
     * @param serviceName service name
     * @return list of enabled plugin descriptions
     */
    @Override
    public List<Description> filterEnabledPlugins(
        List<Description> plugins,
        final String serviceName
    ) {
        List<String> strings = listDisabledPlugins();
        return plugins
            .stream()
            .filter(description -> !strings.contains(serviceName + ":" + description.getName()))
            .collect(Collectors.toList());
    }

    /**
     * @param serviceName service name
     * @return predicate for testing enabled providers for a service
     */
    @Override
    public Predicate<String> enabledPredicateForService(
        final String serviceName
    ) {
        List<String> strings = listDisabledPlugins();
        return name -> !strings.contains(serviceName + ":" + name);
    }
    /**
     * @param serviceName service name
     * @return predicate for testing disabled providers for a service
     */
    @Override
    public Predicate<String> disabledPredicateForService(
        final String serviceName
    ) {
        return enabledPredicateForService(serviceName).negate();
    }

    /**
     * @param pluginName  provider name
     * @param serviceName service name
     * @return true if given plugin is disabled
     */
    @Override
    public boolean isDisabledPlugin(String pluginName, final String serviceName) {

        List<String> disabledPluginsList = listDisabledPlugins();
        return disabledPluginsList.contains(serviceName + ":" + pluginName);

    }

    /**
     * @param pluginName  provider name
     * @param serviceName service name
     * @throws PluginDisabledException if the given plugin is disabled
     */
    @Override
    public void checkDisabledPlugin(String pluginName, final String serviceName)
        throws PluginDisabledException {
        if (isDisabledPlugin(pluginName, serviceName)) {
            throw new PluginDisabledException(String.format(
                "%s Plugin '%s' is disabled",
                serviceName,
                pluginName
            ));
        }
    }
}

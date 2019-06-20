package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.plugins.configuration.Description;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by rodrigo on 30-01-18.
 */
public class PluginControlServiceImpl implements PluginControlService {

    public static final String DISABLED_PLUGINS = "disabled.plugins";
    private HashSet<String> disabledPlugins;
    private final IFramework framework;
    private final String project;

    private PluginControlServiceImpl(IFramework framework, String project) {
        this.framework = framework;
        this.project = project;
    }


    public static PluginControlService forProject(IFramework framework, String project) {

        return new PluginControlServiceImpl(framework, project);
    }

    /**
     * @return list of disabled plugins for the project, in Service:provider format
     */
    @Override
    public List<String> listDisabledPlugins() {
        return new ArrayList<>(getDisabledPlugins());
    }

    @Override
    public Set<String> getDisabledPlugins() {
        if (null == disabledPlugins) {
            synchronized (this) {
                if (null == disabledPlugins) {
                    IRundeckProject frameworkProject = framework.getFrameworkProjectMgr().getFrameworkProject(project);
                    String config =
                            frameworkProject.hasProperty(DISABLED_PLUGINS)
                            ? frameworkProject.getProperty(DISABLED_PLUGINS)
                            : null;
                    disabledPlugins = new HashSet<>(parseConfig(config));
                }
            }
        }
        return disabledPlugins;
    }

    private static List<String> parseConfig(final String pluginConfig) {
        if (pluginConfig != null && !pluginConfig.trim().isEmpty()) {
            return Arrays.asList(pluginConfig.split("\\s*,\\s*"));
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
        Set<String> strings = getDisabledPlugins();
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
        Set<String> strings = getDisabledPlugins();
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
        return getDisabledPlugins().contains(serviceName + ":" + pluginName);
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

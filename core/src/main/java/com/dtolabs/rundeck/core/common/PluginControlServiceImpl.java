package com.dtolabs.rundeck.core.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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

    private Set<String> getDisabledPlugins() {
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

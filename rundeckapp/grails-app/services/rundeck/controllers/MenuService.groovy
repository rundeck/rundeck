package rundeck.controllers

import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.rundeck.core.projects.ProjectConfigurable


class MenuService implements ProjectConfigurable {
    static transactional = false
    public static final List<Property> ProjectConfigProperties = [
            PropertyBuilder.builder().with {
                options 'readmeDisplay'
                title 'Display the Project Readme'
                values("projectList", "projectHome")
                labels([projectList: "Projects List", projectHome: "Project Home Page"])
                required(false)
                defaultValue null
            }.build(),
            PropertyBuilder.builder().with {
                options 'motdDisplay'
                title 'Display the Project MOTD'
                values("projectList", "projectHome", "navbar")
                labels([projectList: "Projects List", projectHome: "Project Home Page", navbar: "Navbar Indicator"])
                required(false)
                defaultValue null
            }.build(),

    ]
    public static final String CONF_PROJ_README_DISPLAY = 'project.gui.readme.display'
    public static final String CONF_PROJ_MOTD_DISPLAY = 'project.gui.motd.display'
    public static final String PROJ_DISPLAY_DEFAULT = 'none'
    final LinkedHashMap<String, String> ConfigPropertiesMapping = [
            'readmeDisplay': CONF_PROJ_README_DISPLAY,
            'motdDisplay'  : CONF_PROJ_MOTD_DISPLAY,
    ]

    @Override
    Map<String, String> getCategories() {
        [readmeDisplay: "gui", motdDisplay: 'gui']
    }

    @Override
    List<Property> getProjectConfigProperties() {
        ProjectConfigProperties
    }

    @Override
    Map<String, String> getPropertiesMapping() {
        ConfigPropertiesMapping
    }

    /**
     * Return project config for node cache delay
     * @param project
     * @return
     */
    Set<String> getReadmeDisplay(final IRundeckProjectConfig projectConfig) {
        getConfigSet(projectConfig, CONF_PROJ_README_DISPLAY, PROJ_DISPLAY_DEFAULT)
    }
    /**
     * Return project config for node cache delay
     * @param project
     * @return
     */
    Set<String> getMotdDisplay(final IRundeckProjectConfig projectConfig) {
        getConfigSet(projectConfig, CONF_PROJ_MOTD_DISPLAY, PROJ_DISPLAY_DEFAULT)
    }

    private Set<String> getConfigSet(
            IRundeckProjectConfig projectConfig, String config, String defval
    )
    {
        projectConfig.hasProperty(config) ?
                (projectConfig.getProperty(config)?.split(', *') as Set) :
                ([defval] as Set)
    }

}

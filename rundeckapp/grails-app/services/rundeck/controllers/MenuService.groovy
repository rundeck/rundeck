package rundeck.controllers

import com.dtolabs.rundeck.core.VersionConstants
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import org.rundeck.app.config.ConfigService
import org.rundeck.app.config.SysConfigProp
import org.rundeck.app.config.SystemConfig
import org.rundeck.app.config.SystemConfigurable
import org.rundeck.core.projects.ProjectConfigurable
import rundeck.services.FrameworkService

@CompileStatic
class MenuService implements ProjectConfigurable, SystemConfigurable {
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
            PropertyBuilder.builder().with {
                title "Allow Unsanitized HTML output"
                description """
If enabled, and a command's log filter specifies the `no-strip` meta tag, the output from the command will not be run through the tag sanitizer. This option only works if the framework level property is also enabled."""
                booleanType "allowUnsanitized"
                required false
                defaultValue "false"
            }.build()
    ]
    public static final String CONF_PROJ_README_DISPLAY = 'project.gui.readme.display'
    public static final String CONF_PROJ_MOTD_DISPLAY = 'project.gui.motd.display'
    public static final String CONF_PROJ_ALLOW_UNSANITIZED = 'project.output.allowUnsanitized'
    public static final String PROJ_DISPLAY_DEFAULT = 'none'
    final LinkedHashMap<String, String> ConfigPropertiesMapping = [
            'readmeDisplay': CONF_PROJ_README_DISPLAY,
            'motdDisplay'  : CONF_PROJ_MOTD_DISPLAY,
            'allowUnsanitized': CONF_PROJ_ALLOW_UNSANITIZED
    ]

    static final SysConfigProp SYS_CONFIG_DETECT_FIRST_RUN = SystemConfig.builder().with {
        key 'rundeck.startup.detectFirstRun'
        authRequired 'ops_admin'
        category 'Startup'
        datatype "Boolean"
        label "Detect First Run Version Splash"
        description 'Detect the first run and show the first run version splash info.'
        defaultValue "true"
        category "Custom"
        visibility "Advanced"
        strata "default"
        required false
        restart false
        build()
    }
    ConfigService configurationService
    FrameworkService frameworkService
    GrailsApplication grailsApplication

    @Override
    Map<String, String> getCategories() {
        [readmeDisplay: "gui", motdDisplay: 'gui', allowUnsanitized: 'gui']
    }

    @Override
    List<Property> getProjectConfigProperties() {
        ProjectConfigProperties
    }

    @Override
    Map<String, String> getPropertiesMapping() {
        ConfigPropertiesMapping
    }

    @Override
    List<SysConfigProp> getSystemConfigProps() {
        [
            SYS_CONFIG_DETECT_FIRST_RUN
        ]
    }

    /**
     *
     * @return true if the "First run" splash info should be displayed
     */
    boolean shouldShowFirstRunInfo(){
        boolean isFirstRun = false
        if(configurationService.getBoolean(MenuService.SYS_CONFIG_DETECT_FIRST_RUN, true) &&
           frameworkService.rundeckFramework.getPropertyLookup().hasProperty('framework.var.dir')) {
            def vardir = frameworkService.rundeckFramework.getPropertyLookup().getProperty('framework.var.dir')
            String buildIdent = VersionConstants.VERSION
            def vers = buildIdent.replaceAll('\\s+\\(.+\\)$','')
            def file = new File(vardir, ".first-run-${vers}")
            if(!file.exists()){
                isFirstRun=true
                file.withWriter("UTF-8"){out->
                    out.write('#'+(new Date().toString()))
                }
            }
        }
        return isFirstRun
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

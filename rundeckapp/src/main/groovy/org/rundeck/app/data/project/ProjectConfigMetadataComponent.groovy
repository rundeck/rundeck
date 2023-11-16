package org.rundeck.app.data.project

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.ProjectManager
import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.project.ProjectMetadataComponent
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ConfigurationService
import rundeck.services.ScheduledExecutionService

@CompileStatic
class ProjectConfigMetadataComponent implements ProjectMetadataComponent {
    public static final String CONFIG = 'config'
    public static final String SYSTEM_MODE = 'sysMode'
    final Set<String> availableMetadataNames = [CONFIG, SYSTEM_MODE].toSet()

    @Autowired
    ProjectManager projectManagerService
    @Autowired
    ConfigurationService configurationService

    @Override
    Optional<List<ComponentMeta>> getMetadataForProject(
        final String project,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
        List<ComponentMeta> result = new ArrayList<>()
        if (names.contains(CONFIG) || names.contains('*')) {
            result.add(
                getProjectConfigMeta(project)
            )
        }
        if (names.contains(SYSTEM_MODE) || names.contains('*')) {
            result.add(
                getSystemModeMeta()
            )
        }
        if(result){
            return Optional.of(result)
        }
        return Optional.empty()
    }

    ComponentMeta getProjectConfigMeta(String project) {
        def conf = projectManagerService.loadProjectConfig(project)

        def map = [
            executionsEnabled: (
                conf.getProperty(ScheduledExecutionService.CONF_PROJECT_DISABLE_EXECUTION) != 'true'
            ),
            scheduleEnabled  : (
                conf.getProperty(ScheduledExecutionService.CONF_PROJECT_DISABLE_SCHEDULE) != 'true'
            )
        ]
        if (conf.hasProperty(ScheduledExecutionService.CONF_GROUP_EXPAND_LEVEL)) {
            map.groupExpandLevel = tryParseInt(conf.getProperty(ScheduledExecutionService.CONF_GROUP_EXPAND_LEVEL), 1)
        }
        return ComponentMeta.with(
            CONFIG,
            map as Map<String, Object>
        )
    }

    private static int tryParseInt(String val, int defval) {
        try {
            return Integer.parseInt(val)
        } catch (NumberFormatException ignored) {
            return defval
        }
    }

    ComponentMeta getSystemModeMeta() {
        return ComponentMeta.with(
            SYSTEM_MODE,
            [
                active: configurationService.isExecutionModeActive()
            ] as Map<String, Object>
        )
    }
}

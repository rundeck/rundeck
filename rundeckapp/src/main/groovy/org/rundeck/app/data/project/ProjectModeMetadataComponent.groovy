package org.rundeck.app.data.project

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.ProjectManager
import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.project.ProjectMetadataComponent
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ConfigurationService
import rundeck.services.ScheduledExecutionService

@CompileStatic
class ProjectModeMetadataComponent implements ProjectMetadataComponent {
    public static final String PROJECT_MODE = 'projMode'
    public static final String SYSTEM_MODE = 'sysMode'
    final Set<String> availableMetadataNames = [PROJECT_MODE, SYSTEM_MODE].toSet()

    @Autowired
    ProjectManager projectManagerService
    @Autowired
    ConfigurationService configurationService

    @Override
    List<ComponentMeta> getMetadataForProject(
        final String project,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
        if (!names.contains(PROJECT_MODE) && !names.contains(SYSTEM_MODE) && !names.contains('*')) {
            return null
        }
        List<ComponentMeta> result = new ArrayList<>()
        if (names.contains(PROJECT_MODE) || names.contains('*')) {
            result.add(
                getProjectModeMeta(project)
            )
        }
        if (names.contains(SYSTEM_MODE) || names.contains('*')) {
            result.add(
                getSystemModeMeta()
            )
        }
        return result
    }

    ComponentMeta getProjectModeMeta(String project) {
        def conf = projectManagerService.loadProjectConfig(project)
        return ComponentMeta.with(
            PROJECT_MODE,
            [
                executionsEnabled: (
                    conf.getProperty(ScheduledExecutionService.CONF_PROJECT_DISABLE_EXECUTION) != 'true'
                ),
                scheduleEnabled  : (
                    conf.getProperty(ScheduledExecutionService.CONF_PROJECT_DISABLE_SCHEDULE) != 'true'
                )
            ] as Map<String, Object>
        )
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

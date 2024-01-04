package org.rundeck.app.data.project

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.project.ProjectMetadataComponent
import org.springframework.beans.factory.annotation.Autowired
import rundeck.controllers.MenuService
import rundeck.services.FrameworkService

@CompileStatic
class ProjectExecutionMetadataComponent implements ProjectMetadataComponent {
    public static final String NAME = 'executions'

    @Autowired
    MenuService menuService
    @Autowired
    FrameworkService frameworkService

    @Override
    Set<String> getAvailableMetadataNames() {
        return [NAME].toSet()
    }

    @Override
    Optional<List<ComponentMeta>> getMetadataForProject(
            final String project,
            final Set<String> names,
            final UserAndRolesAuthContext authContext
    ) {
        if (!names.contains(NAME) && !names.contains('*')) {
            return Optional.empty()
        }
        Map<String, Object> result = new HashMap<String, Object>()

        final def fwkProject = frameworkService.getFrameworkProject(project)
//
        result.put("readmeDisplay", menuService.getReadmeDisplay(fwkProject))

////        result.putAll(getProjAuthzMeta(project, authContext, PROJ_AUTH_CHECK_ACTIONS))
        if(result){
            return Optional.of(
                    [ComponentMeta.with(NAME, result)]
            )
        }

        return Optional.empty()

    }
}

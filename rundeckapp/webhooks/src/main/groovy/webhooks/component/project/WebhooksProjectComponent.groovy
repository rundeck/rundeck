package webhooks.component.project

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import groovy.transform.CompileStatic
import org.rundeck.app.components.project.ProjectComponent
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.projects.ProjectDataExporter
import org.rundeck.core.projects.ProjectDataImporter
import org.springframework.beans.factory.annotation.Autowired
import webhooks.WebhookService

@CompileStatic
class WebhooksProjectComponent implements ProjectComponent {
    public static final String COMPONENT_NAME = 'webhooks'
    @Autowired
    ProjectDataImporter webhooksProjectImporter
    @Autowired
    ProjectDataExporter webhooksProjectExporter
    @Autowired
    WebhookService webhookService

    final String name = COMPONENT_NAME
    final String title = 'Webhooks'

    @Override
    void projectDeleted(final String name) {
        webhookService.deleteWebhooksForProject(name)
    }

    @Override
    List<Property> getExportProperties() {
        webhooksProjectExporter.exportProperties
    }

    @Override
    List<String> getExportAuthRequiredActions() {
        return [AuthConstants.ACTION_ADMIN]
    }

    @Override
    List<String> getImportAuthRequiredActions() {
        return [AuthConstants.ACTION_ADMIN]
    }

    @Override
    List<String> getImportFilePatterns() {
        webhooksProjectImporter.importFilePatterns
    }

    @Override
    List<Property> getImportProperties() {
        webhooksProjectImporter.importProperties
    }

    @Override
    void export(final String project, final Object zipBuilder, final Map<String, String> exportOptions) {
        webhooksProjectExporter.export(project, zipBuilder, exportOptions)
    }

    @Override
    List<String> doImport(
        final UserAndRolesAuthContext authContext,
        final String project,
        final Map<String, File> importFiles,
        final Map<String, String> importOptions
    ) {
        return webhooksProjectImporter.doImport(authContext, project, importFiles, importOptions)
    }
}

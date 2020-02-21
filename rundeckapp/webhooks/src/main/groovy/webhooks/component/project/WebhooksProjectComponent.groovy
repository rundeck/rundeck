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

@CompileStatic
class WebhooksProjectComponent implements ProjectComponent {
    @Autowired
    ProjectDataImporter webhooksProjectImporter
    @Autowired
    ProjectDataExporter webhooksProjectExporter

    final String name = 'webhooks'
    final String exportTitle = 'Webhooks'
    final String importTitle = 'Webhooks'

    @Override
    List<Property> getExportProperties() {
        webhooksProjectExporter.exportProperties
    }

    @Override
    Collection<String> getExportAuthRequiredActions() {
        return [AuthConstants.ACTION_ADMIN]
    }

    @Override
    Collection<String> getImportAuthRequiredActions() {
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

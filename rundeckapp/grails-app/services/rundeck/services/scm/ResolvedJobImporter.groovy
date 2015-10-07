package rundeck.services.scm

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.plugins.scm.ImportResult
import com.dtolabs.rundeck.plugins.scm.JobImporter
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext

/**
 * Resolves a context and context job importer into a job importer
 */
class ResolvedJobImporter implements JobImporter {
    final ScmOperationContext context
    ContextJobImporter jobImporter

    ResolvedJobImporter(
            final ScmOperationContext context,
            final ContextJobImporter jobImporter
    )
    {
        this.context=context
        this.jobImporter = jobImporter
    }

    @Override
    ImportResult importFromStream(final String format, final InputStream input, final Map importMetadata) {
        return jobImporter.importFromStream(context, format, input, importMetadata)

    }

    @Override
    ImportResult importFromMap(final Map input, final Map importMetadata) {
        return jobImporter.importFromMap(context, input, importMetadata)
    }
}

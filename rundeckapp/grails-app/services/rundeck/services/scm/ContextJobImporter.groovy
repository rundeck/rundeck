package rundeck.services.scm

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.plugins.scm.ImportResult
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext

/**
 * Wrap {@link com.dtolabs.rundeck.plugins.scm.JobImporter} with project and auth context
 */
interface ContextJobImporter {

    ImportResult importFromStream(
            final ScmOperationContext context,
            final String format,
            final InputStream input,
            final Map importMetadata
    )

    ImportResult importFromMap(
            final ScmOperationContext context,
            final Map input,
            final Map importMetadata
    )
}

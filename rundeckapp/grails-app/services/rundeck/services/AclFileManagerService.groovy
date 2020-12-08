package rundeck.services


import com.dtolabs.rundeck.core.authorization.providers.Validator
import com.dtolabs.rundeck.core.storage.StorageManager
import groovy.transform.CompileStatic
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import org.rundeck.app.acl.ContextACLStorageFileManager
import org.springframework.beans.factory.InitializingBean

/**
 * Delegates to the aclStorageFileManager bean
 */
@CompileStatic
class AclFileManagerService {

    @Delegate
    ContextACLManager<AppACLContext> aclStorageFileManager
}

package rundeck.services


import com.dtolabs.rundeck.core.authorization.providers.Validator
import com.dtolabs.rundeck.core.storage.StorageManager
import groovy.transform.CompileStatic
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import org.rundeck.app.acl.ContextACLStorageFileManager
import org.springframework.beans.factory.InitializingBean

/**
 * Delegates to the aclStorageFileManager bean, but this bean is necessary as the
 * primary DI bean that can be replaced by another implementation, while allowing the
 * delegated bean to still be used if necessary.
 */
@CompileStatic
class AclFileManagerService {

    @Delegate
    ContextACLManager<AppACLContext> aclStorageFileManager
}

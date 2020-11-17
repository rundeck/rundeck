package rundeck.services

import com.dtolabs.rundeck.core.authorization.RuleSetValidation
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection
import com.dtolabs.rundeck.core.authorization.providers.Validator
import com.dtolabs.rundeck.core.storage.StorageManager
import groovy.transform.CompileStatic
import org.rundeck.app.acl.ACLFileManager
import org.rundeck.app.acl.ACLStorageFileManager
import org.springframework.beans.factory.InitializingBean

import java.util.function.Supplier

@CompileStatic
class AclManagerService implements ACLFileManager, InitializingBean {
    public static final String ACL_STORAGE_PATH_BASE = 'acls/'

    StorageManager configStorageService
    Validator rundeckYamlAclValidator
    @Delegate
    private ACLStorageFileManager aclStorageFileManager

    @Override
    Validator getValidator() {
        return rundeckYamlAclValidator
    }

    @Override
    void afterPropertiesSet() throws Exception {
        aclStorageFileManager = ACLStorageFileManager.
            builder().
            validator(rundeckYamlAclValidator).
            storage(configStorageService).
            prefix(ACL_STORAGE_PATH_BASE).
            build()
    }

    ACLFileManager getProjectAclManager(String project) {
        //verify project exists?
        ACLStorageFileManager.
            builder().
            validator(rundeckYamlAclValidator).
            storage(configStorageService).
            prefix("projects/$project/" + ACL_STORAGE_PATH_BASE).
            build()
    }

}

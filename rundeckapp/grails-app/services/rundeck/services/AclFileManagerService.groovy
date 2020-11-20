package rundeck.services


import com.dtolabs.rundeck.core.authorization.providers.Validator
import com.dtolabs.rundeck.core.storage.StorageManager
import groovy.transform.CompileStatic
import org.rundeck.app.acl.ACLFileManager
import org.rundeck.app.acl.ACLStorageFileManager
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import org.rundeck.app.acl.ContextACLStorageFileManager
import org.springframework.beans.factory.InitializingBean

@CompileStatic
class AclFileManagerService implements InitializingBean {
    public static final String ACL_STORAGE_PATH_BASE = 'acls/'

    StorageManager configStorageService
    Validator rundeckYamlAclValidator

    @Delegate
    private ContextACLManager<AppACLContext> aclManager

    Validator getValidator() {
        return rundeckYamlAclValidator
    }

    @Override
    void afterPropertiesSet() throws Exception {
        aclManager = ContextACLStorageFileManager
            .<AppACLContext>builder()
            .validator(rundeckYamlAclValidator)
            .storageManager(configStorageService)
            .prefixMapping(
                { AppACLContext context ->
                    context.system ?
                    ACL_STORAGE_PATH_BASE :
                    "projects/$context.project/" + ACL_STORAGE_PATH_BASE
                }
            )
            .build()
    }

    ACLFileManager getProjectAclManager(String project) {
         aclManager.forContext(AppACLContext.project(project))
    }


}

package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.providers.Validator
import com.dtolabs.rundeck.core.storage.StorageManager
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import org.rundeck.app.acl.ContextACLStorageFileManager
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired

import java.util.regex.Pattern

/**
 * Factory for ContextACLManager which uses the configStorageService storage
 */
class ContextACLStorageFileManagerFactory implements FactoryBean<ContextACLManager<AppACLContext>> {
    public static final String ACL_STORAGE_PATH_BASE = 'acls/'
    public static final String ACL_PROJECT_STORAGE_PATH_PATTERN = 'projects/{PROJECT}/acls/'

    @Autowired StorageManager configStorageService
    @Autowired Validator rundeckYamlAclValidator
    String systemPrefix
    String projectPattern

    @Override
    ContextACLManager<AppACLContext> getObject() throws Exception {
        return ContextACLStorageFileManager
            .<AppACLContext> builder()
            .validator(rundeckYamlAclValidator)
            .storageManager(configStorageService)
            .prefixMapping(
                { AppACLContext context ->
                    context.system ?
                    systemPrefix :
                    projectPattern.replaceAll(Pattern.quote('{PROJECT}'), context.project)
                }
            ).build()
    }

    @Override
    Class<?> getObjectType() {
        return ContextACLManager
    }
}

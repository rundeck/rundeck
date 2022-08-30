package org.rundeck.app.authorization

import com.dtolabs.rundeck.app.internal.framework.RundeckFrameworkFactory
import com.dtolabs.rundeck.core.authorization.providers.ValidatorFactory
import com.dtolabs.rundeck.core.common.FrameworkProjectMgr
import com.dtolabs.rundeck.core.storage.StorageManager
import groovy.util.logging.Slf4j
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import org.rundeck.app.acl.ContextACLStorageFileManager
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired

import java.util.regex.Pattern

/**
 * Factory for ContextACLManager which uses the configStorageService storage
 */
@Slf4j
class ContextACLStorageFileManagerFactory implements FactoryBean<ContextACLManager<AppACLContext>> {
    public static final String ACL_STORAGE_PATH_BASE = 'acls/'
    public static final String ACL_PROJECT_STORAGE_PATH_PATTERN = 'projects/{PROJECT}/acls/'

    @Autowired StorageManager configStorageService
    @Autowired ValidatorFactory rundeckYamlAclValidatorFactory
    ValidatorFactory validatorFactory;
    String systemPrefix
    String projectPattern

    @Override
    ContextACLManager<AppACLContext> getObject() throws Exception {
        return ContextACLStorageFileManager
            .builder()
            .storageManager(configStorageService)
            .validatorFactory(rundeckYamlAclValidatorFactory)
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

package org.rundeck.app.authorization

import com.dtolabs.rundeck.app.internal.framework.RundeckFrameworkFactory
import com.dtolabs.rundeck.core.authorization.providers.ValidatorFactory
import com.dtolabs.rundeck.core.common.FrameworkProjectMgr
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
    @Autowired ValidatorFactory rundeckYamlAclValidatorFactory
    String projectsStorageType
    FrameworkProjectMgr filesystemProjectManager
    ValidatorFactory validatorFactory;
    String systemPrefix
    String projectPattern

    @Override
    ContextACLManager<AppACLContext> getObject() throws Exception {
        ContextACLManager<AppACLContext> contextACLStorageFileManager = ContextACLStorageFileManager
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
        if (RundeckFrameworkFactory.isFSType(projectsStorageType)) {
            //manager that uses Storage for system files, and filesystem for project files
            return new CombinedContextACLManager(
                systemACLManager: contextACLStorageFileManager,
                projectManager: filesystemProjectManager,
                validatorFactory: validatorFactory
            )
        } else {
            return contextACLStorageFileManager
        }

    }

    @Override
    Class<?> getObjectType() {
        return ContextACLManager
    }
}

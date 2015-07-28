package rundeck.services

import com.dtolabs.rundeck.core.authorization.AclRuleSetSource
import com.dtolabs.rundeck.core.authorization.AclsUtil
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.RuleEvaluator
import com.dtolabs.rundeck.core.authorization.providers.Policies
import com.dtolabs.rundeck.core.authorization.providers.PoliciesCache
import com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization
import com.dtolabs.rundeck.core.authorization.providers.YamlProvider

class AuthorizationService {
    public static final String ACL_STORAGE_PATH_BASE = 'acls/'

    def configStorageService
    def rundeckFilesystemPolicyAuthorization

    /**
     * Get the top-level system authorization
     * @return
     */
    def Authorization getSystemAuthorization() {
        AclsUtil.append rundeckFilesystemPolicyAuthorization, getStoredAuthorization()
    }

    /**
     * list group/role names used by the policies
     * @return
     */
    def Set<String> getRoleList() {
        AclsUtil.getGroups(AclsUtil.merge(getFilesystemRules(), getStoredPolicies()))
    }
    def AclRuleSetSource getFilesystemRules(){
        if(rundeckFilesystemPolicyAuthorization instanceof SAREAuthorization){
            return rundeckFilesystemPolicyAuthorization.policies
        }else if(rundeckFilesystemPolicyAuthorization instanceof RuleEvaluator){
            return rundeckFilesystemPolicyAuthorization
        }
    }


    private Policies getStoredPolicies() {
        //TODO: cache
        loadStoredPolicies()
    }
    /**
     * return authorization from storage contents
     * @return authorization
     */
    public Authorization getStoredAuthorization() {
        //TODO: cache
        loadStoredAuthorization()
    }

    /**
     * load authorization from storage contents
     * @return authorization
     */
    private Policies loadStoredPolicies() {
        def paths = configStorageService.listDirPaths(ACL_STORAGE_PATH_BASE, ".*\\.aclpolicy")
        log.debug("getStoredAuthorization. paths: ${paths}")
        def sources = paths.collect { path ->
            def file = configStorageService.getFileResource(path).contents
            YamlProvider.sourceFromStream("[system:config]${path}", file.inputStream, file.modificationTime)
        }
        new Policies(PoliciesCache.fromSources(sources))
    }

    /**
     * load authorization from storage contents
     * @return authorization
     */
    private Authorization loadStoredAuthorization() {
        return AclsUtil.createAuthorization(loadStoredPolicies())
    }

}

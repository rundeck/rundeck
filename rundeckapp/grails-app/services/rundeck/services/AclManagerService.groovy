package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.ValidationSet
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection
import com.dtolabs.rundeck.core.authorization.providers.YamlProvider
import groovy.transform.CompileStatic
import org.rundeck.app.acl.ACLManager
import rundeck.services.authorization.PoliciesValidation

import java.util.function.Supplier

@CompileStatic
class AclManagerService implements ACLManager {
    public static final String ACL_STORAGE_PATH_BASE = 'acls/'

    ConfigStorageService configStorageService

    /**
     * List the system aclpolicy file paths, including the base dir name of acls/
     * @return
     */
    @Override
    public List<String> listStoredPolicyPaths() {
        configStorageService.listDirPaths(ACL_STORAGE_PATH_BASE, ".*\\.aclpolicy")
    }
    /**
     * List the system aclpolicy file names, not including the dir path
     * @return
     */
    public List<String> listStoredPolicyFiles() {
        listStoredPolicyPaths().collect {
            it.substring(ACL_STORAGE_PATH_BASE.size())
        }
    }

    /**
     *
     * @param file name without path
     * @return true if the policy file with the given name exists
     */
    @Override
    public boolean existsPolicyFile(String file) {
        configStorageService.existsFileResource(ACL_STORAGE_PATH_BASE + file)
    }

    /**
     * @param fileName name of policy file, without path
     * @return text contents of the policy file
     */
    @Override
    public String getPolicyFileContents(String fileName) {
        getAclPolicy(fileName).text
    }
    /**
     * Load content to output stream
     * @param fileName name of policy file, without path
     * @return length of output
     */
    @Override
    public long loadPolicyFileContents(String fileName, OutputStream outputStream) {
        configStorageService.loadFileResource(ACL_STORAGE_PATH_BASE + fileName, outputStream)
    }

    @Override
    AclPolicyFile getAclPolicy(final String fileName) {
        def resource = configStorageService.getFileResource(ACL_STORAGE_PATH_BASE + fileName)
        def file = resource.contents

        return new AclPolicyImpl(
            name: fileName,
            inputStream: file.&getInputStream,
            modified: file.modificationTime,
            created: file.creationTime
        )
    }

    @CompileStatic
    static class AclPolicyImpl implements AclPolicyFile{
        String textContent
        Supplier<InputStream> inputStream
        @Override
        String getText() {
            if (!textContent && inputStream) {
                textContent = inputStream.get().getText()
                inputStream = null
            }
            return textContent
        }
        Date modified
        Date created
        String name
    }

    /**
     * Store a system policy file
     * @param fileName name without path
     * @param fileText contents
     * @return size of bytes stored
     */
    @Override
    public long storePolicyFileContents(String fileName, String fileText) {
        def bytes = fileText.bytes
        def result = configStorageService.writeFileResource(
            ACL_STORAGE_PATH_BASE + fileName,
            new ByteArrayInputStream(bytes),
            [:]
        )
        bytes.length
    }

    /**
     * Delete a policy file
     * @return true if successful
     */
    @Override
    public boolean deletePolicyFile(String fileName) {
        configStorageService.deleteFileResource(ACL_STORAGE_PATH_BASE + fileName)
    }


    @Override
    public PoliciesValidation validateYamlPolicy(String ident, String text) {
        validateYamlPolicy(null, ident, text)
    }
    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident identity string for the sources
     * @param text yaml aclpolicy text
     * @return validation
     */
    @Override
    public PoliciesValidation validateYamlPolicy(String project, String ident, String text) {
        ValidationSet validation = new ValidationSet()
        def source = YamlProvider.sourceFromString(ident, text, new Date(), validation)
        def policies = YamlProvider.policiesFromSource(
            source,
            project ? AuthorizationUtil.projectContext(project) : null,
            validation
        )
        validation.complete();
        new PoliciesValidation(validation: validation, policies: policies)
    }
    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident identity string for the sources
     * @param text yaml aclpolicy text
     * @return validation
     */
    @Override
    public PoliciesValidation validateYamlPolicy(String project, String ident, File source) {
        ValidationSet validation = new ValidationSet()
        PolicyCollection policies = null
        source.withInputStream { stream ->
            def streamSource = YamlProvider.sourceFromStream(ident, stream, new Date(), validation)
            policies = YamlProvider.policiesFromSource(
                streamSource,
                project ? AuthorizationUtil.projectContext(project) : null,
                validation
            )
        }
        validation.complete();
        new PoliciesValidation(validation: validation, policies: policies)
    }

    @Override
    public PoliciesValidation validateYamlPolicy(File file) {
        ValidationSet validation = new ValidationSet()
        def policies = YamlProvider.policiesFromSource(YamlProvider.sourceFromFile(file, validation), null, validation)
        validation.complete();
        new PoliciesValidation(validation: validation, policies: policies)
    }

}

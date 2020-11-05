package rundeck.services

import com.dtolabs.rundeck.core.authorization.RuleSetValidation
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection
import com.dtolabs.rundeck.core.authorization.providers.Validator
import groovy.transform.CompileStatic
import org.rundeck.app.acl.ACLManager

import java.util.function.Supplier

@CompileStatic
class AclManagerService implements ACLManager {
    public static final String ACL_STORAGE_PATH_BASE = 'acls/'

    ConfigStorageService configStorageService
    Validator rundeckYamlAclValidator

    @Override
    Validator getValidator() {
        return rundeckYamlAclValidator
    }

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

    public RuleSetValidation<PolicyCollection> validatePolicyFile(String fname){
        def exists = existsPolicyFile(fname)
        if (!exists) {
            return null
        }
        return validator.validateYamlPolicy(
            null,
            fname,
            getPolicyFileContents(fname)
        )
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
}

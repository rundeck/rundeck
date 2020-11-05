package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection;
import com.dtolabs.rundeck.core.authorization.providers.Validator;

import java.io.File;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

public interface ACLManager {
    Validator getValidator();

    boolean deletePolicyFile(String fileName);

    /**
     * Store a system policy file
     *
     * @param fileName name without path
     * @param fileText contents
     * @return size of bytes stored
     */
    long storePolicyFileContents(String fileName, String fileText);

    /**
     * Retrieve a system policy
     *
     * @param fileName name without path
     * @return definition
     */
    AclPolicyFile getAclPolicy(String fileName);

    interface AclPolicyFile {
        String getText();
        Date getModified();
        Date getCreated();
        String getName();
    }

    /**
     * @param fileName name of policy file, without path
     * @return text contents of the policy file
     */
    String getPolicyFileContents(String fileName);

    /**
     * Load content to output stream
     *
     * @param fileName name of policy file, without path
     * @return length of output
     */
    public long loadPolicyFileContents(String fileName, OutputStream outputStream);

    /**
     * @param file name without path
     * @return true if the policy file with the given name exists
     */
    boolean existsPolicyFile(String file);

    RuleSetValidation<PolicyCollection> validatePolicyFile(String fname);

    /**
     * List the system aclpolicy file paths, including the base dir name of acls/
     */
    List<String> listStoredPolicyPaths();

    /**
     * List the system aclpolicy file names, not including the dir path
     */
    List<String> listStoredPolicyFiles();
}

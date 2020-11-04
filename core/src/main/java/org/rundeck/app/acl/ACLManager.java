package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;

import java.io.File;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

public interface ACLManager {
    RuleSetValidation<?> validateYamlPolicy(File file);

    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     *
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident   identity string for the sources
     * @param source  file source
     * @return validation
     */
    RuleSetValidation<?> validateYamlPolicy(String project, String ident, File source);

    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     *
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident   identity string for the sources
     * @param text    yaml aclpolicy text
     * @return validation
     */
    RuleSetValidation<?> validateYamlPolicy(String project, String ident, String text);

    RuleSetValidation<?> validateYamlPolicy(String ident, String text);

    /**
     * Delete a policy file
     *
     * @return true if successful
     */
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

    /**
     * List the system aclpolicy file paths, including the base dir name of acls/
     */
    List<String> listStoredPolicyPaths();

    /**
     * List the system aclpolicy file names, not including the dir path
     */
    List<String> listStoredPolicyFiles();
}

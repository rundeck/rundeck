package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection;
import com.dtolabs.rundeck.core.authorization.providers.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;

public interface ContextACLManager<T> {
    Validator getValidator();
    ACLFileManager forContext(T context);

    /**
     * Define mapping to add listeners when managers are created
     */
    void addListenerMap(Function<T, ACLFileManagerListener> mapping);

    /**
     * Receive notification of changes
     */
    void addListener(T context, ACLFileManagerListener listener);

    /**
     * Remove a listener
     */
    void removeListener(T context, ACLFileManagerListener listener);

    /**
     * Store a system policy file
     *
     * @param fileName name without path
     * @param input    input stream
     * @return size of bytes stored
     */
    long storePolicyFile(T context, String fileName, InputStream input);

    /**
     * Delete a policy file
     *
     * @return true if successful
     */
    boolean deletePolicyFile(T context, String fileName);

    /**
     * Store a system policy file
     *
     * @param fileName name without path
     * @param fileText contents
     * @return size of bytes stored
     */
    long storePolicyFileContents(T context, String fileName, String fileText);

    /**
     * Retrieve a system policy
     *
     * @param fileName name without path
     * @return definition
     */
    AclPolicyFile getAclPolicy(T context, String fileName);

    /**
     * @param fileName name of policy file, without path
     * @return text contents of the policy file
     */
    String getPolicyFileContents(T context, String fileName) throws IOException;

    /**
     * Load content to output stream
     *
     * @param fileName name of policy file, without path
     * @return length of output
     */
    public long loadPolicyFileContents(T context, String fileName, OutputStream outputStream)
            throws IOException;

    /**
     * @param file name without path
     * @return true if the policy file with the given name exists
     */
    boolean existsPolicyFile(T context, String file);

    RuleSetValidation<PolicyCollection> validatePolicyFile(T context, String fname) throws IOException;


    /**
     * List the system aclpolicy file names, not including the dir path
     */
    List<String> listStoredPolicyFiles(T context);
}

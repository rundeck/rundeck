package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Base context acl manager, uses {@link #forContext(Object)} to retrieve ACLFilemanager, and apply corresponding method
 * call
 *
 * @param <T>
 */
public abstract class BaseContextACLManager<T>
        implements ContextACLManager<T>
{

    @Override
    public void addListener(final T context, final ACLFileManagerListener listener) {
        forContext(context).addListener(listener);
    }

    @Override
    public void removeListener(final T context, final ACLFileManagerListener listener) {
        forContext(context).removeListener(listener);
    }

    @Override
    public long storePolicyFile(final T context, final String fileName, final InputStream input) {
        return forContext(context).storePolicyFile(fileName, input);
    }

    @Override
    public boolean deletePolicyFile(final T context, final String fileName) {
        return forContext(context).deletePolicyFile(fileName);
    }

    @Override
    public long storePolicyFileContents(final T context, final String fileName, final String fileText) {
        return forContext(context).storePolicyFileContents(fileName, fileText);
    }

    @Override
    public AclPolicyFile getAclPolicy(final T context, final String fileName) {
        return forContext(context).getAclPolicy(fileName);
    }

    @Override
    public String getPolicyFileContents(final T context, final String fileName) throws IOException {
        return forContext(context).getPolicyFileContents(fileName);
    }

    @Override
    public long loadPolicyFileContents(
            final T context,
            final String fileName,
            final OutputStream outputStream
    )
            throws IOException
    {
        return forContext(context).loadPolicyFileContents(fileName, outputStream);
    }

    @Override
    public boolean existsPolicyFile(final T context, final String file) {
        return forContext(context).existsPolicyFile(file);
    }

    @Override
    public RuleSetValidation<PolicyCollection> validatePolicyFile(
            final T context, final String fname
    ) throws IOException
    {
        return forContext(context).validatePolicyFile(fname);
    }


    @Override
    public List<String> listStoredPolicyFiles(final T context) {
        return forContext(context).listStoredPolicyFiles();
    }
}

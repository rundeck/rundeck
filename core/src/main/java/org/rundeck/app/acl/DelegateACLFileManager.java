package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.providers.BaseValidator;
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection;
import com.dtolabs.rundeck.core.authorization.providers.Validator;
import lombok.Builder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Builder
public class DelegateACLFileManager
        implements ACLFileManager
{
    private final ACLFileManager delegate;

    @Override
    public BaseValidator getValidator() {
        return getDelegate().getValidator();
    }

    @Override
    public void addListener(final ACLFileManagerListener listener) {
        getDelegate().addListener(listener);
    }

    @Override
    public void removeListener(final ACLFileManagerListener listener) {
        getDelegate().removeListener(listener);
    }

    @Override
    public long storePolicyFile(final String fileName, final InputStream input) {
        return getDelegate().storePolicyFile(fileName, input);
    }

    @Override
    public boolean deletePolicyFile(final String fileName) {
        return getDelegate().deletePolicyFile(fileName);
    }

    @Override
    public long storePolicyFileContents(final String fileName, final String fileText) {
        return getDelegate().storePolicyFileContents(fileName, fileText);
    }

    @Override
    public AclPolicyFile getAclPolicy(final String fileName) {
        return getDelegate().getAclPolicy(fileName);
    }

    @Override
    public String getPolicyFileContents(final String fileName) throws IOException {
        return getDelegate().getPolicyFileContents(fileName);
    }

    @Override
    public long loadPolicyFileContents(final String fileName, final OutputStream outputStream) throws IOException {
        return getDelegate().loadPolicyFileContents(fileName, outputStream);
    }

    @Override
    public boolean existsPolicyFile(final String file) {
        return getDelegate().existsPolicyFile(file);
    }

    @Override
    public RuleSetValidation<PolicyCollection> validatePolicyFile(final String fname) throws IOException {
        return getDelegate().validatePolicyFile(fname);
    }

    @Override
    public List<String> listStoredPolicyFiles() {
        return getDelegate().listStoredPolicyFiles();
    }

    protected ACLFileManager getDelegate() {
        return delegate;
    }
}

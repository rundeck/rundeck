package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection;
import com.dtolabs.rundeck.core.authorization.providers.Validator;
import com.dtolabs.rundeck.core.storage.StorageManager;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Function;

/**
 * Provides ACLFileManager interface with a context argument
 *
 * @param <T>
 */
@Builder
@RequiredArgsConstructor
public class ContextACLStorageFileManager<T>
        implements ContextACLManager<T>
{
    /**
     * Storage manager backend
     */
    private final StorageManager storageManager;
    /**
     * Validator for files
     */
    private final Validator validator;
    /**
     * Mapping from context to storage prefix
     */
    private final Function<T, String> prefixMapping;
    /**
     * Mapping from context to manager for the context
     */
    private final Map<T, ACLFileManager> prefixManagers = Collections.synchronizedMap(new HashMap<>());
    private final List<Function<T, ACLFileManagerListener>>
            listenerMappings =
            Collections.synchronizedList(new ArrayList<>());

    public ACLFileManager forContext(T context) {
        String prefix = prefixMapping.apply(context);
        return prefixManagers.computeIfAbsent(
                context,
                pre -> {
                    ACLStorageFileManager build = ACLStorageFileManager
                            .builder()
                            .validator(validator)
                            .storage(storageManager)
                            .prefix(prefix)
                            .build();
                    //generate any listeners from the mapping for this context
                    synchronized (listenerMappings) {
                        listenerMappings.forEach(f -> {
                            ACLFileManagerListener l = f.apply(pre);
                            if (l != null) {
                                build.addListener(l);
                            }
                        });
                    }
                    return build;
                }
        );
    }

    @Override
    public void addListenerMap(final Function<T, ACLFileManagerListener> mapping) {
        //add listeners for existing managers
        synchronized (prefixManagers) {
            prefixManagers.forEach((k, m) -> m.addListener(mapping.apply(k)));
            listenerMappings.add(mapping);
        }
    }

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
    public List<String> listStoredPolicyPaths(final T context) {
        return forContext(context).listStoredPolicyPaths();
    }

    @Override
    public List<String> listStoredPolicyFiles(final T context) {
        return forContext(context).listStoredPolicyFiles();
    }
}

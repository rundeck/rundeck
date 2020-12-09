package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Function;

/**
 * Base context acl manager, uses {@link #forContext(Object)} to retrieve ACLFilemanager, and apply corresponding method
 * call, maintains created managers in a map, and handles listener mappings for the created managers
 *
 * @param <T>
 */
public abstract class BaseContextACLManager<T>
        implements ContextACLManager<T>
{
    private final List<Function<T, ACLFileManagerListener>>
            listenerMappings =
            Collections.synchronizedList(new ArrayList<>());
    /**
     * Mapping from context to manager for the context
     */
    private final Map<T, ACLFileManager> contextManagers = Collections.synchronizedMap(new HashMap<>());

    /**
     * Applies existing listener mappings to a manager
     *
     * @param ctx        context
     * @param listenable manager
     */
    protected ACLFileManager applyMappings(T ctx, ACLFileManager listenable) {
        synchronized (listenerMappings) {
            listenerMappings.forEach(f -> {
                ACLFileManagerListener l = f.apply(ctx);
                if (l != null) {
                    listenable.addListener(l);
                }
            });
        }
        return listenable;
    }

    /**
     * @return create a manager based on context
     */
    protected abstract ACLFileManager createManager(T context);

    /**
     * Create a manager with applied listener mappings
     * @param context context
     * @return new manager
     */
    private ACLFileManager createWithMappings(T context) {
        return applyMappings(context, createManager(context));
    }

    public ACLFileManager forContext(T context) {
        return contextManagers.computeIfAbsent(context, this::createWithMappings);
    }

    /**
     * adds a mapping from context to listeners, and immediately applies to previously created managers
     *
     * @param mapping
     */
    @Override
    public void addListenerMap(final Function<T, ACLFileManagerListener> mapping) {
        //add listeners for existing managers
        synchronized (contextManagers) {
            contextManagers.forEach((k, m) -> m.addListener(mapping.apply(k)));
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
    public List<String> listStoredPolicyFiles(final T context) {
        return forContext(context).listStoredPolicyFiles();
    }
}

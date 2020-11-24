package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection;
import com.dtolabs.rundeck.core.authorization.providers.Validator;
import com.dtolabs.rundeck.core.storage.StorageManager;
import lombok.Builder;
import lombok.Getter;
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
public class ContextACLStorageFileManager<T> extends BaseContextACLManager<T>
        implements ContextACLManager<T>
{
    /**
     * Storage manager backend
     */
    private final StorageManager storageManager;
    /**
     * Validator for files
     */
    @Getter private final Validator validator;
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

}

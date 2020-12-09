package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.providers.Validator;
import com.dtolabs.rundeck.core.storage.StorageManager;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    @Override
    protected ACLFileManager createManager(final T context) {
        return new ListenableACLFileManager(
                ACLStorageFileManager
                        .builder()
                        .validator(validator)
                        .storage(storageManager)
                        .prefix(prefixMapping.apply(context))
                        .build()
        );
    }
}

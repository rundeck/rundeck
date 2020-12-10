package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.providers.ValidatorFactory;
import com.dtolabs.rundeck.core.storage.StorageManager;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * Provides ACLFileManager interface with a context argument
 *
 * @param <T>
 */
@Builder
@RequiredArgsConstructor
public class ContextACLStorageFileManager
        extends BaseContextACLManager<AppACLContext>
        implements ContextACLManager<AppACLContext>
{
    /**
     * Storage manager backend
     */
    private final StorageManager storageManager;
    private final ValidatorFactory validatorFactory;
    /**
     * Mapping from context to storage prefix
     */
    private final Function<AppACLContext, String> prefixMapping;


    @Override
    protected ACLFileManager createManager(final AppACLContext context) {
        return new ListenableACLFileManager(
                ACLStorageFileManager
                        .builder()
                        .validator(
                                context.isSystem()
                                ? validatorFactory.create()
                                : validatorFactory.forProjectOnly(context.getProject())
                        )
                        .storage(storageManager)
                        .prefix(prefixMapping.apply(context))
                        .build()
        );
    }
}

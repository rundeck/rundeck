package org.rundeck.storage.conf;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;

/**
 * Converts ContentMeta
 */
public interface ContentConverter<T extends ContentMeta> {
    T filterReadData(Path path, T contents);

    T filterCreateData(Path path, T contents);

    T filterUpdateData(Path path, T content);
}

package org.rundeck.storage.conf;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;

/**
 * Converts ContentMeta
 */
public interface ContentConverter<T extends ContentMeta> {
    T convertReadData(Path path, T contents);

    T convertCreateData(Path path, T contents);

    T convertUpdateData(Path path, T content);
}

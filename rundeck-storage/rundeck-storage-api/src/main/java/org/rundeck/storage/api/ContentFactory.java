package org.rundeck.storage.api;

import java.util.Map;

/**
 * ContentFactory can create an appropriate ContentMeta subtype
 *
 * @author greg
 * @since 2014-02-19
 */
public interface ContentFactory<T extends ContentMeta> {
    T create(HasInputStream source, Map<String, String> meta);
}

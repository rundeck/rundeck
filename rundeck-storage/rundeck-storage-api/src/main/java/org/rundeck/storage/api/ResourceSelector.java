package org.rundeck.storage.api;

/**
 * ResourceSelector determines a matching resource
 *
 * @author greg
 * @since 2014-02-19
 */
public interface ResourceSelector<T extends ContentMeta> {
    boolean matchesContent(T content);
}

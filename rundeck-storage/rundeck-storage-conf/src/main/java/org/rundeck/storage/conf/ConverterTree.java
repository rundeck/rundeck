package org.rundeck.storage.conf;

import org.rundeck.storage.impl.DelegateResource;
import org.rundeck.storage.impl.DelegateTree;
import org.rundeck.storage.api.*;

/**
 * Tree that can convert resource content with a {@link ContentConverter}
 */
public class ConverterTree<T extends ContentMeta> extends DelegateTree<T> implements SelectiveTree<T> {
    ContentConverter<T> converter;
    PathSelector pathSelector;
    ResourceSelector<T> resourceSelector;

    /**
     * Convert content from the delegate with the given converter if it matches the selector
     *
     * @param delegate     delegate
     * @param converter    converter
     * @param pathSelector path selection
     */
    public ConverterTree(Tree<T> delegate, ContentConverter<T> converter, PathSelector pathSelector) {
        this(delegate, converter, pathSelector, null);
    }

    public ConverterTree(Tree<T> delegate, ContentConverter<T> converter, ResourceSelector<T> resourceSelector) {
        this(delegate, converter, null, resourceSelector);
    }

    public ConverterTree(Tree<T> delegate, ContentConverter<T> converter, PathSelector pathSelector,
            ResourceSelector<T> resourceSelector) {
        super(delegate);
        this.converter = converter;
        this.pathSelector = pathSelector;
        this.resourceSelector = resourceSelector;
    }

    @Override
    public boolean matchesPath(Path path) {
        return pathSelector.matchesPath(path);
    }

    private T filterReadData(Path path, T contents) {
        return null != converter ? converter.filterReadData(path, contents) : contents;
    }

    private T filterCreateData(Path path, T contents) {
        return null != converter ? converter.filterCreateData(path, contents) : contents;
    }

    private T filterUpdateData(Path path, T content) {
        return null != converter ? converter.filterUpdateData(path, content) : content;
    }

    @Override
    public Resource<T> getResource(Path path) {
        final Resource<T> resource = super.getResource(path);
        if (!resource.isDirectory() && shouldConvert(path, resource.getContents(), pathSelector, resourceSelector)) {
            return filterGetResource(path, resource);
        }
        return resource;
    }

    @Override
    public Resource<T> getPath(Path path) {
        final Resource<T> resource = super.getPath(path);
        if (!resource.isDirectory() && shouldConvert(path, resource.getContents(), pathSelector, resourceSelector)) {
            return filterGetResource(path, resource);
        }
        return resource;
    }

    /**
     * Return true if the path and content match the specified path selector and resource selector
     * @param path path
     * @param content content
     * @param pathSelector1 selector, or null matches all paths
     * @param resourceSelector1 resource selector, or null matches all resources
     * @param <T> type
     * @return true if both match
     */
    static <T extends ContentMeta> boolean shouldConvert(Path path, T content, PathSelector pathSelector1,
            ResourceSelector<T> resourceSelector1) {
        boolean result;
        if (null != pathSelector1) {
            result = pathSelector1.matchesPath(path);
        } else {
            result = true;
        }
        boolean result1;
        if (null != resourceSelector1) {
            result1 = resourceSelector1.matchesContent(content);
        } else {
            result1 = true;
        }
        return result && result1;
    }

    private Resource<T> filterGetResource(Path path, final Resource<T> resource) {
        return new DelegateResource<T>(resource) {
            @Override
            public T getContents() {
                return filterReadData(getPath(), resource.getContents());
            }
        };
    }


    @Override
    public Resource<T> createResource(Path path, T content) {
        if (shouldConvert(path, content, pathSelector, resourceSelector)) {
            return super.createResource(path, filterCreateData(path, content));
        }
        return super.createResource(path, content);
    }

    @Override
    public Resource<T> updateResource(Path path, T content) {
        if (shouldConvert(path, content, pathSelector, resourceSelector)) {
            return super.updateResource(path, filterUpdateData(path, content));
        }
        return super.updateResource(path, content);
    }

}

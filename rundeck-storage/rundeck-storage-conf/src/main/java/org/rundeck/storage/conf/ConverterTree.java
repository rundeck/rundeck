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
        if (shouldConvert(path, resource.getContents())) {
            return filterGetResource(path, resource);
        }
        return resource;
    }

    private boolean shouldConvert(Path path, T content) {
        return matchesPathOr(path, true) && matchesContentOr(content, true);
    }

    private boolean matchesContentOr(T content, boolean b) {
        if (null != resourceSelector) {
            return resourceSelector.matchesContent(content);
        } else {
            return b;
        }
    }

    private boolean matchesPathOr(Path path, boolean or) {
        if (null != pathSelector) {
            return pathSelector.matchesPath(path);
        } else {
            return or;
        }
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
        if (shouldConvert(path, content)) {
            return super.createResource(path, filterCreateData(path, content));
        }
        return super.createResource(path, content);
    }

    @Override
    public Resource<T> updateResource(Path path, T content) {
        if (shouldConvert(path, content)) {
            return super.updateResource(path, filterUpdateData(path, content));
        }
        return super.updateResource(path, content);
    }

}

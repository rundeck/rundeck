package org.rundeck.storage.data.file;

import org.rundeck.storage.impl.ResourceBase;
import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;

/**
* $INTERFACE is ... User: greg Date: 2/18/14 Time: 11:10 AM
*/
final class ContentMetaResource<T extends ContentMeta> extends ResourceBase<T> {
    ContentMetaResource(Path path, T contents, boolean directory) {
        super(path, contents, directory);
    }
}

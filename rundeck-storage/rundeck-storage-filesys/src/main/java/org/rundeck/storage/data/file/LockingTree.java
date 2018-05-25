/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.storage.data.file;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Tree;
import org.rundeck.storage.impl.StringToPathTree;

import java.io.*;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockingTree provides lock objects which can be synchronized for resource access to a particular path.
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-03-28
 */
public abstract class LockingTree<T extends ContentMeta> extends StringToPathTree<T> implements Tree<T> {
    private ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<String,
            Object>(new WeakHashMap<String, Object>());


    /**
     * Return an object that can be synchronized on for the given path.
     *
     * @param path path
     *
     * @return synch object
     */
    protected Object pathSynch(Path path) {
        Object newref = new Object();
        Object oldref = locks.putIfAbsent(path.getPath(), newref);
        return null != oldref ? oldref : newref;
    }

    /**
     * Return a {@link HasInputStream} where all read access to the underlying data is synchronized around the path
     *
     * @param path path
     * @param stream stream
     *
     * @return synchronized stream access
     */
    protected HasInputStream synchStream(final Path path, final HasInputStream stream) {
        return new HasInputStream() {
            @Override
            public InputStream getInputStream() throws IOException {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                writeContent(bytes);
                return new ByteArrayInputStream(bytes.toByteArray());
            }

            @Override
            public long writeContent(OutputStream outputStream) throws IOException {
                synchronized (pathSynch(path)) {
                    return stream.writeContent(outputStream);
                }
            }
        };

    }

}

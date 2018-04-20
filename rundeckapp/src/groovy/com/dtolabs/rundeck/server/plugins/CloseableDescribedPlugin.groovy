/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.Closeables
import com.dtolabs.rundeck.core.plugins.configuration.Description

/**
 * A described plugin which can be closed
 * @author greg
 * @since 3/8/17
 */
class CloseableDescribedPlugin<T> extends DescribedPlugin<T> implements Closeable {
    CloseableProvider<T> closeable;

    CloseableDescribedPlugin(final DescribedPlugin<T> plugin) {
        super(plugin.instance, plugin.description, plugin.name, plugin.file)
        this.closeable = Closeables.closeableProvider(plugin.instance)
    }

    CloseableDescribedPlugin(
            final CloseableProvider<T> closeable,
            final Description description,
            final String name,
            final File file
    )
    {
        super(closeable.provider, description, name, file)
        this.closeable = closeable
    }

    CloseableDescribedPlugin(
            final CloseableProvider<T> closeable,
            final Description description,
            final String name
    )
    {
        super(closeable.provider, description, name)
        this.closeable = closeable
    }

    @Override
    void close() throws IOException {
        closeable?.close()
    }
}

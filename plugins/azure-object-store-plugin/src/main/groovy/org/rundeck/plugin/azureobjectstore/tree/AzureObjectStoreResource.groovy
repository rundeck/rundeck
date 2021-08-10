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
package org.rundeck.plugin.azureobjectstore.tree

import com.dtolabs.rundeck.core.storage.BaseStreamResource
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource


class AzureObjectStoreResource implements Resource<BaseStreamResource> {
    private Path resourcePath
    private BaseStreamResource content
    private boolean isDir

    AzureObjectStoreResource(String resourcePath, BaseStreamResource content) {
        this.resourcePath = PathUtil.asPath(resourcePath)
        this.content = content
    }

    AzureObjectStoreResource(String resourcePath, BaseStreamResource content, boolean isDirectory) {
        this(resourcePath,content)
        this.isDir = isDirectory
    }

    @Override
    BaseStreamResource getContents() {
        return content
    }

    @Override
    boolean isDirectory() {
        return isDir
    }

    @Override
    Path getPath() {
        return resourcePath
    }

}

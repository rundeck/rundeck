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

package com.dtolabs.rundeck.core.storage;

import java.util.Date;
import java.util.Map;

/**
 * Abstract base implementation of {@link ResourceMeta}
 */
abstract class BaseResource implements ResourceMeta {
    Map<String,String> meta;

    protected BaseResource(Map<String, String> meta) {
        this.meta = meta;
    }

    @Override
    public Map<String, String> getMeta() {
        return meta;
    }

    @Override
    public String getContentType() {
        return meta.get(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE);
    }

    @Override
    public long getContentLength() {
        return StorageUtil.parseLong(meta.get(StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH), -1);
    }

    @Override
    public Date getModificationTime() {
        return StorageUtil.parseDate(meta.get(StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME), null);
    }

    @Override
    public Date getCreationTime() {
        return StorageUtil.parseDate(meta.get(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME), null);
    }
}

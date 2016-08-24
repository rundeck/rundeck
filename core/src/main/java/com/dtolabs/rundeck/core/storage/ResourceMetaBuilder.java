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
import java.util.HashMap;
import java.util.Map;

/**
 * Mutable set of rundeck resource metadata.
 */
public class ResourceMetaBuilder implements CanSetResourceMeta, HasResourceMeta {
    Map<String, String> meta;

    public ResourceMetaBuilder(Map<String, String> meta) {
        if (null != meta) {
            this.meta = meta;
        }
    }

    public ResourceMetaBuilder() {
        this(new HashMap<String, String>());
    }

    @Override
    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    @Override
    public void setMeta(String key, String value) {
        meta.put(key, value);
    }

    @Override
    public void setContentType(String value) {
        setMeta(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE, value);
    }

    public String getContentType() {
        return getResourceMeta().get(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE);
    }

    @Override
    public void setContentLength(long length) {
        setMeta(StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH, Long.toString(length));
    }

    public long getContentLength() {
        return Long.parseLong(getResourceMeta().get(StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH));
    }

    @Override
    public void setModificationTime(Date time) {
        setMeta(StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME, StorageUtil.formatDate(time));
    }

    public Date getModificationTime() {
        String s = getResourceMeta().get(StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME);
        if (null != s) {
            return StorageUtil.parseDate(s, null);
        }
        return null;
    }

    @Override
    public void setCreationTime(Date time) {
        setMeta(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME, StorageUtil.formatDate(time));
    }

    public Date getCreationTime() {
        String s = getResourceMeta().get(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME);
        if (null != s) {
            return StorageUtil.parseDate(s, null);
        }
        return null;
    }

    @Override
    public Map<String, String> getResourceMeta() {
        return meta;
    }
}

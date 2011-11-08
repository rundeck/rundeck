/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.common.impl;

import java.io.File;
import java.net.URL;

/**
 * Builder for URLFileUpdater
 */
public class URLFileUpdaterBuilder {
    private URL url;
    private String acceptHeader;
    private int timeout;
    private File cacheMetadataFile;
    private File cachedContent;
    private boolean useCaching;
    private String username;
    private String password;

    public URLFileUpdaterBuilder setUrl(final URL url) {
        this.url = url;
        return this;
    }

    public URLFileUpdaterBuilder setAcceptHeader(final String acceptHeader) {
        this.acceptHeader = acceptHeader;
        return this;
    }

    public URLFileUpdaterBuilder setTimeout(final int timeout) {
        this.timeout = timeout;
        return this;
    }

    public URLFileUpdaterBuilder setCacheMetadataFile(final File cacheMetadataFile) {
        this.cacheMetadataFile = cacheMetadataFile;
        return this;
    }

    public URLFileUpdaterBuilder setCachedContent(final File cachedContent) {
        this.cachedContent = cachedContent;
        return this;
    }

    public URLFileUpdaterBuilder setUseCaching(final boolean useCaching) {
        this.useCaching = useCaching;
        return this;
    }

    public URLFileUpdater createURLFileUpdater() {
        return new URLFileUpdater(url, acceptHeader, timeout, cacheMetadataFile, cachedContent, useCaching, username,
            password);
    }

    public URLFileUpdaterBuilder setUsername(final String username) {
        this.username = username;
        return this;
    }

    public URLFileUpdaterBuilder setPassword(final String password) {
        this.password = password;
        return this;
    }
}
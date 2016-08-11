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

package org.rundeck.storage.data;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.HasInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Basic implementation of ContentMeta
 */
public class DataContent implements ContentMeta {
    InputStream stream;
    HasInputStream lazyStream;
    Map<String, String> meta;

    DataContent(InputStream stream, Map<String, String> meta) {
        this.stream = stream;
        this.meta = meta;
    }

    public DataContent(HasInputStream lazyStream, Map<String, String> meta) {
        this.lazyStream = lazyStream;
        this.meta = meta;
    }

    @Override
    public Map<String, String> getMeta() {
        return meta;
    }


    @Override
    public long writeContent(OutputStream out) throws IOException {
        if (null != stream) {
            return DataUtil.copyStream(stream, out);
        } else if (null != lazyStream) {
            return lazyStream.writeContent(out);
        }
        return -1;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (null != stream) {
            return stream;
        } else if (null != lazyStream) {
            return lazyStream.getInputStream();
        }
        return null;
    }
}

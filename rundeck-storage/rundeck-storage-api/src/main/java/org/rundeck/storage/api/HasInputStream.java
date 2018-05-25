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

package org.rundeck.storage.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * HasInputStream provides lazy loading of an input stream that might cause an exception
 *
 * @author greg
 * @since 2014-02-19
 */
public interface HasInputStream {
    public InputStream getInputStream() throws IOException;

    /**
     * Write the content stream to the output stream
     *
     * @param outputStream output stream
     * @return the content stream
     *
     * @throws IOException on io error
     */
    long writeContent(OutputStream outputStream) throws IOException;
}

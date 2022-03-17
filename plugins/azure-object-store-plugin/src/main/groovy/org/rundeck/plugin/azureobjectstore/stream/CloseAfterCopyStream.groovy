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
package org.rundeck.plugin.azureobjectstore.stream

import com.dtolabs.utils.Streams
import org.rundeck.storage.api.HasInputStream


class CloseAfterCopyStream implements HasInputStream {
    InputStream inputStream

    CloseAfterCopyStream(InputStream inputStream) {
        this.inputStream = inputStream
    }
    @Override
    InputStream getInputStream() throws IOException {
        return inputStream
    }

    @Override
    long writeContent(final OutputStream outputStream) throws IOException {
        long copied = Streams.copyStream(inputStream, outputStream)
        inputStream.close()
        return copied
    }
}

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

package rundeck.services

import com.dtolabs.rundeck.plugins.scm.JobSerializer
import rundeck.codecs.JobsXMLCodec
import rundeck.codecs.JobsYAMLCodec

/**
 * Created by greg on 11/3/16.
 */
class JobFromMapSerializer implements JobSerializer {
    Map<String, String> data

    JobFromMapSerializer(final Map<String, String> data) {
        this.data = data
    }

    @Override
    void serialize(final String format, final OutputStream outputStream) throws IOException {
        String str
        if (format == 'xml') {
            str = JobsXMLCodec.encodeMaps([data])
        } else if (format == 'yaml') {
            str = JobsYAMLCodec.encodeMaps([data])
        } else {
            throw IllegalArgumentException('Unsupported format: ' + format)
        }
        outputStream.write(str.getBytes("UTF-8"))
        outputStream.write('\n'.getBytes("UTF-8"))
    }

    @Override
    void serialize(
            final String format,
            final OutputStream outputStream,
            final boolean preserveUuid,
            final String sourceId
    ) throws IOException
    {
        String str
        if (format == 'xml') {
            str = JobsXMLCodec.encodeMaps([data], preserveUuid, [(data.id): sourceId])
        } else if (format == 'yaml') {
            str = JobsYAMLCodec.encodeMaps([data], preserveUuid, [(data.id): sourceId])
        } else {
            throw IllegalArgumentException('Unsupported format: ' + format)
        }
        outputStream.write(str.getBytes("UTF-8"))
        outputStream.write('\n'.getBytes("UTF-8"))
    }
}

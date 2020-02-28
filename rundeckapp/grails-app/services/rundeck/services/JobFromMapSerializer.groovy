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
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.JobDefinitionException
import org.rundeck.app.components.jobs.JobFormat

/**
 * Serialize a job using its map representation into xml or yaml
 */
class JobFromMapSerializer implements JobSerializer {
    Map<String, String> data

    RundeckJobDefinitionManager jobDefinitionManager

    JobFromMapSerializer(final RundeckJobDefinitionManager jobDefinitionManager1, final Map<String, String> data) {
        this.jobDefinitionManager = jobDefinitionManager1
        this.data = data
    }

    @Override
    void serialize(final String format, final OutputStream outputStream) throws IOException {
        serialize(format, outputStream, true, null)
    }

    @Override
    void serialize(
            final String format,
            final OutputStream outputStream,
            final boolean preserveUuid,
            final String sourceId
    ) throws IOException
    {
        def replaceIds = [(data.id): sourceId]
        Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream,'UTF-8'))
        try{
            //nb: we use the JobFormat directly because the job has already been converted to canonical map data
            jobDefinitionManager.getFormat(format).encode([data], JobFormat.options(preserveUuid, replaceIds,(String)null), writer)
        } catch(JobDefinitionException e) {
            throw new IOException('Failed encoding format: ' + format +": $e",e)
        }
        writer.write('\n')
        writer.flush()
    }
}

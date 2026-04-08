/*
 * Copyright 2026 SimplifyOps, Inc. (http://simplifyops.com)
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

package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.plugins.scm.TemplateJobFileMapper
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

class TemplateJobFileMapperSpec extends Specification {

    @Unroll
    def "reject path traversal attempt: #attackPath"() {
        given:
        def baseDir = Files.createTempDirectory("test").toFile()
        def mapper = new TemplateJobFileMapper('${job.group}/${job.name}.xml', baseDir)
        def job = Mock(JobReference) {
            getGroupPath() >> attackPath
            getJobName() >> "test"
            getProject() >> "proj"
            getId() >> "123"
        }

        when:
        mapper.fileForJob(job)

        then:
        IOException e = thrown()
        e.message.contains("Path traversal detected")

        cleanup:
        baseDir.deleteDir()

        where:
        attackPath << [
            '../../../../../tmp',
            '../../etc',
            '../../../../../../../etc/cron.d',
            'legitimate/../../../etc'
        ]
    }

    @Unroll
    def "allow valid groupPath values: #validPath"() {
        given:
        def baseDir = Files.createTempDirectory("test").toFile()
        def mapper = new TemplateJobFileMapper('${job.group}/${job.name}.xml', baseDir)
        def job = Mock(JobReference) {
            getGroupPath() >> validPath
            getJobName() >> "myjob"
            getProject() >> "proj"
            getId() >> "123"
        }

        when:
        def file = mapper.fileForJob(job)

        then:
        file.canonicalPath.startsWith(baseDir.canonicalPath)

        cleanup:
        baseDir.deleteDir()

        where:
        validPath << ['mygroup', 'group/subgroup', 'team_ops', 'prod-servers', '', null]
    }

    def "reject path equal to parent directory"() {
        given:
        def baseDir = Files.createTempDirectory("test").toFile()
        def mapper = new TemplateJobFileMapper('../${job.name}.xml', baseDir)
        def job = Mock(JobReference) {
            getGroupPath() >> ""
            getJobName() >> "test"
            getProject() >> "proj"
            getId() >> "123"
        }

        when:
        mapper.fileForJob(job)

        then:
        IOException e = thrown()
        e.message.contains("Path traversal detected")

        cleanup:
        baseDir.deleteDir()
    }

    def "accept path in subdirectory"() {
        given:
        def baseDir = Files.createTempDirectory("test").toFile()
        def mapper = new TemplateJobFileMapper('jobs/${job.group}/${job.name}.xml', baseDir)
        def job = Mock(JobReference) {
            getGroupPath() >> "mygroup"
            getJobName() >> "myjob"
            getProject() >> "proj"
            getId() >> "123"
        }

        when:
        def file = mapper.fileForJob(job)

        then:
        file.canonicalPath.startsWith(baseDir.canonicalPath)
        file.canonicalPath.contains("jobs")

        cleanup:
        baseDir.deleteDir()
    }
}

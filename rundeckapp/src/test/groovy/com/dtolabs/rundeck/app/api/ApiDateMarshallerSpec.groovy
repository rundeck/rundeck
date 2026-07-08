/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.app.api

import com.dtolabs.rundeck.app.api.jobs.info.JobInfo
import com.dtolabs.rundeck.app.api.jobs.upload.JobFileInfo
import com.dtolabs.rundeck.app.api.scm.ScmCommit
import com.dtolabs.rundeck.app.api.scm.ScmJobStatus
import com.dtolabs.rundeck.app.api.tokens.Token
import grails.converters.JSON
import grails.converters.XML
import grails.testing.web.GrailsWebUnitTest
import org.rundeck.app.data.model.v1.authtoken.AuthenticationToken
import org.rundeck.app.data.model.v1.authtoken.AuthTokenMode
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

/**
 * RUN-4550: every {@link Date} value exposed by the API (JSON and XML) must be serialized with
 * second-precision W3C/ISO-8601 in UTC (e.g. {@code 2026-03-25T21:16:50Z}), NOT the millisecond
 * precision that Grails 7 / Jackson emits by default (e.g. {@code 2026-03-25T21:16:50.022Z}).
 *
 * These tests cover the DTOs behind every affected endpoint: token list/get/create ({@link Token}),
 * job file info ({@link JobFileInfo}), job scheduling info ({@link JobInfo}, including the
 * {@code List<Date>} field), and SCM commit dates ({@link ScmCommit}, a plain POGO nested in the
 * SCM status/diff responses).
 */
class ApiDateMarshallerSpec extends Specification implements GrailsWebUnitTest {

    /** A Date that carries millisecond precision (…50.022Z) so we can prove the ms are dropped. */
    @Shared
    Date dateWithMillis = Date.from(Instant.parse('2026-03-25T21:16:50.022Z'))
    static final String EXPECTED = '2026-03-25T21:16:50Z'
    static final String UNEXPECTED_MS = '2026-03-25T21:16:50.022Z'

    def setup() {
        def registrar = new ApiMarshallerRegistrar()
        registrar.registerMarshallers()
        registrar.registerApiMarshallers()
    }

    /**
     * Guardrail: the custom Date marshaller must be registered on EVERY versioned converter config,
     * so no API version (present or future) can regress to millisecond precision. Uses a
     * version-agnostic carrier ({@link JobFileInfo} has no {@code @ApiVersion} gating) so it is valid
     * across the whole 1..CURRENT range.
     */
    @Unroll
    def "Date is second-precision for every API version (v#version, #fmt)"() {
        given:
        def info = new JobFileInfo(id: 'f', dateCreated: dateWithMillis, expirationDate: dateWithMillis)

        when:
        def out = fmt == 'json' ? toJson(info, version) : toXml(info, version)

        then:
        out.contains(EXPECTED)
        !out.contains(UNEXPECTED_MS)

        where:
        [version, fmt] << [(1..ApiVersions.API_CURRENT_VERSION).toList(), ['json', 'xml']].combinations()
    }

    private String toJson(Object obj, int version) {
        JSON.use('v' + version)
        (obj as JSON).toString()
    }

    private String toXml(Object obj, int version) {
        XML.use('v' + version)
        (obj as XML).toString()
    }

    private Token sampleToken() {
        AuthenticationToken authToken = Stub(AuthenticationToken) {
            getName() >> 'mytoken'
            getUuid() >> '123uuid'
            getToken() >> 'abc'
            getClearToken() >> 'abc'
            getTokenMode() >> AuthTokenMode.LEGACY
            getCreator() >> 'elf'
            getOwnerName() >> 'bob'
            getAuthRolesSet() >> (['a', 'b'] as Set)
            getExpiration() >> dateWithMillis
        }
        new Token(authToken, true, false)
    }

    @Unroll
    def "Token.expiration is second-precision in #fmt"() {
        given:
        def token = sampleToken()

        when:
        def out = fmt == 'json' ? toJson(token, version) : toXml(token, version)

        then:
        out.contains(EXPECTED)
        !out.contains(UNEXPECTED_MS)

        where:
        fmt    | version
        'json' | ApiVersions.API_CURRENT_VERSION
        'xml'  | ApiVersions.API_CURRENT_VERSION
    }

    @Unroll
    def "JobFileInfo date fields are second-precision in #fmt"() {
        given:
        def info = new JobFileInfo(
                id: 'file-id',
                jobId: 'job-id',
                execId: 1L,
                fileName: 'f.txt',
                dateCreated: dateWithMillis,
                expirationDate: dateWithMillis,
                user: 'bob',
                fileState: 'ok'
        )

        when:
        def out = fmt == 'json' ? toJson(info, version) : toXml(info, version)

        then:
        out.contains(EXPECTED)
        !out.contains(UNEXPECTED_MS)

        where:
        fmt    | version
        'json' | ApiVersions.API_CURRENT_VERSION
        'xml'  | ApiVersions.API_CURRENT_VERSION
    }

    @Unroll
    def "JobInfo nextScheduledExecution and futureScheduledExecutions are second-precision in #fmt"() {
        given:
        def info = new JobInfo(
                id: 'job-id',
                name: 'a job',
                project: 'test',
                nextScheduledExecution: dateWithMillis,
                futureScheduledExecutions: [dateWithMillis, dateWithMillis]
        )

        when:
        def out = fmt == 'json' ? toJson(info, version) : toXml(info, version)

        then:
        out.contains(EXPECTED)
        !out.contains(UNEXPECTED_MS)

        where:
        fmt    | version
        'json' | ApiVersions.API_CURRENT_VERSION
        'xml'  | ApiVersions.API_CURRENT_VERSION
    }

    @Unroll
    def "ScmCommit.date nested in ScmJobStatus is second-precision in #fmt"() {
        given:
        def status = new ScmJobStatus(
                id: 'job-id',
                project: 'test',
                integration: 'import',
                synchState: 'CLEAN',
                commit: new ScmCommit(
                        commitId: 'abc123',
                        message: 'a commit',
                        author: 'bob',
                        date: dateWithMillis
                )
        )

        when:
        def out = fmt == 'json' ? toJson(status, version) : toXml(status, version)

        then:
        out.contains(EXPECTED)
        !out.contains(UNEXPECTED_MS)

        where:
        fmt    | version
        'json' | ApiVersions.API_CURRENT_VERSION
        'xml'  | ApiVersions.API_CURRENT_VERSION
    }
}

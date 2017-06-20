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

package rundeck.controllers

import asset.pipeline.grails.AssetMethodTagLib
import asset.pipeline.grails.AssetProcessorService
import com.dtolabs.rundeck.app.internal.logging.DefaultLogEvent
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import rundeck.Execution
import rundeck.codecs.AnsiColorCodec
import rundeck.codecs.HTMLElementCodec
import rundeck.codecs.URIComponentCodec
import rundeck.services.ApiService
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.LoggingService
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

/**
 * Created by greg on 1/6/16.
 */
@TestFor(ExecutionController)
@Mock([Execution])
@TestMixin(GroovyPageUnitTestMixin)
class ExecutionControllerSpec extends Specification {
    def setup() {
        mockCodec(AnsiColorCodec)
        mockCodec(HTMLElementCodec)
    }
    def "api execution query no project"() {
        setup:
        def query = new ExecutionQuery()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.executionService = Mock(ExecutionService)
        when:
        def result = controller.apiExecutionsQuery(query)
        then:
        1 * controller.apiService.requireVersion(_, _, 5) >> true
        1 * controller.apiService.renderErrorFormat(_, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                        code  : "api.error.parameter.required",
                                                        args  : ['project']]
        )

    }

    def "api execution query, unsupported media type < v14"() {
        setup:
        def query = new ExecutionQuery()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.executionService = Mock(ExecutionService)
        when:
        params.project = 'test'
        request.api_version = 10
        response.format = 'json'
        def result = controller.apiExecutionsQuery(query)
        then:
        1 * controller.apiService.requireVersion(_, _, 5) >> true
        0 * controller.apiService.renderErrorFormat(_, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                        code  : "api.error.parameter.required",
                                                        args  : ['project']]
        )
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, 'test')
        1 * controller.apiService.renderErrorFormat(_, [
                status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                code  : 'api.error.item.unsupported-format',
                args  : ['json']
        ]
        )

    }

    @Unroll()
    def "api execution query, #format format and v14"() {
        setup:
        def query = new ExecutionQuery()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.executionService = Mock(ExecutionService)
        when:
        params.project = 'test'
        request.api_version = 14
        response.format = format
        def result = controller.apiExecutionsQuery(query)
        then:
        1 * controller.apiService.requireVersion(_, _, 5) >> true
        0 * controller.apiService.renderErrorFormat(_, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                        code  : "api.error.parameter.required",
                                                        args  : ['project']]
        )
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, 'test')

        1 * controller.executionService.queryExecutions(query, 0, 20) >> [result: [], total: 1]
        1 * controller.frameworkService.filterAuthorizedProjectExecutionsAll(_, [], [AuthConstants.ACTION_READ]) >> []
        respondJson * controller.executionService.respondExecutionsJson(_, _, [], [total: 1, offset: 0, max: 20])
        respondXml * controller.executionService.respondExecutionsXml(_, _, [], [total: 1, offset: 0, max: 20])

        where:

        format | respondJson | respondXml
        'json' | 1           | 0
        'xml'  | 0           | 1
        'all'  | 0           | 1
    }

    def "api execution query, parse recentFilter param"() {
        setup:
        def query = new ExecutionQuery()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.executionService = Mock(ExecutionService)
        when:
        params.project = 'test'
        request.api_version = 14
        query.recentFilter = '1d'
        def result = controller.apiExecutionsQuery(query)
        then:
        query.doendafterFilter
        !query.doendbeforeFilter
        !query.dostartafterFilter
        !query.dostartbeforeFilter
        null != query.endafterFilter
        null == query.endbeforeFilter
        null == query.startafterFilter
        null == query.startbeforeFilter

        1 * controller.apiService.requireVersion(_, _, 5) >> true

        1 * controller.executionService.queryExecutions(query, 0, 20) >> [result: [], total: 1]
        1 * controller.frameworkService.filterAuthorizedProjectExecutionsAll(_, [], [AuthConstants.ACTION_READ]) >> []
    }
    def "api execution query, parse olderFilter param"() {
        setup:
        def query = new ExecutionQuery()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.executionService = Mock(ExecutionService)
        when:
        params.project = 'test'
        request.api_version = 14
        params.olderFilter = '1d'
        def result = controller.apiExecutionsQuery(query)
        then:
        !query.doendafterFilter
        query.doendbeforeFilter
        !query.dostartafterFilter
        !query.dostartbeforeFilter
        null == query.endafterFilter
        null != query.endbeforeFilter
        null == query.startafterFilter
        null == query.startbeforeFilter

        1 * controller.apiService.requireVersion(_, _, 5) >> true

        1 * controller.executionService.queryExecutions(query, 0, 20) >> [result: [], total: 1]
        1 * controller.frameworkService.filterAuthorizedProjectExecutionsAll(_, [], [AuthConstants.ACTION_READ]) >> []
    }

    class TestReader implements StreamingLogReader {
        List<LogEvent> logs;
        int index = -1;

        @Override
        void openStream(Long offset) throws IOException {
            index = offset;
        }

        @Override
        long getTotalSize() {
            return logs.size()
        }

        @Override
        Date getLastModified() {
            return null
        }

        @Override
        void close() throws IOException {
            index = -1
        }

        @Override
        boolean isComplete() {
            return index > logs.size()
        }

        @Override
        long getOffset() {
            return index
        }

        @Override
        boolean hasNext() {
            return index < logs.size()
        }

        @Override
        LogEvent next() {
            return logs[index++]
        }

        @Override
        void remove() {

        }
    }

    def "render output escapes html"() {
        given:
        def assetTaglib = mockTagLib(AssetMethodTagLib)
        assetTaglib.assetProcessorService = Mock(AssetProcessorService) {
            assetBaseUrl(*_) >> ''
            getAssetPath(*_) >> ''
        }

        Execution e1 = new Execution(
                project: 'test1',
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null
        controller.loggingService = Mock(LoggingService)
        def reader = new ExecutionLogReader(state: ExecutionLogState.AVAILABLE)
        reader.reader = new TestReader(logs:
                                               [
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: new Date(),
                                                               message: message,
                                                               metadata: [:],
                                                               loglevel: LogLevel.NORMAL
                                                       ),
                                               ]
        )
        when:
        params.id = e1.id.toString()
        controller.renderOutput()
        def ostring = response.contentAsString
        then:
        1 * controller.loggingService.getLogReader(e1) >> reader
        ostring.contains(output)


        where:
        message                                            | output
        'a simple message'                                 | 'a simple message'
        'a simple <script>alert("hi");</script> message'           | 'a simple &lt;script&gt;alert(&quot;hi&quot;);&lt;/script&gt; message'
        'ansi sequence \033[31mred\033[0m now normal'      |
                'ansi sequence <span class="ansi-fg-red">red</span><span class="ansi-mode-normal"> now normal</span>'
        '<script>alert("hi");</script> \033[31mred\033[0m' |
                '&lt;script&gt;alert(&quot;hi&quot;);&lt;/script&gt; <span class="ansi-fg-red">red</span><span ' +
                'class="ansi-mode-normal"></span>'
    }
}

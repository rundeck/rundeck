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
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.RundeckLogFormat
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import org.rundeck.core.auth.AuthConstants
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import groovy.mock.interceptor.MockFor
import groovy.xml.MarkupBuilder
import org.grails.plugins.codecs.JSONCodec
import org.rundeck.app.AppConstants
import rundeck.Execution
import rundeck.UtilityTagLib
import rundeck.codecs.AnsiColorCodec
import rundeck.codecs.HTMLElementCodec
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.LoggingService
import rundeck.services.WorkflowService
import rundeck.services.logging.ExecutionLogReader
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import rundeck.services.logging.WorkflowStateFileLoader
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat

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
        mockCodec(JSONCodec)
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

    def "api execution no existing execution"() {
        given:
        messageSource.addMessage("api.error.item.doesnotexist",Locale.ENGLISH,"{0} does not exist: {1}")
        controller.loggingService = Mock(LoggingService)
        controller.configurationService = Mock(ConfigurationService)
        controller.frameworkService = Mock(FrameworkService) {
            authorizeProjectExecutionAny(*_) >> true
            1 * getAuthContextForSubjectAndProject(*_)
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            requireVersion(*_) >> true
            requireExists(*_) >> true
            0 * _(*_)
        }
        when:
        params.id = "-999"
        request.api_version = 21
        response.format = 'json'
        controller.apiExecutionOutput()
        def json = response.json
        then:
        json.error == 'execution does not exist: -999'
        json.id == "-999"
        json.offset == "0"
        json.completed == false

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
        controller.metaClass.checkAllowUnsanitized = { final String project -> false }
        controller.loggingService = Mock(LoggingService)
        controller.configurationService = Mock(ConfigurationService)
        def reader = new ExecutionLogReader(state: ExecutionFileState.AVAILABLE)
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

            controller.frameworkService = Mock(FrameworkService){
                1 * getAuthContextForSubjectAndProject(_, 'test1') >> Mock(UserAndRolesAuthContext)
                1 * authorizeProjectExecutionAny(_, !null, [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW]) >> true
            }
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

    def "render output does not escape html with meta 'no-strip'"() {
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
        controller.metaClass.checkAllowUnsanitized = { final String project -> true }
        controller.metaClass.convertContentDataType = { final Object input, final String inputDataType, Map<String,String> meta, final String outputType, String projectName -> message }
        controller.loggingService = Mock(LoggingService)
        controller.configurationService = Mock(ConfigurationService) {
            getBoolean('gui.execution.logs.renderConvertedContent',true) >> true
        }
        def reader = new ExecutionLogReader(state: ExecutionFileState.AVAILABLE)
        reader.reader = new TestReader(logs:
                                               [
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: new Date(),
                                                               message: message,
                                                               metadata: ["content-meta:no-strip":"true","content-data-type":"text/html"],
                                                               loglevel: LogLevel.NORMAL
                                                       ),
                                               ]
        )

            controller.frameworkService = Mock(FrameworkService){
                1 * getAuthContextForSubjectAndProject(_, 'test1') >> Mock(UserAndRolesAuthContext)
                1 * authorizeProjectExecutionAny(_, !null, [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW]) >> true
            }
        when:
        params.id = e1.id.toString()
        params.convertContent = "true"
        controller.renderOutput()
        def ostring = response.contentAsString
        then:
        1 * controller.loggingService.getLogReader(e1) >> reader
        ostring.contains(output)


        where:
        message                                            | output
        'a simple message'                                 | 'a simple message'
        'a simple <script>alert("hi");</script> message'   | 'a simple <script>alert("hi");</script> message'
        '<style>.mystyle { font-weight: bold; }</style><div class="mystyle">Test</div>'   | '<style>.mystyle { font-weight: bold; }</style><div class="mystyle">Test</div>'
    }

    def "tail exec output maxlines param"() {
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
            controller.configurationService = Mock(ConfigurationService)
            controller.apiService = Mock(ApiService)
            controller.frameworkService = Mock(FrameworkService)
            def reader = new ExecutionLogReader(state: ExecutionFileState.AVAILABLE)
            reader.reader = new TestReader(
                logs:
                    [
                        new DefaultLogEvent(
                            eventType: LogUtil.EVENT_TYPE_LOG,
                            datetime: new Date(),
                            message: 'message1',
                            metadata: [:],
                            loglevel: LogLevel.NORMAL
                        ),
                        new DefaultLogEvent(
                            eventType: LogUtil.EVENT_TYPE_LOG,
                            datetime: new Date(),
                            message: 'message2',
                            metadata: [:],
                            loglevel: LogLevel.NORMAL
                        ),
                        new DefaultLogEvent(
                            eventType: LogUtil.EVENT_TYPE_LOG,
                            datetime: new Date(),
                            message: 'message3',
                            metadata: [:],
                            loglevel: LogLevel.NORMAL
                        ),
                    ]
            )
        when:
            params.id = e1.id.toString()
            params.maxlines = maxlines
            request.addHeader('accept', 'text/plain')
            controller.tailExecutionOutput()
        then:
            rescount == response.text.readLines().size()

            1 * controller.apiService.requireExists(_, e1, _) >> true
            1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, _)
            1 * controller.frameworkService.authorizeProjectExecutionAny(*_) >> true
            1 * controller.loggingService.getLogReader(e1) >> reader

        where:
            maxlines | rescount
            '0'      | 3
            '1'      | 1
            '2'      | 2
            '3'      | 3
            '-1'     | 3
            'asdf'   | 3
    }
    def "tail exec output 0 totsize should have 0 percentLoaded"() {
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
            controller.configurationService = Mock(ConfigurationService)
            controller.apiService = Mock(ApiService)
            controller.frameworkService = Mock(FrameworkService)
            def reader = new ExecutionLogReader(state: ExecutionFileState.AVAILABLE)
            reader.reader = new TestReader(logs: [])
        when:
            params.id = e1.id.toString()
            params.maxlines = '500'
            request.addHeader('accept', 'text/json')
            controller.tailExecutionOutput()
        then:
            def result = response.json
            result.percentLoaded==0.0

            1 * controller.apiService.requireExists(_, e1, _) >> true
            1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, _)
            1 * controller.frameworkService.authorizeProjectExecutionAny(*_) >> true
            1 * controller.loggingService.getLogReader(e1) >> reader

    }

    /**
     * compacted=true, the log entries returned will include only the changed
     * attributes, and if only the "log" is changed, will produce only a string instead of a map.
     * an empty map means the same entries as previously, an null map entry means remove the previous
     * value.
     * @return
     */
    def "api execution output compacted json"() {
        given:

        def assetTaglib = mockTagLib(UtilityTagLib)
        Execution e1 = new Execution(
                project: 'test1',
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null
        controller.loggingService = Mock(LoggingService)
        controller.configurationService = Mock(ConfigurationService)
        controller.frameworkService = Mock(FrameworkService) {
            authorizeProjectExecutionAny(*_) >> true
            1 * getAuthContextForSubjectAndProject(*_)
            1 * isClusterModeEnabled()
            _ * getServerUUID()
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            requireVersion(*_) >> true
            requireExists(*_) >> true
            0 * _(*_)
        }
        def reader = new ExecutionLogReader(state: ExecutionFileState.AVAILABLE)
        def date1 = new Date(90000000)
        def sdf=new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ssXXX')
        sdf.timeZone=TimeZone.getTimeZone('GMT')
        def abstime=sdf.format(date1)
        def sdf2=new SimpleDateFormat('HH:mm:ss')
//        sdf2.timeZone=TimeZone.getTimeZone('GMT')
        def timestr=sdf2.format(date1)
        reader.reader = new TestReader(logs:
                                               [
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: date1,
                                                               message: 'message1',
                                                               metadata: [:],
                                                               loglevel: LogLevel.NORMAL
                                                       ),
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: date1,
                                                               message: 'message2',
                                                               metadata: [:],
                                                               loglevel: LogLevel.NORMAL
                                                       ),
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: date1,
                                                               message: 'message2',
                                                               metadata: [:],
                                                               loglevel: LogLevel.NORMAL
                                                       ),
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: date1,
                                                               message: 'message3',
                                                               metadata: [stepctx: '1'],
                                                               loglevel: LogLevel.DEBUG
                                                       ),
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: date1,
                                                               message: 'message4',
                                                               metadata: [:],
                                                               loglevel: LogLevel.DEBUG
                                                       ),
                                               ]
        )
        when:
        params.id = e1.id.toString()
        params.compacted = 'true'
        request.api_version = 21
        response.format = 'json'
        controller.apiExecutionOutput()
        def json = response.json
        then:
        1 * controller.loggingService.getLogReader(e1) >> reader
        json.compacted == true
        json.compactedAttr == 'log'
        json.entries.size() == 5
        json.entries[0] == [
                absolute_time: abstime,
                log          : 'message1',
                level        : 'NORMAL',
                time         : timestr,
        ]
        json.entries[1] == 'message2'
        json.entries[2] == [:]
        json.entries[3] == [log: 'message3', stepctx: '1', level: 'DEBUG']
        json.entries[4] == [log: 'message4', stepctx: null]

    }
    /**
     * compacted=true, the log entries returned will include only the changed
     * attributes, and if only the "log" is changed, will produce only a string instead of a map.
     * an empty map means the same entries as previously, an null map entry means remove the previous
     * value.
     * @return
     */
    def "api execution output compacted xml"() {
        given:

        def assetTaglib = mockTagLib(UtilityTagLib)
        Execution e1 = new Execution(
                project: 'test1',
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null
        controller.loggingService = Mock(LoggingService)
        controller.configurationService = Mock(ConfigurationService)
        controller.frameworkService = Mock(FrameworkService) {
            authorizeProjectExecutionAny(*_) >> true
            1 * getAuthContextForSubjectAndProject(*_)
            1 * isClusterModeEnabled()
            _ * getServerUUID()
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            requireVersion(*_) >> true
            requireExists(*_) >> true
            renderSuccessXml(_, _, _) >> { args ->
                def writer = new StringWriter()
                def xml = new MarkupBuilder(writer)
                def response = args[1]
                def recall = args[2]
                xml.with {
                    recall.delegate = delegate
                    recall.resolveStrategy = Closure.DELEGATE_FIRST
                    recall()
                }
                def xmlstr = writer.toString()
                response.setContentType('application/xml')
                response.setCharacterEncoding('UTF-8')
                def out = response.outputStream
                out << xmlstr
                out.flush()
            }
            0 * _(*_)
        }
        def reader = new ExecutionLogReader(state: ExecutionFileState.AVAILABLE)
        def date1 = new Date(90000000)
        def sdf=new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ssXXX')
        sdf.timeZone=TimeZone.getTimeZone('GMT')
        def abstime=sdf.format(date1)
        def sdf2=new SimpleDateFormat('HH:mm:ss')
//        sdf2.timeZone=TimeZone.getTimeZone('GMT')
        def timestr=sdf2.format(date1)
        reader.reader = new TestReader(logs:
                                               [
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: date1,
                                                               message: 'message1',
                                                               metadata: [:],
                                                               loglevel: LogLevel.NORMAL
                                                       ),
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: date1,
                                                               message: 'message2',
                                                               metadata: [:],
                                                               loglevel: LogLevel.NORMAL
                                                       ),
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: date1,
                                                               message: 'message2',
                                                               metadata: [:],
                                                               loglevel: LogLevel.NORMAL
                                                       ),
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: date1,
                                                               message: 'message3',
                                                               metadata: [stepctx: '1'],
                                                               loglevel: LogLevel.DEBUG
                                                       ),
                                                       new DefaultLogEvent(
                                                               eventType: LogUtil.EVENT_TYPE_LOG,
                                                               datetime: date1,
                                                               message: 'message4',
                                                               metadata: [:],
                                                               loglevel: LogLevel.DEBUG
                                                       ),
                                               ]
        )
        when:
        params.id = e1.id.toString()
        params.compacted = 'true'
        request.api_version = 21
        response.format = 'xml'
        controller.apiExecutionOutput()
        def xml = response.xml
        then:
        1 * controller.loggingService.getLogReader(e1) >> reader
        xml != null
        xml.compacted.text() == 'true'
        xml.entries.entry.size() == 5

        xml.entries.entry[0]."@absolute_time".text() == abstime
        xml.entries.entry[0]."@log".text() == 'message1'
        xml.entries.entry[0]."@level".text() == 'NORMAL'
        xml.entries.entry[0]."@time".text() == timestr
        xml.entries.entry[0]."@time".size() == 1
        xml.entries.entry[0]."@node".size() == 0
        xml.entries.entry[0]."@stepctx".size() == 0
        xml.entries.entry[0]."@removed".size() == 0

        xml.entries.entry[1]."@absolute_time".size() == 0
        xml.entries.entry[1]."@log".text() == 'message2'
        xml.entries.entry[1]."@level".size() == 0
        xml.entries.entry[1]."@time".size() == 0
        xml.entries.entry[1]."@node".size() == 0
        xml.entries.entry[1]."@stepctx".size() == 0
        xml.entries.entry[0]."@removed".size() == 0

        xml.entries.entry[2]."@absolute_time".size() == 0
        xml.entries.entry[2]."@log".size() == 0
        xml.entries.entry[2]."@level".size() == 0
        xml.entries.entry[2]."@time".size() == 0
        xml.entries.entry[2]."@node".size() == 0
        xml.entries.entry[2]."@stepctx".size() == 0
        xml.entries.entry[0]."@removed".size() == 0


        xml.entries.entry[3]."@absolute_time"
        xml.entries.entry[3]."@log".text() == 'message3'
        xml.entries.entry[3]."@level".text() == 'DEBUG'
        xml.entries.entry[3]."@time".size() == 0
        xml.entries.entry[3]."@node".size() == 0
        xml.entries.entry[3]."@stepctx".text() == '1'
        xml.entries.entry[3]."@removed".size() == 0

        xml.entries.entry[4]."@absolute_time".size() == 0
        xml.entries.entry[4]."@log".text() == 'message4'
        xml.entries.entry[4]."@level".size() == 0
        xml.entries.entry[4]."@time".size() == 0
        xml.entries.entry[4]."@node".size() == 0
        xml.entries.entry[4]."@stepctx".size() == 0
        xml.entries.entry[4]."@removed".text() == 'stepctx'

    }

    def "api execution log reader not found"() {
        given:
        messageSource.addMessage("execution.log.storage.state.NOT_FOUND",Locale.ENGLISH,"The Execution Log could not be found.")
        Execution e1 = new Execution(
                project: 'test1',
                user: 'bob',
                dateStarted: new Date(),
                status: 'running'

        )
        e1.save() != null
        def reader = new ExecutionLogReader(state: ExecutionFileState.NOT_FOUND)
        controller.loggingService = Mock(LoggingService)
        controller.configurationService = Mock(ConfigurationService)
        controller.frameworkService = Mock(FrameworkService) {
            authorizeProjectExecutionAny(*_) >> true
            1 * getAuthContextForSubjectAndProject(*_)
            1 * isClusterModeEnabled()
            _ * getServerUUID()
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            requireVersion(*_) >> true
            requireExists(*_) >> true
            0 * _(*_)
        }
        when:
        params.id = e1.id.toString()
        request.api_version = 21
        response.format = 'json'
        controller.apiExecutionOutput()
        def json = response.json
        then:
        1 * controller.loggingService.getLogReader(e1) >> reader
        json.empty == true
        json.id == e1.id.toString()
        json.offset == "0"
        json.completed == false
        json.execCompleted == false
        json.hasFailedNodes == false
        json.execState == "running"
        json.message == "The Execution Log could not be found."

    }

    def "api execution log reader pending state"() {
        given:
        messageSource.addMessage("execution.log.storage.state.PENDING_REMOTE",Locale.ENGLISH,"Waiting for log output to become available...")
        Execution e1 = new Execution(
                project: 'test1',
                user: 'bob',
                dateStarted: new Date(),
                status: 'running'

        )
        e1.save() != null
        def reader = new ExecutionLogReader(state: ExecutionFileState.PENDING_REMOTE)
        controller.loggingService = Mock(LoggingService)
        controller.configurationService = Mock(ConfigurationService)
        controller.frameworkService = Mock(FrameworkService) {
            authorizeProjectExecutionAny(*_) >> true
            1 * getAuthContextForSubjectAndProject(*_)
            1 * isClusterModeEnabled()
            _ * getServerUUID()
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            requireVersion(*_) >> true
            requireExists(*_) >> true
            0 * _(*_)
        }
        when:
        params.id = e1.id.toString()
        request.api_version = 21
        response.format = 'json'
        controller.apiExecutionOutput()
        def json = response.json
        then:
        1 * controller.loggingService.getLogReader(e1) >> reader
        json.id == e1.id.toString()
        json.offset == "0"
        json.completed == false
        json.execCompleted == false
        json.hasFailedNodes == false
        json.execState == "running"
        json.message == "Pending"
        json.pending == "Waiting for log output to become available..."

    }

    @Unroll
    def "ajax exec node state with compression"() {
        given:
        Execution e1 = new Execution(
            project: 'test1',
            user: 'bob',
            dateStarted: new Date(),
            status: 'running'

        )
        e1.save() != null
        controller.frameworkService = Mock(FrameworkService)
        controller.workflowService = Mock(WorkflowService)

        when:
        if (acceptHeader) {
            request.addHeader('Accept-Encoding', acceptHeader)
        }
        request.addHeader('x-rundeck-ajax', 'true')
        params.id = e1.id.toString()
        params.node = 'anode'
        def result = controller.ajaxExecNodeState()

        then:
        response.header('Content-Encoding') == resultHeader
        controller.frameworkService.authorizeProjectExecutionAny(_, _, _) >> true
        controller.workflowService.requestStateSummary(_, _, _) >> new WorkflowStateFileLoader(
                state: ExecutionFileState.AVAILABLE,
                workflowState: [nodeSummaries: [anode: 'summaries'], nodeSteps: [anode: 'steps']]
        )

        where:
        acceptHeader | resultHeader
        'gzip'       | 'gzip'
        null         | null
    }

    @Unroll
    def "checkAllowUnsanitized"() {

        when:
        def prjCfg = Mock(IRundeckProjectConfig) {
            hasProperty(AppConstants.PROJECT_OUTPUT_ALLOW_UNSANITIZED) >> projectHasProp
            getProperty(AppConstants.PROJECT_OUTPUT_ALLOW_UNSANITIZED) >> project
        }
        def prjMgr = Mock(ProjectManager) {
            loadProjectConfig("proj1") >> prjCfg
        }
        def fwk = Mock(Framework) {
            hasProperty(AppConstants.FRAMEWORK_OUTPUT_ALLOW_UNSANITIZED) >> frameworkHasProp
            getProperty(AppConstants.FRAMEWORK_OUTPUT_ALLOW_UNSANITIZED) >> framework
            getProjectManager() >> prjMgr
        }
        controller.frameworkService = Mock(FrameworkService) {
            getRundeckFramework() >> fwk
        }
        boolean val = controller.checkAllowUnsanitized("proj1")

        then:
        val == expected

        where:
        frameworkHasProp | framework   | projectHasProp | project | expected
            false        | null        |  true          | "true"  | false
            true         | "true"      |  true          | "true"  | true
            true         | "false"     |  true          | "true"  | false
            true         | "false"     |  true          | "false" | false
            true         | "true"      |  true          | "false" | false
            true         | "true"      |  false         | null    | false

    }

    void "downloadOutput with no entries in log"(){

        setup:
        File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1))
        fos << """^text/x-rundeck-log-v2.0^
^2019-07-09T12:56:35Z|stepbegin||{node=server-node|step=1|stepctx=1|user=admin}|^
^2019-07-09T12:56:35Z|nodebegin||{node=remote-node|step=1|stepctx=1|user=remote}|^
^2019-07-09T12:56:45Z|nodeend||{node=remote-node|step=1|stepctx=1|user=remote}|^
^2019-07-09T12:56:45Z|stepend||{node=server-node|step=1|stepctx=1|user=admin}|^
^END^"""
        fos.close()

        Execution e1 = new Execution(outputfilepath: tf1.absolutePath,project:'test1',user:'bob',dateStarted: new Date())
        e1.save()
        controller.loggingService = Mock(LoggingService) {
            getLogReader(_) >> new ExecutionLogReader(state: ExecutionFileState.AVAILABLE, reader: new FSStreamingLogReader(tf1, "UTF-8", new RundeckLogFormat()))
        }
        controller.frameworkService = Mock(FrameworkService) {
            getFrameworkPropertyResolver(_,_) >> null
            1 * getAuthContextForSubjectAndProject(_, 'test1') >> Mock(UserAndRolesAuthContext)
            1 * authorizeProjectExecutionAny(_, !null, [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW]) >> true
        }
        when:
        params.id = e1.id.toString()
        params.formatted = 'true'
        params.timeZone = 'GMT'

        controller.downloadOutput()

        then:
        response.text == "No output"
    }

    @Unroll
    def "endpoint #endpoint requires authorization"() {
        given:
            Execution e1 = new Execution(
                project: 'test1',
                user: 'bob',
                dateStarted: new Date(),
                status: 'running'
            )
            e1.save() != null
            controller.frameworkService = Mock(FrameworkService)
            params.id = e1.id.toString()
        when:
            controller."$endpoint"()
        then:
            1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, 'test1') >>
            Mock(UserAndRolesAuthContext)
            1 * controller.
                frameworkService.
                authorizeProjectExecutionAny(_, !null, [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW]) >> false
            response.status == 403
        where:
            endpoint << ["mail", "downloadOutput", "renderOutput"]
    }
}

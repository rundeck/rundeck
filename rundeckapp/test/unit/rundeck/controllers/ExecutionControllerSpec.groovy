package rundeck.controllers

import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.mixin.TestFor
import rundeck.services.ApiService
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

/**
 * Created by greg on 1/6/16.
 */
@TestFor(ExecutionController)
class ExecutionControllerSpec extends Specification {
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
}

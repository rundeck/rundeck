package rundeck.controllers

import com.dtolabs.rundeck.app.api.authorizations.ActionAuthorization
import com.dtolabs.rundeck.app.api.authorizations.ApplicationAuthorizationContext
import com.dtolabs.rundeck.app.api.authorizations.KindResource
import com.dtolabs.rundeck.app.api.authorizations.AuthorizationsResponse
import com.dtolabs.rundeck.app.api.authorizations.ProjectAuthorizationContext
import com.dtolabs.rundeck.app.api.authorizations.TypeResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.Decision
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.MissingParameter
import rundeck.ScheduledExecution
import rundeck.error_handling.InvalidParameterException
import rundeck.services.ScheduledExecutionService

@Controller
class AuthorizationsController extends ControllerBase {

    static final def APPLICATION_CONTEXT_ALLOWED_TYPES =  [AuthConstants.TYPE_PROJECT, AuthConstants.TYPE_PROJECT_ACL]
    static final def PROJECT_CONTEXT_ALLOWED_TYPES =  [AuthConstants.TYPE_NODE, AuthConstants.TYPE_STORAGE]

    ScheduledExecutionService scheduledExecutionService

    @Get(uri='/authorizations/application/{kind}', produces = io.micronaut.http.MediaType.APPLICATION_JSON)
    @Operation(
            method='GET',
            summary = "Check authorization for application resource kind",
            description='''Evaluates whether the current user has authorization to perform specific actions on resources of a given kind within the application context.

This endpoint checks permissions for a resource type (kind) such as "job", "project", "system", etc., and returns authorization results for each requested action. The evaluation is performed using the application's access control policies and the authenticated user's permissions.

The `actions` parameter must be provided as a query parameter and can be specified multiple times for multiple actions.

Example: `GET /api/56/authorizations/application/job?actions=create&actions=read`

Useful for UI components that need to conditionally display features or for applications that need to verify permissions before attempting operations.

**INCUBATING**: This endpoint is in "incubating" status, and may change.''',
            tags=['Authorization'],
            parameters = [
                    @Parameter(name = 'kind', description = 'Resource Kind', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'actions', description = 'Actions to check authorization for (can be specified multiple times)', required = true, in = ParameterIn.QUERY, array = @ArraySchema (schema = @Schema(type = 'string')))
            ])
    @ApiResponse(
            responseCode='200',
            description='Action authorizations',
            content = @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema=@Schema(implementation=AuthorizationsResponse)
            )
    )
    @ApiResponse(
            responseCode='400',
            description='Bad request - missing required parameter `actions`'
    )
    def appContextAuthorizationsForResourceKind() {
        def kindParams = validateAndGetParamsForKind(params)

        def resource = [type: 'resource', 'kind': kindParams.kind]

        def authContext = rundeckAuthContextProcessor.getAuthContextForSubject(request.subject)
        def decisions = authContext.evaluate(Set.of(resource), kindParams.actions, AuthorizationUtil.RUNDECK_APP_ENV)

        def result = new AuthorizationsResponse(
                authorizationContext: new ApplicationAuthorizationContext(),
                resource: new KindResource(kind: kindParams.kind),
                actionAuthorizations: decisions.collect { Decision decision ->
                    new ActionAuthorization(actionName: decision.action, isAuthorized: decision.isAuthorized())
                } as Set<ActionAuthorization>
        )

        respond(result, formats: ['json'])
    }

    @Get(uri='/authorizations/application/{type}/{specifier}', produces = io.micronaut.http.MediaType.APPLICATION_JSON)
    @Operation(
            method='GET',
            summary = "Get authorizations for an application type with specifier",
            description='''Get authorizations for the supplied set of actions for the subject executing the API call. 
Evaluation is made in the context of the application for the supplied type and specifier.

The `actions` parameter must be provided as a query parameter and can be specified multiple times for multiple actions.

Example: `GET /api/56/authorizations/application/project/myproject?actions=read&actions=delete`

**INCUBATING**: This endpoint is in "incubating" status, and may change.''',
            tags=['Authorization'],
            parameters = [
                    @Parameter(name = 'type', description = 'Resource Type', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'specifier', description = 'Resource specifier', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'actions', description = 'Actions to check authorization for (can be specified multiple times)', required = true, in = ParameterIn.QUERY, array = @ArraySchema (schema = @Schema(type = 'string')))
            ])
    @ApiResponse(
            responseCode='200',
            description='Action authorizations',
            content = @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema=@Schema(implementation=AuthorizationsResponse)
            )
    )
    @ApiResponse(
            responseCode='400',
            description='Bad request - missing required parameter `actions`'
    )
    def appContextAuthorizationsForTypeWithSpecifier() {
        def typeParams = validateAndGetParamsForTypeWithSpecifier(params, APPLICATION_CONTEXT_ALLOWED_TYPES)

        def resource = [type: typeParams.type, name: typeParams.specifier]

        def authContext = rundeckAuthContextProcessor.getAuthContextForSubject(request.subject)
        def decisions = authContext.evaluate(Set.of(resource), typeParams.actions, AuthorizationUtil.RUNDECK_APP_ENV)

        def result = new AuthorizationsResponse(
                authorizationContext: new ApplicationAuthorizationContext(),
                resource: new TypeResource(type: typeParams.type, specifier: typeParams.specifier),
                actionAuthorizations: decisions.collect { Decision decision ->
                    new ActionAuthorization(actionName: decision.action, isAuthorized: decision.isAuthorized())
                } as Set<ActionAuthorization>
        )

        respond(result, formats: ['json'])
    }

    @Get(uri='/authorizations/project/{project}/{kind}', produces = io.micronaut.http.MediaType.APPLICATION_JSON)
    @Operation(
            method='GET',
            summary = "Get authorizations for a project resource kind",
            description='''Get authorizations for the supplied set of actions for the subject executing the API call. 
Evaluation is made in the context of the project for the supplied resource kind.

The `actions` parameter must be provided as a query parameter and can be specified multiple times for multiple actions.

Example: `GET /api/56/authorizations/project/myproject/node?actions=read&actions=run`

**INCUBATING**: This endpoint is in "incubating" status, and may change.''',
            tags=['Authorization'],
            parameters = [
                    @Parameter(name = 'project', description = 'Project Name', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'kind', description = 'Resource Kind', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'actions', description = 'Actions to check authorization for (can be specified multiple times)', required = true, in = ParameterIn.QUERY, array = @ArraySchema (schema = @Schema(type = 'string')))
            ])
    @ApiResponse(
            responseCode='200',
            description='Action authorizations',
            content = @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema=@Schema(implementation=AuthorizationsResponse)
            )
    )
    @ApiResponse(
            responseCode='400',
            description='Bad request - missing required parameter `actions`'
    )
    def projectContextAuthorizationsForResourceKind() {
        def kindParams = validateAndGetParamsForKind(params)

        def resource = [type: 'resource', project: kindParams.projectName, kind: kindParams.kind]

        def authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(request.subject, kindParams.projectName)
        def decisions = authContext.evaluate(Set.of(resource), kindParams.actions, AuthorizationUtil.projectContext(kindParams.projectName))

        def result = new AuthorizationsResponse(
                authorizationContext: new ProjectAuthorizationContext(name: kindParams.projectName),
                resource: new KindResource(kind: kindParams.kind),
                actionAuthorizations: decisions.collect { Decision decision ->
                    new ActionAuthorization(actionName: decision.action, isAuthorized: decision.isAuthorized())
                } as Set<ActionAuthorization>
        )

        respond(result, formats: ['json'])
    }

    @Get(uri='/authorizations/project/{project}/job/{specifier}', produces = io.micronaut.http.MediaType.APPLICATION_JSON)
    @Operation(
            method='GET',
            summary = "Get authorizations for a job",
            description='''Get authorizations for the supplied set of actions for the subject executing the API call. 
Evaluation is made in the context of the project for the supplied job.

**INCUBATING**: This endpoint is in "incubating" status, and may change.''',
            tags=['Authorization'],
            parameters = [
                    @Parameter(name = 'project', description = 'Project Name', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'specifier', description = 'Job Id', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'actions', description = 'Actions to check authorization for', required = true, in = ParameterIn.QUERY, array = @ArraySchema (schema = @Schema(type = 'string')))
            ])
    @ApiResponse(
            responseCode='200',
            description='Action authorizations',
            content = @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema=@Schema(implementation=AuthorizationsResponse)
            )
    )
    def projectContextAuthorizationsForJob() {

        // The `job` authorizations call is a slightly specialized `type` authorizations call, thus reuse existing validation with minor tweaks
        params.put('type', AuthConstants.TYPE_JOB)
        def typeParams = validateAndGetParamsForTypeWithSpecifier(params, [AuthConstants.TYPE_JOB] )

        // Get and validate the job
        final ScheduledExecution job = Optional.ofNullable(scheduledExecutionService.getByIDorUUID(typeParams.specifier))
                .orElseThrow { new InvalidParameterException(paramName: 'specifier', suppliedValue: typeParams.specifier, errorDescription : "It must be a valid job id in the project") }
        if (job.project != typeParams.projectName) {
            throw new InvalidParameterException(paramName: 'specifier', suppliedValue: typeParams.specifier, errorDescription : "It must be a valid job id in the project")
        }

        def resource = rundeckAuthContextProcessor.authResourceForJob(job)

        def authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(request.subject, typeParams.projectName)
        def decisions = authContext.evaluate(Set.of(resource), typeParams.actions, AuthorizationUtil.projectContext(typeParams.projectName))

        def result = new AuthorizationsResponse(
                authorizationContext: new ProjectAuthorizationContext(name: typeParams.projectName),
                resource: new TypeResource(type: typeParams.type, specifier: typeParams.specifier),
                actionAuthorizations: decisions.collect { Decision decision ->
                    new ActionAuthorization(actionName: decision.action, isAuthorized: decision.isAuthorized())
                } as Set<ActionAuthorization>
        )

        respond(result, formats: ['json'])
    }

    @Get(uri='/authorizations/project/{project}/{type}/{specifier}', produces = io.micronaut.http.MediaType.APPLICATION_JSON)
    @Operation(
            method='GET',
            summary = "Get authorizations for a type with specifier",
            description='''Get authorizations for the supplied set of actions for the subject executing the API call. 
Evaluation is made in the context of the project for the type with a specifier.

The `actions` parameter must be provided as a query parameter and can be specified multiple times for multiple actions.

Example: `GET /api/56/authorizations/project/myproject/node/node1?actions=read&actions=run`

**INCUBATING**: This endpoint is in "incubating" status, and may change.''',
            tags=['Authorization'],
            parameters = [
                    @Parameter(name = 'project', description = 'Project Name', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'type', description = 'Resource Type', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'specifier', description = 'Resource specifier', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'actions', description = 'Actions to check authorization for (can be specified multiple times)', required = true, in = ParameterIn.QUERY, array = @ArraySchema (schema = @Schema(type = 'string')))
            ])
    @ApiResponse(
            responseCode='200',
            description='Action authorizations',
            content = @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema=@Schema(implementation=AuthorizationsResponse)
            )
    )
    @ApiResponse(
            responseCode='400',
            description='Bad request - missing required parameter `actions`'
    )
    def projectContextAuthorizationsForTypeWithSpecifier() {
        def typeParams = validateAndGetParamsForTypeWithSpecifier(params, PROJECT_CONTEXT_ALLOWED_TYPES)
        def resource = [type: typeParams.type, project: typeParams.projectName, name: typeParams.specifier]
        def authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(request.subject, typeParams.projectName)
        def decisions = authContext.evaluate(Set.of(resource), typeParams.actions, AuthorizationUtil.projectContext(typeParams.projectName))

        def result = new AuthorizationsResponse(
                authorizationContext: new ProjectAuthorizationContext(name: typeParams.projectName),
                resource: new TypeResource(type: typeParams.type, specifier: typeParams.specifier),
                actionAuthorizations: decisions.collect { Decision decision ->
                    new ActionAuthorization(actionName: decision.action, isAuthorized: decision.isAuthorized())
                } as Set<ActionAuthorization>
        )

        respond(result, formats: ['json'])
    }

    private static Set<String> getActionsFromQueryParams(def params) {
        Optional.ofNullable(params.actions).orElseThrow { new MissingParameter("actions") }

        final def actionsRawVal = params.actions
        Set<String> actions = []

        actionsRawVal instanceof String ? actions.add(actionsRawVal) : actions.addAll(actionsRawVal)
        return actions
    }

    private static Map validateAndGetParamsForKind(def params) {
        final String kind = Optional.ofNullable(params.kind).orElseThrow { new MissingParameter("kind") }

        [kind: kind, actions: getActionsFromQueryParams(params), projectName: params.project]
    }

    private static Map validateAndGetParamsForTypeWithSpecifier(def params, def allowedTypes) {
        final String type = Optional.ofNullable(params.type).orElseThrow { new MissingParameter("type") }
        final String specifier = Optional.ofNullable(params.specifier).orElseThrow { new MissingParameter("specifier") }

        if (type in allowedTypes == false) {
            throw new InvalidParameterException(paramName: 'type', suppliedValue: type, errorDescription : "It must be in: $allowedTypes")
        }

        [type: type, specifier: specifier, actions: getActionsFromQueryParams(params), projectName: params.project]
    }

}

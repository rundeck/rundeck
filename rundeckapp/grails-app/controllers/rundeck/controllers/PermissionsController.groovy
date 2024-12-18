package rundeck.controllers

import com.dtolabs.rundeck.app.api.permissions.ActionAuthorization
import com.dtolabs.rundeck.app.api.permissions.ApplicationAuthorizationContext
import com.dtolabs.rundeck.app.api.permissions.KindResource
import com.dtolabs.rundeck.app.api.permissions.PermissionsResponse
import com.dtolabs.rundeck.app.api.permissions.ProjectAuthorizationContext
import com.dtolabs.rundeck.app.api.permissions.TypeResource
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
import org.rundeck.core.auth.access.MissingParameter
import rundeck.ScheduledExecution
import rundeck.error_handling.InvalidParameterException
import rundeck.services.ScheduledExecutionService

@Controller
class PermissionsController extends ControllerBase {

    static final def APPLICATION_CONTEXT_ALLOWED_TYPES =  ['project', 'project_acl']

    ScheduledExecutionService scheduledExecutionService

    @Get(uri='/api/{api_version}/permissions/application/{kind}', produces = io.micronaut.http.MediaType.APPLICATION_JSON)
    @Operation(
            method='GET',
            description='''Get permissions for an application resource kind.

Since: v53''',
            tags=['permissions'],
            parameters = [
                    @Parameter(name = 'kind', description = 'Resource Kind', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'actions', description = 'Actions to check authorization for', required = true, in = ParameterIn.QUERY, array = @ArraySchema (schema = @Schema(type = 'string')))
            ])
    @ApiResponse(
            responseCode='200',
            description='Action authorizations',
            content = @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema=@Schema(implementation=PermissionsResponse)
            )
    )
    def appContextPermissionsForResourceKind() {
        def kindParams = validateAndGetParamsForKind(params)

        def resource = [type: 'resource', 'kind': kindParams.kind]

        def authContext = rundeckAuthContextProcessor.getAuthContextForSubject(request.subject)
        def decisions = authContext.evaluate(Set.of(resource), kindParams.actions, AuthorizationUtil.RUNDECK_APP_ENV)

        def result = new PermissionsResponse(
                authorizationContext: new ApplicationAuthorizationContext(),
                resource: new KindResource(kind: kindParams.kind),
                actionAuthorizations: decisions.collect { Decision decision ->
                    new ActionAuthorization(actionName: decision.action, isAuthorized: decision.isAuthorized())
                } as Set<ActionAuthorization>
        )

        respond(result, formats: ['json'])
    }

    @Get(uri='/api/{api_version}/permissions/application/{type}/{specifier}', produces = io.micronaut.http.MediaType.APPLICATION_JSON)
    @Operation(
            method='GET',
            description='''Get permissions for an application type with specifier.

Since: v53''',
            tags=['permissions'],
            parameters = [
                    @Parameter(name = 'type', description = 'Resource Type', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'specifier', description = 'Resource specifier', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'actions', description = 'Actions to check authorization for', required = true, in = ParameterIn.QUERY, array = @ArraySchema (schema = @Schema(type = 'string')))
            ])
    @ApiResponse(
            responseCode='200',
            description='Action authorizations',
            content = @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema=@Schema(implementation=PermissionsResponse)
            )
    )
    def appContextPermissionsForTypeWithSpecifier() {
        def typeParams = validateAndGetParamsForTypeWithSpecifier(params, APPLICATION_CONTEXT_ALLOWED_TYPES)

        def resource = [type: typeParams.type, name: typeParams.specifier]

        def authContext = rundeckAuthContextProcessor.getAuthContextForSubject(request.subject)
        def decisions = authContext.evaluate(Set.of(resource), typeParams.actions, AuthorizationUtil.RUNDECK_APP_ENV)

        def result = new PermissionsResponse(
                authorizationContext: new ApplicationAuthorizationContext(),
                resource: new TypeResource(type: typeParams.type, specifier: typeParams.specifier),
                actionAuthorizations: decisions.collect { Decision decision ->
                    new ActionAuthorization(actionName: decision.action, isAuthorized: decision.isAuthorized())
                } as Set<ActionAuthorization>
        )

        respond(result, formats: ['json'])
    }

    @Get(uri='/api/{api_version}/permissions/project/{project}/{kind}', produces = io.micronaut.http.MediaType.APPLICATION_JSON)
    @Operation(
            method='GET',
            description='''Get permissions for a project resource kind.

Since: v53''',
            tags=['permissions'],
            parameters = [
                    @Parameter(name = 'project', description = 'Project Name', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'kind', description = 'Resource Kind', required = true, in = ParameterIn.PATH, schema = @Schema(type = 'string')),
                    @Parameter(name = 'actions', description = 'Actions to check authorization for', required = true, in = ParameterIn.QUERY, array = @ArraySchema (schema = @Schema(type = 'string')))
            ])
    @ApiResponse(
            responseCode='200',
            description='Action authorizations',
            content = @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema=@Schema(implementation=PermissionsResponse)
            )
    )
    def projectContextPermissionsForResourceKind() {
        def kindParams = validateAndGetParamsForKind(params)

        def resource = [type: 'resource', project: kindParams.projectName, kind: kindParams.kind]

        def authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(request.subject, kindParams.projectName)
        def decisions = authContext.evaluate(Set.of(resource), kindParams.actions, AuthorizationUtil.projectContext(kindParams.projectName))

        def result = new PermissionsResponse(
                authorizationContext: new ProjectAuthorizationContext(name: kindParams.projectName),
                resource: new KindResource(kind: kindParams.kind),
                actionAuthorizations: decisions.collect { Decision decision ->
                    new ActionAuthorization(actionName: decision.action, isAuthorized: decision.isAuthorized())
                } as Set<ActionAuthorization>
        )

        respond(result, formats: ['json'])
    }

    @Get(uri='/api/{api_version}/permissions/project/{project}/job/{specifier}', produces = io.micronaut.http.MediaType.APPLICATION_JSON)
    @Operation(
            method='GET',
            description='''Get permissions for a job.

Since: v53''',
            tags=['permissions'],
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
                    schema=@Schema(implementation=PermissionsResponse)
            )
    )
    def projectContextPermissionsForJob() {

        // The `job` permissions call is a slightly specialized `type` permissions call, thus reuse existing validation with minor tweaks
        params.put('type', 'job')
        def typeParams = validateAndGetParamsForTypeWithSpecifier(params, ['job'] )

        // Get and validate the job
        final ScheduledExecution job = Optional.ofNullable(scheduledExecutionService.getByIDorUUID(typeParams.specifier))
                .orElseThrow { new InvalidParameterException(paramName: 'specifier', suppliedValue: typeParams.specifier, errorDescription : "It must be a valid job id in the project") }
        if (job.project != typeParams.projectName) {
            throw new InvalidParameterException(paramName: 'specifier', suppliedValue: typeParams.specifier, errorDescription : "It must be a valid job id in the project")
        }

        def resource = rundeckAuthContextProcessor.authResourceForJob(job)

        def authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(request.subject, typeParams.projectName)
        def decisions = authContext.evaluate(Set.of(resource), typeParams.actions, AuthorizationUtil.projectContext(typeParams.projectName))

        def result = new PermissionsResponse(
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

package com.rundeck.plugin

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.cluster.ClusterInfoService
import com.rundeck.plugin.api.model.ModeLaterRequest
import com.rundeck.plugin.api.model.ModeLaterResponse
import grails.converters.JSON
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.rundeck.core.auth.AuthConstants

import javax.servlet.http.HttpServletResponse


@Controller("/system/executions")
class ExecutionModeController {

    static allowedMethods = [
            getExecutionLater: 'GET',
            getNextExecutionChangeStatus: 'GET',
            apiExecutionModeLaterActive: 'POST',
            apiExecutionModeLaterPassive: 'POST'

    ]

    def executionModeService
    ClusterInfoService clusterInfoService
    AuthContextProcessor rundeckAuthContextProcessor
    def apiService

    private boolean authorizeSystemAdmin() {

        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (!rundeckAuthContextProcessor.authorizeApplicationResourceAny(
            authContext,
            AuthConstants.RESOURCE_TYPE_SYSTEM,
            [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN]
        )) {
            request.errorCode = 'request.error.unauthorized.message'
            request.errorArgs = [
                    'Calendar (admin)',
                    'Server',
                    clusterInfoService.getServerUUID()]
            response.status = HttpServletResponse.SC_FORBIDDEN
            request.titleCode = 'request.error.unauthorized.title'

            render(view: "/common/error", model: [:])
            return false
        }
        return true
    }

    def getExecutionLater() {
        if (!authorizeSystemAdmin()) {
            return
        }

        render(
                executionModeService.getExecutionModeLater() as JSON,
                contentType: 'application/json'
        )
    }

    def getNextExecutionChangeStatus(){
        if (!authorizeSystemAdmin()) {
            return
        }

        Map status = executionModeService.getSystemModeChangeStatus()

        if(status.nextFireTime) {
            Date now = new Date()
            TimeDuration duration = TimeCategory.minus(status.nextFireTime, now)
            if (duration.days != 0) {
                status.msg = message(
                    code: "executions.${status.action}.days",
                    args: [duration.days, duration.hours, duration.minutes]
                ).toString()
            } else if (duration.hours != 0) {
                status.msg = message(code: "executions.${status.action}.hours", args: [duration.hours, duration.minutes]).toString()
            } else if (duration.minutes != 0) {
                status.msg = message(code: "executions.${status.action}.minutes", args: [duration.minutes]).toString()
            } else {
                status.msg = message(code: "executions.${status.action}.seconds", args: [duration.seconds]).toString()
            }
        }

        render(
                status as JSON,
                contentType: 'application/json'
        )
    }

    @Post(uri = "/enable/later")
    @Operation(
        method = "POST",
        summary = "Enable System executions after a duration of time",
        description = """Sets System execution mode to Active at a later time.

Since: v34
""",
        requestBody = @RequestBody(
            required = true,
            description = """Enable Executions.
Specify a `value` with a time duration expression. (See request schema for syntax.)
""",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = [
                    @ExampleObject(value='{"value":"2h30m"}'),
                ],
                schema = @Schema(implementation = ModeLaterRequest)
            )
        )
    )
    @ApiResponse(
        description = "Request processed. The `saved` value may be false if e.g. the current execution mode is already enabled.",
        responseCode = "200",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ModeLaterResponse),
            examples = @ExampleObject('{"saved":true,"msg":"Execution Mode Later saved"}')
        )
    )
    @ApiResponse(
        description = "Request error response. A description of the error is in the `msg` field.",
        responseCode = "400",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ModeLaterResponse),
            examples = @ExampleObject('{"saved":false,"msg":"Project Execution Mode Later saved"}')
        )
    )
    @Tags(
       [
           @Tag(name="system execution mode")
       ]
    )
    def apiExecutionModeLaterActive(){

        if (!authorizeSystemAdmin()) {
            return
        }

        if (!apiService.requireVersion(request, response, PluginUtil.V34)) {
            return
        }

        def result = validateApi(request, response)

        def saved = false
        def msg = ""

        if(!result.fail){

            def status = executionModeService.getCurrentStatus()
            if(status){
                msg = "Executions are already set on active mode, cannot active later"
            }else{
                def config = [activeLater: true, activeLaterValue:result.value]
                saved = executionModeService.saveExecutionModeLater(config)
                if(saved){
                    msg = "Execution Mode Later saved"
                }else{
                    msg = "No changed found"
                }
            }
        }

        if(result.errormsg){
            msg = result.errormsg
            response.status = 400
        }

        render(
                [saved: saved, msg: msg] as JSON,
                contentType: 'application/json'
        )

    }


    @Post(uri = "/disable/later")
    @Operation(
        method = "POST",
        summary = "Disable System executions after a duration of time",
        description = """Sets System execution mode to Passive at a later time.

Since: v34
""",
        requestBody = @RequestBody(
            required = true,
            description = """Disable Executions.
Specify a `value` with a time duration expression. (See request schema for syntax.)
""",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = [
                    @ExampleObject(value='{"value":"2h30m"}'),
                ],
                schema = @Schema(implementation = ModeLaterRequest)
            )
        )
    )
    @ApiResponse(
        description = "Request processed. The `saved` value may be false if e.g. the current execution mode is already disabled.",
        responseCode = "200",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ModeLaterResponse),
            examples = @ExampleObject('{"saved":true,"msg":"Execution Mode Later saved"}')
        )
    )
    @ApiResponse(
        description = "Request error response. A description of the error is in the `msg` field.",
        responseCode = "400",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ModeLaterResponse),
            examples = @ExampleObject('{"saved":false,"msg":"Project Execution Mode Later saved"}')
        )
    )
    @Tags(
        [
            @Tag(name="system execution mode")
        ]
    )
    def apiExecutionModeLaterPassive(){

        if (!authorizeSystemAdmin()) {
            return
        }

        if (!apiService.requireVersion(request, response, PluginUtil.V34)) {
            return
        }

        def result = validateApi(request, response)

        def saved = false
        def msg = ""

        if(!result.fail){

            def status = executionModeService.getCurrentStatus()
            if(!status){
                msg = "Executions are already set on passive mode, cannot disable later"
            }else{
                Map config = [passiveLater:true,passiveLaterValue:result.value]
                saved = executionModeService.saveExecutionModeLater(config)
                if(saved){
                    msg = "Execution Mode Later saved"
                }else{
                    msg = "No changed found"
                }
            }
        }

        if(result.errormsg){
            msg = result.errormsg
            response.status = 400
        }

        render(
                [saved: saved, msg: msg] as JSON,
                contentType: 'application/json'
        )

    }

    def validateApi( request,  response){
        boolean fail = false
        def errormsg = ""
        def value = null

        def data
        try{
            data = request.JSON
        }catch(Exception e){
            errormsg = e.message
        }

        if(!data){
            fail=true
            errormsg = 'Format was not valid. the request must be a json object, for example: {"value":"<timeExpression>"}'
            return [value: data.value, fail: fail, errormsg:errormsg]
        }

        if(!data.value){
            fail=true
            errormsg = 'Format was not valid. the request must be a json object, for example: {"value":"30m"}'
            return [value: data.value, fail: fail, errormsg:errormsg]
        }

        if(!PluginUtil.validateTimeDuration(data.value)){
            fail=true
            errormsg = "Format was not valid, the attribute value is not set properly. Use something like: 3m, 1h, 3d"
            return [value: data.value, fail: fail, errormsg:errormsg]
        }

        return [value: data.value, fail: fail, errormsg:errormsg]


    }
}

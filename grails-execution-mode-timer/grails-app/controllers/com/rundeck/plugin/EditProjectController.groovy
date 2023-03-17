package com.rundeck.plugin

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.rundeck.plugin.api.model.ModeLaterResponse
import com.rundeck.plugin.api.model.ProjectModeLaterRequest
import grails.converters.JSON
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.rundeck.core.auth.AuthConstants

import javax.servlet.http.HttpServletResponse

@Controller("/project")
class EditProjectController {

    static allowedMethods = [
            getExecutionLater: 'GET',
            apiProjectDisableLater: 'POST',
            apiProjectEnableLater: 'POST',

    ]

    def frameworkService
    AuthContextProcessor rundeckAuthContextProcessor
    def updateModeProjectService
    def apiService

    def boolean requireAuth(String project) {

        def authContext =
                rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, project)
        if (!rundeckAuthContextProcessor.authorizeApplicationResourceAny(
            authContext,
            rundeckAuthContextProcessor.authResourceForProject(project),
            [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
        )) {
            request.errorCode = 'request.error.unauthorized.message'
            request.errorArgs = ['Calendar (admin)', 'Server']
            response.status = HttpServletResponse.SC_FORBIDDEN
            request.titleCode = 'request.error.unauthorized.title'

            render(view: "/common/error", model: [:])
            return false
        }
        return true
    }

    def getExecutionLater(String project) {
        if (!requireAuth(project)) {
            return
        }
        String executionLaterPath="extraConfig/executionLater.properties"
        IRundeckProject rundeckProject =  frameworkService.getFrameworkProject(project)
        Map result = updateModeProjectService.getScheduleExecutionLater(rundeckProject, executionLaterPath)

        render(
                result as JSON,
                contentType: 'application/json'
        )
    }

    def getNextExecutionChangeStatus(String project){
        if (!requireAuth(project)) {
            return
        }

        def executionStatus = updateModeProjectService.getProjectModeChangeStatus(project, "executions")
        def scheduleStatus = updateModeProjectService.getProjectModeChangeStatus(project, "schedule")

        def now = new Date()
        if(executionStatus.nextFireTime){
            TimeDuration duration = TimeCategory.minus(executionStatus.nextFireTime, now)
            if (duration.days != 0) {
                executionStatus.msg = message(
                    code: "executions.${executionStatus.action}.days",
                    args: [duration.days, duration.hours, duration.minutes]
                ).toString()
            } else if (duration.hours != 0) {
                executionStatus.msg = message(code: "executions.${executionStatus.action}.hours", args: [duration.hours, duration.minutes]).toString()
            } else if (duration.minutes != 0) {
                executionStatus.msg = message(code: "executions.${executionStatus.action}.minutes", args: [duration.minutes]).toString()
            } else {
                executionStatus.msg = message(code: "executions.${executionStatus.action}.seconds", args: [duration.seconds]).toString()
            }
        }

        if(scheduleStatus.nextFireTime){
            TimeDuration duration = TimeCategory.minus(scheduleStatus.nextFireTime, now)
            if (duration.days != 0) {
                scheduleStatus.msg = message(
                    code: "schedules.${scheduleStatus.action}.days",
                    args: [duration.days, duration.hours, duration.minutes]
                ).toString()
            } else if (duration.hours != 0) {
                scheduleStatus.msg = message(code: "schedules.${scheduleStatus.action}.hours", args: [duration.hours, duration.minutes]).toString()
            } else if (duration.minutes != 0) {
                scheduleStatus.msg = message(code: "schedules.${scheduleStatus.action}.minutes", args: [duration.minutes]).toString()
            } else {
                scheduleStatus.msg = message(code: "schedules.${scheduleStatus.action}.seconds", args: [duration.seconds]).toString()
            }
        }


        render(
                [execution: executionStatus, schedule: scheduleStatus] as JSON,
                contentType: 'application/json'
        )
    }

    @Post(uri = "/{project}/enable/later")
    @Operation(
        method = "POST",
        summary = "Enable Project executions or schedules after a duration of time",
        description = """Sets project execution mode to Active or enable Schedules at a later time.

Since: v34
""",
        requestBody = @RequestBody(
            required = true,
            description = """Enable Schedule or Executions.
Specify the `type` to enable, and a `value` with a time duration expression.
The request must contain a `value` with a "Time duration expression". (See request schema for syntax.)
""",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = [
                    @ExampleObject(name="Enable Schedule",value='{"type":"schedule","value":"2h30m"}'),
                    @ExampleObject(name="Enable Executions",value='{"type":"executions","value":"1d"}')
                ],
                schema = @Schema(implementation = ProjectModeLaterRequest)
            )
        ),
        parameters = @Parameter(
            name = "project",
            in = ParameterIn.PATH,
            description = "project name",
            required = true,
            schema = @Schema(type = 'string')
        )
    )
    @ApiResponse(
        description = "Request processed. The `saved` value may be false if e.g. the current execution mode is already enabled.",
        responseCode = "200",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ModeLaterResponse),
            examples = @ExampleObject('{"saved":true,"msg":"Project Execution Mode Later saved"}')
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
            @Tag(name="project execution mode"),
            @Tag(name="project schedule mode")
        ]
    )
    def apiProjectEnableLater(String project) {
        if (!requireAuth(project)) {
            return
        }

        if (!apiService.requireVersion(request, response, PluginUtil.V34)) {
            return
        }

        //TODO: use ModeLaterRequest type
        def result = validateApi(request, response)
        def saved = false
        def msg = ""

        if(!result.fail){
            Properties projProps = new Properties()
            Set removePrefixes=[]

            IRundeckProject rundeckProject =  frameworkService.getFrameworkProject(project)
            Map properties = rundeckProject.getProjectProperties()
            projProps.putAll(properties)

            //check if current status of project
            def isExecutionDisabledNow = properties[UpdateModeProjectService.CONF_PROJECT_DISABLE_EXECUTION] == 'true'
            def isScheduleDisabledNow = properties[UpdateModeProjectService.CONF_PROJECT_DISABLE_SCHEDULE] == 'true'

            boolean canSave = true

            if(result.config.type == "executions"){

                if(!isExecutionDisabledNow){
                    canSave=false
                    saved=false
                    msg="Executions are already enabled, cannot disable later"
                }

                projProps.put("project.later.executions.enable", "true")
                projProps.put("project.later.executions.enable.value", result.config.value)

                removePrefixes.add("project.later.executions")

            }else{

                if(!isScheduleDisabledNow){
                    canSave=false
                    saved=false
                    msg="Schedule are already enabled, cannot enable later"
                }

                projProps.put("project.later.schedule.enable", "true")
                projProps.put("project.later.schedule.enable.value", result.config.value)

                removePrefixes.add("project.later.schedule")
            }

            if(canSave){
                //save project settings
                frameworkService.updateFrameworkProjectConfig(project, projProps, removePrefixes)

                saved = updateModeProjectService.saveExecutionLaterSettings(project, projProps)
                if(saved){
                    msg = "Project Execution Mode Later saved"
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


    @Post(uri = "/{project}/disable/later")
    @Operation(
        method = "POST",
        summary = "Disable Project executions or schedules after a duration of time",
        description = """Sets project execution mode to Passive or disables Schedules at a later time.

Since: v34
""",
        requestBody = @RequestBody(
            required = true,
            description = """Disable Schedule or Executions.
Specify the `type` to enable, and a `value` with a time duration expression.
The request must contain a `value` with a "Time duration expression". (See request schema for syntax.)
""",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = [
                    @ExampleObject(name="Disable Schedule",value='{"type":"schedule","value":"2h30m"}'),
                    @ExampleObject(name="Disable Executions",value='{"type":"executions","value":"1d"}')
                ],
                schema = @Schema(implementation = ProjectModeLaterRequest)
            )
        ),
        parameters = @Parameter(
            name = "project",
            in = ParameterIn.PATH,
            description = "project name",
            required = true,
            schema = @Schema(type = 'string')
        )
    )
    @ApiResponse(
        description = "Request processed. The `saved` value may be false if e.g. the current execution mode is already enabled.",
        responseCode = "200",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ModeLaterResponse),
            examples = @ExampleObject('{"saved":true,"msg":"Project Execution Mode Later saved"}')
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
            @Tag(name="project execution mode"),
            @Tag(name="project schedule mode")
        ]
    )
    def apiProjectDisableLater(String project){

        if (!requireAuth(project)) {
            return
        }

        if (!apiService.requireVersion(request, response, PluginUtil.V34)) {
            return
        }

        def result = validateApi(request, response)
        def saved = false
        def msg = ""

        if(!result.fail){
            Properties projProps = new Properties()
            Set removePrefixes=[]

            IRundeckProject rundeckProject =  frameworkService.getFrameworkProject(project)
            Map properties = rundeckProject.getProjectProperties()
            projProps.putAll(properties)

            //check if current status of project
            def isExecutionDisabledNow = properties[UpdateModeProjectService.CONF_PROJECT_DISABLE_EXECUTION] == 'true'
            def isScheduleDisabledNow = properties[UpdateModeProjectService.CONF_PROJECT_DISABLE_SCHEDULE] == 'true'

            boolean canSave = true

            if(result.config.type == "executions"){
                if(isExecutionDisabledNow){
                    canSave=false
                    saved=false
                    msg="Execution are already disabled, cannot disable later"
                }

                projProps.put("project.later.executions.disable", "true")
                projProps.put("project.later.executions.disable.value", result.config.value)

                removePrefixes.add("project.later.executions")
            }else{
                if(isScheduleDisabledNow){
                    canSave=false
                    saved=false
                    msg="Schedule are already disabled, cannot disable later"
                }

                projProps.put("project.later.schedule.disable", "true")
                projProps.put("project.later.schedule.disable.value", result.config.value)

                removePrefixes.add("project.later.schedule")
            }

            if(canSave){
                //save project settings
                frameworkService.updateFrameworkProjectConfig(project, projProps, removePrefixes)

                saved = updateModeProjectService.saveExecutionLaterSettings(project, projProps)
                if(saved){
                    msg = "Project Execution Mode Later saved"
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
        def config = null

        try{
            config = request.JSON
        }catch(Exception e){
            errormsg = e.message
        }

        //review config value
        if(!config){
            fail=true
            errormsg = 'Format was not valid, the request must be a json object with the format: {"type":"<executions|schedule>","value":"<timeExpression>"}'
            return [config: config, fail: fail, errormsg:errormsg]

        }

        if(!config.type){
            fail=true
            errormsg = "Format was not valid, the attribute type must be set (executions or schedule)."
            return [config: config, fail: fail, errormsg:errormsg]

        }

        if(config.type){
            def allowedValues = ["executions","schedule"]
            if(!allowedValues.contains(config.type)){
                fail=true
                errormsg = "Format was not valid, the attribute type must be set with the proper value(executions or schedule)."
                return [config: config, fail: fail, errormsg:errormsg]

            }
        }

        if(!config.value){
            fail=true
            errormsg = "Format was not valid, the attribute value must be set."
            return [config: config, fail: fail, errormsg:errormsg]

        }

        if(config.value){
            if(!PluginUtil.validateTimeDuration(config.value)){
                fail=true
                errormsg = "Format was not valid, the attribute value is not set properly. Use something like: 3m, 1h, 3d"
                return [config: config, fail: fail, errormsg:errormsg]

            }
        }

        return [config: config, fail: fail, errormsg:errormsg]


    }

}

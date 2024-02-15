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

import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.app.api.jobs.options.OptionValidateRequest
import com.dtolabs.rundeck.app.api.jobs.options.OptionValidateResponse
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.http.ApacheHttpClient
import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigData
import com.jayway.jsonpath.JsonPath
import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.rundeck.app.jobs.options.ApiTokenReporter
import org.rundeck.app.jobs.options.JobOptionConfigRemoteUrl
import org.rundeck.app.jobs.options.RemoteUrlAuthenticationType
import groovy.transform.PackageScope
import org.rundeck.app.data.providers.v1.user.UserDataProvider
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.rundeck.core.auth.AuthConstants
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService
import rundeck.utils.OptionsUtil

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
/**
 * Controller for manipulating the session-stored set of Options during job edit
 */
@Controller
class EditOptsController extends ControllerBase{
    static Logger logger = LoggerFactory.getLogger(EditOptsController)
    def FrameworkService frameworkService
    UserDataProvider userDataProvider
    def fileUploadService
    def optionValuesService
    def scheduledExecutionService
    MessageSource messageSource
    def static allowedMethods = [
            redo: 'POST',
            remove: 'POST',
            reorder: 'POST',
            revert: 'POST',
            save: 'POST',
            undo: 'POST',
            apiValidateOption: 'POST'
    ]

    def index() {
        redirect(controller: 'menu', action: 'index')
    }

    def error() {
        return render(template: "/common/messages")
    }

    /**
     * Determine if job access is allowed based on ID parameter
     * @param id id value, or null if no job access requested
     * @param actions list of required actions
     * @return true if allowed
     */
    @PackageScope
    boolean allowedJobAuthorization(def id, List<String> actions){
        if(!id){
            return true
        }
        ScheduledExecution scheduledExecution = ScheduledExecution.getByIdOrUUID( id )
        if (notFoundResponse(scheduledExecution, 'Job', id)) {
            return false
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, scheduledExecution.project)
        return !unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectJobAll(
                authContext,
                scheduledExecution,
                actions,
                scheduledExecution.project
            ), actions[0], 'Job', id
        )
    }

    /**
     * render edit form for an option.  params.name= name of existing option to edit, otherwise params.newoption is
     * required to create a new option
     */
    def edit() {
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }

        if (!params.name && !params.newoption) {
            log.error("name parameter required")
            flash.error = "name parameter required"
            return error()
        }

        def editopts = _getSessionOptions()
        if (params.name && !editopts[params.name]) {
            log.error("no option with name ${params.name} found")
            flash.error = "no option with name ${params.name} found"
            return error()
        }
        def outparams=[:]
        if(null != params.name && editopts[params.name]){
            def opt = editopts[params.name]
            outparams = _validateOption(opt, userDataProvider, null,null, null, params.jobWasScheduled == 'true')
            outparams.configMapValidate = validateFileOpt(opt)
        }

        def model = [
                option                     : null != params.name ? editopts[params.name] : null,
                name                       : params.name,
                scheduledExecutionId       : params.scheduledExecutionId,
                newoption                  : params['newoption'],
                edit                       : true,
                fileUploadPluginDescription: fileUploadService.pluginDescription,
                optionValuesPlugins        : optionValuesService.listOptionValuesPlugins()?.sort{a,b->a.key<=>b.key}
        ]
        return render(template: "/scheduledExecution/optEdit", model: model + outparams)
    }

    /**
     * Render view of an option definition. params.name= name of option to render, required.
     */
    def renderOpt() {
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        if (!params.name) {
            log.error("name parameter required")
            flash.error = "name parameter required"
            return error()
        }
        def name = params.name
        def Map editopts = _getSessionOptions()
        if (!editopts[name]) {
            log.error("no option with name ${params.name} found")
            flash.error = "no option with name ${params.name} found"
            return error()
        }
        def optIndex=editopts.values()*.name.indexOf(name)

        return render(
                template: "/scheduledExecution/optlistitemContent",
                model: [
                        optCount            : editopts.size(),
                        optIndex            : optIndex,
                        options             : editopts,
                        option              : editopts[name],
                        name                : name,
                        scheduledExecutionId: params.scheduledExecutionId,
                        edit                : params.edit
                ]
        )
    }

    /**
     * Render all options
     */
    def renderAll() {
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        def Map editopts = _getSessionOptions()
        //configure sorted list
        def options = new TreeSet()
        options.addAll(editopts.values())
        return render(template: "/scheduledExecution/optlistContent", model: [options: options, scheduledExecutionId: params.scheduledExecutionId, edit: params.edit])
    }


    /**
     * Render all options in summary view
     */
    def renderSummary() {
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        def Map editopts = _getSessionOptions()
        //configure sorted list
        def options = new TreeSet()
        options.addAll(editopts.values())
        return render(template: "/scheduledExecution/optionsSummary", model: [options: options, scheduledExecutionId: params.scheduledExecutionId, edit: params.edit])
    }

    /**
     * Save new option or existing option definition. params.name= name of existing option, or params.newoption is required
     */
    def save() {
        withForm{
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }

        if (!params.name && !params.newoption) {
            log.error("name parameter is required")
            flash.error = "name parameter is required"
            return error()
        }
        def editopts = _getSessionOptions()
        def name = params.name
        def origName = params.origName

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, params.project)
        def result = _applyOptionAction(editopts, [action: 'true' == params.newoption ? 'insert' : 'modify', name: origName ? origName : name, params: params], authContext)
        if (result.error) {
            log.error(result.error)

            def model = [
                    option                     : result.option,
                    name                       : params.num,
                    scheduledExecutionId       : params.scheduledExecutionId,
                    origName                   : params.origName,
                    newoption                  : params['newoption'],
                    edit                       : true,
                    regexError                 : result.regexError,
                    configMapValidate          : result.configMapValidate,
                    fileUploadPluginDescription: fileUploadService.pluginDescription,
                    optionValuesPlugins        : optionValuesService.listOptionValuesPlugins()?.sort{a,b->a.key<=>b.key}
            ]
            return render(template: "/scheduledExecution/optEdit", model: model
            )
        }
        _pushUndoAction(params.scheduledExecutionId, result.undo)
        if (result.undo) {
            _clearRedoStack(params.scheduledExecutionId)
        }
        def optIndex=editopts.values()*.name.indexOf(name)
        return render(
                template: "/scheduledExecution/optlistitemContent",
                model: [
                        optCount: editopts.size(),
                        optIndex:optIndex,
                        option: editopts [ name ],
                        name : name,
                        scheduledExecutionId: params.scheduledExecutionId,
                        edit: true
                ]
        )
        }.invalidToken{
            request.error = g.message(code: 'request.error.invalidtoken.message')
            response.status=400
            return error()
        }
    }

    /**
     * Remove an option by name.  params.name required
     */
    def remove() {
        withForm {
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }

        if (!params.name) {
            log.error("name parameter is required")
            flash.error = "name parameter is required"
            return error()
        }
        def editopts = _getSessionOptions()
        def name = params.name

        def result = _applyOptionAction(editopts, [action: 'remove', name: name, params: params])
        def options = new TreeSet()
        options.addAll(editopts.values())
        if (result.error) {
            log.error(result.error)
            return render(template: "/scheduledExecution/optlistContent", model: [options: options, name: name, scheduledExecutionId: params.scheduledExecutionId, edit: true, error: result.error])
        }
        _pushUndoAction(params.scheduledExecutionId, result.undo)
        if (result.undo) {
            _clearRedoStack(params.scheduledExecutionId)
        }

        return render(template: "/scheduledExecution/optlistContent", model: [options: options, name: name, scheduledExecutionId: params.scheduledExecutionId, edit: true])
        }.invalidToken{
            request.error = g.message(code: 'request.error.invalidtoken.message')
            response.status=400
            return error()
        }
    }

    /**
     * duplicate option
     */
    def duplicate() {
        withForm{
            if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
                return
            }
            if (!params.name ) {
                log.error("name parameter is required")
                flash.error = "name parameter is required"
                return error()
            }
            Map editopts = _getSessionOptions()
            def result = _duplicateOption(editopts)

            if (result.actions.error) {
                log.error(result.error)

                def model = [
                        option                     : result.option,
                        name                       : params.num,
                        scheduledExecutionId       : params.scheduledExecutionId,
                        origName                   : params.origName,
                        newoption                  : params['newoption'],
                        edit                       : true,
                        regexError                 : result.regexError,
                        configMapValidate          : result.configMapValidate,
                        fileUploadPluginDescription: fileUploadService.pluginDescription
                ]
                return render(template: "/scheduledExecution/optEdit", model: model
                )
            }
            _pushUndoAction(params.scheduledExecutionId, result.actions.undo)

            def optIndex=editopts.size()-1

            return render(
                    template: "/scheduledExecution/optlistitemContent",
                    model: [
                            optCount: editopts.size(),
                            optIndex:optIndex,
                            option: editopts [ result.name ],
                            name : result.name,
                            scheduledExecutionId: params.scheduledExecutionId,
                            edit: true
                    ]
            )
        }.invalidToken{
            request.error = g.message(code: 'request.error.invalidtoken.message')
            response.status=400
            return error()
        }
    }

    /**
     * Reorder an option by name.  params.name required, other params:
     *
     * one of:
     *
     * * relativePosition: integer indicating relative steps to move
     * * last: true: move to last position
     * * before: (option name) move to above another option by name
     */
    def reorder () {
        withForm {
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        if (!params.name) {
            log.error("name parameter is required")
            flash.error = "name parameter is required"
            return error()
        }
        if (!params.relativePosition && !params.last && !params.before) {
            log.error("relativePosition, last, or before parameter is required")
            flash.error = "relativePosition, last, or before parameter is required"
            return error()
        }
        def editopts = _getSessionOptions()
        def name = params.name

        def result = _applyOptionAction(editopts, [action: 'reorder', name: name, params: params])
        def options = new TreeSet()
        options.addAll(editopts.values())
        if (result.error) {
            log.error(result.error)
            return render(template: "/scheduledExecution/optlistContent", model: [options: options, name: name, scheduledExecutionId: params.scheduledExecutionId, edit: true, error: result.error])
        }
        _pushUndoAction(params.scheduledExecutionId, result.undo)
        if (result.undo) {
            _clearRedoStack(params.scheduledExecutionId)
        }

        return render(template: "/scheduledExecution/optlistContent", model: [options: options, name: name, scheduledExecutionId: params.scheduledExecutionId, edit: true])
        }.invalidToken{
            request.error = g.message(code: 'request.error.invalidtoken.message')
            response.status=400
            return error()
        }
    }


    /**
     * Show undo/redo buttons
     */
    def renderUndo() {
        final String id = params.scheduledExecutionId ? params.scheduledExecutionId : '_new'
        render(template: "/common/undoRedoControls", model: [
                undo         : session.undoOPTS ? session.undoOPTS[id]?.size() : 0,
                redo         : session.redoOPTS ? session.redoOPTS[id]?.size() : 0,
                key          : 'opts',
                revertConfirm: 'all Options',
                highlightundo: session.undoOPTSstate?.get(id)
        ]
        )
    }


    /**
     * Undo action, renders full options list after performing undo
     */
    def undo() {
        withForm{

        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        def editopts = _getSessionOptions()
        def action = _popUndoAction(params.scheduledExecutionId)

        def name = null
        if (action) {
            def result = _applyOptionAction(editopts, action)
            if (result.error) {
                log.error(result.error)
                flash.error = result.error
                response.status = 400
                return error()
            }
            if (null != action.name) {
                name = action.name
            }
            _pushRedoAction(params.scheduledExecutionId, result.undo)
        }
        def options = new TreeSet()
        options.addAll(editopts.values())
        return render(template: "/scheduledExecution/optlistContent", model: [options: options, scheduledExecutionId: params.scheduledExecutionId, edit: params.edit, highlight: name])
        }.invalidToken {
            request.error = g.message(code: 'request.error.invalidtoken.message')
            response.status=400
            return error()
        }
    }



    /**
     * redo action, renders full options list after performing redo
     */
    def redo() {
        withForm{

        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        def editopts = _getSessionOptions()
        def action = _popRedoAction(params.scheduledExecutionId)

        def name = null
        if (action) {
            def result = _applyOptionAction(editopts, action)
            if (result.error) {
                log.error(result.error)
                flash.error = result.error
                response.status = 400
                return error()
            }
            if (null != action.name) {
                name = action.name
            }
            _pushUndoAction(params.scheduledExecutionId, result.undo)
        }

        def options = new TreeSet()
        options.addAll(editopts.values())
        return render(template: "/scheduledExecution/optlistContent", model: [options: options, scheduledExecutionId: params.scheduledExecutionId, edit: params.edit, highlight: name])
        }.invalidToken {
            request.error = g.message(code: 'request.error.invalidtoken.message')
            response.status=400
            return error()
        }
    }

    /**
     * revert action, reloads options from stored ScheduledExecution, clears undo/redo stack, and renders full options list
     */
    def revert() {
        withForm{
        final String uid = params.scheduledExecutionId ? params.scheduledExecutionId : '_new'
        session.editOPTS?.remove(uid)
        session.undoOPTS?.remove(uid)
        session.redoOPTS?.remove(uid)
        renderAll()
        }.invalidToken {
            request.error = g.message(code: 'request.error.invalidtoken.message')
            response.status=400
            return error()
        }
    }

    @Post(uri='/project/{project}/jobs/validateOption')
    @Operation(
        method = "POST",
        requestBody = @RequestBody(description='Option validation request',content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = OptionValidateRequest)
        )),
        responses=[
            @ApiResponse(
                responseCode = "200",
                description = "Option validation with no errors",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OptionValidateResponse)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Option validation with errors, the messages will contain the validation errors keyed by input field path",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OptionValidateResponse)
                )
            )
        ],
        description = """Validates an option defintion for a job, returns any validation errors.

If any validation errors occur, the response will use code 400, otherwise 200 will be returned.

The request body should be a JSON object describing a Job Option definition, 
and a `jobWasScheduled` parameter to indicate if the job was scheduled.

The data format corresponds with a Job Option definition in Job JSON format, with these additional fields:

* `remoteUrlAuthenticationType`: the type of authentication to use for a remote URL
* `configRemoteUrl`: a configuration object for the remote URL values
* `valuesType`: indicates the type of chosen values list, one of "url", "list", or a Option Values plugin type.


Since: V47""",
        summary = "Validate an option"
    )
    def apiValidateOption(
        @Parameter(in= ParameterIn.PATH,description='Project name') String project,
        @Parameter(
            in = ParameterIn.QUERY,
            description = "True if job was scheduled",
            schema = @Schema(type = "boolean")
        ) boolean jobWasScheduled,
        OptionValidateRequest optionData
    ) {
        if (!apiService.requireApi(request, response, ApiVersions.V47)) {
            return
        }
        def validate = new OptionValidateResponse(valid:true)
        def validator = new OptionInputValidator(validate, messageSource, response.locale)
        optionData.validate()
        validator.receiveErrors(optionData.errors)
        _validateOption(
            (OptionData) optionData,
            validator,
            optionData.convertConfigRemoteUrlData(),
            optionData.valuesType,
            jobWasScheduled
        )
        def report = fileUploadService.validateFileOptConfig(optionData, optionData.errors)
        if(report?.errors){
            //TODO pass config errors
        }
        translateApiErrors(validate)
        respond(validate,[status:validate.valid?200:400])
    }
    static final Map<String, String> FieldTranslations = [
        valuesList   : 'values',
        defaultValue : 'value',
        valuesUrlLong: 'valuesUrl',
        secureInput  : 'secure',
        secureExposed: 'valueExposed',
    ]
    @CompileStatic
    protected static void translateApiErrors(OptionValidateResponse validate){
        if(validate.messages){
            FieldTranslations.each { from, to ->
                if (validate.messages.containsKey(from)) {
                    validate.messages[to] = validate.messages.remove(from)
                }
            }
        }
    }
    static class OptionInputValidator implements OptionValidator{
        OptionValidateResponse validate
        MessageSource messageSource
        Locale locale

        OptionInputValidator(OptionValidateResponse validate, MessageSource messageSource, Locale locale){
            this.validate = validate
            this.messageSource = messageSource
            this.locale = locale
        }
        void receiveErrors(Errors errors){
            if(errors.hasErrors()){
                validate.valid =false
            }
            errors.fieldErrors.each{ FieldError err->
                validate.messages.computeIfAbsent(err.field, {String key->[]}).add(
                    messageSource.getMessage(err, locale)
                )
            }
        }
        void rejectValue(String prop, String messageCode) {
            validate.valid = false
            validate.messages.computeIfAbsent(prop, {String key->[]}).add(
                messageSource.getMessage(messageCode, null, messageCode, locale)
            )
        }

        void rejectValue(String prop, String messageCode, Object[] args, String defaultMessage) {
            validate.valid = false
            validate.messages.computeIfAbsent(prop, {String key->[]}).add(
                messageSource.getMessage(messageCode, args, defaultMessage, locale)
            )
        }

        @Override
        void registerErrorMessage(final String prop, final String message) {
            validate.valid = false
            validate.messages.computeIfAbsent(prop, {_->[]}).add(message)
        }
    }

    /**
     *
     * handles ALL modifications to the options via named actions, input is a map:
     * input map:
     * action: name of action 'remove','insert','modify'
     * name: option name to affect
     * params: properties of the item (insert,modify actions)
     *
     *  Returns result map:
     *
     * error: any error message
     * undo: corresponding undo action map
     */
    protected Map _applyOptionAction (Map editopts, Map input, AuthContext authContext=null){
        def result = [:]

        def scheduledExecution = null
        if(input.params?.scheduledExecutionId  && allowedJobAuthorization(input.params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            scheduledExecution = ScheduledExecution.getByIdOrUUID( input.params.scheduledExecutionId )
        }
        if ('remove' == input.action) {
            def name = input.name
            if(null==editopts[name]){
                result.putAll([error: "No option named ${name} exists"])
                return result
            }
            Option item = editopts.get(name)
            editopts.remove(name)

            result['undo'] = [action: 'insert', name: name, params: _getParamsFromOption(item)]
        } else if ('insert' == input.action) {
            def name = input.name
            def option = _setOptionFromParams(new Option(), input.params)
            JobOptionConfigRemoteUrl configRemoteUrl = scheduledExecutionService.getJobOptionConfigRemoteUrl(option, authContext)
            def vres = _validateOption(option, userDataProvider, scheduledExecution, configRemoteUrl, input.params,input.params.jobWasScheduled=='true')
            vres.configMapValidate = validateFileOpt(option)
            if (null != editopts[name]) {
                option.errors.rejectValue('name', 'option.name.duplicate.message', [name] as Object[], "Option already exists: {0}")
            }
            if (option.errors.hasErrors()) {
                result.putAll(vres)
                result.putAll([error: 'Invalid', option: option])
                return result
            }

            //if it was a dupplicate then shift right all next items
            if(option.sortIndex != null)
                editopts.forEach({ nameKey, opVal -> if (opVal.sortIndex >= option.sortIndex) opVal.sortIndex++ })

            editopts[name] = option
            result['undo'] = [action: 'remove', name: name]
        }  else if ('reorder' == input.action) {
            String name = input.name
            if (!editopts[name]) {
                result.error = "No option named ${name} exists"
                return result
            }

            List<String> sortedNames = new TreeSet(editopts.values())*.name
            int oldloc= sortedNames.indexOf(name)
            int position
            if(input.params.relativePosition) {
                //position is relative
                position = input.params.relativePosition as Integer
            }else if(input.params.last in [true,'true']) {
                position = sortedNames.size() - oldloc - 1
            }else if(input.params.before){
                position = sortedNames.indexOf(input.params.before) - oldloc
                if(position>0){
                    position--
                }
            }else{
                result.error = "Parameter relativePosition,last, or before is required"
                return result
            }
            int newloc = oldloc+position
            if(newloc<0 || newloc>editopts.size()-1){
                result.error = "Cannot reorder option ${name} by ${position}: out of bounds"
                return result
            }
            sortedNames.remove(name)
            sortedNames.add(newloc,name)

            int ndx=1
            for (String optName : sortedNames) {
                editopts[optName].sortIndex=ndx
                ndx++
            }
            result['undo'] = [action: 'reorder', name: name, params:[relativePosition:position*-1]]
        } else if ('modify' == input.action) {
            def name = input.name
            if (!editopts[name]) {
                result.error = "No option named ${name} exists"
                return result
            }
            def Option option = editopts[name]
            def clone = option.createClone()
            def moditem = option.createClone()
            _setOptionFromParams(moditem, input.params)
            JobOptionConfigRemoteUrl configRemoteUrl = scheduledExecutionService.getJobOptionConfigRemoteUrl(option, authContext)
            def vres = _validateOption(moditem, userDataProvider,scheduledExecution,  configRemoteUrl, input.params,input.params.jobWasScheduled=='true')
            vres.configMapValidate = validateFileOpt(moditem)
            if (moditem.name != name && null != editopts[moditem.name]) {
                moditem.errors.rejectValue('name', 'option.name.duplicate.message', [moditem.name] as Object[], "Option already exists: {0}")
            }
            if (moditem.errors.hasErrors()) {
                result.putAll(vres)
                result.putAll([error: 'Invalid', option: moditem])
                return result
            }
            _setOptionFromParams(option, input.params)
            if (option.name != name) {
                //renamed
                editopts.remove(name)
                editopts[option.name] = option
            }
            result['undo'] = [action: 'modify', name: option.name, params: _getParamsFromOption(clone)]
        }
        return result
    }

    /**
     * Validate the Option, return any output parameters in a map
     * @param opt the option
     * @param params input params if any
     */
    protected validateFileOpt(Option opt) {
        return fileUploadService.validateFileOptConfig(opt, opt.errors)
    }
    /**
     * Validate the Option, return any output parameters in a map
     * @param opt the option
     * @param params input params if any
     * @deprecated
     */
    @Deprecated
    @CompileStatic
    public static Map _validateOption(Option opt, UserDataProvider udp, ScheduledExecution scheduledExecution, JobOptionConfigRemoteUrl configRemoteUrl, Map params , boolean jobWasScheduled) {
        opt.validate(deepValidate: false)
        opt.convertValuesList()
        def validator = new DomainValidatorShim(opt.errors)
        _validateOption(opt, validator, configRemoteUrl, params?.valuesType?.toString(), jobWasScheduled)
        return validator.results
    }

    @CompileStatic
    static class DomainValidatorShim implements OptionValidator{
        Errors errors
        Map<String,String> results=[:]
        DomainValidatorShim(Errors errors){
            this.errors = errors
        }

        @Override
        void rejectValue(final String prop, final String messageCode) {
            errors.rejectValue(prop,messageCode)
        }

        @Override
        void rejectValue(
            final String prop,
            final String messageCode,
            final Object[] args,
            final String defaultMessage
        ) {
            errors.rejectValue(prop,messageCode,args,defaultMessage)
        }

        @Override
        void registerErrorMessage(final String prop, final String message) {
            results.put(prop,message)
        }
    }
    @CompileStatic
    static interface OptionValidator{
        void rejectValue(String prop, String messageCode)
        void rejectValue(String prop, String messageCode, Object[] args, String defaultMessage)
        void registerErrorMessage(String prop, String message)
    }

    @CompileStatic
    static void _validateOption(
        OptionData opt,
        OptionValidator validator,
        JobOptionConfigRemoteUrl configRemoteUrl,
        String valuesType,
        boolean jobWasScheduled
    ) {
        if (jobWasScheduled && opt.required && opt.optionType == 'file') {
            validator.rejectValue('required', 'option.file.required.message')
            return
        }

        if (opt.hidden && !opt.defaultValue && !opt.defaultStoragePath) {
            validator.rejectValue('hidden', 'option.hidden.notallowed.message')
            return
        }
        if (opt.enforced && (opt.optionValues || opt.valuesList) && opt.defaultValue) {
            if(!opt.multivalued && !opt.optionValues.contains(opt.defaultValue)) {
                validator.rejectValue('defaultValue', 'option.defaultValue.notallowed.message')
            }else if(opt.multivalued && opt.delimiter){
                //validate each default value
                def found = opt.defaultValue.split(Pattern.quote(opt.delimiter)).find{ !opt.optionValues.contains(it) }
                if(found){
                    validator.rejectValue('defaultValue', 'option.defaultValue.multivalued.notallowed.message',[found] as Object[],"{0} invalid value")
                }
            }
        }
        if (opt.enforced && (!opt.optionValues && !opt.valuesList && !opt.realValuesUrl && !opt.optionValuesPluginType)) {
            if (valuesType == 'url') {
                validator.rejectValue('valuesUrlLong', 'option.enforced.emptyvalues.message')
            } else {
                validator.rejectValue('valuesList', 'option.enforced.emptyvalues.message')
            }
        }
        if (opt.regex) {
            //try to parse regular expression syntax
            try {
                Pattern.compile(opt.regex)
            } catch (PatternSyntaxException e) {
                validator.registerErrorMessage('regexError', e.message)
                validator.rejectValue('regex', 'option.regex.invalid.message', [opt.regex] as Object[], "Invalid Regex: {0}")
            }
            if (opt.optionValues || opt.valuesList) {
                def inval = []
                opt.optionValues.each {val ->
                    if (!(val =~ /${opt.regex}/)) {
                        validator.rejectValue('valuesList', 'option.values.regexmismatch.message', [val.toString(), opt.regex] as Object[], "Value does not match regex: {0}")
                    }
                }
            }
            if (opt.defaultValue) {
                if (!(opt.defaultValue =~ /${opt.regex}/)) {
                    validator.rejectValue('defaultValue', 'option.defaultValue.regexmismatch.message', [opt.defaultValue, opt.regex] as Object[], "Default value does not match regex: {0}")
                }
            }
        }
        if(opt.multivalued && !opt.delimiter){
            validator.rejectValue('delimiter', 'option.delimiter.blank.message')
        }
        if(opt.multivalued && opt.secureInput){
            validator.rejectValue('multivalued', 'option.multivalued.secure-conflict.message')
        }
        if(jobWasScheduled && opt.required && !(opt.defaultValue||opt.defaultStoragePath||opt.realValuesUrl)){
            validator.rejectValue('defaultValue', 'option.defaultValue.required.message')
        }
        if(opt.realValuesUrl && configRemoteUrl?.getJsonFilter()){
            try{
                JsonPath.compile(configRemoteUrl?.getJsonFilter())
            } catch (Exception e){
                validator.rejectValue('configRemoteUrl.jsonFilter', 'form.option.valuesType.url.filter.error.label')
            }
        }
    }

        /**
     * Use input parameters to configure an Option object.
     * Special properties "valuesType" and "enforcedType" configure
     * the option when mutually exclusive properties are in the input parameters.
     * @param opt the input Option
     * @param params input map of parameters
     */
    protected Option _setOptionFromParams (Option opt, Map params ){
        def valuesUrl
        if (params.valuesType == 'list') {
            params.remove('valuesUrl')
        } else if (params.valuesType == 'url') {
            params.values = null
            params.valuesList = null
            valuesUrl = params.valuesUrl

            if(params.remoteUrlAuthenticationType || params.remoteUrlJsonFilter){
                JobOptionConfigRemoteUrl jobOptionConfigRemoteUrl = new JobOptionConfigRemoteUrl()

                if(params.remoteUrlAuthenticationType){
                    jobOptionConfigRemoteUrl.authenticationType = RemoteUrlAuthenticationType.valueOf(params.remoteUrlAuthenticationType)

                    if(jobOptionConfigRemoteUrl.authenticationType == RemoteUrlAuthenticationType.BASIC){
                        jobOptionConfigRemoteUrl.username = params.remoteUrlUsername
                        jobOptionConfigRemoteUrl.passwordStoragePath = params.remoteUrlPassword
                    }

                    if(jobOptionConfigRemoteUrl.authenticationType == RemoteUrlAuthenticationType.API_KEY){
                        jobOptionConfigRemoteUrl.keyName = params.remoteUrlKey
                        jobOptionConfigRemoteUrl.tokenStoragePath = params.remoteUrlToken
                        jobOptionConfigRemoteUrl.apiTokenReporter = ApiTokenReporter.valueOf(params.remoteUrlApiTokenReporter)
                    }

                    if(jobOptionConfigRemoteUrl.authenticationType == RemoteUrlAuthenticationType.BEARER_TOKEN){
                        jobOptionConfigRemoteUrl.tokenStoragePath = params.remoteUrlBearerToken
                    }
                }

                jobOptionConfigRemoteUrl.jsonFilter = params.remoteUrlJsonFilter?:null

                JobOptionConfigData jobOptionConfigData = new JobOptionConfigData()
                jobOptionConfigData.addConfig(jobOptionConfigRemoteUrl)
                opt.setOptionConfigData(jobOptionConfigData)
            }else{
                if(opt.getConfigRemoteUrl()!=null){
                    opt.setOptionConfigData(null)
                }
            }
        }

        if (params.enforcedType == 'none') {
            params.regex = null
            params.enforced = false
        } else if (params.enforcedType == 'enforced') {
            params.regex = null
            params.enforced = true
        } else if (params.enforcedType == 'regex') {
            params.enforced = false
        } else {
            params.enforced = false
        }
        if(null==params.required){
            params.required=false
        }
        if(null==params.multivalued){
            params.multivalued=false
        }
        if(null==params.secureInput){
            params.secureInput=false
        }
        if(null==params.secureExposed){
            params.secureExposed=false
        }
        if (params.inputType=='plain') {
            params.secureInput = false
            params.secureExposed = false
            params.isDate = false
        }else if (params.inputType=='secure') {
            params.secureInput = true
            params.secureExposed = false
            params.isDate = false
        }else if (params.inputType=='secureExposed') {
            params.secureInput = true
            params.secureExposed = true
            params.isDate = false
        }else if (params.inputType=='date') {
            params.secureInput = false
            params.secureExposed = false
            params.isDate = true
        }
        if(null==params.sortValues){
            params.sortValues=false
        }
        opt.properties = params
        if (params.optionType && params.configMap) {
            opt.configMap = params.configMap
        }

        opt.valuesList = params.valuesList?:null
        if(params.valuesType == 'list'){
            opt.realValuesUrl=null
        }else if(params.valuesType == 'url'){
            opt.valuesList=null
            if(valuesUrl){
                opt.realValuesUrl=new URL(valuesUrl)
            }else{
                opt.realValuesUrl = null
            }
        } else {
            opt.optionValuesPluginType = params.valuesType
        }

        opt.convertValuesList()
        return opt
    }

    /**
     * Use an Options props to configure params to be memoized for later configuring an Option
     */
    private Map _getParamsFromOption (Option opt){
        def params = [:]
        params.putAll(opt.properties)
        if (opt.valuesList) {
            params.valuesType = 'list'
        } else if (params.valuesUrl) {
            params.valuesType = 'url'
            params.valuesUrl = opt.realValuesUrl?.toExternalForm()
            params.remove('valuesUrlLong')
            params.remove('realValuesUrl')
        }
        if (opt.regex) {
            params.enforcedType = 'regex'
        } else if (opt.enforced) {
            params.enforcedType = 'enforced'
        } else {
            params.enforcedType = 'none'
        }
        if (!params.secureInput){
            params.inputType = 'plain'
        } else if (!params.secureExposed) {
            params.inputType='secure'
        } else if (params.secureExposed){
            params.inputType = 'secureExposed'
        }
        if(params.isDate){
            params.inputType='date'
        }
        params.valuesList = opt.produceValuesList()
        ['values','mapping','log','errors','class', 'metaClass', 'constraints', 'belongsTo', 'scheduledExecution', 'hasMany'].each{params.remove(it)}
        return params
    }

    /**
     *  Load the options stored for the session, or store the specified options map in the session
     */
    private Map _getSessionOptions(Map usedopts = null){
        return getSessionOptions(session,params,usedopts)
    }
    /**
     *  Load the options stored for the session, or store the specified options map in the session
     * @param session the session
     * @param params input parameters
     * @param usedopts options map to store (optional)
     */
    public static Map getSessionOptions (session,params,Map usedopts = null){
        def optid = '_new'
        def Map editopts
        if (!session.editOPTS) {
            session.editOPTS = [:]
        }
        if (params?.scheduledExecutionId) {
            optid = params.scheduledExecutionId
            if (null == session.editOPTS[optid]) {
                ScheduledExecution sched
                if(params?.sched){
                    sched = params.sched
                }else{
                    sched = ScheduledExecution.getByIdOrUUID(params.scheduledExecutionId)
                }
                if (!sched) {
                    session.editOPTS[optid] = [:]
                }else if (sched.options) {
                    editopts = [:]

                    sched.options.each {Option opt ->
                        def cloneOpt = opt.createClone()
                        cloneOpt.getOptionValues()
                        editopts[opt.name] = cloneOpt
                    }
                    session.editOPTS[optid] = editopts
                } else {
                    session.editOPTS[optid] = [:]
                }
            }
        } else if (usedopts) {
            //load from existing execution
            session.editOPTS[optid] = usedopts
        } else if (null == session.editOPTS[optid]) {
            session.editOPTS[optid] = [:]
        }
        editopts = session.editOPTS[optid]
        return editopts
    }

    /** UNDO mechanism  **/

    public static int UNDO_MAX = 20;

    /**
     * push action set to undo the options.
     * @param id id of options to use
     * @param input action map
     */
    private def _pushUndoAction (String id, Map input){
        if (!input) {
            return
        }
        if (!session.undoOPTS) {
            session.undoOPTS = [:]
            session.undoOPTSstate = [:]
        }
        def uid = id ? id : '_new'
        if (!session.undoOPTS[uid]) {
            session.undoOPTS[uid] = [input]
        } else {
            session.undoOPTS[uid] << input
        }
        if (session.undoOPTS[uid].size() > UNDO_MAX) {
            session.undoOPTS[uid].remove(0);
        }
        session.undoOPTSstate[uid]=true
    }

    /**
     * pop action from undo stack
     * @param id id of options to use
     * @return undo action map if it exists
     */
    private def _popUndoAction (id){
        if (!id) {
            id = '_new'
        }
        if (session.undoOPTS && session.undoOPTS[id]) {
            return session.undoOPTS[id].removeLast()
        }
        return null
    }
    /**
     * push action set to redo stack
     * @param id id of options to use
     * @param input action map
     */
    private def _pushRedoAction (id, Map input){
        if (!input) {
            return
        }
        if (!session.redoOPTS) {
            session.redoOPTS = [:]
        }
        if (!session.undoOPTSstate) {
            session.undoOPTSstate = [:]
        }
        def uid = id ? id : '_new'
        if (!session.redoOPTS[uid]) {
            session.redoOPTS[uid] = [input]
        } else {
            session.redoOPTS[uid] << input
        }
        if (session.redoOPTS[uid].size() > UNDO_MAX) {
            session.redoOPTS[uid].remove(0);
        }
        session.undoOPTSstate[uid]=false
    }
    /**
     * pop action set from the undo stack
     * @param id id of options to use
     * @return action map
     */
    private def _popRedoAction (id){
        if (!id) {
            id = '_new'
        }
        if (session.redoOPTS && session.redoOPTS[id]) {
            return session.redoOPTS[id].removeLast()
        }
        return null
    }

    /**
     * Clear the redo stack for the id
     * @param id id of options to use
     */
    private void _clearRedoStack(id){
        if (!id) {
            id = '_new'
        }
        if (session.redoOPTS && session.redoOPTS[id]) {
            session.redoOPTS.remove(id)
        }
    }

    private def _duplicateOption(Map editopts){
        Option option = editopts[params.name]
        Option newOption = option.createClone()

        def duplicateName
        duplicateName = { name, indx, options->
            def newName = name + "_" + indx
            def duplicated = ""
            options.each {key, value->
                if(key == newName){
                    duplicated = key
                    return
                }
            }

            if(duplicated){
                return duplicateName(name,indx+1 ,options)
            }
            return newName

        }
        def newName = duplicateName(params.name, 1, editopts)
        newOption.name = newName

        if(newOption.sortIndex != null)
            newOption.sortIndex++

        def result = _applyOptionAction(editopts, [action: 'insert', name: newName, params: _getParamsFromOption(newOption)])

        return [actions: result, name: newName]
    }

}

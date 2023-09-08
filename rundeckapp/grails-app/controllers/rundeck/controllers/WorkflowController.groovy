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

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import grails.converters.JSON
import groovy.transform.PackageScope
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.rundeck.app.spi.Services
import org.rundeck.core.auth.AuthConstants
import rundeck.*
import rundeck.services.ConfigurationService
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.PluginService
import rundeck.services.ScheduledExecutionService
import rundeck.services.StorageService

import javax.servlet.http.HttpServletResponse

class WorkflowController extends ControllerBase {
    def frameworkService
    PluginService pluginService
    ConfigurationService configurationService;
    StorageService storageService
    ScheduledExecutionService scheduledExecutionService
    AuthorizedServicesProvider rundeckAuthorizedServicesProvider
    static allowedMethods = [
            redo:'POST',
            remove:'POST',
            reorder:'POST',
            revert:'POST',
            save:'POST',
            undo:'POST',
            dashboard: 'POST',
            renderDashboard: 'GET'
    ]
    def index = {
        return redirect(controller: 'menu', action: 'index')
    }

    /**
     *
     * @param id
     * @param actions @param strings @return
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
            rundeckAuthContextProcessor.authorizeProjectJobAny(
                authContext,
                scheduledExecution,
                actions,
                scheduledExecution.project
            ), actions[0], 'Job', id
        )
    }

    def renderDashboard() {
        def renderDashboardsInGUI = configurationService.getBoolean('gui.stepsDashboard', true);
        render(
                contentType: 'application/json', text:
                (
                        [
                                render           : renderDashboardsInGUI,
                        ]
                ) as JSON
        )
    }

    def dashboard() {
        Workflow modelWorkflow = null;
        Workflow editWf = _getSessionWorkflow()

        def scheduledExecution = scheduledExecutionService.getByIDorUUID(params.scheduledExecutionId)
        def dbWf = scheduledExecution?.workflow

        if (!editWf && dbWf) {
            session.removeAttribute('editWF');
            session.removeAttribute('undoWF');
            session.removeAttribute('redoWF');
            modelWorkflow = dbWf
        } else {
            modelWorkflow = editWf
        }

        if (dbWf && editWf) {
            modelWorkflow = new Workflow()
            if( dbWf.commands.size() > editWf.commands.size() ){
                modelWorkflow.setCommands(dbWf.commands);
            }else{
                modelWorkflow.setCommands(editWf.commands);
            }
        }

        return render(template: "/execution/stepsDashboard", model: [workflow: modelWorkflow]
        )
    }

    /**
     * Render the edit form for a workflow item
     */
    def edit() {
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        if (!params.num && !params['newitemtype']) {
            log.error("num parameter is required")
            return renderErrorFragment("num parameter is required")
        }

        def Workflow editwf = _getSessionWorkflow()
        def numi;
        if (params.num) {
            numi = Integer.parseInt(params.num)
            if (numi >= editwf.commands.size()) {
                log.error("num parameter is invalid: ${numi}")
                return renderErrorFragment("num parameter is invalid: ${numi}")
            }
        }
        def item = null != numi ? editwf.commands.get(numi) : null
        final isErrorHandler = params.isErrorHandler == 'true'

        if(isErrorHandler){
            if(params['newitemtype']){
                item=null
            }else{
                item = item.errorHandler
                if(!item){
                    log.error("num parameter is invalid: ${numi} no error handler")
                    return renderErrorFragment("num parameter is invalid: ${numi} no error handler")
                }
            }
        }
        String newitemtype = params['newitemtype']
        String origitemtype
        def newitemDescription
        def dynamicProperties
        AuthContext auth = rundeckAuthContextProcessor.getAuthContextForSubject(request.subject)
        if(item && item.instanceOf(PluginStep)){
            newitemDescription = getPluginStepDescription(item.nodeStep, item.type)
            origitemtype=item.type
            dynamicProperties = getDynamicProperties(params.project,
                                                     origitemtype,
                                                     item.nodeStep,
                                                     rundeckAuthorizedServicesProvider.getServicesWith(auth)
            )
        } else if (item) {
            if(item.instanceOf(JobExec)){
                origitemtype='job'
            }else if(item.instanceOf(CommandExec)){
                origitemtype=item.adhocLocalString?'script':item.adhocRemoteString?'command':'scriptfile'
            }
        } else if (newitemtype && !(newitemtype in ['command', 'script', 'scriptfile', 'job'])) {
            newitemDescription = getPluginStepDescription(params.newitemnodestep == 'true', newitemtype)
            dynamicProperties = getDynamicProperties(
                params.project,
                newitemtype,
                params.newitemnodestep == 'true',
                rundeckAuthorizedServicesProvider.getServicesWith(auth)
            )
        }
        def fprojects = frameworkService.projectNames(auth).findAll{it != params.project}

        [
                item                : item,
                dynamicProperties   : dynamicProperties,
                key                 : params.key,
                num                 : numi,
                scheduledExecutionId: params.scheduledExecutionId,
                newitemtype         : newitemtype,
                origitemtype        : origitemtype,
                newitemDescription  : newitemDescription,
                pluginNotFound      : null == newitemDescription,
                edit                : true,
                isErrorHandler      : isErrorHandler,
                newitemnodestep     : params.newitemnodestep,
                fprojects           : fprojects,
                project             : params.project
        ]
    }
    /**
     * Reorder items
     */
    def copy() {
        withForm {
            if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
                return
            }
            if (!params.num) {
                log.error("num parameter required")
                return renderErrorFragment("num parameter required")
            }

            def Workflow editwf = _getSessionWorkflow()

            def num = Integer.parseInt(params.num)
            def result = _applyWFEditAction(editwf, [action: 'copy', num: num])
            if (result.error) {
                log.error(result.error)
                return renderErrorFragment(result.error)
            }
            _pushUndoAction(params.scheduledExecutionId, result.undo)
            _clearRedoStack(params.scheduledExecutionId)

            return render(template: "/execution/wflistContent", model: [
                    workflow : editwf,
                    edit     : params.edit,
                    highlight: num + 1,
                    project  : params.project
            ]
            )
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return renderErrorFragment(g.message(code: 'request.error.invalidtoken.message'))
        }
    }

    def editStepFilter() {
        if (!params.index && !params.newfiltertype) {
            log.error("index parameter is required")
            return renderErrorFragment("index parameter is required")
        }

        def filterPlugins = pluginService.listPlugins(LogFilterPlugin)
        def newfilterdesc
        def filtertype
        def config = [:]
        if (params.index != null) {
            if (!params.num) {
                log.error("num parameter is required")
                return renderErrorFragment("num parameter is required")
            }

            if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
                return
            }
            def Workflow editwf = _getSessionWorkflow()
            def numi = Integer.parseInt(params.num);
            if (numi >= editwf.commands.size()) {
                log.error("num parameter is invalid: ${numi}")
                return renderErrorFragment("num parameter is invalid: ${numi}")
            }
            def item = null != numi ? editwf.commands.get(numi) : null
            def pluginconfigs = item.getPluginConfigForType('LogFilter')
            def filteri = Integer.parseInt(params.index);
            if (!pluginconfigs || !(pluginconfigs instanceof List && pluginconfigs[filteri])) {
                log.error("index parameter is invalid: ${filteri}")
                return renderErrorFragment("index parameter is invalid: ${filteri}")
            }
            filtertype = pluginconfigs[filteri].type
            config = pluginconfigs[filteri].config
        } else {
            filtertype = params.newfiltertype
        }
        newfilterdesc = filterPlugins[filtertype].description
        def validation
        if (params.validate) {
            config = params.pluginConfig
            validation = _validateLogFilter(config, filtertype)
        } else if (params.editconfig) {
            if(request.JSON){
                config=request.JSON.pluginConfig
            }else{
                config = params.pluginConfig
            }
        }
        [
                num          : params.num,
                index        : params.index,
                type         : filtertype,
                description  : newfilterdesc,
                config       : config,
                newfiltertype: params.newfiltertype,
                report       : validation?.report,
                valid        : validation?.valid
        ]
    }

    def saveStepFilter() {
        def results
        def valid = false
        withForm {
            g.refreshFormTokensHeader()
            if (!params.num) {
                log.error("num parameter is required")
                return renderErrorFragment("num parameter is required")
            }
            if (!params.index && !params.newfiltertype) {
                log.error("index parameter is required")
                return renderErrorFragment("index parameter is required")
            }

            if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
                return
            }
            def Workflow editwf = _getSessionWorkflow()
            def numi = Integer.parseInt(params.num);
            if (numi >= editwf.commands.size()) {
                log.error("num parameter is invalid: ${numi}")
                return renderErrorFragment("num parameter is invalid: ${numi}")
            }
            def indexi = null != params.index ? Integer.parseInt(params.index) : -1;

            def actionName = params.newfiltertype ? 'insertFilter' : 'modifyFilter'

            Map config = params.pluginConfig


            def result = _applyWFEditAction(
                    editwf,
                    [action    : actionName,
                     num       : numi,
                     index     : indexi,
                     filtertype: params.newfiltertype ?: params.type,
                     config    : config]
            )
            if (result.error) {
                log.error(result.error)
            } else {
                _pushUndoAction(params.scheduledExecutionId, result.undo)
                if (result.undo) {
                    _clearRedoStack(params.scheduledExecutionId)
                }
            }

            results = [error: result.error, valid: !result.error, saved: result.saved]
            valid = true
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return renderErrorFragment(g.message(code: 'request.error.invalidtoken.message'))
        }

        if (valid) {
            return respond(results, formats: ['json'])
        }
    }

    def validateStepFilter() {
        if (!params.newfiltertype) {
            log.error("newfiltertype parameter is required")
            return renderErrorFragment("newfiltertype parameter is required")
        }

        Map config = params.pluginConfig

        def validation = _validateLogFilter(config, params.newfiltertype)

        Map results = [
                report: validation?.report,
                valid : validation?.valid,
        ]
        if (validation?.valid) {
            results.saved = [
                    type  : params.newfiltertype,
                    config: validation.props
            ]
        }
        return respond(results, formats: ['json'])
    }

    def removeStepFilter() {
        Map results
        def valid = false
        withForm {
            g.refreshFormTokensHeader()
            if (!params.num) {
                log.error("num parameter is required")
                return renderErrorFragment("num parameter is required")
            }
            if (!params.index && !params.newfiltertype) {
                log.error("index parameter is required")
                return renderErrorFragment("index parameter is required")
            }

            if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
                return
            }
            def Workflow editwf = _getSessionWorkflow()
            def numi = Integer.parseInt(params.num);
            if (numi >= editwf.commands.size()) {
                log.error("num parameter is invalid: ${numi}")
                return renderErrorFragment("num parameter is invalid: ${numi}")
            }
            def indexi =  Integer.parseInt(params.index)

            def actionName = 'removeFilter'

            def result = _applyWFEditAction(
                    editwf,
                    [action: actionName,
                     num   : numi,
                     index : indexi
                    ]
            )
            if (result.error) {
                log.error(result.error)
            } else {
                _pushUndoAction(params.scheduledExecutionId, result.undo)
                if (result.undo) {
                    _clearRedoStack(params.scheduledExecutionId)
                }
            }

            results = [error: result.error, valid: !result.error]
            valid = true
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return renderErrorFragment(g.message(code: 'request.error.invalidtoken.message'))
        }

        if (valid) {
            return respond(results,formats: ['json'])
        }
    }
    /**
     * Return a Description for a plugin type, if available
     * @param type
     * @param framework
     * @return
     */
    private Description getPluginStepDescription(boolean isNodeStep, String type) {
        if (type && !(type in ['command', 'script', 'scriptfile', 'job'])) {
            try {
                return isNodeStep ? frameworkService.getNodeStepPluginDescription(type) :
                        frameworkService.getStepPluginDescription(type)
            } catch (MissingProviderException e) {
                log.warn("step provider not found: ${type}: ${e.message}", e)
            }
        }
        return null
    }
    /**
     * Return a Description for a plugin type, if available
     * @param type
     * @param framework
     * @return
     */
    static private Description getPluginStepDescription(
            FrameworkService frameworkService,
            boolean isNodeStep,
            String type
    )
    {
        if (type && !(type in ['command', 'script', 'scriptfile', 'job'])) {
            try {
                return isNodeStep ? frameworkService.getNodeStepPluginDescription(type) :
                        frameworkService.getStepPluginDescription(type)
            } catch (MissingProviderException e) {
                return null
            }
        }
        return null
    }

    /**
     * Render workflow item
     */
    def renderItem() {
        if (!params.num) {
            log.error("num parameter is required")
            return renderErrorFragment("num parameter is required")
        }

        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        def Workflow editwf = _getSessionWorkflow()
        def numi = Integer.parseInt(params.num)
        if (numi >= editwf.commands.size()) {
            log.error("num parameter is invalid: ${numi}")
            return renderErrorFragment("num parameter is invalid: ${numi}")
        }
        def item = editwf.commands.get(numi)
        final isErrorHandler = params.isErrorHandler == 'true'
        if (isErrorHandler) {
            item = item.errorHandler
            if (!item) {
                log.error("num parameter is invalid: ${numi} no error handler")
                return renderErrorFragment("num parameter is invalid: ${numi} no error handler")
            }
        }
        def itemDescription = item.instanceOf(PluginStep)?getPluginStepDescription(item.nodeStep, item.type):null
        return render(template: "/execution/wflistitemContent", model: [workflow: editwf, item: item, i: params.key, stepNum:numi, scheduledExecutionId: params.scheduledExecutionId, edit: params.edit, isErrorHandler: isErrorHandler, itemDescription: itemDescription])
    }


    /**
     * Save workflow item
     */
    def save() {
        withForm{
            g.refreshFormTokensHeader()
        if (!params.num && !params.newitem) {
            log.error("num parameter is required")
            return renderErrorFragment("num parameter is required")
        }
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        def Workflow editwf = _getSessionWorkflow()
        def item
        def numi
        def wfEditAction = 'true' == params.newitem ? 'insert' : 'modify'
        AuthContext auth = rundeckAuthContextProcessor.getAuthContextForSubject(request.subject)
        def fprojects = frameworkService.projectNames(auth).findAll{it != params.project}
        if (null != params.num) {
            try {
                numi = Integer.parseInt(params.num)
            } catch (NumberFormatException e) {
                log.error("num parameter is invalid: " + params.num)
                flash.'error' = "num parameter is invalid: " + params.num
                return render(template: "/execution/wfitemEdit", model: [item: null, num: numi, scheduledExecutionId: params.scheduledExecutionId, newitemtype: params['newitemtype'], edit: true, fprojects: fprojects])
            }
        } else {
            numi = editwf.commands ? editwf.commands.size() : 0
        }
        final isErrorHandler = params.isErrorHandler == 'true'
        if (isErrorHandler) {
            wfEditAction = 'true' == params.newitem ? 'addHandler' : 'modifyHandler'
        }
        def result = _applyWFEditAction(
                editwf,
                [action: wfEditAction, num: numi, params: params, project: params.project]
        )
        if (result.error) {
            log.error(result.error)
            item=result.item

            def itemDescription
            def dynamicProperties
            if(item && item.instanceOf(PluginStep)){
                itemDescription = getPluginStepDescription(item.nodeStep, item.type)
                dynamicProperties = getDynamicProperties(params.project,
                        item.type,
                        item.nodeStep,
                        rundeckAuthorizedServicesProvider.getServicesWith(auth))
            }

            def newitemtype = params['newitemtype']
            def origitemtype = params['origitemtype']

            return render(
                    template: "/execution/wfitemEdit",
                    model: [
                            item                : result.item,
                            dynamicProperties   : dynamicProperties,
                            key                 : params.key,
                            num                 : params.num,
                            scheduledExecutionId: params.scheduledExecutionId,
                            newitemtype         : newitemtype,
                            origitemtype        : origitemtype,
                            edit                : true,
                            isErrorHandler      : isErrorHandler,
                            newitemDescription  : itemDescription,
                            report              : result.report,
                            fprojects           : fprojects
                    ]
            )
        }
        _pushUndoAction(params.scheduledExecutionId, result.undo)
        if (result.undo) {
            _clearRedoStack(params.scheduledExecutionId)
        }

        item = editwf.commands.get(numi)
        if(isErrorHandler){
            item=item.errorHandler
        }
        def itemDescription = item.instanceOf(PluginStep) ? getPluginStepDescription(item.nodeStep, item.type) : null
        return render(template: "/execution/wflistitemContent", model: [workflow: editwf, item: item, i: params.key, stepNum: numi, scheduledExecutionId: params.scheduledExecutionId, edit: true,isErrorHandler: isErrorHandler,itemDescription: itemDescription])
        }.invalidToken {
            response.status=HttpServletResponse.SC_BAD_REQUEST
            return renderErrorFragment(g.message(code: 'request.error.invalidtoken.message'))
        }
    }



    /**
     * Reorder items
     */
    def reorder() {
        withForm{
        if (!params.fromnum) {
            log.error("fromnum parameter required")
            return renderErrorFragment("fromnum parameter required")
        }
        if (!params.tonum) {
            log.error("tonum parameter required")
            return renderErrorFragment("tonum parameter required")
        }

        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        def Workflow editwf = _getSessionWorkflow()

        def fromi = Integer.parseInt(params.fromnum)
        def toi = Integer.parseInt(params.tonum)
        def result = _applyWFEditAction(editwf, [action: 'move', from: fromi, to: toi])
        if (result.error) {
            log.error(result.error)
            return renderErrorFragment(result.error)
        }
        _pushUndoAction(params.scheduledExecutionId, result.undo)
        _clearRedoStack(params.scheduledExecutionId)

        return render(template: "/execution/wflistContent", model: [workflow: editwf, edit: params.edit, highlight: toi,
                project: params.project])
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return renderErrorFragment(g.message(code: 'request.error.invalidtoken.message'))
        }
    }

    /**
     * Remove an item
     */
    def remove = {
        withForm{
        if (!params.delnum) {
            log.error("delnum parameter required")
            return renderErrorFragment("delnum parameter required")
        }
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        def Workflow editwf = _getSessionWorkflow()

        def fromi = Integer.parseInt(params.delnum)
        def wfEditAction = 'remove'
        final isErrorHandler = params.isErrorHandler == 'true'
        if(isErrorHandler){
            wfEditAction='removeHandler'
        }

        def result = _applyWFEditAction(editwf, [action: wfEditAction, num: fromi])
        if (result.error) {
            log.error(result.error)
            return renderErrorFragment(result.error)
        }
        _pushUndoAction(params.scheduledExecutionId, result.undo)
        _clearRedoStack(params.scheduledExecutionId)

        return render(template: "/execution/wflistContent", model: [workflow: editwf, edit: params.edit, project: params.project])
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return renderErrorFragment(g.message(code: 'request.error.invalidtoken.message'))
        }
    }


    /**
     * Undo change
     */
    def undo() {
        withForm{
        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        def Workflow editwf = _getSessionWorkflow()
        def action = _popUndoAction(params.scheduledExecutionId)

        def num
        if (action) {
            def result = _applyWFEditAction(editwf, action)
            if (result.error) {
                log.error(result.error)
                return renderErrorFragment(result.error)
            }
            if (null != action.num) {
                num = action.num
            } else if (null != action.to) {
                num = action.to
            }
            if(action.action in ['addHandler','removeHandler','modifyHandler']){
                num='eh_'+num
            }
            _pushRedoAction(params.scheduledExecutionId, result.undo)
        }

        return render(template: "/execution/wflistContent", model: [workflow: editwf, edit: params.edit, highlight: num,
                project: params.project])
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return renderErrorFragment(g.message(code: 'request.error.invalidtoken.message'))
        }
    }



    /**
     * redo change
     */
    def redo() {
        withForm{

        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        def Workflow editwf = _getSessionWorkflow()
        def action = _popRedoAction(params.scheduledExecutionId)

        def num
        if (action) {
            def result = _applyWFEditAction(editwf, action)
            if (result.error) {
                log.error(result.error)
                return renderErrorFragment(result.error)
            }
            if (null != action.num) {
                num = action.num
            } else if (null != action.to) {
                num = action.to
            }
            _pushUndoAction(params.scheduledExecutionId, result.undo)
        }

        return render(template: "/execution/wflistContent", model: [workflow: editwf, edit: params.edit, highlight: num, project: params.project])
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return renderErrorFragment(g.message(code: 'request.error.invalidtoken.message'))
        }
    }

    /**
     * revert all changes
     */
    def revert() {
        withForm{

        if(!allowedJobAuthorization(params.scheduledExecutionId, [AuthConstants.ACTION_UPDATE])){
            return
        }
        final String uid = params.scheduledExecutionId ? params.scheduledExecutionId : '_new'
        session.editWF?.remove(uid)
        session.undoWF?.remove(uid)
        session.redoWF?.remove(uid)
        def Workflow editwf = _getSessionWorkflow()

        return render(template: "/execution/wflistContent", model: [workflow: editwf, edit: true,
                project: params.project])
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return renderErrorFragment(g.message(code: 'request.error.invalidtoken.message'))
        }
    }


    /**
     * display undo/redo buttons for current stack state
     */
    def renderUndo() {
        final String id = params.scheduledExecutionId ? params.scheduledExecutionId : '_new'
        return render(
                template: '/common/undoRedoControls',
                model: [
                        undo         : session.undoWF ? session.undoWF[id]?.size() : 0,
                        redo         : session.redoWF ? session.redoWF[id]?.size() : 0,
                        key          : 'workflow',
                        highlightundo: session.undoWFstate?.get(id)
                ]
        )
    }

    /**
     * For each value in the map, replace \r\n line endings with \n
     * @param input
     * @return
     */
    private Map cleanLineEndings(Map input){
        def result = [:]
        input.each {k,v->
            if(v instanceof String)result[k]=v.replaceAll(/\r?\n/,'\n')
            else if(v instanceof String[] || v instanceof Collection) result[k]=v.collect { it.replaceAll(/\r?\n/,'\n') }.join(",")
        }
        result
    }
    /**
     *
     * handles ALL modifications to the workflow via named actions, input in a map:
     * input map:
     * action: name of action 'move','remove','insert','modify','removeHandler','addHandler','modifyHandler'
     * num: item index to affect
     * from/to: (move action) item index to move from and to
     * params: properties of the item (insert,modify,addHandler,modifyHandler actions)
     *
     *  Returns result map:
     *
     * error: any error message
     * undo: corresponding undo action map
     */
    protected Map _applyWFEditAction (Workflow editwf, Map input){
        def result = [:]
        def createItemFromParams={params->
            def item
            if (params.pluginItem) {
                item = new PluginStep()
                item.keepgoingOnSuccess=params.keepgoingOnSuccess
                item.type = params.newitemtype
                item.nodeStep = params.newitemnodestep == 'true'
                item.configuration = cleanLineEndings(params.pluginConfig)
                item.description = params.description
            } else if (params.jobName || 'job' == params.newitemtype) {
                item = new JobExec(params)
                if (params.nodeStep instanceof String) {
                    item.nodeStep = params.nodeStep == 'true'
                }
            } else {
                item = new CommandExec(params)

                def optsmap = ExecutionService.filterOptParams(params)
                if (optsmap) {
                    item.argString = ExecutionService.generateArgline(optsmap)
                    //TODO: validate input options
                }
            }
            item
        }
        def modifyItemFromParams={moditem,params->
            if (params.pluginItem) {
                if (!params.keepgoingOnSuccess) {
                    params.keepgoingOnSuccess = 'false'
                }
                moditem.properties=params.subMap(['keepgoingOnSuccess','description'])
                moditem.configuration = cleanLineEndings(params.pluginConfig)
            } else {
                if(params.nodeStep instanceof String) {
                    params.nodeStep = params.nodeStep == 'true'
                }
                moditem.properties = params
                if (params.jobName) {
                    moditem.nodeStep=params.nodeStep
                }
                def optsmap = ExecutionService.filterOptParams(input.params)
                if (optsmap) {
                    moditem.argString = ExecutionService.generateArgline(optsmap)
                    //TODO: validate input options
                }
            }

        }
        AuthContext auth = rundeckAuthContextProcessor.getAuthContextForSubject(request.subject)
        def fprojects = frameworkService.projectNames(auth)

        if (input.action == 'move') {
            def fromi = input.from
            def toi = input.to
            if (fromi >= editwf.commands.size() || fromi < 0) {
                result.error = "fromnum parameter is invalid: ${fromi}"
                return result
            } else if (toi > editwf.commands.size() || toi < 0) {
                result.error = "tonum parameter is invalid: ${toi}"
                return result
            } else if (toi == fromi) {
                return result
            }
            synchronized (editwf.commands) {
                def wfitem = editwf.commands.remove(fromi)
                editwf.commands.add(toi, wfitem)
            }
            result['undo'] = [action: 'move', from: toi, to: fromi]
        } else if (input.action == 'copy') {
            def num = input.num
            def newi = num + 1
            if (num >= editwf.commands.size() || num < 0) {
                result.error = "fromnum parameter is invalid: ${num}"
                return result
            }

            def origStep = editwf.commands.get(num)
            def wfitem = origStep.createClone()
            if(origStep.errorHandler){
                wfitem.errorHandler=origStep.errorHandler.createClone()
            }
            editwf.commands.add(newi, wfitem)
            result['undo'] = [action: 'remove', num: newi]
        } else if (input.action == 'remove') {
            def numi = input.num
            def item = editwf.commands.remove(numi)
            if (editwf.commands.size() < 1) {
                editwf.commands = new ArrayList()
            }

            result['undo'] = [action: 'insert', num: numi, params: item.properties]
        } else if (input.action == 'insert') {
            def num = input.num
            def item= createItemFromParams(input.params)
            _validateCommandExec(item, params.newitemtype, fprojects, false, input.project)
            if (item.errors.hasErrors()) {
                return [error: item.errors.allErrors.collect {g.message(error: it)}.join(","), item: item]
            }
            def validation = _doValidatePluginStep(item)
            if(!validation.valid){
                return [error: "Plugin configuration was not valid: ${validation.report}", item: item,report: validation.report]
            }
            _sanitizePluginStep(item,validation)
            if (null != editwf.commands) {
                editwf.commands.add(num, item)
            } else {
                editwf.addToCommands(item)
                num = 0
            }
            result['undo'] = [action: 'remove', num: num]
        } else if (input.action == 'insertFilter') {
            def numi = input.num
            def indexi = input.index
            Map config = input.config
            def filtertype = input.filtertype

            if (numi >= (editwf.commands ? editwf.commands.size() : 1)) {
                result.error = "num parameter is invalid: ${numi}"
                return result
            }

            def validation = _validateLogFilter(config, filtertype)
            if (!validation.valid) {
                return [error : "Plugin configuration was not valid: ${validation.report}",
                        config: config,
                        report: validation.report]
            }
            config = validation.props

            def WorkflowStep item = editwf.commands.get(numi)
            def filterConfig = item.getPluginConfigForType(ServiceNameConstants.LogFilter)
            if (filterConfig instanceof List) {
                List configs = (List) filterConfig
                if(indexi<0){
                    indexi=configs.size()
                }
                configs.add(indexi, [type: filtertype, config: config])
            } else {
                filterConfig = [[type: filtertype, config: config]]
                indexi = 0;
            }
            item.storePluginConfigForType(ServiceNameConstants.LogFilter, filterConfig)

            result['saved'] = [type: filtertype, config: config]
            result['undo'] = [action: 'removeFilter', num: input.num, index: indexi]
        } else if (input.action == 'removeFilter') {
            def numi = input.num
            def indexi = input.index
            if (numi >= (editwf.commands ? editwf.commands.size() : 1)) {
                result.error = "num parameter is invalid: ${numi}"
                return result
            }
            def WorkflowStep item = editwf.commands.get(numi)
            def filterConfig = item.getPluginConfigForType(ServiceNameConstants.LogFilter)

            if (!(filterConfig instanceof List)) {
                result.error = "index parameter is invalid: ${indexi}"
                return result
            }
            List filterConfigs = (List) filterConfig

            if (indexi >= filterConfigs.size()) {
                result.error = "index parameter is invalid: ${indexi}"
                return result
            }
            def filterdef = filterConfigs.remove(indexi)

            item.storePluginConfigForType(ServiceNameConstants.LogFilter, filterConfig)

            result['undo'] = [
                    action    : 'insertFilter',
                    num       : input.num,
                    index     : indexi,
                    config    : filterdef.config,
                    filtertype: filterdef.type
            ]
        } else if (input.action == 'modifyFilter') {
            def numi = input.num
            def indexi = input.index

            def config = input.config
            def filtertype = input.filtertype

            if (numi >= (editwf.commands ? editwf.commands.size() : 1)) {
                result.error = "num parameter is invalid: ${numi}"
                return result
            }
            def WorkflowStep item = editwf.commands.get(numi)
            def filterConfig = item.getPluginConfigForType(ServiceNameConstants.LogFilter)

            if (!(filterConfig instanceof List)) {
                result.error = "index parameter is invalid: ${indexi}"
                return result
            }
            List filterConfigs = (List) filterConfig

            if (indexi >= filterConfigs.size()) {
                result.error = "index parameter is invalid: ${indexi}"
                return result
            }

            def validation = _validateLogFilter(config, filtertype)
            if (!validation.valid) {
                return [error : "Plugin configuration was not valid: ${validation.report}",
                        config: config,
                        report: validation.report]
            }
            config = validation.props

            def origfilter = filterConfigs.get(indexi)

            filterConfigs.set(indexi, [type: filtertype, config: config])

            item.storePluginConfigForType(ServiceNameConstants.LogFilter, filterConfig)
            result['saved'] = [type: filtertype, config: config]
            result['undo'] = [
                    action    : 'modifyFilter',
                    num       : numi,
                    index     : indexi,
                    config    : origfilter.config,
                    filtertype: origfilter.type
            ]
        } else if (input.action == 'modify') {
            def numi = input.num
            if (numi >= (editwf.commands ? editwf.commands.size() : 1)) {
                result.error = "num parameter is invalid: ${numi}"
                return result
            }
            def WorkflowStep item = editwf.commands.get(numi)
            def clone = item.createClone()
            def moditem = item.createClone()
            modifyItemFromParams(moditem,input.params)
            _validateCommandExec(moditem,input.params.origitemtype, fprojects)
            if (moditem.errors.hasErrors()) {
                return [error: moditem.errors.allErrors.collect {g.message(error: it)}.join(","), item: moditem]
            }
            def validation = _doValidatePluginStep(moditem)
            if (!validation.valid) {
                return [error: "Plugin configuration was not valid", item: moditem, report: validation.report]
            }

            modifyItemFromParams(item, input.params)
            _sanitizePluginStep(item, validation)
            result['undo'] = [action: 'modify', num: numi, params: clone.properties]
        }else if (input.action=='removeHandler'){
            //remove error handler from wfstep
            def numi = input.num
            def WorkflowStep wfitem = editwf.commands.get(numi)
            def WorkflowStep item = wfitem.errorHandler
            wfitem.errorHandler=null

            result['undo'] = [action: 'addHandler', num: numi, params: item.properties]
        }else if (input.action=='addHandler'){
            //add error handler for wfstep
            def numi = input.num
            if (numi >= (editwf.commands ? editwf.commands.size() : 1)) {
                result.error = "num parameter is invalid: ${numi}"
                return result
            }
            def WorkflowStep item = editwf.commands.get(numi)
            def ehitem= createItemFromParams(input.params)
            _validateCommandExec(ehitem, params.newitemtype, fprojects, false, input.project)
            if (ehitem.errors.hasErrors()) {
                return [error: ehitem.errors.allErrors.collect {g.message(error: it)}.join(","), item: ehitem]
            }
            def validation = _doValidatePluginStep(ehitem)
            if (!validation.valid) {
                return [error: "Plugin configuration was not valid", item: ehitem, report: validation.report]
            }
            _sanitizePluginStep(ehitem, validation)
            item.errorHandler= ehitem
            result['undo'] = [action: 'removeHandler', num: numi]
        } else if (input.action == 'modifyHandler') {
            def numi = input.num
            if (numi >= (editwf.commands ? editwf.commands.size() : 1)) {
                result.error = "num parameter is invalid: ${numi}"
                return result
            }
            def WorkflowStep stepitem = editwf.commands.get(numi)
            def WorkflowStep ehitem = stepitem.errorHandler

            if (!ehitem) {
                result.error = "num parameter is invalid: ${numi}: no error handler"
                return result
            }
            def clone = ehitem.createClone()
            def moditem = ehitem.createClone()

            modifyItemFromParams(moditem, input.params)

            _validateCommandExec(moditem,input.params.origitemtype, fprojects)
            if (moditem.errors.hasErrors()) {
                return [error: moditem.errors.allErrors.collect {g.message(error: it)}.join(","), item: moditem]
            }
            def validation = _doValidatePluginStep(moditem)
            if (!validation.valid) {
                return [error: "Plugin configuration was not valid", item: moditem, report: validation.report]
            }

            modifyItemFromParams(ehitem, input.params)
            _sanitizePluginStep(ehitem, validation)

            result['undo'] = [action: 'modifyHandler', num: numi, params: clone.properties]
        }
        return result
    }

    public static int UNDO_MAX = 20;

    /**
     * Push item to undo stack
     */
    private void _pushUndoAction(id, Map input){
        if (!input) {
            return
        }
        if (!session.undoWF) {
            session.undoWF = [:]
            session.undoWFstate = [:]
        }
        def uid = id ? id : '_new'
        if (!session.undoWF[uid]) {
            session.undoWF[uid] = [input]
        } else {
            session.undoWF[uid] << input
        }
        if (session.undoWF[uid].size() > UNDO_MAX) {
            session.undoWF[uid].remove(0);
        }
        session.undoWFstate[uid] = true
    }

    /**
     * Pop item from undo stack
     */
    private Map _popUndoAction (String id){
        if (!id) {
            id = '_new'
        }
        if (session.undoWF && session.undoWF[id]) {
            return session.undoWF[id].removeLast()
        }
        return null
    }

    /**
     * Push item to redo stack
     */
    private void _pushRedoAction (id, Map input){
        if (!input) {
            return
        }
        if (!session.redoWF) {
            session.redoWF = [:]
        }
        if (!session.undoWFstate) {
            session.undoWFstate = [:]
        }
        def uid = id ? id : '_new'
        if (!session.redoWF[uid]) {
            session.redoWF[uid] = [input]
        } else {
            session.redoWF[uid] << input
        }
        if (session.redoWF[uid].size() > UNDO_MAX) {
            session.redoWF[uid].remove(0);
        }
        session.undoWFstate[uid] = false
    }
    /**
     * pop item from redo stack
     */
    private Map  _popRedoAction (id){
        if (!id) {
            id = '_new'
        }
        if (session.redoWF && session.redoWF[id]) {
            return session.redoWF[id].removeLast()
        }
        return null
    }
    /**
     * Clear redo stack
     */
    private void _clearRedoStack (id){
        if (!id) {
            id = '_new'
        }
        if (session.redoWF && session.redoWF[id]) {
            session.redoWF.remove(id)
        }
    }

    /**
     * Return the session-stored workflow, or store the specified one in the session
     */
    private def _getSessionWorkflow (Workflow usedwf = null){
        return getSessionWorkflow(session,params,usedwf)
    }
    /**
     * Return the session-stored workflow, or store the specified one in the session
     * @param session the session container
     * @param params the params
     * @param usedwf Workflow to store in the session (optional)
     */
    public static Workflow getSessionWorkflow (session,params,Workflow usedwf = null){
        def wfid = '_new'
        def Workflow editwf
        if (!session.editWF) {
            session.editWF = [:]
        }
        if (params?.scheduledExecutionId) {
            wfid = params.scheduledExecutionId
            if (!session.editWF[wfid]) {
                ScheduledExecution sched = ScheduledExecution.getByIdOrUUID(params.scheduledExecutionId)
                if (!sched) {
                    session.editWF[wfid] = new Workflow()
                }

                if (sched.workflow) {
                    session.editWF[wfid] = new Workflow(sched.workflow)
                }
            }
        } else if (usedwf) {
            //load from existing execution
            session.editWF[wfid] = new Workflow(usedwf)
        } else if (!session.editWF[wfid]) {
            session.editWF[wfid] = new Workflow()
        }
        editwf = session.editWF[wfid]
        return editwf
    }



    /**
     * Validate a WorkflowStep object.  will call Errors.rejectValue for
     * any invalid fields for the object.
     * @param exec the WorkflowStep
     * @param type type if specified in params
     */
    public static boolean _validateCommandExec(WorkflowStep exec, String type = null, List authProjects = null, boolean strict=false, String project=null) {
        if (exec instanceof JobExec) {
            if(strict){
                def refSe = exec.findJob(project)

                if(!refSe){
                    exec.errors.rejectValue('jobName', 'commandExec.jobName.strict.validation.message')
                }
            }
            if (!exec.jobName && !exec.uuid) {
                exec.errors.rejectValue('jobName', 'commandExec.jobName.blank.message')
            }
            if(exec.uuid && !exec.jobName){
                def refSe = exec.findJob(project)
                if(refSe){
                    exec.jobProject = refSe.project
                }
            }
            if(exec.jobProject){
                if(authProjects && !authProjects.contains(exec.jobProject) && strict){
                    exec.errors.rejectValue('jobProject', 'commandExec.jobProject.unauth.message')
                }
            }
        }else if(exec instanceof CommandExec){
            if (!exec.adhocRemoteString && 'command' == type) {
                exec.errors.rejectValue('adhocRemoteString', 'commandExec.adhocExecution.adhocRemoteString.blank.message')
            } else if (!exec.adhocLocalString && 'script' == type) {
                exec.errors.rejectValue('adhocLocalString', 'commandExec.adhocExecution.adhocLocalString.blank.message')
            } else if (!exec.adhocFilepath && 'scriptfile' == type) {
                exec.errors.rejectValue('adhocFilepath', 'commandExec.adhocExecution.adhocFilepath.blank.message')
            } else if (!exec.adhocRemoteString && !exec.adhocLocalString && !exec.adhocFilepath) {
                exec.errors.rejectValue('adhocExecution', 'commandExec.adhocExecution.adhocString.blank.message')
            } else {
                def x = ['adhocLocalString', 'adhocRemoteString', 'adhocFilepath'].grep {
                    exec[it]
                }
                if (x && x.size() > 1) {
                    exec.errors.rejectValue('adhocRemoteString', 'scheduledExecution.adhocString.duplicate.message')
                }
            }
        }else if(exec instanceof PluginStep){
            //TODO: validate
        }
        //TODO: validate log filter plugins
    }
    /**
     * Validate a WorkflowStep object.  will call Errors.rejectValue for
     * any invalid fields for the object.
     * @param exec the WorkflowStep
     * @param type type if specified in params
     */
    static Map _validatePluginStep(FrameworkService frameworkService, WorkflowStep exec) {
        if (exec instanceof PluginStep) {
            PluginStep item = exec as PluginStep
            def description = getPluginStepDescription(frameworkService, item.nodeStep, item.type)
            if (!description) {
                return [valid: false, report: "Plugin not found: " + item.type]
            }
            return frameworkService.validateDescription(
                    description,
                    '',
                    item.configuration,
                    null,
                    PropertyScope.Instance,
                    PropertyScope.Project
            )
        } else {
            return [valid: true]
        }
    }
    /**
     * Validate a WorkflowStep object.  will call Errors.rejectValue for
     * any invalid fields for the object.
     * @param exec the WorkflowStep
     * @param type type if specified in params
     */
    protected Map _doValidatePluginStep(WorkflowStep exec) {
        _validatePluginStep(frameworkService, exec)
    }
    private void _sanitizePluginStep(WorkflowStep item, Map validation){
        if (item instanceof PluginStep) {
            PluginStep step = item as PluginStep
            //set configuration based on parsed props
            step.configuration=validation.props
        }
    }

    /**
     * Get the dynamics properties of a plugin.
     * @param project name of project
     * @param newItemType new item type
     */
    private Map<String, Object> getDynamicProperties(
        String project,
        String newItemType,
        boolean isNodeStep,
        Services services
    ) {

        try {
            return pluginService.getDynamicProperties(
                frameworkService.rundeckFramework,
                isNodeStep ? ServiceNameConstants.WorkflowNodeStep : ServiceNameConstants.WorkflowStep,
                newItemType,
                project,
                services
            )
        } catch (MissingProviderException e) {
            log.warn("step provider not found: ${newItemType}: ${e.message}", e)
        }
        return null
    }

    /**
     * Validate a LogFilterPlugin configuration.
     * @param config the config
     * @param type plugin type
     */
    protected Map _validateLogFilter(Map config, String type) {
        _validateLogFilter(frameworkService, pluginService, config, type)
    }
    /**
     * Validate a LogFilterPlugin configuration.
     * @param config the config
     * @param type plugin type
     */
    static Map _validateLogFilter(
            FrameworkService frameworkService,
            PluginService pluginService,
            Map config,
            String type
    )
    {
        def described = pluginService.getPluginDescriptor(type, LogFilterPlugin)
        if (!described) {
            return [valid: false, report: "The LogFilter provider \"$type\" was not found"]
        }
        return frameworkService.validateDescription(
                described.description,
                '',
                config,
                null,
                PropertyScope.Instance,
                PropertyScope.Project
        )
    }
}

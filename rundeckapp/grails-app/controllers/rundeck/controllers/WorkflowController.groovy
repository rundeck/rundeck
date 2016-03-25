package rundeck.controllers

import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import rundeck.WorkflowStep
import rundeck.Workflow
import rundeck.JobExec
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.PluginStep
import com.dtolabs.rundeck.core.plugins.configuration.Description
import rundeck.services.ExecutionService

import javax.servlet.http.HttpServletResponse

class WorkflowController extends ControllerBase {
    def frameworkService
    static allowedMethods = [
            redo:'POST',
            remove:'POST',
            reorder:'POST',
            revert:'POST',
            save:'POST',
            undo:'POST',
    ]
    def index = {
        return redirect(controller: 'menu', action: 'index')
    }


    /**
     * Render the edit form for a workflow item
     */
    def edit = {
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
        def newitemtype = params['newitemtype']
        def origitemtype
        def newitemDescription
        if(item && item.instanceOf(PluginStep)){
            newitemDescription = getPluginStepDescription(item.nodeStep, item.type)
            origitemtype=item.type
        } else{
            newitemDescription = getPluginStepDescription(params.newitemnodestep == 'true', params['newitemtype'])
        }
        if(item){
            if(item.instanceOf(JobExec)){
                origitemtype='job'
            }else if(item.instanceOf(CommandExec)){
                origitemtype=item.adhocLocalString?'script':item.adhocRemoteString?'command':'scriptfile'
            }
        }

        return render(
                template: "/execution/wfitemEdit",
                  model: [
                          item                : item,
                          key                 : params.key,
                          num                 : numi,
                          scheduledExecutionId: params.scheduledExecutionId,
                          newitemtype         : newitemtype,
                          origitemtype         : origitemtype,
                          newitemDescription  : newitemDescription,
                          pluginNotFound      : null == newitemDescription,
                          edit                : true,
                          isErrorHandler      : isErrorHandler,
                          newitemnodestep     : params.newitemnodestep
                  ]
        )
    }

    /**
     * Return a Description for a plugin type, if available
     * @param type
     * @param framework
     * @return
     */
    private Description getPluginStepDescription(boolean isNodeStep, String type) {
        if (type && !(type in ['command', 'script', 'scriptfile', 'job'])) {
            try{
                return isNodeStep ? frameworkService.getNodeStepPluginDescription(type) : frameworkService.getStepPluginDescription(type)
            }catch(MissingProviderException e){
                log.warn("step provider not found: ${type}: ${e.message}",e)
            }
        }
        return null
    }

    /**
     * Render workflow item
     */
    def render = {
        if (!params.num) {
            log.error("num parameter is required")
            return renderErrorFragment("num parameter is required")
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
    def save = {
        withForm{
            g.refreshFormTokensHeader()
        if (!params.num && !params.newitem) {
            log.error("num parameter is required")
            return renderErrorFragment("num parameter is required")
        }
        def Workflow editwf = _getSessionWorkflow()
        def item
        def numi
        def wfEditAction = 'true' == params.newitem ? 'insert' : 'modify'
        if (null != params.num) {
            try {
                numi = Integer.parseInt(params.num)
            } catch (NumberFormatException e) {
                log.error("num parameter is invalid: " + params.num)
                flash.'error' = "num parameter is invalid: " + params.num
                return render(template: "/execution/wfitemEdit", model: [item: null, num: numi, scheduledExecutionId: params.scheduledExecutionId, newitemtype: params['newitemtype'], edit: true])
            }
        } else {
            numi = editwf.commands ? editwf.commands.size() : 0
        }
        final isErrorHandler = params.isErrorHandler == 'true'
        if (isErrorHandler) {
            wfEditAction = 'true' == params.newitem ? 'addHandler' : 'modifyHandler'
        }
        def result = _applyWFEditAction(editwf, [action: wfEditAction, num: numi, params: params])
        if (result.error) {
            log.error(result.error)
            item=result.item
            def itemDescription = item.instanceOf(PluginStep) ? getPluginStepDescription(item.nodeStep, item.type) : null

            def newitemtype = params['newitemtype']
            def origitemtype = params['origitemtype']

            return render(
                    template: "/execution/wfitemEdit",
                    model: [
                            item                : result.item,
                            key                 : params.key,
                            num                 : params.num,
                            scheduledExecutionId: params.scheduledExecutionId,
                            newitemtype         : newitemtype,
                            origitemtype        : origitemtype,
                            edit                : true,
                            isErrorHandler      : isErrorHandler,
                            newitemDescription  : itemDescription,
                            report              : result.report
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
    def reorder = {
        withForm{
        if (!params.fromnum) {
            log.error("fromnum parameter required")
            return renderErrorFragment("fromnum parameter required")
        }
        if (!params.tonum) {
            log.error("tonum parameter required")
            return renderErrorFragment("tonum parameter required")
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
    def undo = {
        withForm{
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
    def redo = {
        withForm{
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
    def revert = {
        withForm{
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
    def renderUndo = {
        final String id = params.scheduledExecutionId ? params.scheduledExecutionId : '_new'
        return render(template: '/common/undoRedoControls', model: [undo: session.undoWF ? session.undoWF[id]?.size() : 0, redo: session.redoWF ? session.redoWF[id]?.size() : 0, key: 'workflow'])
    }

    /**
     * For each value in the map, replace \r\n line endings with \n
     * @param input
     * @return
     */
    private Map cleanLineEndings(Map input){
        def result = [:]
        input.each {k,v->
            result[k]=v.replaceAll(/\r?\n/,'\n')
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
            _validateCommandExec(item, params.newitemtype)
            if (item.errors.hasErrors()) {
                return [error: item.errors.allErrors.collect {g.message(error: it)}.join(","), item: item]
            }
            def validation=_validatePluginStep(item, params.newitemtype)
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

            _validateCommandExec(moditem,input.params.origitemtype)
            if (moditem.errors.hasErrors()) {
                return [error: moditem.errors.allErrors.collect {g.message(error: it)}.join(","), item: moditem]
            }
            def validation = _validatePluginStep(moditem, params.newitemtype)
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
            _validateCommandExec(ehitem, params.newitemtype)
            if (ehitem.errors.hasErrors()) {
                return [error: ehitem.errors.allErrors.collect {g.message(error: it)}.join(","), item: ehitem]
            }
            def validation = _validatePluginStep(ehitem, params.newitemtype)
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

            _validateCommandExec(moditem,input.params.origitemtype)
            if (moditem.errors.hasErrors()) {
                return [error: moditem.errors.allErrors.collect {g.message(error: it)}.join(","), item: moditem]
            }
            def validation = _validatePluginStep(moditem, params.newitemtype)
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
    }

    /**
     * Pop item from undo stack
     */
    private Map _popUndoAction (String id){
        if (!id) {
            id = '_new'
        }
        if (session.undoWF && session.undoWF[id]) {
            return session.undoWF[id].pop()
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
        def uid = id ? id : '_new'
        if (!session.redoWF[uid]) {
            session.redoWF[uid] = [input]
        } else {
            session.redoWF[uid] << input
        }
        if (session.redoWF[uid].size() > UNDO_MAX) {
            session.redoWF[uid].remove(0);
        }
    }
    /**
     * pop item from redo stack
     */
    private Map  _popRedoAction (id){
        if (!id) {
            id = '_new'
        }
        if (session.redoWF && session.redoWF[id]) {
            return session.redoWF[id].pop()
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
    public static boolean _validateCommandExec(WorkflowStep exec, String type = null) {
        if (exec instanceof JobExec) {
            if (!exec.jobName) {
                exec.errors.rejectValue('jobName', 'commandExec.jobName.blank.message')
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
    }
    /**
     * Validate a WorkflowStep object.  will call Errors.rejectValue for
     * any invalid fields for the object.
     * @param exec the WorkflowStep
     * @param type type if specified in params
     */
     protected Map _validatePluginStep(WorkflowStep exec, String type = null) {
        if(exec instanceof PluginStep){
            PluginStep item = exec as PluginStep
            def description=getPluginStepDescription(item.nodeStep, item.type)
            return frameworkService.validateDescription(
                    description,
                    '',
                    item.configuration,
                    null,
                    PropertyScope.Instance,
                    PropertyScope.Project
            )
        }else{
            return [valid:true]
        }
    }
    private void _sanitizePluginStep(WorkflowStep item, Map validation){
        if (item instanceof PluginStep) {
            PluginStep step = item as PluginStep
            //set configuration based on parsed props
            step.configuration=validation.props
        }
    }
    
}

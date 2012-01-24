class WorkflowController {

    def index = {
        return redirect(controller: 'menu', action: 'index')
    }


    /**
     * Render the edit form for a workflow item
     */
    def edit = {
        if (!params.num && !params['newitemtype']) {
            log.error("num parameter is required")
            flash.error = "num parameter is required"
            return error.call()
        }

        def Workflow editwf = _getSessionWorkflow()
        def numi;
        if (params.num) {
            numi = Integer.parseInt(params.num)
            if (numi >= editwf.commands.size()) {
                log.error("num parameter is invalid: ${numi}")
                flash.error = "num parameter is invalid: ${numi}"
                return error.call()
            }
        }
        return render(template: "/execution/wfitemEdit", model: [item: null != numi ? editwf.commands.get(numi) : null, num: numi, scheduledExecutionId: params.scheduledExecutionId, newitemtype: params['newitemtype'], edit: true])
    }

    /**
     * Render workflow item
     */
    def render = {
        if (!params.num) {
            log.error("num parameter is required")
            flash.error = "num parameter is required"
            return error.call()
        }

        def Workflow editwf = _getSessionWorkflow()
        def numi = Integer.parseInt(params.num)
        if (numi >= editwf.commands.size()) {
            log.error("num parameter is invalid: ${numi}")
            flash.error = "num parameter is invalid: ${numi}"
            return error.call()
        }
        return render(template: "/execution/wflistitemContent", model: [workflow: editwf, item: editwf.commands.get(numi), i: numi, scheduledExecutionId: params.scheduledExecutionId, edit: params.edit])
    }


    /**
     * Save workflow item
     */
    def save = {
        if (!params.num && !params.newitem) {
            log.error("num parameter is required")
            flash.error = "num parameter is required"
            return error.call()
        }
        def Workflow editwf = _getSessionWorkflow()
        def item
        def numi
        if (!params.newitem && null != params.num) {
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
        def result = _applyWFEditAction(editwf, [action: 'true' == params.newitem ? 'insert' : 'modify', num: numi, params: params])
        if (result.error) {
            log.error(result.error)
            return render(template: "/execution/wfitemEdit", model: [item: result.item, num: params.num, scheduledExecutionId: params.scheduledExecutionId, newitemtype: params['newitemtype'], edit: true])
        }
        _pushUndoAction(params.scheduledExecutionId, result.undo)
        if (result.undo) {
            _clearRedoStack(params.scheduledExecutionId)
        }

        return render(template: "/execution/wflistitemContent", model: [workflow: editwf, item: editwf.commands.get(numi), i: numi, scheduledExecutionId: params.scheduledExecutionId, edit: true])
    }



    /**
     * Reorder items
     */
    def reorder = {
        if (!params.fromnum) {
            log.error("fromnum parameter required")
            flash.error = "fromnum parameter required"
            return error.call()
        }
        if (!params.tonum) {
            log.error("tonum parameter required")
            flash.error = "tonum parameter required"
            return error.call()
        }

        def Workflow editwf = _getSessionWorkflow()

        def fromi = Integer.parseInt(params.fromnum)
        def toi = Integer.parseInt(params.tonum)
        def result = _applyWFEditAction(editwf, [action: 'move', from: fromi, to: toi])
        if (result.error) {
            log.error(result.error)
            flash.error = result.error
            return error.call()
        }
        _pushUndoAction(params.scheduledExecutionId, result.undo)
        _clearRedoStack(params.scheduledExecutionId)

        return render(template: "/execution/wflistContent", model: [workflow: editwf, edit: params.edit, highlight: toi, project:session.project])
    }

    /**
     * Remove an item
     */
    def remove = {

        if (!params.delnum) {
            log.error("delnum parameter required")
            flash.error = "delnum parameter required"
            return error.call()
        }
        def Workflow editwf = _getSessionWorkflow()

        def fromi = Integer.parseInt(params.delnum)

        def result = _applyWFEditAction(editwf, [action: 'remove', num: fromi])
        if (result.error) {
            log.error(result.error)
            flash.error = result.error
            return error.call()
        }
        _pushUndoAction(params.scheduledExecutionId, result.undo)
        _clearRedoStack(params.scheduledExecutionId)

        return render(template: "/execution/wflistContent", model: [workflow: editwf, edit: params.edit, project: session.project])
    }


    /**
     * Undo change
     */
    def undo = {

        def Workflow editwf = _getSessionWorkflow()
        def action = _popUndoAction(params.scheduledExecutionId)

        def num
        if (action) {
            def result = _applyWFEditAction(editwf, action)
            if (result.error) {
                log.error(result.error)
                flash.error = result.error
                return error.call()
            }
            if (null != action.num) {
                num = action.num
            } else if (null != action.to) {
                num = action.to
            }
            _pushRedoAction(params.scheduledExecutionId, result.undo)
        }

        return render(template: "/execution/wflistContent", model: [workflow: editwf, edit: params.edit, highlight: num, project: session.project])
    }



    /**
     * redo change
     */
    def redo = {

        def Workflow editwf = _getSessionWorkflow()
        def action = _popRedoAction(params.scheduledExecutionId)

        def num
        if (action) {
            def result = _applyWFEditAction(editwf, action)
            if (result.error) {
                log.error(result.error)
                flash.error = result.error
                return error.call()
            }
            if (null != action.num) {
                num = action.num
            } else if (null != action.to) {
                num = action.to
            }
            _pushUndoAction(params.scheduledExecutionId, result.undo)
        }

        return render(template: "/execution/wflistContent", model: [workflow: editwf, edit: params.edit, highlight: num, project: session.project])
    }

    /**
     * revert all changes
     */
    def revert = {
        final String uid = params.scheduledExecutionId ? params.scheduledExecutionId : '_new'
        session.editWF?.remove(uid)
        session.undoWF?.remove(uid)
        session.redoWF?.remove(uid)
        def Workflow editwf = _getSessionWorkflow()

        return render(template: "/execution/wflistContent", model: [workflow: editwf, edit: true, project: session.project])
    }


    /**
     * display undo/redo buttons for current stack state
     */
    def renderUndo = {
        final String id = params.scheduledExecutionId ? params.scheduledExecutionId : '_new'
        return render(template: '/common/undoRedoControls', model: [undo: session.undoWF ? session.undoWF[id]?.size() : 0, redo: session.redoWF ? session.redoWF[id]?.size() : 0, key: 'workflow'])
    }

    /**
     *
     * handles ALL modifications to the workflow via named actions, input in a map:
     * input map:
     * action: name of action 'move','remove','insert','modify'
     * num: item index to affect
     * from/to: (move action) item index to move from and to
     * params: properties of the item (insert,modify actions)
     *
     *  Returns result map:
     *
     * error: any error message
     * undo: corresponding undo action map
     */
    Map _applyWFEditAction (Workflow editwf, Map input){
        def result = [:]
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
            def item

            if (input.params.jobName || 'job' == input.params.newitemtype) {
                item = new JobExec(input.params)
            } else {
                item = new CommandExec(input.params)

                def optsmap = ExecutionService.filterOptParams(input.params)
                if (optsmap) {
                    item.argString = ExecutionService.generateArgline(optsmap)
                    //TODO: validate input options
                }
            }
            _validateCommandExec(item, params.newitemtype)
            if (item.errors.hasErrors()) {
                return [error: item.errors.allErrors.collect {g.message(error: it)}.join(","), item: item]
            }
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
            def CommandExec item = editwf.commands.get(numi)
            def clone = item.createClone()
            def moditem = item.createClone()
            moditem.properties = input.params
            _validateCommandExec(moditem)
            if (moditem.errors.hasErrors()) {
                return [error: moditem.errors.allErrors.collect {g.message(error: it)}.join(","), item: moditem]
            }
            item.properties = input.params
            def optsmap = ExecutionService.filterOptParams(input.params)
            if (optsmap) {
                item.argString = ExecutionService.generateArgline(optsmap)
                //TODO: validate input options
            }
            result['undo'] = [action: 'modify', num: numi, params: clone.properties]
        }
        return result
    }

    public static int UNDO_MAX = 20;

    /**
     * Push item to undo stack
     */
    void _pushUndoAction(id, Map input){
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
    Map _popUndoAction (String id){
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
    void _pushRedoAction (id, Map input){
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
    Map  _popRedoAction (id){
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
    void _clearRedoStack (id){
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
    private def _getSessionWorkflow = {Workflow usedwf = null ->
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
                ScheduledExecution sched = ScheduledExecution.get(params.scheduledExecutionId)
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
     * Validate a CommandExec or JobExec object.  will call Errors.rejectValue for
     * any invalid fields for the object.
     * @param exec the CommandExec
     * @param type type if specified in params
     */
    public static boolean _validateCommandExec(CommandExec exec, String type = null) {
        if (exec instanceof JobExec) {
            if (!exec.jobName) {
                exec.errors.rejectValue('jobName', 'commandExec.jobName.blank.message')
            }
        }else{
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
        } 
    }
    
}

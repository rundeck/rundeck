package rundeck.controllers

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import rundeck.Option
import rundeck.ScheduledExecution

/**
 * Controller for manipulating the session-stored set of Options during job edit
 */
class EditOptsController {
    def static allowedMethods = [
            redo: 'POST',
            remove: 'POST',
            revert: 'POST',
            save: 'POST',
            undo: 'POST',
    ]
    def index = {
        redirect(controller: 'menu', action: 'index')
    }

    def error = {
        return render(template: "/common/messages")
    }

    /**
     * render edit form for an option.  params.name= name of existing option to edit, otherwise params.newoption is
     * required to create a new option
     */
    def edit = {
        if (!params.name && !params.newoption) {
            log.error("name parameter required")
            flash.error = "name parameter required"
            return error.call()
        }
        def editopts = _getSessionOptions()
        if (params.name && !editopts[params.name]) {
            log.error("no option with name ${params.name} found")
            flash.error = "no option with name ${params.name} found"
            return error.call()
        }
        def outparams=[:]
        if(null != params.name && editopts[params.name]){

            outparams = _validateOption(editopts[params.name], null,params.jobWasScheduled=='true')
        }
        return render(template: "/scheduledExecution/optEdit", model: [option: null != params.name ? editopts[params.name] : null, name: params.name, scheduledExecutionId: params.scheduledExecutionId, newoption: params['newoption'], edit: true] + outparams)
    }

    /**
     * Render view of an option definition. params.name= name of option to render, required.
     */
    def render = {
        if (!params.name) {
            log.error("name parameter is required")
            flash.error = "name parameter is required"
            return error.call()
        }
        def name = params.name

        def Map editopts = _getSessionOptions()
        if (!editopts[name]) {
            log.error("name parameter is invalid: ${name}")
            flash.error = "name parameter is invalid: ${name}"
            return error.call()
        }

        return render(template: "/scheduledExecution/optlistitemContent", model: [options: editopts, option: editopts[name], name: name, scheduledExecutionId: params.scheduledExecutionId, edit: params.edit])
    }

    /**
     * Render all options
     */
    def renderAll = {
        def Map editopts = _getSessionOptions()
        //configure sorted list
        def options = new TreeSet()
        options.addAll(editopts.values())
        return render(template: "/scheduledExecution/optlistContent", model: [options: options, scheduledExecutionId: params.scheduledExecutionId, edit: params.edit])
    }


    /**
     * Render all options in summary view
     */
    def renderSummary = {
        def Map editopts = _getSessionOptions()
        //configure sorted list
        def options = new TreeSet()
        options.addAll(editopts.values())
        return render(template: "/scheduledExecution/optionsSummary", model: [options: options, scheduledExecutionId: params.scheduledExecutionId, edit: params.edit])
    }

    /**
     * Save new option or existing option definition. params.name= name of existing option, or params.newoption is required
     */
    def save = {
        withForm{
        if (!params.name && !params.newoption) {
            log.error("name parameter is required")
            flash.error = "name parameter is required"
            return error.call()
        }
        def editopts = _getSessionOptions()
        def name = params.name
        def origName = params.origName

        def result = _applyOptionAction(editopts, [action: 'true' == params.newoption ? 'insert' : 'modify', name: origName ? origName : name, params: params])
        if (result.error) {
            log.error(result.error)
            return render(template: "/scheduledExecution/optEdit", model: [option: result.option, name: params.num, scheduledExecutionId: params.scheduledExecutionId, origName: params.origName, newoption: params['newoption'], edit: true, regexError: result.regexError])
        }
        _pushUndoAction(params.scheduledExecutionId, result.undo)
        if (result.undo) {
            _clearRedoStack(params.scheduledExecutionId)
        }

        return render(template: "/scheduledExecution/optlistitemContent", model: [option: editopts[name], name: name, scheduledExecutionId: params.scheduledExecutionId, edit: true])
        }.invalidToken{
            request.error = g.message(code: 'request.error.invalidtoken.message')
            return error.call()
        }
    }

    /**
     * Remove an option by name.  params.name required
     */
    def remove = {
        withForm {
        if (!params.name) {
            log.error("name parameter is required")
            flash.error = "name parameter is required"
            return error.call()
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
            return error.call()
        }
    }


    /**
     * Show undo/redo buttons
     */
    def renderUndo = {
        final String id = params.scheduledExecutionId ? params.scheduledExecutionId : '_new'
        render(template: "/common/undoRedoControls", model: [undo: session.undoOPTS ? session.undoOPTS[id]?.size() : 0, redo: session.redoOPTS ? session.redoOPTS[id]?.size() : 0, key: 'opts', revertConfirm: 'all Options'])
    }


    /**
     * Undo action, renders full options list after performing undo
     */
    def undo = {
        withForm{
        def editopts = _getSessionOptions()
        def action = _popUndoAction(params.scheduledExecutionId)

        def name = null
        if (action) {
            def result = _applyOptionAction(editopts, action)
            if (result.error) {
                log.error(result.error)
                flash.error = result.error
                return error.call()
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
            return error.call()
        }
    }



    /**
     * redo action, renders full options list after performing redo
     */
    def redo = {
        withForm{
        def editopts = _getSessionOptions()
        def action = _popRedoAction(params.scheduledExecutionId)

        def name = null
        if (action) {
            def result = _applyOptionAction(editopts, action)
            if (result.error) {
                log.error(result.error)
                flash.error = result.error
                return error.call()
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
            return error.call()
        }
    }

    /**
     * revert action, reloads options from stored ScheduledExecution, clears undo/redo stack, and renders full options list
     */
    def revert = {
        withForm{
        final String uid = params.scheduledExecutionId ? params.scheduledExecutionId : '_new'
        session.editOPTS?.remove(uid)
        session.undoOPTS?.remove(uid)
        session.redoOPTS?.remove(uid)
        return renderAll.call()
        }.invalidToken {
            request.error = g.message(code: 'request.error.invalidtoken.message')
            return error.call()
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
    protected Map _applyOptionAction (Map editopts, Map input){
        def result = [:]
        if ('remove' == input.action) {
            def name = input.name
            if(null==editopts[name]){
                result.putAll([error: "No option named ${name} exists"])
                return result
            }
            def Option item = editopts.remove(name)

            result['undo'] = [action: 'insert', name: name, params: _getParamsFromOption(item)]
        } else if ('insert' == input.action) {
            def name = input.name
            def option = _setOptionFromParams(new Option(), input.params)
            def vres = _validateOption(option, input.params,input.params.jobWasScheduled=='true')
            if (null != editopts[name]) {
                option.errors.rejectValue('name', 'option.name.duplicate.message', [name] as Object[], "Option already exists: {0}")
            }
            if (option.errors.hasErrors()) {
                result.putAll(vres)
                result.putAll([error: 'Invalid', option: option])
                return result
            }

            editopts[name] = option
            result['undo'] = [action: 'remove', name: name]
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
            def vres = _validateOption(moditem, input.params,input.params.jobWasScheduled=='true')
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
    public static _validateOption(Option opt, Map params = null, boolean jobWasScheduled=false) {
        opt.validate()
        def result = [:]
        if (opt.enforced && (opt.values || opt.valuesList) && opt.defaultValue) {
            opt.convertValuesList()
            if(!opt.multivalued && !opt.values.contains(opt.defaultValue)) {
                opt.errors.rejectValue('defaultValue', 'option.defaultValue.notallowed.message')
            }else if(opt.multivalued && opt.delimiter){
                //validate each default value
                def found = opt.defaultValue.split(Pattern.quote(opt.delimiter)).find{ !opt.values.contains(it) }
                if(found){
                    opt.errors.rejectValue('defaultValue', 'option.defaultValue.multivalued.notallowed.message',[found] as Object[],"{0} invalid value")
                }
            }
        }
        if (opt.enforced && (!opt.values && !opt.valuesList && !opt.realValuesUrl)) {
            if (params && params.valuesType == 'url') {
                opt.errors.rejectValue('valuesUrl', 'option.enforced.emptyvalues.message')
            } else {
                opt.errors.rejectValue('values', 'option.enforced.emptyvalues.message')
            }
        }
        if (opt.regex) {
            //try to parse regular expression syntax
            try {
                Pattern.compile(opt.regex)
            } catch (PatternSyntaxException e) {
                result.regexError = e.message
                opt.errors.rejectValue('regex', 'option.regex.invalid.message', [opt.regex] as Object[], "Invalid Regex: {0}")
            }
            if (opt.values || opt.valuesList) {
                opt.convertValuesList()
                def inval = []
                opt.values.each {val ->
                    if (!(val =~ /${opt.regex}/)) {
                        opt.errors.rejectValue('values', 'option.values.regexmismatch.message', [val.toString(), opt.regex] as Object[], "Value does not match regex: {0}")
                    }
                }
            }
            if (opt.defaultValue) {
                if (!(opt.defaultValue =~ /${opt.regex}/)) {
                    opt.errors.rejectValue('defaultValue', 'option.defaultValue.regexmismatch.message', [opt.defaultValue, opt.regex] as Object[], "Default value does not match regex: {0}")
                }
            }
        }
        if(opt.multivalued && !opt.delimiter){
            opt.errors.rejectValue('delimiter', 'option.delimiter.blank.message')
        }
        if(opt.multivalued && opt.secureInput){
            opt.errors.rejectValue('multivalued', 'option.multivalued.secure-conflict.message')
        }
        if(jobWasScheduled && opt.required && !(opt.defaultValue||opt.defaultStoragePath)){
            opt.errors.rejectValue('defaultValue', 'option.defaultValue.required.message')
        }
        return result
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
        }else if (params.inputType=='secure') {
            params.secureInput = true
            params.secureExposed = false
        }else if (params.inputType=='secureExposed') {
            params.secureInput = true
            params.secureExposed = true
        }

        opt.properties = params
        opt.valuesList = params.valuesList
        if(params.valuesType == 'list'){
            opt.realValuesUrl=null
        }else if(params.valuesType == 'url'){
            opt.values=null
            opt.valuesList=null
            if(valuesUrl){
                opt.realValuesUrl=new URL(valuesUrl)
            }else{
                opt.realValuesUrl = null
            }
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
        if (opt.values) {
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
                ScheduledExecution sched = ScheduledExecution.getByIdOrUUID(params.scheduledExecutionId)
                if (!sched) {
                    session.editOPTS[optid] = [:]
                }else if (sched.options) {
                    editopts = [:]

                    sched.options.each {Option opt ->
                        editopts[opt.name] = opt.createClone()
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
            return session.undoOPTS[id].pop()
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
        def uid = id ? id : '_new'
        if (!session.redoOPTS[uid]) {
            session.redoOPTS[uid] = [input]
        } else {
            session.redoOPTS[uid] << input
        }
        if (session.redoOPTS[uid].size() > UNDO_MAX) {
            session.redoOPTS[uid].remove(0);
        }
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
            return session.redoOPTS[id].pop()
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

}

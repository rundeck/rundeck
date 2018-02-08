package rundeck.controllers

import com.dtolabs.rundeck.app.support.trigger.TriggerCreate
import com.dtolabs.rundeck.app.support.trigger.TriggerRequest
import com.dtolabs.rundeck.app.support.trigger.TriggerUpdate
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.core.triggers.TriggerAction
import org.rundeck.core.triggers.TriggerCondition
import rundeck.TriggerRep
import rundeck.services.PluginService

class TriggerController extends ControllerBase implements PluginListRequired {
    def frameworkService
    def triggerService
    PluginService pluginService
    static allowedMethods = [
            'createPost': 'POST',
            'deletePost': 'POST',
            'updatePost': 'POST',
    ]
    Map<String, Class> requiredPluginTypes = [actionPlugins: TriggerAction, conditionPlugins: TriggerCondition]
    Collection<String> requiredPluginActionNames = ['create', 'edit', 'show', 'createPost', 'updatePost']


    def index(String project) {
        //TODO project
        redirect(action: 'list', params: [project: project])
    }

    def cancel(String id, String project) {
        if (id && project) {
            return redirect(action: 'show', params: [id: id, project: project])
        }
        redirect(action: 'list', params: [project: project])
    }

    def list(String project) {
//        def framework = frameworkService.getRundeckFramework()

//        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)

        //TODO: auth
        def triggers = TriggerRep.findAllByProject(project)
        [triggers: triggers, project: project]
    }

    def create(String project) {

    }

    /**
     * Remove map entries where the value is null or a blank string
     * @param map
     * @return
     */
    private static Map cleanMap(Map map) {
        map ? map.entrySet().findAll { it.value }.collectEntries { [it.key, it.value] } : [:]
    }
    def createPost(TriggerCreate input) {
        if (!requestHasValidToken()) {
            return
        }
        //TODO: project exists?

        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, input.project)

        //TODO: auth

        Map conditionMap = cleanMap(params.conditionConfig)
        Map actionMap = cleanMap(params.actionConfig)
        //TODO: trigger user data
        Map userData = cleanMap(params.userDataConfig)



        def result = triggerService.createTrigger(authContext, input, conditionMap, actionMap, userData)

        if (result.error) {
            //edit form
            request.error = 'Validation error'
            return render(
                    view: '/trigger/create',
                    model: [trigger: result.trigger, validation: result.validation, project: input.project]
            )
        }
        def trigger = result.trigger

        return redirect(action: 'show', params: [id: trigger.uuid, project: input.project])
    }

    def delete(TriggerRequest input) {

        show(input)
    }

    def deletePost(TriggerRequest input) {
        if (!requestHasValidToken()) {
            return
        }
        //TODO: project exists?
        //TODO: auth
        def trigger = TriggerRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(trigger, 'Trigger', input.id)) {
            return
        }

        boolean result = triggerService.deleteTrigger(trigger)

        if (result) {
            flash.message = "Trigger $input.id was deleted"
        } else {
            flash.error = "Trigger $input.id was NOT deleted"
        }
        redirect(action: 'list', params: [project: input.project])
    }

    def edit(TriggerRequest input) {
        show(input)
    }

    def updatePost(TriggerUpdate input) {
        if (!requestHasValidToken()) {
            return
        }
        def trigger = TriggerRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(trigger, 'Trigger', input.id)) {
            return
        }

        //TODO: condition map data

        Map conditionMap = cleanMap(params.conditionConfig)
        Map actionMap = cleanMap(params.actionConfig)
        //TODO: trigger user data
        Map userData = cleanMap(params.userDataConfig)
        //TODO...

        //TODO; auth

        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, input.project)

        def result = triggerService.updateTrigger(authContext, trigger, input, conditionMap, actionMap, userData)

        if (result.error) {
            //edit form
            request.error = 'Validation error'
            return render(
                    view: '/trigger/edit',
                    model: [trigger: trigger, validation: result.validation, project: input.project]
            )
        }

        flash.message = "Trigger updated"
        redirect(action: 'show', params: [project: input.project, id: input.id])
    }

    def show(TriggerRequest input) {

        //TODO: project exists?
        //TODO: auth?

        def trigger = TriggerRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(trigger, 'Trigger', input.id)) {
            return
        }

        [trigger: trigger, project: input.project]
    }

    def test(TriggerRequest input) {
        def trigger = TriggerRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(trigger, 'Trigger', input.id)) {
            return
        }

        triggerService.triggerConditionMet(input.id, triggerService.contextForTrigger(trigger), params.data ?: [:])
        flash.message = "Trigger started"
        redirect(action: 'show', params: params)
    }


    def renderConditionCreate(String type) {
    }

    def renderConditionEdit(String type) {

    }

    def renderConditionView(String type) {

    }
}

package rundeck.controllers

import com.dtolabs.rundeck.app.support.task.TaskCreate
import com.dtolabs.rundeck.app.support.task.TaskRequest
import com.dtolabs.rundeck.app.support.task.TaskUpdate
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import org.rundeck.core.tasks.TaskAction
import org.rundeck.core.tasks.TaskTrigger
import rundeck.TaskRep
import rundeck.services.PluginService

class TaskController extends ControllerBase implements PluginListRequired {
    def frameworkService
    def taskRunService
    PluginService pluginService
    static allowedMethods = [
            'createPost': 'POST',
            'deletePost': 'POST',
            'updatePost': 'POST',
    ]
    Map<String, Class> requiredPluginTypes = [actionPlugins: TaskAction, triggerPlugins: TaskTrigger]
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
        def tasks = TaskRep.findAllByProject(project)
        [tasks: tasks, project: project]
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
    def createPost(TaskCreate input) {
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



        def result = taskRunService.createTask(authContext, input, conditionMap, actionMap, userData)

        if (result.error) {
            //edit form
            request.error = 'Validation error'
            return render(
                    view: '/task/create',
                    model: [task: result.task, validation: result.validation, project: input.project]
            )
        }
        def task = result.task

        return redirect(action: 'show', params: [id: task.uuid, project: input.project])
    }

    def delete(TaskRequest input) {

        show(input)
    }

    def deletePost(TaskRequest input) {
        if (!requestHasValidToken()) {
            return
        }
        //TODO: project exists?
        //TODO: auth
        def task = TaskRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(task, 'Task', input.id)) {
            return
        }

        boolean result = taskRunService.deleteTask(task)

        if (result) {
            flash.message = "Task $input.id was deleted"
        } else {
            flash.error = "Task $input.id was NOT deleted"
        }
        redirect(action: 'list', params: [project: input.project])
    }

    def edit(TaskRequest input) {
        show(input)
    }

    def updatePost(TaskUpdate input) {
        if (!requestHasValidToken()) {
            return
        }
        def trigger = TaskRep.findByProjectAndUuid(input.project, input.id)
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

        def result = taskRunService.updateTask(authContext, trigger, input, conditionMap, actionMap, userData)

        if (result.error) {
            //edit form
            request.error = 'Validation error'
            return render(
                    view: '/task/edit',
                    model: [trigger: trigger, validation: result.validation, project: input.project]
            )
        }

        flash.message = "Trigger updated"
        redirect(action: 'show', params: [project: input.project, id: input.id])
    }

    def show(TaskRequest input) {

        //TODO: project exists?
        //TODO: auth?

        def task = TaskRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(task, 'Task', input.id)) {
            return
        }

        [task: task, project: input.project]
    }

    def test(TaskRequest input) {
        def trigger = TaskRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(trigger, 'Trigger', input.id)) {
            return
        }

        taskRunService.taskTriggerFired(input.id, taskRunService.contextForTask(trigger), params.data ?: [:])
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

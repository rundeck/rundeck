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
    def taskService
    PluginService pluginService
    static allowedMethods = [
            'createPost': 'POST',
            'deletePost': 'POST',
            'updatePost': 'POST',
    ]
    Map<String, Class> requiredPluginTypes = [actionPlugins: TaskAction, triggerPlugins: TaskTrigger]
    Collection<String> requiredPluginActionNames = ['create', 'edit', 'show', 'createPost', 'updatePost']


    def index(String project) {

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

    def createPost(TaskCreate input) {
        if (!requestHasValidToken()) {
            return
        }


        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, input.project)

        //TODO: auth

        Map triggerMap = ParamsUtil.cleanMap(params.triggerConfig)
        Map actionMap = ParamsUtil.cleanMap(params.actionConfig)

        Map userData = params.userData?.containsKey('0.key') ? ParamsUtil.parseIndexedMapParams(params.userData) :
                       ParamsUtil.cleanMap(params.userDataConfig)



        def result = taskService.createTask(authContext, input, triggerMap, actionMap, userData)

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

        //TODO: auth
        def task = TaskRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(task, 'Task', input.id)) {
            return
        }

        boolean result = taskService.deleteTask(task)

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
        def task = TaskRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(task, 'Task', input.id)) {
            return
        }
        System.err.println("params: ${params.actionConfig}")
        Map triggerMap = ParamsUtil.cleanMap(params.triggerConfig)
        Map actionMap = ParamsUtil.cleanMap(params.actionConfig)

        Map userData = params.userData?.containsKey("0.key") ? ParamsUtil.parseIndexedMapParams(params.userData) :
                       ParamsUtil.cleanMap(params.userDataConfig)

        //TODO; auth

        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, input.project)

        def result = taskService.updateTask(authContext, task, input, triggerMap, actionMap, userData)

        if (result.error) {
            //edit form
            request.error = 'Validation error'
            return render(
                    view: '/task/edit',
                    model: [task: task, validation: result.validation, project: input.project]
            )
        }

        flash.message = "Task updated"
        redirect(action: 'show', params: [project: input.project, id: input.id])
    }

    def show(TaskRequest input) {

        //TODO: auth?

        def task = TaskRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(task, 'Task', input.id)) {
            return
        }

        [task: task, project: input.project]
    }

    def test(TaskRequest input) {
        def task = TaskRep.findByProjectAndUuid(input.project, input.id)
        if (notFoundResponse(task, 'Task', input.id)) {
            return
        }

        taskService.taskTriggerFired(input.id, taskService.contextForTask(task), params.data ?: [:])
        flash.message = "Task started"
        redirect(action: 'show', params: params)
    }


}

class ParamsUtil {

    /**
     * Remove map entries where the value is null or a blank string
     * @param map
     * @return
     */
    static Map cleanMap(Map map) {
        def datamap = map ? map.entrySet().
            findAll { it.value && !it.key.startsWith('_') }.
            collectEntries { [it.key, it.value] } : [:]
        //parse map type entries
        return parseMapTypeEntries(datamap)

    }

    /**
     * Finds all "map" type entries, and converts them using the {@link #parseIndexedMapParams(java.util.Map)}.
     * A map entry is defined as an entry PREFIX, where a key PREFIX_.type is present with value "map",
     * and a PREFIX.map is present which can be parsed as an indexed map param. (Alternately, if the PREFIX value is a Map which contains a "map" entry, that is used.)
     * All entries starting with "PREFIX." are
     * removed and an entry PREFIX is created which contains the parsed indexed map.
     * @param datamap
     */
    public static Map parseMapTypeEntries(Map datamap) {
        def outmap = new HashMap(datamap)
        def types = datamap.keySet().findAll { it.endsWith('._type') }
        types.each { String typek ->
            def keyname = typek.substring(0, typek.length() - ('._type'.length()))
            def typeval = datamap.get(typek)
            def mapval = datamap.get(keyname + '.map')
            if (!mapval && datamap.get(keyname) instanceof Map) {
                mapval = datamap.get(keyname).get('map')
            }
            if (typeval == 'map' && (mapval instanceof Map)) {
                def pmap = parseIndexedMapParams(mapval)

                outmap[keyname] = pmap
                def entries = datamap.keySet().findAll { it.startsWith(keyname + '.') }
                entries.each { outmap.remove(it) }
            } else if (typeval == 'map' && !mapval) {
                //empty map
                outmap[keyname] = [:]
                def entries = datamap.keySet().findAll { it.startsWith(keyname + '.') }
                entries.each { outmap.remove(it) }

            }
        }
        outmap
    }
    /**
     * Parse input data with index key/value entries into a map.
     * If a key of the form "0.key" exists,  look for 0.value, to find a key/value pair, then
     * increment the index until no corresponding key is found. Empty keys are skipped, but
     * considered valid indexes. Empty or null values are interpreted as empty strings.
     *
     * @param map
     * @return the key/value data as a single map, or empty map if none is found
     */
    static Map parseIndexedMapParams(Map map) {
        int index = 0
        def data = [:]
        while (map != null && map.containsKey("${index}.key".toString())) {

            def key = map["${index}.key".toString()]
            def value = map["${index}.value".toString()]
            if (key) {
                data[key] = value?.toString() ?: ''
            }
            index++
        }
        data
    }
}
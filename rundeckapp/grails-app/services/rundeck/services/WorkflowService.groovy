package rundeck.services

import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowState
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateImpl
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateListener
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStepStateImpl
import com.dtolabs.rundeck.app.internal.workflow.WorkflowStateListenerAction
import com.dtolabs.rundeck.app.support.ExecutionContext
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.state.*
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import grails.converters.JSON
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Execution
import rundeck.JobExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.services.logging.ExecutionLogState
import rundeck.services.logging.WorkflowStateFileLoader
import rundeck.services.workflow.StateMapping

class WorkflowService implements ApplicationContextAware{
    public static final String STATE_FILE_FILETYPE = "state.json"

    protected def ExecutionService executionService
    def ApplicationContext applicationContext
    def LogFileStorageService logFileStorageService
    def grailsApplication
    static transactional = false
    def stateMapping = new StateMapping()
    /**
     * Cache of loaded execution state data after executions complete
     */
    def Cache<Long, Map> stateCache
    /**
     * in-memory states of executions while executions are running
     */
    Map<Long, WorkflowState> activeStates = new HashMap<Long, WorkflowState>()
    /**
     * initialized in bootstrap
     */
    void initialize()  {
        if(!executionService){
            executionService=applicationContext.executionService
        }
        def spec=grailsApplication.config.rundeck?.workflowService?.stateCache?.spec?: "maximumSize=5,expireAfterAccess=60s"
        stateCache= CacheBuilder.from(spec).build()
    }

    /**
     * Generate the mutable state container for the given job and workflow
     * @param execContext
     * @param wf
     * @param project
     * @param framework
     * @param jobcontext
     * @param secureOptions
     * @return
     */
    def MutableWorkflowState createStateForWorkflow(ExecutionContext execContext, Workflow wf, String project,
                                                    Framework framework, AuthContext authContext, Map jobcontext,
                                                    Map secureOptions) {
        //create a context used for workflow execution
        def context = executionService.createContext(execContext, null, framework,authContext, execContext.user,
                jobcontext,null, null, secureOptions)

        def workflow = createStateForWorkflow(wf, project, framework, context, secureOptions)

        return workflow
    }

    /**
     * Generate the mutable state container for the workflow, given workflow execution context info
     * @param wf
     * @param project
     * @param framework
     * @param parent
     * @param secureOptions
     * @return
     */
    def MutableWorkflowStateImpl createStateForWorkflow( Workflow wf, String project, Framework framework,
                                                    StepExecutionContext parent, Map secureOptions, StepIdentifier parentId=null) {

        Map<Integer, MutableWorkflowStepStateImpl> substeps = [:]
        wf.commands.eachWithIndex { WorkflowStep step, int ndx ->
            def stepId= StateUtils.stepIdentifierAppend(parentId, StateUtils.stepIdentifier(ndx + 1))
            if (step instanceof JobExec) {

                JobExec jexec = (JobExec) step
                def schedlist = ScheduledExecution.findAllScheduledExecutions(jexec.jobGroup, jexec.jobName, project)
                if (!schedlist || 1 != schedlist.size()) {
                    //skip
                    return
                }
                def id = schedlist[0].id

                ScheduledExecution se = ScheduledExecution.get(id)

                //generate a workflow context
                StepExecutionContext newContext=null
                try{
                    newContext=executionService.createJobReferenceContext(se,parent, OptsUtil.burst(jexec.argString?:''),false)
                }catch (ExecutionServiceValidationException e){
                    log.error("Error validating job reference context: "+e.message,e)
                    //invalid arguments
                }

                substeps[ndx] = new MutableWorkflowStepStateImpl(stepId,
                        createStateForWorkflow(se.workflow, project,framework,newContext,secureOptions))
            } else {
                substeps[ndx] = new MutableWorkflowStepStateImpl(stepId)
            }
            substeps[ndx].nodeStep = !!step.nodeStep
        }
        return new MutableWorkflowStateImpl(parent ? (parent.nodes.nodeNames as List) : null, wf.commands.size(),
                substeps, parentId,framework.frameworkNodeName)
    }
    /**
     * Create and return a listener for changes to the workflow state for an execution
     * @param execution
     */
    def WorkflowExecutionListener createWorkflowStateListenerForExecution(Execution execution, Framework framework,
            AuthContext authContext, Map jobcontext, Map secureOpts) {
        final long id = execution.id

        MutableWorkflowState state = createStateForWorkflow(execution, execution.workflow, execution.project, framework,
                authContext, jobcontext, secureOpts)

        activeStates.put(id, state)
        def mutablestate = new MutableWorkflowStateListener(state)
        def chain = [mutablestate]
        def File outfile = getStateFileForExecution(execution)
        def storagerequest = logFileStorageService.prepareForFileStorage(execution, STATE_FILE_FILETYPE, outfile)
        chain << new WorkflowStateListenerAction(onWorkflowExecutionStateChanged: {
            ExecutionState executionState, Date timestamp, List<String> nodeSet ->
                if (executionState.completedState) {
                    //workflow finished:
                    persistExecutionState(storagerequest, execution.id, state, outfile)
                }
        })
        new WorkflowExecutionStateListenerAdapter(chain)
    }

    /**
     * Return the file for the state.json for the execution
     * @param execution
     * @return
     */
    public File getStateFileForExecution(Execution execution) {
        logFileStorageService.getFileForExecutionFiletype(execution, STATE_FILE_FILETYPE, true)
    }

    def persistExecutionState(Closure storagerequest, Long id, WorkflowState state, File file) {
        Map data=serializeStateJson(id, state, file)
        stateCache.put(id, data)
        activeStates.remove(id)
        storagerequest?.call()
        log.debug("${id}: execution state.json persisted to file. [submitted for remote storage? ${storagerequest?true:false}]")
    }

    def Map serializeStateJson(Long id,WorkflowState state, File file){
        def data = stateMapping.mapOf(id, state)
        file.withWriter { w->
            w << data.encodeAsJSON()
        }
        return data
    }
    def Map deserializeState(File file){
        if(file.canRead()){
            return JSON.parse(file.text)
        }
        return null
    }

    /**
     * Return an WorkflowStateFileLoader containing state of logfile availability, and content if available
     * @param e execution
     * @param performLoad if true, perform remote file transfer
     */
    WorkflowStateFileLoader requestState(Execution e, boolean performLoad = true) {

        //look for active state
        def state1 = activeStates[e.id]
        if (state1) {
            def state= stateMapping.mapOf(e.id, state1)
            return new WorkflowStateFileLoader(workflowState: state, state: ExecutionLogState.AVAILABLE)
        }

        //look for cached local data
        def statemap=stateCache.getIfPresent(e.id)
        if (statemap) {
            return new WorkflowStateFileLoader(workflowState: statemap, state: ExecutionLogState.AVAILABLE)
        }

        //request file via file storage
        def loader = logFileStorageService.requestLogFileLoad(e, STATE_FILE_FILETYPE, performLoad)

        if (loader.file) {
            //cache local data
            statemap = deserializeState(loader.file)
            stateCache.put(e.id,statemap)
        }
        return new WorkflowStateFileLoader(workflowState: statemap, state: loader.state, errorCode: loader.errorCode,
                errorData: loader.errorData, file: loader.file)
    }
}

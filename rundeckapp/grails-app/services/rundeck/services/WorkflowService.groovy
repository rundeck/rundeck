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

package rundeck.services

import com.dtolabs.rundeck.app.internal.workflow.LogMutableWorkflowState
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowState
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateImpl
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateListener
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStepStateImpl
import com.dtolabs.rundeck.app.internal.workflow.WorkflowStateListenerAction
import com.dtolabs.rundeck.app.internal.workflow.ExceptionHandlingMutableWorkflowState
import com.dtolabs.rundeck.app.support.ExecutionContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.IWorkflow
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.state.*
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.jobs.JobReferenceItem
import com.dtolabs.rundeck.core.jobs.SubWorkflowExecutionItem
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import grails.converters.JSON
import grails.util.Environment
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import rundeck.services.logging.WorkflowStateFileLoader
import rundeck.services.workflow.StateMapping
import java.util.concurrent.ConcurrentHashMap

class WorkflowService implements ApplicationContextAware{
    public static final String STATE_FILE_FILETYPE = "state.json"

    protected def ExecutionService executionService
    def ApplicationContext applicationContext
    def LogFileStorageService logFileStorageService
    def ConfigurationService configurationService
    WorkflowStateDataLoader workflowStateDataLoader
    ExecutionUtilService executionUtilService
    static transactional = false
    def stateMapping = new StateMapping()
    /**
     * Cache of loaded execution state data after executions complete
     */
    def Cache<Long, Map> stateCache
    /**
     * in-memory states of executions while executions are running
     */
    Map<Long, WorkflowState> activeStates = new ConcurrentHashMap<>()
    /**
     * initialized in bootstrap
     */
    void initialize()  {
        if(!executionService){
            executionService=applicationContext.executionService
        }
        def spec= configurationService.getString("workflowService.stateCache.spec", "maximumSize=5,expireAfterAccess=60s")
        stateCache= CacheBuilder.from(spec).build()
    }

    WorkflowState getActiveState(Long execId) {
        activeStates[execId]
    }

    Map getCachedState(Long execId) {
        stateCache.getIfPresent(execId)
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
    def MutableWorkflowState createStateForWorkflow(ExecutionContext execContext, IWorkflow wf, String project,
                                                    IFramework framework,
                                                    UserAndRolesAuthContext authContext,
                                                    Map jobcontext,
                                                    Map secureOptions) {
        //create a context used for workflow execution
        def context = executionService.createContext(execContext, null, framework,authContext, execContext.user,
                jobcontext,null, null,null, secureOptions)

        def workflow = createStateForWorkflow(wf, project, framework.frameworkNodeName, context, secureOptions)

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
    def MutableWorkflowStateImpl createStateForWorkflow(IWorkflow wf, String project, String frameworkNodeName,
                                                        StepExecutionContext parent, Map secureOptions, StepIdentifier parentId=null) {

        Map<Integer, MutableWorkflowStepStateImpl> substeps = [:]
        wf.commands.eachWithIndex { StepExecutionItem step, int ndx ->
            def stepId= StateUtils.stepIdentifierAppend(parentId, StateUtils.stepIdentifier(ndx + 1))
            if (step instanceof JobReferenceItem) {

                JobReferenceItem jexec = (JobReferenceItem) step
                ScheduledExecution se = findJob(jexec, project)
                if (!se) {
                    //skip
                    return
                }

                //generate a workflow context
                StepExecutionContext newContext=null
                try {
                    def jobArgs = jexec.args
                    newContext = executionService.createJobReferenceContext(
                            se,
                            null,
                            parent,
                            jobArgs,
                            jexec.nodeFilter,
                            jexec.nodeKeepgoing,
                            jexec.nodeThreadcount,
                            jexec.nodeRankAttribute,
                            jexec.nodeRankOrderAscending,
                            null,
                            jexec.nodeIntersect,
                            jexec.importOptions,
                            false,
                            jexec.childNodes
                    )
                } catch (ExecutionServiceValidationException e) {
                    log.error("Error validating job reference context: "+e.message,e)
                    //invalid arguments
                }

                WorkflowExecutionItem item = executionUtilService.createExecutionItemForWorkflow(se.workflow)

                substeps[ndx] = new MutableWorkflowStepStateImpl(stepId,
                        createStateForWorkflow(item.workflow, project,frameworkNodeName,newContext,secureOptions))
            } else if (step instanceof SubWorkflowExecutionItem) {
                SubWorkflowExecutionItem rstep = (SubWorkflowExecutionItem) step
                substeps[ndx] = new MutableWorkflowStepStateImpl(stepId,
                        createStateForWorkflow(rstep.subWorkflow.workflow, project,frameworkNodeName,parent,secureOptions))
            }else{
                def mutableStep = new MutableWorkflowStepStateImpl(stepId)
                if(step.getRunner()){
                    mutableStep.runnerNode = step.getRunner().nodename
                }
                substeps[ndx] = mutableStep
            }

            boolean isNodeStep = false
            if(step instanceof NodeStepExecutionItem){
                isNodeStep = true
            }

            substeps[ndx].nodeStep = isNodeStep
        }
        return new MutableWorkflowStateImpl(parent ? (parent.nodes.nodeNames as List) : null, wf.commands.size(),
                substeps, parentId,frameworkNodeName)
    }


    /**
     * Create and return a listener for changes to the workflow state for an execution
     * @param execution
     */
    def WorkflowExecutionListener createWorkflowStateListenerForExecution(
        Execution execution,
        IWorkflow workflow,
        IFramework framework,
        UserAndRolesAuthContext authContext,
        Map jobcontext,
        Map secureOpts
    ) {
        final long id = execution.id

        MutableWorkflowState state = createStateForWorkflow(execution, workflow, execution.project, framework,
                authContext, jobcontext, secureOpts)
        def logstate
        if(Environment.getCurrent() == Environment.DEVELOPMENT){
            //add state change logger in dev mode
            logstate = new LogMutableWorkflowState(state)
            state = logstate
        }
        state = new ExceptionHandlingMutableWorkflowState(state)
        state.runtimeExceptionHandler = { name, Exception e ->
            log.error(name + ": " + e.message, e)
            if (Environment.getCurrent() == Environment.DEVELOPMENT) {
                //print state change list in dev mode
                log.error(logstate.stateChanges.encodeAsJSON().toString())
                log.error(stateMapping.mapOf(id, state).encodeAsJSON().toString())
            }
        }


        activeStates.put(id, state)
        def mutablestate = new MutableWorkflowStateListener(state)
        def chain = [mutablestate]
        def File outfile = getStateFileForExecution(execution)
        chain << new WorkflowStateListenerAction(
                onWorkflowExecutionStateChanged: { ExecutionState executionState, Date timestamp, List<String> nodeSet ->
                    if (executionState.completedState) {
                        persistExecutionState(execution.id, state, outfile, true)
                    }
                },
                onStepStateChanged: { StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp ->
                    persistExecutionState(execution.id, state, outfile)
                }
        )
        if (Environment.getCurrent() == Environment.DEVELOPMENT) {
            chain << new WorkflowStateListenerAction(onWorkflowExecutionStateChanged: {
                ExecutionState executionState, Date timestamp, List<String> nodeSet ->
                    if (executionState.completedState) {
                        log.debug(logstate.stateChanges.encodeAsJSON().toString())
                    }
            })
        }
        new WorkflowExecutionStateListenerAdapter(chain)
    }

    /**
     * Return the file for the state.json for the execution
     * @param execution
     * @return
     */
    public File getStateFileForExecution(Execution execution) {
        logFileStorageService.getFileForExecutionFiletype(execution, STATE_FILE_FILETYPE, false, false)
    }

    /**
     * Return the file for the state.json for the execution
     * @param execution
     * @return
     */
    public File getStateFileForExecution(ExecutionReference execution) {
        logFileStorageService.getFileForExecutionFiletype(execution, STATE_FILE_FILETYPE, false)
    }

    void persistExecutionState(Long id, WorkflowState state, File file, boolean finalState = false) {
        Map data = serializeStateJson(id, state, file)
        if(finalState){
            stateCache.put(id, data)
            activeStates.remove(id)
        }
        log.debug("${id}: execution state.json persisted to file.")
    }

    def Map serializeStateJson(Long id, WorkflowState state, File file) {
        def data = stateMapping.mapOf(id, state)
        serializeStateDataJson(id, data, file)
    }

    def Map serializeStateDataJson(Long id, Map data, File file) {
        file.withWriter { w ->
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
            return new WorkflowStateFileLoader(workflowState: state, state: ExecutionFileState.AVAILABLE)
        }

        //look for cached local data
        def statemap=stateCache.getIfPresent(e.id)
        if (statemap) {
            return new WorkflowStateFileLoader(workflowState: statemap, state: ExecutionFileState.AVAILABLE)
        }

        //request file via file storage
        def loader = workflowStateDataLoader.loadWorkflowStateData(e.asReference(), performLoad)

        if (loader.file) {
            //cache local data
            statemap = deserializeState(loader.file)
            stateCache.put(e.id,statemap)
        } else if(loader.stream) {
            statemap = JSON.parse(loader.stream, "UTF-8")
            stateCache.put(e.id,statemap)
        }
        return new WorkflowStateFileLoader(workflowState: statemap, state: loader.state, errorCode: loader.errorCode,
                errorData: loader.errorData, file: loader.file)
    }
    /**
     * Summarize the data for only the selected nodes
     * @param e execution
     * @param nodes list of selected nodes to get state for
     * @param selectedOnly if ture, only get state for the nodes, otherwise all node states are returned
     * @param performLoad if true, ask log storage to load the log if necessary
     * @param stepStates if true, include individual step states
     * @return
     */
    WorkflowStateFileLoader requestStateSummary(
            Execution e,
            List<String> nodes,
            boolean selectedOnly = false,
            boolean performLoad = true,
            boolean stepStates = false
    )
    {
        //look for active state
        def state1 = activeStates[e.id]
        if (state1) {
            def state= stateMapping.mapOf(e.id, state1)
            state=stateMapping.summarize(new HashMap(state),nodes,selectedOnly,stepStates)
            return new WorkflowStateFileLoader(workflowState: state, state: ExecutionFileState.AVAILABLE)
        }

        //look for cached local data
        def statemap=stateCache.getIfPresent(e.id)
        if (statemap && !configurationService.getBoolean("clusterMode.enabled", false)) {
            statemap=stateMapping.summarize(new HashMap(statemap),nodes,selectedOnly,stepStates)
            return new WorkflowStateFileLoader(workflowState: statemap, state: ExecutionFileState.AVAILABLE)
        }

        //request file via file storage
        def loader = workflowStateDataLoader.loadWorkflowStateData(e.asReference(), performLoad)

        if (loader.file) {
            //cache local data
            statemap = deserializeState(loader.file)
            if (loader.state == ExecutionFileState.AVAILABLE) {
                //final state, so cache result.  If AVAILABLE_PARTIAL, do not cache it
                stateCache.put(e.id, statemap)
            }
            statemap=stateMapping.summarize(new HashMap(statemap),nodes,selectedOnly,stepStates)
        } else if(loader.stream) {
            //cache local data
            statemap = JSON.parse(loader.stream, "UTF-8")
            if (loader.state == ExecutionFileState.AVAILABLE) {
                //final state, so cache result.  If AVAILABLE_PARTIAL, do not cache it
                stateCache.put(e.id, statemap)
            }
            try {
                statemap = stateMapping.summarize(new HashMap(statemap), nodes, selectedOnly, stepStates)
            } catch (Exception ex) {
                log.error("Failed to summarize state", ex)
                println "State data: " + new ObjectMapper().writeValueAsString(statemap)
            }
        }
        return new WorkflowStateFileLoader(
                workflowState: statemap,
                state: loader.state,
                errorCode: loader.errorCode,
                errorData: loader.errorData,
                file: loader.file,
                retryBackoff: loader.retryBackoff
        )
    }

    private List scanWorkflowsWithConfigError973(){
        List strategies = Workflow.createCriteria().list {
            projections {
                distinct('strategy')
            }
        }

        def workflowsWithRulesetError = []
        strategies?.each {String strg ->
            workflowsWithRulesetError += Workflow.createCriteria().list {
                like('pluginConfig','%\\{\"WorkflowStrategy\":\\{\"' + strg + '\":\\{\"' + strg + '\"%')
            }
        }

        return workflowsWithRulesetError
    }

    /**
     * Correct invalid imported data for Workflow config
     * rundeck 3.2.4-3.2.6, issue 973
     * @return
     */
    public Map applyWorkflowConfigFix973(){
        Map result = [:]
        result.success = true

        log.info("Searching for workflows with config errors")
        List workflowToBeFixed = scanWorkflowsWithConfigError973()

        if(workflowToBeFixed?.size() > 0){
            log.warn("Found ${workflowToBeFixed?.size()} workflows with config errors")
        } else {
            log.info("No workflow with config error was found")
        }

        result.invalidCount = workflowToBeFixed?.size()
        result.changesetList = []

        workflowToBeFixed?.each {Workflow w->
            Map changeset = [:]

            changeset.workflowId = w.id

            if(w.validatePluginConfigMap()){
                String message = "The workflow config for ${w.id} is valid and will not be fixed"
                changeset.result = message
                log.warn(message)
                return
            }

            log.info("Fixing workflow config for ${w.id}: ${w.pluginConfig}")

            changeset.before = w.pluginConfig

            def map = w.getPluginConfigMap()

            if(map && (map[ServiceNameConstants.WorkflowStrategy] instanceof Map)
                && (map[ServiceNameConstants.WorkflowStrategy][w.strategy] instanceof Map)
                && map[ServiceNameConstants.WorkflowStrategy][w.strategy][w.strategy]){
                map[ServiceNameConstants.WorkflowStrategy] = map[ServiceNameConstants.WorkflowStrategy][w.strategy]
            }

            w.setPluginConfigMap(map)

            log.info("Fixed workflow config for ${w.id}: ${w.pluginConfig}")

            changeset.after = w.pluginConfig

            if(!w.validatePluginConfigMap()){
                log.error("The workflow config ${w.id} ${w.pluginConfig} is not valid and will not be saved")
                return
            }

            if(!w.save()){
                String message = "Error saving config fix for workflow ${w.id}"
                changeset.result = message
                log.error(message)
                result.success = false
            } else {
                changeset.result = 'success'
            }

            result.changesetList += changeset
        }

        return result;
    }


    ScheduledExecution findJob(JobReferenceItem jobRef, String project) {
        if (!jobRef.useName && jobRef.uuid) {
            return ScheduledExecution.findByUuid(jobRef.uuid)
        } else {
            String jobIdentifier = jobRef.jobIdentifier
            String jobName = jobIdentifier
            String jobGroup = null

            // Parse the jobIdentifier to extract jobGroup and jobName
            // Format is: "groupPath/jobName" or just "jobName" if no group
            if (jobIdentifier?.contains('/')) {
                int lastSlash = jobIdentifier.lastIndexOf('/')
                jobGroup = jobIdentifier.substring(0, lastSlash)
                jobName = jobIdentifier.substring(lastSlash + 1)
            } else {
                jobName = jobIdentifier
                jobGroup = null
            }

            return ScheduledExecution.findByProjectAndJobNameAndGroupPath(
                    jobRef.project ?: project,
                    jobName,
                    jobGroup ?: null
            )
        }
    }
}

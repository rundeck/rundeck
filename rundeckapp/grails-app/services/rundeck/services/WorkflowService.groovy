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
import com.dtolabs.rundeck.core.execution.workflow.HasParentStepContext
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
    ScheduledExecutionService scheduledExecutionService
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

        // The flat command list may contain runs of items implementing HasParentStepContext
        // (sub-steps of a Conditional that were flattened by ExecutionUtilService.consolidateWorkflowSteps).
        // Group those contiguous runs under a single parent MutableWorkflowStepStateImpl so the
        // state tree mirrors the original (un-flattened) job definition layout.
        Map<Integer, MutableWorkflowStepStateImpl> substeps = [:]
        List<StepExecutionItem> commands = wf.commands as List<StepExecutionItem>
        int logicalIdx = 0
        int i = 0
        while (i < commands.size()) {
            StepExecutionItem step = commands[i]
            HasParentStepContext parentedHead = asConditionalSubStep(step)
            if (parentedHead != null) {
                int parentStepNum = parentedHead.parentStepNumber
                int groupEnd = i
                while (groupEnd < commands.size()) {
                    HasParentStepContext h = asConditionalSubStep(commands[groupEnd])
                    if (h == null || h.parentStepNumber != parentStepNum) break
                    groupEnd++
                }
                logicalIdx++
                StepIdentifier groupStepId = StateUtils.stepIdentifierAppend(parentId, StateUtils.stepIdentifier(logicalIdx))
                MutableWorkflowStateImpl innerWorkflow = buildConditionalSubWorkflowState(
                        commands, i, groupEnd, project, frameworkNodeName, parent, secureOptions, groupStepId
                )
                MutableWorkflowStepStateImpl groupState = new MutableWorkflowStepStateImpl(groupStepId, innerWorkflow)
                // a Conditional wrapper is not itself a node step; node-step semantics belong to its sub-steps.
                groupState.nodeStep = false
                substeps[logicalIdx - 1] = groupState
                i = groupEnd
                continue
            }

            logicalIdx++
            int ndx = logicalIdx - 1
            StepIdentifier stepId = StateUtils.stepIdentifierAppend(parentId, StateUtils.stepIdentifier(logicalIdx))
            substeps[ndx] = buildStepStateForItem(step, stepId, project, frameworkNodeName, parent, secureOptions, parentId)
            if (substeps[ndx] == null) {
                // a JobReferenceItem whose target job no longer exists — skip the slot.
                substeps.remove(ndx)
                logicalIdx--
            }
            i++
        }
        return new MutableWorkflowStateImpl(parent ? (parent.nodes.nodeNames as List) : null, logicalIdx,
                substeps, parentId,frameworkNodeName)
    }

    /**
     * Build the nested {@link MutableWorkflowStateImpl} that holds the state for a contiguous
     * run of flattened conditional sub-step items (commands[start..end-1] all share the same
     * {@link HasParentStepContext#getParentStepNumber()}).
     *
     * Sub-step indices come from each item's {@link HasParentStepContext#getSubStepNumber()}
     * so the inner state slots line up with the hierarchical {@code stepctx} ("parent/sub")
     * emitted by the listeners.
     */
    private MutableWorkflowStateImpl buildConditionalSubWorkflowState(
            List<StepExecutionItem> commands,
            int start,
            int end,
            String project,
            String frameworkNodeName,
            StepExecutionContext parent,
            Map secureOptions,
            StepIdentifier groupStepId
    ) {
        Map<Integer, MutableWorkflowStepStateImpl> innerSubsteps = [:]
        int maxSubStep = 0
        for (int j = start; j < end; j++) {
            StepExecutionItem inner = commands[j]
            HasParentStepContext h = (HasParentStepContext) inner
            int subStepNumber = h.subStepNumber
            if (subStepNumber > maxSubStep) maxSubStep = subStepNumber
            // Use a LOCAL 1-based identifier within the sub-workflow (e.g. [1], [2]).
            // The serializer (StateMapping.stepctxToString) prepends the outer parent context
            // automatically, so using the full path here would produce an extra level (e.g. "1/1/1").
            StepIdentifier innerStepId = StateUtils.stepIdentifier(subStepNumber)
            MutableWorkflowStepStateImpl innerState = buildStepStateForItem(
                    inner, innerStepId, project, frameworkNodeName, parent, secureOptions, null
            )
            if (innerState != null) {
                innerSubsteps[subStepNumber - 1] = innerState
            }
        }
        // parentStepId is null so that any auto-created slots inside the sub-workflow also
        // get local 1-based identifiers (matching normal job-reference sub-workflow behaviour).
        return new MutableWorkflowStateImpl(
                parent ? (parent.nodes.nodeNames as List) : null,
                maxSubStep,
                innerSubsteps,
                null,
                frameworkNodeName
        )
    }

    /**
     * Build a {@link MutableWorkflowStepStateImpl} for a single flat {@link StepExecutionItem},
     * using the same JobReferenceItem / SubWorkflowExecutionItem / plain-step branching as the
     * historical implementation. Returns {@code null} when the step is a job-ref to a missing
     * target job (caller should skip the slot).
     */
    private MutableWorkflowStepStateImpl buildStepStateForItem(
            StepExecutionItem step,
            StepIdentifier stepId,
            String project,
            String frameworkNodeName,
            StepExecutionContext parent,
            Map secureOptions,
            StepIdentifier parentId
    ) {
        boolean isNodeStep = false
        MutableWorkflowStepStateImpl built = null

        if (step instanceof JobReferenceItem) {
            JobReferenceItem jexec = (JobReferenceItem) step
            ScheduledExecution se = scheduledExecutionService.findJobFromJobReference(jexec, project)
            if (!se) {
                return null
            }

            StepExecutionContext newContext = null
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
            }

            WorkflowExecutionItem item = jexec.getWorkflow()
            isNodeStep = jexec.nodeStep
            built = new MutableWorkflowStepStateImpl(stepId,
                    createStateForWorkflow(item.workflow, project, frameworkNodeName, newContext, secureOptions))
        } else if (step instanceof SubWorkflowExecutionItem) {
            SubWorkflowExecutionItem rstep = (SubWorkflowExecutionItem) step
            built = new MutableWorkflowStepStateImpl(stepId,
                    createStateForWorkflow(rstep.subWorkflow.workflow, project, frameworkNodeName, parent, secureOptions))
        } else {
            built = new MutableWorkflowStepStateImpl(stepId)
            if (step.getRunner() && parentId == null) {
                built.runnerNode = step.getRunner().nodename
            }
            if (step instanceof NodeStepExecutionItem) {
                isNodeStep = true
            }
        }
        built.nodeStep = isNodeStep
        return built
    }

    /**
     * @return the item viewed as a flattened conditional sub-step (positive parent and sub
     * indices), or {@code null} if it is a flat top-level step.
     */
    private static HasParentStepContext asConditionalSubStep(StepExecutionItem step) {
        if (!(step instanceof HasParentStepContext)) {
            return null
        }
        HasParentStepContext parented = (HasParentStepContext) step
        if (parented.parentStepNumber <= 0 || parented.subStepNumber <= 0) {
            return null
        }
        return parented
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

}

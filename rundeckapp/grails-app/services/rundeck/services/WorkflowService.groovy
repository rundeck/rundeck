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
                // For nested conditionals, only group items that are at the same nesting level
                // (same parent path, not just same parentStepNumber)
                List<Integer> parentPath = parentedHead.parentStepPath
                int parentStepNum = parentedHead.parentStepNumber
                int groupEnd = i

                // Group consecutive items that share the same parent context
                while (groupEnd < commands.size()) {
                    HasParentStepContext h = asConditionalSubStep(commands[groupEnd])
                    if (h == null) break

                    // For single-level conditionals (no parentStepPath), match by parentStepNumber
                    if (parentPath == null) {
                        if (h.parentStepNumber != parentStepNum) break
                    } else {
                        // For nested conditionals, items belong to the same group if they have
                        // the same parent path OR if their path starts with this path
                        // (meaning they're nested under this conditional)
                        List<Integer> hPath = h.parentStepPath
                        if (hPath == null) {
                            // Single-level conditional substep - check if it belongs to the same top-level parent
                            // This handles mixed conditionals with both nested and direct substeps
                            if (h.parentStepNumber != parentStepNum) {
                                break  // Different top-level parent, end group
                            }
                            // Same parent, continue grouping
                        } else {
                            // Only include items at the same level (exact path match) or nested deeper
                            if (!pathEquals(hPath, parentPath) && !pathStartsWith(hPath, parentPath)) {
                                break
                            }
                            // Stop if we encounter a different top-level parent
                            if (hPath[0] != parentStepNum) {
                                break
                            }
                        }
                    }
                    groupEnd++
                }

                logicalIdx++
                StepIdentifier groupStepId = StateUtils.stepIdentifierAppend(parentId, StateUtils.stepIdentifier(logicalIdx))
                // Pass [parentStepNum] as currentLevelPath - this is the level we're building,
                // not the first item's full parent path
                List<Integer> currentLevelPath = [parentStepNum]
                MutableWorkflowStateImpl innerWorkflow = buildConditionalSubWorkflowState(
                        commands, i, groupEnd, project, frameworkNodeName, parent, secureOptions, groupStepId, currentLevelPath
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
            def stepState = buildStepStateForItem(step, stepId, project, frameworkNodeName, parent, secureOptions, parentId)
            if (stepState != null) {
                substeps[ndx] = stepState
            }
            // If stepState is null (e.g., a JobReferenceItem whose target job no longer exists),
            // we skip adding it to the map. The MutableWorkflowStateImpl constructor will create
            // a placeholder step for the missing index, preserving the logical step slot so later
            // steps keep their original logical numbering and remain aligned with listener-emitted
            // step contexts.
            i++
        }
        return new MutableWorkflowStateImpl(parent ? (parent.nodes.nodeNames as List) : null, logicalIdx,
                substeps, parentId,frameworkNodeName)
    }

    /**
     * Build the nested {@link MutableWorkflowStateImpl} that holds the state for a contiguous
     * run of flattened conditional sub-step items.
     *
     * For nested conditionals, this method recursively builds the state tree by grouping
     * items by their substep number at the current nesting level and creating nested
     * sub-workflows for items that belong to deeper levels.
     *
     * @param currentLevelPath The parent path of the current nesting level (e.g., [2] or [2, 2])
     */
    private MutableWorkflowStateImpl buildConditionalSubWorkflowState(
            List<StepExecutionItem> commands,
            int start,
            int end,
            String project,
            String frameworkNodeName,
            StepExecutionContext parent,
            Map secureOptions,
            StepIdentifier groupStepId,
            List<Integer> currentLevelPath = null
    ) {
        Map<Integer, MutableWorkflowStepStateImpl> innerSubsteps = [:]
        int maxSubStep = 0
        int j = start


        while (j < end) {
            StepExecutionItem inner = commands[j]
            HasParentStepContext h = (HasParentStepContext) inner
            List<Integer> itemPath = h.parentStepPath

            // Determine the substep number at the current level
            int substepAtThisLevel
            boolean isDirectChild

            if (itemPath == null) {
                // Single-level conditional: item is a direct child
                substepAtThisLevel = h.subStepNumber
                isDirectChild = true
            } else if (pathEquals(itemPath, currentLevelPath)) {
                // Item's parent path equals current level: it's a direct child
                substepAtThisLevel = h.subStepNumber
                isDirectChild = true
            } else if (pathStartsWith(itemPath, currentLevelPath)) {
                // Item's parent path starts with current level: it's nested deeper
                substepAtThisLevel = itemPath[currentLevelPath.size()]
                isDirectChild = false
            } else {
                // This shouldn't happen in a properly grouped set
                log.warn("Unexpected item at level ${currentLevelPath}: itemPath=${itemPath}")
                j++
                continue
            }

            if (substepAtThisLevel > maxSubStep) maxSubStep = substepAtThisLevel

            if (isDirectChild) {
                // This is a leaf step at the current level
                StepIdentifier innerStepId = StateUtils.stepIdentifier(substepAtThisLevel)
                MutableWorkflowStepStateImpl innerState = buildStepStateForItem(
                        inner, innerStepId, project, frameworkNodeName, parent, secureOptions, null
                )
                if (innerState != null) {
                    innerSubsteps[substepAtThisLevel - 1] = innerState
                }
                j++
            } else {
                // This item and potentially following items belong to a nested level
                // Group all items that belong to this substep at the current level
                int groupEnd = j
                List<Integer> nestedLevelPath = buildNestedPath(currentLevelPath, substepAtThisLevel)

                while (groupEnd < end) {
                    HasParentStepContext candidate = (HasParentStepContext) commands[groupEnd]
                    List<Integer> candidatePath = candidate.parentStepPath

                    // Check if this candidate belongs to the same nested group
                    if (candidatePath == null || !pathStartsWith(candidatePath, nestedLevelPath)) {
                        break
                    }
                    groupEnd++
                }

                // Recursively build the nested workflow
                StepIdentifier innerStepId = StateUtils.stepIdentifier(substepAtThisLevel)
                MutableWorkflowStateImpl nestedWorkflow = buildConditionalSubWorkflowState(
                        commands, j, groupEnd, project, frameworkNodeName, parent, secureOptions, innerStepId, nestedLevelPath
                )
                MutableWorkflowStepStateImpl nestedState = new MutableWorkflowStepStateImpl(innerStepId, nestedWorkflow)
                nestedState.nodeStep = false
                innerSubsteps[substepAtThisLevel - 1] = nestedState
                j = groupEnd
            }
        }

        return new MutableWorkflowStateImpl(
                parent ? (parent.nodes.nodeNames as List) : null,
                maxSubStep,
                innerSubsteps,
                null,
                frameworkNodeName
        )
    }

    /**
     * Build the path for a nested level by appending the substep number to the current level path.
     */
    private static List<Integer> buildNestedPath(List<Integer> currentLevelPath, int substep) {
        if (currentLevelPath == null) {
            return [substep]
        }
        List<Integer> result = new ArrayList<>(currentLevelPath)
        result.add(substep)
        return result
    }

    /**
     * Check if two parent paths are equal.
     */
    private static boolean pathEquals(List<Integer> path1, List<Integer> path2) {
        if (path1 == null && path2 == null) return true
        if (path1 == null || path2 == null) return false
        if (path1.size() != path2.size()) return false
        for (int i = 0; i < path1.size(); i++) {
            if (path1[i] != path2[i]) return false
        }
        return true
    }

    /**
     * Check if a parent path starts with a given prefix.
     * E.g., [2, 2, 1] starts with [2, 2], but not with [2, 3]
     */
    private static boolean pathStartsWith(List<Integer> path, List<Integer> prefix) {
        if (path == null || prefix == null || path.size() < prefix.size()) {
            return false
        }
        for (int i = 0; i < prefix.size(); i++) {
            if (path[i] != prefix[i]) {
                return false
            }
        }
        return true
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

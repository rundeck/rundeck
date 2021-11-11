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
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.state.*
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import grails.converters.JSON
import grails.util.Environment
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Execution
import rundeck.JobExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep
import org.rundeck.app.services.ExecutionFile

import org.rundeck.app.services.ExecutionFileProducer
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import rundeck.services.logging.ProducedExecutionFile
import rundeck.services.logging.WorkflowStateFileLoader
import rundeck.services.workflow.StateMapping

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

class WorkflowService implements ApplicationContextAware,ExecutionFileProducer{
    public static final String STATE_FILE_FILETYPE = "state.json"
    final String executionFileType = STATE_FILE_FILETYPE

    protected def ExecutionService executionService
    def ApplicationContext applicationContext
    def LogFileStorageService logFileStorageService
    def grailsApplication
    def ConfigurationService configurationService
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

    @Override
    boolean isExecutionFileGenerated() {
        return false
    }

    @Override
    boolean isCheckpointable() {
        return true
    }

    @Override
    ExecutionFile produceStorageFileForExecution(final ExecutionReference e) {
        File localfile = getStateFileForExecution(e)
        new ProducedExecutionFile(localFile: localfile,fileDeletePolicy: ExecutionFile.DeletePolicy.WHEN_RETRIEVABLE)
    }

    ExecutionFile produceStorageFileForExecution(final Execution e) {
        File localfile = getStateFileForExecution(e)
        new ProducedExecutionFile(localFile: localfile,fileDeletePolicy: ExecutionFile.DeletePolicy.WHEN_RETRIEVABLE)
    }

    @Override
    ExecutionFile produceStorageCheckpointForExecution(final ExecutionReference e) {
        long eid=Long.parseLong(e.id)
        File tempFile
        if (activeStates[eid]) {
            tempFile = Files.createTempFile("WorkflowService-storage-${eid}", ".json").toFile()
            persistExecutionStateCheckpoint(eid, activeStates[eid], tempFile)
            return new ProducedExecutionFile(
                    localFile: tempFile,
                    fileDeletePolicy: ExecutionFile.DeletePolicy.ALWAYS
            )
        }
        File localfile = getStateFileForExecution(e)

        def localproduced = new ProducedExecutionFile(
                localFile: getStateFileForExecution(e),
                fileDeletePolicy: ExecutionFile.DeletePolicy.WHEN_RETRIEVABLE
        )
        if (e.dateCompleted != null && localfile.exists()) {
            return localproduced
        }
        def statemap = stateCache.getIfPresent(eid)
        if (statemap) {
            tempFile = Files.createTempFile("WorkflowService-storage-${eid}", ".json").toFile()
            serializeStateDataJson(eid, statemap, tempFile)
            return new ProducedExecutionFile(
                    localFile: tempFile,
                    fileDeletePolicy: ExecutionFile.DeletePolicy.ALWAYS
            )
        }
        return localproduced
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
    def MutableWorkflowStateImpl createStateForWorkflow( Workflow wf, String project, String frameworkNodeName,
                                                    StepExecutionContext parent, Map secureOptions, StepIdentifier parentId=null) {

        Map<Integer, MutableWorkflowStepStateImpl> substeps = [:]
        wf.commands.eachWithIndex { WorkflowStep step, int ndx ->
            def stepId= StateUtils.stepIdentifierAppend(parentId, StateUtils.stepIdentifier(ndx + 1))
            if (step instanceof JobExec) {

                JobExec jexec = (JobExec) step
                def searchProject = jexec.jobProject? jexec.jobProject: project
                def schedlist = ScheduledExecution.findAllScheduledExecutions(jexec.jobGroup, jexec.jobName, searchProject)
                if (!schedlist || 1 != schedlist.size()) {
                    //skip
                    return
                }
                def id = schedlist[0].id

                ScheduledExecution se = ScheduledExecution.get(id)

                //generate a workflow context
                StepExecutionContext newContext=null
                try {
                    def jobArgs = OptsUtil.burst(jexec.argString ?: '')
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

                substeps[ndx] = new MutableWorkflowStepStateImpl(stepId,
                        createStateForWorkflow(se.workflow, project,frameworkNodeName,newContext,secureOptions))
            } else {
                substeps[ndx] = new MutableWorkflowStepStateImpl(stepId)
            }
            substeps[ndx].nodeStep = !!step.nodeStep
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
        IFramework framework,
        UserAndRolesAuthContext authContext,
        Map jobcontext,
        Map secureOpts
    ) {
        final long id = execution.id

        MutableWorkflowState state = createStateForWorkflow(execution, execution.workflow, execution.project, framework,
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
        chain << new WorkflowStateListenerAction(onWorkflowExecutionStateChanged: {
            ExecutionState executionState, Date timestamp, List<String> nodeSet ->
                if (executionState.completedState) {
                    persistExecutionStateFinal(execution.id, state, outfile)
                }
        })
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

    def persistExecutionStateCheckpoint(Long id, WorkflowState state, File file) {
        Map data = serializeStateJson(id, state, file)
    }

    def persistExecutionStateFinal(Long id, WorkflowState state, File file) {
        Map data = serializeStateJson(id, state, file)
        stateCache.put(id, data)
        activeStates.remove(id)
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
        def loader = logFileStorageService.requestLogFileLoad(e, STATE_FILE_FILETYPE, performLoad)

        if (loader.file) {
            //cache local data
            statemap = deserializeState(loader.file)
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
        if (statemap) {
            statemap=stateMapping.summarize(new HashMap(statemap),nodes,selectedOnly,stepStates)
            return new WorkflowStateFileLoader(workflowState: statemap, state: ExecutionFileState.AVAILABLE)
        }

        //request file via file storage
        def loader = logFileStorageService.requestLogFileLoad(e, STATE_FILE_FILETYPE, performLoad)

        if (loader.file) {
            //cache local data
            statemap = deserializeState(loader.file)
            if (loader.state == ExecutionFileState.AVAILABLE) {
                //final state, so cache result.  If AVAILABLE_PARTIAL, do not cache it
                stateCache.put(e.id, statemap)
            }
            statemap=stateMapping.summarize(new HashMap(statemap),nodes,selectedOnly,stepStates)
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
}

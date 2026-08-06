/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.app.support.BuilderUtil
import com.dtolabs.rundeck.core.NodesetEmptyException
import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.config.Features
import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import org.rundeck.app.data.workflow.ConditionalStep
import org.rundeck.core.execution.ExecCommand
import org.rundeck.core.execution.ScriptCommand
import org.rundeck.core.execution.ScriptFileCommand
import com.dtolabs.rundeck.core.execution.ServiceThreadBase
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.PluginStepExecutionItemImpl
import com.dtolabs.rundeck.core.jobs.JobRefCommandBase
import com.dtolabs.rundeck.core.execution.workflow.ControlBehavior
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItemImpl
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.WorkflowImpl
import com.dtolabs.rundeck.core.plugins.PluginConfiguration
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import com.dtolabs.rundeck.execution.ExecutionItemFactory
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.fasterxml.jackson.databind.ObjectMapper
import grails.converters.JSON
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.app.components.RundeckJobDefinitionManager
import rundeck.CommandExec
import rundeck.Execution
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.codecs.JobsXMLCodec
import rundeck.services.logging.ExecutionLogWriter

import java.text.SimpleDateFormat

import static org.apache.tools.ant.util.StringUtils.getStackTrace

/**
 * Non-transactional service for execution utility methods
 */
class ExecutionUtilService {
    static transactional = false
    MetricService metricService
    FeatureService featureService
    ConfigurationService configurationService
    LogFileStorageService logFileStorageService
    def ThreadBoundOutputStream sysThreadBoundOut
    def ThreadBoundOutputStream sysThreadBoundErr
    RundeckJobDefinitionManager rundeckJobDefinitionManager
    ScheduledExecutionService scheduledExecutionService

    @CompileStatic
    def finishExecution(ExecutionService.AsyncStarted execMap) {
        finishExecutionMetrics(execMap)
        finishExecutionLogging(execMap)
    }
    @CompileStatic
    def  finishExecutionMetrics(ExecutionService.AsyncStarted execMap) {
        def ServiceThreadBase thread = execMap.thread
        if (!thread.isSuccessful()) {
            metricService.markMeter(ExecutionService.name, 'executionFailureMeter')
        } else {
            metricService.markMeter(ExecutionService.name, 'executionSuccessMeter')
        }
    }
    @CompileStatic
    def finishExecutionLogging(ExecutionService.AsyncStarted execMap) {
        ServiceThreadBase<WorkflowExecutionResult> thread = execMap.thread
        ExecutionLogWriter loghandler = execMap.loghandler
        def exportJobDef = configurationService.getBoolean('execution.logs.fileStorage.generateExecutionXml',true)
        if(exportJobDef){
            //creating xml file
            File xmlFile = logFileStorageService.
                    getFileForExecutionFiletype(execMap.execution, ProjectService.EXECUTION_XML_LOG_FILETYPE, false)

            getExecutionXmlFileForExecution(execMap.execution, xmlFile)
        }
        try {
            WorkflowExecutionResult object = thread.resultObject
            if (!thread.isSuccessful()) {
                Throwable exc = thread.getThrowable()
                def errmsgs = []

                if (exc && exc instanceof NodesetEmptyException) {
                    errmsgs << exc.getMessage()
                } else if (exc) {
                    errmsgs << exc.getMessage()
                    if (exc.getCause()) {
                        errmsgs << "Caused by: " + exc.getCause().getMessage()
                    }
                } else if (object) {
                    loghandler.logVerbose(object.toString())
                }
                if (errmsgs) {
                    log.error(
                            "Execution failed: " + execMap.execution.id +
                                    " in project ${execMap.execution.project}: " +
                                    errmsgs.join(",")
                    )

                    loghandler.logError(errmsgs.join(','))
                    if (exc) {
                        loghandler.logVerbose(getStackTrace(exc))
                    }
                } else {
                    if (object?.controlBehavior == ControlBehavior.Halt && object?.statusString) {
                        def msg = "Execution halted (\"${object.statusString}\"):${execMap.execution.id}" +
                                " in project ${execMap.execution.project}: " +
                                object?.toString()

                        log.error(msg)
                        loghandler.logWarn(msg)
                    } else {
                        def msg = "Execution failed: ${execMap.execution.id}" +
                                " in project ${execMap.execution.project}: " +
                                object?.toString()
                        log.error(msg)
                        loghandler.logError(msg)
                    }
                }
            } else {
                if (object?.controlBehavior == ControlBehavior.Halt) {
                    def msg = "Execution halted (succeeded):${execMap.execution.id}" +
                            " in project ${execMap.execution.project}: " +
                            object?.toString()

                    log.info(msg)
                    loghandler.log(msg)
                } else {
                    log.info(
                            "Execution successful: " + execMap.execution.id + " in project ${execMap.execution.project}"
                    )
                }
            }
        } finally {
            sysThreadBoundOut.removeThreadStream()?.close()
            sysThreadBoundErr.removeThreadStream()?.close()
            loghandler.close()
        }
    }


    /**
     * Create an WorkflowExecutionItem instance for the given Workflow,
     * suitable for the ExecutionService layer
     */
    public WorkflowExecutionItem createExecutionItemForWorkflow(WorkflowData workflow, String parentProject=null, ConditionalSet conditionalSetParentJob = null) {
        if (!workflow.commands || workflow.commands.size() < 1) {
            throw new Exception("Workflow is empty")
        }

        List<StepExecutionItem> stepExecutionItems = consolidateWorkflowSteps(workflow, parentProject, conditionalSetParentJob)
        def impl = new WorkflowImpl(
                stepExecutionItems,
                workflow.threadcount,
                workflow.keepgoing,
                workflow.strategy ? workflow.strategy : "node-first"
        )
        impl.setPluginConfig(workflow.pluginConfigMap)
        final WorkflowExecutionItemImpl item = new WorkflowExecutionItemImpl(impl)
        return item
    }

    private List<StepExecutionItem> consolidateWorkflowSteps(WorkflowData workflow, String parentProject, ConditionalSet conditionalSetParentJob) {
        return consolidateWorkflowStepsRecursive(workflow.commands, parentProject, conditionalSetParentJob)
    }

    /**
     * Recursively flatten conditional workflow steps, combining parent and child conditions
     * and tracking the full parent step path for nested conditionals.
     * @param steps list of workflow steps to process
     * @param parentProject parent project for job references
     * @param parentConditionSet combined conditions from all parent conditional steps
     * @param parentStepPath full path of parent step numbers for nested conditionals (e.g., [2, 2] for "2/2/*")
     * @param subStepCounter counter for substeps at the current nesting level
     * @return flattened list of execution items with combined conditions and proper step context
     */
    private List<StepExecutionItem> consolidateWorkflowStepsRecursive(
            List<WorkflowStepData> steps,
            String parentProject,
            ConditionalSet parentConditionSet,
            List<Integer> parentStepPath = null,
            int[] subStepCounter = null
    ) {
        boolean conditionalFeatureEnabled = featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL)

        List<StepExecutionItem> stepExecutionItems = []

        // Iterate through commands in order to preserve the original sequence.
        // The "logical" step number tracks the index in the original (un-flattened) job
        // definition; conditional sub-steps inherit it as their parent step number so the
        // workflow listeners can emit a hierarchical stepctx (e.g. "2/1" or "2/2/1") that
        // aligns with the job definition's step layout.
        int logicalStepNumber = 0
        steps.each { command ->
            logicalStepNumber++
            if (command instanceof ConditionalStep && conditionalFeatureEnabled && command.conditionSet) {
                ConditionalSet combinedConditionSet = combineConditionSets(parentConditionSet, command.conditionSet)
                if (command.subSteps) {
                    // Build the parent path for nested conditionals
                    List<Integer> newParentPath
                    int[] newCounter

                    if (parentStepPath != null) {
                        // We're already inside a conditional - this is a nested conditional
                        // Enforce maximum nesting depth of 1
                        // parentStepPath.size()=1: one level nested (allowed), size()=2+: too deep (reject)
                        if (parentStepPath.size() >= 2) {
                            throw new IllegalArgumentException(
                                "Conditional steps cannot be nested more than one level deep. " +
                                "Found conditional step at depth ${parentStepPath.size()}."
                            )
                        }
                        // Increment the counter at this level and append to the path
                        int[] counter = subStepCounter ?: [0] as int[]
                        counter[0] = counter[0] + 1
                        newParentPath = new ArrayList<>(parentStepPath)
                        newParentPath.add(counter[0])
                        newCounter = [0] as int[]  // Start fresh counter for the nested level
                    } else {
                        // This is a top-level conditional
                        newParentPath = [logicalStepNumber]
                        newCounter = [0] as int[]
                    }

                    List<StepExecutionItem> nestedItems = consolidateWorkflowStepsRecursive(
                            command.subSteps,
                            parentProject,
                            combinedConditionSet,
                            newParentPath,
                            newCounter
                    )
                    stepExecutionItems.addAll(nestedItems)
                }
            } else {
                StepExecutionItem item = itemForWFCmdItem(
                        command,
                        command.errorHandler ? itemForWFCmdItem(command.errorHandler, null, parentProject) : null,
                        parentProject
                )
                if (parentConditionSet) {
                    item.conditions = parentConditionSet
                }
                if (item != null) {
                    if (parentStepPath != null) {
                        // Mark with full parent path for proper nested context
                        int[] counter = subStepCounter ?: [0] as int[]
                        counter[0] = counter[0] + 1
                        markAsConditionalSubStep(item, parentStepPath, counter[0])
                    } else {
                        markWithLogicalStepNumber(item, logicalStepNumber)
                    }
                    stepExecutionItems.add(item)
                }
            }
        }

        stepExecutionItems
    }

    /**
     * Combine two ConditionalSets using Cartesian product of OR groups to implement AND logic.
     * Example: Parent [A, B] + Child [C, D] → Combined [A+C, A+D, B+C, B+D]
     * All conditions in a group must be true (AND), at least one group must match (OR)
     * @param parent parent conditional set (may be null)
     * @param child child conditional set (may be null)
     * @return combined conditional set, or the non-null input if one is null
     */
    private ConditionalSet combineConditionSets(ConditionalSet parent, ConditionalSet child) {
        if (parent == null) return child
        if (child == null) return parent

        // Guard against null or empty condition groups
        def parentGroups = parent.conditionGroups
        def childGroups = child.conditionGroups

        if (parentGroups == null || parentGroups.isEmpty()) return child
        if (childGroups == null || childGroups.isEmpty()) return parent

        // Create a new combined ConditionalSet
        def combined = new org.rundeck.app.data.workflow.ConditionalSetImpl()
        combined.nodeStep = parent.nodeStep || child.nodeStep

        // Cartesian product of condition groups (implements AND logic between parent and child)
        List combinedGroups = []
        parentGroups.each { parentGroup ->
            childGroups.each { childGroup ->
                // Merge AND groups: all conditions must be true
                List mergedGroup = []
                mergedGroup.addAll(parentGroup)
                mergedGroup.addAll(childGroup)
                combinedGroups.add(mergedGroup)
            }
        }
        combined.conditionGroups = combinedGroups
        return combined
    }

    /**
     * Promote a flattened conditional sub-step item so it implements
     * {@link com.dtolabs.rundeck.core.execution.workflow.HasParentStepContext} with the
     * given parent step path (for nested conditionals) or parent step number (for single-level)
     * and sub-step number (1-based index within the parent's sub-step list).
     * Also stamps {@code logicalStepNumber} so the listener can map the flat engine index
     * back to the correct logical step slot in the state tree.
     *
     * For nested conditionals, the parentStepPath contains the full path (e.g., [2, 2] for "2/2/*").
     * For backward compatibility, parentStepNumber is set to the first element of the path.
     *
     * If the item type does not support promotion the call is a no-op; the item will be
     * treated as a flat top-level step and produce non-hierarchical stepctx in logs.
     */
    private static void markAsConditionalSubStep(StepExecutionItem item, List<Integer> parentStepPath, int subStep) {
        if (parentStepPath == null || parentStepPath.isEmpty()) {
            return
        }

        // For backward compatibility: parentStepNumber is the first element of the path
        int parentStep = parentStepPath[0]

        if (item instanceof PluginStepExecutionItemImpl) {
            PluginStepExecutionItemImpl pluginItem = (PluginStepExecutionItemImpl) item
            pluginItem.setParentStepNumber(parentStep)
            pluginItem.setSubStepNumber(subStep)
            pluginItem.setLogicalStepNumber(parentStep)
            // Set the full parent path for nested conditionals
            if (parentStepPath.size() > 1) {
                pluginItem.setParentStepPath(new ArrayList<>(parentStepPath))
            }
        } else if (item instanceof JobRefCommandBase) {
            JobRefCommandBase jobRefItem = (JobRefCommandBase) item
            jobRefItem.setParentStepNumber(parentStep)
            jobRefItem.setSubStepNumber(subStep)
            jobRefItem.setLogicalStepNumber(parentStep)
            // Set the full parent path for nested conditionals
            if (parentStepPath.size() > 1) {
                jobRefItem.setParentStepPath(new ArrayList<>(parentStepPath))
            }
        }
    }

    /**
     * Stamp a regular (non-conditional) step with its 1-based logical step number in the
     * original job definition so that workflow listeners can emit the correct step context
     * even when the flat engine step number differs (e.g., after conditional sub-steps
     * were expanded into the engine list).
     */
    private static void markWithLogicalStepNumber(StepExecutionItem item, int logicalStep) {
        if (item instanceof PluginStepExecutionItemImpl) {
            ((PluginStepExecutionItemImpl) item).setLogicalStepNumber(logicalStep)
        } else if (item instanceof JobRefCommandBase) {
            ((JobRefCommandBase) item).setLogicalStepNumber(logicalStep)
        }
    }

    public StepExecutionItem itemForWFCmdItem(final WorkflowStepData step, final StepExecutionItem handler=null,final parentProject=null) throws FileNotFoundException {
        if(step.type == "conditional"){
            //log.warn("Workflow step ${step} has a condition set, but conditions are not supported in this context, ignoring")
        } else if(step instanceof CommandExec || step.instanceOf(CommandExec)){
            CommandExec cmd= step as CommandExec
            String type
            if (null != cmd.getAdhocRemoteString()) {
                type = ExecCommand.EXEC_COMMAND_TYPE
            } else if (null != cmd.getAdhocLocalString()) {
                type = ScriptCommand.SCRIPT_COMMAND_TYPE;
            } else if (null != cmd.getAdhocFilepath()) {
                type = ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE;
            }else {
                throw new IllegalArgumentException("Workflow step type was not expected: "+step);
            }

            return ExecutionItemFactory.createScriptFileItem(
                    type,
                    cmd.convertToPluginConfig(),
                    handler,
                    !!cmd.keepgoingOnSuccess,
                    cmd.description,
                    createLogFilterConfigs(step.getPluginConfigListForType(ServiceNameConstants.LogFilter)),
                    step.conditionSet
            )
        }else if (step instanceof JobExec || step.instanceOf(JobExec)) {
            final JobExec jobcmditem = step as JobExec

            WorkflowExecutionItem jobReferenceWorkflow = null
            ScheduledExecution se = scheduledExecutionService.findJobFromJobExec(jobcmditem, jobcmditem.jobProject ?: parentProject)
            if(se){
                jobReferenceWorkflow = createExecutionItemForWorkflow(se?.getWorkflowData(), parentProject)
            }

            final String[] args
            if (null != jobcmditem.getArgString()) {
                final List<String> strings = OptsUtil.burst(jobcmditem.getArgString());
                args = strings.toArray(new String[strings.size()]);
            } else {
                args = new String[0];
            }
            String tmpProj = jobcmditem.jobProject
            if(!jobcmditem.jobProject && parentProject){
                tmpProj = parentProject
            }
            return ExecutionItemFactory.createJobRef(
                    jobcmditem.getJobIdentifier(),
                    args,
                    !!jobcmditem.nodeStep,
                    handler,
                    !!jobcmditem.keepgoingOnSuccess,
                    jobcmditem.nodeFilter?:null,
                    jobcmditem.nodeThreadcount!=null && jobcmditem.nodeThreadcount>=1?jobcmditem.nodeThreadcount:null,
                    jobcmditem.nodeKeepgoing,
                    jobcmditem.nodeRankAttribute,
                    jobcmditem.nodeRankOrderAscending,
                    step.description,
                    jobcmditem.nodeIntersect,
                    tmpProj,
                    jobcmditem.failOnDisable,
                    jobcmditem.importOptions,
                    jobcmditem.uuid,
                    jobcmditem.useName,
                    jobcmditem.ignoreNotifications,
                    jobcmditem.childNodes,
                    jobReferenceWorkflow,
                    step.conditionSet
            )
        }else if(step instanceof PluginStep || step.instanceOf(PluginStep)  || step.instanceOf(ConditionalStep)){
            final WorkflowStepData stepitem = step
            if(stepitem.nodeStep) {
                return ExecutionItemFactory.createPluginNodeStepItem(
                        stepitem.type,
                        stepitem.configuration,
                        !!stepitem.keepgoingOnSuccess,
                        handler,
                        step.description,
                        createLogFilterConfigs(step.getPluginConfigListForType(ServiceNameConstants.LogFilter)),
                        step.conditionSet
                )
            }else {
                return ExecutionItemFactory.createPluginStepItem(
                        stepitem.type,
                        stepitem.configuration,
                        !!stepitem.keepgoingOnSuccess,
                        handler,
                        step.description,
                        createLogFilterConfigs(step.getPluginConfigListForType(ServiceNameConstants.LogFilter)),
                        step.conditionSet
                )
            }
        } else {
            throw new IllegalArgumentException("Workflow step type was not expected: "+step);
        }
    }

    /**
     * Create the list of plugin configuration, given the configuration for LogFilter type
     * @param config configuration list for LogFilter
     * @return list of configurations, or empty list
     */
    public static List<PluginConfiguration> createLogFilterConfigs(Object configurations) {
        List<PluginConfiguration> configs = []
        if (configurations && configurations instanceof Collection) {
            configurations.each { conf ->
                if (conf && conf instanceof Map) {
                    String name = conf['type']
                    if (conf['config'] instanceof Map) {
                        Map pluginconfig = conf['config']
                        configs << createLogFilterConfig(name, pluginconfig)
                    }
                }
            }
        }
        return configs;
    }

    public static PluginConfiguration createLogFilterConfig(String name, Map pluginconfig) {
        new SimplePluginConfiguration(ServiceNameConstants.LogFilter, name, pluginconfig)
    }

    /**
     * Write execution.xml file and return
     * @param exec execution
     * @param file path to store the file on filesystem.
     * @return file containing execution.xml
     */
    File getExecutionXmlFileForExecution(Execution execution, File executionXmlfile) {
        executionXmlfile?.withWriter("UTF-8") { Writer writer ->
            exportExecutionXml(
                    execution,
                    writer,
                    "output-${execution.id}.rdlog"
            )
        }

        executionXmlfile
    }


    /**
     * Write execution.xml file to the writer
     * @param exec execution
     * @param writer writer
     * @param logfilepath optional new outputfilepath to set for the xml
     * @return
     */
    def exportExecutionXml(Execution exec, Writer writer, String logfilepath =null){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        def dateConvert = {
            sdf.format(it)
        }
        BuilderUtil builder = new BuilderUtil()
        builder.converters = [(Date): dateConvert, (java.sql.Timestamp): dateConvert]
        def map = exec.toMap()
        BuilderUtil.makeAttribute(map, 'id')
        if (logfilepath) {
            //change entry to point to local file
            map.outputfilepath = logfilepath
        }
        JobsXMLCodec.convertWorkflowMapForBuilder(map.workflow)
        def exportJobDef = configurationService.getBoolean('execution.logs.fileStorage.generateExecutionXml',true)
        if(exportJobDef && exec.scheduledExecution){
            map.fullJob = rundeckJobDefinitionManager.jobMapToXMap(rundeckJobDefinitionManager.jobToMap(exec.scheduledExecution))
        }
        def xml = new MarkupBuilder(writer)
        builder.objToDom("executions", [execution: map], xml)
    }

    Map runRefJobWithTimer(Thread thread, long startTime, boolean shouldCheckTimeout, long timeoutms){

        boolean never = true
        def interrupt = false
        int killcount = 0
        def killLimit = 100
        while (thread.isAlive() || never) {
            never = false
            try {
                thread.join(1000)
            } catch (InterruptedException e) {
                //interrupt
                interrupt = true
            }
            if (thread.interrupted) {
                interrupt = true
            }
            def duration = System.currentTimeMillis() - startTime
            if (shouldCheckTimeout
                    && duration > timeoutms
            ) {
                interrupt = true
            }
            if (interrupt) {
                if (killcount < killLimit) {
                    //send wave after wave
                    thread.abort()
                    Thread.yield();
                    killcount++;
                } else {
                    //reached pre-set kill limit, so shut down
                    thread.stop()
                }
            }
        }

        return [result: thread.result, interrupt: interrupt]
    }
}

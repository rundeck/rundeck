package org.rundeck.app.data.execution

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.utils.NodeSet
import groovy.util.logging.Log4j2
import org.rundeck.app.data.job.converters.ScheduledExecutionFromRdJobUpdater
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.execution.ExecutionCreationSource
import org.rundeck.app.execution.ExecutionCreator
import org.springframework.beans.factory.annotation.Autowired
import rundeck.Execution
import rundeck.Orchestrator
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.data.execution.ExecutionOptionProcessor
import rundeck.services.ExecutionService
import rundeck.services.ExecutionServiceException

@Log4j2
class RdJobExecutionCreator implements ExecutionCreator<Execution> {
    @Autowired
    ExecutionService executionService
    @Autowired
    ExecutionOptionProcessor executionOptionProcessor

    @Override
    Execution createExecution(ExecutionCreationSource sourceData) {
        JobData job = sourceData.get("job", JobData)
        UserAndRolesAuthContext authContext = sourceData.get("authContext", UserAndRolesAuthContext)
        String runAsUser = sourceData.get("runAsUser", String)
        Map input = sourceData.get("input", Map)
        Map securedOpts = sourceData.get("secureOpts", Map)
        Map secureExposedOpts = sourceData.get("secureExposedOpts", Map)
        Map props = [:]

        Execution execution = new Execution()
        execution.jobUuid = job.uuid
        execution.project = job.project
        execution.user = runAsUser ? runAsUser : authContext.username
        execution.loglevel = job.logConfig?.loglevel
        execution.doNodedispatch = job.nodeConfig?.doNodedispatch
        execution.filter = job.nodeConfig?.filter
        execution.filterExclude = job.nodeConfig?.filterExclude
        execution.nodeExcludePrecedence = job.nodeConfig?.nodeExcludePrecedence
        execution.nodeThreadcount = job.nodeConfig?.nodeThreadcount
        execution.nodeKeepgoing = job.nodeConfig?.nodeKeepgoing
        execution.nodeRankOrderAscending = job.nodeConfig?.nodeRankOrderAscending
        execution.nodeRankAttribute = job.nodeConfig?.nodeRankAttribute
        execution.excludeFilterUncheck = job.nodeConfig?.excludeFilterUncheck
        execution.argString = job.argString
        execution.timeout = job.timeout
        execution.retry = job.retry
        execution.retryDelay = job.retryDelay
        Workflow workflow = new Workflow()
        execution.workflow = ScheduledExecutionFromRdJobUpdater.updateWorkflow(workflow, job.workflow)
        if(job.orchestrator) execution.orchestrator = ScheduledExecutionFromRdJobUpdater.updateOrchestrator(new Orchestrator(), job.orchestrator)
        execution.userRoles =  authContext.roles ? ( authContext.roles as List) : null

        if (input && 'true' == input['_replaceNodeFilters']) {
            if('filter' == input.nodeoverride){
                input.filter = input.nodefilter
                input.doNodedispatch = true
            }else{
                //remove all existing node filters to replace with input filters
                props = props.findAll {!(it.key =~ /^(filter|node(Include|Exclude).*)$/)}

                def filterprops = input.findAll { it.key =~ /^(filter|node(Include|Exclude).*)$/ }
                def nset = executionService.filtersAsNodeSet(filterprops)
                input.filter = NodeSet.generateFilter(nset)
                input.filterExclude=""
                input.doNodedispatch=true
            }
        }
        if (input) {
            props.putAll(input.subMap(['argString','filter','filterExclude','loglevel','retryAttempt','doNodedispatch','retryPrevId','retryOriginalId']).findAll{it.value!=null})
            props.putAll(input.findAll{it.key.startsWith('option.') && it.value!=null})
        }

        if (input && input['executionType']) {
            props.executionType = input['executionType']
        } else {
            throw new ExecutionServiceException("executionType is required")
        }
        if(input['meta'] instanceof Map){
            props.extraMetadataMap = input['meta']
        }

        // Parse base received options for processing and validation.
        HashMap baseOptParams = executionOptionProcessor.parseJobOptionInput(props, job, authContext)

        /*
        Process job lifecycle before execution event
        */

        def beforeExecutionResult = executionService.checkBeforeJobExecution(job, baseOptParams, props, authContext)

        // Process metadata updates
        if (beforeExecutionResult?.isUseNewMetadata()) {
            props.extraMetadataMap = beforeExecutionResult.newExecutionMetadata
        }

        // Process option values updates
        Map optparams = baseOptParams
        if (beforeExecutionResult?.isUseNewValues()) {
            optparams = beforeExecutionResult.optionsValues
            executionOptionProcessor.checkSecuredOptions(beforeExecutionResult.optionsValues, securedOpts, secureExposedOpts)
        }

        /* End job lifecycle processing */

        // Final option values validation
        executionOptionProcessor.validateOptionValues(job, optparams, authContext)

        optparams = executionOptionProcessor.removeSecureOptionEntries(job, optparams)

        props.argString = ExecutionOptionProcessor.generateJobArgline(job, optparams)
        if (props.retry?.contains('${')) {
            //replace data references
            if (optparams) {
                props.retry = DataContextUtils.replaceDataReferencesInString(props.retry, DataContextUtils.addContext("option", optparams, null)).trim()
            }
        }
        if(props.retry){
            //validate retry is a valid integer
            try{
                Integer.parseInt(props.retry)
            }catch(NumberFormatException e){
                throw new ExecutionServiceException("Unable to create execution: the value for 'retry' was not a valid integer: "+e.message,e)
            }
        }
        if (props.timeout?.contains('${')) {
            //replace data references
            if (optparams) {
                props.timeout = DataContextUtils.replaceDataReferencesInString(props.timeout, DataContextUtils.addContext("option", optparams, null))
            }
        }
        if (props.retryDelay?.contains('${')) {
            //replace data references
            if (optparams) {
                props.retryDelay = DataContextUtils.replaceDataReferences(props.retryDelay, DataContextUtils.addContext("option", optparams, null))
            }
        }
        if (props.nodeThreadcountDynamic?.contains('${')) {
            //replace data references
            if (optparams) {
                props.nodeThreadcount = DataContextUtils.replaceDataReferencesInString(props.nodeThreadcountDynamic, DataContextUtils.addContext("option", optparams, null))

                if(!props.nodeThreadcount.isInteger()){
                    props.nodeThreadcount = 1
                }
            }
        }

        execution.doNodedispatch=props.doNodedispatch?"true" == props.doNodedispatch.toString():false
        execution.filter= props.filter
        execution.filterExclude= props.filterExclude
        execution.nodeExcludePrecedence=props.nodeExcludePrecedence
        execution.nodeThreadcount=props.nodeThreadcount
        execution.timeout=props.timeout?:null
        execution.executionType= props.executionType ?: 'scheduled'
        execution.retryDelay= props.retryDelay?:null
        execution.argString=props.argString
        execution.retryAttempt=props.retryAttempt?:0
        execution.retryOriginalId=props.retryOriginalId?:null
        execution.retryPrevId=props.retryPrevId?:null
        execution.retry=props.retry?:null
        execution.serverNodeUUID= executionService.frameworkService.getServerUUID()
        execution.excludeFilterUncheck= props.excludeFilterUncheck?"true" == props.excludeFilterUncheck.toString():false
        execution.extraMetadataMap= props.extraMetadataMap?:null

        if(!execution.loglevel){
            execution.loglevel=executionService.defaultLogLevel
        }

        if (workflow && !workflow.save(flush:true)) {
            execution.workflow.errors.allErrors.each { log.error(it.toString()) }
            log.error("unable to save execution workflow")
            throw new ExecutionServiceException("unable to create execution workflow")
        }
        return execution

    }
}

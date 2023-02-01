package org.rundeck.app.data.execution

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.utils.NodeSet
import grails.gorm.transactions.Transactional
import groovy.util.logging.Log4j2
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.execution.ExecutionCreationSource
import org.rundeck.app.execution.ExecutionCreator
import org.springframework.beans.factory.annotation.Autowired
import rundeck.Execution
import rundeck.Orchestrator
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ExecutionService
import rundeck.services.ExecutionServiceException
import rundeck.services.FrameworkService

@Log4j2
@Transactional
class GormExecutionCreator implements ExecutionCreator<Execution> {

    @Autowired
    FrameworkService frameworkService
    @Autowired
    ExecutionService executionService

    @Override
    Execution createExecution(ExecutionCreationSource sourceData) {
        JobData job = sourceData.get("job", JobData)
        UserAndRolesAuthContext authContext = sourceData.get("authContext", UserAndRolesAuthContext)
        String runAsUser = sourceData.get("runAsUser", String)
        Map input = sourceData.get("input", Map)
        Map securedOpts = sourceData.get("secureOpts", Map)
        Map secureExposedOpts = sourceData.get("secureExposedOpts", Map)
        Map props = [:]

        def se = ScheduledExecution.findByUuid(job.uuid)
        def propset=[
                'project',
                'user',
                'loglevel',
                'doNodedispatch',
                'filter',
                'filterExclude',
                'nodeExcludePrecedence',
                'nodeThreadcount',
                'nodeThreadcountDynamic',
                'nodeKeepgoing',
                'nodeRankOrderAscending',
                'nodeRankAttribute',
                'workflow',
                'argString',
                'timeout',
                'retry',
                'retryDelay',
                'excludeFilterUncheck'
        ]
        propset.each{k->
            props.put(k,se[k])
        }
        if(se.orchestrator) {
            props["orchestrator"] = new Orchestrator(se.orchestrator.toMap())
        }
        props.user = authContext.username
        def roles = authContext.roles
        if (roles) {
            props.userRoles = (roles as List)
        }
        if (runAsUser) {
            props.user = runAsUser
        }
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
        HashMap baseOptParams = executionService.parseJobOptionInput(props, se, authContext)

        /*
        Process job lifecycle before execution event
        */

        def beforeExecutionResult = executionService.checkBeforeJobExecution(se, baseOptParams, props, authContext)

        // Process metadata updates
        if (beforeExecutionResult?.isUseNewMetadata()) {
            props.extraMetadataMap = beforeExecutionResult.newExecutionMetadata
        }

        // Process option values updates
        Map optparams = baseOptParams
        if (beforeExecutionResult?.isUseNewValues()) {
            optparams = beforeExecutionResult.optionsValues
            executionService.checkSecuredOptions(beforeExecutionResult.optionsValues, securedOpts, secureExposedOpts)
        }

        /* End job lifecycle processing */

        // Final option values validation
        executionService.validateOptionValues(se, optparams, authContext)

        optparams = executionService.removeSecureOptionEntries(se, optparams)

        props.argString = executionService.generateJobArgline(se, optparams)
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

        Workflow workflow = new Workflow(se.workflow)
        //create duplicate workflow
        props.workflow = workflow

        def Execution execution
        if (props.project && props.workflow) {
            execution = new Execution(project:props.project,
                    user:props.user, loglevel:props.loglevel,
                    doNodedispatch:props.doNodedispatch?"true" == props.doNodedispatch.toString():false,
                    filter: props.filter,
                    filterExclude: props.filterExclude,
                    nodeExcludePrecedence:props.nodeExcludePrecedence,
                    nodeThreadcount:props.nodeThreadcount,
                    nodeKeepgoing:props.nodeKeepgoing,
                    orchestrator:props.orchestrator,
                    nodeRankOrderAscending:props.nodeRankOrderAscending,
                    nodeRankAttribute:props.nodeRankAttribute,
                    workflow:props.workflow,
                    argString:props.argString,
                    executionType: props.executionType ?: 'scheduled',
                    timeout:props.timeout?:null,
                    retryAttempt:props.retryAttempt?:0,
                    retryOriginalId:props.retryOriginalId?:null,
                    retryPrevId:props.retryPrevId?:null,
                    retry:props.retry?:null,
                    retryDelay: props.retryDelay?:null,
                    serverNodeUUID: frameworkService.getServerUUID(),
                    excludeFilterUncheck: props.excludeFilterUncheck?"true" == props.excludeFilterUncheck.toString():false,
                    extraMetadataMap: props.extraMetadataMap?:null
            )

            execution.userRoles = props.userRoles


            //parse options
            if(!execution.loglevel){
                execution.loglevel=executionService.defaultLogLevel
            }

        } else {
            throw new IllegalArgumentException("insufficient props to create a new Execution instance: " + props)
        }
        execution.scheduledExecution=se
        execution.jobUuid = se?.uuid
        if (workflow && !workflow.save(flush:true)) {
            execution.workflow.errors.allErrors.each { log.error(it.toString()) }
            log.error("unable to save execution workflow")
            throw new ExecutionServiceException("unable to create execution workflow")
        }
        return execution

    }
}

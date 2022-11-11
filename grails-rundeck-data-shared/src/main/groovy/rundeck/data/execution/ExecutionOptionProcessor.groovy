package rundeck.data.execution

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.options.RemoteJsonOptionRetriever
import com.dtolabs.rundeck.core.utils.OptsUtil
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.rundeck.app.execution.StorageAccessChecks
import org.rundeck.app.job.option.RemoteOptionValueLoader
import org.rundeck.app.job.option.RemoteOptionValuesResponse
import org.rundeck.app.jobfilerecord.JobFileRecordValidator
import org.rundeck.storage.api.StorageException
import org.springframework.context.MessageSource
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.RequestContextUtils
import rundeck.data.exceptions.ExecutionServiceValidationException
import rundeck.data.storage.AuthorizedKeyStorageTreeProvider
import rundeck.data.util.JobDataUtil
import rundeck.data.util.JobOptionUtil

import javax.servlet.http.HttpSession
import java.text.MessageFormat
import java.util.regex.Pattern

class ExecutionOptionProcessor {

    MessageSource messageSource
    RemoteOptionValueLoader remoteOptionValueLoader
    JobFileRecordValidator jobFileRecordValidator
    StorageAccessChecks storageAccessChecks
    AuthorizedKeyStorageTreeProvider storageService


    /**
     * It replaces values coming from optionsValues into secured options and secured exposed options
     * it doesn't add values to secured/exposed options
     * @param optionsValues
     * @param securedOpts
     * @param securedExposedOpts
     */
    def checkSecuredOptions(optionsValues, Map securedOpts = [:], Map securedExposedOpts = [:]){

        optionsValues.each { k, v ->
            if(securedOpts[k]){
                securedOpts.put(k, v)
            }else if(securedExposedOpts[k]){
                securedExposedOpts.put(k, v)
            }
        }
    }

    /**
     * Load stored password default values for secure options with defaultStoragePath, and no value set.
     *
     * @param scheduledExecution job
     * @param secureOptsExposed exposed secure option values
     * @param secureOpts private secure option values
     * @param authContext auth context
     */
    void loadSecureOptionStorageDefaults(
            JobData scheduledExecution,
            Map secureOptsExposed,
            Map secureOpts,
            AuthContext authContext,
            boolean failIfMissingRequired=false,
            Map<String, String> args = null,
            Map<String, String> job = null,
            Map secureOptionNodeDeferred = null
    )
    {
        def found = scheduledExecution.optionSet?.findAll {
            it.secureInput && it.defaultStoragePath
        }?.findAll {
            it.secureExposed ?
                    !(secureOptsExposed?.containsKey(it.name)) :
                    !(secureOpts?.containsKey(it.name))
        }
        if (found) {
            //load secure option defaults from key storage
            def keystore = storageService.storageTreeWithContext(authContext)
            found?.each {
                def defStoragePath = it.defaultStoragePath
                def failMessage
                def exists=false
                try {
                    //search and replace ${option.
                    if (args && defStoragePath?.contains('${option.')) {
                        defStoragePath = DataContextUtils.replaceDataReferencesInString(defStoragePath, DataContextUtils.addContext("option", args, null)).trim()
                    }
                    if (job && defStoragePath?.contains('${job.')) {
                        defStoragePath = DataContextUtils.replaceDataReferencesInString(defStoragePath, DataContextUtils.addContext("job", job, null)).trim()
                    }
                    def password
                    def nodeDeferred = false
                    if (defStoragePath?.contains('${node.')) {
                        nodeDeferred = secureOptionNodeDeferred != null ? true : false
                        password = defStoragePath //to be resolved later
                        exists=true
                    }else {
                        if(keystore.hasPassword(defStoragePath)){
                            password = keystore.readPassword(defStoragePath)
                            exists=true
                        }else{
                            failMessage = "path not found"
                        }
                    }
                    if(exists){
                        if (it.secureExposed) {
                            secureOptsExposed[it.name] = new String(password)
                            if(nodeDeferred) {
                                secureOptionNodeDeferred[it.name] = password
                            }
                        } else {
                            secureOpts[it.name] = new String(password)
                            if(nodeDeferred) {
                                secureOptionNodeDeferred[it.name] = password
                            }
                        }
                    }
                } catch (StorageException e) {
                    failMessage = e.message
                }

                if(!exists){
                    if (it.required && failIfMissingRequired) {
                        throw new ExecutionServiceException(
                                "Required option '${it.name}' default value could not be loaded from key storage " +
                                        "path: ${defStoragePath}: ${failMessage}"
                        )
                    } else {
                        log.warn(
                                "Option '${it.name}' default value could not be loaded from key storage " +
                                        "path: ${defStoragePath}: ${failMessage}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Parse input "option.NAME" values, or a single "argString" value. Add default missing defaults for required
     * options. return a key value map for option name and value.
     * @param props
     * @param scheduledExec
     * @return a Map of String to String, does not produce multiple values for multivalued options
     */
    HashMap parseJobOptionInput(Map props, JobData job, UserAndRolesAuthContext authContext = null) {
        def optparams = JobOptionUtil.filterOptParams(props)
        if (!optparams && props.argString) {
            optparams = JobOptionUtil.parseOptsFromString(props.argString)
        }
        optparams = addOptionDefaults(job, optparams)
        optparams = addRemoteOptionSelected(job, optparams, authContext)
        optparams
    }

    /**
     * evaluate the options and return a map of the values of any secure options, using defaults for required options if
     * they are not present, and selecting between exposed/hidden secure values
     */
    Map selectSecureOptionInput(JobData jobData, Map params, Boolean exposed=false) throws ExecutionServiceException {
        def results=[:]
        def optparams
        if (params?.argString) {
            optparams = JobOptionUtil.parseOptsFromString(params.argString)
        }else if(params?.optparams){
            optparams=params.optparams
        }else{
            optparams = JobOptionUtil.filterOptParams(params)
        }
        final options = jobData.optionSet
        if (options) {
            options.each { opt ->
                if (opt.secureInput && optparams[opt.name] && (exposed && opt.secureExposed || !exposed && !opt.secureExposed)) {
                    results[opt.name]= optparams[opt.name]
                }else if (opt.secureInput && opt.defaultValue && opt.required && (exposed && opt.secureExposed || !exposed && !opt.secureExposed)) {
                    results[opt.name] = opt.defaultValue
                }
            }
        }
        return results
    }
    /**
     * Return a map containing all params that are not secure option parameters
     */
    Map removeSecureOptionEntries(JobData jobData, Map params) throws ExecutionServiceException {
        def results=new HashMap(params)
        final options = jobData.optionSet
        if (options) {
            options.each { OptionData opt ->
                if (opt.secureInput) {
                    results.remove(opt.name)
                }
            }
        }
        return results
    }

    /**
     * evaluate the options in the input argString, and if any Options defined for the Job have enforced=true, the values
     * is taken from remote URL, have a selected value as default, and have null value in the input properties,
     * then append the selected by default option value to the argString
     */
    Map addRemoteOptionSelected(JobData jobData, Map optparams, UserAndRolesAuthContext authContext = null) throws ExecutionServiceException {
        def newmap = new HashMap(optparams)

        final options = jobData.optionSet
        if (options) {
            def defaultoptions=[:]
            options.each {OptionData opt ->
                if(null==optparams[opt.name] && opt.enforced && !opt.optionValues){
                    Map remoteOptions = remoteOptionValueLoader.loadOptionsRemoteValues(jobData, [option: opt.name, extra: [option: optparams]], authContext?.username)
                    if(!remoteOptions.err && remoteOptions.values){
                        Map selectedOption = remoteOptions.values.find {it instanceof Map && [true, 'true'].contains(it.selected)}
                        if(selectedOption){
                            defaultoptions[opt.name]=selectedOption.value
                        }
                    }
                }
            }
            if(defaultoptions){
                newmap.putAll(defaultoptions)
            }
        }
        return newmap
    }

    /**
     * evaluate the options in the input argString, and if any Options defined for the Job have required=true, have a
     * defaultValue, and have null value in the input properties, then append the default option value to the argString
     */
    Map addOptionDefaults(JobData jobData, Map optparams) throws ExecutionServiceException {
        def newmap = new HashMap(optparams)

        final options = jobData.optionSet
        if (options) {
            def defaultoptions=[:]
            options.each {OptionData opt ->
                if (null==optparams[opt.name] && opt.defaultValue) {
                    defaultoptions[opt.name]=opt.defaultValue
                }
            }
            if(defaultoptions){
                newmap.putAll(defaultoptions)
            }
        }
        return newmap
    }

    /**
     * Add only the options that exists on the child job
     */
    Map<String,String> addImportedOptions(JobData jobData, Map optparams, StepExecutionContext executionContext) throws ExecutionServiceException {
        def newMap = new HashMap()
        executionContext.dataContext.option.each {dcopt ->
            if(jobData.optionSet.find { opt -> opt.name == dcopt.key}){
                newMap<<dcopt
            }
        }
        return newMap+optparams
    }


    /**
     * evaluate the options in the input properties, and if any Options defined for the Job have regex constraints,
     * require the values in the properties to match the regular expressions.  Throw ExecutionServiceException if
     * any options don't match.
     * @deprecated unused? cull
     */
    boolean validateInputOptionValues(JobData jobData, Map props) throws ExecutionServiceException{
        def optparams = JobOptionUtil.filterOptParams(props)
        if(!optparams && props.argString){
            optparams = parseJobOptsFromString(jobData,props.argString)
        }
        return validateOptionValues(jobData,optparams)
    }
    /**
     * evaluate the options value map, and if any Options defined for the Job have regex constraints,
     * require the values in the properties to match the regular expressions.  Throw ExecutionServiceException if
     * any options don't match.
     * @param scheduledExecution the job
     * @param optparams Map of String to String
     * @param authContext auth for reading storage defaults
     */
    boolean validateOptionValues(
            JobData jobData,
            Map optparams,
            AuthContext authContext = null,
            isJobRef = false
    ) throws ExecutionServiceValidationException
    {

        def fail = false
        def sb = []

        def failedkeys = [:]
        def invalidOpt={OptionData opt, String msg->
            fail = true
            if (!failedkeys[opt.name]) {
                failedkeys[opt.name] = ''
            }
            sb << msg
            failedkeys[opt.name] += msg
        }
        if (jobData.optionSet) {
            jobData.optionSet.each { OptionData opt ->
                if (!opt.multivalued && optparams[opt.name] && !(optparams[opt.name] instanceof String)) {
                    invalidOpt opt,lookupMessage("domain.Option.validation.multivalue.notallowed",[opt.name,opt.secureInput ? '***' : optparams[opt.name]])
                    return
                }

                if (JobOptionUtil.isFileType(opt) && optparams[opt.name]) {
                    def validate = jobFileRecordValidator.validateFileRefForJobOption(
                            optparams[opt.name],
                            JobDataUtil.getExtId(jobData),
                            opt.name,
                            isJobRef
                    )
                    if (!validate.valid) {
                        invalidOpt opt, lookupMessage('domain.Option.validation.file.' + validate.error, validate.args)
                    }
                }
                if (opt.required && !optparams[opt.name]) {

                    if (!opt.defaultStoragePath) {
                        invalidOpt opt,lookupMessage("domain.Option.validation.required",[opt.name])
                        return
                    }
                    try {
                        def canread = storageAccessChecks.canReadStoragePassword(
                                authContext,
                                opt.defaultStoragePath,
                                true
                        )

                        if (!canread) {
                            invalidOpt opt, lookupMessage(
                                    "domain.Option.validation.required.storageDefault",
                                    [opt.name, opt.defaultStoragePath]
                            )
                            return
                        }
                    } catch (ExecutionServiceException e1) {

                        invalidOpt opt, lookupMessage(
                                "domain.Option.validation.required.storageDefault.reason",
                                [opt.name, opt.defaultStoragePath, e1.message]

                        )
                        return
                    }
                }
                if(opt.enforced && !(opt.optionValues || opt.optionValuesPluginType)){
                    RemoteOptionValuesResponse remoteOptions = remoteOptionValueLoader.loadOptionsRemoteValues(jobData,
                            [option: opt.name, extra: [option: optparams]], authContext?.username)
                    if(!remoteOptions.err && remoteOptions.values){
                        opt.optionValues = remoteOptions.values.collect { optValue ->
                            if (optValue instanceof Map) {
                                return optValue.value
                            } else {
                                return optValue
                            }
                        }
                    }
                }
                if (opt.multivalued) {
                    boolean multivaluedOptionEvalFailed = false
                    if (opt.regex && !opt.enforced && optparams[opt.name]) {
                        def val
                        if (optparams[opt.name] instanceof Collection) {
                            val = [optparams[opt.name]].flatten();
                        } else {
                            val = optparams[opt.name].toString().split(Pattern.quote(opt.delimiter))
                        }
                        List failedValues = []
                        val.grep { it }.each { value ->
                            if (!(value ==~ opt.regex)) {
                                failedValues += value
                                multivaluedOptionEvalFailed = true
                            }
                        }
                        if (multivaluedOptionEvalFailed) {
                            invalidOpt opt,lookupMessage("domain.Option.validation.regex.values",[opt.name, failedValues,opt.regex])
                            return
                        }
                    }
                    if (opt.enforced && opt.optionValues && optparams[opt.name]) {
                        def val
                        if (optparams[opt.name] instanceof Collection) {
                            val = [optparams[opt.name]].flatten();
                        } else {
                            val = optparams[opt.name].toString().split(Pattern.quote(opt.delimiter))
                        }
                        if (!opt.optionValues.containsAll(val.grep { it })) {
                            invalidOpt opt,lookupMessage("domain.Option.validation.allowed.values",[opt.name,optparams[opt.name],opt.optionValues])
                            return
                        }
                    }
                } else {
                    if (opt.regex && !opt.enforced && optparams[opt.name]) {
                        if (!(optparams[opt.name] ==~ opt.regex)) {
                            invalidOpt opt, opt.secureInput ?
                                    lookupMessage("domain.Option.validation.secure.invalid",[opt.name])
                                    : lookupMessage("domain.Option.validation.regex.invalid",[opt.name,optparams[opt.name],opt.regex])

                            return
                        }
                    }
                    if (opt.enforced && opt.optionValues &&
                            optparams[opt.name] &&
                            optparams[opt.name] instanceof String &&
                            !opt.optionValues.contains(optparams[opt.name])) {
                        invalidOpt opt,  opt.secureInput ?
                                lookupMessage("domain.Option.validation.secure.invalid",[opt.name])
                                : lookupMessage("domain.Option.validation.allowed.invalid",[opt.name,optparams[opt.name],opt.optionValues])
                        return
                    }
                }
            }
        }
        if (fail) {
            def msg = sb.join('\n')
            throw new ExecutionServiceValidationException(msg, optparams, failedkeys)
        }
        return !fail
    }

    /**
     *  Parse an argString for a Job, treating multi-valued options as delimiter-separated and converting to a List of values
     * @param jobData
     * @param argString
     * @return map of option name to value, where value is a String or a List of Strings
     */
    Map parseJobOptsFromString(JobData jobData, String argString){
        def optparams = JobOptionUtil.parseOptsFromString(argString)
        if(optparams){
            //look for multi-valued options and try to split on delimiters
            jobData.optionSet.each{OptionData opt->
                if(opt.multivalued && optparams[opt.name]){
                    def arr = optparams[opt.name].split(Pattern.quote(opt.delimiter))
                    optparams[opt.name]=arr as List
                }
            }
        }
        return optparams
    }

    /**
     * Generate an argString from a map of options and values
     */
    static String generateArgline(Map<String,String> opts){
        def argsList = []
        for (Map.Entry<String, String> entry : opts.entrySet()) {
            String val = opts.get(entry.key)
            argsList<<'-'+entry.key
            argsList<<val
        }
        return OptsUtil.join(argsList)
    }

    /**
     * Generate an argString from a map of options and values
     */
    static String generateJobArgline(JobData jobData,Map<String,Object> opts){
        def newopts = [:]
        def addOptVal={key,obj,OptionData opt=null->
            String val
            if (obj instanceof String[] || obj instanceof Collection) {
                //join with delimiter
                if (opt && opt.delimiter) {
                    val = obj.grep { it }.join(opt.delimiter)
                } else {
                    val = obj.grep { it }.join(",")
                }
            } else {
                val = (String) obj
            }
            newopts[key] = val
        }
        for (OptionData opt : jobData.optionSet.findAll {opts.containsKey(it.name)}) {
            addOptVal(opt.name, opts.get(opt.name),opt)
        }
        //add any input options that don't match job options, to preserve information
        opts.keySet().findAll {!newopts[it]}.sort().each {
            addOptVal(it, opts[it])
        }
        return generateArgline(newopts)
    }

    def lookupMessage(String theKeys, List<Object> data, String defaultMessage=null) {
        lookupMessage([theKeys] as String[], data, defaultMessage)
    }
    /**
     * @parameter key
     * @returns corresponding value from messages.properties
     */
    def lookupMessage(String[] theKeys, List<Object> data, String defaultMessage=null) {
        def locale = getLocale()
        def theValue = null
        theKeys.any{key->
            try {
                theValue =  messageSource.getMessage(key,data as Object[],locale )
                return true
            } catch (org.springframework.context.NoSuchMessageException e){
            } catch (java.lang.NullPointerException e) {
            }
            return false
        }
        if(null==theValue && defaultMessage){
            MessageFormat format = new MessageFormat(defaultMessage);
            theValue=format.format(data as Object[])
        }
        return theValue
    }


    /**
     * Get the locale
     * @return locale
     * */
    def getLocale() {
        Locale locale = null
        try {
            locale = RequestContextUtils.getLocale(getSession().request)
        }
        catch(java.lang.IllegalStateException e){
            //log.debug "Running in console?"
        }
        //log.debug "locale: ${locale}"
        return locale
    }
    /**
     * Get the HTTP Session
     * @return session
     **/
    private HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }
}

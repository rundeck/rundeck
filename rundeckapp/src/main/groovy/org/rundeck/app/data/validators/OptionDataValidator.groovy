package org.rundeck.app.data.validators

import grails.validation.Validateable
import groovy.util.logging.Slf4j
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.rundeck.app.data.util.OptionDataUtil
import org.rundeck.app.data.util.RemoteJsonUtil
import rundeck.ScheduledExecution
import rundeck.controllers.ScheduledExecutionController
import rundeck.services.FrameworkService
import rundeck.utils.OptionsUtil

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Slf4j
class OptionDataValidator {

    static boolean validateOption(FrameworkService frameworkService, JobData jobData, OptionData opt, Map params = null, boolean jobWasScheduled=false, String username="anonymous",String userEmail="") {
        if(!(opt instanceof Validateable)) return
        opt.validate()
        def result = [:]
        if (jobWasScheduled && opt.required && opt.typeFile) {
            opt.errors.rejectValue('required', 'option.file.required.message')
            return result
        }

        if (opt.hidden && !opt.defaultValue && !opt.defaultStoragePath) {
            opt.errors.rejectValue('hidden', 'option.hidden.notallowed.message')
            return result
        }
        if (opt.enforced && (opt.optionValues || opt.valuesList) && opt.defaultValue) {
            opt.convertValuesList()
            if(!opt.multivalued && !opt.optionValues.contains(opt.defaultValue)) {
                opt.errors.rejectValue('defaultValue', 'option.defaultValue.notallowed.message')
            }else if(opt.multivalued && opt.delimiter){
                //validate each default value
                def found = opt.defaultValue.split(Pattern.quote(opt.delimiter)).find{ !opt.optionValues.contains(it) }
                if(found){
                    opt.errors.rejectValue('defaultValue', 'option.defaultValue.multivalued.notallowed.message',[found] as Object[],"{0} invalid value")
                }
            }
        }
        if (opt.enforced && (!opt.optionValues && !opt.valuesList && !opt.realValuesUrl && !opt.optionValuesPluginType)) {
            if (params && params.valuesType == 'url') {
                opt.errors.rejectValue('valuesUrl', 'option.enforced.emptyvalues.message')
            } else {
                opt.errors.rejectValue('valuesList', 'option.enforced.emptyvalues.message')
            }
        }
        if (opt.regex) {
            //try to parse regular expression syntax
            try {
                Pattern.compile(opt.regex)
            } catch (PatternSyntaxException e) {
                result.regexError = e.message
                opt.errors.rejectValue('regex', 'option.regex.invalid.message', [opt.regex] as Object[], "Invalid Regex: {0}")
            }
            if (opt.optionValues || opt.valuesList) {
                opt.convertValuesList()
                def inval = []
                opt.optionValues.each {val ->
                    if (!(val =~ /${opt.regex}/)) {
                        opt.errors.rejectValue('values', 'option.values.regexmismatch.message', [val.toString(), opt.regex] as Object[], "Value does not match regex: {0}")
                    }
                }
            }
            if (opt.defaultValue) {
                if (!(opt.defaultValue =~ /${opt.regex}/)) {
                    opt.errors.rejectValue('defaultValue', 'option.defaultValue.regexmismatch.message', [opt.defaultValue, opt.regex] as Object[], "Default value does not match regex: {0}")
                }
            }
        }
        if(opt.multivalued && !opt.delimiter){
            opt.errors.rejectValue('delimiter', 'option.delimiter.blank.message')
        }
        if(opt.multivalued && opt.secureInput){
            opt.errors.rejectValue('multivalued', 'option.multivalued.secure-conflict.message')
        }
        if(jobWasScheduled && opt.required && !(opt.defaultValue||opt.defaultStoragePath)){
            boolean hasSelectedOnRemoteValue = false
            if(opt.realValuesUrl){
                try{
                    def realUrl = opt.realValuesUrl.toExternalForm()
                    def urlExpanded = OptionDataUtil.expandUrl(frameworkService, opt, realUrl, jobData, [:], realUrl.matches(/(?i)^https?:.*$/),username, userEmail)
                    def remoteResult= RemoteJsonUtil.getRemoteJSON(urlExpanded, 10, 0, 5)
                    if(remoteResult){
                        def remoteJson = remoteResult.json
                        if(remoteJson && remoteJson instanceof List && ((List<Map>)remoteJson).any {Map item -> return item.selected}){
                            hasSelectedOnRemoteValue = true
                        }
                    }
                } catch (Exception e){
                    log.error("getRemoteJSON error: ${e.message}")
                    e.printStackTrace()
                }
            }
            if(!hasSelectedOnRemoteValue) opt.errors.rejectValue('defaultValue', 'option.defaultValue.required.message')
        }
        return result
    }
}

package rundeck.data.validation.validators.joboptions

import groovy.util.logging.Slf4j
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Slf4j
class JobOptionDataValidator implements Validator {

    FrameworkServiceCapabilities frameworkService
    JobData jobData

    JobOptionDataValidator(FrameworkServiceCapabilities frameworkService, JobData jobData) {
        this.frameworkService = frameworkService
        this.jobData = jobData
    }

    @Override
    boolean supports(Class<?> clazz) {
        return OptionData.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        OptionData opt = (OptionData)target
        boolean jobWasScheduled = jobData.scheduled
        if (jobWasScheduled && opt.required && opt.optionType == 'file') {
            errors.rejectValue('required', 'option.file.required.message')
            return
        }
        if(opt.optionType == 'file') {
            new FileUploadJobOptionValidator(frameworkService).validate(target, errors)
            return
        }

        if (opt.hidden && !opt.defaultValue && !opt.defaultStoragePath) {
            errors.rejectValue("hidden", 'option.hidden.notallowed.message')
            return
        }
        if(opt.multivalued && !opt.delimiter){
            errors.rejectValue('delimiter', 'option.delimiter.blank.message')
            return
        }
        if (opt.enforced && (opt.optionValues || opt.valuesList) && opt.defaultValue) {
            def valueList = opt.valuesList ? opt.valuesList.split(opt.delimiter) : opt.optionValues
            if(!opt.multivalued && !valueList.contains(opt.defaultValue)) {
                errors.rejectValue('defaultValue', 'option.defaultValue.notallowed.message')
            }else if(opt.multivalued && opt.delimiter){
                //validate each default value
                def found = opt.defaultValue.split(Pattern.quote(opt.delimiter)).find{ !valueList.contains(it) }
                if(found){
                    errors.rejectValue('defaultValue', 'option.defaultValue.multivalued.notallowed.message',[found] as Object[],"{0} invalid value")
                }
            }
        }

        if (opt.regex) {
            //try to parse regular expression syntax
            try {
                Pattern.compile(opt.regex)
            } catch (PatternSyntaxException e) {
                errors.rejectValue('regex', 'option.regex.invalid.message', [opt.regex] as Object[], "Invalid Regex: {0}")
            }
            if (opt.optionValues || opt.valuesList) {
                opt.optionValues.each {val ->
                    if (!(val =~ /${opt.regex}/)) {
                        opt.errors.rejectValue('values', 'option.values.regexmismatch.message', [val.toString(), opt.regex] as Object[], "Value does not match regex: {0}")
                    }
                }
            }
            if (opt.defaultValue) {
                if (!(opt.defaultValue =~ /${opt.regex}/)) {
                    errors.rejectValue('defaultValue', 'option.defaultValue.regexmismatch.message', [opt.defaultValue, opt.regex] as Object[], "Default value does not match regex: {0}")
                }
            }
        }

        if(opt.multivalued && opt.secureInput){
            errors.rejectValue('multivalued', 'option.multivalued.secure-conflict.message')
        }
        if(jobWasScheduled && opt.required && !(opt.defaultValue||opt.defaultStoragePath)){
            boolean hasSelectedOnRemoteValue = false
            if(opt.realValuesUrl){
                try{
                    def realUrl = opt.realValuesUrl.toExternalForm()
                    def urlExpanded = frameworkService.jobOptionUrlExpander.expandUrl(realUrl, jobData, opt, null)
                    def remoteResult= frameworkService.remoteJsonOptionRetriever.getRemoteJson(urlExpanded, 10, 0, 5, false)
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
            if(!hasSelectedOnRemoteValue) errors.rejectValue('defaultValue', 'option.defaultValue.required.message')
        }
    }

}

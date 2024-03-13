package com.dtolabs.rundeck.app.api.jobs.options

import grails.compiler.GrailsCompileStatic
import grails.validation.Validateable
import io.swagger.v3.oas.annotations.media.Schema
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.rundeck.app.jobs.options.JobOptionConfigRemoteUrl
import org.rundeck.app.jobs.options.RemoteUrlAuthenticationType
import org.springframework.validation.Errors
import rundeck.data.validation.shared.SharedJobOptionConstraints

/**
 * Defines the fields for a request to validate an option
 * Uses the canonical field names expected in the JSON and YAML formats, which
 * differ from the field names used in the Java interfaces
 */
@GrailsCompileStatic
@Schema
class OptionValidateRequest extends OptionInput implements OptionData, Validateable {
    /**
     * The canonical name for the "realValuesUrl" field
     */
    String storagePath

    @Override
    String getDefaultStoragePath() {
        return storagePath
    }

    @Override
    void setDefaultStoragePath(final String defaultStoragePath) {
        this.storagePath = defaultStoragePath
    }

    /**
     * The canonical name for the "valuesUrl" field
     */
    String valuesUrl

    @Override
    URL getRealValuesUrl() {
        if(valuesUrl){
            try{
                return new URL(this.valuesUrl)
            }catch (MalformedURLException e){
                return null
            }
        }
        return null
    }

    @Override
    void setRealValuesUrl(final URL realValuesUrl) {
        this.valuesUrl = realValuesUrl.toString()
    }
/**
     * the canonical name for "defaultValue" field
     */
    String value

    @Override
    String getDefaultValue() {
        this.value
    }

    @Override
    void setDefaultValue(final String defaultValue) {
        this.value=defaultValue
    }
    /**
     * The canonical name for the valuesList string
     */
    SortedSet<String> values

    @Override
    String getValuesList() {
        if(this.values && !this.values.isEmpty()){
            return this.values.join(this.delimiter?:',')
        }else{
            return null
        }
    }

    @Override
    void setValuesList(final String valuesList) {
        if(valuesList){
            this.values=valuesList.split(this.valuesListDelimiter?:',') as SortedSet<String>
        }else{
            this.values=null
        }
    }

    @Override
    List<String> getOptionValues() {
        if(values && !values.isEmpty()){
            return values as List<String>
        }else{
            return super.getOptionValues()
        }
    }
/**
     * The canonical name for the secureInput field
     */
    Boolean secure

    @Override
    Boolean getSecureInput() {
        return this.secure
    }

    @Override
    void setSecureInput(final Boolean secureInput) {
        this.secure=secureInput
    }
/**
     * The canonical name for the secureExposed field
     */
    Boolean valueExposed

    @Override
    Boolean getSecureExposed() {
        return valueExposed
    }

    @Override
    void setSecureExposed(final Boolean secureExposed) {
        this.valueExposed=secureExposed
    }

    String valuesType
    String remoteUrlAuthenticationType
    Map configRemoteUrl
    static constraints = {
        importFrom SharedJobOptionConstraints
        valuesType(nullable: true)
        configRemoteUrl(nullable: true)
        //nb: see RemoteUrlAuthenticationType.groovy
        remoteUrlAuthenticationType(nullable: true, inList: RemoteUrlAuthenticationType.values()*.name())
        realValuesUrl(nullable: true)
        valuesUrl(nullable: true, blank:true, validator: { String val, OptionValidateRequest obj, Errors errors ->
            if(val){
                try {
                    def url = new URL(val)
                } catch (MalformedURLException e) {
                    errors.rejectValue('valuesUrl', 'option.valuesUrl.invalid.message')
                    return false
                }
            }
            return true
        })
        value(nullable: true, blank:true)
        values(nullable: true)
        secure(nullable: true)
        valueExposed(nullable: true)
    }

    JobOptionConfigRemoteUrl convertConfigRemoteUrlData() {
        def entry = this.getConfigData()?.getJobOptionEntry(JobOptionConfigRemoteUrl.TYPE)
        if (entry) {
            return entry as JobOptionConfigRemoteUrl
        } else if(configRemoteUrl) {
            return JobOptionConfigRemoteUrl.fromMap(configRemoteUrl+[authenticationType:remoteUrlAuthenticationType])
        }
        null
    }
}

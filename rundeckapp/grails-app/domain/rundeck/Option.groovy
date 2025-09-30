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

package rundeck


import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigData
import com.dtolabs.rundeck.core.jobs.JobOption
import grails.web.databinding.DataBindingUtils
import org.rundeck.app.jobs.options.JobOptionConfigPluginAttributes
import org.rundeck.app.jobs.options.JobOptionConfigRemoteUrl
import com.dtolabs.rundeck.plugins.jobs.JobOptionImpl
import com.dtolabs.rundeck.plugins.option.OptionValue
import com.dtolabs.rundeck.util.StringNumericSort
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.rundeck.app.data.model.v1.job.option.OptionValueData
import rundeck.data.validation.shared.SharedJobOptionConstraints

import java.util.regex.Pattern

/*
 * Option domain class, stores the definition of allowable user inputs for a
 * CLI option for a WOrkflow Job (ScheduledExecution)
 *
 * The name is required, specifying the option name.  The defaultValue is the
 * value selected by default, the values are a set of value options to present.
 *
 * if "enforced" is true, then the chosen input must be one of the allowed values.
 *
 * "valuesUrl" is a URL to point to a REST-ful web endpoint to retrieve the values set from.
 * "regex" can be a regular expression to validate the input. (if enforced is false)
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: May 7, 2010 11:15:54 AM
 * $Id$
 */

public class Option implements Comparable, OptionData {

    static final String DEFAULT_DELIMITER =','

    ScheduledExecution scheduledExecution
    String name
    Integer sortIndex
    String description
    String defaultValue
    String defaultStoragePath
    Boolean enforced
    Boolean required
    Boolean isDate
    String dateFormat
    URL valuesUrl
    String label
    /**
     * supercedes valuesUrl and allows longer values. 
     */
    URL valuesUrlLong
    String regex
    String valuesList
    String valuesListDelimiter
    Boolean multivalued
    String delimiter
    Boolean secureInput
    Boolean secureExposed
    String optionType
    String configData
    Boolean multivalueAllSelected
    String optionValuesPluginType
    List<OptionValue> valuesFromPlugin
    Boolean hidden
    Boolean sortValues
    List<String> optionValues


    static belongsTo=[scheduledExecution:ScheduledExecution]
    static transients = ['realValuesUrl', 'configMap','optionConfigData', 'typeFile', 'valuesFromPlugin', 'optionValues']

    static constraints={
        importFrom SharedJobOptionConstraints
        valuesUrl(nullable:true)
        valuesUrlLong(nullable:true)
        scheduledExecution(nullable:true)
    }

    List<OptionValueData> getValuesFromPlugin() {
        return valuesFromPlugin
    }

    public Map getConfigMap() {
        JobOptionConfigData optionConfigData = getOptionConfigData()
        //de-serialize the json
        if (null != optionConfigData) {
            JobOptionConfigPluginAttributes pluginAttributes = optionConfigData.getJobOptionEntry(JobOptionConfigPluginAttributes.TYPE)
            if(pluginAttributes){
                return  pluginAttributes.toMap()
            }
        } else {
            return null
        }
    }

    public void setConfigMap(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            JobOptionConfigPluginAttributes configPluginAttributes= new JobOptionConfigPluginAttributes(obj)
            JobOptionConfigData configData = new JobOptionConfigData()
            configData.addConfig(configPluginAttributes)
            setOptionConfigData(configData)
        }
    }

    public JobOptionConfigData getOptionConfigData() {
        //de-serialize the json
        if (null != configData) {
            final ObjectMapper mapper = new ObjectMapper()
            try {
                return mapper.readValue(configData, JobOptionConfigData.class)
            } catch (JsonParseException e) {
                return null
            }
        } else {
            return null
        }
    }

    public void setOptionConfigData(JobOptionConfigData obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            configData = mapper.writeValueAsString(obj)
        } else {
            configData = null
        }
    }

    static mapping = {
        table "rdoption"
        valuesUrlLong length:3000
        description type: 'text'
        defaultValue type: 'text'
        regex type: 'text'
        optionType type: 'text'
        configData type: 'text'
        valuesList type: 'text'
//        values type: 'string'//, lazy: false
    }
    /**
     * Return canonical map representation
     */
    public Map toMap(){
        final Map map = [:]
        if (null != optionType) {
            map.type = optionType
            def config = getConfigMap()
            if (config) {
                map.config = config
            }
        }
        if (null!=sortIndex) {
            map.sortIndex = sortIndex
        }
        if(sortValues){
            map.sortValues = sortValues
        }
        if(enforced){
            map.enforced=enforced
        }
        if(required){
            map.required=required
        }
        if(isDate){
            map.isDate=isDate
            map.dateFormat=dateFormat
        }
        if(label){
            map.label = label
        }
        if(description){
            map.description=description
        }
        if(defaultValue){
            map.value=defaultValue
        }
        if(defaultStoragePath){
            map.storagePath=defaultStoragePath
        }
        if(getRealValuesUrl()){
            map.valuesUrl=getRealValuesUrl().toExternalForm()

            if(configData){
                JobOptionConfigRemoteUrl jobOptionConfigRemoteUrl = getOptionConfigData().getJobOptionEntry(JobOptionConfigRemoteUrl.TYPE)
                if(jobOptionConfigRemoteUrl){
                    map.configRemoteUrl = jobOptionConfigRemoteUrl.toMap()
                }
            }
        }
        if(regex){
            map.regex=regex
        }
        if(getOptionValues()){
            map.values=getOptionValues()
            map.valuesListDelimiter = valuesListDelimiter?:','
        }
        if(multivalued){
            map.multivalued=multivalued
            map.delimiter=delimiter?:','
            if (multivalueAllSelected) {
                map.multivalueAllSelected = true
            }
        }
        if(secureInput){
            map.secure=secureInput
        }
        if(secureExposed && secureInput){
            map.valueExposed= secureExposed
        }
        if(optionValuesPluginType) {
            map.optionValuesPluginType = optionValuesPluginType
        }
        if(hidden){
            map.hidden = hidden
        }
        if(name){
            map.name = name
        }
        return map
    }
    public JobOption toJobOption(){
        JobOptionImpl.fromOptionMap(toMap())
    }

    public static Option fromMap(String name,Map datain){
        Option opt = new Option()
        Map data = [:]
        data.putAll(datain)
        data['name']=name


        data.enforced=data.enforced?true:false
        data.required=data.required?true:false
        data.isDate=data.isDate?true:false
        if(!data.isDate){
            data.remove('dateFormat')
        }
        if(data.type){
            data.optionType = data.remove('type')
            if (data.config && data.config instanceof Map) {
                data.configMap = data.remove('config')
            }
        }
        if(data.value){
            data.defaultValue=data.remove('value')
        }
        if(data.storagePath){
            data.defaultStoragePath=data.remove('storagePath')
        }
        if(data.valuesUrl){
            data.valuesUrlLong = data.remove('valuesUrl')
        }

        if(data.values){
            def dataval = data.remove('values')
            def values = dataval instanceof Collection ? dataval : new TreeSet([dataval])
            if(!data.valuesListDelimiter){
                data.valuesListDelimiter=DEFAULT_DELIMITER
            }
            data.optionValues=new ArrayList<String>(values);
            data.valuesList = values
        }

        def multivalued = data.remove('multivalued')
        def delimiter = data.remove('delimiter')
        if(multivalued in [true, 'true']){
            data.multivalued=true
            data.multivalueAllSelected = Boolean.valueOf(data.multivalueAllSelected)
            if(delimiter) {
                data.delimiter = delimiter
            }
        }
        if(data.secure){
            data.secureInput=Boolean.valueOf(data.remove('secure'))
        }else{
            data.secureInput=false
        }
        if(data.secureInput && data.valueExposed){
            data.secureExposed=Boolean.valueOf(data.remove('valueExposed'))
        }else{
            data.secureExposed=false
        }
        def configRemoteUrlData = data.remove('configRemoteUrl')
        if(configRemoteUrlData!=null && configRemoteUrlData instanceof Map && configRemoteUrlData.size()>0){
            def configRemoteUrl = JobOptionConfigRemoteUrl.fromMap(configRemoteUrlData)
            def configData = new JobOptionConfigData()
            configData.addConfig(configRemoteUrl)
            data.optionConfigData=configData
        }

        DataBindingUtils.bindObjectToInstance opt, data, [], [], null
        opt.valuesList = opt.produceValuesList()
        return opt
    }
    /**
     * Return the string equivalent of the values set member
     */
    public String produceValuesList(){
        if (valuesListDelimiter == null) {
            valuesListDelimiter = DEFAULT_DELIMITER
        }

        if(optionValues) {
            valuesList = optionValues.join(valuesListDelimiter)
        }else{
            return null
        }
    }

    def beforeUpdate(){
        if (valuesUrl) {
            if (!valuesUrlLong) {
                this.valuesUrlLong = valuesUrl
            }
            this.valuesUrl = null
        }
    }

    def beforeInsert() {
        if (valuesUrl) {
            if(!valuesUrlLong){
                this.valuesUrlLong=valuesUrl
            }
            this.valuesUrl = null
        }
    }
    public URL getRealValuesUrl(){
        return valuesUrl?:valuesUrlLong
    }
    public void setRealValuesUrl(URL url){
        this.valuesUrl=null
        this.valuesUrlLong=url
    }
    /**
     * Convert the valuesList string member into the values set member
     */
    void convertValuesList(){
        if(valuesList!=null){
            if(valuesListDelimiter == null){
                valuesListDelimiter=DEFAULT_DELIMITER
            }
            optionValues=new ArrayList()
            optionValues.addAll(valuesList.split(Pattern.quote(valuesListDelimiter)).collect{it.trim()}.grep{it}.unique() as List)

            if(optionValues && sortValues){
                sortValuesList()
            }
        }
    }

    /**
     *
     * @return
     */
    List listDefaultMultiValues(){
        if(multivalued && defaultValue && delimiter){
            return defaultValue.split(Pattern.quote(delimiter)) as List
        }
        []
    }

    /**
     * Compare by (sortIndex, name)
     * @param obj
     * @return
     */
    int compareTo(obj) {
        if (null != sortIndex && null != obj.sortIndex) {
            return sortIndex <=> obj.sortIndex
        } else if (null == sortIndex && null == obj.sortIndex) {
            return name <=> obj.name
        } else {
            return sortIndex != null ? -1 : 1
        }
    }

    boolean isTypeFile() {
        this.optionType == 'file'
    }

    /**
     * create a clone Option object and set the valuesList string
     */
    public Option createClone(){
        Option opt = new Option()
        ['name', 'description', 'defaultValue', 'defaultStoragePath', 'sortIndex', 'enforced', 'required', 'isDate',
         'dateFormat', 'valuesList', 'valuesUrl', 'valuesUrlLong', 'regex', 'multivalued',
         'multivalueAllSelected', 'label',
         'delimiter', 'optionValuesPluginType',
         'secureInput', 'secureExposed', 'optionType', 'configData', 'hidden','sortValues','valuesListDelimiter'].
                each { k ->
            opt[k]=this[k]
        }
        if(!opt.valuesList){
            opt.valuesList=this.produceValuesList()
        }
        return opt
    }


    void sortValuesList(){
        def numericValues = optionValues.findAll { it.isNumber() }.collect {it.toDouble()}
        if(numericValues.size()==optionValues.size()){
            def stringNumericList= optionValues.findAll { it.isNumber() }.collect {new StringNumericSort(it, it.toDouble())}
            StringNumericSort.sortNumeric(stringNumericList)
            optionValues = stringNumericList.collect{it.strValue}
        }else{
            optionValues = optionValues.sort()
        }
    }

    List<String> getOptionValues(){
        if(optionValues==null){
            if(valuesList){
                convertValuesList()
            }
        }
        optionValues
    }

    List parseOptionValueList(){
        if(valuesFromPlugin){
            return valuesFromPlugin.collect{[name:it.name,value:it.value]}
        }
        return null
    }

    JobOptionConfigRemoteUrl getConfigRemoteUrl(){
        return this.getOptionConfigData()?.getJobOptionEntry(JobOptionConfigRemoteUrl.TYPE)
    }

    public String toString ( ) {
        return "Option{" +
        "name='" + name + '\'' +
        "sortIndex='" + sortIndex + '\'' +
        ", description='" + description + '\'' +
        ", defaultValue='" + defaultValue + '\'' +
        ", storagePath='" + defaultStoragePath + '\'' +
        ", enforced=" + enforced +
        ", required=" + required +
        ", isDate=" + isDate +
        ", dateFormat=" + dateFormat +
        ", valuesUrl=" + getRealValuesUrl() +
        ", regex='" + regex + '\'' +
        ", multivalued='" + multivalued + '\'' +
                ", multivalueAllSelected='" + multivalueAllSelected + '\'' +
        ", secureInput='" + secureInput + '\'' +
        ", secureExposed='" + secureExposed + '\'' +
        ", delimiter='" + delimiter + '\'' +
               ', optionValuesPluginType=' + optionValuesPluginType + '\'' +
                ", optionType='" + optionType + '\'' +
                ", configData='" + configData + '\'' +
        ", hidden='" + '\'' +
        '}' ;
    }


}

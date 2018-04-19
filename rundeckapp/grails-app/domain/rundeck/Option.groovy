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

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper

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

public class Option implements Comparable{


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
    SortedSet<String> values
    static hasMany = [values:String]
    URL valuesUrl
    String label
    /**
     * supercedes valuesUrl and allows longer values. 
     */
    URL valuesUrlLong
    String regex
    String valuesList
    Boolean multivalued
    String delimiter
    Boolean secureInput
    Boolean secureExposed
    String optionType
    String configData
    Boolean multivalueAllSelected

    static belongsTo=[scheduledExecution:ScheduledExecution]
    static transients = ['valuesList', 'realValuesUrl', 'configMap', 'typeFile']

    static constraints={
        name(nullable:false,blank:false,matches: '[a-zA-Z_0-9.-]+')
        description(nullable:true)
        defaultValue(nullable:true)
        defaultStoragePath(nullable:true,matches: '^(/?)keys/.+')
        enforced(nullable:false)
        required(nullable:true)
        isDate(nullable:true)
        dateFormat(nullable: true, maxSize: 30)
        values(nullable:true)
        valuesUrl(nullable:true)
        valuesUrlLong(nullable:true)
        regex(nullable:true)
        scheduledExecution(nullable:true)
        delimiter(nullable:true)
        multivalued(nullable:true)
        secureInput(nullable:true)
        secureExposed(nullable:true)
        sortIndex(nullable:true)
        optionType(nullable: true, maxSize: 255)
        configData(nullable: true)
        multivalueAllSelected(nullable: true)
        label(nullable: true)
    }


    public Map getConfigMap() {
        //de-serialize the json
        if (null != configData) {
            final ObjectMapper mapper = new ObjectMapper()
            try {
                return mapper.readValue(configData, Map.class)
            } catch (JsonParseException e) {
                return null
            }
        } else {
            return null
        }
    }

    public void setConfigMap(Map obj) {
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
        }
        if(regex){
            map.regex=regex
        }
        if(values){
            map.values=values as List
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
        return map
    }

    public static Option fromMap(String name,Map data){
        Option opt = new Option()
        opt.name=name
        if(data.label){
            opt.label=data.label
        }
        opt.enforced=data.enforced?true:false
        opt.required=data.required?true:false
        opt.isDate=data.isDate?true:false
        if(opt.isDate){
            opt.dateFormat=data.dateFormat
        }
        if (data.type) {
            opt.optionType = data.type
            def config = data.config
            if (config && config instanceof Map) {
                opt.configMap = config
            }
        }
        if(data.description){
            opt.description=data.description
        }
        if(data.sortIndex!=null){
            opt.sortIndex=data.sortIndex
        }
        if(data.value){
            opt.defaultValue = data.value
        }
        if(data.storagePath){
            opt.defaultStoragePath=data.storagePath
        }
        if(data.valuesUrl){
            opt.realValuesUrl=new URL(data.valuesUrl)
        }
        if(null!=data.regex){
            opt.regex=data.regex
        }
        if(data.values){
            opt.values=data.values instanceof Collection?new TreeSet(data.values):new TreeSet([data.values])
        }
        if(data.multivalued){
            opt.multivalued=true
            if(data.delimiter){
                opt.delimiter=data.delimiter
            }
            opt.multivalueAllSelected = Boolean.valueOf(data.multivalueAllSelected)
        }
        if(data.secure){
            opt.secureInput=Boolean.valueOf(data.secure)
        }else{
            opt.secureInput=false
        }
        if(opt.secureInput && data.valueExposed){
            opt.secureExposed=Boolean.valueOf(data.valueExposed)
        }else{
            opt.secureExposed=false
        }
        return opt
    }
    /**
     * Return the string equivalent of the values set member
     */
    public String produceValuesList(){
        if(values){
            return values.join(",")
        }else{
            return ''
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
            def x=new TreeSet()
            x.addAll(valuesList.split(",").collect{it.trim()}.grep{it} as List)
            values=x
            valuesList=null
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
         'dateFormat', 'values', 'valuesList', 'valuesUrl', 'valuesUrlLong', 'regex', 'multivalued',
         'multivalueAllSelected', 'label',
         'delimiter',
         'secureInput', 'secureExposed', 'optionType', 'configData'].
                each { k ->
            opt[k]=this[k]
        }
        if(!opt.valuesList && values){
            opt.valuesList=this.produceValuesList()
        }
        return opt
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
        ", values=" + values +
        ", valuesUrl=" + getRealValuesUrl() +
        ", regex='" + regex + '\'' +
        ", multivalued='" + multivalued + '\'' +
                ", multivalueAllSelected='" + multivalueAllSelected + '\'' +
        ", secureInput='" + secureInput + '\'' +
        ", secureExposed='" + secureExposed + '\'' +
        ", delimiter='" + delimiter + '\'' +
                ", optionType='" + optionType + '\'' +
                ", configData='" + configData + '\'' +
        '}' ;
    }


}

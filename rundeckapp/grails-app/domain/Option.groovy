/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
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
    String description
    String defaultValue
    Boolean enforced
    Boolean required
    SortedSet values
    static hasMany = [values:String]
    URL valuesUrl
    String valuesUrlString
    String regex
    String valuesList
    static belongsTo=[scheduledExecution:ScheduledExecution]
    static transients=['valuesList','valuesUrlString']

    static constraints={
        name(nullable:false,blank:false)
        description(nullable:true)
        defaultValue(nullable:true)
        enforced(nullable:false)
        required(nullable:true)
        values(nullable:true)
        valuesUrl(nullable:true)
        regex(nullable:true)
        scheduledExecution(nullable:true)
    }

    /**
     * Return canonical map representation
     */
    public Map toMap(){
        final Map map = [:]
        if(enforced){
            map.enforced=enforced
        }
        if(required){
            map.required=required
        }
        if(description){
            map.description=description
        }
        if(defaultValue){
            map.value=defaultValue
        }
        //valuesUrl: valuesUrl.toExternalForm(), regex: regex, values: values
        if(valuesUrl){
            map.valuesUrl=valuesUrl.toExternalForm()
        }
        if(regex){
            map.regex=regex
        }
        if(values){
            map.values=values as List
        }
        return map
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

    int compareTo(obj) {
        name.compareTo(obj.name)
    }


    /**
     * create a clone Option object and set the valuesList string
     */
    public Option createClone(){
        Option opt = new Option()
        ['name','description','defaultValue','enforced','required','values','valuesList','valuesUrl','regex'].each{k->
            opt[k]=this[k]
        }
        if(!opt.valuesList && values){
            opt.valuesList=this.produceValuesList()
        }
        return opt
    }
}
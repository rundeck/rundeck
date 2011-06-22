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
	String remoteValue
    Boolean enforced
    Boolean required
    SortedSet values
    static hasMany = [values:String]
    URL valuesUrl
    String valuesUrlString
    String regex
    String valuesList
    Boolean multivalued
    String delimiter
    static belongsTo=[scheduledExecution:ScheduledExecution]
    static transients=['valuesList','valuesUrlString']

    static constraints={
        name(nullable:false,blank:false)
        description(nullable:true)
        defaultValue(nullable:true)
		remoteValue(nullable:true)
        enforced(nullable:false)
        required(nullable:true)
        values(nullable:true)
        valuesUrl(nullable:true)
        regex(nullable:true)
        scheduledExecution(nullable:true)
        delimiter(nullable:true)
        multivalued(nullable:true)
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
		if(remoteValue){
			map.value=remoteValue
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
        if(multivalued){
            map.multivalued=multivalued
            map.delimiter=delimiter?:','
        }
        return map
    }

    public static Option fromMap(String name,Map data){
        Option opt = new Option()
        opt.name=name
        opt.enforced=data.enforced?true:false
        opt.required=data.required?true:false
        if(data.description){
            opt.description=data.description
        }
        if(data.value){
            opt.defaultValue = data.value
        }
		if(data.remoteValue){
			opt.remoteValue = data.remoteValue
		}
        if(data.valuesUrl){
            opt.valuesUrl=new URL(data.valuesUrl)
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
        ['name','description','defaultValue','remoteValue','enforced','required','values','valuesList','valuesUrl','regex','multivalued','delimiter'].each{k->
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
        ", description='" + description + '\'' +
        ", defaultValue='" + defaultValue + '\'' +
		", remoteValue='" + remoteValue + '\'' +
        ", enforced=" + enforced +
        ", required=" + required +
        ", values=" + values +
        ", valuesUrl=" + valuesUrl +
        ", regex='" + regex + '\'' +
        ", multivalued='" + multivalued + '\'' +
        ", delimiter='" + delimiter + '\'' +
        '}' ;
    }


}
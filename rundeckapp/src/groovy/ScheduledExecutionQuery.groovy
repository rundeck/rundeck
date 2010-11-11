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
 * ScheduledExecutionQuery.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Feb 12, 2010 1:02:43 PM
 * $Id$
 */

public class ScheduledExecutionQuery extends BaseQuery{

    String jobFilter
    String projFilter
    String groupPath
    
    String descFilter
    String loglevelFilter
    String idlist

    /**
     * text filters
     */
    public final static TEXT_FILTERS = [
                job:'jobName',
                proj:'project',
                desc:'description',
            ]
    /**
     * equality filters
     */
    public final static EQ_FILTERS=[
                loglevel:'loglevel',
            ]
    /**
     * Boolean filters
     */
    public final static  BOOL_FILTERS=[
            ]
    /**
     * all filters
     */
    public final static  ALL_FILTERS = [ :]
    static{
            ALL_FILTERS.putAll(TEXT_FILTERS)
            ALL_FILTERS.putAll(EQ_FILTERS)
            ALL_FILTERS.putAll(BOOL_FILTERS)
    }


    static constraints={
        
    }


    public String toString(){
        StringBuffer sb = new StringBuffer()
        sb.append("ScheduledExecutionQuery[")
        ALL_FILTERS.each{k,v->
            if(this[k+'Filter']){
                sb.append(k)
                sb.append('Filter: ')
                sb.append("'${this[k+'Filter']}',")
            }
        }
        if(this['groupPath']){
            sb.append('groupPath: ')
            sb.append("'${this['groupPath']}',")
        }
        sb.append("]")
        return sb.toString()
    }
    /**
     * validate filter
     */
    public void configureFilter(){
       
    }

}
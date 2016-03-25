package com.dtolabs.rundeck.app.api

import com.dtolabs.rundeck.core.common.FrameworkResource
import grails.validation.Validateable

/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */
 
/*
 * ApiBulkJobDeleteRequest.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 9/25/12 5:57 PM
 * 
 */
@Validateable
class ApiBulkJobDeleteRequest {
    public static final String IDLIST_REGEX = '^' + FrameworkResource.VALID_RESOURCE_NAME_CHARSET_REGEX + '(,' +
            '' + FrameworkResource.VALID_RESOURCE_NAME_CHARSET_REGEX + ')*$'
    List<String> ids
    String idlist
    String id
    static constraints={
        ids(nullable:true,validator: {val,obj->
            def test = val.every{
                it==~FrameworkResource.VALID_RESOURCE_NAME_REGEX
            }
            if(!test){
                return false
            }
        })
        idlist(nullable:true, matches: IDLIST_REGEX)
        id(nullable:true, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
    }
    def Set<String> generateIdSet(){
        def ids = new HashSet<String>()
        if (this.ids) {
            ids.addAll(this.ids)
        }
        if (idlist) {
            ids.addAll(idlist.split(','))
        }
        ids
    }
}

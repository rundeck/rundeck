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

package com.dtolabs.rundeck.app.api

import com.dtolabs.rundeck.app.api.CDataString
import grails.converters.JSON
import grails.converters.XML

import javax.annotation.PostConstruct

/**
 * Created by greg on 10/28/15.
 */
class ApiMarshallerRegistrar {
    @PostConstruct
    void registerMarshallers() {
        XML.registerObjectMarshaller(CDataString) { data,XML xml->
            if(data.value?.contains('\r') || data.value?.contains('\n')  ) {
                xml.chars('')//forces current tag completion
                xml.stream.append('<![CDATA['+data.value.replaceAll(']]>',']]]]><![CDATA[>')+']]>')
                return null
            }else{
                return data.value
            }
        }
        JSON.registerObjectMarshaller(CDataString) { data, JSON json->
            if(data.value !=null){
                return data.value
            } else{
                json.value(null)
                return null
            }
        }
    }
}

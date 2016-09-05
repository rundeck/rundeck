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

import com.dtolabs.rundeck.app.api.marshall.ApiMarshaller
import com.dtolabs.rundeck.app.api.marshall.CustomJsonMarshaller
import com.dtolabs.rundeck.app.api.marshall.CustomXmlMarshaller
import grails.converters.JSON
import grails.converters.XML
import rundeck.filters.ApiRequestFilters

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
    /**
     *
     * XXX: Note: this is intentionally not called in registerMarshallers, and is instead invoked by Bootstrap.groovy
     * due to an apparent issue where metaclasses for some types are loaded too soon
     * if called via {@code @PostConstruct}
     * <a href="https://github.com/grails/grails-core/issues/9140#issuecomment-143678429">grails issue ref</a>
     */
    void registerApiMarshallers(){
        def curVersion = ApiRequestFilters.API_CURRENT_VERSION
        def api = new ApiMarshaller('com.dtolabs.rundeck.app.api')

        //default marshaller configuration implementation
        XML.registerObjectMarshaller(new CustomXmlMarshaller(api, curVersion))
        JSON.registerObjectMarshaller(new CustomJsonMarshaller(api, curVersion))

        //use custom configuration for specific API versions
        (1..ApiRequestFilters.API_CURRENT_VERSION).each { apivers ->
            XML.createNamedConfig("v${apivers}") { cfg ->
                cfg.registerObjectMarshaller(new CustomXmlMarshaller(api, apivers))
            }
            JSON.createNamedConfig("v${apivers}") { cfg ->
                cfg.registerObjectMarshaller(new CustomJsonMarshaller(api, apivers))
            }
        }

    }
}

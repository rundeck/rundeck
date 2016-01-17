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

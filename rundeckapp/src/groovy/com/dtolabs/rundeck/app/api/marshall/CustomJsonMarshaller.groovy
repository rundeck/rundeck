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

package com.dtolabs.rundeck.app.api.marshall

import grails.converters.JSON
import grails.converters.XML
import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.NameAwareMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller

/**
 * Custom JSON marshaller which handles "wrapper" objects. If a {@link ApiResource} class defines
 * a single field with {@link SubElement}, that object will be marshalled in place of the parent.
 * If any other non-ignored fields are defined, then the subelement will be treated as a simple field.
 */
class CustomJsonMarshaller extends BaseCustomMarshaller implements ObjectMarshaller<JSON> {

    CustomJsonMarshaller(ApiMarshaller marshaller, int apiVersion) {
        super(marshaller, apiVersion)
    }

    @Override
    boolean supports(final Object object) {
        return super.supports(object)
    }

    @Override
    void marshalObject(final Object object, final JSON converter) throws ConverterException {
        def (Map attrs, Map fields, Map subelements) = introspect(object)

        //if only one subelement, simply apply that
        if (subelements.size() == 1 && !fields) {
            converter.convertAnother(subelements.values().first())
            return
        }
        converter.convertAnother(attrs + fields + subelements)
    }
}

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

import grails.converters.XML
import grails.util.GrailsNameUtils
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.converters.marshaller.NameAwareMarshaller
import org.grails.web.converters.marshaller.ObjectMarshaller

/**
 * Custom XMl marshaller, uses annotations on fields/class to convert to XML.
 * Fields annotated with {@link XmlAttribute} will be used as attributes of the root element.
 * If the type is annotated with {@link ElementName} that value will be used as the root element name.
 * Any fields annotated with {@link SubElement} will be inlined in the root element, instead of wrapped
 * with the field name.
 *
 * TODO: better handling of embedded object types
 */
class CustomXmlMarshaller extends BaseCustomMarshaller implements ObjectMarshaller<XML>, NameAwareMarshaller {
    CustomXmlMarshaller(ApiMarshaller marshaller, int apiVersion) {
        super(marshaller, apiVersion)
    }
    @Override
    boolean supports(final Object object) {
        return super.supports(object)
    }

    @Override
    String getElementName(final Object o) {
        def annotation = o.getClass().getAnnotation(ElementName)
        if (annotation) {
            return annotation.value()
        }
        return GrailsNameUtils.getPropertyName(o.class)
    }


    @Override
    void marshalObject(final Object object, final XML xml) throws ConverterException {
        def (Map attrs, Map fields, Map subelements, Map colnames, Map<String,String> format) = introspect(object)

        attrs.each { String k, Object v ->
            xml.attribute(k, formatCustom(v,format[k])?.toString() ?: '')
        }
        xml.build {
            def xmld = delegate
            fields.each { k, v ->
                if (v instanceof Collection && colnames[k]) {
                    def name = colnames[k]
                    xmld."$k" {
                        v.each { val ->
                            xmld."$name"(val)
                        }
                    }
                } else if (v instanceof Map && colnames[k]) {
                    def name = colnames[k]
                    xmld."$k" {
                        v.each { mk, val ->
                            xmld."$name"([key: mk], val)
                        }
                    }
                } else {
                    xmld."$k"(formatCustom(v,format[k]))
                }
            }
        }
        subelements.each { k, v ->
            xml.convertAnother(formatCustom(v,format[k]))
        }

    }

}

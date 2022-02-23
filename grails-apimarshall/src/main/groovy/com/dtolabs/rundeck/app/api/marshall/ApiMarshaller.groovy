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

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter

import java.lang.reflect.Field

/**
 * Scans all classes in a package for {@link ApiResource} annotations, and can introspect annotations on those classes.
 */
class ApiMarshaller {
    String packageName
    List<Class> clazzes
    protected Map<Class, Map<String, ResourceField>> classFields = [:]

    ApiMarshaller(String packageName) {
        clazzes = null
        this.packageName = packageName
    }

    /**
     * @param object
     * @return true if the object's class is one of the ApiResource types
     */
    boolean supports(final Object object) {
        if (null == clazzes) {
            clazzes = scanPackageForAnnotatedTypes(packageName, ApiResource)
        }
        return object.class in clazzes
    }

    def Map<String, ResourceField> getFieldDefsForClass(Class clazz) {
        if (null == classFields[clazz]) {
            classFields[clazz] = introspectFields(clazz)
        }
        return classFields[clazz]
    }


    private static ArrayList<Class> scanPackageForAnnotatedTypes(String pkgName, Class annotation) {
        def apiResourceTypes = []
        def scanner = new ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(new AnnotationTypeFilter(annotation))
        scanner.findCandidateComponents(pkgName).each { BeanDefinition bd ->
            apiResourceTypes << Class.forName(bd.beanClassName)
        }
        apiResourceTypes
    }

    private static ArrayList<Class> scanPackageForAllTypes(String pkgName) {
        def apiResourceTypes = []
        def scanner = new ClassPathScanningCandidateComponentProvider(false)
        scanner.findCandidateComponents(pkgName).each { BeanDefinition bd ->
            apiResourceTypes << Class.forName(bd.beanClassName)
        }
        apiResourceTypes
    }

    private static Map<String, ResourceField> introspectFields(Class clazz) {
        def Map<String, ResourceField> fieldDefs = [:]
        clazz.getDeclaredFields().each { Field f ->
            def fieldDef = new ResourceField(name: f.name)
            fieldDefs.put(f.name, fieldDef)

            ApiVersion apiVersionCheck = f.getAnnotation(ApiVersion)
            if (null != apiVersionCheck) {
                fieldDef.apiVersionMin = apiVersionCheck.value()
                fieldDef.apiVersionMax = apiVersionCheck.max()
            }
            Ignore ignore = f.getAnnotation(Ignore)
            if (null != ignore) {
                fieldDef.ignoreOnlyIfNull = ignore.onlyIfNull()
                fieldDef.ignore = !ignore.onlyIfNull()
            }
            def attr = f.getAnnotation(XmlAttribute)
            if (attr) {
                fieldDef.xmlAttr = attr.value() ?: f.name
            }

            def subElement = f.getAnnotation(SubElement)
            if (subElement != null) {
                fieldDef.subElement = true

            }
            def elemName = f.getAnnotation(ElementName)
            if (elemName) {
                fieldDef.elementName = elemName.value()
            }
            def colElem = f.getAnnotation(CollectionElement)
            if (colElem) {
                fieldDef.collectionKeyName = colElem.value()
            }
            def fmat = f.getAnnotation(CustomFormat)
            if (fmat) {
                fieldDef.customFormat = fmat.value()
            }
        }
        return fieldDefs
    }

}

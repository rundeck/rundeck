/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package rundeck.controllers

import java.lang.annotation.Annotation
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Allows multiple "afterInterceptors" on controllers by annotation with {@link AfterInterceptor}
 */
trait AfterInterceptorChain {
    TypeHelperUtil typeHelperUtil = new TypeHelperUtil(null)
    def afterInterceptor = { model ->

        if (null == typeHelperUtil.clazz) {
            typeHelperUtil.clazz = this.class
        }
        //look for AfterInterceptor annotations

        def model2 = typeHelperUtil.loadAfterInterceptorMethods().inject(model) { mod, Method method ->
            def result = method.invoke(this, mod)
            if (result != null && result instanceof Map) {
                return result
            } else {
                return mod
            }
        }


        model2 = typeHelperUtil.loadAfterInterceptorFields().inject(model2) { mod, Field field ->
            def result = field.get(this).call(mod)
            if (result != null && result instanceof Map) {
                return result
            } else {
                return mod
            }
        }

        model2
    }
}

public class TypeHelperUtil {
    Class clazz

    TypeHelperUtil(final Class clazz) {
        this.clazz = clazz
    }
    private List<Class> superList

    public List<Class> loadAfterInterceptorSupers() {
        if (null == superList) {
            superList = listSuperTypes(clazz)
        }
        superList
    }
    private List<Method> methodList

    public List<Method> loadAfterInterceptorMethods() {
        if (null == methodList) {
            methodList = listAnnotatedMethods(
                loadAfterInterceptorSupers(),
                AfterInterceptor
            )
        }
        methodList
    }
    private List<Field> fieldList

    public List<Field> loadAfterInterceptorFields() {
        if (null == fieldList) {
            fieldList = listAnnotatedFieldsOfType(
                loadAfterInterceptorSupers(),
                AfterInterceptor,
                Closure
            )
        }
        fieldList
    }

    static List<Field> listAnnotatedFieldsOfType(
        List<Class> supers,
        Class<? extends Annotation> annotation,
        Class<?> type
    ) {
        List<Field> fieldList = []

        supers.each { Class btype ->
            fieldList.addAll btype.getDeclaredFields()
        }
        def fields = fieldList.findAll {
            it.getAnnotation(annotation) != null && it.getType() == type
        }
        fields
    }

    static List<Method> listAnnotatedMethods(List<Class> supers, Class<? extends Annotation> annotation) {
        ArrayList<Method> methodList = []
        supers.each { Class btype ->
            methodList.addAll btype.getDeclaredMethods()
        }
        def methods = methodList.findAll {
            it.getAnnotation(annotation) != null
        }
        methods
    }

    static List<Class> listSuperTypes(Class<?> clazz) {
        def supers = [clazz]
        def ctype = clazz
        while (ctype.superclass != Object.class) {
            supers << ctype.superclass
            ctype = ctype.superclass
        }
        supers
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD, ElementType.FIELD])
/**
 * Annotation to indicate a method of Closure field is an AfterInterceptor for use with {@link AfterInterceptorChain}
 */
public @interface AfterInterceptor {

}

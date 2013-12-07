package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.server.plugins.builder.ScriptPluginBuilder
import org.springframework.scripting.groovy.GroovyObjectCustomizer

/**
 * PluginCustomizer provides the Rundeck groovy script plugin DSL to define a plugin builder by use of a
 * `rundeckPlugin` method.
 * Created by greg
 * Date: 4/16/13
 * Time: 3:19 PM
 */
class PluginCustomizer implements GroovyObjectCustomizer {

    public void customize(GroovyObject goo) {
        if (goo instanceof Script) {
            goo.metaClass.rundeckPlugin = { Class clazz, Closure clos ->
                def builder = ScriptPluginBuilder.forPluginClass(clazz, goo.class.name)
                if(builder){
                    clos.delegate = builder
                    clos.resolveStrategy = Closure.DELEGATE_FIRST
                    clos.call()
                    return builder
                }
                return goo;
            }
        }
    }
}

package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.apache.log4j.Logger
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/24/13
 * Time: 12:03 PM
 */
class StreamingLogWriterPluginBuilder extends ScriptPluginBuilder implements PluginBuilder<StreamingLogWriterPlugin>{
    static Logger logger = Logger.getLogger(StreamingLogWriterPluginBuilder)
    Map<String, Closure> handlers = [:]
    StreamingLogWriterPluginBuilder(String name) {
        super(name)
    }

    @Override
    StreamingLogWriterPlugin buildPlugin() {
        return new ScriptStreamingLogWriterPlugin(handlers, descriptionBuilder.build())
    }

    @Override
    Object invokeMethod(String name, Object args) {
        List list = InvokerHelper.asList(args);
        if (name=='open' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptStreamingLogWriterPlugin.validOpenClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'close' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptStreamingLogWriterPlugin.validCloseClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'addEvent' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptStreamingLogWriterPlugin.validEventClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
    }
}

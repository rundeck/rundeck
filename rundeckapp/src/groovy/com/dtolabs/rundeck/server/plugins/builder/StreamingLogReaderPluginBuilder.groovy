package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.plugins.logging.StreamingLogReaderPlugin
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.apache.log4j.Logger
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/24/13
 * Time: 2:27 PM
 */
class StreamingLogReaderPluginBuilder extends ScriptPluginBuilder implements PluginBuilder<StreamingLogReaderPlugin> {
    static Logger logger = Logger.getLogger(StreamingLogReaderPluginBuilder)
    Map<String, Closure> handlers = [:]

    StreamingLogReaderPluginBuilder(String name) {
        super(name)
    }

    @Override
    StreamingLogReaderPlugin buildPlugin() {
        return new ScriptStreamingLogReaderPlugin(handlers, descriptionBuilder.build())
    }

    @Override
    Object invokeMethod(String name, Object args) {
        List list = InvokerHelper.asList(args);
        if (name == 'open' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptStreamingLogReaderPlugin.validOpenClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'close' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptStreamingLogReaderPlugin.validCloseClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'next' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptStreamingLogReaderPlugin.validNextClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'info' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptStreamingLogReaderPlugin.validInfoClosure (list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
    }
}

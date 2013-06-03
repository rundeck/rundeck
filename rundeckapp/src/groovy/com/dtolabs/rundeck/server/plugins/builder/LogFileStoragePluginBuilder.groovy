package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.apache.log4j.Logger
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 6/3/13
 * Time: 2:51 PM
 */
class LogFileStoragePluginBuilder extends ScriptPluginBuilder implements PluginBuilder<LogFileStoragePlugin>{
    static Logger logger = Logger.getLogger(StreamingLogWriterPluginBuilder)
    Map<String, Closure> handlers = [:]

    LogFileStoragePluginBuilder (String name) {
        super(name)
    }

    @Override
    Object buildPlugin() {
        return new ScriptLogFileStoragePlugin(handlers, descriptionBuilder.build())
    }

    @Override
    Object invokeMethod(String name, Object args) {
        List list = InvokerHelper.asList(args);
        if (name == 'state' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptLogFileStoragePlugin.validStateClosure (list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'store' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptLogFileStoragePlugin.validStoreClosure  (list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'retrieve' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptLogFileStoragePlugin.validRetrieveClosure  (list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }

        throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
    }
}

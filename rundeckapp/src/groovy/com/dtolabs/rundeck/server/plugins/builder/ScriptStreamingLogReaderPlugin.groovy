package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.app.internal.logging.DefaultLogEvent
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.logging.StreamingLogReaderPlugin
import org.apache.log4j.Logger

import java.text.SimpleDateFormat

/**
 * Implements a StreamingLogReaderPlugin from a set of closures
 */
class ScriptStreamingLogReaderPlugin implements StreamingLogReaderPlugin, Describable,Configurable {
    static Logger logger = Logger.getLogger(ScriptStreamingLogReaderPlugin)
    Description description
    private Map<String, Closure> handlers
    Map configuration
    private Map streamContext
    Map<String, ? extends Object> context
    long offset = 0
    Date lastModified
    long totalSize = -1
    LogEvent nextEvent
    boolean complete
    private SimpleDateFormat simpleDateFormat

    ScriptStreamingLogReaderPlugin(Map<String, Closure> handlers, Description description) {
        this.description = description
        this.handlers = handlers
        simpleDateFormat= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        simpleDateFormat.timeZone=TimeZone.getTimeZone("GMT")
    }

    @Override
    void configure(Properties configuration) throws ConfigurationException {
        this.configuration = new HashMap(configuration)
    }
    @Override
    boolean initialize(Map<String, ? extends Object> context) {
        this.context = context
        return readInfo()
    }

    Date getLastModified() {
        return this.lastModified
    }

    long getTotalSize() {
        return this.totalSize
    }

    public void openStream(Long offset) throws IOException {
        logger.debug("openStream ${context}, off: ${offset}")
        ['info', 'open', 'next', 'close'].each {
            if (!handlers[it]) {
                throw new RuntimeException("LogReaderPlugin: '${it}' closure not defined for plugin ${description.name}")
            }
        }
        def closure = handlers.open
        if (closure.getMaximumNumberOfParameters() == 3) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            this.streamContext = newclos.call(context, configuration, offset)
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = [execution: context, configuration: configuration]
            this.streamContext = newclos.call(context, offset)
        } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Object) {
            def Closure newclos = closure.clone()
            newclos.delegate = [execution: context, configuration: configuration]
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            this.streamContext = newclos.call(offset)
        } else {
            logger.error("LogWriterPlugin: 'open' closure signature invalid for plugin ${description.name}, cannot open")
        }
        this.offset=offset
        readNextEvent()
    }

    private boolean readInfo() {
        logger.debug("read info")
        def closure = handlers.info
        if (!closure) {
            throw new RuntimeException("LogReaderPlugin: 'info' closure not defined for plugin ${description.name}")
        }
        def metadata = [:]
        def readystate=false
        if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = [execution: context, configuration: configuration]
            metadata = newclos.call(context, configuration)
        } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Object) {
            def Closure newclos = closure.clone()
            newclos.delegate = [execution: context, configuration: configuration]
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            metadata = newclos.call(context)
        } else {
            logger.error("LogWriterPlugin: 'info' closure signature invalid for plugin ${description.name}, cannot open")
        }
        if (metadata) {
            Object lastmod=metadata.lastModified
            if (null != lastmod && (lastmod instanceof Long)) {
                lastModified = new Date(lastmod)
            } else if (null != lastmod && (lastmod instanceof Date)) {
                lastModified = lastmod
            }
            if (metadata.totalSize) {
                totalSize = metadata.totalSize
            }
            readystate=metadata.ready
        }
        return readystate
    }

    @Override
    boolean hasNext() {
        logger.debug("hasNext? "+(nextEvent!=null))
        return nextEvent != null
    }

    @Override
    LogEvent next() {
        logger.debug("next: " + nextEvent)
        def found = nextEvent
        nextEvent = null
        readNextEvent()
        return found
    }

    private void readNextEvent() {
        logger.debug("readNextEvent")
        def closure = handlers.next
        if (!closure) {
            throw new RuntimeException("LogReaderPlugin: 'next' closure not defined for plugin ${description.name}")
        }
        if (closure.getMaximumNumberOfParameters() == 1) {
            def Closure newclos = closure.clone()
            def thisplugin = this
            def del = new Expando([context: this.streamContext, configuration: configuration])
            del.complete= {
                logger.debug("complete() called")
                thisplugin.complete = true
            }
            newclos.delegate = del
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            def result = newclos.call(this.streamContext)
            if(null!=result?.offset){
                this.offset=result.offset
            }
            if(result?.complete){
                this.complete=true
            }
            nextEvent = createEvent(result.event)
        } else {
            logger.error("LogWriterPlugin: 'open' closure signature invalid for plugin ${description.name}, cannot open")
        }
    }

    private LogEvent createEvent(def data) {
        logger.debug("createEvent with data: ${data}")
        if (data instanceof LogEvent) {
            return (LogEvent) data
        } else if (data instanceof Map) {
            def DefaultLogEvent event = new DefaultLogEvent()
            event.message = data.remove('message')
            event.loglevel = createLogLevel(data.remove('loglevel'))
            event.datetime = createDate(data.remove('datetime'))
            event.eventType = data.remove('eventType')
            event.metadata = data.containsKey('meta')?data.meta:data
            return event
        }
        return null
    }

    private Date createDate(Object time) {
        if(time instanceof Date){
            return (Date)time
        }else if((time instanceof Long)){
            return new Date(time)
        }else if(time instanceof String){
            return simpleDateFormat.parse((String)time)
        } else {
            return new Date(0)
        }
    }

    private static LogLevel createLogLevel(Object data) {
        if(data instanceof LogLevel){
            return (LogLevel)data
        }else if(data instanceof String){
            return LogLevel.looseValueOf((String)data,LogLevel.OTHER)
        }else{
            return LogLevel.OTHER
        }
    }

    @Override
    void remove() {
        throw new RuntimeException("Unsupported")
    }

    @Override
    void close() {
        logger.debug("close")
        def closure = handlers.close
        if (closure.getMaximumNumberOfParameters() == 1) {
            def Closure newclos = closure.clone()
            newclos.delegate = [context: this.streamContext, configuration: configuration]
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            try {
                newclos.call(this.streamContext)
            } catch (IOException e) {
                logger.error("LogReaderPlugin: 'close' for plugin ${description.name}: " + e.message, e)
            }
        } else {
            logger.error("LogReaderPlugin: 'close' closure signature invalid for plugin ${description.name}, cannot close")
        }
    }

    public static boolean validOpenClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 3) {
            return closure.parameterTypes[0] == Map && closure.parameterTypes[1] == Map && (closure.parameterTypes[2] in [Long, long.class])
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == Map && (closure.parameterTypes[1] in [Long, long.class])
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            return (closure.parameterTypes[0] in [Long, long.class]) || closure.parameterTypes[0] == Object
        }
        return false
    }

    public static boolean validInfoClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == Map && closure.parameterTypes[1] == Map
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            return true
        }
        return false
    }

    public static boolean validNextClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 1) {
            return true
        }
        return false
    }

    public static boolean validCloseClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 1) {
            return true
        }
        return false
    }
}

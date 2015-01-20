package com.dtolabs.rundeck.core.tasks.net;

import com.dtolabs.rundeck.core.execution.BaseLogger;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;

/**
 * Uses an inheritable ThreadLocal {@link BaseLogger} to log Jsch messages, bind it by calling {@link #getInstance(com.dtolabs.rundeck.core.execution.BaseLogger, int)}
 * and then {@link JSch#setLogger(com.jcraft.jsch.Logger) }
 *
 *
 */
public class ThreadBoundJschLogger implements Logger {
    private InheritableThreadLocal<BaseLogger> baseLogger;
    private InheritableThreadLocal<PluginLogger> pluginLogger;
    private InheritableThreadLocal<Integer> logLevel;
    private static ThreadBoundJschLogger instance;

    private ThreadBoundJschLogger() {
        baseLogger = new InheritableThreadLocal<BaseLogger>();
        pluginLogger = new InheritableThreadLocal<PluginLogger>();
        logLevel = new InheritableThreadLocal<Integer>();
    }

    /**
     *  @return the shared instance
     * @param logger logger
     * @param loggingLevel level
     */
    public static ThreadBoundJschLogger getInstance(final BaseLogger logger, final int loggingLevel) {
        getInstance();
        instance.setThreadLogger(logger, loggingLevel);
        return instance;
    }
    /**
     * @return the shared instance
     * @param logger logger
     * @param loggingLevel level
     */
    public static ThreadBoundJschLogger getInstance(final PluginLogger logger, final int loggingLevel) {
        getInstance();
        instance.setThreadLogger(logger, loggingLevel);
        return instance;
    }

    private static void getInstance() {
        synchronized (ThreadBoundJschLogger.class) {
            if (null == instance) {
                instance = new ThreadBoundJschLogger();
            }
        }
    }

    /**
     * Set the thread-inherited logger with a loglevel on Jsch
     *
     * @param logger logger
     * @param loggingLevel level
     */
    private void setThreadLogger(BaseLogger logger, int loggingLevel) {
        baseLogger.set(logger);
        logLevel.set(loggingLevel);
        JSch.setLogger(this);
    }
    /**
     * Set the thread-inherited logger with a loglevel on Jsch
     *
     * @param logger logger
     * @param loggingLevel level
     */
    private void setThreadLogger(PluginLogger logger, int loggingLevel) {
        pluginLogger.set(logger);
        logLevel.set(loggingLevel);
        JSch.setLogger(this);
    }

    public boolean isEnabled(int level) {
        Integer integer = logLevel.get();
        return null != integer && integer <= level;
    }

    public void log(int level, String message) {
        BaseLogger baseLogger1 = baseLogger.get();
        PluginLogger pluginLogger1 = pluginLogger.get();
        if (null != baseLogger1) {
            switch (level) {
                case Logger.FATAL:
                case Logger.ERROR:
                    baseLogger1.error(message);
                    break;
                case Logger.WARN:
                    baseLogger1.warn(message);
                    break;
                case Logger.DEBUG:
                    baseLogger1.debug(message);
                    break;
                case Logger.INFO:
                default:
                    baseLogger1.log(message);
                    break;
            }
        }
        if (null != pluginLogger1) {
            switch (level) {
                case Logger.FATAL:
                case Logger.ERROR:
                    pluginLogger1.log(0, message);
                    break;
                case Logger.WARN:
                    pluginLogger1.log(1, message);
                    break;
                case Logger.DEBUG:
                    pluginLogger1.log(5, message);
                    break;
                case Logger.INFO:
                default:
                    pluginLogger1.log(5, message);
                    break;
            }
        }
    }
}

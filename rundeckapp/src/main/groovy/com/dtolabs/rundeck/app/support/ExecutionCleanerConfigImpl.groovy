package com.dtolabs.rundeck.app.support

import groovy.transform.CompileStatic

/**
 * Implementation of ExecutionCleanerConfig
 */
@CompileStatic
class ExecutionCleanerConfigImpl implements ExecutionCleanerConfig {
    boolean enabled
    int maxDaysToKeep
    String cronExpression
    int minimumExecutionToKeep
    int maximumDeletionSize

    /**
     * Build with a closure
     * @param clos
     * @return
     */
    static ExecutionCleanerConfig build(@DelegatesTo(Builder) Closure clos){
        def builder = new Builder()
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_FIRST
        clos.call()
        return builder.build()
    }

    @CompileStatic
    static class Builder {
        ExecutionCleanerConfigImpl impl = new ExecutionCleanerConfigImpl()

        Builder enabled(boolean val) {
            impl.enabled = val
            return this
        }

        Builder maxDaysToKeep(int val) {
            impl.maxDaysToKeep = val
            return this
        }

        Builder minimumExecutionToKeep(int val) {
            impl.minimumExecutionToKeep = val
            return this
        }

        Builder maximumDeletionSize(int val) {
            impl.maximumDeletionSize = val
            return this
        }

        Builder cronExpression(String val) {
            impl.cronExpression = val
            return this
        }

        ExecutionCleanerConfigImpl build() {
            return impl;
        }
    }
}

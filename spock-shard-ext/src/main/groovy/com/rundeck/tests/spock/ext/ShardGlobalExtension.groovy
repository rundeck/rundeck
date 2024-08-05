package com.rundeck.tests.spock.ext

import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo

/**
 * Global extension to process all specs,
 * will apply sharding to all specs only if the global flag is set.
 */
@CompileStatic
class ShardGlobalExtension implements IGlobalExtension {
    private ShardConfig config

    ShardGlobalExtension(final ShardConfig config) {
        this.config = config
    }

    @Override
    void start() {
        config.validate()
    }

    @Override
    void visitSpec(final SpecInfo spec) {
        config.processSpec(spec, false)
    }
}

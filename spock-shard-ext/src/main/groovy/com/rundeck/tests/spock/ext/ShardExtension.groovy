package com.rundeck.tests.spock.ext

import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.IAnnotationDrivenExtension
import org.spockframework.runtime.model.SpecInfo

/**
 * Extension to process the Shard annotation, the sharding will
 * apply to Specs annotated with {@link Shard}, but may optionally
 * apply to all specs if the global flag is set.
 */
@CompileStatic
class ShardExtension implements IAnnotationDrivenExtension<Shard> {
    ShardConfig config

    ShardExtension(final ShardConfig config) {
        this.config = config
    }

    @Override
    void visitSpecAnnotation(final Shard annotation, final SpecInfo spec) {
        config.validate()
        config.processSpec(spec, true)
    }

}
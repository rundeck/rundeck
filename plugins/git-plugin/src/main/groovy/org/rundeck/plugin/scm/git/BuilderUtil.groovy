package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.core.plugins.views.BasicInputViewBuilder
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder

/**
 * Groovy helpers for builder
 */
class BuilderUtil {

    static Description pluginDescription(@DelegatesTo(DescriptionBuilder) Closure clos) {
        def builder = DescriptionBuilder.builder()
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_FIRST
        clos.call()
        return builder.build()
    }

    static BasicInputView inputView(String id, @DelegatesTo(BasicInputViewBuilder) Closure clos) {
        def builder = BasicInputViewBuilder.forActionId(id)
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_FIRST
        clos.call()
        return builder.build()
    }

    static Property property(@DelegatesTo(PropertyBuilder) Closure clos) {
        def builder = PropertyBuilder.builder()
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_FIRST
        clos.call()
        return builder.build()
    }
    static Property property(Property orig,@DelegatesTo(PropertyBuilder) Closure clos) {
        def builder = PropertyBuilder.builder(orig)
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_FIRST
        clos.call()
        return builder.build()
    }
}

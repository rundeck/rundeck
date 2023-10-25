package org.rundeck.util.extensions

import org.spockframework.runtime.AbstractRunListener
import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo

class TestResultExtension implements IGlobalExtension {

    @Override
    void visitSpec(SpecInfo spec) {
        spec.addListener(new ErrorListener())
    }

    static class ErrorListener extends AbstractRunListener {
        ErrorInfo errorInfo

        @Override
        void beforeIteration(IterationInfo iteration) {
            errorInfo = null
        }

        @Override
        void error(ErrorInfo error) {
            errorInfo = error
        }
    }
}
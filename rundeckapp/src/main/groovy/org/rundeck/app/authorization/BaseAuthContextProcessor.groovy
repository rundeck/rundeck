package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import groovy.transform.CompileStatic

/**
 * Combines AuthContextProvider/Evaluator beans
 */
@CompileStatic
class BaseAuthContextProcessor implements AppAuthContextProcessor {
    @Delegate
    AuthContextProvider rundeckAuthContextProvider
    @Delegate
    AppAuthContextEvaluator rundeckAuthContextEvaluator
}

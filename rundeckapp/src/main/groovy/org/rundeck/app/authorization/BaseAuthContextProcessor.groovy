package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import groovy.transform.CompileStatic

/**
 * Combines AuthContextProvider/Evaluator beans
 */
@CompileStatic
class BaseAuthContextProcessor implements AuthContextProcessor {
    @Delegate
    AuthContextProvider rundeckAuthContextProvider
    @Delegate
    AuthContextEvaluator rundeckAuthContextEvaluator
}

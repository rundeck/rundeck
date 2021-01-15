package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor

/**
 * Extend AuthContextProcessor with AppAuthContextEvaluator
 */
interface AppAuthContextProcessor extends AuthContextProcessor, AppAuthContextEvaluator{

}

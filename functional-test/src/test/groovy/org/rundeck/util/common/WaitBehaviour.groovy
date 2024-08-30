package org.rundeck.util.common

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString

import java.time.Duration

@CompileStatic
trait WaitBehaviour {

    /** Waits for the resource to be in the expected state by retrieving it and evaluating its acceptance as many times as needed.
     * @param retryableResourceRetriever executed multiple times to retrieve the current state of the R resource.
     * @param resourceAcceptanceEvaluator evaluates the state of the resource and and returns true if the resource is accepted.
     * @param timeout max duration to wait for the resource to reach the expected state.
     * @param checkPeriod time to wait between each check.
     * @return the resource that met the acceptance criteria.
     * @throws ResourceAcceptanceTimeoutException if the timeout is reached waiting for the resource to reach the expected state.
     *
     * @see WaitUtils#waitFor(Closure, Closure, Duration, Duration) for the static version of this method.
     */
    final  <R> R waitFor(
            Closure<R> retryableResourceRetriever,
            @ClosureParams(value = FromString, options = ["R"]) Closure<Boolean> resourceAcceptanceEvaluator = { !!it },
            Duration timeout = WaitingTime.MODERATE,
            Duration checkPeriod = WaitingTime.LOW) {
        WaitUtils.waitFor(retryableResourceRetriever, resourceAcceptanceEvaluator, timeout, checkPeriod)
    }

    /**
     * Converts a closure that does verification on a single resource to a closure that does the verification on a collection of such resources.
     * @param verifier closure that verifies a single resource.
     * @return closure that takes a collection of resources and runs the verifier on every resource, short-circuiting if needed.
     */
    final Closure<Boolean> verifyForAll(Closure<Boolean> verifier) {
        { Collection<?> coll ->
            coll?.every(verifier)
        }
    }
}

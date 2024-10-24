package com.dtolabs.rundeck.core.utils;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public class WaitUtils {

    /**
     * Waits for the resource to be in the expected state by retrieving it and evaluating its acceptance as many times as needed.
     *
     * @param retryableResourceRetriever  executed multiple times to retrieve the current state of the R resource.
     * @param resourceAcceptanceEvaluator evaluates the state of the resource and  returns true if the resource is accepted.
     * @param timeout  max duration to wait for the resource to reach the expected state.
     * @param checkPeriod time to wait between each check.
     * @return the resource that met the acceptance criteria.
     * @throws ResourceAcceptanceTimeoutException if the timeout is reached waiting for the resource to reach the expected state.
     * @throws InterruptedException if the timeout is reached waiting for the resource to reach the expected state.
     */
    public static <R> R waitFor(
            Supplier<R> retryableResourceRetriever,
            Function<R, Boolean> resourceAcceptanceEvaluator,
            Duration timeout,
            Duration checkPeriod) throws InterruptedException {
        R r = retryableResourceRetriever.get();
        Boolean acceptanceResult = resourceAcceptanceEvaluator.apply(r);
        long initTime = System.currentTimeMillis();
        while (!acceptanceResult) {
            Thread.sleep(Math.min(checkPeriod.toMillis(), timeout.toMillis()));
            if ((System.currentTimeMillis() - initTime) >= timeout.toMillis()) {
                throw new ResourceAcceptanceTimeoutException("Timeout reached (" + timeout.toMillis() + "ms) waiting for value: " + r + " to reach the desired state");
            }
            r = retryableResourceRetriever.get();
            acceptanceResult = resourceAcceptanceEvaluator.apply(r);
        }
        return r;
    }
}

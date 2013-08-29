package org.grails.plugins.metricsweb

import com.codahale.metrics.Gauge

import java.util.concurrent.Callable

/**
 * Easy way to create a gauge using a closure: new CallableGauge&lt;X&gt;({ x })
 */
class CallableGauge<T> implements Gauge<T>{
    Callable<T> callable

    CallableGauge(Callable<T> callable) {
        this.callable = callable
    }

    @Override
    T getValue() {
        return callable.call()
    }
}

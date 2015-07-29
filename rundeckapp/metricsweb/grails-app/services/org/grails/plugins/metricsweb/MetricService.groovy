package org.grails.plugins.metricsweb

import com.codahale.metrics.Counter
import com.codahale.metrics.Metric
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import org.codehaus.groovy.reflection.ReflectionUtils

class MetricService {
    static transactional = false
    def metricRegistry


    def Metric meter(String classname, String metricName) {
        metricRegistry.meter(MetricRegistry.name(classname, metricName))
    }

    def Timer timer(String classname, String metricName) {
        metricRegistry.timer(MetricRegistry.name(classname, metricName))
    }

    def Counter counter(String classname, String metricName) {
        metricRegistry.counter(MetricRegistry.name(classname, metricName))
    }

    def markMeter(String classname, String metricName) {
        meter(classname, metricName).mark()
    }

    def incCounter(String classname, String metricName) {
        counter(classname, metricName)?.inc()
    }


    def decCounter(String classname, String metricName) {
        counter(classname, metricName)?.dec()
    }


    def withTimer(String classname, String name, Closure clos) {
        timer(classname, name).time(clos)
    }


}

package org.grails.plugins.metricsweb

import com.codahale.metrics.Counter
import com.codahale.metrics.Metric
import com.codahale.metrics.MetricRegistry
import org.codehaus.groovy.reflection.ReflectionUtils

class MetricService {
    static transactional = false
    def metricRegistry

    def Metric meter(String metricName) {
        meter(ReflectionUtils.getCallingClass(0).name, metricName)
    }

    def Metric meter(String classname, String metricName) {
        metricRegistry.meter(MetricRegistry.name(classname, metricName))
    }

    def Timer timer(String classname, String metricName) {
        metricRegistry.timer(MetricRegistry.name(classname, metricName))
    }

    def Timer timer(String metricName) {
        timer(ReflectionUtils.getCallingClass(0).name, metricName)
    }

    def Counter counter(String classname, String metricName) {
        metricRegistry.counter(MetricRegistry.name(classname, metricName))
    }

    def markMeter(String metricName) {
        markMeter(ReflectionUtils.getCallingClass(0).name, metricName)
    }

    def markMeter(String classname, String metricName) {
        meter(classname, metricName).mark()
    }

    def incCounter(String classname, String metricName) {
        counter(classname, metricName)?.inc()
    }

    def incCounter(String metricName) {
        incCounter(ReflectionUtils.getCallingClass(0).name, metricName)
    }

    def decCounter(String classname, String metricName) {
        counter(classname, metricName)?.dec()
    }

    def decCounter(String metricName) {
        decCounter(ReflectionUtils.getCallingClass(0).name, metricName)
    }

    def withTimer(String classname, String name, Closure clos) {
        timer(classname, name).time(clos)
    }

    def withTimer(String name, Closure clos) {
        withTimer(ReflectionUtils.getCallingClass(0).name, name, clos)
    }

    def withTimer(Closure clos) {
        withTimer(ReflectionUtils.getCallingClass(0).name, 'Timer', clos)
    }

}

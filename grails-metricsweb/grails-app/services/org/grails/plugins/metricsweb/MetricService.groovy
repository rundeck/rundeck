/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.plugins.metricsweb

import com.codahale.metrics.Counter
import com.codahale.metrics.Histogram
import com.codahale.metrics.Meter
import com.codahale.metrics.Metric
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import grails.compiler.GrailsCompileStatic
import org.codehaus.groovy.reflection.ReflectionUtils

import java.util.concurrent.Callable

@GrailsCompileStatic
class MetricService {
    static transactional = false
    MetricRegistry metricRegistry


    Meter meter(String classname, String metricName) {
        metricRegistry.meter(MetricRegistry.name(classname, metricName))
    }

    def Timer timer(String classname, String metricName) {
        metricRegistry.timer(MetricRegistry.name(classname, metricName))
    }

    def Counter counter(String classname, String metricName) {
        metricRegistry.counter(MetricRegistry.name(classname, metricName))
    }

    def Histogram histogram(String classname, String metricName) {
        metricRegistry.histogram(MetricRegistry.name(classname, metricName))
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

    public <T> T withTimer(String classname, String name, Callable<T> clos) {
        return timer(classname, name).time((Callable<T>)clos)
    }
}

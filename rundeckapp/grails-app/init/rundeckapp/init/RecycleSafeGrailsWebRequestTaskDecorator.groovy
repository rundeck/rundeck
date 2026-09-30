/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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
package rundeckapp.init

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils
import org.springframework.core.task.TaskDecorator
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

/**
 * Propagates the current Grails web request onto async tasks, tolerating a recycled request.
 *
 * Replaces org.grails.plugins.web.async.GrailsWebRequestTaskDecorator, which rebuilds the
 * GrailsWebRequest on the worker thread from the request captured at submit time:
 *
 *     GrailsWebRequest taskRequest = new GrailsWebRequest(
 *             captured.currentRequest, captured.currentResponse, captured.attributes)
 *     WebUtils.storeGrailsWebRequest(taskRequest)
 *     try { task.run() } finally { ... }
 *
 * On Jetty 12 EE11 the container recycles the ServletApiRequest once the originating request
 * completes, so ServletApiRequest.getRequest() returns null and that constructor throws inside
 * GrailsWebRequest.inheritEncodingStateRegistry. The construction sits *before* the try, so the
 * exception escapes the Runnable: the submitted task never runs and the pool worker dies. The
 * symptom is a steady drip of
 *
 *     Exception in thread "grails-promise-N" java.lang.NullPointerException:
 *       Cannot invoke "org.eclipse.jetty.server.Request.getAttribute(String)" because the return
 *       value of "org.eclipse.jetty.ee11.servlet.ServletApiRequest.getRequest()" is null
 *
 * with the thread number climbing as the pool replaces the threads it lost. Grails events dispatch
 * on this same executor -- EventBusFactoryBean builds the ExecutorEventBus from grailsPromiseFactory
 * -- so dropped work is not limited to explicit Promises.
 *
 * Here the rebuild is attempted and, when it fails, the task runs without a web request bound
 * rather than not at all. Work that genuinely needs the request was already losing it in this case;
 * the difference is that it now fails in the task, where it can be seen, instead of taking the
 * thread down silently.
 */
@Slf4j
@CompileStatic
class RecycleSafeGrailsWebRequestTaskDecorator implements TaskDecorator {

    /** Attribute name used only to ask the originating request whether it is still alive. */
    private static final String RECYCLE_PROBE_ATTRIBUTE =
            'rundeck.taskDecorator.recycleProbe'

    @Override
    Runnable decorate(Runnable task) {
        GrailsWebRequest captured = GrailsWebRequest.lookup()
        if (captured == null) {
            return task
        }
        return new Runnable() {
            @Override
            void run() {
                RequestAttributes previous = RequestContextHolder.getRequestAttributes()
                GrailsWebRequest taskRequest = null
                try {
                    // Probe the originating request before binding it. The constructor below does
                    // not touch the parts Jetty recycles, so on a dead request it still succeeds
                    // and the failure surfaces much later and much deeper -- DefaultLinkGenerator
                    // reading an attribute from inside a project deletion, where it aborts the
                    // delete of an execution, the foreign key then blocks the job, and the project
                    // is left stuck in "disabled or being deleted" for the rest of the run. This is
                    // the same call that fails there, made where it can still be handled.
                    captured.currentRequest.getAttribute(RECYCLE_PROBE_ATTRIBUTE)
                    taskRequest = new GrailsWebRequest(
                            captured.currentRequest,
                            captured.currentResponse,
                            captured.attributes)
                } catch (Exception e) {
                    // The originating request has been recycled by the container. Debug, not warn:
                    // for a task submitted from a request thread and run after the response
                    // completed this is the norm, not a fault, and it would otherwise be noisy.
                    log.debug("Originating request no longer available, running task without one", e)
                }
                if (taskRequest == null) {
                    task.run()
                    return
                }
                WebUtils.storeGrailsWebRequest(taskRequest)
                try {
                    task.run()
                }
                finally {
                    if (previous == null) {
                        RequestContextHolder.resetRequestAttributes()
                    }
                    else {
                        RequestContextHolder.setRequestAttributes(previous)
                    }
                }
            }
        }
    }
}

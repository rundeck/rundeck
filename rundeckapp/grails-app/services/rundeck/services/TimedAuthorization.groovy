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

package rundeck.services

import com.codahale.metrics.Meter
import com.codahale.metrics.Timer
import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.Decision

import javax.security.auth.Subject

/**
 * Created by greg on 7/29/15.
 */
class TimedAuthorization implements Authorization {
    Authorization authorization
    Timer evaluateTimer
    Timer evaluateSetTimer
    Meter evaluateMeter
    Meter evaluateSetMeter

    TimedAuthorization(
            final Authorization authorization,
            final Timer evaluateTimer,
            final Timer evaluateSetTimer,
            final Meter evaluateMeter,
            final Meter evaluateSetMeter
    )
    {
        this.authorization = authorization
        this.evaluateTimer = evaluateTimer
        this.evaluateSetTimer = evaluateSetTimer
        this.evaluateMeter = evaluateMeter
        this.evaluateSetMeter = evaluateSetMeter
    }

    @Override
    Decision evaluate(
            final Map<String, String> resource,
            final Subject subject,
            final String action,
            final Set<Attribute> environment
    )
    {
        evaluateMeter.mark()
        evaluateTimer.time{
            authorization.evaluate(resource,subject,action,environment)
        }
    }

    @Override
    Set<Decision> evaluate(
            final Set<Map<String, String>> resources,
            final Subject subject,
            final Set<String> actions,
            final Set<Attribute> environment
    )
    {
        evaluateSetMeter.mark()
        evaluateSetTimer.time {
            authorization.evaluate(resources, subject, actions, environment)
        }
    }
}

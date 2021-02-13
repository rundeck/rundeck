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

import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.Explanation
import grails.test.hibernate.HibernateSpec
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.authorization.AppAuthContextEvaluator
import rundeck.ScheduledExecution
import spock.lang.Specification

import javax.security.auth.Subject

class ReportServiceSpec extends HibernateSpec implements ServiceUnitTest<ReportService> {
    List<Class> getDomainClasses() { [ScheduledExecution] }

    def "executions history authorizations"(){
        given:
            def job1Resource = ['kind': 'job', 'group': 'agroup1', 'name': 'aname1']
            def job2Resource = ['kind': 'job', 'group': 'agroup2', 'name': 'aname2']
            def setDecision = new HashSet<Decision>()

            setDecision<< newDecisionInstance(Explanation.Code.GRANTED, true, job1Resource, 'view')
            setDecision<< newDecisionInstance(Explanation.Code.REJECTED, false, job1Resource, 'read')
            setDecision<< newDecisionInstance(Explanation.Code.REJECTED, false, job1Resource, 'view_history')
            //job 2
            setDecision<< newDecisionInstance(Explanation.Code.GRANTED, true, job2Resource, 'view')
            setDecision<< newDecisionInstance(Explanation.Code.GRANTED, true, job2Resource, 'read')
            setDecision<< newDecisionInstance(Explanation.Code.REJECTED_DENIED, false, job2Resource, 'view_history')

            def authContext = Mock(AuthContext)
            service.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                authorizeProjectResources(_, _, _, _) >> setDecision

            }
        when:
            def result = service.jobHistoryAuthorizations(authContext, 'aProject')

        then:
            result[ReportService.DENIED_VIEW_HISTORY_JOBS] == ['agroup2/aname2']

    }

    private Decision newDecisionInstance(
            Explanation.Code explanation,
            boolean authorized,
            Map<String, String> resource,
            String action
    ) {
        newDecisionInstance(newInstanceExplanation(explanation), authorized, resource, action)
    }

    private Decision newDecisionInstance(
            Explanation explanation,
            boolean authorized,
            Map<String, String> resource,
            String action
    ) {
        return new Decision() {
            private String representation

            public boolean isAuthorized() {
                return authorized
            }

            public Map<String, String> getResource() {
                return resource
            }

            public String getAction() {
                return action;
            }

            public Set<Attribute> getEnvironment() {
                return null
            }

            public Subject getSubject() {
                return null
            }

            public String toString() {
                if (representation == null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Decision for: ");
                    builder.append("res<");
                    Iterator<Map.Entry<String, String>> riter = resource.entrySet().iterator();
                    while (riter.hasNext()) {
                        Map.Entry<String, String> s = riter.next();
                        builder.append(s.getKey()).append(':').append(s.getValue());
                        if (riter.hasNext()) {
                            builder.append(", ")
                        }
                    }

                    builder.append("> subject<")

                    builder.append("> action<")
                    builder.append("> env<")
                    builder.append(">")
                    builder.append(": authorized: ")
                    builder.append(isAuthorized())
                    builder.append(": ")
                    builder.append(explanation.toString())

                    this.representation = builder.toString()
                }
                return this.representation;
            }

            public Explanation explain() {
                return explanation
            }

            public long evaluationDuration() {
                return 0
            }
        };
    }

    Explanation newInstanceExplanation(Explanation.Code reasonId) {
        new Explanation() {

            public Explanation.Code getCode() {
                return reasonId
            }

            public void describe(PrintStream out) {
                out.println(toString())
            }

            public String toString() {
                return "\t" + "some reason => " + reasonId
            }
        }
    }
}


/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */
 
/*
 * ExecutionQuery.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 9/10/12 3:49 PM
 * 
 */
class ExecutionQuery extends ScheduledExecutionQuery{

    String statusFilter
    String abortedbyFilter
    List<String> jobListFilter
    List<String> jobIdListFilter
    List<String> excludeJobListFilter
    List<String> excludeJobIdListFilter
    Date startafterFilter
    Date startbeforeFilter
    Date endafterFilter
    Date endbeforeFilter
    Boolean adhoc
    boolean dostartafterFilter
    boolean dostartbeforeFilter
    boolean doendafterFilter
    boolean doendbeforeFilter
    String recentFilter
    String userFilter

    static constraints={
        statusFilter(inList: [ExecutionController.EXECUTION_RUNNING, ExecutionController.EXECUTION_ABORTED, ExecutionController.EXECUTION_FAILED, ExecutionController.EXECUTION_SUCCEEDED])
    }


    public void configureFilter() {
        super.configureFilter()
        if (recentFilter) {
            Calendar n = GregorianCalendar.getInstance()
            def matcher = recentFilter =~ /^(\d+)([hdwmy])$/
            if (matcher.matches()) {
                def i = matcher.group(1).toInteger()
                def ndx
                switch (matcher.group(2)) {
                    case 'h':
                        ndx = Calendar.HOUR_OF_DAY
                        break
                    case 'd':
                        ndx = Calendar.DAY_OF_YEAR
                        break
                    case 'w':
                        ndx = Calendar.WEEK_OF_YEAR
                        break
                    case 'm':
                        ndx = Calendar.MONTH
                        break
                    case 'y':
                        ndx = Calendar.YEAR
                        break
                }
                n.add(ndx, -1 * i)

                doendafterFilter = true
                endafterFilter = n.getTime()

                doendbeforeFilter = false
                dostartafterFilter = false
                dostartbeforeFilter = false
            } else {
                if (!doendbeforeFilter && !dostartafterFilter && !dostartbeforeFilter && !doendafterFilter) {
                    recentFilter = null
                }
            }
        }
    }
}

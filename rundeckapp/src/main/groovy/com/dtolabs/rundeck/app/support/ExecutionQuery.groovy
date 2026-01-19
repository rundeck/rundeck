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

package com.dtolabs.rundeck.app.support

import com.google.common.collect.Lists
import grails.gorm.DetachedCriteria
import grails.validation.Validateable
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.ExecReport
import rundeck.services.ExecutionService

/*
 * ExecutionQuery.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 9/10/12 3:49 PM
 *
 */
class ExecutionQuery extends ScheduledExecutionQuery implements Validateable{

    String statusFilter
    String abortedbyFilter
    List<String> jobListFilter
    List<String> jobIdListFilter
    List<String> excludeJobListFilter
    List<String> excludeJobIdListFilter
    String excludeJobFilter
    String excludeJobExactFilter
    String excludeGroupPath
    String excludeGroupPathExact
    Date startafterFilter
    Date startbeforeFilter
    Date endafterFilter
    Date endbeforeFilter
    Boolean adhoc
    boolean dostartafterFilter
    boolean dostartbeforeFilter
    boolean doendafterFilter
    boolean doendbeforeFilter
    boolean includeJobRef
    List<String> execProjects
    String recentFilter
    String userFilter
    String executionTypeFilter
    String adhocStringFilter
    String nodeFilter
    String optionFilter

    boolean excludeRunning = false


    static constraints={
        statusFilter(nullable:true)
        sortOrder(nullable: true)
        loglevelFilter(nullable:true)
        excludeJobIdListFilter(nullable:true)
        recentFilter(nullable:true)
        jobListFilter(nullable:true)
        startafterFilter(nullable:true)
        jobFilter(nullable:true)
        jobExactFilter(nullable:true)
        idlist(nullable:true)
        projFilter(nullable:true)
        userFilter(nullable:true)
        excludeGroupPath(nullable:true)
        excludeJobFilter(nullable:true)
        endafterFilter(nullable:true)
        excludeGroupPathExact(nullable:true)
        groupPath(nullable:true)
        descFilter(nullable:true)
        excludeJobExactFilter(nullable:true)
        sortBy(nullable:true)
        jobIdListFilter(nullable:true)
        endbeforeFilter(nullable:true)
        groupPathExact(nullable:true)
        startbeforeFilter(nullable:true)
        excludeJobListFilter(nullable:true)
        abortedbyFilter(nullable:true)
        adhoc(nullable:true)
        executionTypeFilter( nullable: true)
        execProjects(nullable:true)
        adhocStringFilter(nullable:true)
        nodeFilter(nullable:true)
        optionFilter(nullable:true)
    }
    /**
     * Modify a date by rewinding a certain number of units
     * @param recentFilter string matching "\d+[hdwmyns]"
     * @param date date to start with, or null indicating now
     * @return modified date, or null if format was not matched
     */
    public static Date parseRelativeDate(String recentFilter, Date date=null){
        Calendar n = GregorianCalendar.getInstance()
        n.setTime(date?:new Date())
        def matcher = recentFilter =~ /^(\d+)([hdwmyns])$/
        if (matcher.matches()) {
            def i = matcher.group(1).toInteger()
            def ndx
            switch (matcher.group(2)) {
                case 'h':
                    ndx = Calendar.HOUR_OF_DAY
                    break
                case 'n':
                    ndx = Calendar.MINUTE
                    break
                case 's':
                    ndx = Calendar.SECOND
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

            return n.getTime()
        }
        null
    }

    public void configureFilter() {
        super.configureFilter()
        if (recentFilter) {
            Date recentDate=parseRelativeDate(recentFilter)
            if (null!=recentDate) {
                doendafterFilter = true
                endafterFilter = recentDate

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


  /**
   * Creates a criteria closure for this query.
   * @param customDelegate sets a custom closure delegate. If not defined the default will be used.
   * @return A closure to run this query within a criteria of Execution
   */
  def createCriteria(def customDelegate = null, jobQueryComponents = null) {

      def query = this
      def state = query.statusFilter
      def txtfilters = ScheduledExecutionQuery.TEXT_FILTERS
      def eqfilters = ScheduledExecutionQuery.EQ_FILTERS
      def boolfilters = ScheduledExecutionQuery.BOOL_FILTERS
      def filters = ScheduledExecutionQuery.ALL_FILTERS
      def excludeTxtFilters = ['excludeJob': 'jobName']
      def excludeEqFilters = ['excludeJobExact': 'jobName']

      def jobqueryfilters = ['jobListFilter', 'jobIdListFilter', 'excludeJobListFilter', 'excludeJobIdListFilter', 'jobFilter', 'jobExactFilter', 'groupPath', 'groupPathExact', 'descFilter', 'excludeGroupPath', 'excludeGroupPathExact', 'excludeJobFilter', 'excludeJobExactFilter']

      def convertids = { String s ->
        try {
          return Long.valueOf(s)
        } catch (NumberFormatException e) {
          return s
        }
      }
      def idlist = query.jobIdListFilter?.collect(convertids)
      def xidlist = query.excludeJobIdListFilter?.collect(convertids)

      def hasJobFilters = jobqueryfilters.any { query[it] }
      if (hasJobFilters && query.adhoc) {
        throw new ExecutionQueryException('api.executions.jobfilter.adhoc.conflict');
      }
      def criteriaClos = { //isCount ->
        if (query.adhoc) {
          isNull('scheduledExecution')
        } else if (null != query.adhoc || hasJobFilters) {
          isNotNull('scheduledExecution')
        }
        if (!query.adhoc && hasJobFilters) {
          delegate.'scheduledExecution' {
            //begin related ScheduledExecution query
            if (idlist) {
              def idq = {
                idlist.each { theid ->
                  if (theid instanceof Long) {
                    eq("id", theid)
                  } else {
                      if(query.includeJobRef && query.execProjects){
                          or {
                              eq("uuid", theid)
                              exists(new DetachedCriteria(ReferencedExecution, "re").build {
                                  projections { property 're.execution.id' }
                                  eq('re.jobUuid', theid)
                                  eqProperty('re.execution.id', 'this.id')
                                  List execProjectsPartitioned = Lists.partition(query.execProjects, 1000)
                                  or {
                                      for (def partition : execProjectsPartitioned) {
                                          'in'('this.project', partition)
                                      }
                                  }
                              })

                          }
                      }else{
                          eq("uuid", theid)
                      }
                  }
                }
              }
              if (idlist.size() > 1) {

                or {
                  idq.delegate = delegate
                  idq()
                }
              } else {
                idq.delegate = delegate
                idq()
              }
            }
            if (xidlist) {
              not {
                xidlist.each { theid ->
                  if (theid instanceof Long) {
                    eq("id", theid)
                  } else {
                    eq("uuid", theid)
                  }
                }
              }
            }
            if (query.jobListFilter || query.excludeJobListFilter) {
              if (query.jobListFilter) {
                or {
                  query.jobListFilter.each {
                    def z = it.split("/") as List
                    if (z.size() > 1) {
                      and {
                        eq('jobName', z.removeLast())
                        eq('groupPath', z.join("/"))
                      }
                    } else {
                      and {
                        eq('jobName', z.removeLast())
                        or {
                          eq('groupPath', "")
                          isNull('groupPath')
                        }
                      }
                    }
                  }
                }
              }
              if (query.excludeJobListFilter) {
                not {
                  or {
                    query.excludeJobListFilter.each {
                      def z = it.split("/") as List
                      if (z.size() > 1) {
                        and {
                          eq('jobName', z.removeLast())
                          eq('groupPath', z.join("/"))
                        }
                      } else {
                        and {
                          eq('jobName', z.removeLast())
                          or {
                            eq('groupPath', "")
                            isNull('groupPath')
                          }
                        }
                      }
                    }

                  }
                }
              }
            }

            txtfilters.each { key, val ->
              if (query["${key}Filter"]) {
                ilike(val, '%' + query["${key}Filter"] + '%')
              }
            }

            eqfilters.each { key, val ->
              if (query["${key}Filter"]) {
                eq(val, query["${key}Filter"])
              }
            }
            boolfilters.each { key, val ->
              if (null != query["${key}Filter"]) {
                eq(val, query["${key}Filter"])
              }
            }
            excludeTxtFilters.each { key, val ->
              if (query["${key}Filter"]) {
                not {
                  ilike(val, '%' + query["${key}Filter"] + '%')
                }
              }
            }
            excludeEqFilters.each { key, val ->
              if (query["${key}Filter"]) {
                not {
                  eq(val, query["${key}Filter"])
                }
              }
            }


            if ('-' == query['groupPath']) {
              or {
                eq("groupPath", "")
                isNull("groupPath")
              }
            } else if (query["groupPath"] && '*' != query["groupPath"]) {
              or {
                ilike("groupPath", query["groupPath"] + "/%")
                eq("groupPath", query['groupPath'])
              }
            }
            if ('-' == query['excludeGroupPath']) {
              not {
                or {
                  eq("groupPath", "")
                  isNull("groupPath")
                }
              }
            } else if (query["excludeGroupPath"]) {
              not {
                or {
                  ilike("groupPath", query["excludeGroupPath"] + "/%")
                  eq("groupPath", query['excludeGroupPath'])
                }
              }
            }
            if (query["groupPathExact"]) {
              if ("-" == query["groupPathExact"]) {
                or {
                  eq("groupPath", "")
                  isNull("groupPath")
                }
              } else {
                eq("groupPath", query['groupPathExact'])
              }
            }
            if (query["excludeGroupPathExact"]) {
              if ("-" == query["excludeGroupPathExact"]) {
                not {
                  or {
                    eq("groupPath", "")
                    isNull("groupPath")
                  }
                }
              } else {
                or {
                  ne("groupPath", query['excludeGroupPathExact'])
                  isNull("groupPath")
                }
              }
            }

            //end related ScheduledExecution query
          }
        }
        // project filter is not applied when the jobUUID filter is set to improve the response time of that case
        if(query.projFilter && !query.jobIdListFilter) {
          eq('project', query.projFilter)

          if(excludeRunning){
              isNotNull('dateCompleted')
          }
        }
        if (query.userFilter) {
          eq('user', query.userFilter)
        }
        if (state == ExecutionService.EXECUTION_RUNNING) {
          isNull('dateCompleted')
          or {
            isNull('status')
            and{
              ne('status', ExecutionService.EXECUTION_SCHEDULED)
              ne('status', ExecutionService.EXECUTION_QUEUED)
            }
          }
        } else if (state == ExecutionService.EXECUTION_SCHEDULED) {
          eq('status', ExecutionService.EXECUTION_SCHEDULED)
        } else if (state == ExecutionService.EXECUTION_QUEUED) {
          eq('status', ExecutionService.EXECUTION_QUEUED)
        } else if (state == ExecutionService.EXECUTION_ABORTED) {
          isNotNull('dateCompleted')
          eq('cancelled', true)
        } else if (state == ExecutionService.EXECUTION_TIMEDOUT) {
          isNotNull('dateCompleted')
          eq('timedOut', true)
        }else if (state == ExecutionService.EXECUTION_FAILED_WITH_RETRY) {
          isNotNull('dateCompleted')
          eq('willRetry', true)
        } else if(state == ExecutionService.EXECUTION_FAILED){
          eq('status',  'failed')
          eq('cancelled', false)
          isNotNull('dateCompleted')
        }else if(state == ExecutionService.EXECUTION_SUCCEEDED){
          eq('status',  'succeeded')
          eq('cancelled', false)
          isNotNull('dateCompleted')
        }else if(state){
          eq('status',  state)
        }
        if (query.executionTypeFilter) {
          eq('executionType', query.executionTypeFilter)
        }
        if (query.abortedbyFilter) {
          eq('abortedby', query.abortedbyFilter)
        }
        if (query.dostartafterFilter && query.dostartbeforeFilter && query.startbeforeFilter && query.startafterFilter) {
          between('dateStarted', query.startafterFilter, query.startbeforeFilter)
        } else if (query.dostartbeforeFilter && query.startbeforeFilter) {
          le('dateStarted', query.startbeforeFilter)
        } else if (query.dostartafterFilter && query.startafterFilter) {
          ge('dateStarted', query.startafterFilter)
        }

        if (query.doendafterFilter && query.doendbeforeFilter && query.endafterFilter && query.endbeforeFilter) {
          between('dateCompleted', query.endafterFilter, query.endbeforeFilter)
        } else if (query.doendbeforeFilter && query.endbeforeFilter) {
          le('dateCompleted', query.endbeforeFilter)
        }
        if (query.doendafterFilter && query.endafterFilter) {
          ge('dateCompleted', query.endafterFilter)
        }

        if (query.adhocStringFilter) {
          isNull('scheduledExecution')
          exists(new DetachedCriteria(ExecReport, "er").build {
              projections { property 'er.executionId' }
              eqProperty('er.executionId', 'this.id')
              eqProperty('er.project', 'this.project')
              ilike('er.title', '%' + query.adhocStringFilter + '%')
          })
        }
        if(query.nodeFilter){
          if(query.nodeFilter.startsWith('name:') || !(query.nodeFilter.contains(":") || query.nodeFilter.contains(".*"))){
              def node = query.nodeFilter.startsWith('name:')?(query.nodeFilter.split("name:")[1]).stripIndent():query.nodeFilter;
              or {
                  ilike("failedNodeList", '%' + node + '%')
                  ilike("succeededNodeList", '%' + node + '%')
              }

          }else{
              ilike("filter", '%' + query.nodeFilter + '%')
          }
        }

        if(query.optionFilter){
          ilike('argString', "%${query.optionFilter}%")
        }

        def critDelegate = delegate
        jobQueryComponents?.each { name, jobQuery ->
          jobQuery.extendCriteria(query, [:], critDelegate)
        }

      }

      // Set custom delegate if defined
      if(customDelegate != null) {
        criteriaClos.delegate = customDelegate
      }

      return criteriaClos
    }

    /**
     * Count all executions for a job, including both direct executions and referenced executions
     * Uses a dual-query approach: counts direct executions separately from referenced executions
     * @return total count of direct executions + referenced executions (database-agnostic)
     */
    def countDirectAndReferencedExecutions() {
        def query = this
        def count = 0

        if (query.jobIdListFilter) {
            //this apply when only one job UUID is specified (execution show page or job page)
            String jobUuid = query.jobIdListFilter[0]

            // COUNT QUERY 1: Executions with ReferencedExecution entries (cross-project job references)
            // Uses HQL with INNER JOIN instead of EXISTS for better performance (1900x faster)
            // Counts executions in projects that have referenced this job (via workflow job reference steps)
            // execProjects contains all projects where this job has been referenced, including projFilter
            def hql = """
                        SELECT COUNT(e.id)
                        FROM Execution e
                        INNER JOIN ReferencedExecution re WITH re.execution.id = e.id
                        WHERE e.scheduledExecution IS NOT NULL
                        AND re.jobUuid = :jobUuid
                        AND e.project IN (:projects)
                    """
            def count1 = Execution.executeQuery(hql, [jobUuid: jobUuid, projects: query.execProjects])[0] ?: 0

            // COUNT QUERY 2: Direct executions in the job's own project (without ReferencedExecution)
            // This counts executions that were directly triggered (not via a parent job's workflow step)
            // Note: projFilter may be in execProjects if the job references itself, but such executions
            // will have a ReferencedExecution entry and thus only be counted in COUNT QUERY 1
            def hql2 = """
                        SELECT COUNT(e.id)
                        FROM Execution e
                        WHERE e.project = :project
                        AND e.scheduledExecution IS NOT NULL
                        AND e.jobUuid = :jobUuid
                        AND NOT EXISTS (
                            SELECT 1 FROM ReferencedExecution re
                            WHERE re.execution.id = e.id
                        )
                    """
            def count2 = Execution.executeQuery(hql2, [project: query.projFilter, jobUuid: jobUuid])[0] ?: 0

            count += (count1 as Long) + (count2 as Long)
        }
        return count
    }

    /**
     * Check if this query should use the UNION optimization
     */
    boolean shouldUseUnionQuery() {
        return this.includeJobRef &&
                this.projFilter &&
                this.execProjects &&
                this.jobIdListFilter &&
                this.jobIdListFilter.size() == 1 &&
                !(this.jobIdListFilter[0] instanceof Long)
    }


}

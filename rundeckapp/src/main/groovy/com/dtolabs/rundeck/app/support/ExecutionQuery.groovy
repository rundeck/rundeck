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
    String olderFilter
    String userFilter
    String executionTypeFilter
    String adhocStringFilter
    String nodeFilter
    String optionFilter

    boolean excludeRunning = false
    boolean executionSummary = false


    static constraints={
        statusFilter(nullable:true)
        sortOrder(nullable: true)
        loglevelFilter(nullable:true)
        excludeJobIdListFilter(nullable:true)
        recentFilter(nullable:true)
        olderFilter(nullable:true)
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
     * Escape special LIKE wildcard characters in user input to prevent LIKE pattern injection.
     * This ensures that user-provided %, _, and \ characters are treated as literals.
     * @param input the user input string to escape
     * @return the escaped string safe for use in LIKE patterns
     */
    static String escapeLikePattern(String input) {
        if (input == null) return null
        return input.replace('\\', '\\\\').replace('%', '\\%').replace('_', '\\_')
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
                like("groupPath", query["groupPath"] + "/%")
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
                  like("groupPath", query["excludeGroupPath"] + "/%")
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
        if(query.projFilter) {
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
          isNotNull('dateCompleted')
          eq('cancelled', false)
          eq('status',  'failed')
        }else if(state == ExecutionService.EXECUTION_SUCCEEDED){
          isNotNull('dateCompleted')
          eq('cancelled', false)
          eq('status',  'succeeded')
        }else if(state){
          isNotNull('dateCompleted')
          eq('cancelled', false)
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
          def escapedAdhocString = ExecutionQuery.escapeLikePattern(query.adhocStringFilter)
          exists(new DetachedCriteria(ExecReport, "er").build {
              projections { property 'er.executionId' }
              eqProperty('er.executionId', 'this.id')
              eqProperty('er.project', 'this.project')
              ilike('er.title', '%' + escapedAdhocString + '%')
          })
        }
        if(query.nodeFilter){
          // Check if this is a simple node name filter vs a complex filter expression
          // Simple: "name:nodename" or plain "nodename" (no special chars)
          // Complex: filters with ":" (like "tags:prod") or regex patterns (".*")
          if(query.nodeFilter.startsWith('name:') || !(query.nodeFilter.contains(":") || query.nodeFilter.contains(".*"))){
              def node = query.nodeFilter.startsWith('name:') ? query.nodeFilter.substring(5).trim() : query.nodeFilter
              def escapedNode = ExecutionQuery.escapeLikePattern(node)
              or {
                  ilike("failedNodeList", '%' + escapedNode + '%')
                  ilike("succeededNodeList", '%' + escapedNode + '%')
              }

          }else{
              def escapedFilter = ExecutionQuery.escapeLikePattern(query.nodeFilter)
              ilike("filter", '%' + escapedFilter + '%')
          }
        }

        if(query.optionFilter){
          def escapedOption = ExecutionQuery.escapeLikePattern(query.optionFilter)
          ilike('argString', '%' + escapedOption + '%')
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
     * Count executions using HQL without expensive JOINs to scheduled_execution table.
     * Handles both simple queries and cross-project job reference queries (includeJobRef).
     * 
     * For includeJobRef queries: Uses dual-query approach to count direct executions 
     * and referenced executions separately.
     * 
     * For simple queries: Uses single HQL count on execution table only.
     * 
     * Applies all query filters (status, user, date range, etc.) to ensure accurate counts.
     * @return total execution count
     */
    Long countExecutions() {
        // Build dynamic filter conditions (shared between both approaches)
        def filterConditions = []
        def filterParams = [:]
        def state = this.statusFilter

        // Status filter with all the complex state logic
        if (state == ExecutionService.EXECUTION_RUNNING) {
            filterConditions << "e.dateCompleted IS NULL"
            filterConditions << "(e.status IS NULL OR (e.status != :scheduledStatus AND e.status != :queuedStatus))"
            filterParams.scheduledStatus = ExecutionService.EXECUTION_SCHEDULED
            filterParams.queuedStatus = ExecutionService.EXECUTION_QUEUED
        } else if (state == ExecutionService.EXECUTION_SCHEDULED) {
            filterConditions << "e.status = :status"
            filterParams.status = ExecutionService.EXECUTION_SCHEDULED
        } else if (state == ExecutionService.EXECUTION_QUEUED) {
            filterConditions << "e.status = :status"
            filterParams.status = ExecutionService.EXECUTION_QUEUED
        } else if (state == ExecutionService.EXECUTION_ABORTED) {
            filterConditions << "e.dateCompleted IS NOT NULL"
            filterConditions << "e.cancelled = true"
        } else if (state == ExecutionService.EXECUTION_TIMEDOUT) {
            filterConditions << "e.dateCompleted IS NOT NULL"
            filterConditions << "e.timedOut = true"
        } else if (state == ExecutionService.EXECUTION_FAILED_WITH_RETRY) {
            filterConditions << "e.dateCompleted IS NOT NULL"
            filterConditions << "e.willRetry = true"
        } else if (state == ExecutionService.EXECUTION_FAILED) {
            filterConditions << "e.dateCompleted IS NOT NULL"
            filterConditions << "e.cancelled = false"
            filterConditions << "e.status = :status"
            filterParams.status = 'failed'
        } else if (state == ExecutionService.EXECUTION_SUCCEEDED) {
            filterConditions << "e.dateCompleted IS NOT NULL"
            filterConditions << "e.cancelled = false"
            filterConditions << "e.status = :status"
            filterParams.status = 'succeeded'
        } else if (state) {
            // Custom status
            filterConditions << "e.dateCompleted IS NOT NULL"
            filterConditions << "e.cancelled = false"
            filterConditions << "e.status = :status"
            filterParams.status = state
        }

        // User filter
        if (this.userFilter) {
            filterConditions << "e.user = :userFilter"
            filterParams.userFilter = this.userFilter
        }

        // Execution type filter
        if (this.executionTypeFilter) {
            filterConditions << "e.executionType = :executionTypeFilter"
            filterParams.executionTypeFilter = this.executionTypeFilter
        }

        // Aborted by filter
        if (this.abortedbyFilter) {
            filterConditions << "e.abortedby = :abortedbyFilter"
            filterParams.abortedbyFilter = this.abortedbyFilter
        }

        // Date started filters
        if (this.dostartafterFilter && this.dostartbeforeFilter && this.startbeforeFilter && this.startafterFilter) {
            filterConditions << "e.dateStarted BETWEEN :startafterFilter AND :startbeforeFilter"
            filterParams.startafterFilter = this.startafterFilter
            filterParams.startbeforeFilter = this.startbeforeFilter
        } else if (this.dostartbeforeFilter && this.startbeforeFilter) {
            filterConditions << "e.dateStarted <= :startbeforeFilter"
            filterParams.startbeforeFilter = this.startbeforeFilter
        } else if (this.dostartafterFilter && this.startafterFilter) {
            filterConditions << "e.dateStarted >= :startafterFilter"
            filterParams.startafterFilter = this.startafterFilter
        }

        // Date completed filters
        if (this.doendafterFilter && this.doendbeforeFilter && this.endafterFilter && this.endbeforeFilter) {
            filterConditions << "e.dateCompleted BETWEEN :endafterFilter AND :endbeforeFilter"
            filterParams.endafterFilter = this.endafterFilter
            filterParams.endbeforeFilter = this.endbeforeFilter
        } else if (this.doendbeforeFilter && this.endbeforeFilter) {
            filterConditions << "e.dateCompleted <= :endbeforeFilter"
            filterParams.endbeforeFilter = this.endbeforeFilter
        } else if (this.doendafterFilter && this.endafterFilter) {
            filterConditions << "e.dateCompleted >= :endafterFilter"
            filterParams.endafterFilter = this.endafterFilter
        }

        // Exclude running filter
        if (this.excludeRunning) {
            filterConditions << "e.dateCompleted IS NOT NULL"
        }

        // Build the filter clause string
        def filterClause = filterConditions ? " AND " + filterConditions.join(" AND ") : ""

        // Choose counting strategy based on query type
        if (shouldUseUnionQuery()) {
            // Cross-project job reference query: use dual-query approach
            return countWithJobReferences(filterClause, filterParams)
        } else {
            // Simple query: use single HQL count
            return countSimple(filterClause, filterParams)
        }
    }

    /**
     * Count executions using simple HQL without JOINs.
     * Used for queries that don't need cross-project job references.
     */
    private Long countSimple(String filterClause, Map filterParams) {
        def params = [:]
        if (filterParams) {
            params.putAll(filterParams)
        }

        // Build base HQL
        def hqlParts = ["SELECT COUNT(*) FROM Execution e WHERE 1=1"]

        // Project filter
        if (this.projFilter) {
            hqlParts << "AND e.project = :project"
            params.project = this.projFilter
        }

        // Job UUID filter - uses execution.job_uuid column directly (no JOIN needed)
        if (this.jobIdListFilter) {
            if (this.jobIdListFilter.size() == 1) {
                hqlParts << "AND e.jobUuid = :jobUuid"
                params.jobUuid = this.jobIdListFilter[0].toString()
            } else {
                hqlParts << "AND e.jobUuid IN (:jobUuids)"
                params.jobUuids = this.jobIdListFilter.collect { it.toString() }
            }
        }

        def hql = hqlParts.join(' ') + filterClause
        def result = Execution.executeQuery(hql, params)
        return (result[0] ?: 0) as Long
    }

    /**
     * Count executions including cross-project job references.
     * Uses dual-query approach: counts direct executions separately from referenced executions.
     */
    private Long countWithJobReferences(String filterClause, Map filterParams) {
        if (!this.jobIdListFilter) {
            return 0L
        }

        String jobUuid = this.jobIdListFilter[0]

        // COUNT QUERY 1: Executions with ReferencedExecution entries (cross-project job references)
        // Uses HQL with INNER JOIN instead of EXISTS for better performance (1900x faster)
        // Counts executions in projects that have referenced this job (via workflow job reference steps)
        // execProjects contains all projects where this job has been referenced, including projFilter
        def hql1Base = """SELECT COUNT(e.id)
FROM Execution e
INNER JOIN ReferencedExecution re WITH re.execution.id = e.id
WHERE e.scheduledExecution IS NOT NULL
AND re.jobUuid = :jobUuid
AND e.project IN (:projects)"""
        def hql1 = hql1Base + filterClause
        def params1 = [jobUuid: jobUuid, projects: this.execProjects]
        if (filterParams) {
            params1.putAll(filterParams)
        }
        def count1 = Execution.executeQuery(hql1, params1)[0] ?: 0

        // COUNT QUERY 2: Direct executions in the job's own project (without ReferencedExecution)
        // This counts executions that were directly triggered (not via a parent job's workflow step)
        // Note: projFilter may be in execProjects if the job references itself, but such executions
        // will have a ReferencedExecution entry and thus only be counted in COUNT QUERY 1
        def hql2Base = """SELECT COUNT(e.id)
FROM Execution e
WHERE e.project = :project
AND e.scheduledExecution IS NOT NULL
AND e.jobUuid = :jobUuid
AND NOT EXISTS (
    SELECT 1 FROM ReferencedExecution re
    WHERE re.execution.id = e.id
)"""
        def hql2 = hql2Base + filterClause
        def params2 = [project: this.projFilter, jobUuid: jobUuid]
        if (filterParams) {
            params2.putAll(filterParams)
        }
        def count2 = Execution.executeQuery(hql2, params2)[0] ?: 0

        return (count1 as Long) + (count2 as Long)
    }

    /**
     * Check if this query should use the cross-project job reference optimization
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

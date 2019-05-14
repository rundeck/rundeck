<template>
  <div class="row">
    <div class="col-xs-12">
      <div class="card">
        <div class="card-content">
          <!-- <ul v-for="rpt in reports" :key="rpt.execution.id">
            <li>{{rpt.execution.id}}</li>
          </ul> -->

          <div >
          <section class="section-space-bottom">

            <span  >

                <span v-if="pagination.total>0" class="text-muted">
                {{reports.length}} of
                </span>
                <a href="#">
                <span class="summary-count" :class="{ 'text-primary': pagination.total < 1, 'text-info': pagination.total > 0 }">
                  {{pagination.total}}
                </span>
                {{$tc('execution',pagination.total)}}
                </a>
            </span>
            In the last

            <dropdown >
              <span class="dropdown-toggle text-info">
                {{period}}
                <span class="caret"></span>
              </span>
              <template slot="dropdown">
                <li v-for="(val,pval) in periods" :key="pval">
                  <a role="button" @click="changePeriod(pval)">
                  {{pval}}
                  <span v-if="period===pval">√</span>
                  </a>
                </li>
              </template>
            </dropdown>

          <!-- bulk edit controls -->
          <div class="pull-right" v-if="auth.deleteExec && pagination.total>0">
              <span v-if="bulkEditMode" class="history_bulk_edit">
                <strong>{{bulkSelectedIds.length}}</strong> selected
                <span class="btn btn-default btn-xs act_bulk_edit_selectall  " @click="bulkEditSelectAll">
                    <i18n path="select.all"/>
                </span>
                <span class="btn btn-default btn-xs act_bulk_edit_deselectall  "
                      data-toggle="modal"
                      data-target="#cleanselections">
                    <i18n path="select.none"/>
                </span>

                <btn size="xs" type="danger" class="btn-fill"
                      :disabled="bulkSelectedIds.length<1"
                      @click="showBulkEditConfirm=true">
                    <i18n path="delete.selected.executions"/>
                </btn>
                <span class="btn btn-default btn-xs"
                      @click="bulkEditMode=false"
                      >
                    <i18n path="cancel.bulk.delete"/>
                </span>
            </span>


            <button class="btn btn-xs btn-warning"
            v-if="auth.deleteExec && !bulkEditMode"
              @click="bulkEditMode=true"
              >
                {{$t('bulk.edit')}}
            </button>
            <!-- TODO modals -->
        </div>

      <div v-if="reports.length < 1 " class="help-block">
          <span class="text-secondary" >
            No results for the query
          </span>
      </div>

    </section>

      <!-- Bulk edit modals -->

      <div class="modal" id="cleanselections" tabindex="-1" role="dialog"
                 aria-labelledby="cleanselectionstitle" aria-hidden="true">
          <div class="modal-dialog">
              <div class="modal-content">
                  <div class="modal-header">
                      <button type="button" class="close" data-dismiss="modal"
                              aria-hidden="true">&times;</button>
                      <h4 class="modal-title" id="cleanselectionstitle">Clean bulk selection</h4>
                  </div>

                  <div class="modal-body">

                      <p>Clear all <strong>{{bulkSelectedIds.length}}</strong>
                          executions or only executions shown on this page?
                      </p>
                  </div>

                  <div class="modal-footer">

                          <button type="submit"
                                  class="btn btn-default  "
                                  data-dismiss="modal"
                                  @click="bulkEditDeselectAll">
                              Only shown executions
                          </button>
                          <button class="btn btn-danger "
                                  data-dismiss="modal"
                                  @click="bulkEditDeselectAllPages">
                              All
                          </button>
                  </div>
              </div>
          </div>
      </div>
      <modal v-model="showBulkEditConfirm" id="bulkexecdelete" :title="$t('Bulk Delete Executions')">
        <p>Really delete <strong >{{bulkSelectedIds.length}}</strong>
            {{$tc('execution',bulkSelectedIds.length)}}
            ?
        </p>

        <div slot="footer">
            <btn @click="showBulkEditConfirm=false">
                {{$t('cancel')}}
            </btn>
            <btn type="danger"
                  @click="performBulkDelete"
                  data-dismiss="modal">
                {{$t('Delete Selected')}}
            </btn>
        </div>
      </modal>
       <div class="modal" id="bulkexecdeletex" tabindex="-1" role="dialog"
                 aria-labelledby="bulkexecdeletetitle" aria-hidden="true">
          <div class="modal-dialog">
              <div class="modal-content">
                  <div class="modal-header">
                      <button type="button" class="close" data-dismiss="modal"
                              aria-hidden="true">&times;</button>
                      <h4 class="modal-title" id="bulkexecdeletetitle">
                        {{$t('Bulk Delete Executions')}}
                        </h4>
                  </div>

                  <div class="modal-body">

                      <p>Really delete <strong >{{bulkSelectedIds.length}}</strong>
                          {{$tc('execution',bulkSelectedIds.length)}}
                          ?
                      </p>
                  </div>

                  <div class="modal-footer">

                          <button type="submit" class="btn btn-default  " data-dismiss="modal">
                              {{$t('cancel')}}
                          </button>
                          <button class="btn btn-danger "
                                  @click="performBulkDelete"
                                  data-dismiss="modal"
                                  >
                              {{$t('Delete Selected')}}
                          </button>
                  </div>
              </div>
          </div>
      </div>
      <modal v-model="showBulkEditResults" id="bulkexecdeleteresult" :title="$t('Bulk Delete Executions: Results')" >
                  <div  v-if="bulkEditProgress">
                      <em>
                          <i class="glyphicon glyphicon-time text-info"></i>
                          {{$t('Requesting bulk delete, please wait.')}}
                      </em>
                  </div>
                  <div v-if="!bulkEditProgress">

                      <p
                      v-if="bulkEditResults && bulkEditResults.requestCount > 0"
                          class="text-info">

                          <i18n path="bulkresult.attempted.text" tag="p" >
                            <strong >{{bulkEditResults.requestCount}}</strong>
                          </i18n>

                      </p>
                      <p
                      v-if="bulkEditResults && bulkEditResults.successCount > 0"
                          class="text-success">


                          <i18n path="bulkresult.success.text" tag="p" >
                            <strong >{{bulkEditResults.successCount}}</strong>
                          </i18n>
                      </p>
                      <p
                       v-if="bulkEditResults && bulkEditResults.failedCount > 0"
                              class="text-warning">

                          <i18n path="bulkresult.failed.text" tag="p" >
                            <strong >{{bulkEditResults.failedCount}}</strong>
                          </i18n>
                      </p>
                      <div v-if="bulkEditResults && bulkEditResults.failures && bulkEditResults.failures.length > 0">
                          <ul v-for="(message,ndx) in bulkEditResults.failures" :key="ndx">
                              <li>{{message}}</li>
                          </ul>
                      </div>

                      <div  v-if="bulkEditError">
                            <p class="text-danger" >{{bulkEditError}}</p>
                      </div>

                  </div>
                <div slot="footer">
                  <btn @click="showBulkEditResults=false">{{$t('close')}}</btn>
                </div>
      </modal>

    <table class=" table table-hover table-condensed events-table events-table-embed"

           v-if="reports.length > 0">
        <tbody class="no-border-on-first-tr"  v-for="rpt in reports" :key="rpt.execution.id">
        <tr class="link activity_row autoclick"
        @click="autoBulkEdit(rpt)"
          :class="{succeed:rpt.status==='succeed',fail:rpt.status==='fail',highlight:highlightExecutionId==rpt.executionId,job:rpt.jobId,adhoc:!rpt.jobId}">
            <td class="eventicon" v-if="bulkEditMode">
                <input
                type="checkbox"
                name="bulk_edit"
                :value="rpt.executionId"
                v-model="bulkSelectedIds"
                class="_defaultInput"/>
            </td>
            <td class="eventicon autoclickable" :title="executionState(rpt.execution.status)">
                <i class="exec-status icon" :data-execstate="executionStateCss(rpt.execution.status)" :data-statusstring="rpt.execution.status"></i>
            </td>
            <td class="eventtitle autoclickable" :class="{job:rpt.jobId,adhoc:!rpt.jobId}" >
                <a  :href="rpt.executionHref" class="_defaultAction">
                #{{rpt.executionId}}
                </a>
                <!-- <g:if test="${includeJobRef}">
                    <span data-bind="text: textJobRef('${scheduledExecution.extid}')"></span>
                </g:if> -->


                      <span v-if="!rpt.jobDeleted && rpt.jobId">
                        {{rpt.groupPath}}
                        {{rpt.jobName}}
                      </span>
                      <span v-else>
                        {{rpt.executionString}}
                      </span>

                    <span v-if="rpt.jobDeleted" class="text-primary">
                        (<i18n path="domain.ScheduledExecution.title"/>
                        {{rpt.jobName}}
                        has been deleted)
                    </span>

                <span v-if="isCustomStatus(rpt.execution.status)">
                    <span class="exec-status-text custom-status" >{{rpt.execution.status}}</span>
                </span>
            </td>
            <td class="eventargs autoclickable" >
                <div class="argstring-scrollable">
                <span v-if="rpt.execution.jobArguments">
                    <span v-for="(value,key) in rpt.execution.jobArguments" :key="key">
                        {{key}}:
                        <code  class="optvalue">{{value}}</code>
                    </span>
                </span>

                <span v-if="!rpt.execution.jobArguments">{{rpt.execution.argString}}</span>

                </div>
            </td>
            <td class="right date autoclickable">
                <span v-if="rpt.dateCompleted">
                    <span class="timeabs">
                        {{rpt.dateCompleted | moment(momentJobFormat)}}
                    </span>
                    <span title="">
                        <span class="text-primary"><i18n path="in.of" default="in"/></span>
                        <span class="duration" data-bind="text: durationHumanize()">{{rpt.duration | duration('humanize')}}</span>
                    </span>
                </span>
                <span v-if="!rpt.dateCompleted && rpt.status == 'scheduled'">
                    Scheduled; starting <span data-bind="text: timeToStart()"></span>
                </span>
                <span v-if="!rpt.dateCompleted && rpt.jobPercentageFixed >= 0 && rpt.status != 'scheduled'">
                    <div v-if="!rpt.job || rpt.jobAverageDuration==0">
                    <!-- <g:render template="/common/progressBar" model="${[
                            indefinite: true, title: 'Running', innerContent: 'Running', width: 120,
                            progressClass: 'rd-progress-exec progress-striped active indefinite progress-embed',
                            progressBarClass: 'progress-bar-info',
                    ]}"/> -->
                    striped-progress
                    </div>
                    <div v-if="rpt.job && rpt.jobAverageDuration>0">
                      progress-bar
                        <!-- <g:set var="progressBind" value="${', css: { \'progress-bar-info\': jobPercentageFixed() < 105 ,  \'progress-bar-warning\': jobPercentageFixed() > 104  }'}"/> -->
                        <!-- <g:render template="/common/progressBar"
        model="[completePercent: 0,
                                          progressClass: 'rd-progress-exec progress-embed',
                                          progressBarClass: '',
                                          containerId: 'progressContainer2',
                                          innerContent: '',
                                          showpercent: true,
                                          progressId: 'progressBar',
                                          bind: 'jobPercentageFixed()',
                                          progressBind: progressBind,
                                  ]"/> -->
                                  <!-- <progress-bar v-model="jobPercentageFixed"></progress-bar> -->
                    </div>
                </span>
            </td>

            <td class="  user text-right autoclickable" style="white-space: nowrap;">
                <em><i18n path="by" default="by"/></em>
                {{rpt.user}}
            </td>


        </tr>
        </tbody>
    </table>
    <offset-pagination
          :pagination="pagination"
          @change="changePageOffset($event)"
          :disabled="loadingEvents"
        />
            </div>
        </div>
    </div>

  </div>
</template>

<script lang="ts">
import axios from 'axios'
import Vue from 'vue'
import OffsetPagination from '@rundeck/ui-trellis/src/components/utils/OffsetPagination.vue'
// import { client } from '../../services/rundeckClient'

import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"
import { ExecutionBulkDeleteResponse } from 'ts-rundeck/dist/lib/models';



const knownStatusList = ['scheduled','running','succeed','succeeded','failed',
    'cancel','aborted','retry','timedout','timeout','fail'];

export default Vue.extend({
  name: 'ActivityList',
  components:{
    OffsetPagination
  },
  props: [
    'project',
    'queryParams',
    'eventBus'
  ],
  data () {
    return {
      href:'#',
      reports:[],
      lastDate:-1,
      pagination:{
        offset:0,
        max:20,
        total:0
      },
      internalquery:{
        recentFilter: '1d'
      },
      loading: false,
      momentJobFormat:'M/DD/YY h:mm a',
      momentRunFormat:'h:mm a',
      bulkEditMode:false,
      bulkSelectedIds: [] as string[],
      bulkEditProgress:false,
      bulkEditResults:null as null|ExecutionBulkDeleteResponse,
      bulkEditError:'',
      showBulkEditResults:false,
      showBulkEditConfirm:false,
      highlightExecutionId:null,
      activityUrl:'',
      bulkDeleteUrl:'',
      auth:{
        projectAdmin:false,
        deleteExec:false
      },

      period:'Day',
      periods: {
        Hour:'1h',
        Day:'1d',
        Week:'1w',
        Month:'1m'
      } as {[name:string]:string}
    }
  },
  methods: {
    toggleSelectId(id:string){
      const ndx=this.bulkSelectedIds.indexOf( id )
      if(ndx>=0){
        //remove
        this.bulkSelectedIds.splice(ndx,1)
      }else{
        this.bulkSelectedIds.push(id)
      }
    },
    selectId(id:string){
      const ndx=this.bulkSelectedIds.indexOf( id )
      if(ndx<0){
        this.bulkSelectedIds.push(id)
      }
    },
    deselectId(id:string){
      const ndx=this.bulkSelectedIds.indexOf( id )
      if(ndx>=0){
        //remove
        this.bulkSelectedIds.splice(ndx,1)
      }
    },
    autoBulkEdit(rpt:any){
      if(!this.bulkEditMode){
        if(rpt.executionHref){
          window.location=rpt.executionHref
        }
      }
      this.toggleSelectId(rpt.executionId)
    },
    bulkEditSelectAll(){
      this.reports.forEach((val:any)=>this.selectId(val.executionId))
    },
    bulkEditDeselectAll(){
      this.reports.forEach((val:any)=>this.deselectId(val.executionId))
    },
    bulkEditDeselectAllPages(){
      this.bulkSelectedIds=[]
    },
    isCustomStatus(status:string) {
      return knownStatusList.indexOf(status)<0
    },
    executionStateCss(status:string){
      return this.executionState(status).toUpperCase()
    },
    executionState(status:string){
        if (status == 'scheduled') {
            return 'scheduled';
        }
        if (status == 'succeed' || status == 'succeeded') {
            return 'succeeded';
        }
        if (status == 'fail' || status == 'failed') {
            return 'failed';
        }
        if (status == 'cancel' || status == 'aborted') {
            return 'aborted';
        }
        if (status == 'running') {
            return 'running';
        }
        if (status == 'timedout') {
            return 'timedout';
        }
        if (status == 'retry') {
            return 'failed-with-retry';
        }
        return 'other';
    },
    changePeriod(val: string){
      this.period=val
      this.internalquery.recentFilter=this.periods[val]
      this.loadActivity(0)
    },
    async bulkDeleteExecutions(ids:string[]){
      const rundeckContext = getRundeckContext()
      this.bulkEditProgress=true
      this.showBulkEditResults = true
      try{
        this.bulkEditResults = await rundeckContext.rundeckClient.executionBulkDelete({ids})
        this.bulkEditProgress=false
        this.bulkSelectedIds = []
        if(this.bulkEditResults.allsuccessful){
          this.bulkEditMode = false
        }
        this.loadActivity(this.pagination.offset)
        console.log("response: ",this.bulkEditResults)
      }catch(error){
        this.bulkEditProgress=false
        this.bulkEditError=error
      }
    },
    performBulkDelete(){
      this.showBulkEditConfirm=false
      this.bulkDeleteExecutions(this.bulkSelectedIds)
    },
    async loadActivity(offset:number){
      this.loading = true
      const response = await axios.get(this.activityUrl,{
        headers: {'x-rundeck-ajax': true},
        params: Object.assign({offset: offset, max: this.pagination.max},this.internalquery),
        withCredentials: true
      })
      this.loading=false
      if (response.data) {
        this.pagination.offset=response.data.offset
        this.pagination.total=response.data.total
        this.lastDate=response.data.lastDate
        this.reports = response.data.reports
        this.eventBus&&this.eventBus.$emit('activity-query-result',response.data)
      }

    },
    changePageOffset(offset:number){
      if (this.loading) {
        return;
      }
      this.loadActivity(offset)
    },

  },

  mounted () {
    if(window._rundeck.data.jobslistDateFormatMoment){
      this.momentJobFormat=window._rundeck.data.jobslistDateFormatMoment
    }
    if(this.queryParams){
      this.internalquery=this.queryParams
    }
     this.eventBus&&this.eventBus.$on('change-query-period', (data:string) => {
      this.internalquery.recentFilter=data
      this.loadActivity(0)
    })
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      this.auth.projectAdmin=window._rundeck.data['projectAdminAuth']
      this.auth.deleteExec=window._rundeck.data['deleteExecAuth']
      this.activityUrl = window._rundeck.data['activityUrl']
      this.bulkDeleteUrl = window._rundeck.data['bulkDeleteUrl']
      this.loadActivity(0)
    }
  }
})
</script>

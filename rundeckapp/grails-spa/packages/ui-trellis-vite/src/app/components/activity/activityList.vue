<template>
  <div class=" activity-list">

          <section class="section-space-bottom">

            <span>

                <span v-if="pagination.total > 0 && pagination.total > pagination.max" class="text-muted">
                  {{pagination.offset+1}}
                  -
                  <span v-if="!loading">
                  {{pagination.offset+reports.length}}
                  </span>
                  <span v-else class="text-muted">
                    <i class="fas fa-spinner fa-pulse" ></i>
                  </span>
                of
                </span>

                <a :href="activityHref" class="link-quiet">
                  <span class="summary-count" :class="{ 'text-strong': pagination.total < 1, 'text-info': pagination.total > 0 }" v-if="pagination.total>=0">
                    {{pagination.total}}
                  </span>
                  <span v-else-if="!loadError" class="text-muted">
                    <i class="fas fa-spinner fa-pulse" ></i>
                  </span>
                  {{$tc('execution',pagination.total>0?pagination.total:0)}}
                </a>
            </span>


          <activity-filter v-model="query" :event-bus="eventBus" :opts="filterOpts" v-if="showFilters"></activity-filter>

          <div class="pull-right">
            <span v-if="runningOpts.allowAutoRefresh" class="pr-2">
              <input type=checkbox id=auto-refresh v-model=autorefresh />
              <label for="auto-refresh" class="pr-2">{{$t('Auto refresh')}}</label>
            </span>
            <!-- bulk edit controls -->
            <span  v-if="auth.deleteExec && pagination.total>0 && showBulkDelete" class="spacing-x">
                <span v-if="bulkEditMode" >
                  <i18n path="bulk.selected.count">
                    <strong>{{bulkSelectedIds.length}}</strong>
                  </i18n>
                  <span class="btn btn-default btn-xs   " @click="bulkEditSelectAll">
                      <i18n path="select.all"/>
                  </span>
                  <span class="btn btn-default btn-xs   "
                        @click="showBulkEditCleanSelections=true">
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


              <btn size="xs" type="default" v-if="auth.deleteExec && !bulkEditMode" @click="bulkEditMode=true">
                  {{$t('bulk.delete')}}
              </btn>
            </span>
          </div>

    </section>

    <!-- Bulk edit modals -->
    <modal  v-model="showBulkEditCleanSelections" id="cleanselections" :title="$t('Clear bulk selection')" append-to-body>

      <i18n tag="p" path="clearselected.confirm.text">
        <strong>{{bulkSelectedIds.length}}</strong>
      </i18n>

      <div slot="footer">
        <btn data-dismiss="modal" @click="showBulkEditCleanSelections=false">{{$t('cancel')}}</btn>
        <button type="submit"
                class="btn btn-default  "
                data-dismiss="modal"
                @click="bulkEditDeselectAll">
            {{$t('Only shown executions')}}
        </button>
        <button class="btn btn-danger "
                data-dismiss="modal"
                @click="bulkEditDeselectAllPages">
            {{$t('all')}}
        </button>
      </div>
    </modal>

    <modal v-model="showBulkEditConfirm" id="bulkexecdelete" :title="$t('Bulk Delete Executions')" append-to-body>
      <i18n tag="p" path="delete.confirm.text">
        <strong>{{bulkSelectedIds.length}}</strong>
        <span>{{$tc('execution',bulkSelectedIds.length)}}</span>
      </i18n>

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

    <modal v-model="showBulkEditResults" id="bulkexecdeleteresult" :title="$t('Bulk Delete Executions: Results')" append-to-body>
                <div  v-if="bulkEditProgress">
                    <em>
                        <b class="glyphicon glyphicon-time text-info"></b>
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
    <div class="card-content-full-width">
    <table class=" table table-hover table-condensed " >
      <tbody  v-if="running && running.executions&& running.executions.length>0" class="running-executions">
        <tr
            v-for="exec in running.executions"
            :key="exec.id"
            class="execution link activity_row autoclickable"
            :class="{nowrunning:!exec.dateCompleted,[exec.status]:true}"
            @click="autoBulkEdit(exec)"
            @click.middle="middleClickRow(exec)"
            >
          <!-- #{{exec.id}} -->

            <td class="eventicon" v-if="bulkEditMode">
                <input
                type="checkbox"
                name="bulk_edit"
                :value="exec.id"
                v-model="bulkSelectedIds"
                :disabled="exec.status==='running' || exec.status==='scheduled' || exec.status==='queued'"
                class="_defaultInput"/>
            </td>
                 <td class="eventicon " :title="executionState(exec.status)" >
                    <b class="fas fa-circle-notch fa-spin text-info" v-if="exec.status==='running'"></b>
                    <b class="fas fa-clock text-muted " v-else-if="exec.status==='scheduled'"></b>
                    <b class="fas fa-layer-group text-muted " v-else-if="exec.status==='queued'"></b>
                    <b class="exec-status icon" :data-execstate="executionStateCss(exec.status)" :data-statusstring="exec.status" v-else></b>
                </td>
                <td class="right date " v-tooltip.bottom="runningStatusTooltip(exec)">
                    <span v-if="exec.dateStarted.date" >
                        <i class=" ">
                            {{exec.dateStarted.date | moment(momentJobFormat)}}
                        </i>
                        <i class="timerel text-muted" v-if="isRecentCalendarDate(exec.dateStarted.date)">
                            {{exec.dateStarted.date | moment('calendar',null)}}
                        </i>
                        <i class="timerel text-muted" v-else>
                            {{exec.dateStarted.date | moment('from','now')}}
                        </i>
                    </span>
                </td>
                <td class="dateStarted date " v-tooltip="runningStatusTooltip(exec)" colspan="2">
                      <progress-bar v-if="exec.status === 'scheduled'" :value="100" striped  type="default" label :label-text="$t('job.execution.starting.0',[runningStartedDisplay(exec.dateStarted.date)])" ></progress-bar>
                      <progress-bar v-else-if="exec.status === 'queued'" :value="100" striped  type="default" label :label-text="$t('job.execution.queued')" ></progress-bar>
                      <progress-bar v-else-if="exec.job && exec.job.averageDuration" :value="jobDurationPercentage(exec)" striped active type="info" label min-width></progress-bar>
                      <progress-bar v-else-if="exec.dateStarted.date" :value="100" striped active type="info" label :label-text="$t('running')" ></progress-bar>
                </td>

                <td class="  user text-right " style="white-space: nowrap;">
                    <em><i18n path="by" default="by"/></em>
                    {{exec.user}}
                </td>

                <td class=" eventtitle job" v-if="exec.job" v-tooltip="(exec.job.group ? exec.job.group + '/' +exec.job.name : '')">
                    {{exec.job.name}}
                </td>

                <td class="activity-list__eventargs" v-if="exec.job">
                  <span v-if="exec.job.options" class="activity-list__eventargs-string">
                      <span v-for="(value,key) in exec.job.options" :key="key">
                          <span class="optkey">{{key}}</span>
                          <code  class="optvalue">{{value}}</code>
                      </span>
                  </span>
                </td>


                <td class="eventtitle adhoc " v-if="!exec.job" colspan="2">
                    {{exec.description}}
                </td>

                <td class="text-right">
                    <a class="link-quiet" title="View execution output" :href=exec.permalink>#{{exec.id}}</a>
                </td>
            </tr>
        </tbody>
        <tbody class="since-count-data autoclickable"  @click="reload" v-if="sincecount>0">
          <tr>
              <td colspan=8 class=text-center>
                {{ $tc('info.newexecutions.since.0', sincecount) }}
            </td>
          </tr>
        </tbody>
        <tbody class="history-executions" v-if="reports.length > 0">
        <tr class="link activity_row autoclickable"
            @click="autoBulkEdit(rpt)"
            @click.middle="middleClickRow(rpt)"
            :class="[`ali-${rpt.execution.id}`,{succeed:rpt.status==='succeed',fail:rpt.status==='fail',missed:rpt.status==='missed',highlight:highlightExecutionId==rpt.executionId,job:rpt.jobId,adhoc:!rpt.jobId}]"
            v-for="rpt in reports"
            :key="rpt.execution.id"
          >
            <td class="eventicon" v-if="bulkEditMode">
                <input
                type="checkbox"
                name="bulk_edit"
                :value="rpt.executionId"
                v-model="bulkSelectedIds"
                class="_defaultInput"/>
            </td>
            <td class="eventicon " :title="reportState(rpt)" >
                <b class="exec-status icon" :data-execstate="reportStateCss(rpt)" :data-statusstring="reportState(rpt)"></b>
            </td>
            <td class="right date"
              v-tooltip.bottom="{
                text: $t(rpt.status==='missed'?'info.missed.0.1':'info.completed.0.1',[jobCompletedISOFormat(rpt.dateCompleted),jobCompletedFromNow(rpt.dateCompleted)]),
                'viewport': `.ali-${rpt.execution.id}`
            }">
                <span v-if="rpt.dateCompleted">
                    <span class="timeabs ">
                        {{rpt.dateCompleted | moment(momentJobFormat)}}
                    </span>
                    <span class="timerel text-muted" v-if="isRecentCalendarDate(rpt.dateCompleted)">
                        {{rpt.dateCompleted | moment('calendar',null)}}
                    </span>
                    <span class="timerel text-muted" v-else>
                        {{rpt.dateCompleted | moment('from','now')}}
                    </span>
                </span>
            </td>

            <td class=" node-stats" :title="$t('0.total',[rpt.node.total])">
                <span v-if="rpt.node.failed>0">{{rpt.node.failed}} {{$t('failed')}}</span>
                <span v-else-if="rpt.node.succeeded>0">{{rpt.node.succeeded}} {{$t('ok')}}</span>
            </td>
            <td class="duration text-secondary">
                <span class="duration" v-if="rpt.status==='missed'">missed</span>
                <span class="duration" v-else>{{formatDurationMomentHumanize(rpt.duration)}}</span>
            </td>
            <td class="  user text-right " style="white-space: nowrap;">
                <em><i18n path="by" default="by"/></em>
                {{rpt.user}}
            </td>
            <td class="eventtitle " :class="{job:rpt.jobId,adhoc:!rpt.jobId}" >
                  <span v-if="!rpt.jobDeleted && rpt.jobId" v-tooltip="(rpt.jobGroup ? rpt.jobGroup + '/' +rpt.jobName: '')">
                    {{rpt.jobName}}
                  </span>
                  <span v-else>
                    {{rpt.executionString}}
                  </span>
                  <span v-if="query.jobIdFilter && rpt.jobId && query.jobIdFilter !==rpt.jobId && query.jobIdFilter !=='!null'" class="text-secondary">
                    <i class="fas fa-arrow-circle-right-alt"></i>
                    {{$t('Referenced')}}
                  </span>


                <span v-if="rpt.jobDeleted" class="text-strong">
                      {{$t('job.has.been.deleted.0',[rpt.jobName])}}
                </span>

                <span v-if="isCustomReportStatus(rpt)">
                    <span class="exec-status-text custom-status" >{{rpt.execution.status}}</span>
                </span>
            </td>
            <td class="activity-list__eventargs" >

                <span v-if="rpt.execution.jobArguments" class="activity-list__eventargs-string">
                    <span v-for="(value,key) in rpt.execution.jobArguments" :key="key">
                        {{key}}:
                        <code  class="optvalue">{{value}}</code>
                    </span>
                </span>

                <span v-if="!rpt.execution.jobArguments" class="activity-list__eventargs-string">{{rpt.execution.argString}}</span>


            </td>

            <td class="text-right">
                <a class="link-quiet" :href="rpt.executionHref" @click.middle.stop>
                #{{rpt.executionId}}
                </a>
            </td>
        </tr>
        </tbody>
    </table>
    </div>

    <div v-if="reports.length < 1 " class="loading-area">
        <span class="loading-text" v-if="!loading && !loadError">
          {{$t('results.empty.text')}}
        </span>
        <div class="loading-text" v-if="loading && lastDate<0">
          <i class="fas fa-spinner fa-pulse" ></i>
          {{$t('Loading...')}}
        </div>
        <div class="text-warning" v-if="loadError">
          <i class="fas fa-error" ></i>
          {{$t('error.message.0',[loadError])}}

        </div>
    </div>

    <offset-pagination
          :pagination="pagination"
          @change="changePageOffset($event)"
          :disabled="loading"
          :showPrefix="false"
        >
        </offset-pagination>

  </div>
</template>

<script lang="ts">
import axios from 'axios'
import Vue from 'vue'
import moment from 'moment'
import OffsetPagination from '@/library/components/utils/OffsetPagination.vue'
import ActivityFilter from './activityFilter.vue'

import {getRundeckContext} from "@/library/centralService"
import {Execution, ExecutionBulkDeleteResponse} from '@rundeck/client/dist/lib/models';
//import {clearTimeout, setTimeout} from 'timers';

/**
 * Generate a URL
 * @param url
 * @param params
 * @returns {string}
 * @private
 */
function _genUrl(url:string, params:any) {
  var urlparams = [];
  if (typeof (params) == 'string') {
    urlparams = [params];
  } else if (typeof (params) == 'object') {
    for (var e in params) {
      urlparams.push(encodeURIComponent(e) + "=" + encodeURIComponent(params[e]));
    }
  }
  return url + (urlparams.length ? ((url.indexOf('?') > 0 ? '&' : '?') + urlparams.join("&")) : '');
}


const knownStatusList = ['scheduled','running','queued','succeed','succeeded','failed','missed',
    'cancel','aborted','retry','timedout','timeout','fail'];

function nodeStats(node: string) {
    let info = {
        total: 1,
        succeeded: 1,
        failed: 0
    } as { [key: string]: number }
    let match = node.match(/(\d+)\/(\d+)\/(\d+)/)
    if (match) {
        info.succeeded = parseInt(match[1])
        info.failed = parseInt(match[2])
        info.total = parseInt(match[3])
    }
    return info
}

export default Vue.extend({
  name: 'ActivityList',
  components:{
    OffsetPagination,
    ActivityFilter
  },
  props: [
    'eventBus',
    'displayMode'
  ],
  data() {
    return {
      projectName:'',
      activityPageHref:'',
      sinceUpdatedUrl:'',
      reports:[],
      running: null as null|any,
      lastDate:-1,
      pagination:{
        offset:0,
        max:10,
        total:-1
      },
      filterOpts: {},
      showFilters: false,
      showBulkDelete: true,
      runningOpts: {
        loadRunning:true,
        allowAutoRefresh: true
      } as {[key:string]:any},
      autorefresh:false,
      autorefreshms:5000,
      autorefreshtimeout:null as null | any,
      sincecount:0,
      loading: false,
      loadingRunning: false,
      loadError:null,
      momentJobFormat:'M/DD/YY h:mm a',
      momentRunFormat:'h:mm a',
      bulkEditMode:false,
      bulkSelectedIds: [] as string[],
      bulkEditProgress:false,
      bulkEditResults:null as null|ExecutionBulkDeleteResponse,
      bulkEditError:'',
      showBulkEditResults:false,
      showBulkEditConfirm:false,
      showBulkEditCleanSelections:false,
      highlightExecutionId:null,
      activityUrl:'',
      nowrunningUrl:'',
      bulkDeleteUrl:'',
      auth:{
        projectAdmin:false,
        deleteExec:false
      },
      query: {
        jobFilter:'',
        jobIdFilter:'',
        userFilter:'',
        execnodeFilter:'',
        titleFilter:'',
        statFilter:'',
        recentFilter:'',
        filterName:''
      } as {[key:string]:string},
      currentFilter:'',
      disableRefresh:false,
    }
  },
  methods: {
    jobDurationPercentage(exec:Execution){
      if(exec.job && exec.job.averageDuration && exec.dateStarted && exec.dateStarted.date){
        const diff=moment().diff(moment(exec.dateStarted.date))
        return Math.floor(100 * ( diff / exec.job.averageDuration ))
      }
      return 0
    },
    runningStartedDisplay(date:string){
      if(!date){
        return ''
      }
      return moment(date).fromNow()
    },
    executionScheduledDisplay(date:string){
      if(!date){
        return ''
      }
      return moment(date).toNow()
    },
    jobCompletedFormat(date:string){
      if(!date){
        return ''
      }
      return moment(date).format(this.momentJobFormat)
    },
    jobCompletedISOFormat(date:string){
      if(!date){
        return ''
      }
      return moment(date).toISOString()
    },
    jobCompletedFromNow(date:string){
      if(!date){
        return ''
      }
      return moment(date).fromNow()
    },
    isRecentCalendarDate(date:string){
      if(!date){
        return false
      }
      return moment().diff(moment(date),'days') <= 7
    },
    runningStatusTooltip(exec:Execution){
      if(exec.status?.toString() === 'queued') {
        return (this as any).$t('job.execution.queued')
      }else
        if(exec.status == 'scheduled' && exec.dateStarted && exec.dateStarted.date){
        return (this as any).$t('job.execution.starting.0',[this.runningStartedDisplay(exec.dateStarted.date)])
      }else if(exec.dateStarted && exec.dateStarted.date){
        const startmo=moment(exec.dateStarted.date)
        if(exec.job && exec.job.averageDuration){
          const expected = startmo.clone()
          expected.add(exec.job.averageDuration,'ms')
          return (this as any).$t('info.started.expected.0.1',[startmo.fromNow(), expected.fromNow()])
        }
        return (this as any).$t('info.started.0',[startmo.fromNow()])
      }
      return ''
    },
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
    middleClickRow(rpt:any) {
      if(rpt.executionHref){
        window.open(rpt.executionHref, '_blank')
      }else if(rpt.permalink){
        window.open(rpt.permalink, '_blank')
      }
    },
    autoBulkEdit(rpt:any){
      if(!this.bulkEditMode){
        if(rpt.executionHref){
          window.location=rpt.executionHref
        }else if(rpt.permalink){
          window.location=rpt.permalink
        }
      }else if(rpt.executionId){
        this.toggleSelectId(rpt.executionId)
      }else if(rpt.id && rpt.status!=='running' && rpt.status!=='scheduled' && rpt.status!=='queued'){
        this.toggleSelectId(rpt.id)
      }
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
    isCustomReportStatus(rpt:any){
      return this.isCustomStatus(this.reportState(rpt))
    },
    executionStateCss(status:string){
      return this.executionState(status).toUpperCase()
    },
    reportStateCss(rpt:any){
      return this.executionStateCss(this.reportState(rpt))
    },
    formatDurationMomentHumanize(ms:any) {
        moment.relativeTimeThreshold('ss', -1)
        if (ms < 0) {
            return '';
        }
        var duration = moment.duration(ms);
        return duration.humanize();
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
        if (status == 'queued') {
            return 'queued';
        }
        if (status == 'missed') {
          return 'missed';
        }
        if (status == 'timedout') {
            return 'timedout';
        }
        if (status == 'retry') {
            return 'failed-with-retry';
        }
        return 'other';
    },
    reportState(rpt:any){
      return rpt.execution.cancelled?this.executionState('cancel'):this.executionState(rpt.execution.status)
    },
    reload(){
      this.reports=[]
      this.pagination.total=-1
      this.lastDate=-1
      this.sincecount=0
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

      }catch(error){
        this.bulkEditProgress = false
        //@ts-ignore
        this.bulkEditError = error
      }
    },
    performBulkDelete(){
      this.showBulkEditConfirm=false
      this.bulkDeleteExecutions(this.bulkSelectedIds)
    },
    async loadSince(){
      const rundeckContext = getRundeckContext()
      if(this.lastDate<0){
        return
      }
      try{
        const response = await axios.get(this.sinceUpdatedUrl,{
          headers: {'x-rundeck-ajax': true},
          params: Object.assign({offset: this.pagination.offset, max: this.pagination.max},this.query,{since:this.lastDate}),
          withCredentials: true
        })

          if (this.lastDate > 0 && response.data.since && response.data.since.count) {
            this.sincecount = response.data.since.count
          }

      }catch(error){
        //@ts-ignore
        this.disableRefresh = !this.disableRefresh;
        this.loadError = error.message
      }
    },
    async loadRunning(){
      // const rundeckContext = getRundeckContext()
      this.loadingRunning = true
      let qparams: { [key: string]: string } = {}

      // include scheduled and queued on running list.
      qparams.includePostponed = "true"

      if (this.query.jobIdFilter) {
        qparams.jobIdFilter = this.query.jobIdFilter
      }
      try{
        //this.running = await rundeckContext.rundeckClient.executionListRunning(this.projectName)
        const response = await axios.get(this.nowrunningUrl,{
            headers: {'x-rundeck-ajax': true},
            params: qparams,
            withCredentials: true
        })

          let executions = response.data.executions.map((e: any) => {
              let { 'date-started': dateStarted, 'date-completed': dateCompleted } = e
              return Object.assign({ dateStarted, dateCompleted }, e) as Execution
          })
          this.running = { executions, paging: response.data.paging }
          this.loadingRunning = false
          this.eventBus.$emit('activity-nowrunning-count', executions.length)

      }catch(error){
        this.disableRefresh = !this.disableRefresh;
        this.loadingRunning=false
        //@ts-ignore
        this.loadError = error.message
      }
    },
    async loadActivity(offset:number){
      this.loading = true
      this.pagination.offset=offset
      let xquery:{[key:string]:string}={}
      if(this.query.jobIdFilter){
        xquery['includeJobRef']='true'
      }
      if(this.query.filterName){
        xquery['filterName']=this.query.filterName
      }else{
        Object.assign(xquery,this.query)
      }
      try{
        const response = await axios.get(this.activityUrl,{
          headers: {'x-rundeck-ajax': true},
          params: Object.assign({offset: offset, max: this.pagination.max},xquery),
          withCredentials: true
        })
        this.loading=false
        if (response.data) {
          this.pagination.offset=response.data.offset
          this.pagination.total=response.data.total
          this.lastDate=response.data.lastDate
          this.reports = response.data.reports.map((rpt: any) => {
            rpt.node = nodeStats(rpt.node)
            return rpt
          })
          this.eventBus&&this.eventBus.$emit('activity-query-result',response.data)
        }
      }catch(error){
        this.loading=false
        //@ts-ignore
        this.loadError=error.message
      }
    },
    changePageOffset(offset:number){
      if (this.loading) {
        return;
      }
      this.loadActivity(offset)
    },
    checkrefresh(time: number = 0) {
      if(!this.loadingRunning && this.autorefresh && !this.disableRefresh){
        let delay: number = time ? (new Date().getTime() - time) : 0
        let ms = this.loadError ? (this.autorefreshms * 10) : this.autorefreshms
        ms = time > 0 ? Math.min(60000, Math.max(ms, 5 * delay)) : 0
        this.autorefreshtimeout = setTimeout(() => {
          let cur = new Date()
          Promise.all([
            this.loadRunning(),
            this.loadSince()
          ]).then(() => this.checkrefresh(cur.getTime()))
        }, ms);
      }
    },
    startAutorefresh(){
      this.checkrefresh()
    },
    stopAutorefresh(){
      if(this.autorefreshtimeout){
        clearTimeout(this.autorefreshtimeout)
        this.autorefreshtimeout=null
      }
    },
    fullQueryParams():any{
      if(this.query.filterName){
        return {filterName:this.query.filterName}
      }
      let params={} as {[key:string]:string}
      for(let v in this.query){
        if(this.query[v]){
          params[v]=this.query[v]
        }
      }
      return params
    },
  },
  watch:{
    query:{
      handler(newValue,oldValue){
        this.reload()
      },
      deep:true
    },
    autorefresh:{
      handler(newValue,oldValue){
        if(newValue){
          //turn on
          this.startAutorefresh()
        }else{
          //turn off
          this.stopAutorefresh()
        }
      }
    },
  },
  computed:{
    activityHref():string {
      return _genUrl(this.activityPageHref , this.fullQueryParams())
    }
  },
  mounted () {
    if(window._rundeck.data.jobslistDateFormatMoment){
      this.momentJobFormat=window._rundeck.data.jobslistDateFormatMoment
    }

    this.projectName=window._rundeck.projectName
    if (window._rundeck && window._rundeck.data) {
      this.auth.projectAdmin=window._rundeck.data['projectAdminAuth']
      this.auth.deleteExec=window._rundeck.data['deleteExecAuth']
      this.activityUrl = window._rundeck.data['activityUrl']
      this.nowrunningUrl = window._rundeck.data['nowrunningUrl']
      this.bulkDeleteUrl = window._rundeck.data['bulkDeleteUrl']
      this.activityPageHref = window._rundeck.data['activityPageHref']
      this.sinceUpdatedUrl = window._rundeck.data['sinceUpdatedUrl']
        this.autorefreshms = window._rundeck.data['autorefreshms'] || 5000

      if(window._rundeck.data['pagination'] && window._rundeck.data['pagination'].max){
        this.pagination.max=window._rundeck.data['pagination'].max
      }
      if(window._rundeck.data['filterOpts'] ){
        this.filterOpts = window._rundeck.data.filterOpts
      }
      this.showFilters = true
      if(window._rundeck.data['query'] ){
        this.query = Object.assign({},this.query,window._rundeck.data['query'] )
      }else{
        this.loadActivity(0)
      }
      if(window._rundeck.data['runningOpts']){
        this.runningOpts= window._rundeck.data.runningOpts
      }

      if(window._rundeck.data['viewOpts']){
        this.showBulkDelete= window._rundeck.data.viewOpts.showBulkDelete
      }
      if (this.runningOpts['autorefresh']) {
        this.autorefresh = true
      }else if (this.runningOpts['loadRunning']) {
        this.loadRunning()
      }
    }
  }
})
</script>
<style lang="scss" >
.activity-list .table{
  margin-bottom:0;
}

.activity-list__eventargs {
  max-width: 250px;
}

.activity-list__eventargs-string {
  display: inline-block;
  overflow-x: auto;
  max-width: 100%;
  white-space: nowrap;
}

.loading-area{
  padding: 50px;
  background: var(--background-color-accent-lvl2);
  font-size: 14px;
  text-align: center;
  .loading-text{
    font-style:italic;
    color: var(--font-color);
  }
}
td.eventtitle.adhoc {
    font-style: italic;
}
.table > tbody > tr > td.eventicon{
  padding:0 0 0 10px;
}
$since-bg: #ccf;
.table tbody.since-count-data{
  background: $since-bg;
  color: white;
  > tr > td{
    padding: 2px;
  }
  > tr:hover{
    background: var(--background-color-accent-lvl2)
  }
}
.running-executions + .history-executions,
.running-executions + .since-count-data,
.since-count-data + .history-executions {
  > tr:first-child > td{
    border-top: 2px solid $since-bg;
  }
}
.progress{
  height: 20px;
  margin:0;
}
.missed {
  background-color: var(--warning-bg-color);
  --text-muted-color: var(--font-color);
  --text-secondary-color: var(--font-color);
}
.spacing-x{
  * + *{
    margin-left: 1rem;
  }
}
</style>

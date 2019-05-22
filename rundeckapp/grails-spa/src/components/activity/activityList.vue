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

                <a :href="activityHref">
                  <span class="summary-count" :class="{ 'text-primary': pagination.total < 1, 'text-info': pagination.total > 0 }" v-if="pagination.total>=0">
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
            <span v-if="runningOpts.allowAutoRefresh">
              <input type=checkbox id=auto-refresh v-model=autorefresh />
              <label for="auto-refresh">{{$t('Auto refresh')}}</label>
            </span>
            <!-- bulk edit controls -->
            <span  v-if="auth.deleteExec && pagination.total>0 && showBulkDelete">
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


              <btn size="xs" type="secondary" v-if="auth.deleteExec && !bulkEditMode" @click="bulkEditMode=true">
                  {{$t('bulk.delete')}}
              </btn>
            </span>
          </div>

    </section>

    <!-- Bulk edit modals -->
    <modal  v-model="showBulkEditCleanSelections" id="cleanselections" :title="$t('Clear bulk selection')">

      <i18n tag="p" path="clearselected.confirm.text">
        <strong>{{bulkSelectedIds.length}}</strong>
      </i18n>

      <div slot="footer">
        <btn data-dismiss="modal">{{$t('cancel')}}</btn>
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

    <modal v-model="showBulkEditConfirm" id="bulkexecdelete" :title="$t('Bulk Delete Executions')">
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
    <div class="card-content-full-width">
    <table class=" table table-hover table-condensed " >
      <tbody  v-if="running && running.executions.length>0" class="running-executions">
        <tr
            v-for="exec in running.executions"
            :key="exec.id"
            class="execution link activity_row autoclickable"
            :class="{nowrunning:!exec.dateCompleted,[exec.status]:true}"
            @click="autoBulkEdit(exec)"
            >
          <!-- #{{exec.id}} -->

            <td class="eventicon" v-if="bulkEditMode">
                <input
                type="checkbox"
                name="bulk_edit"
                :value="exec.id"
                v-model="bulkSelectedIds"
                class="_defaultInput"/>
            </td>
                 <td class="eventicon " :title="executionState(exec.status)" >
                    <i class="fas fa-circle-notch fa-spin text-info"  v-if="exec.status==='running'"></i>
                    <i class="fas fa-clock text-muted " v-else-if="exec.status==='scheduled'"></i>
                    <i class="exec-status icon" :data-execstate="executionStateCss(exec.status)" :data-statusstring="exec.status" v-else></i>
                </td>

                <td class="dateStarted date " v-tooltip="runningStatusTooltip(exec)">
                      <progress-bar v-if="exec.status == 'scheduled'" :value="100" striped  type="default" label :label-text="$t('job.execution.starting.0',[runningStartedDisplay(exec.dateStarted.date)])" />
                      <progress-bar v-else-if="exec.job && exec.job.averageDuration" :value="jobDurationPercentage(exec)" striped active type="info" label min-width/>
                      <progress-bar v-else-if="exec.dateStarted.date" :value="100" striped active type="info" label :label-text="$t('running')" />
                </td>

                <td class="  user text-right " style="white-space: nowrap;">
                    <em><i18n path="by" default="by"/></em>
                    {{exec.user}}
                </td>

                <td class=" eventtitle job" v-if="exec.job" v-tooltip="(exec.job.group ? exec.job.group + '/' +exec.job.name : '')">
                    {{exec.job.name}}
                </td>

                <td class="eventargs" v-if="exec.job">
                  <span v-if="exec.job.options">
                      <span v-for="(value,key) in exec.job.options" :key="key">
                          {{key}}:
                          <code  class="optvalue">{{value}}</code>
                      </span>
                  </span>
                </td>


                <td class="eventtitle adhoc " v-if="!exec.job" colspan="2">
                    {{exec.description}}
                </td>

                <td class="text-right">
                    <a title="View execution output" :href=exec.permalink>#{{exec.id}}</a>
                </td>
            </tr>
        </tbody>
        <tbody class="since-count-data autoclickable"  @click="reload" v-if="sincecount>0">
          <tr>
            <td colspan=6 class=text-center>
                {{ $tc('info.newexecutions.since.0', sincecount) }}
            </td>
          </tr>
        </tbody>
        <tbody class="history-executions" v-if="reports.length > 0">
        <tr class="link activity_row autoclickable"
            @click="autoBulkEdit(rpt)"
            :class="{succeed:rpt.status==='succeed',fail:rpt.status==='fail',highlight:highlightExecutionId==rpt.executionId,job:rpt.jobId,adhoc:!rpt.jobId}"
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
            <td class="eventicon " :title="executionState(rpt.execution.status)" >
                <i class="exec-status icon" :data-execstate="executionStateCss(rpt.execution.status)" :data-statusstring="rpt.execution.status"></i>
            </td>
            <td class="right date " v-tooltip.bottom="$t('info.completed.0',[jobCompletedFormat(rpt.dateCompleted)])">
                <span v-if="rpt.dateCompleted">
                    <span class="timeabs">
                        {{rpt.dateCompleted | moment('from','now')}}
                    </span>
                    <span title="">
                        <span class="text-primary"><i18n path="in.of" default="in"/></span>
                        <span class="duration" data-bind="text: durationHumanize()">{{rpt.duration | duration('humanize')}}</span>
                    </span>
                </span>
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
                  <span v-if="query.jobIdFilter && rpt.jobId && query.jobIdFilter !==rpt.jobId" class="text-secondary">
                    <i class="fas fa-arrow-circle-right-alt"></i>
                    {{$t('Referenced')}}
                  </span>


                <span v-if="rpt.jobDeleted" class="text-primary">
                      {{$t('job.has.been.deleted.0',[rpt.jobName])}}
                </span>

                <span v-if="isCustomStatus(rpt.execution.status)">
                    <span class="exec-status-text custom-status" >{{rpt.execution.status}}</span>
                </span>
            </td>
            <td class="eventargs " >
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

            <td class="text-right">
                <a  :href="rpt.executionHref">
                #{{rpt.executionId}}
                </a>
            </td>
        </tr>
        </tbody>
    </table>
    </div>

    <div v-if="reports.length < 1 " class="loading-area">
        <span class="text-secondary" v-if="!loading && !loadError">
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
import OffsetPagination from '@rundeck/ui-trellis/src/components/utils/OffsetPagination.vue'
import ActivityFilter from './activityFilter.vue'

import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"
import { ExecutionBulkDeleteResponse, ExecutionListRunningResponse, Execution } from 'ts-rundeck/dist/lib/models';
import { setTimeout, clearTimeout } from 'timers';

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


const knownStatusList = ['scheduled','running','succeed','succeeded','failed',
    'cancel','aborted','retry','timedout','timeout','fail'];

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
      running: null as null|ExecutionListRunningResponse,
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
    runningStatusTooltip(exec:Execution){
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
    autoBulkEdit(rpt:any){
      if(!this.bulkEditMode){
        if(rpt.executionHref){
          window.location=rpt.executionHref
        }else if(rpt.permalink){
          window.location=rpt.permalink
        }
      }
      if(rpt.executionId){
        this.toggleSelectId(rpt.executionId)
      }else if(rpt.id){
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
        this.bulkEditProgress=false
        this.bulkEditError=error
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
        
        if(this.lastDate>0  && response.data.since && response.data.since.count ){
            this.sincecount=response.data.since.count
        }
      }catch(error){
        this.loadError = error.message
      }
    },
    async loadRunning(){
      const rundeckContext = getRundeckContext()
      this.loadingRunning=true
      try{
        this.running = await rundeckContext.rundeckClient.executionListRunning(this.projectName)
        this.loadingRunning=false
        this.checkrefresh()
      }catch(error){
        this.loadingRunning=false
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
          this.reports = response.data.reports
          this.eventBus&&this.eventBus.$emit('activity-query-result',response.data)
        }
      }catch(error){
        this.loading=false
        this.loadError=error.message
      }
    },
    changePageOffset(offset:number){
      if (this.loading) {
        return;
      }
      this.loadActivity(offset)
    },
    checkrefresh(){
      if(!this.loadingRunning && this.autorefresh){
        this.autorefreshtimeout = setTimeout(()=>{
          // this.reload();
          // this.loadActivity(this.pagination.offset)
          this.loadRunning()
          this.loadSince()
        }, this.loadError?(this.autorefreshms*10):this.autorefreshms);
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
      this.autorefreshms = window._rundeck.data['autorefreshms']
      
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
      if(this.runningOpts['loadRunning']){
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
.loading-area{
  padding: 50px;
  background: #efefefef;
  font-size: 14px;
  text-align: center;
  .loading-text{
    font-style:italic;
    color: #bbbbbb;
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
    background: darken($color: $since-bg, $amount: 20%)
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
</style>

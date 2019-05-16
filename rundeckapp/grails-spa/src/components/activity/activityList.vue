<template>
  <div class="row">
    <div class="col-xs-12">
      <div class="card">
        <div class="card-content">

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

            <dropdown v-if="query.recentFilter!=='-'">
              <span class="dropdown-toggle text-info">
                <i18n :path="'period.label.'+period.name"/>
                <span class="caret"></span>
              </span>
              <template slot="dropdown">
                <li v-for="perobj in periods" :key="perobj.name">
                  <a role="button" @click="changePeriod(perobj)">
                  <i18n :path="'period.label.'+perobj.name"/>
                  <span v-if="period.name===perobj.name">√</span>
                  </a>
                </li>
              </template>
            </dropdown>

          <activity-filter  v-model="query"></activity-filter>
          <!-- bulk edit controls -->
          <div class="pull-right" v-if="auth.deleteExec && pagination.total>0">
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

    <table class=" table table-hover table-condensed " v-if="reports.length > 0">
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
          :disabled="loading"
          :showPrefix="false"
        >
        </offset-pagination>
            </div>
        </div>
    </div>

  </div>
</template>

<script lang="ts">
import axios from 'axios'
import Vue from 'vue'
import OffsetPagination from '@rundeck/ui-trellis/src/components/utils/OffsetPagination.vue'
import ActivityFilter from './activityFilter.vue'

import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"
import { ExecutionBulkDeleteResponse } from 'ts-rundeck/dist/lib/models';

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
    'queryParams',
    'eventBus',
    'displayMode'
  ],
  data () {
    return {
      projectName:'',
      activityPageHref:'',
      reports:[],
      lastDate:-1,
      pagination:{
        offset:0,
        max:10,
        total:-1
      },
      loading: false,
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
      bulkDeleteUrl:'',
      auth:{
        projectAdmin:false,
        deleteExec:false
      },
      query: {
        jobFilter:'',
        jobIdFilter:'',
        userFilter:'',
        execNodeFilter:'',
        titleFilter:'',
        statFilter:'',
        recentFilter:''
      },
      period:{name:'All',params:{}},
      periods: [
        {name:'All',params:{recentFilter:''}},
        {name:'Hour',params:{recentFilter:'1h'}},
        {name:'Day',params:{recentFilter:'1d'}},
        {name:'Week',params:{recentFilter:'1w'}},
        {name:'Month',params:{recentFilter:'1m'}},
       ] as {[name:string]:any}[]
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
    reload(){
      this.reports=[]
      this.pagination.total=-1
      this.lastDate=-1
      this.loadActivity(0)
    },
    changePeriod(period: any){
      this.period=period
      this.query.recentFilter=period.params.recentFilter
      this.reload()
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
      this.pagination.offset=offset
      try{
        const response = await axios.get(this.activityUrl,{
          headers: {'x-rundeck-ajax': true},
          params: Object.assign({offset: offset, max: this.pagination.max},this.query),
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

  },
  watch:{
    query:{
      handler(newValue,oldValue){
        console.log("changed query",newValue)
        this.reload()
      },
      deep:true
    }
  },
  computed:{
    activityHref():string {
      return _genUrl(this.activityPageHref , this.period.params)
    }
  },
  mounted () {
    if(window._rundeck.data.jobslistDateFormatMoment){
      this.momentJobFormat=window._rundeck.data.jobslistDateFormatMoment
    }
    if(this.queryParams){
      // this.internalquery=this.queryParams
    }

    this.projectName=window._rundeck.projectName
    if (window._rundeck && window._rundeck.data) {
      this.auth.projectAdmin=window._rundeck.data['projectAdminAuth']
      this.auth.deleteExec=window._rundeck.data['deleteExecAuth']
      this.activityUrl = window._rundeck.data['activityUrl']
      this.bulkDeleteUrl = window._rundeck.data['bulkDeleteUrl']
      this.activityPageHref=window._rundeck.data['activityPageHref']
      if(window._rundeck.data['pagination'] && window._rundeck.data['pagination'].max){
        this.pagination.max=window._rundeck.data['pagination'].max
      }
      this.loadActivity(0)
    }
  }
})
</script>
<style lang="scss" >
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
</style>

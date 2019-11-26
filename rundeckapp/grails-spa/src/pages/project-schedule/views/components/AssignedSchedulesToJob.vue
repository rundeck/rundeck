<template>
  <div class="assign-schedule-job">
    <div v-for="scheduleDef in currentlyAssigned" class="col-sm-6">
      <ul class="options">
        <li class="el-collapse-item scheduleEntry">
          <div class="opt item ">
            <span>{{scheduleDef.name}} - {{scheduleDef.cronString}}</span>
            <span class="btn btn-xs btn-danger pull-right" @click="controlDeletePanelFor(scheduleDef, true)" title="delete">
              <i class="glyphicon glyphicon-remove"></i>
            </span>
          </div>

          <div v-if="deletePanelOpenFor == scheduleDef.name" class="panel panel-danger">
            <div class="panel-heading">
              <span>{{$t('Delete this Schedule?')}}</span>
            </div>
            <div class="panel-footer">
              <span class="btn btn-default btn-xs" @click="controlDeletePanelFor(scheduleDef, false)">{{$t('Cancel')}}</span>
              <span class="btn btn-danger btn-xs"  @click="deAssignScheduleDef(scheduleDef)">{{$t('Delete')}}</span>
            </div>
          </div>

        </li>
      </ul>
    </div>
    <div class="col-sm-12" style="margin-top: 10px">
      <span class="btn btn-default btn-sm ready" title="Associate Scheduled Definition" id="scheduleAssociate"
        @click="showAssignScheduleModal=true,getScheduleDefList(0)">
          <b class="glyphicon glyphicon-plus"></b>
          {{$t('Associate Schedule Definition')}}
      </span>
    </div>
    <modal v-model="showAssignScheduleModal" id="assignScheduleModal" :title="$t('Assign Schedules to job')" @hide="closeAssign">
      <div class="row">
        <div class="card ">
          <div class="col-xs-12">
            <div class="list-group">
              <a href="#" class="list-group-item" v-for="schedule in scheduledDefinitions"  @click="selectedSchedule=schedule">
                <span>{{schedule.name}}</span>
              </a>
            </div>
          </div>
        </div>
        <offset-pagination
          :pagination="pagination"
          @change="changePageOffset($event)"
          :disabled="loading"
          :showPrefix="false"
        ></offset-pagination>
      </div>
    </modal>
  </div>
</template>

<script>
    import {getAllProjectSchedules} from "../../scheduleDefinition";
    import OffsetPagination from '@rundeck/ui-trellis/src/components/utils/OffsetPagination.vue'

    export default {
        name: "AssignedSchedulesToJob",
        props: ['eventBus','jobName'],
        components: {
            OffsetPagination
        },
        data : function() {
            return {
                showAssignScheduleModal: false,
                currentlyAssigned: [],
                deletePanelOpenFor: null,
                loading: false,
                scheduleSearchResult: null,
                scheduledDefinitions: null,
                pagination:{
                    offset:0,
                    max:100,
                    total:-1
                },
                selectedSchedule: null
            }
        },
        methods:{
            async getScheduleDefList(offset){
                this.loading = true;
                this.pagination.offset = offset;

                var currScheduleDefNames = [];
                this.currentlyAssigned.forEach(schedule => {
                    console.log(schedule)
                    console.log("testetst")
                    currScheduleDefNames.push(schedule.name);
                });

                this.scheduleSearchResult = await getAllProjectSchedules(this.pagination.offset, null,currScheduleDefNames)
                this.scheduledDefinitions = this.scheduleSearchResult.schedules
                this.pagination.max = this.scheduleSearchResult.maxRows
                this.pagination.total = this.scheduleSearchResult.totalRecords
                this.loading = false
            },
            changePageOffset(offset) {
                if (this.loading) {
                    return;
                }
                this.getScheduleDefList(offset)
            },
            closeAssign(pressedButtonName){
                if('ok' == pressedButtonName){
                    _addScheduleDefinitions(this.selectedSchedule);
                    console.log(this.selectedSchedule)
                    this.currentlyAssigned.push(this.selectedSchedule);
                }
                this.showAssignScheduleModal=false
            },
            controlDeletePanelFor(scheduleDef, open){
                if(open){
                    this.deletePanelOpenFor = scheduleDef.name;
                } else {
                    this.deletePanelOpenFor = null;
                }
            },
            deAssignScheduleDef(scheduleDef){
                _removeScheduleDefinitions(scheduleDef);

                this.currentlyAssigned = this.currentlyAssigned.filter(function(item) {
                    return item.name != scheduleDef.name;
                });

                this.controlDeletePanelFor(scheduleDef, false);
            },
            loadJsonData(id) {//TODO: move to util, this is a copy of a function on application.js
                var dataElement = document.getElementById(id);
                // unescape the content of the span
                if (!dataElement) {
                    return null;
                }
                var jsonText = dataElement.textContent || dataElement.innerText;
                return jsonText && jsonText != '' ? JSON.parse(jsonText) : null;
            }
        },
        mounted() {
            var dbAssignedSchedules = this.loadJsonData('scheduleDataList');
            dbAssignedSchedules.forEach(schedule => {
                this.currentlyAssigned.push(schedule)
            })
        },
        computed:{}
    }
</script>

<style scoped>

</style>

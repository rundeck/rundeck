<template>
  <div class="assign-schedule-job">
    <span class="btn btn-default btn-sm ready" title="Associate Scheduled Definition" id="scheduleAssociate"
      @click="showAssignScheduleModal=true,getCurrentSchedules(0)">
        <b class="glyphicon glyphicon-plus"></b>
        {{$t('Associate Schedule Definition')}}
    </span>
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
            async getCurrentSchedules(offset){
                this.loading = true;
                this.currentlyAssigned = []
                var assignedSchedules = jQuery.parseJSON(jQuery('#scheduleDataListJSON').val())
                assignedSchedules.forEach(schedule => {
                    this.currentlyAssigned.push(schedule.name)
                })
                this.pagination.offset = offset
                this.scheduleSearchResult = await getAllProjectSchedules(this.pagination.offset, null, this.currentlyAssigned)
                this.scheduledDefinitions = this.scheduleSearchResult.schedules
                this.pagination.max = this.scheduleSearchResult.maxRows
                this.pagination.total = this.scheduleSearchResult.totalRecords
                this.loading = false
            },
            changePageOffset(offset) {
                if (this.loading) {
                    return;
                }
                this.getCurrentSchedules(offset)
            },
            closeAssign(pressedButtonName){
                if('ok' == pressedButtonName){
                    var nameToAdd = this.selectedSchedule.name
                    _addScheduleDefinitions({"name":nameToAdd})
                }
                this.showAssignScheduleModal=false
            }
        },
        mounted() {
        },
        computed:{}
    }
</script>

<style scoped>

</style>

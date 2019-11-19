<template>
  <div>
    <modal id="assignedJobsModal" v-model="showAssignedJobsModal" :title="$t('Assigned Jobs To ' + schedule.name)" size="lg" :footer=false @hide="close()">
      <div class="modal-body">
        <div class="row">
          <div class="card ">

            <div class="col-xs-12">
              <div class="list-group">
                <a href="#" class="list-group-item" v-for="job in jobSearchResult"  >
                  <span>{{job.name}}</span>
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
      </div>
    </modal>
  </div>
</template>

<script>

    import axios from 'axios'
    import OffsetPagination from '@rundeck/ui-trellis/src/components/utils/OffsetPagination.vue'
    import {
        getJobsAssociated
    } from "../scheduleDefinition";

    export default {
        name: "AssignedJobs",
        props: ['eventBus', 'schedule'],
        components: {
            OffsetPagination
        },
        data : function() {
            return {
                loading: false,
                showAssignedJobsModal: true,
                pagination:{
                    offset:0,
                    max:100,
                    total:-1
                },
                jobSearchResult: [],
            }
        },
        async mounted() {
            this.getJobsAssociated(0)
            console.log(this.jobSearchResult)
        },
        methods:{
            close(){
                this.eventBus.$emit('closeAssignedJobsModal', {})
            },
            async getJobsAssociated(offset){
                this.loading = true
                this.pagination.offset = offset
                var result = await getJobsAssociated(this.pagination.offset, this.schedule.name)
                this.pagination.max = result.maxRows
                this.pagination.total = result.totalRecords
                this.jobSearchResult = result.scheduledExecutions
                this.loading = false
            },
            changePageOffset(offset) {
                if (this.loading) {
                    return;
                }
                this.getJobsAssociated(offset)
            },
        }
    }
</script>

<style scoped>

</style>

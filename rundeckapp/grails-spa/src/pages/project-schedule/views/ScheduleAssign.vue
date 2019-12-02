<template>
  <div>
    <modal v-model="showInnerModal" v-bind:header="false" v-bind:footer="true" size="lg" @hide="startClosing">
      <div class="modal-body">
        <div class="row">
          <div class="card ">
            <div class="input-group input-group-sm">
              <input type="search" name="name" :placeholder="$t('placeholder.searchByName')" class="form-control input-sm"
                     v-model="searchFilters.name"/>
              <span class="input-group-addon"><i class="glyphicon glyphicon-search "></i></span>
            </div>
            <div class="col-xs-12">
              <div class="list-group">
                <a href="#" class="list-group-item" @click="isAlreadyAssociated(job.id) ? toggleDeassociation(job.id) : toggleAssociation(job.id)" v-for="job in jobSearchResult"  >
                  <span v-if="!isScheduledForDeassociation(job.id)">{{job.name}}</span>
                  <s v-if="isScheduledForDeassociation(job.id)">{{job.name}}</s>
                  <span v-if="isScheduledForDeassociation(job.id)" class="glyphicon glyphicon-minus text-danger"></span>
                  <span v-if="isScheduledForAssociation(job.id)" class="glyphicon glyphicon-plus text-success"></span>
                  <span v-if="isAlreadyAssociated(job.id) && !isScheduledForDeassociation(job.id)" class="glyphicon glyphicon-ok"></span>
                </a>
              </div>
            </div>
          </div>
          <hr/>
          <offset-pagination
            :pagination="pagination"
            @change="changePageOffset($event)"
            :disabled="loading"
            :showPrefix="false"
          ></offset-pagination>
          <hr/>
          <div class="col-xs-6">
            <h4><span>{{$t('label.assignUnassign')}}:</span></h4>
            <div class="list-group" style="margin-bottom: 0px;">
              <a href="#" class="list-group-item" @click="toggleDeassociation(jobUuid)" v-for="jobUuid in jobIdsToDeassociate"  >
                <s v-if="isScheduledForDeassociation(jobUuid)">{{getFromCurrentlyAssociated(jobUuid).name}}</s>
                <span class="glyphicon glyphicon-minus text-danger"></span>
              </a>
            </div>
            <div class="list-group">
              <a href="#" class="list-group-item" @click="toggleAssociation(job.id)" v-for="job in jobsToAssociate"  >
                <span>{{job.name}}</span>
                <span class="glyphicon glyphicon-plus text-success"></span>
              </a>
            </div>
          </div>

          <div class="col-xs-6">
            <h4><span>{{$t('label.currentlyAssigned')}}:</span></h4>
            <div class="list-group">
              <a href="#" class="list-group-item" @click="toggleDeassociation(job.uuid)" v-for="job in schedule.scheduledExecutions"  >
                <span v-if="!isScheduledForDeassociation(job.uuid)">{{job.name}}</span>
                <s v-if="isScheduledForDeassociation(job.uuid)">{{job.name}}</s>
                <span v-if="!isScheduledForDeassociation(job.uuid)" class="glyphicon glyphicon-ok"></span>
                <span v-if="isScheduledForDeassociation(job.uuid)" class="glyphicon glyphicon-minus text-danger"></span>
              </a>
            </div>
          </div>
        </div>
      </div>
    </modal>
  </div>
</template>
<script>

    import Vue from "vue";
    import OffsetPagination from '@rundeck/ui-trellis/src/components/utils/OffsetPagination.vue'
    import axios from 'axios'
    import JobConfigPicker from '@rundeck/ui-trellis/src/components/plugins/JobConfigPicker'

    export default {
        name: 'ScheduleAssign',
        props: ['schedule', 'eventBus'],
        components: {JobConfigPicker, OffsetPagination},
        data: function () {
            return {
                showInnerModal: true,
                searchFilters: {
                    name: ""
                },
                jobSearchResult: [],
                jobsToAssociate: [],
                jobIdsToDeassociate: [],
                pagination:{
                  offset:0,
                  max:4,
                  total:-1
                },
                loading: false
            }
        },
        async mounted() {
            this.updateSearchResults(0);
        },
        watch: {
            'searchFilters.name': function (name) {
                this.updateSearchResults(this.pagination.offset);
            }
        },
        methods: {
            changePageOffset(offset) {
              if (this.loading) {
                return;
              }
              this.updateSearchResults(offset)
            },
            updateSearchResults(offset) {
                this.pagination.offset = offset
                this.loading = true;
                axios({
                    method: 'get',
                    headers: {'x-rundeck-ajax': true},
                    url: `/menu/jobsSearchJson`,
                    params: {
                        jobFilter: this.searchFilters.name,
                        projFilter: window._rundeck.projectName,
                        offset: this.pagination.offset,
                        max: this.pagination.max
                    },
                    withCredentials: true
                }).then((response) => {
                    this.jobSearchResult = []
                    if (response.data) {
                        this.jobSearchResult = response.data.jobs
                        this.pagination.total = response.data.total
                        this.pagination.offset = response.data.offset
                        this.pagination.max = response.data.max
                    }
                    this.loading = false
                })
            },
            startClosing(pressedButtonName) {
                if ('ok' == pressedButtonName) {
                    axios({
                        method: 'post',
                        headers: {'x-rundeck-ajax': true},
                        url: `/projectSchedules/reassociate`,
                        data: {
                            scheduleDefId: this.schedule.id,
                            jobUuidsToAssociate: this.jobsToAssociate.map(job => job.id),
                            jobUuidsToDeassociate: this.jobIdsToDeassociate
                        },
                        params: {
                            project: window._rundeck.projectName
                        },
                        withCredentials: true
                    }).then((response) => {
                        if (response.status >= 200 && response.status < 300) {
                            this.eventBus.$emit('SCHEDULE_ASSIGN_CLOSING', {});
                        }
                    });
                } else {
                    this.eventBus.$emit('SCHEDULE_ASSIGN_CLOSING', {});
                }


            },
            isScheduledForDeassociation(jobUuid) {
                return this.jobIdsToDeassociate.indexOf(jobUuid) > -1;
            },
            isScheduledForAssociation(jobUuid) {
                return this.jobsToAssociate.some(job => job.id == jobUuid);
            },
            isAlreadyAssociated(jobUuid) {
                if(this.schedule.scheduledExecutions)
                return this.schedule.scheduledExecutions.some((job) => job.uuid == jobUuid);
            },
            getFromCurrentlyAssociated(jobUuid) {
                if(this.schedule.scheduledExecutions)
                return this.schedule.scheduledExecutions.find(jobCandidate => jobCandidate.uuid == jobUuid);
            },
            toggleAssociation(jobUuid) {
                if (!this.isScheduledForAssociation(jobUuid)) {
                    let job = this.jobSearchResult.find(jobCandidate => jobCandidate.id == jobUuid);

                    this.jobsToAssociate.push(job);
                } else {
                    let jobIndex = this.jobsToAssociate.findIndex(jobCandidate => jobCandidate.id == jobUuid);

                    this.jobsToAssociate.splice(jobIndex, 1);
                }
            },
            isScheduledForDeassociation(jobId) {
                return this.jobIdsToDeassociate.indexOf(jobId) > -1
            },
            toggleDeassociation(jobUuid) {
                if (!this.isScheduledForDeassociation(jobUuid)) {
                    this.jobIdsToDeassociate.push(jobUuid);
                } else {
                    this.jobIdsToDeassociate.splice(
                        this.jobIdsToDeassociate.indexOf(jobUuid), 1
                    );
                }
            }
        }
    }
</script>


<template>
  <div v-if="project">
    <div class="col-xs-12">
      <div class="card">
        <div class="input-group search-bar">
          <input type="search" name="name" placeholder="Project Schedule search: type name" class="form-control input-sm" v-model="searchFilters.name"/>
          <span class="input-group-addon"><i class="glyphicon glyphicon-search "></i></span>
        </div>
        <div class="card-content ">
          <div v-if="loading" class="project_list_item">
            <b class="fas fa-spinner fa-spin loading-spinner text-muted fa-2x"></b>
          </div>
          <div class="card-content-full-width">
            <table class=" table table-hover table-condensed " >
              <tbody  v-if="scheduledDefinitions && scheduledDefinitions.length>0" class="history-executions">
                <tr v-for="projectSchedule in scheduledDefinitions">
                  <td class="eventicon" v-if="bulkDelete">
                    <input
                      type="checkbox"
                      name="bulk_edit"
                      :value="projectSchedule.id"
                      v-model="bulkSelectedIds"
                      class="_defaultInput"/>
                  </td>
                  <td>
                    <a href="#" class="text-h3  link-hover  text-inverse project_list_item_link"
                      @click="openAssignedJobs(projectSchedule)">
                      <small>{{projectSchedule.name}}</small>
                      <small class="text-secondary text-base"><em>{{projectSchedule.description}}</em></small>
                    </a>
                  </td>
                  <td>
                    <small>{{getCronExpression(projectSchedule)}}</small>
                  </td>
                  <td>
                    <div class="pull-right">
                      <div class="btn-group dropdown-toggle-hover">
                        <a href="#" class="as-block link-hover link-block-padded text-inverse dropdown-toggle" data-toggle="dropdown">
                          <span>{{$t("button.actions")}}</span>
                          <span class="caret"></span>
                        </a>
                        <ul class="dropdown-menu pull-right" role="menu">
                          <li>
                            <a href="#"
                               @click="openSchedulePersistModal(projectSchedule)">
                              <span>{{$t("button.editSchedule")}}</span>
                            </a>
                          </li>
                          <li class="divider"></li>
                          <li>
                            <a href="#" @click="openScheduleAssign(projectSchedule)">
                              <i class="glyphicon glyphicon-plus"></i>
                              <span>{{$t("button.assignToJobs")}}</span>
                            </a>
                          </li>
                          <li class="divider"></li>
                          <li>
                            <a href="#" @click="scheduleToDelete=projectSchedule,showDeleteConfirm=true">
                              <i class="glyphicon glyphicon-minus"></i>
                              <span>{{$t("button.deleteSchedule")}}</span>
                            </a>
                          </li>
                        </ul>
                      </div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <offset-pagination
            :pagination="pagination"
            @change="changePageOffset($event)"
            :disabled="loading"
            :showPrefix="false"
          ></offset-pagination>
        </div>
      </div>
      <br>
      <div class="row col-xs-12" >
        <div class=" btn-wrapper pull-right">
          <button v-if="bulkDelete" href="#" class="btn btn-default btn-xs" @click="showBulkEditConfirm=true" :disabled="bulkSelectedIds.length  < 1">
            <i class="btn-fill btn btn-danger btn-xs"></i>
            <span v-if="bulkDelete">{{$t("button.deleteSelected")}}</span>
          </button>
          <button href="#" class="btn btn-default btn-xs" @click="switchBulkDelete(bulkDelete)">
            <i class="glyphicon glyphicon-trash"></i>
            <span v-if="!bulkDelete">{{$t("button.bulkDelete")}}</span>
            <span v-else-if="bulkDelete">{{$t("button.cancelBulkDelete")}}</span>
          </button>
        </div>
        <div class=" btn-wrapper pull-right">
          <button href="#" class="btn btn-default btn-xs" @click="openUploadDefinitionModal()">
            <i class="glyphicon glyphicon-upload"></i>
            <span>{{$t("button.uploadScheduleDefinitions")}}</span>
          </button>
        </div>
        <div class=" btn-wrapper pull-right">
          <button href="#" class="btn btn-default btn-xs" @click="openSchedulePersistModal(null)">
            <i class="glyphicon glyphicon-upload"></i>
            <span>{{$t("button.createDefinition")}}</span>
          </button>
        </div>
      </div>
    </div>
    <schedule-assign
      v-if="showScheduleAssign"
      v-bind:schedule="activeSchedule"
      v-bind:event-bus="eventBus"/>
    <schedule-persist
      v-if="showEditSchedule"
      v-bind:event-bus="eventBus"
      v-bind:schedule="activeSchedule">
    </schedule-persist>
    <schedule-upload
      v-if="showUploadDefinitionModal"
      v-bind:event-bus="eventBus">
    </schedule-upload>
    <assigned-jobs-modal
      v-if="showAssignedJobsModal"
      v-bind:event-bus="eventBus"
      v-bind:schedule="activeSchedule">
    </assigned-jobs-modal>
    <message-modal
      v-if="messagesModal.showMessageModal"
      v-bind:event-bus="eventBus"
      v-bind:success="messagesModal.success"
      v-bind:messages="messagesModal.messages"
    ></message-modal>

    <modal v-model="showBulkEditConfirm" id="bulkexecdelete" :title="$t('title.bulkDeleteSchedules')">
      <i18n tag="p" path="button.deleteConfirm">
        <strong>{{bulkSelectedIds.length}}</strong>
        <span>{{$tc('scheduleDefinitions',bulkSelectedIds.length)}}</span>
      </i18n>

      <div slot="footer">
        <btn @click="showBulkEditConfirm=false">
          {{$t('cancel')}}
        </btn>
        <btn type="danger"
             @click="bulkDeleteSchedules"
             data-dismiss="modal">
          {{$t('Delete Selected')}}
        </btn>
      </div>
    </modal>

    <modal v-model="showDeleteConfirm" id="deleteConfirm" :title="$t('title.deleteSchedules')">
      <i18n tag="p" path="button.deleteConfirm">
        <strong>{{scheduleToDelete.name}}</strong>
        <span>{{$tc('scheduleDefinitions',scheduleToDelete.name)}}</span>
      </i18n>

      <assigned-jobs-data v-if="showDeleteConfirm"
        v-bind:event-bus="eventBus"
        v-bind:schedule="computedScheduleToDelete"
      ></assigned-jobs-data>

      <div slot="footer">
        <btn @click="showDeleteConfirm=false">
          {{$t('cancel')}}
        </btn>
        <btn type="danger"
             @click="deleteSchedule(), showDeleteConfirm=false"
             data-dismiss="modal">
          {{$t('Delete Selected')}}
        </btn>
      </div>
    </modal>

  </div>

</template>


<script>

    import axios from 'axios'
    import OffsetPagination from '@rundeck/ui-trellis/src/components/utils/OffsetPagination.vue'
    import SchedulePersist from './SchedulePersist.vue'
    import ScheduleAssign from "@/pages/project-schedule/views/ScheduleAssign.vue"
    import Vue from "vue"
    import AssignedJobsModal from "./AssignedJobsModal";
    import {
      bulkDeleteSchedules,
      getAllProjectSchedules, getCronExpression,
      StandardResponse
    } from "../scheduleDefinition";
    import ScheduleUpload from "./ScheduleUpload";
    import MessageModal from "./MessageModal";
    import AssignedJobsData from "./components/AssignedJobsData";

    export default Vue.extend({
        name: 'ScheduleDefinitionsView',
        props: [ 'eventBus' ],
        components:{
            AssignedJobsData,
            MessageModal,
            ScheduleUpload,
            ScheduleAssign,
            OffsetPagination,
            SchedulePersist,
            AssignedJobsModal

        },
        data : function() {
            return {
                showAssignedJobsModal: false,
                showUploadDefinitionModal: false,
                scheduleSearchResult: null,
                showEditSchedule: false,
                scheduledDefinitions: null,
                loading: false,
                project: "",
                rdBase: "",
                filteredProjectSchedules: "",
                searchFilters: {
                    name: ""
                },
                pagination:{
                    offset:0,
                    max:100,
                    total:-1
                },

                //element control
                showScheduleAssign: false,
                activeSchedule: {},
                bulkDelete: false,
                bulkSelectedIds: [],
                responseFromDelete: StandardResponse,
                showBulkEditConfirm: false,
                messagesModal: {
                    messages: null,
                    success: null,
                    showMessageModal: false
                },
                showDeleteConfirm: false,
                scheduleToDelete: {}
            }
        },
        async mounted() {
            if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
                this.project = window._rundeck.projectName
                await this.updateSearchResults(0);
            }
            this.eventBus.$on('SCHEDULE_ASSIGN_CLOSING', (payload) => {
                this.doCloseScheduleAssign();
            });
            this.eventBus.$on('closeSchedulePersistModal', (payload) =>{
                this.closeSchedulePersistModal(payload.reload)
            });
            this.eventBus.$on('closeUploadDefinitionModal', (payload) =>{
                this.closeUploadDefinitionModal(payload.reload)
            });
            this.eventBus.$on('closeAssignedJobsModal', (payload) =>{
               this.showAssignedJobsModal = false
            });
            this.eventBus.$on('closeMessagesModal', (payload) =>{
                this.messagesModal.messages = null
                this.messagesModal.success = null
                this.messagesModal.showMessageModal = false
            });
        },
        watch: {
            'searchFilters.name': function(val, preVal){
                this.updateSearchResults(0);
            }
        },
        methods: {
            changePageOffset(offset) {
                if (this.loading) {
                    return;
                }
                this.updateSearchResults(offset)
            },
            async updateSearchResults(offset) {
                this.loading = true;

                this.pagination.offset = offset
                this.scheduleSearchResult = await getAllProjectSchedules(this.pagination.offset, this.searchFilters.name, null)
                this.scheduledDefinitions = this.scheduleSearchResult.schedules
                this.pagination.max = this.scheduleSearchResult.maxRows
                this.pagination.total = this.scheduleSearchResult.totalRecords

                this.loading = false
            },
            openScheduleAssign(schedule) {
                this.assignActiveSchedule(schedule)
                this.showScheduleAssign = true;
            },
            assignActiveSchedule(schedule){
                this.activeSchedule = schedule;
            },
            doCloseScheduleAssign() {
                this.showScheduleAssign = false;
                this.updateSearchResults(this.pagination.offset)
            },
            openSchedulePersistModal(schedule){
                this.assignActiveSchedule(schedule)
                this.showEditSchedule = true
            },
            closeSchedulePersistModal(reload){
                this.showEditSchedule = false
                this.updateSearchResults(this.pagination.offset)
            },
            getCronExpression(schedule){
                return getCronExpression(schedule)
            },
            deleteSchedule(){
                axios({
                    method: 'post',
                    headers: {'x-rundeck-ajax': true},
                    url: `/projectSchedules/deleteSchedule`,
                    params: {
                        project: window._rundeck.projectName
                    },
                    data: {
                        schedule: this.scheduleToDelete
                    },
                    withCredentials: true
                }).then((response) => {
                    this.scheduleToDelete = {}
                    this.updateSearchResults(this.pagination.offset)
                })

            },
            openUploadDefinitionModal(){
                this.showUploadDefinitionModal = true
            },
            closeUploadDefinitionModal(reload){
                this.showUploadDefinitionModal = false
                if(reload){
                    this.updateSearchResults(this.pagination.offset)
                }
            },
            switchBulkDelete(bulkDelete){
                this.bulkDelete = !bulkDelete
            },
            async bulkDeleteSchedules(){
              this.responseFromDelete = await bulkDeleteSchedules(this.bulkSelectedIds)
              if(this.responseFromDelete.success){
                this.bulkSelectedIds = []
                this.bulkDelete = false
              }
              this.showBulkEditConfirm = false
              this.updateSearchResults(0)
            },
            openAssignedJobs(projectSchedule){
                this.activeSchedule = projectSchedule
                this.showAssignedJobsModal = true
            },
            closePopup() {
                this.messagesModal.messages = null
                this.messagesModal.success = null
                this.messagesModal.showMessageModal = false
            },
        },
        computed: {
            computedScheduleToDelete: function () {
                return this.scheduleToDelete
            }
        }
    });
</script>



<style scoped lang="scss">
  table.table-condensed td{
    padding: 2px 20px;
  }
  .table{
    margin-bottom: 0px;
  }
  a.dropdown-toggle{
    padding: 2px;
  }
  div.card-content.compact {
    padding : 5px;
  }
  div.search-bar {
    margin-bottom: 0px;
  }
  div.btn-wrapper {
    margin-left: 4px;
  }
</style>

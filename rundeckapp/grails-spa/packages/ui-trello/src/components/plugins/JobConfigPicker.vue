<!--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <div>
    <btn @click="modalOpen=true" :class="btnClass" :size="btnSize" :type="btnType">
      <slot>Choose A Job &hellip;</slot>
    </btn>

    <modal v-model="modalOpen" :title="'Choose A Job'" ref="modal" append-to-body :size="size">

      <div v-if="showProjectSelector"><label>Project:</label><project-picker v-model="project"></project-picker></div>

      <div v-if="showScheduledToggle" class="form-group">

        <select v-model="filterType" id="_job_config_picker_scheduled_filter" class="form-control">
          <option value="">All Jobs</option>
          <option value="scheduled">Scheduled Jobs</option>
          <option value="notscheduled">Non-Scheduled Jobs</option>
        </select>

      </div>
      <div class="list-group" v-for="(item,name) in jobTree.groups" :key="'group'+name">
        <div class="list-group-item" v-if="name && item.jobs.length>0">
           <h4 class="list-group-item-heading">{{item.label}}</h4>
        </div>
        <div v-for="job in item.jobs" :key="job.id"
             class="list-group-item"
             style="overflow:hidden; text-overflow: ellipsis; white-space: nowrap"
             >

             <a href="#" class="" :title="'Choose this job: '+job.id"
                   @click="selectedJob=job">
                 <i class="glyphicon glyphicon-book"></i>
                 {{job.name}}
             </a>


            <span class="text-secondary">
              {{job.description}}
            </span>
            <span class="text-muted" v-if="job.scheduled">
              <i class="glyphicon glyphicon-time"></i>
            </span>

        </div>
      </div>
      <div slot="footer">
        <btn @click="modalOpen=false">Cancel</btn>
      </div>

    </modal>
  </div>
</template>
<script lang="ts">
import { JobReference } from '../../interfaces/JobReference'
import { JobTree } from '../../types/JobTree'
import { GroupedJobs, TreeItem } from '../../types/TreeItem'
import { Job } from '@rundeck/client/dist/lib/models'
import Vue from 'vue'
import ProjectPicker from './ProjectPicker.vue'
import { Component, Prop, Watch } from 'vue-property-decorator'
import { client } from '../../modules/rundeckClient'

Vue.component("project-picker",ProjectPicker)

@Component
export default class JobConfigPicker extends Vue {
  @Prop({ required: false, default: '' })
  value!: string
  @Prop({ required: false, default: '' })
  size!: string
  @Prop({ required: false, default: "" })
  btnType!: string
  @Prop({ required: false, default: "" })
  btnSize!: string
  @Prop({ required: false, default: "" })
  btnClass!: string
  @Prop({required:false,default:true})
  showScheduledToggle!:boolean
  @Prop({required:false,default:false})
  showScheduledDefault!:boolean

  selectedJob: JobReference | null = null
  modalOpen: boolean = false
  jobs: Job[] = []
  jobTree: JobTree = new JobTree()
  project: string = ''
  showProjectSelector: boolean = true
  filterType: string = this.showScheduledDefault?'scheduled':''

@Watch('project')
@Watch('filterType')
  loadJobs() {
    if(this.project != '') {
      let params:{[name: string ] : any} = {}

      if(this.filterType!=''){
        params['scheduledFilter']=(this.filterType==='scheduled')
      }

      client.jobList(this.project,params ).then(result => {
        this.$set(this,'jobTree', new JobTree())
        this.$set(this,'jobs', result)
        this.jobs.forEach(job => this.jobTree.insert(job))
      })
    }
  }

  @Watch('selectedJob')
  jobChosen() {
    this.modalOpen = false
    this.$emit('input', this.selectedJob ? this.selectedJob.id : '')
  }

  mounted() {
    if(window._rundeck.projectName) {
      this.showProjectSelector = false
      this.project = window._rundeck.projectName
    }
  }
}
</script>
<style lang="scss">
</style>

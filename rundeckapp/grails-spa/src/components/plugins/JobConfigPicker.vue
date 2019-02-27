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
    <btn @click="modalOpen=true">
      <slot>{{$t('choose a job')}} &hellip;</slot>
    </btn>

    <modal v-model="modalOpen" :title="$t('choose a job')" ref="modal" append-to-body>

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


            <span class="text-primary">
              {{job.description}}
            </span>


        </div>
      </div>
      <div slot="footer">
        <btn @click="modalOpen=false">{{$t('cancel')}}</btn>
      </div>

    </modal>
  </div>
</template>
<script lang="ts">
import { getProjectJobs, JobReference } from '@/services/jobService'
import { JobTree } from '@/utilities/JobTree'
import { GroupedJobs, TreeItem } from '@/utilities/TreeItem'
import { Job } from 'ts-rundeck/dist/lib/lib/models'
import Vue from 'vue'
import { Component, Prop, Watch } from 'vue-property-decorator'



@Component
export default class JobConfigPicker extends Vue {
  @Prop({ required: false, default: '' })
  value!: string

  selectedJob: JobReference | null = null
  modalOpen: boolean = false
  jobs: Job[] = []
  jobTree: JobTree = new JobTree()

  loadJobs() {
    getProjectJobs().then(result => {
      this.jobs = result
      this.jobs.forEach(job => this.jobTree.insert(job))
    })
  }

  @Watch('selectedJob')
  jobChosen() {
    this.modalOpen = false
    this.$emit('input', this.selectedJob ? this.selectedJob.id : '')
  }

  mounted() {
    this.loadJobs()
  }
}
</script>
<style lang="scss">
</style>

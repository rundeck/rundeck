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
    <select v-bind:value="value" v-on:input="$emit('input',$event.target.value)" class="form-control">
        <option v-for="project in projects" :key="project" v-bind:value="project">{{project}}</option>
    </select>
  </div>
</template>
<script lang="ts">
import { JobReference } from '../../interfaces/JobReference'
import { JobTree } from '../../types/JobTree'
import { GroupedJobs, TreeItem } from '../../types/TreeItem'
import { Job } from '@rundeck/client/dist/lib/models'
import Vue from 'vue'
import { Component, Prop, Watch } from 'vue-property-decorator'
import { client } from '../../modules/rundeckClient'



@Component
export default class ProjectPicker extends Vue {
  @Prop({ required: false, default: '' })
  value!: string

  projects: string[] = []

  loadProjects() {
      this.projects.push('')
      client.projectList().then(result => {
          result.forEach(prj => {
              if(prj.name) this.projects.push(prj.name)
          })
      })
  }

  mounted() {
    this.loadProjects()
  }
}
</script>
<style lang="scss">
</style>

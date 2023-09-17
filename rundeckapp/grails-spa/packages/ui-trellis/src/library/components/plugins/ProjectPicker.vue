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
    <select v-model="value" class="form-control">
        <option v-for="project in projects" :key="project" v-bind:value="project">{{project}}</option>
    </select>
  </div>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import { client } from '../../modules/rundeckClient'

export default defineComponent({
  name: 'ProjectPicker',
  props: {
    modelValue: {
      required: false,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      value: this.modelValue,
      projects: [] as string[],
    }
  },
  methods: {
    loadProjects() {
      this.projects.push('')
      client.projectList().then(result => {
        result.forEach(prj => {
          if (prj.name) this.projects.push(prj.name)
        })
      })
    }
  },
  mounted() {
    this.loadProjects()
  },
  watch: {
    value() {
      this.$emit('update:modelValue', this.value)
    },
    modelValue() {
      this.value = this.modelValue
    },
  },
})

</script>
<style lang="scss">
</style>

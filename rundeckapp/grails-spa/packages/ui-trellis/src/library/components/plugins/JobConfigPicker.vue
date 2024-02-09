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
    <btn
      :class="btnClass"
      :size="btnSize"
      :type="btnType"
      @click="modalOpen = true"
    >
      <slot>Choose A Job &hellip;</slot>
    </btn>

    <modal
      ref="modal"
      v-model="modalOpen"
      :title="'Choose A Job'"
      append-to-body
      :size="size"
    >
      <div v-if="showProjectSelector">
        <label>Project:</label
        ><project-picker v-model="project"></project-picker>
      </div>

      <div v-if="showScheduledToggle" class="form-group">
        <select
          id="_job_config_picker_scheduled_filter"
          v-model="filterType"
          class="form-control"
        >
          <option value="">All Jobs</option>
          <option value="scheduled">Scheduled Jobs</option>
          <option value="notscheduled">Non-Scheduled Jobs</option>
        </select>
      </div>
      <div
        v-for="(item, name) in jobTree.groups"
        :key="'group' + name"
        class="list-group"
      >
        <div v-if="name && item.jobs.length > 0" class="list-group-item">
          <h4 class="list-group-item-heading">{{ item.label }}</h4>
        </div>
        <div
          v-for="job in item.jobs"
          :key="job.id"
          class="list-group-item"
          style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
        >
          <a
            href="#"
            class=""
            :title="'Choose this job: ' + job.id"
            @click="selectJob(job)"
          >
            <i class="glyphicon glyphicon-book"></i>
            {{ job.name }}
          </a>

          <span class="text-secondary">
            {{ job.description }}
          </span>
          <span v-if="job.scheduled" class="text-muted">
            <i class="glyphicon glyphicon-time"></i>
          </span>
        </div>
      </div>
      <template #footer>
        <div>
          <btn @click="modalOpen = false">Cancel</btn>
        </div>
      </template>
    </modal>
  </div>
</template>
<script lang="ts">
import { defineComponent } from "vue";
import ProjectPicker from "./ProjectPicker.vue";
import { JobTree } from "../../types/JobTree";
import { Job } from "@rundeck/client/dist/lib/models";
import { client } from "../../modules/rundeckClient";

export default defineComponent({
  name: "JobConfigPicker",
  components: {
    ProjectPicker,
  },
  props: {
    modelValue: {
      type: String,
      required: false,
      default: "",
    },
    size: {
      type: String,
      required: false,
      default: "",
    },
    btnType: {
      type: String,
      required: false,
      default: "",
    },
    btnSize: {
      type: String,
      required: false,
      default: "",
    },
    btnClass: {
      type: String,
      required: false,
      default: "",
    },
    showScheduledToggle: {
      type: Boolean,
      required: false,
      default: true,
    },
    showScheduledDefault: {
      type: Boolean,
      required: false,
      default: false,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      selectedJob: null as Job | null,
      modalOpen: false,
      jobs: [] as Job[],
      jobTree: new JobTree(),
      project: "",
      showProjectSelector: true,
      filterType: this.showScheduledDefault ? "scheduled" : "",
    };
  },
  watch: {
    project() {
      this.onProjectOrFilterTypeChange();
    },
    filterType() {
      this.onProjectOrFilterTypeChange();
    },
  },
  mounted() {
    if (window._rundeck.projectName) {
      this.showProjectSelector = false;
      this.project = window._rundeck.projectName;
    }
  },
  methods: {
    onProjectOrFilterTypeChange() {
      if (this.project !== "") {
        const params: { [name: string]: any } = {};

        if (this.filterType != "") {
          params["scheduledFilter"] = this.filterType === "scheduled";
        }

        client.jobList(this.project, params).then((result) => {
          this.jobTree = new JobTree();
          this.jobs = result;
          this.jobs.forEach((job) => this.jobTree.insert(job));
        });
      }
    },
    selectJob(job: Job) {
      this.selectedJob = job;
      this.$emit("update:modelValue", this.selectedJob.id);
      this.modalOpen = false;
    },
  },
});
</script>
<style lang="scss"></style>
